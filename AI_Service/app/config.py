"""Cấu hình đọc từ biến môi trường (docker-compose.yml truyền vào; chạy local thì dùng giá trị mặc định)."""

import os
from pathlib import Path

DATABASE_URL = os.getenv("AI_DATABASE_URL", "postgresql://postgres:postgres@localhost:5432/techshopping")

# Chatbot gọi API nghiệp vụ của Spring Boot (Function Calling) qua địa chỉ này
BACKEND_URL = os.getenv("AI_BACKEND_URL", "http://localhost:8080/api/v1")

# Backend gửi khoá này trong header X-Internal-Key; AI_Service từ chối request không có khoá đúng
INTERNAL_KEY = os.getenv("AI_INTERNAL_KEY", "")

MODEL_DIR = Path(os.getenv("AI_MODEL_DIR", Path(__file__).resolve().parent.parent / "models"))

# ---- LLM của chatbot ----
# openai: API kiểu OpenAI (Ollama chạy trên máy = MIỄN PHÍ, hoặc Gemini / Groq); anthropic: Claude (trả phí)
LLM_PROVIDER = (os.getenv("LLM_PROVIDER") or "openai").lower()
LLM_BASE_URL = os.getenv("LLM_BASE_URL") or "http://localhost:11434/v1"
LLM_API_KEY = os.getenv("LLM_API_KEY") or "ollama"        # Ollama không cần khoá thật
LLM_MODEL = os.getenv("LLM_MODEL") or "qwen3:8b"
# reasoning_effort gửi kèm mỗi request (vd. "none" tắt bước "suy nghĩ" của Qwen3: 40 giây → 2 giây);
# để trống thì không gửi. Mặc định "none" cho các mô hình qwen3
LLM_REASONING_EFFORT = os.getenv("LLM_REASONING_EFFORT") or ("none" if LLM_MODEL.startswith("qwen3") else "")

ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY") or ""
ANTHROPIC_MODEL = os.getenv("ANTHROPIC_MODEL") or "claude-opus-5-5"

# Mức suy luận của Claude (low | medium | high): hội thoại mua sắm thường đủ với low, nhanh và rẻ hơn
CHAT_EFFORT = os.getenv("AI_CHAT_EFFORT") or "low"

EMBEDDING_MODEL = os.getenv("AI_EMBEDDING_MODEL", "intfloat/multilingual-e5-small")

# Cách tìm trong kho tri thức RAG: vector | keyword | hybrid (vector + từ khoá, gộp bằng RRF).
# scripts/eval_retrieval.py: vector tốt nhất (MRR 0,896; hybrid 0,82–0,86; keyword 0,75) nên là mặc định
RAG_MODE = (os.getenv("RAG_MODE") or "vector").lower()
# Số chunk chính sách đưa cho LLM: Hit@3 = 0,87, Hit@5 = 0,93 (chunk ngắn nên K = 5 gần như không tốn thêm)
RAG_POLICY_K = int(os.getenv("RAG_POLICY_K") or "5")
# trọng số danh sách từ khoá trong RRF của chế độ hybrid (danh sách vector luôn có trọng số 1)
RAG_KEYWORD_WEIGHT = float(os.getenv("RAG_KEYWORD_WEIGHT") or "1.0")

# ---- tham số khuyến nghị (báo cáo mục 3.5) ----

# w_t của từng hành vi (Bảng 3.19), giống InteractionType.java ở backend
INTERACTION_WEIGHTS = {"VIEW": 1.0, "CLICK": 2.0, "LIKE": 3.0, "ADD_TO_CART": 4.0, "PURCHASE": 5.0}

ALPHA = float(os.getenv("REC_ALPHA", "0.4"))          # trọng số CBF trong Hybrid: α=0.4 tốt nhất theo scripts/evaluate.py
COLD_START_K = int(os.getenv("REC_COLD_START_K", "5"))  # k của Bảng 3.21
CANDIDATES_K = 50                                       # K ứng viên lấy từ mỗi mô hình
UPSELL_DELTA = 0.30                                     # δ: giá cao hơn tối đa 30%

ALS_FACTORS = int(os.getenv("ALS_FACTORS", "32"))
ALS_REGULARIZATION = float(os.getenv("ALS_REGULARIZATION", "0.1"))   # λ
ALS_CONFIDENCE = float(os.getenv("ALS_CONFIDENCE", "10"))            # a trong c_ui = 1 + a·r(u,i)
ALS_ITERATIONS = int(os.getenv("ALS_ITERATIONS", "15"))
