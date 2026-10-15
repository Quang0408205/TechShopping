"""
Xây kho tri thức RAG (báo cáo mục 3.6.2 a): Thu thập → Làm sạch → Chunking → Embedding → Vector Database.

Nguồn: knowledge/*.md (chính sách, mỗi mục "## " là một chunk, doc_type POLICY) + mỗi sản phẩm đang bán là
một chunk (tên, danh mục, hãng, giá, phiên bản, thông số chính; doc_type PRODUCT). Mô tả sản phẩm crawl từ TGDĐ
không được đưa vào: phần lớn là câu quảng cáo ("thu cũ đổi mới, trả góp 0%...") làm nhiễu câu hỏi về chính sách.
Chạy lại khi chính sách hoặc catalogue đổi:

    python -m scripts.build_index
"""

import re
from pathlib import Path

from psycopg.rows import dict_row
from psycopg.types.json import Jsonb

from app.chatbot.rag import embed_passages
from app.db import connect

KNOWLEDGE_DIR = Path(__file__).resolve().parent.parent / "knowledge"
MAX_SPECS = 12


def clean(text: str) -> str:
    text = re.sub(r"<[^>]+>", " ", text or "")
    return re.sub(r"\s+", " ", text).strip()


def policy_chunks() -> list[dict]:
    chunks = []
    for path in sorted(KNOWLEDGE_DIR.glob("*.md")):
        document = path.read_text(encoding="utf-8")
        title = document.splitlines()[0].lstrip("# ").strip()
        for section in re.split(r"^## ", document, flags=re.MULTILINE)[1:]:
            heading, _, body = section.partition("\n")
            chunks.append({"doc_type": "POLICY", "source": path.name, "product_id": None,
                           "title": f"{title} – {heading.strip()}", "content": clean(body), "metadata": {}})
    return chunks


def product_chunks(conn) -> list[dict]:
    products = conn.execute("""
        select p.product_id, p.name, c.name as category, coalesce(b.name, '') as brand, p.warranty_months,
               coalesce((select min(coalesce(v.discount_price, v.price)) from product_variants v
                         where v.product_id = p.product_id), p.discount_price, p.base_price) as price,
               (select string_agg(distinct v.variant_name, ', ') from product_variants v
                where v.product_id = p.product_id) as variants
        from products p join categories c using (category_id) left join brands b using (brand_id)
        where p.deleted_at is null and coalesce(p.is_active, true)
    """).fetchall()
    specs: dict[int, list[str]] = {}
    for row in conn.execute("""
        select product_id, spec_name, spec_value from product_specifications
        where spec_value not ilike 'hãng không công bố' order by product_id, spec_order nulls last
    """):
        items = specs.setdefault(row["product_id"], [])
        if len(items) < MAX_SPECS:
            items.append(f"{row['spec_name']}: {clean(row['spec_value'])[:80]}")

    chunks = []
    for p in products:
        lines = [f"{p['name']} – {p['category']} hãng {p['brand']}.",
                 f"Giá bán từ {int(p['price']):,}đ.".replace(",", "."),
                 f"Bảo hành {p['warranty_months'] or 12} tháng."]
        if p["variants"]:
            lines.append(f"Phiên bản: {p['variants'][:300]}.")
        if specs.get(p["product_id"]):
            lines.append("Thông số: " + "; ".join(specs[p["product_id"]]) + ".")
        chunks.append({"doc_type": "PRODUCT", "source": f"product:{p['product_id']}", "product_id": p["product_id"],
                       "title": p["name"], "content": " ".join(lines),
                       "metadata": {"category": p["category"], "brand": p["brand"], "price": float(p["price"])}})
    return chunks


def main():
    with connect(readonly=False) as conn:
        conn.row_factory = dict_row
        chunks = policy_chunks() + product_chunks(conn)
        print(f"Embedding {len(chunks)} chunks ...")
        vectors = embed_passages([f"{c['title']}. {c['content']}" for c in chunks])
        conn.execute("truncate kb_chunks")
        with conn.cursor() as cur:
            cur.executemany("""
                insert into kb_chunks (doc_type, source, product_id, title, content, metadata, embedding)
                values (%s, %s, %s, %s, %s, %s, %s::vector)
            """, [(c["doc_type"], c["source"], c["product_id"], c["title"], c["content"], Jsonb(c["metadata"]),
                   "[" + ",".join(f"{x:.6f}" for x in v) + "]") for c, v in zip(chunks, vectors)])
        print(f"Stored {len(chunks)} chunks in kb_chunks")


if __name__ == "__main__":
    main()
