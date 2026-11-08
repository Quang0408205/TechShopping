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
# Thông số khách hay hỏi theo nhu cầu ("chụp ảnh đẹp", "pin trâu", "chơi game") được lấy trước, theo thứ tự này;
# còn chỗ thì lấy thêm các thông số khác theo thứ tự gốc. Trước đây chỉ lấy 12 thông số đầu theo thứ tự crawl nên
# hơn nửa chunk điện thoại thiếu độ phân giải camera và không chunk nào có dung lượng pin.
KEY_SPECS = {
    "Điện Thoại": ["Chip xử lý (CPU)", "RAM", "Dung lượng lưu trữ", "Độ phân giải camera sau", "Tính năng camera sau",
                   "Độ phân giải camera trước", "Dung lượng pin", "Hỗ trợ sạc tối đa", "Kích thước màn hình",
                   "Tần số quét", "Tiêu chuẩn chống nước, chống bụi", "Hệ điều hành"],
    "Máy Tính Bảng": ["Chip xử lý (CPU)", "RAM", "Dung lượng lưu trữ", "Kích thước màn hình", "Công nghệ màn hình",
                      "Tần số quét", "Dung lượng pin", "Hỗ trợ sạc tối đa", "Độ phân giải camera sau",
                      "Mạng di động", "Thực hiện cuộc gọi", "Hệ điều hành"],
    "Laptop": ["Công nghệ CPU", "Loại CPU", "RAM", "Ổ cứng", "Card đồ họa", "Kích thước màn hình", "Độ phân giải",
               "Tần số quét", "Thông tin Pin", "Khối lượng máy", "Hệ điều hành", "Hiệu năng xử lý AI (TOPS)"],
    "Đồng Hồ Thông Minh": ["Theo dõi sức khoẻ", "Thời gian sử dụng", "Dung lượng pin", "Kích thước màn hình",
                           "Công nghệ màn hình", "Tiêu chuẩn chống nước, chống bụi", "Định vị", "Thực hiện cuộc gọi",
                           "SIM", "Tương thích", "Môn thể thao", "Hệ điều hành"],
    "Đồng Hồ Thời Trang": ["Đối tượng sử dụng", "Loại máy", "Đường kính mặt", "Chất liệu dây", "Chất liệu mặt",
                           "Chất liệu khung viền", "Tiêu chuẩn chống nước, chống bụi", "Tiện ích", "Nguồn năng lượng",
                           "Bộ sưu tập", "Thời gian sử dụng", "Sản xuất tại"],
    "Phụ Kiện": ["Loại sản phẩm", "Tương thích", "Công nghệ âm thanh", "Thời gian sử dụng", "Kết nối",
                 "Dung lượng pin", "Nguồn ra", "Cổng sạc", "Thời gian sạc", "Tiện ích", "Công nghệ/Tiện ích",
                 "Trong hộp có"],
}


def pick_specs(category: str, specs: list[tuple[str, str]]) -> list[str]:
    """Tối đa MAX_SPECS "tên: giá trị": thông số chính của danh mục trước, rồi các thông số khác theo thứ tự gốc."""
    by_name: dict[str, str] = {}
    for name, value in specs:
        by_name.setdefault(name, value)          # trùng tên thì giữ dòng đầu, như trước
    key = [name for name in KEY_SPECS.get(category, []) if name in by_name]
    rest = [name for name in by_name if name not in key]
    return [f"{name}: {clean(by_name[name])[:80]}" for name in (key + rest)[:MAX_SPECS]]


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
    raw_specs: dict[int, list[tuple[str, str]]] = {}
    for row in conn.execute("""
        select product_id, spec_name, spec_value from product_specifications
        where spec_value not ilike 'hãng không công bố' order by product_id, spec_order nulls last
    """):
        raw_specs.setdefault(row["product_id"], []).append((row["spec_name"], row["spec_value"]))
    chunks = []
    for p in products:
        head = [f"{p['name']} – {p['category']} hãng {p['brand']}.",
                f"Giá bán từ {int(p['price']):,}đ.".replace(",", "."),
                f"Bảo hành {p['warranty_months'] or 12} tháng."]
        if p["variants"]:
            head.append(f"Phiên bản: {p['variants'][:300]}.")
        raw = raw_specs.get(p["product_id"], [])
        # content (LLM đọc, keyword search): thông số chính của danh mục trước.
        # embed (vector search): 12 thông số đầu theo thứ tự gốc như trước. Đo trên evals/retrieval_cases.json,
        # đưa thông số chính vào embedding làm các chunk cùng loại giống nhau hơn và hạ vector Hit@1 0,850 → 0,775
        # (nhầm Redmi Note 14 Pro+ / 15 Pro+, tai nghe chống ồn), nên embedding giữ cách cũ.
        content = head + (["Thông số: " + "; ".join(pick_specs(p["category"], raw)) + "."] if raw else [])
        embed = head + (["Thông số: " + "; ".join(f"{n}: {clean(v)[:80]}" for n, v in raw[:MAX_SPECS]) + "."]
                        if raw else [])
        chunks.append({"doc_type": "PRODUCT", "source": f"product:{p['product_id']}", "product_id": p["product_id"],
                       "title": p["name"], "content": " ".join(content), "embed": " ".join(embed),
                       "metadata": {"category": p["category"], "brand": p["brand"], "price": float(p["price"])}})
    return chunks


def main():
    with connect(readonly=False) as conn:
        conn.row_factory = dict_row
        chunks = policy_chunks() + product_chunks(conn)
        print(f"Embedding {len(chunks)} chunks ...")
        vectors = embed_passages([f"{c['title']}. {c.get('embed', c['content'])}" for c in chunks])
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
