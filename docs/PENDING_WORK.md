# Việc còn dở: TechShopping

Cập nhật: **2026-09-25, cuối session 2**. Người dùng dừng phiên sau Checkpoint 2.7.
Chi tiết kỹ thuật đầy đủ (kiến trúc, API, test, quyết định) nằm trong [`CLAUDE_CONTEXT.md`](CLAUDE_CONTEXT.md). File này chỉ liệt kê **những gì chưa xong**, theo thứ tự nên làm.

> **Prompt gợi ý khi mở chat mới:**
> "Đọc `docs/PENDING_WORK.md` và `docs/CLAUDE_CONTEXT.md`, kiểm tra lại code/git/DB xem có khớp không, rồi làm tiếp từ việc đầu tiên chưa xong. Không commit/push nếu tôi chưa yêu cầu."

---

## Trạng thái nhanh

| Hạng mục | Trạng thái |
|---|---|
| Phase 1 – Product (backend) | ✅ Xong, commit `21d2cda` (người dùng tự commit) |
| Phase 2 – Backend 2.1 → 2.5b (User, Auth, JWT, phân quyền, `/users/me`, admin quản lý user) | ✅ Xong, commit **`c9e2d6c`** trên nhánh `quang` |
| 2.6 – Frontend đăng nhập / đăng ký / header | ✅ Xong, **chưa commit** |
| Tái cấu trúc frontend (thư mục theo role, header/footer dùng chung) | ✅ Xong, **chưa commit** |
| 2.7 – Trang tài khoản `customer/account.html` | ✅ Xong và đã báo cáo, **chưa được duyệt, chưa commit** |
| 2.8 – Trang sản phẩm lấy dữ liệu từ API | ❌ **Chưa làm** (checkpoint cuối của Phase 2) |
| Push lên GitHub | ❌ Chưa push lần nào, và chưa được yêu cầu |

---

## Việc 0: Kiểm tra đầu phiên (bắt buộc)

1. Docker Desktop đang chạy → `docker compose up -d`. Cần cả `techshopping-postgres` và `techshopping-redis`.
2. `git branch --show-current` phải là **`quang`**. `git status` phải khớp với danh sách "chưa commit" ở Việc 2.
3. Chạy `cd backend/Tech && ./mvnw clean test` → phải **225 tests, 0 failures**.
4. Nếu có gì khác với ghi chép thì **báo người dùng trước**, không tự đoán.

## Việc 1: Xin duyệt Checkpoint 2.7

2.7 đã làm xong và đã báo cáo, nhưng người dùng dừng phiên trước khi duyệt. Gồm:
- `frontend/customer/account.html`, `js/customer/account.js`, `css/customer/account.css`;
- tên người dùng trên header giờ link tới trang tài khoản (`js/core/main.js`);
- chuyển `.page-hero` từ `products.css` sang `style.css`. Nhờ vậy các trang giỏ hàng / liên hệ / khuyến nghị / dịch vụ có nền xám và khoảng đệm giống trang Sản phẩm. Đây là một **thay đổi giao diện nhỏ**, người dùng có thể muốn hoàn tác;
- chuyển `.form-error` / `.field-error` từ `login.css` sang `style.css`, và thêm `.form-success`.

Kết quả kiểm tra: E2E trên Edge 31/31, hồi quy 32/32. → Hỏi người dùng duyệt hay muốn sửa gì.

## Việc 2: Commit phần frontend (chờ người dùng đồng ý)

**Chưa commit:**
- **đã stage:** toàn bộ 2.6 + tái cấu trúc frontend (đổi tên / chuyển thư mục);
- **chưa stage:**
  - sửa đổi 2.7 trong `frontend/css/style.css`, `css/auth/login.css`, `css/customer/products.css`, `js/core/main.js`;
  - file mới `frontend/customer/account.html`, `js/customer/account.js`, `css/customer/account.css`;
  - `backend/Tech/src/main/resources/application-dev.yml` (thêm CORS cho cổng 5501).
- `docs/` **không commit**; người dùng quyết định để untracked.

Khi được đồng ý:
```bash
git add -A frontend/ backend/Tech/src/main/resources/application-dev.yml
git status && git diff --cached --stat     # kiểm tra: không có docs/, không có file lạ
git commit -m "Frontend: auth pages, role-based structure, shared layout, account page"   # + dòng Co-Authored-By theo quy định của phiên
```
**Không push** nếu người dùng chưa yêu cầu.

## Việc 3: Checkpoint 2.8, danh sách sản phẩm lấy từ API (chưa làm)

Mục tiêu: trang Sản phẩm và mục "Sản phẩm nổi bật" ở trang chủ hiển thị **877 sản phẩm thật** từ backend thay cho 8 sản phẩm giả. **Không sửa backend** (quyết định của người dùng).

