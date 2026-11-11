"""
AI Service (Python/FastAPI). Chỉ Spring Boot Backend gọi vào, kèm header X-Internal-Key.
Endpoint nội bộ: khuyến nghị và /chat (chatbot).
Tài liệu tự sinh: http://localhost:8000/docs
"""

import asyncio
import json
import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.concurrency import run_in_threadpool
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app import config
from app.chatbot import agent
from app.recommender.engine import Preferences, recommender, seconds_until_retrain
from app.recommender.evaluation import evaluation_job

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("ai-service")

RETRAIN_EVERY_SECONDS = 24 * 3600
RETRAIN_RETRY_SECONDS = 3600


async def _retrain_daily():
    """
    Huấn luyện lại khi mô hình đủ 24 giờ tuổi, tính từ lúc nó được huấn luyện (trained_at) chứ không từ lúc dịch vụ
    khởi động. Trước 11/11/2026 vòng lặp chờ 24 giờ kể từ khi khởi động, nên máy tắt / mở lại mỗi ngày thì mô hình
    lưu trong volume không bao giờ được huấn luyện lại (sản phẩm và hành vi mới không vào gợi ý).
    """
    delay = seconds_until_retrain(recommender.model.trained_at, RETRAIN_EVERY_SECONDS)
    while True:
        await asyncio.sleep(delay)
        try:
            await run_in_threadpool(recommender.train)
            delay = RETRAIN_EVERY_SECONDS
        except Exception:
            log.exception("Scheduled retraining failed; keeping the previous model")
            delay = RETRAIN_RETRY_SECONDS


@asynccontextmanager
async def lifespan(_: FastAPI):
    await run_in_threadpool(recommender.load_or_train)
    task = asyncio.create_task(_retrain_daily())
    yield
    task.cancel()


app = FastAPI(title="TechShopping AI Service", version="1.0", lifespan=lifespan)


def internal_only(x_internal_key: str | None = Header(default=None)):
    if config.INTERNAL_KEY and x_internal_key != config.INTERNAL_KEY:
        raise HTTPException(status_code=401, detail="Missing or wrong X-Internal-Key")


class PreferencesBody(BaseModel):
    """Câu trả lời bảng hỏi (khách đăng nhập: Backend đọc từ user_preferences; khách vãng lai: trình duyệt gửi)."""
    category_ids: list[int] = Field(default_factory=list, alias="categoryIds", max_length=20)
    brand_ids: list[int] = Field(default_factory=list, alias="brandIds", max_length=100)
    price_levels: list[str] = Field(default_factory=list, alias="priceLevels", max_length=3)
    need_ids: list[int] = Field(default_factory=list, alias="needIds", max_length=50)

    def to_preferences(self) -> Preferences:
        return Preferences(tuple(self.category_ids), tuple(self.brand_ids), tuple(self.price_levels),
                           tuple(self.need_ids))


class RecommendRequest(BaseModel):
    user_id: int | None = Field(default=None, alias="userId")
    n: int = Field(default=8, ge=1, le=50)
    exclude_product_ids: list[int] = Field(default_factory=list, alias="excludeProductIds")
    preferences: PreferencesBody | None = None


class ProductRequest(BaseModel):
    product_id: int = Field(alias="productId")
    user_id: int | None = Field(default=None, alias="userId")
    n: int = Field(default=4, ge=1, le=20)


class ChatTurn(BaseModel):
    role: str = Field(pattern="^(user|assistant)$")
    content: str = Field(max_length=20000)


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=1000)
    history: list[ChatTurn] = Field(default_factory=list, max_length=40)
    access_token: str | None = Field(default=None, alias="accessToken")


def _items(recommendations) -> dict:
    return {"items": [r.to_dict() for r in recommendations]}


@app.get("/health")
def health():
    model = recommender.model
    return {"status": "UP", "trainedAt": model.trained_at.isoformat(timespec="seconds"), **model.stats}


@app.post("/recommend", dependencies=[Depends(internal_only)])
def recommend(request: RecommendRequest):
    """
    Khuyến nghị cá nhân hoá Top-N. basis: BEHAVIOR (theo hành vi của khách), PREFERENCES (khách chưa có hành vi:
    theo câu trả lời bảng hỏi), POPULAR (chưa có cả hai: phổ biến + mới, giao diện mời trả lời bảng hỏi).
    """
    preferences = request.preferences.to_preferences() if request.preferences else None
    items, basis = recommender.personal(request.user_id, request.n, request.exclude_product_ids,
                                        preferences=preferences)
    return {**_items(items), "basis": basis}


@app.post("/similar", dependencies=[Depends(internal_only)])
def similar(request: ProductRequest):
    """Sản phẩm tương tự sản phẩm đang xem (CBF giữa hai sản phẩm)."""
    return _items(recommender.similar(request.product_id, request.n))


@app.post("/upsell", dependencies=[Depends(internal_only)])
def upsell(request: ProductRequest):
    return _items(recommender.upsell(request.product_id, request.user_id, request.n))


@app.post("/cross-sell", dependencies=[Depends(internal_only)])
def cross_sell(request: ProductRequest):
    return _items(recommender.cross_sell(request.product_id, request.user_id, request.n))


@app.post("/train", dependencies=[Depends(internal_only)])
async def train():
    """Huấn luyện lại ALS + tính lại vector sản phẩm (chạy tự động mỗi 24 giờ)."""
    return await run_in_threadpool(recommender.train)


@app.get("/evaluation", dependencies=[Depends(internal_only)])
def evaluation():
    """Đánh giá offline gần nhất (Precision / Recall / NDCG / HitRate / Coverage theo α) + có đang chạy không."""
    return evaluation_job.status()


@app.post("/evaluation", dependencies=[Depends(internal_only)])
def start_evaluation():
    """Chạy lại đánh giá offline ở nền (khoảng 5 phút) trên một bản mô hình riêng; xem kết quả bằng GET."""
    return evaluation_job.start()


def _require_llm():
    reason = agent.not_ready_reason()
    if reason:
        raise HTTPException(status_code=503, detail=reason)


@app.post("/chat", dependencies=[Depends(internal_only)])
def chat(request: ChatRequest):
    """
    Một lượt hội thoại. accessToken là JWT của khách (Backend chuyển tiếp) để tool gọi API nghiệp vụ
    dưới quyền của chính khách; không có token thì chỉ trả lời câu hỏi chung.
    """
    _require_llm()
    try:
        return agent.chat(request.message, [t.model_dump() for t in request.history], request.access_token)
    except agent.provider().errors as error:
        log.warning("LLM error: %s", error)
        raise HTTPException(status_code=503, detail="LLM unavailable") from error


@app.post("/chat/stream", dependencies=[Depends(internal_only)])
def chat_stream(request: ChatRequest):
    """
    Như /chat nhưng trả Server-Sent Events: "data: {json}\\n\\n" cho từng sự kiện delta / status / done,
    hoặc một sự kiện {"type": "error"} nếu LLM lỗi giữa chừng.
    """
    _require_llm()

    def events():
        try:
            for event in agent.chat_events(request.message, [t.model_dump() for t in request.history],
                                           request.access_token):
                yield "data: " + json.dumps(event, ensure_ascii=False) + "\n\n"
        except agent.provider().errors as error:
            log.warning("LLM error during stream: %s", error)
            yield "data: " + json.dumps({"type": "error", "message": "LLM unavailable"}) + "\n\n"

    return StreamingResponse(events(), media_type="text/event-stream",
                             headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})
