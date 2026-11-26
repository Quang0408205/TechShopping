# Phần AI của TechShopping: Khuyến nghị sản phẩm + Chatbot

Tài liệu này giải thích **mọi thứ đã thêm vào dự án cho phần AI**: dùng công nghệ gì, file nào làm gì, thuật toán chạy ra sao, cách cài đặt, chạy, kiểm thử.

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
13. [Học các công thức: video và tài liệu](#13-học-các-công-thức-video-và-tài-liệu)

---

## Chỉ số mới nhất (tóm tắt, chạy lại toàn bộ ngày 27/11/2026)

Ngày 27/11/2026 chạy lại **toàn bộ** phép đo trên code hiện tại (commit `977819e`) và database hiện tại (đã dựng lại,
**khác dữ liệu ngày 09/11**, nên không so trực tiếp với các số trước đó). Chi tiết và cách đo xem mục 9; file gốc ở
`evals/results/`.

- Khuyến nghị, mua kèm, nâng cấp: ra **y hệt** lần 26/11 (cùng dữ liệu, cùng code, cùng hạt giống ngẫu nhiên).
- **Kho tri thức RAG được dựng lại** (`scripts.build_index`). Lý do: bảng `kb_chunks` trong database là bản cũ, không khớp
  project. Chunk "Phí giao hàng" vẫn ghi chính sách cũ "đơn từ 10.000.000đ miễn phí giao hàng" (trong khi
  `knowledge/giao-hang.md` đã đổi từ 01/11), và còn 1 chunk của sản phẩm đã xoá ngày 08/11 (4.303 chunk sản phẩm thay
  vì 4.302). Vì vậy câu `pol-shipping-fee` trượt ở các lần 25/11 và 27/11 (trước khi dựng lại): chatbot nêu đúng chunk
  cũ, nhưng chunk đó sai so với chính sách hiện tại. Sau khi dựng lại, kho có 4.317 chunk (15 chính sách + 4.302 sản
  phẩm). RAG và chatbot dưới đây là số **sau khi dựng lại**.

| Phần | Chỉ số | Giá trị | Nguồn |
|---|---|---|---|
| Khuyến nghị (dữ liệu mô phỏng, 548 khách, N = 10) | NDCG@10 **cấu hình đang chạy** (Hybrid α = 0,6 + MMR λ = 0,7) | **0,0801** (KTC 95%: 0,0656–0,0958) | `recommender_latest.md` |
| | Precision / Recall / HitRate @10 | 0,0310 / 0,0965 / 0,2336 | như trên |
| | Coverage / Diversity | 19,1% / 0,515 | như trên |
| | Mô hình **chọn trên validation lần này** (Hybrid α = 0,4, không xếp lại) | NDCG@10 **0,0878** (0,0723–0,1030), P 0,0356, R 0,1110, HitRate 0,2500; thêm MMR λ = 0,8: 0,0817 | như trên |
| | NDCG@10 các đối chứng: phổ biến / Item-kNN / CBF / ALS | 0,0231 / 0,0596 / 0,0656 / 0,0742 | như trên |
| | Hybrid α = 0,4 hơn ALS, CBF, kNN, phổ biến | đều có ý nghĩa (+0,0135 / +0,0221 / +0,0281 / +0,0647) | như trên |
| | Tham số chọn trên validation | ALS 32 chiều λ = 0,1 a = 5; α = 0,4; nửa đời 90 ngày; MMR λ = 0,8; k = 1 (cấu hình đang chạy vẫn là của 09/11: ALS 16, α = 0,6, MMR 0,7, k = 2) | như trên |
| Khuyến nghị (Amazon Reviews 2023, 20.000 khách thật) | NDCG@10 Hybrid α = 0,2 | **0,0280** (0,0262–0,0297); hơn CBF, kNN, phổ biến có ý nghĩa, ngang ALS (0,0274) | `amazon_cell_latest.md` (**09/11**, không chạy lại: cần tải 4 GB dữ liệu và ~65 phút) |
| | Mua kèm HitRate@4 | 2,16% (bán chạy nhất: 1,55%; hơn có ý nghĩa) | như trên |
| Mua kèm (dữ liệu mô phỏng) | HitRate@4 | 3,02% so với bán chạy nhất 6,03% (chênh −3,02 điểm %, chưa đủ chắc chắn; đơn mẫu sinh ngẫu nhiên); 51% sản phẩm có danh sách mua kèm | `recommender_latest.md` |
| Nâng cấp | Sản phẩm có gợi ý / mức đắt hơn | 97,7% / +10% | `recommender_latest.md` |
| RAG (40 câu, kho đã dựng lại) | Hit@1 / Hit@3 / MRR của `vector` (đang dùng) | **0,850 / 0,925 / 0,899**; keyword 0,700 / 0,825 / 0,765 | `retrieval_latest.md` |
| Chatbot (30 câu, Qwen3 8B + giám khảo) | Đạt | **28/30 (93%)**, 27,1 giây / câu, 2,0 lần gọi LLM / câu, chi phí 0đ (trước khi dựng lại kho: 27/30, 30,7 giây) | `latest.md` = `20261127-0205.json` |
| Test | pytest AI Service | **84 đạt** | |
| | JUnit Backend | **903 đạt** (133 lớp; database kiểm thử phải chạy migration `user_preferences.sql`, mục 9.3) | |

Lưu ý khi trích dẫn:
- Các con số khuyến nghị trên dữ liệu mô phỏng chỉ có ý nghĩa **so sánh tương đối** giữa các mô hình (mục 9.1).
- Bộ lọc ốp lưng / kính theo đời máy chỉ bật khi hiển thị, không bật trong đánh giá offline (mục 4.4, 11).
- Chatbot 28/30: 2 câu trượt (`acc-order-code`, `act-cancel`) do **dữ liệu**: database dựng lại không còn đơn DH00003119 và
  DH00000221 thuộc tài khoản kiểm thử, nên chatbot trả "không tồn tại hoặc không thuộc tài khoản của bạn" (đúng hành vi,
  sai kỳ vọng của câu hỏi). Mục 9.4 có chi tiết.
- Amazon vẫn là số 09/11 (không chạy lại: cần tải 4 GB dữ liệu và ~65 phút). Mọi số còn lại đo ngày 27/11/2026. Nếu đổi
  dữ liệu, chính sách hoặc mô hình thì chạy lại theo mục 9 (đổi chính sách / catalogue thì chạy `scripts.build_index`
  trước) rồi cập nhật bảng này.

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
| **Spring `RestClient`** | Gọi AI Service (timeout 3 giây cho khuyến nghị, 240 giây cho chatbot) |
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
│   ├── main.py              FastAPI: /recommend /similar /upsell /cross-sell /train /evaluation /chat /chat/stream /health
│   ├── config.py            Đọc biến môi trường + tham số mô hình (α, k, K, δ, ALS...)
│   ├── db.py                Kết nối PostgreSQL (mặc định chỉ đọc)
│   ├── recommender/
│   │   ├── data.py          Đọc sản phẩm, thông số, tương tác, đơn hàng, quan hệ sản phẩm
│   │   ├── features.py      Trích thông số (RAM, bộ nhớ, màn hình, camera), Min-Max, One-Hot, điểm r(u,i)
│   │   ├── als.py           Thuật toán ALS cho phản hồi ngầm định + fold-in
│   │   ├── engine.py        CBF, Hybrid, cold-start, lọc, xếp hạng, Upsell, Cross-sell, lý do đề xuất
│   │   ├── compat.py        Luật phụ kiện vừa máy (đời máy, cổng sạc, Apple), thiết bị cùng hãng cho mua kèm
│   │   └── evaluation.py    Đánh giá offline (Precision / Recall / NDCG / HitRate / Coverage theo α), ghi kết quả
│   └── chatbot/
│       ├── rag.py           Embedding + tìm kiếm vector trong pgvector (có lọc metadata)
│       ├── tools.py         11 function cho Function Calling (gọi API Backend bằng token của khách)
│       └── agent.py         Vòng hội thoại với LLM (streaming): hỏi → gọi tool → trả lời; 2 nhà cung cấp: OpenAI-compatible (Ollama) / Claude
├── evals/
│   ├── chatbot_cases.json   30 câu hỏi kiểm thử chatbot kèm đáp án mong đợi
│   ├── retrieval_cases.json 40 câu hỏi có đáp án để đánh giá phần tìm kiếm RAG
│   └── results/             Kết quả mỗi lần chạy eval_chatbot.py (latest.md để dán vào báo cáo);
│                            recommender_latest.md / .csv / .json + recommender_alpha.svg: đánh giá khuyến nghị
├── knowledge/               Tài liệu chính sách cho RAG (Markdown)
│   ├── giao-hang.md, thanh-toan.md, bao-hanh-tra-hang.md, ho-tro.md
└── scripts/
    ├── seed_interactions.py Sinh dữ liệu hành vi MÔ PHỎNG (chỉ dùng local)
    ├── build_relations.py   Tạo quan hệ phụ kiện (ACCESSORY) cho "Mua kèm"
    ├── build_index.py       Xây kho tri thức RAG (chunk → embedding → kb_chunks)
    ├── evaluate.py          Đánh giá khuyến nghị (gọi app/recommender/evaluation.py), in bảng + ghi evals/results/
    ├── eval_amazon.py       Cùng quy trình đánh giá, chạy trên dữ liệu mua hàng thật Amazon Reviews 2023 (mục 9.1c)
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
| `entity/recommendation/UserPreference.java`, `PriceLevel.java`, `repository/recommendation/UserPreferenceRepository.java` | Mới (09/11) | Bảng `user_preferences`: câu trả lời bảng hỏi sở thích |
| `service/recommendation/PreferenceService.java`, `controller/recommendation/PreferenceController.java` | Mới (09/11) | Lựa chọn của bảng hỏi; đọc / lưu câu trả lời (kiểm tra mã danh mục, hãng, nhu cầu thuộc danh mục đã chọn); `RecommendationService` gửi câu trả lời sang AI cho danh sách PERSONAL |
| Test: `PreferenceApiIntegrationTest` + 3 test trong `RecommendationServiceTest` | Mới (09/11) | Lựa chọn công khai, lưu / thay câu trả lời, từ chối câu trả lời sai, cờ "đã có hành vi"; câu trả lời của tài khoản / trình duyệt được gửi sang AI |

### 3.3. Frontend (`frontend/`)

| File | Thay đổi |
|---|---|
| `js/core/api.js` | `trackInteraction()` + tự gửi `CLICK` khi bấm vào thẻ sản phẩm bất kỳ |
| `js/core/ui.js` | `fetchRecommendations()`, `renderRecommendationGrid()` (thẻ sản phẩm + dòng **lý do gợi ý**) |
| `js/customer/recommendation.js` | 5 khu của trang Khuyến nghị lấy dữ liệu từ AI thay cho quy tắc tạm |
| `customer/preferences.html`, `js/customer/preferences.js`, `css/customer/preferences.css` | Trang "Sở thích mua sắm" (bảng hỏi 4 câu); `js/core/api.js`: lưu câu trả lời của khách chưa đăng nhập, `pageAfterLogin()` chuyển khách mới tới bảng hỏi sau đăng ký / đăng nhập |
| `js/home.js`, `index.html` | Khu gợi ý ở trang chủ (ngay sau Flash sale) lấy 8 gợi ý PERSONAL từ AI khi khu này sắp vào màn hình: đã đăng nhập → "Gợi ý dành cho bạn" theo lịch sử, kèm lý do; chưa đăng nhập hoặc AI ngừng → đổi tiêu đề thành "Được quan tâm nhiều"; lỗi hoặc rỗng thì ẩn cả khu |
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
| `LIKE` | Backend tự ghi | Thêm vào danh sách yêu thích (bấm trái tim); bỏ khỏi danh sách không xoá hành vi đã ghi | 3 |
| `ADD_TO_CART` | Backend tự ghi | Thêm vào giỏ hàng | 4 |
| `PURCHASE` | Backend tự ghi | Đặt hàng | 5 |

- Chỉ ghi khi khách đã đăng nhập.
- Trình duyệt chỉ được gửi `VIEW` và `CLICK`, nên không thể tự khai là "đã mua".
- Cùng một hành vi với cùng một sản phẩm trong 30 phút chỉ ghi một lần, nên tải lại trang không làm tăng điểm.

### 4.2. Xây đặc trưng (`features.py`)

- **Điểm tương tác:** `r(u,i) = Σ_t w_t · log(1 + n(u,i,t)) · 0,5^(tuổi / h)`. Hàm log làm cho việc xem đi xem lại nhiều lần không lấn át được hành vi mua; hệ số `0,5^(tuổi / h)` giảm dần hành vi cũ (tuổi = số ngày từ lần gần nhất của hành vi đó, nửa đời h = 90 ngày chọn trên validation; `REC_HALF_LIFE_DAYS = 0` tắt).
- **Lý do chọn trọng số w_t (xem 1, bấm 2, thích 3, thêm giỏ 4, mua 5):** xếp theo mức cam kết của khách: xem chỉ là lướt qua, bấm là chủ động chọn, thích là bày tỏ, thêm giỏ là ý định mua, mua là chắc chắn. Thang tuyến tính 1–5 giữ thứ tự đó mà không để một hành vi lấn át hẳn các hành vi còn lại.
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
| **Hybrid** | Min-Max trên tập ứng viên, `Score = α·S'_CBF + (1−α)·S'_CF`, **α = 0,6** (cấu hình đang chạy, chọn trên validation 09/11; các lần đo 25/11–27/11 chọn α = 0,4, mục 9.1) | `engine.py` |

- **Fold-in:** khi có request, vector của khách được tính lại từ hành vi *hiện tại*, nên hành vi mới phát sinh sau lần huấn luyện gần nhất vẫn được tính.
- **Huấn luyện:** khoảng 7 giây (25/11/2026: 2.491 người dùng × 4.491 sản phẩm; ALS 16 chiều, λ = 1, a = 5, nửa đời 90 ngày). Tự chạy lại khi mô hình đủ 24 giờ tuổi, tính từ lúc huấn luyện (khởi động lại dịch vụ với mô hình cũ hơn 24 giờ thì huấn luyện lại ngay; trước 11/11/2026 tính từ lúc khởi động, nên máy tắt / mở mỗi ngày thì mô hình không được cập nhật); mô hình lưu ở volume `ai_models`.

### 4.4. Các bước tạo danh sách "Gợi ý dành cho bạn"

1. Lấy **K = 50** ứng viên tốt nhất từ CBF và 50 từ CF, rồi hợp nhất.
2. **Lọc:** bỏ sản phẩm bị ẩn, đã xoá, giá = 0, và sản phẩm khách **đã tương tác hoặc đã mua**. Khi hiển thị
   (`/recommend`) còn bỏ ốp lưng, kính cường lực… ghi đời máy mà khách chưa xem hay mua đời máy đó (`own_models_only`,
   từ 09/11/2026: trước đó 144/2.302 khách được gợi ý ốp lưng iPhone 15 Pro dù chỉ xem máy OPPO hay Samsung). Bộ lọc
   này không bật trong đánh giá offline (mục 9.1), nên số liệu ở đó là của mô hình chưa có bộ lọc.
3. Tính điểm Hybrid và sắp xếp giảm dần. Bằng điểm thì ưu tiên sản phẩm phổ biến hơn, rồi sản phẩm cập nhật gần hơn.
4. **Xếp lại cho đa dạng (MMR, λ = 0,7):** chọn lần lượt từng sản phẩm j có
   `λ·Score(j) − (1 − λ)·max_{k đã chọn} cos(v_j, v_k)` lớn nhất. Sản phẩm gần giống món đã chọn (5 chiếc iPhone cùng
   dòng) bị trừ điểm, nhường chỗ cho sản phẩm khác loại. λ = 1 thì giữ nguyên thứ tự theo điểm (`REC_MMR_LAMBDA`).
5. Lấy **Top-N** (mặc định 8) kèm **lý do đề xuất**:
   - *"Tương tự iPhone 16 bạn đã quan tâm"* khi điểm chủ yếu đến từ CBF (sản phẩm khách đã xem, thích, thêm giỏ hoặc mua);
   - *"Khách quan tâm Samsung Galaxy S26 Ultra cũng quan tâm sản phẩm này"* khi điểm chủ yếu đến từ CF (sản phẩm khách
     đã tương tác có vector ALS gần nhất); không xác định được thì *"Khách hàng có sở thích giống bạn cũng quan tâm"*.

**Cold-start**:

| Trường hợp | Cách xử lý |
|---|---|
| Khách có ≥ 2 lượt tương tác | Hybrid α = 0,6, xếp lại bằng MMR |
| Khách có đúng 1 lượt tương tác | Chỉ dùng CBF (α = 1), cũng xếp lại bằng MMR |
| Khách chưa có hành vi (mới đăng ký / chưa đăng nhập) | **Bảng hỏi sở thích** (mục 4.4b): gợi ý theo câu trả lời, không dùng hành vi của khách khác |
| Chưa có hành vi và chưa trả lời bảng hỏi | Web hiện lời mời trả lời thay cho danh sách; API vẫn trả ¾ phổ biến + ¼ mới (`basis = POPULAR`) |
| Sản phẩm mới chưa ai tương tác | Chỉ có điểm CBF |

Ngưỡng **k = 2** (`REC_COLD_START_K`) được chọn trên validation 09/11 (mục 9.1, bước 5; các lần đo 25/11–27/11 ra k = 1): trước 08/11/2026 cố định k = 5 theo
thiết kế, nhưng đo theo số lượt thì Hybrid đã tốt hơn chỉ CBF từ lượt thứ 2.

### 4.4b. Khách mới: bảng hỏi sở thích (từ 09/11/2026, theo góp ý của GVHD)

Trước đây khách chưa có hành vi nhận danh sách phổ biến, tức là gợi ý theo hành vi của **toàn bộ** khách, ai cũng như ai.
Nay hệ thống **hỏi** khách rồi gợi ý theo câu trả lời (preference elicitation):

| Câu | Lựa chọn (lấy từ dữ liệu) | Bắt buộc |
|---|---|---|
| 1. Loại sản phẩm quan tâm | 6 danh mục đang bán, chọn nhiều | Có |
| 2. Tầm giá | Phổ thông / Tầm trung / Cao cấp = phân khúc giá **trong từng danh mục** (phân vị 1/3, 2/3 như CBF) | Không |
| 3. Hãng yêu thích | Các hãng có hàng ở những danh mục đã chọn, hãng nhiều sản phẩm trước | Không |
| 4. Nhu cầu sử dụng | Bảng `needs` / `product_needs` (chơi game, chụp ảnh, học tập văn phòng, đồ hoạ…): điện thoại, laptop, máy tính bảng | Không |

**Khi nào hỏi:** đăng ký / đăng nhập (kể cả Google) mà tài khoản chưa có hành vi và chưa trả lời → chuyển tới trang
`customer/preferences.html` trước. Khu "Gợi ý dành cho bạn" (trang chủ, trang Khuyến nghị) hiện lời mời trả lời thay cho
danh sách khi chưa có cả hai. Khách chưa đăng nhập cũng trả lời được: câu trả lời lưu ở trình duyệt, gửi kèm
`POST /recommendations`, đăng nhập thì chép vào tài khoản. Sửa lại ở menu tài khoản "Sở thích mua sắm".

**Cách tính** (`Recommender.from_preferences`):

1. Ứng viên: sản phẩm đang bán thuộc danh mục đã chọn; bỏ ốp lưng, kính cường lực… theo đời máy (chưa biết khách dùng
   máy nào).
2. `Score_Q(i) = (1 + Σ_q khớp_q(i)) / (1 + |Q_i|)`: số 1 là câu danh mục (ứng viên luôn khớp); `Q_i` là các câu khách
   đã trả lời **và áp dụng được cho danh mục của i**: hãng (1 nếu đúng hãng đã chọn; chỉ tính khi khách chọn hãng có
   bán ở danh mục này, nên chọn Samsung không trừ điểm laptop), tầm giá (1 nếu đúng phân khúc), nhu cầu (tỉ lệ nhu cầu
   đã chọn của danh mục này mà sản phẩm đáp ứng).
3. Mỗi danh mục xếp bằng MMR (λ = 0,7); bằng điểm thì ưu tiên **điểm đánh giá, rồi số lượt đánh giá của sản phẩm**
   (thuộc tính sản phẩm), không dùng độ phổ biến tính từ hành vi khách khác. Sau đó lấy lần lượt từng vị trí của các
   danh mục để danh mục nào đã chọn cũng có mặt.
4. Lý do: *"Hợp với lựa chọn của bạn: Samsung · tầm trung · chụp ảnh, quay phim"*, hoặc *"Thuộc danh mục Laptop bạn
   quan tâm"* khi chỉ khớp câu danh mục.

Từ lượt xem / thích / thêm giỏ / mua đầu tiên, gợi ý chuyển sang theo **hành vi thật** của khách (CBF, rồi Hybrid từ
k = 2 lượt) như bảng trên. API trả thêm `basis`: `BEHAVIOR` / `PREFERENCES` / `POPULAR` để giao diện đổi tiêu đề.
Đánh giá offline (mục 9.1) không đổi: khách mô phỏng không có câu trả lời bảng hỏi.

### 4.5. Tương tự, Upsell, Cross-sell

| Loại | Điều kiện chọn | Xếp hạng | Ví dụ lý do |
|---|---|---|---|
| **SIMILAR** | Cùng danh mục, đang bán; phụ kiện cùng loại (camera IP → camera IP, sạc → sạc), ốp lưng / kính cường lực cùng đời máy | Cosine giữa hai sản phẩm | "Cùng hãng Apple, cùng tầm giá" |
| **UPSELL** | Cùng danh mục, giá cao hơn tối đa **δ = 30%**, **không kém hơn** ở thông số nào (RAM / bộ nhớ / camera) mà cả hai cùng ghi, và **tốt hơn ít nhất 1 thông số** hoặc **cùng hãng** (đời mới / bản cao hơn cùng dòng: iPhone 17 Pro Max → iPhone 18 Pro Max). Phụ kiện: **cùng loại** (sạc → sạc, tai nghe → tai nghe; `compat.accessory_kind`), ốp lưng / kính cường lực cùng đời máy. Đồng hồ thời trang (không có thông số) chỉ xét giá | Hybrid nếu khách có dữ liệu, ngược lại theo độ giống sản phẩm đang xem | "Nâng cấp camera 50MP, chỉ thêm 3.000.000đ" |
| **CROSS_SELL** | Quan hệ `ACCESSORY / COMPATIBLE / BOUGHT_TOGETHER` trong `product_relations` + cặp mua **cùng đơn** có **lift > 1** + **"khách mua A cũng mua B"** (đếm theo khách trên toàn lịch sử thêm giỏ / mua, B khác danh mục với A, ít nhất 2 khách chung, lift > 1; `_co_buyers`); cặp từ luật phải qua `compat.compatible` (phụ kiện vừa máy, thiết bị cùng hãng; đang xem phụ kiện thì xét chiều máy → phụ kiện: tai nghe → điện thoại được, camera IP → điện thoại thì không) | 0.6 × mức liên quan + 0.4 × Hybrid (khi đã đăng nhập); mức liên quan: phụ kiện = strength, cùng đơn = 0,5 + 0,5·confidence, cùng khách = 0,4 + 0,5·confidence | "Phụ kiện phù hợp với sản phẩm này" / "Thường được mua cùng sản phẩm này" / "Khách mua sản phẩm này cũng mua" |

Đơn hàng hiện còn ít nên chưa tính được "mua chung" đáng tin. Vì vậy `build_relations.py` tạo **6.191 quan hệ phụ kiện** theo quy tắc:

- **Điện thoại:** ốp lưng, kính cường lực, sạc, cáp, tai nghe.
- **Laptop:** chuột, bàn phím, balo, túi chống sốc.
- **Máy tính bảng:** Apple Pencil, bàn phím, sạc…
- **Đồng hồ thông minh:** sạc, tai nghe…

Mỗi thiết bị có tối đa 8 phụ kiện (tối đa 2 mỗi nhóm). Phụ kiện cùng hãng được ưu tiên. Phụ kiện chỉ dành cho iPhone (Lightning, MagSafe iPhone, Magic Mouse…) chỉ gắn với sản phẩm Apple.
Các luật nằm ở `app/recommender/compat.py`, dùng chung cho `build_relations.py` và `cross_sell`.

Phụ kiện chỉ vừa một số máy (`fits`, từ 08/11/2026; trước đó iPhone 17e được gợi ý ốp lưng iPhone 15 Pro):

- **Ốp lưng, kính cường lực, miếng dán ghi tên đời máy** chỉ gắn với đúng đời đó: "Ốp lưng iPhone 16" gắn với iPhone 16,
  không gắn với iPhone 16e, 16 Plus hay 17e. Catalogue hiện chỉ có ốp iPhone 15 Pro và iPhone 16, nên đa số điện thoại
  không có ốp trong danh sách mua kèm (đúng hơn là gợi ý ốp không vừa).
- **Apple Pencil Pro** chỉ gắn với iPad Pro, iPad Air, iPad mini (iPad thường không dùng được).
- **Cáp sạc từ tính (Magnetic) của Apple** là sạc riêng của Apple Watch nên chỉ gắn với Apple Watch (trước đó cáp này
  được gợi ý cho gần 500 máy, kể cả điện thoại Android và đồng hồ Garmin, Huawei). Pin sạc dự phòng không dây Magnetic
  (chuẩn Qi2) không bị ảnh hưởng.
- **Cáp Type-C – Lightning** chỉ gắn với iPhone 14 trở về trước. Từ iPhone 15, mọi iPad và Apple Watch đang bán đều
  không dùng Lightning, nên trong catalogue hiện tại cáp này không gắn với máy nào (trước đó gợi ý cho 12 iPhone,
  14 iPad, 13 Apple Watch).
- **Máy tính bảng** không gắn bàn phím có dây (trước đó 38 máy được gợi ý bàn phím cơ gaming có dây).
- **Balo / túi laptop ghi cỡ inch** chỉ gắn với laptop có màn hình không lớn hơn cỡ đó (đọc từ thông số "Kích thước màn hình").

Luật mua chung (cùng đơn, "khách mua A cũng mua B") học từ hành vi nên có thể ra cặp không hợp lý, nhất là trên dữ liệu
mô phỏng. Vì vậy `cross_sell` lọc các cặp này qua `compat.compatible` (từ 09/11/2026):

- phụ kiện phải thuộc nhóm phụ kiện của loại máy và qua các luật trên (trước đó: Nothing Phone (4b) → camera IP,
  vivo Y05e → cáp Lightning, Redmi Watch → bộ kích sóng Wi-Fi);
- thiết bị chỉ gợi ý thiết bị khác **cùng hãng**, tức cùng hệ sinh thái: OPPO A6t Pro → OPPO Watch X3, Apple Watch →
  MacBook, Galaxy Tab → Galaxy S26 Ultra (trước đó: laptop Acer → iPhone Air, Garmin → Motorola);
- danh mục ngoài catalogue (dữ liệu Amazon ở mục 9.1c) không bị lọc, nên kết quả trên Amazon không đổi.

Quét toàn bộ 868 thiết bị sau khi sửa: không còn cặp nào ở trên, máy nào cũng đủ 4 gợi ý mua kèm.

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
| `PRODUCT` | Bảng `products` + thông số + phiên bản | 4.302 | Tên, danh mục, hãng, giá, bảo hành, phiên bản, tối đa 12 thông số (thông số chính của danh mục trước) |

- Nội dung chính sách viết **đúng theo code Backend**: phí giao hàng mặc định 30.000đ (một số tỉnh có phí riêng), không có ngưỡng miễn phí; trả góp 0% cho đơn từ 3.000.000đ, kỳ hạn 3/6/9/12 tháng; trả hàng trong 7 ngày…
- **Không đưa mô tả sản phẩm crawl vào chunk.** Phần mô tả toàn câu quảng cáo ("thu cũ đổi mới, trả góp 0%…"). Khi còn để mô tả, câu hỏi "đổi trả trong bao nhiêu ngày" trả về… đồng hồ Casio.
- **Thông số chính trước** (`KEY_SPECS` trong `build_index.py`): điện thoại lấy chip, RAM, bộ nhớ, camera sau / trước,
  pin, sạc, màn hình, chống nước…; laptop lấy CPU, RAM, ổ cứng, card đồ hoạ, màn hình, pin, khối lượng…; còn chỗ
  mới lấy các thông số khác theo thứ tự gốc. Trước đây chỉ lấy 12 thông số đầu theo thứ tự crawl nên 79/166 chunk điện
  thoại có độ phân giải camera và **không chunk nào có dung lượng pin** (nay 161/169 và 145/169, bằng số máy có thông số
  đó trong database). Hệ quả cũ: hỏi "điện thoại chụp ảnh đẹp" ra máy ghi "camera sau: Không có".
- **Embedding vẫn tính trên văn bản cũ** (12 thông số đầu theo thứ tự gốc), còn `content` (LLM đọc, tìm theo từ khoá)
  là bản có thông số chính. Đã thử đưa thông số chính vào embedding: các chunk cùng loại giống nhau hơn và vector Hit@1
  giảm 0,850 → 0,775 (nhầm Redmi Note 14 Pro+ với 15 Pro+, tai nghe không chống ồn), nên giữ cách cũ.
- Mỗi chunk lưu `metadata` (danh mục, hãng, giá) để lọc.
- Mô hình E5 cần tiền tố `"passage: "` cho tài liệu và `"query: "` cho câu hỏi.

**Truy xuất** (`app/chatbot/rag.py`), cấu hình bằng `RAG_MODE`:

| Cách tìm | Cách làm | Mạnh / yếu |
|---|---|---|
| **`vector`** (mặc định) | Embedding câu hỏi rồi lấy Top-K chunk có cosine lớn nhất (toán tử `<=>`, chỉ mục **HNSW**: tìm láng giềng gần đúng, không cần so với cả 4.317 vector) | Hiểu nghĩa ("máy pin trâu"); thực tế cũng tìm đúng mã máy |
| `keyword` | Full-text search của PostgreSQL trên cột `search_tsv`: bỏ dấu tiếng Việt (`unaccent`), tên sản phẩm trọng số A, nội dung trọng số D, bỏ từ dừng ("giá", "bao", "nhiêu"...), khớp tiền tố | Nhanh (10 ms), nhưng không hiểu từ đồng nghĩa và dễ xếp sai thứ tự |
| `hybrid` | Chạy cả hai rồi gộp bằng **Reciprocal Rank Fusion** có trọng số: `điểm = 1/(60+hạng_vector) + w/(60+hạng_từ_khoá)` (`RAG_KEYWORD_WEIGHT`) | Thử nghiệm; đo được là kém hơn `vector` (mục 9.5) |

- Tách theo loại tài liệu: câu hỏi chính sách chỉ tìm trong `POLICY`, lấy **K = 5** (`RAG_POLICY_K`); tìm sản phẩm chỉ trong `PRODUCT`, tối đa 8.
- Với sản phẩm: lọc theo danh mục, hãng, khoảng giá **trước** khi xếp hạng. Nhờ vậy "điện thoại chụp ảnh đẹp dưới 10 triệu" vừa hiểu đúng nghĩa vừa đúng điều kiện giá.
- Bỏ chunk có cosine dưới ngưỡng (0,8 chính sách, 0,78 sản phẩm), để LLM không trả lời dựa trên nội dung không liên quan.
- Phía LLM: `temperature = 0.2` để câu trả lời ổn định (không cần chỉnh top-k / top-p khi sinh chữ).

### 5.3. Function Calling: 11 tool (`app/chatbot/tools.py`)

| Tool | Cần đăng nhập | Lấy dữ liệu từ |
|---|---|---|
| `search_policies(query)` | | RAG, tài liệu `POLICY` |
| `search_products(query, category, brand, min_price, max_price, limit)` | | RAG, tài liệu `PRODUCT` + lọc metadata; 2 sản phẩm đầu kèm giá theo dung lượng (`GET /products/{id}/variants`) |
| `get_product_details(product_id)` | | `GET /products/{id}`, `/variants`, `/specifications` (giá **hiện tại**) |
| `recommend_products(type, product_id \| product_name)` | | `POST /recommendations` (dùng lại hệ khuyến nghị); chỉ có tên thì tool tự tìm sản phẩm |
| `get_my_orders()` | ✅ | `GET /orders`: đơn chưa xong, tối đa 3 đơn đã xong gần nhất và các dòng trả lời dựng sẵn `answer_lines` |
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
- **Chốt chặn cho mô hình nhỏ** (đo bằng bộ đánh giá, mục 9.4):
  - trạng thái đăng nhập được ghi trong **lời nhắc hệ thống**, không chèn vào tin nhắn khách (chèn vào tin nhắn làm Qwen quên gọi tool);
  - lượt đầu không gọi tool nào thì **nhắc lại một lần** "câu này cần tra cứu thì gọi tool ngay" và gửi sự kiện `reset` để xoá đoạn "để mình tra cứu..." đã hiện (Ollama bỏ qua `tool_choice="required"` nên phải làm bằng code). Lời nhắc trích nguyên văn câu hỏi mới nhất: trước 09/11/2026 nó viết "câu hỏi trên", và khi hội thoại dài Qwen hiểu nhầm là một câu trước đó (hỏi giải toán sau khi đã hỏi giá iPhone thì trả về danh sách iPhone);
  - còn thao tác chờ bấm Xác nhận mà câu trả lời viết "Đã thêm / đã huỷ" thì đổi thành "Chuẩn bị thêm / chuẩn bị huỷ";
  - khách chưa đăng nhập: lời nhắc hệ thống nói rõ "hỏi đơn hàng, giỏ hàng, bảo hành của chính mình thì mời đăng nhập, không hỏi mã đơn" (trước 09/11/2026 chỉ ghi "cần đăng nhập" và khoảng 1/10 lần mô hình hỏi lại mã đơn thay vì gọi tool; sau khi sửa 12/12 lần đúng);
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
| POST | `/api/v1/recommendations` | Công khai (token tuỳ chọn) | `{type, productId?, size?, preferences?}` → `{type, source, basis, items:[{product, score, reason}]}`; `preferences`: câu trả lời bảng hỏi của khách chưa đăng nhập |
| GET | `/api/v1/recommendations/preference-options` | Công khai | Lựa chọn của bảng hỏi: danh mục đang bán, hãng (số sản phẩm), nhu cầu |
| GET / PUT | `/api/v1/users/me/preferences` | Đã đăng nhập | Câu trả lời của tôi (`answered`, `hasActivity`) / lưu lại (thay toàn bộ; ít nhất 1 danh mục) |
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
| POST | `/recommend` | `{userId?, n, excludeProductIds, preferences?}`: khuyến nghị cá nhân, trả thêm `basis` (BEHAVIOR / PREFERENCES / POPULAR) |
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

Bảng hỏi sở thích (09/11/2026) dùng bảng **`user_preferences`** (migration `user_preferences.sql`, đã có trong
`techshopping.sql`): mỗi dòng là một lựa chọn của khách, đúng một trong `category_id` / `brand_id` / `need_id` /
`price_level` (LOW, MID, HIGH); khoá ngoại tới `users`, `categories`, `brands`, `needs`. Database đã có thì chạy
migration này cho cả `techshopping` và `techshopping_test`.

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
- **Đặt context 8.192 token.** Ollama mặc định chỉ 4.096 (máy ít VRAM), mà một câu hỏi đơn hàng sau 6 lượt hội thoại đã
  cần khoảng 5.000 token; vượt giới hạn thì Ollama cắt phần đầu prompt (lời nhắc hệ thống) và mô hình bỏ quy tắc.
  - **Windows:** System Properties → Environment Variables → thêm biến người dùng `OLLAMA_CONTEXT_LENGTH` = `8192`,
    rồi thoát hẳn Ollama ở khay hệ thống và mở lại.
  - **macOS (brew):** thêm vào `EnvironmentVariables` của dịch vụ rồi khởi động lại:
    ```bash
    plutil -replace EnvironmentVariables.OLLAMA_CONTEXT_LENGTH -string 8192 ~/Library/LaunchAgents/sh.brew.ollama.plist
    launchctl bootout gui/$(id -u) ~/Library/LaunchAgents/sh.brew.ollama.plist
    launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/sh.brew.ollama.plist
    ```
    (`brew services restart` có thể chép lại plist gốc của brew; sửa cả `/opt/homebrew/opt/ollama/sh.brew.ollama.plist`
    nếu dùng lệnh đó.)
  - Kiểm tra: hỏi chatbot một câu rồi chạy `ollama ps`, cột `CONTEXT` phải là `8192`. `qwen3:8b` khi đó chiếm khoảng
    6,0 GB (4.096 token: 5,3 GB).
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
docker compose exec ai-service python -m scripts.evaluate                        # đánh giá → evals/results/recommender_latest.*
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
| `REC_ALPHA`, `REC_COLD_START_K`, `REC_HALF_LIFE_DAYS`, `REC_MMR_LAMBDA` | `0.6`, `2`, `90`, `0.7` | α của Hybrid, ngưỡng k cold-start, nửa đời giảm trọng số hành vi cũ (0 = không giảm), λ của MMR (1 = không xếp lại) |
| `ALS_FACTORS`, `ALS_REGULARIZATION`, `ALS_CONFIDENCE`, `ALS_ITERATIONS` | `16`, `1.0`, `5`, `15` | Siêu tham số ALS (số chiều, λ, a chọn trên validation) |

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

### 9.0. Các chỉ số đánh giá gợi ý nghĩa là gì

Mỗi khách được chấm riêng rồi lấy trung bình. Ví dụ một khách: trong phần kiểm thử (bị giấu khi học), khách thật sự
quan tâm **5 sản phẩm**; hệ thống gợi ý **10 sản phẩm** (N = 10), trúng **2 món** ở vị trí 1 và 6.

| Chỉ số | Trả lời câu hỏi | Ví dụ trên |
|---|---|---|
| **Precision@10** | Trong 10 món gợi ý, bao nhiêu phần trăm trúng? | 2 / 10 = 0,2 |
| **Recall@10** | Trong các món khách thật sự quan tâm, đoán ra được bao nhiêu? | 2 / 5 = 0,4 |
| **HitRate@10** | Có trúng ít nhất 1 món không? Trung bình trên mọi khách = tỉ lệ khách được đoán trúng | 1 |
| **NDCG@10** | Như Recall nhưng **thưởng món trúng nằm ở đầu danh sách**: món ở vị trí k được 1 / log2(k + 1) điểm (vị trí 1: 1; vị trí 2: 0,63; vị trí 10: 0,29), chia cho điểm cao nhất có thể (IDCG: mọi món đúng xếp đầu) nên luôn từ 0 đến 1 | (1 + 0,356) / 2,949 = 0,46; nếu 2 món trúng nằm ở vị trí 9 và 10 thì chỉ còn 0,20 |

NDCG@10 là chỉ số chính để chọn tham số và so sánh mô hình, vì khách thường chỉ nhìn vài sản phẩm đầu.

Hai chỉ số sau đo **cả danh sách**, không đo độ trúng:

| Chỉ số | Ý nghĩa |
|---|---|
| **Coverage** | Bao nhiêu % trong 4.302 sản phẩm đang bán từng được gợi ý cho ít nhất một khách. Thấp = ai cũng nhận cùng vài món (danh sách phổ biến: 0,4%) |
| **Diversity** | 10 món gợi ý cho một khách khác nhau đến đâu: trung bình 1 − cosine giữa các cặp vector thuộc tính. 0 = giống hệt, 1 = khác hẳn. MMR (mục 4.4) dùng để tăng chỉ số này |

**Khoảng tin cậy 95% (KTC):** lấy lại ngẫu nhiên tập khách 1.000 lần (bootstrap) để xem con số dao động bao nhiêu.
Khi so hai mô hình, tính KTC của **hiệu số** trên cùng tập khách: không chứa 0 thì chênh lệch là thật, chứa 0 thì
chưa đủ chắc chắn (có thể do may rủi).

**Vì sao các con số trông thấp:** chọn 10 trong 4.302 sản phẩm, đoán ngẫu nhiên chỉ được Precision khoảng 0,001.
Điều cần chứng minh là mô hình hơn các đối chứng một cách có ý nghĩa, không phải đạt số tuyệt đối cao. Với hệ thống
đang dùng (bảng mục 9.1): HitRate 0,23 = khoảng một phần tư số khách có ít nhất một món trúng; Precision 0,031 = trung
bình khoảng 0,3 món trúng trong 10; Recall 0,098 = đoán ra khoảng 10% số món khách quan tâm sau đó.

### 9.1. Đánh giá offline (`scripts/evaluate.py`)

Cách đánh giá (khoảng 3 phút):

- Hành vi của mỗi người dùng được chia theo thời gian thành 3 phần: **70% đầu để huấn luyện, 10% tiếp theo để tinh
  chỉnh (validation), 20% cuối để kiểm thử**.
- Tinh chỉnh trên validation (học trên phần huấn luyện), lần lượt từng bước, mỗi bước giữ giá trị có NDCG@10 cao nhất:
  1. tham số ALS: số chiều 16 / 32 / 64 × λ 0,1 / 1 × a 5 / 10 / 20 (18 bộ, chấm ALS riêng);
  2. α = 0; 0,1; …; 1 với ALS đã chỉnh;
  3. nửa đời giảm trọng số hành vi cũ: không giảm / 30 / 90 ngày, với α đã chọn;
  4. λ của MMR: 1 / 0,9 / … / 0,5. Xếp lại gần như luôn làm NDCG giảm một chút, nên bước này **không** lấy NDCG cao
     nhất mà lấy λ có độ đa dạng cao nhất trong các λ còn giữ được ít nhất 95% NDCG của λ = 1;
  5. ngưỡng cold-start k: khách mô phỏng nào cũng có ≥ 12 lượt, nên giả lập khách mới bằng cách **chỉ giữ m lượt đầu
     của 30% khách** (m = 1, 2, 3, 5, 8, 12; 70% còn lại giữ nguyên để ALS vẫn học được), học lại rồi so NDCG@10 của
     phổ biến / chỉ CBF / Hybrid trên chính các khách đó. k = m nhỏ nhất mà từ đó trở đi Hybrid không kém CBF.
- Kết quả báo cáo: học lại trên huấn luyện + validation với tham số đã chọn, chấm trên phần kiểm thử. Phần kiểm thử
  không tham gia chọn tham số nào, nên con số không bị "đẹp" nhờ chọn trên chính nó.
- Đối chứng: danh sách phổ biến, **item–item kNN** (sim(i, j) = cosine giữa hai cột của ma trận r(u,i); điểm của j =
  Σ_i r(u,i)·sim(i, j)), chỉ CBF, chỉ ALS.
- "Đúng" theo hai cách: mọi tương tác trong phần được chấm mà chưa gặp khi học, và chỉ thêm giỏ / mua (tín hiệu mạnh).
- Thêm độ đa dạng (Diversity = trung bình 1 − cosine giữa các cặp sản phẩm trong cùng danh sách), đường cong
  cold-start trên kiểm thử, có / bỏ hành vi LIKE, khách thật sự chỉ có 1 lượt, mua kèm (đơn 20% cuối theo ngày đặt;
  luật kết hợp học trên đơn 80% đầu) và nâng cấp.
- Khoảng tin cậy 95%: bootstrap 1.000 lần theo người dùng (cùng một mẫu cho mọi mô hình); so sánh hai mô hình bằng
  khoảng tin cậy của hiệu NDCG trên cùng người dùng (không chứa 0 = chênh lệch có ý nghĩa).
- Dữ liệu mô phỏng hiện có trong database (đo 25/11/2026, chạy lại 26/11 và **27/11/2026 01:24** ra y hệt mọi con số
  bên dưới): 16.643 / 2.508 / 5.831 hành vi huấn luyện / validation / kiểm thử; tinh chỉnh trên 354 người dùng, kiểm
  thử 548 người dùng có ≥ 2 lượt, N = 10. Khoảng 7 phút (lần 27/11: 397 giây).

> ⚠️ **Dữ liệu trong database đã khác lần đo 09/11/2026** (database được dựng lại: 2.491 người dùng, chỉ còn 2 lượt LIKE
> trong phần học thay vì 709). Vì vậy số liệu 25/11 **không so trực tiếp** với các con số ghi trước ngày này (như
> NDCG 0,0769 của lần 09/11). Báo cáo nên dùng bộ số 25/11 và ghi rõ ngày đo.

**Tham số chọn trên validation, lần đo 25/11/2026** (354 người dùng; chạy lại 27/11 chọn đúng các tham số này):

| Tham số | Chọn lần 25/11 | Hệ thống đang chạy (cấu hình, chọn lần 09/11) | NDCG@10 validation (lần 25/11) |
|---|---|---|---|
| ALS | 32 chiều, λ = 0,1, a = 5 | 16 chiều, λ = 1, a = 5 | 0,1012 (32 chiều λ 1: 0,1000; 16 chiều λ 1: 0,0778) |
| α | **0,4** | 0,6 | 0,1251 (α 0,6: 0,1149; ALS riêng 0,1012; CBF riêng 0,0842) |
| Nửa đời | 90 ngày | 90 ngày | 0,1285 (30 ngày: 0,1267; không giảm: 0,1251) |
| MMR λ | **0,8** | 0,7 | NDCG 0,1246 = 97% của λ = 1, Diversity 0,540 (λ = 1: 0,420; λ = 0,7: NDCG 0,1128, Diversity 0,632) |
| Ngưỡng k | **1** lượt | 2 lượt | Hybrid ≥ CBF từ lượt đầu (0,1232 so với 0,1105) |

Trên dữ liệu hiện tại, validation chọn tham số khác cấu hình đang chạy (cấu hình trong `config.py` vẫn là của lần 09/11:
α = 0,6, ALS 16 chiều, MMR λ = 0,7, k = 2). Đổi sang bộ mới bằng biến môi trường `REC_ALPHA`, `REC_MMR_LAMBDA`,
`REC_COLD_START_K` (và ALS bằng `ALS_FACTORS`, `ALS_REGULARIZATION`, `ALS_CONFIDENCE`) rồi huấn luyện lại; **chưa đổi** vì cần bạn quyết định
(tham số tốt nhất thay đổi theo dữ liệu, xem mục 9.1c: trên Amazon α = 0,2).

| λ của MMR (validation 25/11) | 1 (không xếp lại) | 0,9 | **0,8** | 0,7 | 0,6 | 0,5 |
|---|---|---|---|---|---|---|
| NDCG@10 | 0,1285 | 0,1272 | **0,1246** | 0,1128 | 0,1018 | 0,0885 |
| Diversity | 0,420 | 0,470 | **0,540** | 0,632 | 0,735 | 0,797 |

Kết quả trên tập kiểm thử (đúng = mọi tương tác, 548 người dùng):

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 | Coverage | Diversity |
|---|---|---|---|---|---|---|
| Phổ biến (đối chứng) | 0.0075 | 0.0436 | 0.0231 (0.0161–0.0308) | 0.0748 | 0.4% | 0.639 |
| Item–item kNN (đối chứng) | 0.0254 | 0.0785 | 0.0596 (0.0474–0.0723) | 0.1898 | 24.9% | 0.624 |
| CBF (α = 1) | 0.0259 | 0.0861 | 0.0656 (0.0540–0.0790) | 0.2117 | 24.3% | 0.172 |
| CF / ALS (α = 0) | 0.0307 | 0.0945 | 0.0742 (0.0607–0.0881) | 0.2190 | 12.6% | 0.629 |
| **Hybrid α = 0,4 (chọn trên validation, không xếp lại)** | **0.0356** | **0.1110** | **0.0878 (0.0723–0.1030)** | **0.2500** | 16.5% | 0.431 |
| Hybrid α = 0,4 + MMR λ = 0,8 (chọn trên validation) | 0.0316 | 0.0995 | 0.0817 (0.0670–0.0969) | 0.2336 | 16.3% | 0.548 |
| Hybrid α = 0,6 (không xếp lại) | 0.0305 | 0.0969 | 0.0808 (0.0657–0.0966) | 0.2281 | 23.3% | 0.313 |
| **Hybrid α = 0,6 + MMR λ = 0,7 (cấu hình đang chạy)** | **0.0310** | **0.0965** | **0.0801 (0.0656–0.0958)** | **0.2336** | 19.1% | **0.515** |

Chênh lệch NDCG@10 của Hybrid α = 0,4 trên cùng người dùng: hơn ALS +0,0135 (KTC 95%: +0,0065 đến +0,0199), hơn CBF
+0,0221 (+0,0117 đến +0,0337), hơn kNN +0,0281 (+0,0186 đến +0,0390), hơn danh sách phổ biến +0,0647 (+0,0477 đến
+0,0820): **cả bốn đều có ý nghĩa**. Khác lần 09/11 (khi Hybrid chỉ hơn CBF chưa đủ chắc chắn), lần này Hybrid hơn **cả hai
thành phần** của nó; ALS (0,0742) cũng mạnh hơn CBF (0,0656). CBF vẫn làm
danh sách kém đa dạng (0,17).

**MMR đánh đổi độ chính xác lấy độ đa dạng:** so với Hybrid α = 0,4 không xếp lại trên cùng người dùng, độ đa dạng tăng
+0,118 (KTC 95%: +0,110 đến +0,125; 0,43 → 0,55), còn NDCG giảm 0,0061 (−0,0092 đến −0,0028, **có ý nghĩa**, khoảng 7%).

Chỉ tính thêm giỏ / mua là đúng (400 người dùng): Hybrid α = 0,4 NDCG 0,0579 (0,0416–0,0764), có MMR 0,0541, CBF 0,0548,
ALS 0,0502, kNN 0,0451, phổ biến 0,0243; Hybrid α = 0,6 + MMR λ = 0,7 (cấu hình đang chạy) 0,0513. Hybrid hơn kNN và
phổ biến có ý nghĩa; hơn ALS (+0,0077, KTC 0,0000 đến +0,0165) và CBF (+0,0030) chưa đủ chắc chắn.

**Cold-start: NDCG@10 theo số lượt của khách mới** (kiểm thử, cắt lịch sử của khoảng 90 khách còn m lượt đầu; bảng
validation dùng để chọn k nằm trong `recommender_latest.md`):

| Số lượt m | Phổ biến | Chỉ CBF | Hybrid (ALS từ lượt đầu) |
|---|---|---|---|
| 1 | 0,037 | 0,141 | 0,158 |
| 2 | 0,036 | 0,160 | 0,182 |
| 3 | 0,036 | 0,162 | 0,189 |
| 5 | 0,034 | 0,164 | 0,212 |
| 8 | 0,028 | 0,165 | 0,223 |
| 12 | 0,028 | 0,164 | 0,210 |

- Hybrid hơn chỉ CBF ở **mọi** số lượt kể cả lượt đầu, cả trên validation (m = 1: 0,1232 so với 0,1105) lẫn kiểm thử,
  nên quy tắc "k = m nhỏ nhất mà từ đó Hybrid không kém CBF" cho **k = 1** (lần 09/11 cho k = 2). Cả CBF và Hybrid đều
  hơn hẳn danh sách phổ biến ngay từ lượt đầu, và càng nhiều lượt càng chính xác.
- 171 khách **thật sự chỉ có 1 lượt** (khách mẫu chỉ có 1 đơn hàng): CBF 0,0174 so với phổ biến 0,0181, chênh −0,0006
  (KTC −0,020 đến +0,021) = **ngang nhau**. Lần 09/11 danh sách phổ biến hơn CBF có ý nghĩa; nay không còn. Đơn hàng
  mẫu không theo sở thích từng khách, và đây là dữ liệu giả nên chưa kết luận được cho khách thật; cần đo lại khi có
  hành vi thật. Khách như vậy đã có bảng hỏi sở thích (mục 4.4b) thay cho danh sách phổ biến.

**Danh sách yêu thích (LIKE): có so với bỏ.** Database hiện chỉ có **2 lượt LIKE** trong phần học, nên phép so sánh không
còn ý nghĩa (NDCG 0,0817 so với 0,0818, chênh −0,0001, KTC −0,0002 đến +0,0000). Lần 09/11 (709 lượt LIKE) cũng ra
chênh lệch chưa đủ chắc chắn (+0,0027). Cơ chế LIKE vẫn được ghi và dùng đúng (r(u,i) có thêm trọng số 3); tác dụng thật
chỉ đo được khi có khách thật dùng trái tim. Dữ liệu mô phỏng khi đó theo giả định "thích = dấu hiệu sẽ mua"
(`seed_interactions.py`).

Phần chưa đánh giá được trên dữ liệu hiện có:

- **Mua kèm:** HitRate@4 = **3,02%**, **thấp hơn "bán chạy nhất" 6,03%** nhưng chưa đủ chắc chắn (199 cặp đơn – sản phẩm;
  KTC 95% của chênh lệch: −7,0 đến +1,0 điểm %), 51% sản phẩm có danh sách mua kèm. Chỉ luật cùng đơn: 2,51% (42%
  sản phẩm có gợi ý). Đơn hàng mẫu được sinh ngẫu nhiên nên không có cặp "hay mua cùng nhau" thật, nên chưa chứng minh
  được mua kèm hơn bán chạy trên dữ liệu này (lần 09/11 hai bên bằng nhau, 2,17%). Trên dữ liệu mua hàng thật (mục 9.1c)
  cách mới hơn bán chạy có ý nghĩa. Điểm này không đo việc gợi ý đúng đời máy hay đúng cổng sạc; phần đó do luật phụ
  kiện bảo đảm và có test riêng. Từ 11/11/2026, đang xem phụ kiện thì chỉ gợi ý máy dùng được phụ kiện đó
  (bỏ các cặp kiểu "camera IP mua kèm iPhone"): đo lại 26/11 HitRate không đổi, sản phẩm có danh sách mua kèm giảm từ
  61% xuống 51%.
- **Nâng cấp:** 97,7% sản phẩm đang bán có gợi ý nâng cấp, gợi ý đầu tiên đắt hơn trung bình 10%. Từ 11/11/2026 gợi ý
  không được kém hơn ở thông số nào và phụ kiện phải cùng loại (trước đó 16–21% gợi ý cho laptop, điện thoại, máy tính
  bảng kém hơn ở một thông số, và ốp lưng được "nâng cấp" lên camera IP, loa).

Các phép đo này đã sẵn sàng; chỉ có ý nghĩa khi có hành vi và đơn hàng thật.

Mỗi lần chạy ghi vào `evals/results/`: `recommender_latest.md` (bảng dán vào báo cáo), `recommender_latest.csv` (Excel),
`recommender_latest.json` và `recommender_alpha.svg` (biểu đồ NDCG@10 / Recall@10 theo α, chèn được vào Word).
Mỗi lần chạy cũng được thêm vào lịch sử trong database (`recommendation_evaluations` + chỉ số từng mô hình
`recommendation_evaluation_rows`, migration `recommendation_evaluations.sql`): đây là bảng riêng của phần AI và là nơi
duy nhất AI Service ghi khi đang chạy; mọi truy vấn khác vẫn chỉ đọc.
Trang admin **Gợi ý sản phẩm** (`admin/recommendations.html`, chỉ ADMIN) hiện mô hình đang dùng, bảng này kèm biểu đồ và
nút "Đánh giá lại" (chạy nền trên một bản mô hình riêng, không ảnh hưởng gợi ý đang phục vụ khách).

### 9.1b. Hiệu quả trên web (tỉ lệ bấm)

- Lượt hiển thị: mỗi sản phẩm backend trả trong một danh sách gợi ý cho khách đã đăng nhập (bảng `recommendations`).
- Lượt bấm: khách bấm vào chính sản phẩm đó trong khu gợi ý trong vòng 1 ngày (`recommendations.clicked_at`,
  migration `recommendation_clicks.sql`); tỉ lệ bấm (CTR) = lượt bấm / lượt hiển thị.
- Chuyển đổi: sau khi bấm, khách thêm sản phẩm vào giỏ hoặc mua trong 7 ngày.
- Xem theo từng khu (Gợi ý cho bạn, Tương tự, Nâng cấp, Mua kèm) ở trang admin **Gợi ý sản phẩm**, mục "Hiệu quả trên web".
  Số liệu chỉ có ý nghĩa khi có khách thật dùng web (dữ liệu mô phỏng không bấm).

> ⚠️ **Dữ liệu hành vi là mô phỏng.** Hệ thống mới chạy nên chưa có hành vi thật. `seed_interactions.py` tạo 300 khách mô phỏng có "hồ sơ sở thích": danh mục, hãng (35% trung thành với một hệ sinh thái như Apple/Xiaomi), tầm giá, kèm 10% hành vi ngẫu nhiên và LIKE theo giả định "thích = dấu hiệu sẽ mua"; ngoài ra ghi PURCHASE cho đơn hàng của khách mẫu `@example.test` (tài khoản thật không bị đụng tới: backend đã tự ghi PURCHASE lúc họ đặt hàng). Các con số trên chỉ dùng để **so sánh tương đối giữa các mô hình**; báo cáo đã ghi rõ điều này. Vì khách mô phỏng được tạo theo đúng danh mục, hãng và tầm giá (các thuộc tính CBF dùng), dữ liệu này có thể **thiên vị CBF**; trên khách thật CBF và Hybrid có thể kém hơn số trên.
>
> **Trước khi chạy cho khách thật:** xoá hành vi mô phỏng (`docker compose exec ai-service python -m scripts.seed_interactions --reset`) rồi huấn luyện lại, để mô hình đang phục vụ khách không học từ dữ liệu giả.

### 9.1c. Kiểm chứng trên dữ liệu mua hàng thật (Amazon Reviews 2023, `scripts/eval_amazon.py`)

Vì hành vi trong hệ thống là mô phỏng (và có thể thiên vị CBF), quy trình ở mục 9.1 được chạy **nguyên vẹn** trên dữ liệu
công khai có hành vi thật: Amazon Reviews 2023 (McAuley Lab), nhóm **Cell Phones & Accessories**, cùng ngành hàng với đề tài.
Script chỉ thay nguồn dữ liệu (hàm đọc sản phẩm, thông số, hành vi, đơn hàng), không sửa thuật toán và không ghi vào database.

- **Lấy mẫu** (giữ quy mô gần hệ thống): 5.000 sản phẩm nhiều đánh giá nhất của tập 5-core → lọc lặp 5-core → 20.000 người
  dùng ngẫu nhiên (seed 42). Kết quả: 4.999 sản phẩm, 41 danh mục, 126.097 lượt (09/2009 → 08/2023).
- **Ánh xạ**: mỗi đánh giá = một lượt PURCHASE (Amazon không công bố xem / thêm giỏ); đơn hàng cho mua kèm = sản phẩm cùng
  người, cùng ngày; danh mục = cấp 3 cây danh mục; thương hiệu = 100 hãng nhiều sản phẩm nhất, còn lại "Khác"; giá thiếu =
  trung vị danh mục; RAM / bộ nhớ / màn hình lấy từ `details` (bỏ giá trị nhập sai).

```bash
cd AI_Service
curl -LO https://huggingface.co/datasets/McAuley-Lab/Amazon-Reviews-2023/resolve/main/benchmark/5core/rating_only/Cell_Phones_and_Accessories.csv
python scripts/eval_amazon.py sample Cell_Phones_and_Accessories.csv amazon/
curl -L https://huggingface.co/datasets/McAuley-Lab/Amazon-Reviews-2023/resolve/main/raw/meta_categories/meta_Cell_Phones_and_Accessories.jsonl \
  | python scripts/eval_amazon.py filter-meta amazon/asins.txt amazon/meta.jsonl   # đọc luồng 4 GB, giữ 4.999 dòng
python scripts/eval_amazon.py run amazon/      # ~65 phút CPU → evals/results/amazon_cell_latest.md / .json
```

Kết quả (chạy lần đầu 08/11/2026, chạy lại 09/11/2026 14:18: số liệu không đổi; kiểm thử 20.000 người dùng, NDCG@10 kèm KTC 95%):

| Mô hình | NDCG@10 | HitRate@10 | Coverage |
|---|---|---|---|
| Phổ biến | 0,0118 (0,0106–0,0129) | 0,0259 | 0,3% |
| Item-kNN | 0,0208 (0,0193–0,0224) | 0,0423 | 99,7% |
| CBF (α = 1) | 0,0062 (0,0053–0,0070) | 0,0137 | 70,6% |
| CF/ALS (α = 0) | 0,0274 (0,0257–0,0291) | 0,0563 | 36,2% |
| **Hybrid α = 0,2** (chọn trên validation) | **0,0280** (0,0262–0,0297) | **0,0583** | 44,1% |
| Hybrid α = 0,2 + MMR λ = 0,9 | 0,0277 (0,0260–0,0295) | 0,0576 | 42,9% |
| Hybrid α = 0,6 (cấu hình đang dùng) | 0,0213 (0,0198–0,0229) | 0,0459 | 81,7% |

Đọc kết quả:
- Hybrid (α chọn trên validation) cao nhất; hơn **có ý nghĩa** CBF (+0,0218), Item-kNN (+0,0072), phổ biến (+0,0162);
  hơn ALS +0,0006 nhưng KTC (−0,0000 → +0,0012) chạm 0 → **ngang ALS**, độ phủ cao hơn (44% so với 36%).
- Trên hành vi thật, validation chọn **α = 0,2** (nghiêng về hành vi), khác α = 0,6 trên dữ liệu mô phỏng: CBF ở đây yếu
  (người mua phụ kiện cần đồ bổ trợ cho máy, không cần thêm món giống món đã mua). Trọng số α phụ thuộc dữ liệu → khi có
  khách thật phải **chọn lại α** bằng chính quy trình đánh giá (nút "Đánh giá lại" ở trang admin rồi đổi biến môi trường `REC_ALPHA`).
- MMR λ = 0,9: đa dạng +0,058 (có ý nghĩa), NDCG không giảm có ý nghĩa. Nửa đời tốt nhất: không giảm (dữ liệu trải 14 năm).
- **Mua kèm** (6.447 cặp đơn – sản phẩm, đơn = cùng người cùng ngày; luật chỉ học từ hành vi trước ngày cắt của đơn kiểm thử):

  | Cách gợi ý | HitRate@4 | Có gợi ý |
  |---|---|---|
  | **Đang dùng: cùng đơn + khách mua A cũng mua B** | **2,16%** | 40% |
  | Chỉ luật cùng đơn (cách cũ) | 1,04% | 14% |
  | Chỉ khách mua A cũng mua B | 1,69% | 38% |
  | Bán chạy nhất | 1,55% | 100% |

  Cách đang dùng hơn bán chạy **có ý nghĩa** (+0,60 điểm %, KTC 0,17 → 1,02) và hơn cách cũ (+1,12 điểm %): luật cùng
  đơn chỉ có ở 14% sản phẩm vì ít đơn nhiều món, đếm theo khách thì dày hơn nhiều.
- Chưa tốt: giả lập khách mới chỉ có 27–63 người đủ 12 lượt nên **không dùng** để kết luận ngưỡng k.

### 9.2. Unit test các công thức khuyến nghị (`tests/`)

84 test pytest, không cần database (dữ liệu nhỏ dựng tay trong `tests/conftest.py`):

- `test_features.py`: r(u,i) = Σ w·ln(1 + n) đúng trọng số từng hành vi (thêm giỏ + bấm = 4,159), giảm còn một nửa
  sau một nửa đời, Min-Max, vector đặc trưng (giá và phân khúc giá trong từng danh mục, RAM / bộ nhớ đọc từ phiên bản);
- `test_als.py`: fold-in bằng đúng nghiệm đóng x_u = (YᵀC_uY + λI)⁻¹YᵀC_u p_u; khách giống nhau được gợi ý sản phẩm
  của nhau; huấn luyện lặp lại cho cùng kết quả;
- `test_engine.py`: cosine và Hybrid đúng ví dụ tính tay của báo cáo, khách mới dưới 5 lượt chỉ dùng CBF, khách chưa
  có hành vi và chưa trả lời bảng hỏi nhận sản phẩm phổ biến; bảng hỏi (mục 4.4b): căn cứ BEHAVIOR / PREFERENCES /
  POPULAR, Score_Q đúng ví dụ tính tay, câu "hãng" chỉ tính ở danh mục có hãng đã chọn, bằng điểm thì theo điểm đánh
  giá sản phẩm chứ không theo hành vi khách khác, bỏ ốp lưng theo đời máy và sản phẩm loại trừ; không gợi ý sản phẩm đã xem hoặc ngừng bán, ốp lưng theo đời máy chỉ hiện với
  khách đã quan tâm đúng đời máy (đánh giá offline không lọc), luật mua kèm (lift > 1, ít nhất
  2 đơn), luật "khách mua A cũng mua B" (đếm theo khách, chỉ khác danh mục, bỏ hành vi xem; từ phụ kiện chỉ gợi ý máy dùng được phụ kiện đó) và lý do hiển thị, "Tương tự" của phụ kiện cùng loại, nâng cấp (cùng danh mục, đắt hơn tối đa 30%, không kém hơn ở thông số nào, khác hãng thì phải tốt hơn ít nhất một thông số, phụ kiện cùng loại và cùng đời máy), tính nhiều α một lần cho cùng kết quả,
  lý do ALS nêu đúng sản phẩm khách đã quan tâm, mô hình giữ tham số lúc học, lịch huấn luyện lại tính theo tuổi mô hình, MMR đúng ví dụ tính tay (bỏ qua sản
  phẩm trùng với món đã chọn khi λ = 0,5, giữ thứ tự khi λ = 0,8) và không làm danh sách kém đa dạng hơn;
- `test_evaluation.py`: Precision / Recall / NDCG / HitRate đúng ví dụ tính tay, chia dữ liệu theo thời gian từng
  người dùng (70 / 10 / 20) và đơn hàng theo ngày đặt, khoảng tin cậy bootstrap, item–item kNN, độ đa dạng, quy tắc
  chọn λ của MMR;
- `test_chat_tools.py`: chunk sản phẩm lấy thông số chính của danh mục trước, tóm tắt cho LLM rút gọn danh sách
  phiên bản mà vẫn giữ camera / pin, tool bỏ tham số lạ thay vì lỗi, Backend trả thiếu dữ liệu thì tool báo lỗi
  thay vì làm hỏng cả lượt chat, lời nhắc gọi tool trích đúng câu hỏi mới nhất (hội thoại dài không trả lời nhầm câu
  trước), thao tác chờ xác nhận không bị nói là "đã thêm / đã huỷ", `get_my_orders` tách đơn chưa xong và dựng dòng
  trả lời, bỏ câu nhắc nút Xác nhận khi không có thao tác, chọn phiên bản theo từng từ (kể cả khi phiên bản nằm trong
  tên sản phẩm), giá theo dung lượng, `recommend_products` tìm sản phẩm theo tên;
- `test_relations.py`: ốp lưng ghi đời máy chỉ gắn đúng đời đó (iPhone 16 ≠ 16e / 16 Plus / 17e), Apple Pencil Pro chỉ
  với iPad Pro / Air / mini, cáp sạc từ tính Apple chỉ với Apple Watch, cáp Lightning chỉ với iPhone ≤ 14, máy tính
  bảng không nhận bàn phím có dây, túi laptop phải vừa cỡ màn hình; `compatible` lọc luật mua chung (phụ kiện đúng
  nhóm, thiết bị cùng hãng, dữ liệu ngoài catalogue không bị lọc); loại phụ kiện đọc từ đầu tên ("Bộ sạc" = "Sạc", "AirPods" = tai nghe, camera IP khác camera cầm tay);
- cold-start: truyền ngưỡng k cho từng lần gợi ý mà không đổi cấu hình chung, quy tắc chọn k (m nhỏ nhất mà từ đó Hybrid
  không kém CBF).

```bash
cd AI_Service
uv pip install -r requirements-dev.txt     # hoặc pip install -r requirements-dev.txt
python -m pytest
```

Các test này đã bắt một lỗi thật: khách có 1–4 lượt tương tác (chỉ dùng CBF) làm `/recommend` lỗi 500, nên khách mới
luôn nhận danh sách dự phòng thay vì gợi ý theo sở thích. Lỗi đã được sửa trong `engine.py`.

### 9.3. Test tự động (Backend)

- 15 unit test mới:
  - `InteractionRecorderTest`: trọng số, chặn lặp, chặn PURCHASE từ trình duyệt;
  - `RecommendationServiceTest`: giữ thứ tự AI, bỏ sản phẩm ẩn, dự phòng Upsell đắt hơn hẳn; gửi câu trả lời bảng hỏi
    của tài khoản (khách đăng nhập) hoặc của trình duyệt (khách vãng lai) sang AI và chuyển tiếp `basis`;
  - `PreferenceApiIntegrationTest`: lựa chọn của bảng hỏi công khai (chỉ danh mục có hàng, nhu cầu có sản phẩm), câu trả
    lời cần token, lưu / thay toàn bộ, từ chối khi thiếu danh mục / mã không tồn tại / nhu cầu của danh mục chưa chọn,
    cờ "đã có hành vi";
  - `ChatServiceTest`: lưu tin, chặn phiên người khác, gắn phiên khách sau đăng nhập, cắt lịch sử dài, AI ngừng → 503.
- `ChatServiceTest` có thêm 2 test cho streaming: chuyển tiếp `delta`/`status`, kết thúc bằng `done` có thao tác; AI ngừng → sự kiện `error`.
- Toàn bộ **903 test** backend (133 lớp) đều đạt (09/11/2026, sau khi thêm bảng hỏi sở thích: 8 test mới). Trước đó
  là 895 test, 132 lớp; con số "902 test, 133 lớp" ghi trước đây đếm nhầm báo cáo cũ của `HtmlSanitizerTest` (7 test,
  lớp đã xoá) còn sót trong `target/surefire-reports`. Số chuẩn là dòng `Tests run:` ở cuối `./mvnw test`.
- **Chạy lại 27/11/2026: 903 / 903 đạt** (133 lớp, 0 lỗi). Lần chạy đầu ra 403 lỗi vì database `techshopping_test`
  chưa có bảng `user_preferences` (Hibernate báo `Schema validation: missing table [user_preferences]` nên không khởi
  động được Spring context). Chạy migration `database/migrations/user_preferences.sql` cho `techshopping_test` rồi chạy
  lại thì đạt hết. Lỗi nằm ở database kiểm thử, không phải ở code.
- Test tích hợp cần database `techshopping_test`. Nếu máy có Postgres khác chiếm `localhost:5432`, chạy:
  ```bash
  SPRING_DATASOURCE_URL=jdbc:postgresql://<IP-máy>:5432/techshopping_test SPRING_DATA_REDIS_HOST=<IP-máy> ./mvnw test
  ```

### 9.4. Đánh giá chatbot tự động (`scripts/eval_chatbot.py`)

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

**Lần chạy mới nhất** (03/11/2026, máy có GPU 4 GB, cùng cấu hình Qwen3 8B + giám khảo; đây là số dùng trong báo cáo
và slide): **26/30 (87%)**, trung bình 30,9 giây và 2,0 lần gọi LLM mỗi câu. Thời gian dài vì mô hình 8 tỷ tham số không
vừa 4 GB bộ nhớ GPU nên một phần chạy trên CPU. Chính sách, dữ liệu cá nhân, cửa hàng và an toàn vẫn đạt 100%; 4 câu
trượt đều là câu mô hình phải tự tổng hợp thông tin sản phẩm: hai câu hỏi giá nêu giá của phiên bản khác hoặc chỉ nêu
giá thấp nhất, một câu gợi ý phụ kiện chung chung, một câu hỏi lại màu thay vì đề xuất thêm vào giỏ.

**Lần chạy 08/11/2026** (Apple M1 Pro 16 GB, Ollama, cùng Qwen3 8B + giám khảo). Số 26/30 ở trên đo trên máy khác với
database khác, nên lần này so **trên cùng máy, cùng database**:

| Phiên bản | Đạt | Thời gian / câu |
|---|---|---|
| Code trước khi sửa | 24/30 (80%) | 10,5 s |
| Sau khi sửa: thông số chính trong chunk, tool đơn hàng mới, chốt chặn | **24/30 (80%)** | 12,5 s |

- 6 câu trượt **giống nhau ở cả hai phiên bản**:
  - 3 câu do bộ câu hỏi không còn khớp dữ liệu: `pol-shipping-fee` vẫn đòi ngưỡng miễn phí giao hàng 10 triệu (chính sách
    đã bỏ ngưỡng này); `acc-order-code`, `act-cancel` dùng đơn 1271 / 159, trên database này không thuộc tài khoản kiểm thử;
  - 3 câu là điểm yếu có sẵn: hỏi giá bản 256GB nhưng nêu giá thấp nhất (`prod-followup`), không gợi ý được phụ kiện
    (`rec-accessory`), hỏi lại màu thay vì đề xuất thêm vào giỏ (`act-add-cart`).
- Thời gian tăng 2 giây vì câu trả lời sản phẩm có thêm thông số và câu đơn hàng nêu đủ các đơn.
- Thay đổi mà 30 câu không đo, kiểm tra riêng:
  - "Điện thoại chụp ảnh đẹp dưới 10 triệu" không còn gợi ý máy ghi "camera sau: Không có", có kèm camera và pin.
  - "Đơn hàng của tôi tới đâu rồi?": trước đây tool trả 10 đơn, mô hình liệt kê hết (~500 token, 36–43 giây; có lịch sử
    thì prompt + trả lời 4.345 token, vượt context 4.096 của Ollama) và còn nói nhầm "đã giao xong" khi khách có đơn đang
    chờ. Nay: tài khoản 1 đơn đang chờ 6,6–8,2 giây; tài khoản 4 đơn đang chờ nêu đủ 4/4 mã ở 5/5 lần (trước: 1/4), 16 giây.
  - Mô hình thêm tham số lạ (`city`) không còn làm tool lỗi; câu nhắc "bấm nút Xác nhận" khi không có nút bị bỏ.
- Bài học khi sửa (đo trên cùng máy):
  - gửi cả chunk (~800 ký tự) cho LLM thay vì 400: 8 sản phẩm làm prompt vượt 4.096 token, mô hình bỏ quy tắc (kẻ bảng,
    trả lời lan man), **19/30**; rút gọn danh sách phiên bản rồi cắt ở 500 ký tự thì giữ được thông số mà không vượt;
  - thêm khoảng 800 ký tự vào lời nhắc hệ thống: tỉ lệ từ chối câu ngoài phạm vi giảm từ 5/5 xuống 2/5 lần thử; viết gọn
    lại và đưa quy tắc phạm vi lên đầu thì về 5/5;
  - viết lại quy tắc đơn hàng không giúp (1/4 mã ở cả hai cách viết); dựng sẵn dòng trả lời trong kết quả tool mới giúp.
- Sau đó tăng context của Ollama lên 8.192 (mục 8.1, bước 1) và chạy lại: vẫn **24/30**, 11,8 giây / câu, cùng 6 câu
  trượt. Bộ 30 câu là câu hỏi một lượt nên không chạm giới hạn 4.096; khác biệt nằm ở hội thoại dài: câu hỏi đơn hàng
  sau 6 lượt cần 5.023 token và vẫn nêu đủ 4/4 đơn đang chờ (18,5 giây).
- Cuối cùng sửa 6 câu trượt còn lại, rồi chạy lại cả bộ: **30/30 (100%)**, 12,6 giây / câu
  (`evals/results/20261108-1253.json`; lần so sánh với code cũ: `20261108-1053.json`).
- 09/11/2026, sau khi sửa lời nhắc gọi tool (trích câu hỏi mới nhất) và câu "đã thêm" khi còn chờ xác nhận: chạy
  lại cả bộ vẫn **30/30 (100%)**, 12,0 giây / câu (`evals/results/20261109-1304.json`).
- 09/11/2026 16:09, sau khi nói rõ cách xử lý câu hỏi dữ liệu cá nhân khi khách chưa đăng nhập: **30/30 (100%)**,
  12,9 giây / câu (`evals/results/20261109-1609.json`). Kết quả trên database cũ (09/11); số hiện tại ở dưới (27/11).
  - **Sửa bộ câu hỏi (4 câu)**: `pol-shipping-fee` kiểm theo chính sách hiện tại (30.000đ, phí riêng theo tỉnh; không được
    nêu ngưỡng miễn phí cũ); `acc-order-code`, `act-cancel` dùng đơn của chính tài khoản kiểm thử (DH00003119,
    DH00000221); `act-add-cart` nêu rõ màu ("256GB màu Đen"), vì bản 256GB có 3 màu nên hỏi lại là đúng. Các mã đơn
    phụ thuộc dữ liệu mẫu: trên database khác phải đổi theo đơn của `EVAL_LOGIN`.
  - **Sửa code (2 câu)**: `recommend_products` nhận thêm `product_name` (mô hình không biết id của iPhone 17e nên gọi với
    id rỗng và nhận danh sách trống); `search_products` trả thêm `prices_by_version` cho 2 sản phẩm đầu (trước chỉ có giá
    thấp nhất nên hỏi "bản Pro 256GB" chỉ nói được "giá từ…"). Kèm theo: chọn phiên bản theo từng từ ("256GB màu Đen" khớp
    "256 GB - Đen", trước so nguyên chuỗi nên không khớp) và đọc phiên bản trong tên sản phẩm khi mô hình không điền
    `variant_name`.
  - 30/30 là **một lần chạy**; mô hình 8B trả lời khác nhau giữa các lần, các lần chạy trước trên cùng máy dao động
    23–24/30 với cùng bộ câu hỏi cũ.

- **25/11/2026 21:37** (sau khi dựng lại database, cùng Qwen3 8B + giám khảo): **27/30 (90%)**, 27,8 giây / câu, 2,0 lần gọi LLM / câu
  (`evals/results/20261125-2137.json`). Đạt 100%: sản phẩm, khuyến nghị, cửa hàng, an toàn; chính sách 8/9, dữ liệu cá nhân
  5/6, thao tác có xác nhận 3/4. Ba câu trượt:
  - `acc-order-code` ("Kiểm tra giúp mình đơn DH00003119") và `act-cancel` ("Huỷ giúp mình đơn DH00000221"): database mới
    không còn hai đơn này thuộc tài khoản kiểm thử, nên chatbot đáp "không tồn tại hoặc không thuộc tài khoản của bạn" và
    không đề xuất huỷ. Đây là hành vi **đúng**; lỗi nằm ở bộ câu hỏi phụ thuộc dữ liệu mẫu (đã ghi chú ở dưới), cần đổi mã
    đơn theo đơn của `EVAL_LOGIN` rồi chạy lại. Chưa sửa bộ câu hỏi để không "chỉnh đề cho khớp kết quả".
  - `pol-shipping-fee`: câu trả lời thiếu cụm "tỉnh / thành" và nêu "đơn từ 10.000.000đ được miễn phí giao hàng". Kiểm tra
    lại ngày 27/11 thấy nguyên nhân là **kho tri thức cũ**: chunk "Phí giao hàng" trong `kb_chunks` vẫn là chính sách cũ,
    không khớp `knowledge/giao-hang.md`. Giám khảo LLM cho đạt vì câu trả lời khớp kết quả tool; bộ kiểm cụm từ mới bắt
    được lỗi.
- **27/11/2026 01:43**, cùng database và kho tri thức cũ: **27/30 (90%)**, 30,7 giây / câu, đúng 3 câu trượt như trên,
  câu trả lời `pol-shipping-fee` giống hệt (`evals/results/20261127-0143.json`).
- **27/11/2026 02:05, sau khi dựng lại kho tri thức** (`scripts.build_index`: 15 chunk chính sách + 4.302 chunk sản phẩm):
  **28/30 (93%)**, 27,1 giây / câu, 2,0 lần gọi LLM / câu (`evals/results/20261127-0205.json`, đang là `latest.md`). Đạt
  100%: chính sách (9/9), sản phẩm, khuyến nghị, cửa hàng, an toàn; dữ liệu cá nhân 5/6, thao tác có xác nhận 3/4.
  - `pol-shipping-fee` nay đạt: "phí mặc định 30.000đ cho mỗi đơn, không phân biệt giá trị đơn. Một số tỉnh / thành có
    phí riêng…".
  - Còn 2 câu trượt `acc-order-code`, `act-cancel`, cùng lý do dữ liệu như trên (mã đơn không thuộc tài khoản kiểm thử).
  - Kết quả 30/30 ngày 09/11 là trên database cũ; số **28/30 này mới là số hiện tại**.

Kết quả nằm ở `AI_Service/evals/results/latest.md` (bảng tỉ lệ đạt theo nhóm, thời gian trả lời, chi phí, danh sách câu trượt kèm lý do). Bản chi tiết từng câu (câu trả lời, tool đã gọi, token) nằm ở `evals/results/<thời gian>.json`. Logic chấm điểm đã được kiểm tra bằng agent giả: bắt đúng thiếu tool, thiếu / thừa cụm từ, sai giá, đề xuất thao tác thừa.

### 9.5. Đánh giá phần tìm kiếm RAG (`scripts/eval_retrieval.py`)

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

Kết quả lần chạy **27/11/2026**, sau khi dựng lại kho tri thức cho khớp `knowledge/*.md` và catalogue hiện tại (4.317
chunk; xem mục "Chỉ số mới nhất"):

| Cách tìm | Hit@1 | Hit@3 | Hit@5 | Hit@8 | Recall@3 | MRR | ms / câu |
|---|---|---|---|---|---|---|---|
| **vector** | **0,850** | **0,925** | **0,950** | **1,000** | **0,848** | **0,899** | 35 |
| keyword | 0,700 | 0,825 | 0,875 | 0,900 | 0,775 | 0,765 | 10 |
| hybrid (w = 1) | 0,775 | 0,900 | 0,950 | 0,950 | 0,842 | 0,845 | 39 |
| hybrid (w = 0,5) | 0,800 | 0,900 | 0,950 | 0,950 | 0,827 | 0,856 | 39 |
| hybrid (w = 0,25) | 0,800 | 0,900 | 0,925 | 0,950 | 0,833 | 0,859 | 38 |

Theo nhóm, `vector` (Hit@1 / Hit@3 / MRR): chính sách 0,80 / 0,87 / 0,86; gõ đúng tên / mã máy 1,00 / 1,00 / 1,00; mô tả
theo nhu cầu 0,62 / 0,88 / 0,77.

- So với kho cũ (lần chạy 25/11, 26/11 và 27/11 01:17 ra y hệt nhau): `vector` giữ nguyên Hit@K, MRR 0,900 → 0,899;
  `keyword` Hit@1 0,725 → 0,700, MRR 0,779 → 0,765; hybrid w = 1 tăng (MRR 0,832 → 0,845), w = 0,25 giảm (0,874 →
  0,859). Thay đổi do nội dung chunk "Phí giao hàng" mới và bỏ chunk của sản phẩm đã xoá.
- `keyword` không dùng khi chạy thật.

**Kết luận:**
- **Tìm kiếm lai không tốt hơn**: ở mọi trọng số đều kém `vector`; trọng số từ khoá càng nhỏ thì càng gần `vector`.
- Lý do: mô hình e5 đã tìm đúng **100% câu gõ đúng tên / mã máy**, nên phần từ khoá chỉ thêm nhiễu (ví dụ "Galaxy S26+" lại xếp bản Ultra lên đầu). Vì vậy mặc định là `vector`, còn `hybrid` được giữ làm tuỳ chọn.
- **Chọn K:** với câu hỏi chính sách, Hit@3 = 0,867, Hit@5 = 0,933, Hit@8 = 1,0. Chunk chính sách ngắn nên tăng K từ 3 lên **5** gần như không tốn thêm. Chạy lại bộ đánh giá chatbot sau thay đổi này vẫn đạt 9/9 chính sách (27/30 tổng ngày 13/10, 26/30 ngày 03/11).
- Điểm yếu còn lại nằm ở câu mô tả theo nhu cầu (Hit@1 = 0,62). Ví dụ "chuột không dây chơi game" ra chuột **có dây**, "đồng hồ Apple cho thể thao mạo hiểm" ra Series 11 thay vì Ultra. Hướng cải thiện tiếp là **xếp hạng lại bằng cross-encoder** (chưa làm).
- Trong quá trình làm, bộ đánh giá còn phát hiện chính **đáp án của bộ test bị thiếu**: câu "sạc dự phòng 20000mAh" ban đầu chỉ đánh dấu 3 sản phẩm, trong khi catalogue có 6 sản phẩm như vậy. Đáp án đã được bổ sung.

### 9.6. Đã kiểm thử thủ công trên hệ thống chạy thật

| Trường hợp | Kết quả |
|---|---|
| Khách chưa đăng nhập / đã đăng nhập xem khuyến nghị | Phổ biến + mới / danh sách cá nhân hoá, có lý do |
| SIMILAR, UPSELL, CROSS_SELL cho iPhone 17e | iPhone cùng tầm giá; iPhone 17 (+6,5 triệu, bản cao hơn cùng hãng) rồi máy khác hãng có camera tốt hơn, không kém thông số nào (OPPO Find X9s, Galaxy S26+, Galaxy S25 Ultra); sạc 20W Apple, AirPods 4, AirPods Pro, AirPods Max |
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
- Bộ lọc ốp lưng / kính cường lực theo đời máy (mục 4.4) chỉ bật khi hiển thị, chưa có trong đánh giá offline: chạy thử
  ngày 09/11/2026 với bộ lọc bật trong đánh giá thì λ của MMR chọn trên validation đổi từ 0,7 sang 0,8.
- `LIKE` trong dữ liệu mô phỏng theo giả định "thích = dấu hiệu sẽ mua" (mục 9.1): đo được cơ chế chạy đúng, chưa phải tác dụng trên khách thật.
- Cross-sell "mua chung" chưa có tác dụng trên dữ liệu mẫu (đơn sinh ngẫu nhiên); trên web hiện dựa chủ yếu vào quan hệ phụ kiện theo quy tắc. Luật "khách mua A cũng mua B" chỉ phát huy khi có lịch sử mua thật (đã kiểm chứng trên Amazon, mục 9.1c).

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

---

## 13. Học các công thức: video và tài liệu

Phần này giúp hiểu **mọi công thức trong hệ thống AI**: mỗi công thức nằm ở file nào, nên xem video / đọc tài liệu nào.

> Cách tìm: tra cứu ngày 25/11/2026. Tên video lấy từ kết quả tìm kiếm; **chưa xem hết từng video**, nên xem thử vài phút
> đầu để chọn video hợp trình độ. Link YouTube có thể bị gỡ. Với MMR, HNSW, bootstrap và CBF **không tìm được video phù
> hợp**, nên chỉ có tài liệu đọc (ghi rõ ở từng nhóm).

### 13.1. Bản đồ: công thức → file → học ở đâu

| # | Công thức / khái niệm | Nằm ở | Mục README | Học ở nhóm |
|---|---|---|---|---|
| 1 | Điểm tương tác `r(u,i) = Σ w_t · log(1 + n) · 0,5^(tuổi/h)` (trọng số hành vi, hàm log, giảm theo thời gian) | `features.py` | 4.2 | A (phản hồi ngầm định) |
| 2 | Min-Max, One-Hot, vector sản phẩm 110 chiều | `features.py` | 4.2 | B |
| 3 | Cosine similarity, hồ sơ khách `p_u`, `Score_CBF = cos(p_u, v_i)` | `engine.py` | 4.3 | B |
| 4 | ALS cho phản hồi ngầm định: `c_ui = 1 + a·r`, `x_u = (YᵀY + Yᵀ(C_u−I)Y + λI)⁻¹ YᵀC_u p_u`, fold-in | `als.py` | 4.3 | A |
| 5 | Hybrid `α·S'_CBF + (1−α)·S'_CF` sau Min-Max | `engine.py` | 4.3 | A, B |
| 6 | MMR `λ·Score(j) − (1−λ)·max cos(v_j, v_k)` | `engine.py` | 4.4 | G (chỉ tài liệu đọc) |
| 7 | Lift, confidence (luật mua kèm), item-item kNN | `engine.py`, `evaluation.py` | 4.5, 9.1 | A, B |
| 8 | Precision / Recall / HitRate / NDCG @10, Coverage, Diversity | `evaluation.py` | 9.0 | C |
| 9 | Chia dữ liệu theo thời gian, tinh chỉnh trên validation, khoảng tin cậy bootstrap | `evaluation.py` | 9.1 | C (bootstrap: tài liệu) |
| 10 | Embedding E5, cosine `<=>` trong pgvector, chỉ mục HNSW | `rag.py`, `build_index.py` | 5.2 | D |
| 11 | Reciprocal Rank Fusion `1/(60+hạng_vector) + w/(60+hạng_từ_khoá)` | `rag.py` | 5.2 | E |
| 12 | Hit@K, Recall@K, MRR của tìm kiếm | `eval_retrieval.py` | 9.5 | C |
| 13 | Function Calling, vòng lặp tối đa 6 lượt, giám khảo LLM | `agent.py`, `tools.py`, `eval_chatbot.py` | 5.3, 5.4, 9.4 | F |

### 13.2. Video nên xem (theo thứ tự nên học)

**A. Lọc cộng tác, phân rã ma trận, ALS với phản hồi ngầm định** (công thức 1, 4, 5, 7)

| Video | Học được gì |
|---|---|
| [Recommender Systems Lecture](https://www.youtube.com/watch?v=aScTu5-bBeg) | Bài giảng nền tảng: baseline, SVD, phân rã ma trận, SGD |
| [Matrix Completion for Collaborative Filtering](https://www.youtube.com/watch?v=yXPlhx4xWRQ) | Nhìn lọc cộng tác như bài toán điền ma trận bằng mô hình hạng thấp |
| [Implicit ALS in Python: Build a Recommender from Clicks](https://www.youtube.com/watch?v=TFjk_th3qME) | **Gần nhất với `als.py`**: ALS cho lượt xem / bấm / mua (thư viện `implicit`) |
| [Alternating Least Squares (ALS): Building Your First Production-Ready Recommender System](https://www.youtube.com/watch?v=E6n0-BNsDFE) | ALS ở góc nhìn triển khai thực tế |
| [Implicit Feedback: Techniques for Deployment and Evaluation](https://www.youtube.com/watch?v=P6rmI6uch0g) | Phản hồi ngầm định: triển khai và đánh giá |

Tài liệu đọc kèm (nên đọc nếu muốn hiểu công thức 4): bài gốc **Hu, Koren, Volinsky (2008), "Collaborative Filtering for
Implicit Feedback Datasets"**, đặt ra độ ưa thích `p_ui` và độ tin cậy `c_ui = 1 + a·r` mà `als.py` dùng;
[hướng dẫn ALS của Spark](https://spark.apache.org/docs/3.4.0/ml-collaborative-filtering.html) (tham số `implicitPrefs`,
`alpha`); bản diễn giải [ALS Implicit Collaborative Filtering](https://medium.com/radon-dev/als-implicit-collaborative-filtering-5ed653ba39fe).

**B. Lọc theo nội dung và cosine** (công thức 2, 3, 5, 7): không tìm được video đáng tin trong lần tra cứu này. Đọc
[Content-based filtering (Google Machine Learning)](https://developers.google.com/machine-learning/recommendation/content-based/basics)
và [What is content-based filtering? (IBM)](https://www.ibm.com/think/topics/content-based-filtering). Có thể tự tìm trên
YouTube với từ khoá "content based filtering cosine similarity". Ý chính: so **hồ sơ khách** với **vector sản phẩm** bằng
cosine, không so khách với khách.

**C. Chỉ số đánh giá** (công thức 8, 9, 12)

| Video | Học được gì |
|---|---|
| [Evaluation Measures for Search and Recommender Systems](https://www.youtube.com/watch?v=BD9TkvEsKwM) | Recall@K, MRR, MAP@K, **NDCG@K** kèm code Python: khớp với mục 9.0 và 9.5 |
| [What are Precision@k and Recall@k?](https://www.youtube.com/watch?v=WEJcETfWwOo) | Precision@k và Recall@k cho hệ khuyến nghị |

Đọc thêm: [Pinecone: Evaluation Measures in Information Retrieval](https://www.pinecone.io/learn/offline-evaluation/md/),
[Evaluation Metrics for Recommendation Systems](https://towardsdatascience.com/evaluation-metrics-for-recommendation-systems-an-overview-71290690ecba).
Bootstrap khoảng tin cậy: chưa có video; ý chính là lấy lại mẫu có hoàn lại 1.000 lần rồi lấy khoảng 2,5%–97,5% (mục 9.0).

**D. Embedding, RAG, vector database** (công thức 10)

| Video | Học được gì |
|---|---|
| [Semantic Search, RAG, Embeddings, Vector DB](https://www.youtube.com/watch?v=kmkDD84hq4w) | Tìm kiếm ngữ nghĩa và RAG không dùng framework |
| [Embeddings Explained for Beginners: Build a Local RAG App](https://www.youtube.com/watch?v=9QL9DSUTf3s) | Embedding là gì, tự xây RAG chạy cục bộ (cùng hướng với dự án) |
| [Embeddings & Vector Databases for RAG: Semantic Search Explained](https://www.youtube.com/watch?v=zhv8wuPSWHw) | Vector database và tìm kiếm ngữ nghĩa |

HNSW (chỉ mục của pgvector): chưa có video; đọc [IVFFlat vs. HNSW in pgvector](https://medium.com/@linz07m/ivfflat-vs-hnsw-in-pgvector-a9996c0abe95)
và bài gốc Malkov & Yashunin (2016), "Efficient and robust approximate nearest neighbor search using HNSW".

**E. Tìm kiếm lai và Reciprocal Rank Fusion** (công thức 11; đã đo là kém `vector` ở mục 9.5, nhưng cần hiểu để bảo vệ lựa chọn)

| Video | Học được gì |
|---|---|
| [Hybrid RAG Explained: Dense + BM25 + RRF for Better Search Quality](https://www.youtube.com/watch?v=UgYrGLKyYmI) | Vì sao gộp từ khoá và vector, RRF hoạt động ra sao |
| [Hybrid Search in RAG Explained](https://www.youtube.com/watch?v=D8LmsEOJvPI) | Tìm kiếm lai trong RAG |
| [Reciprocal Rank Fusion in Python: Combine BM25 and Embedding Search](https://www.youtube.com/watch?v=tHTQw-zHxks) | Cài RRF bằng Python |

Hằng số 60 trong công thức RRF là giá trị mặc định của bài gốc Cormack et al. (2009).

**F. Function Calling, vòng lặp agent, đánh giá chatbot** (công thức 13)

| Video | Học được gì |
|---|---|
| [How LLM Tool Use Works](https://www.youtube.com/watch?v=64sytAQHncU) | Khái niệm: mô hình chỉ *yêu cầu* gọi tool, ứng dụng mới chạy |
| [Tool Calling Explained: Turn Your LLM into an AI Agent](https://www.youtube.com/watch?v=KJf7SqPCRXg) | Từ LLM thành agent |
| [How to Connect an LLM to Python Functions (Tool Calling Tutorial)](https://www.youtube.com/watch?v=liV0bfZ5Wu0) | Thực hành nối LLM với hàm Python (như `tools.py`) |
| [Agent Function and Tool calling: How To Evaluate](https://www.youtube.com/watch?v=EfhylWtNb1s) | Đánh giá agent bằng giám khảo LLM (như `eval_chatbot.py`) |

Đọc thêm: [Tool Calling Explained](https://towardsdatascience.com/tool-calling-explained/) (vòng lặp: gửi ngữ cảnh, mô hình
yêu cầu tool, chạy tool, nối kết quả, lặp có giới hạn số vòng, đúng như mục 5.4).

**G. MMR (xếp lại cho đa dạng)** (công thức 6): chưa có video. Đọc
[Diversifying search results with Maximum Marginal Relevance (Elastic)](https://www.elastic.co/search-labs/blog/maximum-marginal-relevance-diversify-results):
chọn lần lượt món có `λ·liên quan − (1−λ)·giống món đã chọn` lớn nhất; λ = 1 là chỉ xét độ liên quan.

### 13.3. Lộ trình học gợi ý (khoảng 6–8 giờ)

1. **Nền tảng khuyến nghị:** nhóm A (2 video đầu) + bài Hu–Koren–Volinsky + nhóm B (đọc Google ML) → hiểu mục 4.2–4.3.
2. **Đọc ngược code:** mở `features.py`, `als.py`, `engine.py`, đối chiếu từng công thức; chạy
   `python -m pytest tests/test_als.py tests/test_engine.py` (test dựng ví dụ tính tay, giúp kiểm tra mình hiểu đúng).
3. **Đánh giá:** nhóm C, rồi tự tính tay ví dụ NDCG ở mục 9.0 (ra 0,46) để chắc chắn hiểu.
4. **Chatbot / RAG:** nhóm D → E → F, đối chiếu mục 5.2–5.4.
5. **Câu hỏi bảo vệ hay gặp:** Vì sao α = 0,6 trên dữ liệu mô phỏng nhưng 0,2 trên Amazon? Vì sao MMR giảm NDCG mà vẫn dùng?
   Vì sao `vector` thắng `hybrid`? Vì sao LLM không được tự thêm giỏ / huỷ đơn? (đáp án ở mục 9.1, 9.1c, 9.5, 5.6).