### 3.1 `frontend/customer/products.html`
- Bỏ 8 thẻ `.product-card` hardcode.
- **Giữ** `.filter-bar` với các nút `data-category` = `all` / `laptop` / `phone` / `tablet` / `accessory`, và một `.product-grid` rỗng.
- Thêm nút "Xem thêm" trong khối `.view-all` (class có sẵn), và các thông báo đang tải / không có sản phẩm / lỗi.
- Giữ nguyên giao diện.

### 3.2 `frontend/js/customer/products.js`
- `GET /api/v1/categories?size=100` (công khai, **không** gửi token). Map slug của nút lọc sang slug trong DB rồi lấy `categoryId`:

  | Nút (`data-category`) | Slug trong DB |
  |---|---|
  | `laptop` | `laptop` |
  | `phone` | `dien-thoai` |
  | `tablet` | `may-tinh-bang` |
  | `accessory` | `phu-kien` |

  DB còn `dong-ho-thong-minh` và `dong-ho-thoi-trang`: không có nút, chỉ hiện ở "Tất cả". Hai danh mục `E2E Devices` / `E2E Phones` là rác test, có 0 sản phẩm.
- `GET /api/v1/products?categoryId=<id>&isActive=true&page=<n>&size=12&sort=id`, kết quả nằm trong `data.content`, `data.totalPages`.
- Render thẻ theo **đúng markup cũ**: `.product-card` > `.product-image` (img) + `.product-info` gồm:
  - `.product-category` = `categoryName`;
  - `h3` = `name`;
  - `.product-price` = `formatPrice(discountPrice ?? basePrice)`;
  - nút `.add-cart` có `data-name` / `data-price`.
- **Ảnh:** `ProductResponse` không có URL ảnh → với mỗi thẻ gọi `GET /api/v1/products/{id}/images` và lấy ảnh `isPrimary`, không có thì lấy ảnh đầu tiên. Có 116 sản phẩm không có ảnh, và request có thể lỗi → dùng ảnh local theo danh mục: `../assets/images/laptop.png`, `phone.png`, `tablet.png`, `banphim.png`.
- Giữ hành vi `?category=phone` trên URL (các thẻ danh mục ở trang chủ link tới đây).
- "Xem thêm" → tải trang kế tiếp, nối thêm thẻ.
- Sau khi render gọi `setupAddToCart(grid)`. Cần sửa `setupAddToCart` trong `js/core/main.js` để nhận tham số gốc (mặc định `document`), tránh gắn sự kiện 2 lần.

### 3.3 `frontend/index.html`, mục "SẢN PHẨM NỔI BẬT"
- Thay 4 thẻ hardcode bằng 4 sản phẩm đầu tiên từ API, dùng chung hàm render thẻ.
- Chọn cách đơn giản nhất: một helper dùng chung (vd. `js/core/products-ui.js`) nạp ở cả hai trang, **hoặc** hàm render nhỏ trong `js/home.js`. Ghi lựa chọn vào `CLAUDE_CONTEXT.md`.

### 3.4 Không đổi
- Giỏ hàng: vẫn localStorage, lưu theo tên, cho tới Phase 3. Trang khuyến nghị, trang liên hệ.

### 3.5 Kiểm tra
- Backend chạy profile **dev** là được, vì chỉ có GET và DB dev có 877 sản phẩm thật.
- E2E trên Edge (xem `docs/tools/e2e/README.md`):
  - grid hiện 12 thẻ, "Xem thêm" thêm 12;
  - mỗi nút lọc chỉ hiện đúng danh mục; so số lượng với `GET /products?categoryId=`;
  - `?category=phone` chọn sẵn nút;
  - sản phẩm không có ảnh dùng ảnh thay thế;
  - thêm vào giỏ hoạt động với thẻ mới render;
  - trang chủ hiện 4 sản phẩm nổi bật;
  - không có lỗi JS; có ảnh chụp màn hình.
- Kiểm tra số request hợp lý: khoảng 12 request ảnh mỗi trang.

### 3.6 Kết thúc
- Cập nhật `CLAUDE_CONTEXT.md`, báo cáo, **dừng chờ duyệt**.
- Sau 2.8 là **hết Phase 2**: đề xuất commit cuối, viết tổng kết Phase 2, và hỏi người dùng về việc push / merge (xem Việc 4).

## Việc 4: Git sau khi xong Phase 2 (hỏi người dùng)

