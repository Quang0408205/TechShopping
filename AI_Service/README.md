# Phần AI của TechShopping: Khuyến nghị sản phẩm + Chatbot

Tài liệu này giải thích **mọi thứ đã thêm vào dự án cho phần AI**: dùng công nghệ gì, file nào làm gì, thuật toán chạy ra sao, cách cài đặt, chạy, kiểm thử. Thiết kế bám theo báo cáo khoá luận (mục 3.5 Recommendation, 3.6 Chatbot, 3.7 Đánh giá).

Phần AI gồm hai chức năng:

| Chức năng | Khách thấy ở đâu | Cần gì để chạy |
|---|---|---|
| **Khuyến nghị sản phẩm** (Recommendation System) | Trang **Khuyến nghị**; 3 khu ở trang **chi tiết sản phẩm**: *Có thể nâng cấp lên*, *Thường được mua kèm*, *Sản phẩm tương tự* | Chạy ngay, không cần khoá API |
| **Trợ lý mua sắm AI** (Chatbot) | Nút 💬 ở góc mọi trang khách; trang admin **Lịch sử chatbot** | Mặc định **miễn phí**: mô hình mở Qwen3 8B chạy trên máy bằng Ollama. Có thể đổi sang Claude (trả phí) hoặc API miễn phí khác chỉ bằng `.env` |

---

## Mục lục

