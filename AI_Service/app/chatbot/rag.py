"""
RAG (báo cáo mục 3.6.2): tìm Top-K chunk liên quan nhất trong kho tri thức kb_chunks.

Ba cách tìm (RAG_MODE trong app/config.py, mặc định "hybrid"):
  vector   Embedding câu hỏi → K chunk có cosine lớn nhất (pgvector, chỉ mục HNSW). Hiểu nghĩa ("máy pin trâu"),
           nhưng yếu với mã máy ("S26+", "MTJV3", "GA-2100").
  keyword  Tìm từ khoá full-text của PostgreSQL trên cột search_tsv (bỏ dấu tiếng Việt, tên sản phẩm có trọng số
           cao hơn nội dung). Bắt đúng mã máy, nhưng không hiểu từ đồng nghĩa.
  hybrid   Chạy cả hai rồi gộp bằng Reciprocal Rank Fusion có trọng số:
               điểm(d) = 1 / (RRF_K + hạng_vector(d)) + w · 1 / (RRF_K + hạng_từ_khoá(d))
           Không cần chuẩn hoá hai loại điểm khác thang đo, chỉ dùng thứ hạng; w = RAG_KEYWORD_WEIGHT.

Mô hình embedding intfloat/multilingual-e5-small (384 chiều, hỗ trợ tiếng Việt) chạy ngay trong AI_Service,
không gọi API ngoài. Mô hình E5 yêu cầu tiền tố "query: " cho câu hỏi và "passage: " cho tài liệu.
"""

import re
import threading
import unicodedata

import numpy as np

from app.config import EMBEDDING_MODEL, RAG_KEYWORD_WEIGHT, RAG_MODE
from app.db import connect

_model = None
_lock = threading.Lock()

DOC_TYPES = ("POLICY", "PRODUCT")
MODES = ("vector", "keyword", "hybrid")

RRF_K = 60                  # hằng số chuẩn của RRF (Cormack và cộng sự, 2009)
CANDIDATES = 40             # số ứng viên lấy từ mỗi danh sách trước khi gộp

# Từ không mang nghĩa tìm kiếm (đã bỏ dấu); khớp các từ này làm nhiễu xếp hạng từ khoá
STOPWORDS = set("""
a ai anh ban bao biet bo cai can cho chi co con cua da dang de den di do duoc em gi giup ha hay hien hoi khong la
gia lam luon may minh moi mot muon nao nay ne nen nhe nhi nhieu nhung nua o oi on qua ra roi sao se shop sau thi the
theo thoi toi tot trong tu va van vay ve voi vua xin xem
""".split())


def embedder():
    global _model
    with _lock:
        if _model is None:
            from sentence_transformers import SentenceTransformer
            _model = SentenceTransformer(EMBEDDING_MODEL, device="cpu")
    return _model


def embed_passages(texts: list[str], batch_size: int = 64) -> np.ndarray:
    return embedder().encode(["passage: " + t for t in texts], batch_size=batch_size,
                             normalize_embeddings=True, show_progress_bar=len(texts) > 200)


def embed_query(text: str) -> np.ndarray:
    return embedder().encode(["query: " + text], normalize_embeddings=True)[0]


def keyword_query(text: str) -> str | None:
    """
    "Galaxy S26+ giá bao nhiêu?" → "galaxy:* | s26:*" cho to_tsquery('simple', ...). Bỏ dấu giống f_unaccent của
    PostgreSQL, bỏ từ dừng, dùng OR (khớp càng nhiều từ điểm càng cao) và tìm theo tiền tố ("265" khớp "265s").
    """
    plain = unicodedata.normalize("NFD", text.lower().replace("đ", "d"))
    plain = "".join(ch for ch in plain if unicodedata.category(ch) != "Mn")
    tokens = [t for t in re.split(r"[^a-z0-9]+", plain) if t and t not in STOPWORDS]
    tokens = list(dict.fromkeys(tokens))[:12]
    if not tokens:
        return None
    return " | ".join(f"{t}:*" if len(t) >= 3 else t for t in tokens)