- Nhánh `quang` chỉ có ở local, chưa có upstream.
- `main` local có `21d2cda` (Phase 1), đang ahead 1 / behind 1 so với `origin/main`; `origin/main` có frontend của HoangPhuoc38 (`f58f92d`).
- Cần hỏi: push `quang` lên GitHub? Tạo PR vào `main`? Ai merge? **Không tự làm.**

## Việc 5: Người dùng cần tự làm (không phải Claude)

1. **Tạo tài khoản ADMIN trong DB dev.** Hiện DB dev có 0 user, nên không ai thêm / sửa / xoá sản phẩm được. Chạy backend một lần với biến môi trường (xem "Hướng dẫn chạy" bên dưới).
2. **Đặt Live Server cổng 5501** (`"liveServer.settings.port": 5501` trong settings của VS Code), vì cổng 5500 bị Oracle `TNSLSNR` chiếm.
3. **Giữ một `JWT_SECRET` cố định** khi chạy dev. Đổi secret thì mọi token cũ mất hiệu lực.

## Việc 6: Tồn đọng và quyết định còn mở (chưa ai yêu cầu làm)

| # | Việc | Ghi chú |
|---|---|---|
| 6.1 | Có đưa script E2E (`docs/tools/e2e/`) vào repo không, ví dụ `frontend/tests/`? | Người dùng chưa trả lời |
| 6.2 | Dọn rác test trong **DB dev**: categories 10–11 `E2E …`, brand 56 `E2E Brand`, products 878–879 đã xoá mềm (+1 ảnh, 1 spec) | Cần người dùng **cho phép** xoá dữ liệu |
| 6.3 | User thử trong **DB test**: `e2e-admin`, `e2e.customer`, `fe.e2e` … `fe.e2e6` | Vô hại; nếu muốn xoá phải được cho phép |
| 6.4 | **Mobile ≤ 700px:** CSS gốc ẩn `.login-btn`, nên trên điện thoại không thấy tên người dùng / nút Đăng xuất / Đăng nhập | Có từ trước; đề xuất sửa nếu người dùng muốn |
| 6.5 | Link "Quên mật khẩu?" ở trang đăng nhập đang là `#` | Backend chưa có luồng reset mật khẩu (chưa có trong kế hoạch) |
| 6.6 | Footer luôn có link "Đăng nhập", kể cả khi đã đăng nhập | Nhỏ; có thể ẩn hoặc đổi thành "Tài khoản" |
| 6.7 | Chưa có **giao diện admin** (`frontend/admin/` đang trống). Admin hiện chỉ thao tác qua Swagger / API | Phase sau |
| 6.8 | `js/customer/recommendation.js` dùng ảnh không tồn tại `../assets/images/smartphone.jpg`; `services.css` / `services.js` rỗng; không có favicon | Có từ trước; thuộc phase sau |
| 6.9 | Chất lượng dữ liệu crawl: brand bị tách ("iPhone (Apple)" / "Apple"), màu khác nhau chỉ ở chữ hoa/thường, cột `rom` trong CSV sai, 116 sản phẩm không có ảnh, 11 sản phẩm giá 0 | Vấn đề dữ liệu, không phải bug code |
| 6.10 | Giới hạn thiết kế auth: access token không thu hồi được trong ≤ 30 phút; không có "đăng xuất mọi thiết bị"; chưa có rate limit cho `/auth/login` | Đã được chấp nhận; rate limit để Phase 10 |

## Việc 7: Sau Phase 2

Phase 3 – **Giỏ hàng (Cart)**: các bảng `carts`, `cart_items`, liên kết `product_variants`.
Theo quy trình của người dùng:
1. **chỉ kiểm tra + báo cáo** (bảng, quan hệ, rủi ro, file dự kiến, câu hỏi cần chốt);
2. chờ duyệt, rồi làm theo checkpoint.

Frontend giỏ hàng hiện lưu localStorage theo **tên** sản phẩm, sẽ phải chuyển sang `variantId`.

---

## Hướng dẫn chạy (tóm tắt)

**Backend (PowerShell, trong `backend\Tech`):**
```powershell
docker compose up -d                          # chạy ở thư mục gốc repo
$env:JWT_SECRET = "<chuỗi ngẫu nhiên ≥ 32 ký tự, giữ cố định>"
# Chỉ lần đầu, để tạo ADMIN (mật khẩu ≥ 8 ký tự):
$env:ADMIN_EMAIL = "admin@lahy.vn"; $env:ADMIN_USERNAME = "admin"; $env:ADMIN_PASSWORD = "<mật khẩu của bạn>"
.\mvnw.cmd spring-boot:run                    # http://localhost:8080, Swagger: /swagger-ui/index.html
```

**Frontend:** mở `frontend/index.html` bằng Live Server (cổng **5501**). Trang đăng nhập: `frontend/auth/login.html`.