1. [Kiến trúc tổng thể](#1-kiến-trúc-tổng-thể)
2. [Công nghệ sử dụng](#2-công-nghệ-sử-dụng)
3. [Các file đã thêm / sửa](#3-các-file-đã-thêm--sửa)
4. [Hệ thống khuyến nghị hoạt động thế nào](#4-hệ-thống-khuyến-nghị-hoạt-động-thế-nào)
5. [Chatbot hoạt động thế nào](#5-chatbot-hoạt-động-thế-nào)
6. [API](#6-api)
7. [Cơ sở dữ liệu](#7-cơ-sở-dữ-liệu)
8. [Cài đặt và chạy](#8-cài-đặt-và-chạy)
9. [Kiểm thử và kết quả đánh giá](#9-kiểm-thử-và-kết-quả-đánh-giá)
10. [Bảo mật và chi phí](#10-bảo-mật-và-chi-phí)
11. [Giới hạn hiện tại](#11-giới-hạn-hiện-tại)
12. [Gặp lỗi thì làm gì](#12-gặp-lỗi-thì-làm-gì)

---

## 1. Kiến trúc tổng thể

```mermaid
flowchart LR
    W[Website<br/>HTML/CSS/JS] -->|REST + JWT| B[Spring Boot Backend<br/>:8080]
    B -->|REST + X-Internal-Key| AI[AI Service<br/>Python/FastAPI :8000]
    B --> DB[(PostgreSQL 16<br/>+ pgvector)]
    AI -->|chỉ đọc| DB
    AI -->|Function Calling<br/>kèm JWT của khách| B
    AI -->|API kiểu OpenAI| O[Ollama trên máy<br/>Qwen3 8B - miễn phí]
    AI -.->|tuỳ chọn, trả phí| C[Claude<br/>Anthropic API]
```

Nguyên tắc chính:

- **Trình duyệt không gọi thẳng AI Service.** Mọi request đi qua Backend. Backend gửi kèm header `X-Internal-Key`, AI Service từ chối request không có khoá này.
- **Backend giữ quyền kiểm soát nghiệp vụ.** Backend kiểm tra lại sản phẩm AI trả về (còn bán, chưa xoá), lưu lịch sử, phân quyền, giới hạn số lần gọi.
- **Recommendation Service chỉ đọc database** (kết nối đặt `default_transaction_read_only=on`).
- **Chatbot không đọc bảng nghiệp vụ trực tiếp.** Muốn xem đơn hàng, giỏ hàng… nó gọi API Backend **bằng chính token của khách**, nên Backend vẫn kiểm tra quyền như bình thường.
- **AI hỏng thì web vẫn chạy.** Khuyến nghị có danh sách dự phòng; chatbot tự quay về trả lời theo từ khoá.

---

## 2. Công nghệ sử dụng

### AI Service (thư mục `AI_Service/`)

| Công nghệ | Phiên bản | Dùng để |
|---|---|---|
| **Python** | 3.11 | Ngôn ngữ của AI Service |
| **FastAPI** + **Uvicorn** | 0.118 / 0.37 | REST API nội bộ, tài liệu tự sinh ở `/docs` |
| **NumPy**, **SciPy**, **pandas** | 2.3 / 1.16 / 2.3 | Xử lý dữ liệu, ma trận đặc trưng, ma trận thưa; **ALS tự cài đặt bằng NumPy** (không dùng thư viện ALS có sẵn) |
| **psycopg 3** | 3.2 | Kết nối PostgreSQL |
| **sentence-transformers** + **PyTorch (CPU)** | 5.1 / 2.8 | Chạy mô hình Embedding cho RAG |
| Mô hình **`intfloat/multilingual-e5-small`** | | Biến văn bản thành vector 384 chiều; hỗ trợ tiếng Việt, chạy ngay trong container, không gọi dịch vụ ngoài |
| **Ollama** + mô hình **Qwen3 8B** (`qwen3:8b`) | | **LLM mặc định, miễn phí**: chạy ngay trên máy (GPU Apple Silicon / NVIDIA), hỗ trợ tiếng Việt và gọi tool |
| **OpenAI Python SDK** (`openai`) | ≥ 1.60 | Client cho mọi API **kiểu OpenAI Chat Completions**: Ollama, hoặc gói miễn phí của Gemini / Groq |
| **Anthropic Python SDK** (`anthropic`) | ≥ 1.0 | Tuỳ chọn trả phí: gọi **Claude** (`claude-opus-5-5`, `claude-haiku-5-5`...) với *tool use* |
| **httpx** | 0.28 | Chatbot gọi API nghiệp vụ của Spring Boot |
| **Docker** | | Image `python:3.11-slim`, tải sẵn mô hình embedding khi build |

### Cơ sở dữ liệu

| Công nghệ | Dùng để |
|---|---|
| **PostgreSQL 16** | Database chung của hệ thống |
| **pgvector 0.8** (image `pgvector/pgvector:pg16-trixie`) | Vector Database cho RAG: kiểu `vector(384)`, toán tử khoảng cách cosine `<=>`, chỉ mục **HNSW** |
| **JSONB** | Metadata của chunk (danh mục, hãng, giá) để lọc khi tìm kiếm |

### Backend (Spring Boot 4, đã có sẵn)

| Công nghệ | Dùng để |
|---|---|
| **Spring `RestClient`** | Gọi AI Service (timeout 3 giây cho khuyến nghị, 90 giây cho chatbot) |
| **Spring Data JPA** | Entity mới: `UserInteraction`, `Recommendation`, `ChatSession`, `ChatMessage` |
| **Redis** | Giới hạn số tin nhắn chatbot mỗi giờ |
| **JUnit 5 + Mockito** | Unit test cho các service mới |

### Frontend (HTML/CSS/JS thuần, đã có sẵn)

Không thêm thư viện. Dùng `fetch` với `keepalive` để ghi hành vi bấm sản phẩm ngay cả khi trình duyệt đang chuyển trang.

---

## 3. Các file đã thêm / sửa

### 3.1. AI Service (`AI_Service/`): mới hoàn toàn

```
AI_Service/
├── Dockerfile, .dockerignore, requirements.txt
├── app/
│   ├── main.py              FastAPI: /recommend /similar /upsell /cross-sell /train /chat /chat/stream /health
│   ├── config.py            Đọc biến môi trường + tham số mô hình (α, k, K, δ, ALS...)
│   ├── db.py                Kết nối PostgreSQL (mặc định chỉ đọc)
│   ├── recommender/
│   │   ├── data.py          Đọc sản phẩm, thông số, tương tác, đơn hàng, quan hệ sản phẩm
│   │   ├── features.py      Trích thông số (RAM, bộ nhớ, màn hình, camera), Min-Max, One-Hot, điểm r(u,i)
│   │   ├── als.py           Thuật toán ALS cho phản hồi ngầm định + fold-in
│   │   └── engine.py        CBF, Hybrid, cold-start, lọc, xếp hạng, Upsell, Cross-sell, lý do đề xuất
│   └── chatbot/
│       ├── rag.py           Embedding + tìm kiếm vector trong pgvector (có lọc metadata)
│       ├── tools.py         11 function cho Function Calling (gọi API Backend bằng token của khách)
│       └── agent.py         Vòng hội thoại với LLM (streaming): hỏi → gọi tool → trả lời; 2 nhà cung cấp: OpenAI-compatible (Ollama) / Claude
├── evals/
│   ├── chatbot_cases.json   30 câu hỏi kiểm thử chatbot kèm đáp án mong đợi
│   ├── retrieval_cases.json 40 câu hỏi có đáp án để đánh giá phần tìm kiếm RAG
│   └── results/             Kết quả mỗi lần chạy eval_chatbot.py (latest.md để dán vào báo cáo)
├── knowledge/               Tài liệu chính sách cho RAG (Markdown)
│   ├── giao-hang.md, thanh-toan.md, bao-hanh-tra-hang.md, ho-tro.md
└── scripts/
    ├── seed_interactions.py Sinh dữ liệu hành vi MÔ PHỎNG (chỉ dùng local)
    ├── build_relations.py   Tạo quan hệ phụ kiện (ACCESSORY) cho "Mua kèm"
    ├── build_index.py       Xây kho tri thức RAG (chunk → embedding → kb_chunks)
    ├── evaluate.py          Đánh giá khuyến nghị: Precision / Recall / NDCG / HitRate / Coverage
    ├── eval_retrieval.py    Đánh giá tìm kiếm RAG: Hit@K / Recall@K / MRR cho vector, keyword, hybrid
    └── eval_chatbot.py      Đánh giá chatbot tự động: tool, nội dung, thao tác + giám khảo LLM
```

### 3.2. Backend (`backend/Tech/src/main/java/com/example/Tech/`)

| File | Mới / sửa | Nội dung |
|---|---|---|
| `config/AiServiceProperties.java`, `config/AiConfig.java` | Mới | Cấu hình `app.ai.*`, 2 `RestClient` (khuyến nghị / chatbot) có header `X-Internal-Key` |
| `entity/recommendation/InteractionType.java` | Mới | `VIEW=1, CLICK=2, LIKE=3, ADD_TO_CART=4, PURCHASE=5` (trọng số w_t) |
| `entity/recommendation/UserInteraction.java`, `Recommendation.java`, `RecommendationType.java` | Mới | Bảng `user_interactions`, `recommendations`; loại `PERSONAL / SIMILAR / UPSELL / CROSS_SELL` |
| `service/recommendation/InteractionRecorder.java` | Mới | Ghi hành vi, bỏ qua lặp lại trong 30 phút |
| `service/recommendation/AiRecommendationClient.java` | Mới | Gọi AI Service |
| `service/recommendation/RecommendationService.java` | Mới | Kiểm tra lại sản phẩm, lưu log, **danh sách dự phòng** khi AI ngừng |
| `controller/recommendation/InteractionController.java`, `RecommendationController.java` | Mới | API ghi hành vi, API khuyến nghị, API huấn luyện lại (ADMIN) |
| `entity/chat/ChatSession.java`, `ChatMessage.java` + repository | Mới | Bảng `chat_sessions`, `chat_messages` |
| `service/chat/ChatService.java`, `AiChatClient.java`, `ChatRateLimiter.java` | Mới | Quản lý phiên chat, gọi chatbot (thường + streaming), chuyển tiếp SSE, giới hạn số tin nhắn |
| `dto/response/chat/ChatAction.java` | Mới | Thao tác chatbot đề xuất (thêm giỏ / huỷ đơn) để hiện nút Xác nhận |
| `controller/chat/ChatController.java`, `AdminChatController.java` | Mới | API chat (`/chat`, `/chat/stream`) cho khách + xem lịch sử cho ADMIN |
| `service/impl/cart/CartServiceImpl.java` | Sửa | Thêm vào giỏ → ghi `ADD_TO_CART` |
| `service/impl/order/OrderServiceImpl.java` | Sửa | Đặt hàng → ghi `PURCHASE` cho từng sản phẩm |
| `service/product/ProductService(Impl).java` | Sửa | Thêm `getActiveByIds` (lấy sản phẩm đang bán theo đúng thứ tự AI trả) |
| `security/SecurityConfig.java` | Sửa | `POST /recommendations`, `POST /chat`, `POST /chat/stream`, `GET /chat/*/messages` công khai (token tuỳ chọn) |
| `exception/ErrorCode.java` | Sửa | `AI_SERVICE_UNAVAILABLE (503)`, `CHAT_SESSION_NOT_FOUND (404)` |
| `resources/application.yml` | Sửa | Khối `app.ai` (base-url, internal-key, timeout) |
| Test: `InteractionRecorderTest`, `RecommendationServiceTest`, `ChatServiceTest` | Mới | 15 unit test; sửa nhẹ 3 test cũ vì constructor có thêm tham số |

### 3.3. Frontend (`frontend/`)

| File | Thay đổi |
|---|---|
| `js/core/api.js` | `trackInteraction()` + tự gửi `CLICK` khi bấm vào thẻ sản phẩm bất kỳ |
| `js/core/ui.js` | `fetchRecommendations()`, `renderRecommendationGrid()` (thẻ sản phẩm + dòng **lý do gợi ý**) |
| `js/customer/recommendation.js` | 5 khu của trang Khuyến nghị lấy dữ liệu từ AI thay cho quy tắc tạm |
| `js/customer/product-detail.js`, `customer/product-detail.html` | Gửi `VIEW`; thêm 3 khu Nâng cấp / Mua kèm / Tương tự; nhớ sản phẩm xem gần nhất |
| `js/customer/chatbot.js` | Widget gọi chatbot AI: **chữ hiện dần** (đọc luồng SSE), dòng "Đang tra cứu…", **nút Xác nhận** cho thao tác đề xuất, giữ cuộc trò chuyện khi chuyển trang, **tự quay về chế độ từ khoá** khi AI ngừng |
| `js/admin/chat-history.js`, `admin/chat-history.html`, `partials/admin-sidebar.html` | Trang Lịch sử chatbot đọc dữ liệu thật, hiện trong menu ADMIN |
| `css/customer-theme.css`, `css/style.css`, `css/admin/admin.css` | Style dòng lý do gợi ý, xuống dòng trong tin nhắn chatbot |

### 3.4. Database và Docker

| File | Thay đổi |
|---|---|
| `database/migrations/ai_recommendation.sql` | **Migration cho database đã có** (chạy lại được nhiều lần) |
| `database/techshopping.sql` | Schema gốc cập nhật giống migration (database tạo mới) |
| `docker-compose.yml` | Image Postgres → `pgvector/pgvector:pg16-trixie`; thêm service `ai-service`; biến môi trường AI cho backend |
| `.env.example` | `LLM_PROVIDER`, `LLM_BASE_URL`, `LLM_MODEL`, `ANTHROPIC_API_KEY`, `AI_INTERNAL_KEY` |

---

## 4. Hệ thống khuyến nghị hoạt động thế nào

### 4.1. Thu thập hành vi

| Hành vi | Ai ghi | Khi nào | Trọng số w_t |
|---|---|---|---|
| `VIEW` | Website → `POST /api/v1/interactions` | Mở trang chi tiết sản phẩm | 1 |
| `CLICK` | Website → `POST /api/v1/interactions` | Bấm thẻ sản phẩm trong danh sách | 2 |
| `LIKE` | (chưa dùng) | Dự phòng cho danh sách yêu thích | 3 |
| `ADD_TO_CART` | Backend tự ghi | Thêm vào giỏ hàng | 4 |
| `PURCHASE` | Backend tự ghi | Đặt hàng | 5 |

- Chỉ ghi khi khách đã đăng nhập.
- Trình duyệt chỉ được gửi `VIEW` và `CLICK`, nên không thể tự khai là "đã mua".
- Cùng một hành vi với cùng một sản phẩm trong 30 phút chỉ ghi một lần, nên tải lại trang không làm tăng điểm.

### 4.2. Xây đặc trưng (`features.py`)

- **Điểm tương tác:** `r(u,i) = Σ_t w_t · log(1 + n(u,i,t))`. Hàm log làm cho việc xem đi xem lại nhiều lần không lấn át được hành vi mua.
- **Thông số kỹ thuật:** đọc `ram_gb`, `storage_gb`, `screen_inch`, `camera_mp` theo thứ tự tin cậy:
  1. tên sản phẩm (dữ liệu TGDĐ ghi sẵn, ví dụ `8GB/256GB` hoặc `(Core 5, 16GB, 512GB)`);
  2. bảng `product_specifications`;
  3. cột `ram` / `storage` của `product_variants`.

  "Hãng không công bố" được xem là thiếu và thay bằng **trung vị của danh mục**. Với laptop, dữ liệu import ghi nhầm RAM vào cột `storage` của phiên bản, nên giá trị < 64 GB ở đó bị bỏ qua.
- **Vector sản phẩm v_i (110 chiều):** gồm One-Hot (danh mục, hãng, phân khúc giá Thấp/Trung/Cao), giá chuẩn hoá Min-Max và 4 thông số chuẩn hoá. Giá và phân khúc giá tính **trong từng danh mục**, vì điện thoại 5 triệu là "thấp" nhưng đồng hồ 5 triệu là "cao".

### 4.3. Các mô hình

| Mô hình | Cách tính | File |
|---|---|---|
| **CBF** (lọc theo nội dung) | Hồ sơ `p_u` = trung bình có trọng số r(u,i) của các v_i; `Score_CBF = cos(p_u, v_i)` | `engine.py` |
| **CF / ALS** (lọc cộng tác) | Phân rã ma trận người dùng × sản phẩm theo phản hồi ngầm định: `c_ui = 1 + a·r(u,i)`, nghiệm đóng `x_u = (YᵀY + Yᵀ(C_u−I)Y + λI)⁻¹ YᵀC_u p_u`, luân phiên X và Y | `als.py` |
| **Hybrid** | Min-Max trên tập ứng viên, `Score = α·S'_CBF + (1−α)·S'_CF`, **α = 0.4** (chọn bằng đánh giá) | `engine.py` |

- **Fold-in:** khi có request, vector của khách được tính lại từ hành vi *hiện tại*, nên hành vi mới phát sinh sau lần huấn luyện gần nhất vẫn được tính.
- **Huấn luyện:** khoảng 3 giây (352 người dùng × 4.487 sản phẩm). Tự chạy lại mỗi 24 giờ; mô hình lưu ở volume `ai_models`.

### 4.4. Các bước tạo danh sách "Gợi ý dành cho bạn"

1. Lấy **K = 50** ứng viên tốt nhất từ CBF và 50 từ CF, rồi hợp nhất.
2. **Lọc:** bỏ sản phẩm bị ẩn, đã xoá, giá = 0, và sản phẩm khách **đã tương tác hoặc đã mua**.
3. Tính điểm Hybrid và sắp xếp giảm dần. Bằng điểm thì ưu tiên sản phẩm phổ biến hơn, rồi sản phẩm cập nhật gần hơn.
4. Lấy **Top-N** (mặc định 8) kèm **lý do đề xuất**:
   - *"Tương tự iPhone 16 bạn đã xem"* khi điểm chủ yếu đến từ CBF;
   - *"Khách hàng có sở thích giống bạn cũng quan tâm"* khi điểm chủ yếu đến từ CF.

**Cold-start** (Bảng 3.21 trong báo cáo):

| Trường hợp | Cách xử lý |
|---|---|
| Khách có ≥ 5 lượt tương tác | Hybrid α = 0.4 |
| Khách có 1–4 lượt tương tác | Chỉ dùng CBF (α = 1) |
| Khách mới / chưa đăng nhập | Khoảng ¾ sản phẩm phổ biến nhất + ¼ sản phẩm mới nhất |
| Sản phẩm mới chưa ai tương tác | Chỉ có điểm CBF |

### 4.5. Tương tự, Upsell, Cross-sell

| Loại | Điều kiện chọn | Xếp hạng | Ví dụ lý do |
|---|---|---|---|
| **SIMILAR** | Cùng danh mục, đang bán | Cosine giữa hai sản phẩm | "Cùng hãng Apple, cùng tầm giá" |
| **UPSELL** | Cùng danh mục, giá cao hơn tối đa **δ = 30%**, có ít nhất 1 thông số (RAM / bộ nhớ / camera) tốt hơn. Danh mục không có thông số (đồng hồ thời trang) chỉ xét giá | Hybrid nếu khách có dữ liệu, ngược lại theo độ giống sản phẩm đang xem | "Nâng cấp camera 50MP, chỉ thêm 3.000.000đ" |
| **CROSS_SELL** | Quan hệ `ACCESSORY / COMPATIBLE / BOUGHT_TOGETHER` trong `product_relations` + cặp mua chung có **lift > 1** | 0.6 × mức liên quan + 0.4 × Hybrid (khi đã đăng nhập) | "Phụ kiện phù hợp với sản phẩm này" |

Đơn hàng hiện còn ít nên chưa tính được "mua chung" đáng tin. Vì vậy `build_relations.py` tạo **6.086 quan hệ phụ kiện** theo quy tắc:

- **Điện thoại:** ốp lưng, kính cường lực, sạc, cáp, tai nghe.
- **Laptop:** chuột, bàn phím, balo, túi chống sốc.
- **Máy tính bảng:** Apple Pencil, bàn phím, sạc…
- **Đồng hồ thông minh:** sạc, tai nghe…

Mỗi thiết bị có tối đa 8 phụ kiện (tối đa 2 mỗi nhóm). Phụ kiện cùng hãng được ưu tiên. Phụ kiện chỉ dành cho iPhone (Lightning, MagSafe iPhone, Magic Mouse…) chỉ gắn với sản phẩm Apple.

### 4.6. Khi AI Service không trả lời (dự phòng)

Backend chờ tối đa 3 giây. Quá thời gian đó, Backend trả `source: "FALLBACK"`:

| Loại | Danh sách dự phòng |
|---|---|
| PERSONAL | Sản phẩm nhiều đánh giá nhất |
| SIMILAR | Cùng danh mục |
| UPSELL | Cùng danh mục, giá **cao hơn** sản phẩm đang xem |
| CROSS_SELL | Rỗng (khu "Mua kèm" tự ẩn) |

---

## 5. Chatbot hoạt động thế nào

### 5.1. Luồng một tin nhắn

```mermaid
sequenceDiagram
    participant K as Khách (widget)
    participant B as Spring Boot
    participant A as AI Service
    participant C as LLM (Ollama / Claude)
    K->>B: POST /api/v1/chat/stream {sessionToken, message} (+ JWT nếu đã đăng nhập)
    B->>B: giới hạn số lần (Redis), tạo/kiểm tra phiên, lưu tin nhắn khách
    B-->>K: SSE session {sessionToken}
    B->>A: POST /chat/stream {message, 12 tin gần nhất, accessToken}
    loop tối đa 6 vòng
        A->>C: câu hỏi + lịch sử + 11 tool (streaming)
        C-->>A: từng đoạn chữ
        A-->>B: SSE delta
        B-->>K: SSE delta (chữ hiện dần)
        C-->>A: yêu cầu gọi tool (vd. get_my_orders)
        A-->>K: SSE status "Đang xem đơn hàng của bạn..." (qua Backend)
        A->>B: GET /api/v1/orders (Bearer token của khách)
        B-->>A: dữ liệu của chính khách
    end
    A-->>B: SSE done {reply, productIds, actions, toolsUsed}
    B->>B: lưu câu trả lời, lấy thông tin sản phẩm
    B-->>K: SSE done {reply, products, actions}
```

### 5.2. RAG: kho tri thức

**Xây kho** (`scripts/build_index.py`), đi theo chuỗi: Thu thập → Làm sạch → Chunking → Embedding → pgvector.

| Loại chunk | Nguồn | Số lượng | Nội dung |
|---|---|---|---|
| `POLICY` | `knowledge/*.md`, mỗi mục `##` là một chunk | 15 | Giao hàng, thanh toán, trả góp, bảo hành, bảo trì, trả hàng, hỗ trợ |
| `PRODUCT` | Bảng `products` + thông số + phiên bản | 4.288 | Tên, danh mục, hãng, giá, bảo hành, phiên bản, tối đa 12 thông số |

- Nội dung chính sách viết **đúng theo code Backend**: phí giao hàng 30.000đ, miễn phí từ 10.000.000đ; trả góp 0% cho đơn từ 3.000.000đ, kỳ hạn 3/6/9/12 tháng; trả hàng trong 7 ngày…
- **Không đưa mô tả sản phẩm crawl vào chunk.** Phần mô tả toàn câu quảng cáo ("thu cũ đổi mới, trả góp 0%…"). Khi còn để mô tả, câu hỏi "đổi trả trong bao nhiêu ngày" trả về… đồng hồ Casio.
- Mỗi chunk lưu `metadata` (danh mục, hãng, giá) để lọc.
- Mô hình E5 cần tiền tố `"passage: "` cho tài liệu và `"query: "` cho câu hỏi.

**Truy xuất** (`app/chatbot/rag.py`), cấu hình bằng `RAG_MODE`:

| Cách tìm | Cách làm | Mạnh / yếu |
|---|---|---|
| **`vector`** (mặc định) | Embedding câu hỏi rồi lấy Top-K chunk có cosine lớn nhất (toán tử `<=>`, chỉ mục **HNSW**: tìm láng giềng gần đúng, không cần so với cả 4.303 vector) | Hiểu nghĩa ("máy pin trâu"); thực tế cũng tìm đúng mã máy |
| `keyword` | Full-text search của PostgreSQL trên cột `search_tsv`: bỏ dấu tiếng Việt (`unaccent`), tên sản phẩm trọng số A, nội dung trọng số D, bỏ từ dừng ("giá", "bao", "nhiêu"...), khớp tiền tố | Nhanh (10 ms), nhưng không hiểu từ đồng nghĩa và dễ xếp sai thứ tự |
| `hybrid` | Chạy cả hai rồi gộp bằng **Reciprocal Rank Fusion** có trọng số: `điểm = 1/(60+hạng_vector) + w/(60+hạng_từ_khoá)` (`RAG_KEYWORD_WEIGHT`) | Thử nghiệm; đo được là kém hơn `vector` (mục 9.4) |

- Tách theo loại tài liệu: câu hỏi chính sách chỉ tìm trong `POLICY`, lấy **K = 5** (`RAG_POLICY_K`); tìm sản phẩm chỉ trong `PRODUCT`, tối đa 8.
- Với sản phẩm: lọc theo danh mục, hãng, khoảng giá **trước** khi xếp hạng. Nhờ vậy "điện thoại chụp ảnh đẹp dưới 10 triệu" vừa hiểu đúng nghĩa vừa đúng điều kiện giá.
- Bỏ chunk có cosine dưới ngưỡng (0,8 chính sách, 0,78 sản phẩm), để LLM không trả lời dựa trên nội dung không liên quan.
- Phía LLM: `temperature = 0.2` để câu trả lời ổn định (không cần chỉnh top-k / top-p khi sinh chữ).

### 5.3. Function Calling: 11 tool (`app/chatbot/tools.py`)

| Tool | Cần đăng nhập | Lấy dữ liệu từ |
|---|---|---|
| `search_policies(query)` | | RAG, tài liệu `POLICY` |
| `search_products(query, category, brand, min_price, max_price, limit)` | | RAG, tài liệu `PRODUCT` + lọc metadata |
| `get_product_details(product_id)` | | `GET /products/{id}`, `/variants`, `/specifications` (giá **hiện tại**) |
| `recommend_products(type, product_id)` | | `POST /recommendations` (dùng lại hệ khuyến nghị) |
| `get_my_orders()` | ✅ | `GET /orders` |
| `get_order_detail(order)` | ✅ | `GET /orders/{id}` hoặc tìm theo mã `DH...` |
| `get_my_cart()` | ✅ | `GET /cart` |
| `get_my_service_requests()` | ✅ | `GET /service-requests/mine` |
| `find_stores(city)` | | `GET /stores` |
| `propose_add_to_cart(product_id \| product_name, variant_name, quantity)` | ✅ | `GET /products/{id}`, `/variants` → **đề xuất** thêm giỏ (mục 5.6); chỉ có tên thì tool tự tìm sản phẩm |
| `propose_cancel_order(order)` | ✅ | `GET /orders/...` → **đề xuất** huỷ đơn đang chờ xác nhận (mục 5.6) |

- **Chưa đăng nhập** mà hỏi đơn hàng: tool trả `LOGIN_REQUIRED` và LLM mời khách đăng nhập.
- **Hỏi đơn của người khác:** Backend trả 404, tool trả `NOT_FOUND`. Trường hợp này đã kiểm thử thật.
- Lỗi khác được đổi thành `TOOL_FAILED`; LLM không thấy chi tiết lỗi nội bộ.

### 5.4. Gọi LLM (`app/chatbot/agent.py`)

Vòng hội thoại viết một lần, chạy được với hai loại nhà cung cấp, chọn bằng `LLM_PROVIDER`:

| `LLM_PROVIDER` | Dùng cho | Thư viện | Chi phí |
|---|---|---|---|
| `openai` (**mặc định**) | Ollama chạy trên máy (mặc định `qwen3:8b`), hoặc API miễn phí khác có chuẩn OpenAI (Gemini, Groq...) | `openai`, Chat Completions + `tools`, `stream=True` | **0đ** với Ollama |
| `anthropic` | Claude (`claude-opus-5-5`, `claude-haiku-5-5`...) | `anthropic`, Messages API + tool use, streaming | Trả phí theo token |

- Hai lớp `OpenAICompatibleProvider` và `AnthropicProvider` cùng có các hàm: tạo tin nhắn đầu, **stream một lượt** (trả từng đoạn chữ, cuối cùng là danh sách tool cần gọi), và nối kết quả tool vào hội thoại theo đúng định dạng của từng bên. 11 tool được khai báo một lần rồi tự chuyển sang định dạng của từng nhà cung cấp.
- Vòng lặp tối đa 6 lượt. Mỗi đoạn chữ thành sự kiện SSE `delta`; trước khi chạy tool phát sự kiện `status` ("Đang tra cứu chính sách...").
- Nếu LLM dừng vì hết độ dài hoặc bị chặn thì không chạy tool của lượt đó. Nếu LLM sinh tham số tool không phải JSON hợp lệ, tool không chạy mà trả lỗi `INVALID_ARGUMENTS` để LLM thử lại.
- Qwen3 được gửi `reasoning_effort = none` (`LLM_REASONING_EFFORT`) để tắt bước "suy nghĩ": khoảng 40 giây xuống 2 giây mỗi lượt mà vẫn chọn đúng tool.
- **Chốt chặn cho mô hình nhỏ** (đo bằng bộ đánh giá, mục 9.3):
  - trạng thái đăng nhập được ghi trong **lời nhắc hệ thống**, không chèn vào tin nhắn khách (chèn vào tin nhắn làm Qwen quên gọi tool);
  - lượt đầu không gọi tool nào thì **nhắc lại một lần** "câu này cần tra cứu thì gọi tool ngay" và gửi sự kiện `reset` để xoá đoạn "để mình tra cứu..." đã hiện (Ollama bỏ qua `tool_choice="required"` nên phải làm bằng code);
  - có thao tác chờ xác nhận mà câu trả lời không nhắc bấm nút thì tự thêm dòng "👉 Bấm nút Xác nhận bên dưới";
  - câu trả lời chứa nguyên văn hướng dẫn nội bộ (bị prompt injection) thì thay bằng câu từ chối.
- Riêng với Claude: mức suy luận `AI_CHAT_EFFORT` (mặc định `low`), prompt caching cho phần lời nhắc + tool, và server-side fallback khi bị bộ lọc an toàn từ chối (tự tắt với Haiku).
- **Lời nhắc hệ thống** yêu cầu LLM:
  - trả lời tiếng Việt, ngắn gọn;
  - **chỉ dùng dữ liệu từ tool**, không bịa giá hay chính sách, kể cả với câu hỏi nối tiếp;
  - hiểu câu hỏi nối tiếp theo ngữ cảnh;
  - hỏi lại khi thiếu thông tin;
  - từ chối chủ đề ngoài mua sắm.
- Sản phẩm mà tool trả về **và** được nhắc tên trong câu trả lời sẽ được gửi kèm, để widget hiện thành thẻ có link.

### 5.5. Phiên chat và giao diện

- Backend tạo `session_token` ngẫu nhiên 32 ký tự. Trình duyệt lưu nó trong `localStorage`, nên **chuyển trang vẫn giữ cuộc trò chuyện** (`GET /chat/{token}/messages`).
- Phiên của khách chưa đăng nhập **được gắn vào tài khoản** khi khách đăng nhập rồi chat tiếp. Phiên của một tài khoản thì người khác không mở được.
- Backend gửi **12 tin nhắn gần nhất** làm ngữ cảnh; mỗi tin cắt còn tối đa 4.000 ký tự.
- **Giới hạn:** 60 tin/giờ cho mỗi tài khoản, 20 tin/giờ cho mỗi IP chưa đăng nhập (Redis).
- **Streaming:** widget gọi `POST /api/v1/chat/stream` và đọc luồng Server-Sent Events: `session` → nhiều `delta` / `status` (có thể có `reset`) → `done` (câu trả lời đã lưu + thẻ sản phẩm + nút thao tác). Lỗi kiểm tra (429, 404, 400) vẫn trả JSON trước khi luồng bắt đầu. Backend đọc luồng của AI Service bằng `RestClient.exchange` và ghi ra `StreamingResponseBody`, không giữ kết nối database trong lúc chờ.
- **AI ngừng hoặc LLM chưa chạy:** luồng kết thúc bằng sự kiện `error` (API thường `/chat` trả 503), widget tự trả lời theo từ khoá (chính sách cố định + tìm sản phẩm theo tên).
- **ADMIN:** trang *Lịch sử chatbot* xem mọi phiên (khách nào, câu hỏi đầu, số tin nhắn) và toàn bộ nội dung.

### 5.6. Thao tác có xác nhận (thêm giỏ, huỷ đơn)

Khách có thể nhờ trợ lý *"thêm iPhone 17e vào giỏ"* hoặc *"huỷ giúp đơn DH00000290"*. Để an toàn, **chatbot không bao giờ tự thực hiện**:

1. LLM gọi `propose_add_to_cart` / `propose_cancel_order`. Tool kiểm tra điều kiện bằng API của chính khách:
   - sản phẩm còn bán; nếu nhiều phiên bản mà khách chưa chọn thì trả danh sách để LLM hỏi lại;
   - đơn hàng thuộc tài khoản và đang `PENDING`.
2. Tool trả `WAITING_FOR_CUSTOMER_CONFIRMATION` và thêm một **action** `{type, label, variantId|orderId, quantity}`. Lời nhắc hệ thống cấm LLM nói "đã thêm / đã huỷ".
3. Widget hiện nút **"✓ Xác nhận: Thêm iPhone 17e 256GB (256 GB - Hồng nhạt) × 1 vào giỏ"**.
4. Chỉ khi khách bấm, trình duyệt mới gọi **đúng API thường** của khách: `POST /api/v1/cart/items` hoặc `POST /api/v1/orders/{id}/cancel`. LLM không nằm trên đường thực thi, nên dù mô hình bị lừa (prompt injection) cũng không thể tự đặt / huỷ gì.

---

## 6. API

### 6.1. API Backend (trình duyệt gọi). Xem đầy đủ ở Swagger: http://localhost:8080/swagger-ui/index.html

| Phương thức | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/v1/interactions` | Đăng nhập | `{productId, type: "VIEW"\|"CLICK"}` → 204 |
| POST | `/api/v1/recommendations` | Công khai (token tuỳ chọn) | `{type, productId?, size?}` → `{type, source, items:[{product, score, reason}]}` |
| POST | `/api/v1/admin/recommendations/train` | ADMIN | Huấn luyện lại ngay, trả thống kê |
| POST | `/api/v1/chat/stream` | Công khai (token tuỳ chọn) | `{sessionToken?, message}` → luồng SSE `session`, `delta`, `status`, `done {sessionToken, reply, products, actions}` hoặc `error` (widget dùng API này) |
| POST | `/api/v1/chat` | Công khai (token tuỳ chọn) | Như trên nhưng trả một lần: `{sessionToken, reply, products, actions}` |
| GET | `/api/v1/chat/{token}/messages` | Chủ phiên | Tải lại cuộc trò chuyện |
| GET | `/api/v1/admin/chat-sessions` | ADMIN | Danh sách phiên, mới hoạt động trước |
| GET | `/api/v1/admin/chat-sessions/{id}/messages` | ADMIN | Nội dung một phiên |

Ví dụ:

```bash
curl -X POST http://localhost:8080/api/v1/recommendations \
  -H "Content-Type: application/json" \
  -d '{"type":"UPSELL","productId":37,"size":3}'
```

```json
{"type":"UPSELL","source":"AI","items":[
  {"product":{"id":...,"name":"Samsung Galaxy S26 5G 12GB/256GB", "...":"..."},
   "score":0.992,"reason":"Nâng cấp camera 50MP, chỉ thêm 3.000.000đ"}]}
```

### 6.2. API nội bộ của AI Service (chỉ Backend gọi, cần `X-Internal-Key`). Tài liệu: http://localhost:8000/docs

| Phương thức | Endpoint | Mô tả |
|---|---|---|
| POST | `/recommend` | `{userId?, n, excludeProductIds}`: khuyến nghị cá nhân |
| POST | `/similar` | `{productId, n}`: sản phẩm tương tự |
| POST | `/upsell` | `{productId, userId?, n}` |
| POST | `/cross-sell` | `{productId, userId?, n}` |
| POST | `/train` | Huấn luyện lại |
| POST | `/chat` | `{message, history, accessToken?}` → `{reply, productIds, actions, toolsUsed, usage}` |
| POST | `/chat/stream` | Như `/chat` nhưng trả SSE: `delta`, `status`, `done` hoặc `error` |
| GET | `/health` | Trạng thái + thống kê lần huấn luyện gần nhất (không cần khoá) |

---

## 7. Cơ sở dữ liệu

File `database/migrations/ai_recommendation.sql` (chạy lại nhiều lần vẫn an toàn) thực hiện:

| Đối tượng | Thay đổi |
|---|---|
| `user_interactions` | `CHECK` loại hành vi ∈ {VIEW, CLICK, LIKE, ADD_TO_CART, PURCHASE}; index `(user_id, product_id, interaction_type, timestamp)` |
| `recommendations` | Index `(user_id, created_at)`; mỗi sản phẩm hiển thị cho khách đã đăng nhập được lưu kèm `score` và `reason` |
| `product_relations` | `UNIQUE(product_id, related_product_id, relation_type)`; loại quan hệ: `SIMILAR`, `ACCESSORY`, `COMPATIBLE`, `BOUGHT_TOGETHER` |
| `chat_sessions` | `user_id` cho phép NULL (khách chưa đăng nhập); thêm `session_token` (unique), `last_message_at` |
| **`kb_chunks`** (mới) | Kho tri thức RAG: `doc_type`, `source`, `product_id`, `title`, `content`, `metadata jsonb`, `embedding vector(384)`; chỉ mục HNSW cosine |
| Extension | `create extension vector` (pgvector) |

---

## 8. Cài đặt và chạy

### 8.1. Lần đầu (sau khi `git pull`)

**Bước 1. Cài LLM miễn phí (Ollama)** trên máy chạy Docker. Không làm bước này thì khuyến nghị vẫn chạy, chỉ chatbot ở chế độ từ khoá.

- **Windows:** tải bộ cài ở https://ollama.com/download rồi cài (Ollama tự chạy nền).
- **macOS:** `brew install ollama` rồi `brew services start ollama` (hoặc tải app ở ollama.com).
- Tải mô hình (khoảng 5.2 GB, một lần):
  ```powershell
  ollama pull qwen3:8b
  ```
- Kiểm tra: mở http://localhost:11434 thấy chữ *Ollama is running*.
- Cần máy RAM ≥ 16 GB (hoặc GPU ≥ 8 GB). Máy yếu hơn dùng `qwen3:4b` (nhẹ hơn, trả lời kém hơn): `ollama pull qwen3:4b` và đặt `LLM_MODEL=qwen3:4b` trong `.env`.
- Không cần sửa `.env`: docker-compose đã mặc định gọi Ollama ở `http://host.docker.internal:11434/v1`.

**Bước 2.** Chạy migration cho database đang có (PowerShell):
```powershell
Get-Content database\migrations\ai_recommendation.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
```

**Bước 3.** Build và chạy:
```powershell
docker compose up -d --build
```
- Image Postgres đổi sang `pgvector/pgvector:pg16-trixie`: vẫn PostgreSQL 16, **cùng volume, không mất dữ liệu**. Bản *trixie* dùng cùng thư viện glibc với image `postgres:16` cũ, nên không lệch collation.
- Lần đầu build `ai-service` mất vài phút (tải PyTorch CPU + mô hình embedding).
- Nếu bước 3 chạy trước bước 2 thì chạy lại migration rồi `docker compose restart ai-service backend`.

**Bước 4.** Chuẩn bị dữ liệu AI. Mỗi lệnh chạy lại được nhiều lần:
```powershell
docker compose exec ai-service python -m scripts.build_relations                 # quan hệ phụ kiện (~6.000 dòng)
docker compose exec ai-service python -m scripts.seed_interactions --users 300   # hành vi MÔ PHỎNG (chỉ local)
docker compose exec ai-service python -m scripts.build_index                     # kho tri thức RAG (~2 phút)
docker compose exec ai-service python -m scripts.evaluate                        # in bảng đánh giá
docker compose exec ai-service python -c "from app.recommender.engine import recommender; print(recommender.train())"
docker compose restart ai-service                                                # nạp mô hình vừa huấn luyện
```

**Bước 5.** Kiểm tra:
- http://localhost:8000/health phải có `"status":"UP"` và `"interactions"` > 0.
- Mở http://localhost:5510/customer/recommendation.html, rồi bấm 💬 hỏi thử *"Điện thoại chụp ảnh đẹp dưới 10 triệu?"*

### 8.2. Biến môi trường

| Biến (`.env`) | Mặc định | Ý nghĩa |
|---|---|---|
| `LLM_PROVIDER` | `openai` | `openai` = API kiểu OpenAI (Ollama, Gemini, Groq...); `anthropic` = Claude |
| `LLM_BASE_URL` | `http://host.docker.internal:11434/v1` | Địa chỉ API (mặc định Ollama trên máy host) |
| `LLM_API_KEY` | `ollama` | Khoá API (Ollama không cần khoá thật) |
| `LLM_MODEL` | `qwen3:8b` | Tên mô hình |
| `RAG_MODE`, `RAG_POLICY_K`, `RAG_KEYWORD_WEIGHT` | `vector`, `5`, `1.0` | Cách tìm trong kho tri thức (`vector` / `keyword` / `hybrid`), số chunk chính sách, trọng số từ khoá khi `hybrid` |
| `LLM_REASONING_EFFORT` | `none` với qwen3, trống với mô hình khác | Gửi kèm `reasoning_effort`; `none` tắt bước "suy nghĩ" của Qwen3 (≈ 40 giây → 2 giây mỗi lượt) |
| `ANTHROPIC_API_KEY` | (trống) | Chỉ cần khi `LLM_PROVIDER=anthropic` |
| `ANTHROPIC_MODEL` | `claude-opus-5-5` | Mô hình Claude (rẻ: `claude-haiku-5-5`) |
| `AI_CHAT_EFFORT` | `low` | Mức suy luận (`low` / `medium` / `high`) |
| `AI_INTERNAL_KEY` | `dev-internal-key` | Khoá chung Backend ↔ AI Service; **đổi khi triển khai thật** |
| `REC_ALPHA`, `REC_COLD_START_K` | `0.4`, `5` | α của Hybrid, ngưỡng k cold-start |
| `ALS_FACTORS`, `ALS_REGULARIZATION`, `ALS_CONFIDENCE`, `ALS_ITERATIONS` | `32`, `0.1`, `10`, `15` | Siêu tham số ALS |

### 8.3. Dùng hằng ngày

| Việc | Lệnh |
|---|---|
| Sửa code AI Service | `docker compose up -d --build ai-service` |
| Xem log | `docker compose logs -f ai-service` |
| Huấn luyện lại ngay | `POST /api/v1/admin/recommendations/train` (Swagger, đăng nhập ADMIN), hoặc gọi thẳng trong container: `docker compose exec ai-service python -c "from app.recommender.engine import recommender; print(recommender.train())"` rồi `docker compose restart ai-service` |
| Sửa chính sách trong `knowledge/*.md` hoặc catalogue thay đổi | `docker compose exec ai-service python -m scripts.build_index` |
| Xoá dữ liệu mô phỏng | `docker compose exec ai-service python -m scripts.seed_interactions --reset` |

### 8.4. Chạy AI Service ngoài Docker (để debug)

```bash
cd AI_Service
uv venv .venv --python 3.11 && uv pip install --python .venv/bin/python -r requirements.txt
export AI_DATABASE_URL=postgresql://postgres:postgres@localhost:5432/techshopping
export AI_BACKEND_URL=http://localhost:8080/api/v1 AI_INTERNAL_KEY=dev-internal-key
.venv/bin/uvicorn app.main:app --reload --port 8001
```

---

## 9. Kiểm thử và kết quả đánh giá

### 9.1. Đánh giá offline (`scripts/evaluate.py`)

Cách đánh giá:

- Với mỗi người dùng, 80% hành vi đầu (theo thời gian) dùng để huấn luyện, 20% cuối dùng để kiểm thử.
- "Đúng" nghĩa là sản phẩm khách tương tác trong phần kiểm thử mà chưa từng gặp khi huấn luyện.
- Dữ liệu mô phỏng: 285 người dùng, 16.900 hành vi huấn luyện, 4.317 hành vi kiểm thử, N = 10.

| Mô hình | Precision@10 | Recall@10 | NDCG@10 | HitRate@10 | Coverage |
|---|---|---|---|---|---|
| Phổ biến (đường cơ sở) | 0.0081 | 0.0223 | 0.0153 | 0.0772 | 0.4% |
| CBF (α = 1) | 0.0491 | 0.1327 | 0.1033 | 0.3684 | 18.1% |
| CF / ALS (α = 0) | 0.0488 | 0.1360 | 0.1079 | 0.3439 | 14.8% |
| Hybrid α = 0.3 | 0.0618 | 0.1779 | 0.1374 | 0.4246 | 15.6% |
| **Hybrid α = 0.4** | **0.0621** | **0.1798** | **0.1389** | **0.4316** | 15.9% |
| Hybrid α = 0.5 | 0.0611 | 0.1745 | 0.1368 | 0.4211 | 16.3% |

Hybrid tốt hơn từng mô hình riêng lẻ khoảng 30% NDCG, nên chọn **α = 0.4**.

> ⚠️ **Dữ liệu hành vi là mô phỏng.** Hệ thống mới chạy nên chưa có hành vi thật. `seed_interactions.py` tạo 300 khách mô phỏng có "hồ sơ sở thích": danh mục, hãng (35% trung thành với một hệ sinh thái như Apple/Xiaomi), tầm giá, kèm 10% hành vi ngẫu nhiên. Các con số trên chỉ dùng để **so sánh tương đối giữa các mô hình**; báo cáo đã ghi rõ điều này.

### 9.2. Test tự động (Backend)

- 15 unit test mới:
  - `InteractionRecorderTest`: trọng số, chặn lặp, chặn PURCHASE từ trình duyệt;
  - `RecommendationServiceTest`: giữ thứ tự AI, bỏ sản phẩm ẩn, dự phòng Upsell đắt hơn hẳn;
  - `ChatServiceTest`: lưu tin, chặn phiên người khác, gắn phiên khách sau đăng nhập, cắt lịch sử dài, AI ngừng → 503.
- `ChatServiceTest` có thêm 2 test cho streaming: chuyển tiếp `delta`/`status`, kết thúc bằng `done` có thao tác; AI ngừng → sự kiện `error`.
- Toàn bộ **663 test** backend đều đạt.
- Test tích hợp cần database `techshopping_test`. Nếu máy có Postgres khác chiếm `localhost:5432`, chạy:
  ```bash
  SPRING_DATASOURCE_URL=jdbc:postgresql://<IP-máy>:5432/techshopping_test SPRING_DATA_REDIS_HOST=<IP-máy> ./mvnw test
  ```

### 9.3. Đánh giá chatbot tự động (`scripts/eval_chatbot.py`)

Bộ **30 câu hỏi** trong `evals/chatbot_cases.json`, chia 7 nhóm: RAG chính sách (9), Sản phẩm (5), Khuyến nghị (2), Dữ liệu cá nhân (6), Thao tác có xác nhận (4), Cửa hàng (1), An toàn (3: câu ngoài phạm vi, prompt injection, chính sách không tồn tại).

Mỗi câu chạy qua **luồng thật** (LLM + RAG + tool + API Backend) rồi được chấm theo bốn tiêu chí:

| Tiêu chí | Kiểm tra |
|---|---|
| Tool | Các tool bắt buộc có được gọi (vd. hỏi đơn hàng thì phải gọi `get_my_orders`) |
| Nội dung | Có cụm từ bắt buộc ("7 ngày", "30.000"...), không có cụm từ cấm ("đã huỷ đơn"), có đúng **giá hiện tại** lấy từ Backend |
| Thao tác | Đề xuất đúng loại khi được nhờ; **không** đề xuất khi khách chỉ hỏi |
| Giám khảo LLM | Một lần gọi LLM riêng (trả JSON `{pass, reason}`) chấm theo tiêu chí của câu và kiểm tra **mọi con số / chính sách trong câu trả lời đều có trong kết quả tool** (không bịa) |

```powershell
docker compose exec ai-service python -m scripts.eval_chatbot --dry-run    # kiểm tra bộ câu hỏi, không tốn phí
docker compose exec ai-service python -m scripts.eval_chatbot              # chạy thật (~5 phút với Ollama, miễn phí)
docker compose exec ai-service python -m scripts.eval_chatbot --only act-cancel --no-judge
```

**Kết quả đã đo** (13/10/2026, M1 Pro 16 GB, Ollama). Qua 4 lần đo, bộ đánh giá vừa so sánh mô hình vừa chỉ ra chỗ cần sửa:

| Cấu hình | Đạt (tự động) | Đạt (có giám khảo) | Thời gian / câu |
|---|---|---|---|
| Qwen 2.5 7B, ghi chú đăng nhập chèn vào tin nhắn | 15/30 (50%) | | 6,2 s |
| Qwen 2.5 7B, ghi chú trong lời nhắc hệ thống | 19/30 (63%) | | 6,3 s |
| Qwen 2.5 7B + nhắc gọi tool + chặn rò rỉ | 21/30 (70%) | | 7,8 s |
| **Qwen3 8B** (`reasoning_effort=none`) + các chốt chặn | **28/30 (93%)** | **27/30 (90%)** | 8,3 s |

Với Qwen3 8B có giám khảo:
- **Đạt 100%:** chính sách, dữ liệu cá nhân, cửa hàng, an toàn.
- **3 câu trượt do giám khảo bắt được:**
  - liệt kê thêm các bản iPhone khác ngoài bản Pro khách hỏi;
  - gợi ý phụ kiện chung chung, không nêu sản phẩm cụ thể;
  - **bịa một màu iPhone 17e không có trong dữ liệu**.

Kết quả nằm ở `AI_Service/evals/results/latest.md` (bảng tỉ lệ đạt theo nhóm, thời gian trả lời, chi phí, danh sách câu trượt kèm lý do), có thể dán thẳng vào mục 4.8.4 của báo cáo. Bản chi tiết từng câu (câu trả lời, tool đã gọi, token) nằm ở `evals/results/<thời gian>.json`. Logic chấm điểm đã được kiểm tra bằng agent giả: bắt đúng thiếu tool, thiếu / thừa cụm từ, sai giá, đề xuất thao tác thừa.

### 9.4. Đánh giá phần tìm kiếm RAG (`scripts/eval_retrieval.py`)

Đo riêng bước tìm kiếm, **không gọi LLM**, chạy vài giây:

- **Bộ dữ liệu:** 40 câu hỏi có đáp án trong `evals/retrieval_cases.json`:
  - 15 câu chính sách;
  - 17 câu gõ đúng tên / mã máy ("Galaxy S26+", "MTJV3", "GA-2100-1A1DR");
  - 8 câu mô tả theo nhu cầu ("chuột không dây để chơi game").
- **Chỉ số:**
  - **Hit@K**: có ít nhất một chunk đúng trong K kết quả đầu;
  - **Recall@K**: tỉ lệ chunk đúng tìm được trong K kết quả đầu;
  - **MRR**: trung bình của 1 / vị trí chunk đúng đầu tiên.

```powershell
docker compose exec ai-service python -m scripts.eval_retrieval     # kết quả: evals/results/retrieval_latest.md
```

| Cách tìm | Hit@1 | Hit@3 | Hit@5 | Hit@8 | MRR | ms / câu |
|---|---|---|---|---|---|---|
| **vector** | **0,850** | **0,925** | **0,950** | **1,000** | **0,896** | 35 |
| keyword | 0,675 | 0,800 | 0,850 | 0,925 | 0,747 | 10 |
| hybrid (w = 1) | 0,750 | 0,875 | 0,925 | 0,950 | 0,824 | 37 |
| hybrid (w = 0,5) | 0,750 | 0,875 | 0,950 | 0,950 | 0,825 | 35 |
| hybrid (w = 0,25) | 0,800 | 0,900 | 0,950 | 0,950 | 0,857 | 35 |

**Kết luận:**
- **Tìm kiếm lai không tốt hơn**: ở mọi trọng số đều kém `vector`; trọng số từ khoá càng nhỏ thì càng gần `vector`.
- Lý do: mô hình e5 đã tìm đúng **100% câu gõ đúng tên / mã máy**, nên phần từ khoá chỉ thêm nhiễu (ví dụ "Galaxy S26+" lại xếp bản Ultra lên đầu). Vì vậy mặc định là `vector`, còn `hybrid` được giữ làm tuỳ chọn.
- **Chọn K:** với câu hỏi chính sách, Hit@3 = 0,867, Hit@5 = 0,933, Hit@8 = 1,0. Chunk chính sách ngắn nên tăng K từ 3 lên **5** gần như không tốn thêm. Chạy lại bộ đánh giá chatbot sau thay đổi này vẫn đạt 9/9 chính sách và 27/30 tổng.
- Điểm yếu còn lại nằm ở câu mô tả theo nhu cầu (Hit@1 = 0,62). Ví dụ "chuột không dây chơi game" ra chuột **có dây**, "đồng hồ Apple cho thể thao mạo hiểm" ra Series 11 thay vì Ultra. Hướng cải thiện tiếp là **xếp hạng lại bằng cross-encoder** (chưa làm).
- Trong quá trình làm, bộ đánh giá còn phát hiện chính **đáp án của bộ test bị thiếu**: câu "sạc dự phòng 20000mAh" ban đầu chỉ đánh dấu 3 sản phẩm, trong khi catalogue có 6 sản phẩm như vậy. Đáp án đã được bổ sung.

### 9.5. Đã kiểm thử thủ công trên hệ thống chạy thật

| Trường hợp | Kết quả |
|---|---|
| Khách chưa đăng nhập / đã đăng nhập xem khuyến nghị | Phổ biến + mới / danh sách cá nhân hoá, có lý do |
| SIMILAR, UPSELL, CROSS_SELL cho iPhone 17e | iPhone cùng tầm giá; Galaxy S26 (+3 triệu, camera tốt hơn); AirPods, ốp MagSafe, cáp, sạc |
| Bấm 3 lần cùng sản phẩm | Chỉ 1 dòng CLICK |
| Thêm vào giỏ | Ghi ADD_TO_CART (điểm 4) |
| Trình duyệt gửi PURCHASE | 400 |
| Gọi AI Service không có `X-Internal-Key` | 401 |
| Tắt AI Service | `source: FALLBACK`, trang vẫn có sản phẩm |
| RAG: "đổi trả mấy ngày", "trả góp cần gì", "phí ship", "máy hư bảo hành ở đâu" | Đúng chunk ở vị trí 1 (độ tương đồng 0.86–0.90) |
| Tool đọc đơn hàng của khách khác | `NOT_FOUND` |
| Tool đơn hàng / giỏ hàng khi chưa đăng nhập | `LOGIN_REQUIRED` |
| Tool đề xuất thêm giỏ / huỷ đơn của chính khách | Tạo action + nút Xác nhận; đơn của khách khác → `NOT_FOUND`; chưa đăng nhập → `LOGIN_REQUIRED` |
| Luồng SSE Backend → AI khi chưa có khoá | `session` rồi `error`; widget chuyển sang chế độ từ khoá |
| Hội thoại hoàn chỉnh qua Backend → AI Service → Ollama (streaming) | Đạt: `session` → `status` → 49 `delta` → `done` có nút Xác nhận + thẻ sản phẩm; 12–20 s / câu khi đã nạp mô hình (lần đầu ~40 s) |
| Hội thoại với Claude | Chưa chạy (không có `ANTHROPIC_API_KEY`); cùng code, đổi bằng `LLM_PROVIDER=anthropic` |

---

## 10. Bảo mật và chi phí

- **Khoá API** chỉ nằm trong `.env` (đã nằm trong `.gitignore`) và trong container `ai-service`; không bao giờ gửi xuống trình duyệt.
- **AI Service** chỉ mở cổng 8000 trên `127.0.0.1` để thử `/docs`. Request nghiệp vụ phải có `X-Internal-Key`.
- **Không tin dữ liệu từ LLM:** LLM không truyền `user_id`. Mọi tool dùng token của khách, nên Backend tự quyết ai được xem gì.
- **LLM không thực thi thao tác ghi:** thêm giỏ / huỷ đơn chỉ chạy khi khách bấm nút Xác nhận, bằng API thường của khách.
- **Phiên chat** dùng token ngẫu nhiên, không dùng id tăng dần (tránh đoán phiên của người khác).
- **Chi phí:** mặc định **0đ** (Ollama chạy trên máy). Nếu chuyển sang Claude thì mỗi tin nhắn là 1 đến vài lần gọi có tính phí. Cách hạn chế:
  - giới hạn 60 tin/giờ/tài khoản, 20 tin/giờ/IP khách;
  - effort mặc định `low`;
  - prompt caching cho phần cố định;
  - có thể đổi `ANTHROPIC_MODEL` sang Sonnet/Haiku để rẻ hơn.
- **Khuyến nghị, Embedding và (mặc định) LLM đều chạy cục bộ**, không tốn phí và không gửi dữ liệu khách ra ngoài.

---

## 11. Giới hạn hiện tại

- Dữ liệu hành vi để huấn luyện / đánh giá là **mô phỏng**. Khi có khách thật, hành vi được ghi tự động và mô hình tự học lại mỗi ngày.
- Sản phẩm thêm mới chỉ được gợi ý sau lần huấn luyện kế tiếp (tối đa 24 giờ, hoặc huấn luyện ngay bằng API ADMIN).
- Kho tri thức RAG không tự cập nhật khi đổi giá / thêm sản phẩm: cần chạy lại `build_index`. Riêng `get_product_details` luôn lấy giá **hiện tại** từ Backend.
- "Còn hàng" không được lọc khi gợi ý, vì tồn kho tính theo từng chi nhánh và chỉ kiểm tra khi nhân viên xác nhận đơn.
- `LIKE` chưa có chức năng tương ứng (chưa có danh sách yêu thích).
- Cross-sell "mua chung" (lift) chưa có tác dụng vì đơn mẫu mỗi đơn chỉ 1 sản phẩm; hiện dựa vào quan hệ phụ kiện theo quy tắc.

---

## 12. Gặp lỗi thì làm gì

| Hiện tượng | Nguyên nhân / cách xử lý |
|---|---|
| Chatbot chỉ trả lời theo từ khoá ("trợ lý AI đang tạm ngưng") | Ollama chưa chạy hoặc chưa tải mô hình: mở http://localhost:11434, chạy `ollama list` (phải có `qwen3:8b`). Nếu dùng Claude: sai `ANTHROPIC_API_KEY` / hết credit. Xem `docker compose logs ai-service` |
| Chatbot trả lời rất chậm (> 30 giây) | Máy thiếu RAM / không có GPU. Dùng `LLM_MODEL=qwen3:4b`, hoặc chuyển sang API miễn phí trên mạng (Gemini / Groq) bằng `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL` |
| Khuyến nghị luôn `source: FALLBACK` | `ai-service` chưa chạy hoặc đang khởi động; kiểm tra http://localhost:8000/health |
| Log backend: `missing table [kb_chunks]` / `missing column [session_token]` | Chưa chạy migration `ai_recommendation.sql` |
| `extension "vector" is not available` | Postgres vẫn là image cũ: `docker compose up -d postgres` để đổi sang `pgvector/pgvector:pg16-trixie` |
| Cảnh báo *collation version mismatch* | Dùng nhầm tag `pg16` (bookworm). Phải là `pg16-trixie` |
| "Gợi ý dành cho bạn" toàn sản phẩm phổ biến | Khách chưa có hành vi, hoặc chưa chạy `seed_interactions` + restart `ai-service` |
| Chatbot không tìm được chính sách / sản phẩm | Bảng `kb_chunks` trống: chạy `scripts.build_index` |
| Script trên máy local báo `role "postgres" does not exist` | Máy có Postgres khác (Homebrew) chiếm `localhost:5432`; dùng IP LAN của máy hoặc chạy script trong container (`docker compose exec ai-service ...`) |