def _filters(doc_type, category, brand, min_price, max_price) -> tuple[list[str], dict]:
    conditions, params = [], {}
    if doc_type in DOC_TYPES:
        conditions.append("doc_type = %(doc_type)s")
        params["doc_type"] = doc_type
    if category:
        conditions.append("metadata->>'category' ilike %(category)s")
        params["category"] = category
    if brand:
        conditions.append("metadata->>'brand' ilike %(brand)s")
        params["brand"] = brand
    if min_price is not None:
        conditions.append("(metadata->>'price')::numeric >= %(min_price)s")
        params["min_price"] = min_price
    if max_price is not None:
        conditions.append("(metadata->>'price')::numeric <= %(max_price)s")
        params["max_price"] = max_price
    return conditions, params


COLUMNS = "chunk_id, doc_type, source, product_id, title, content, metadata"


def _vector_rows(conn, query: str, conditions: list[str], params: dict, limit: int) -> list[dict]:
    params = {**params, "v": "[" + ",".join(f"{x:.6f}" for x in embed_query(query)) + "]", "n": limit}
    where = ("where " + " and ".join(conditions)) if conditions else ""
    # lọc metadata + HNSW: quét rộng hơn để vẫn đủ kết quả sau khi lọc
    conn.execute("set local hnsw.ef_search = 200")
    return conn.execute(f"""
        select {COLUMNS}, 1 - (embedding <=> %(v)s::vector) as similarity
        from kb_chunks {where}
        order by embedding <=> %(v)s::vector
        limit %(n)s
    """, params).fetchall()


def _keyword_rows(conn, query: str, conditions: list[str], params: dict, limit: int) -> list[dict]:
    tsquery = keyword_query(query)
    if not tsquery:
        return []
    params = {**params, "q": tsquery, "n": limit}
    where = " and ".join(conditions + ["search_tsv @@ to_tsquery('simple', %(q)s)"])
    return conn.execute(f"""
        select {COLUMNS}, ts_rank(search_tsv, to_tsquery('simple', %(q)s)) as keyword_score
        from kb_chunks where {where}
        order by keyword_score desc, chunk_id
        limit %(n)s
    """, params).fetchall()


def search(query: str, k: int = 5, doc_type: str | None = None, min_similarity: float = 0.8,
           category: str | None = None, brand: str | None = None,
           min_price: float | None = None, max_price: float | None = None, mode: str | None = None,
           keyword_weight: float | None = None) -> list[dict]:
    """
    Top-K chunk cho câu hỏi. Bộ lọc metadata (danh mục, hãng, khoảng giá) áp dụng trước khi xếp hạng.
    Với vector / hybrid: chunk chỉ tìm thấy bằng vector mà cosine < min_similarity bị bỏ, để LLM không trả lời
    dựa trên nội dung không liên quan; chunk khớp từ khoá thì được giữ.
    Mỗi kết quả có similarity (cosine, 0 nếu chỉ khớp từ khoá) và score (điểm dùng để xếp hạng).
    """
    mode = mode or RAG_MODE
    weight = RAG_KEYWORD_WEIGHT if keyword_weight is None else keyword_weight
    conditions, params = _filters(doc_type, category, brand, min_price, max_price)
    with connect() as conn:
        vector = _vector_rows(conn, query, conditions, params, CANDIDATES if mode == "hybrid" else k) \
            if mode in ("vector", "hybrid") else []
        keyword = _keyword_rows(conn, query, conditions, params, CANDIDATES if mode == "hybrid" else k) \
            if mode in ("keyword", "hybrid") else []

    if mode == "vector":
        return [dict(r, similarity=round(float(r["similarity"]), 3), score=round(float(r["similarity"]), 3))
                for r in vector if r["similarity"] >= min_similarity]
    if mode == "keyword":
        return [dict(r, similarity=0.0, score=round(float(r["keyword_score"]), 4)) for r in keyword]

    fused: dict[int, dict] = {}
    for rank, row in enumerate(vector, 1):
        item = fused.setdefault(row["chunk_id"], dict(row, similarity=float(row["similarity"]), score=0.0,
                                                      keyword=False))
        item["score"] += 1 / (RRF_K + rank)
    for rank, row in enumerate(keyword, 1):
        item = fused.setdefault(row["chunk_id"], dict(row, similarity=0.0, score=0.0, keyword=False))
        item["score"] += weight / (RRF_K + rank)
        item["keyword"] = True
    ranked = sorted(fused.values(), key=lambda r: r["score"], reverse=True)
    kept = [r for r in ranked if r["keyword"] or r["similarity"] >= min_similarity][:k]
    for r in kept:
        r.pop("keyword_score", None)
        r["similarity"] = round(r["similarity"], 3)
        r["score"] = round(r["score"], 5)
    return kept
