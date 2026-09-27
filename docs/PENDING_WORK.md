# Việc còn dở: TechShopping

Cập nhật: **2026-09-27, session 4**. Đã sửa lỗi "không load được sản phẩm" (Việc 1, cách b), B1 đã được duyệt, và toàn bộ phần chưa commit đã được commit theo yêu cầu người dùng (chưa push). Việc tiếp theo: **Việc 2**.
Chi tiết kỹ thuật đầy đủ (kiến trúc, API, test, quyết định) nằm trong [`CLAUDE_CONTEXT.md`](CLAUDE_CONTEXT.md). File này chỉ liệt kê **những gì chưa xong**, theo thứ tự nên làm.

> **Prompt gợi ý khi mở chat mới:**
> "Đọc `docs/PENDING_WORK.md` và `docs/CLAUDE_CONTEXT.md`, kiểm tra lại code/git/DB xem có khớp không, rồi làm tiếp từ việc đầu tiên chưa xong. Không commit/push nếu tôi chưa yêu cầu."

---

## Trạng thái nhanh

| Hạng mục | Trạng thái |
|---|---|
| Phase 1 – Product (backend) | ✅ Xong, commit `21d2cda` |
| Phase 2 – Backend 2.1 → 2.5b | ✅ Xong, commit `c582eee` |
| 2.6, tái cấu trúc frontend, 2.7 | ✅ Xong, người dùng đã commit và push (`115954b`, `0b720de`) |
| CP0 – giải nén frontend tham chiếu | ✅ Xong, đã duyệt |
| R – đổi tên LAHY → **POY** | ✅ Xong, đã duyệt, đã commit (session 4) |
| 2.8 mở rộng – trang sản phẩm từ API | ✅ Xong, đã duyệt, đã commit (session 4) |
| A2 – trang chi tiết sản phẩm (API thật) | ✅ Xong, đã duyệt, đã commit (session 4) |
| Đổi khoá localStorage `lahy_*` → `poy_*` | ✅ Xong, đã commit (session 4) |
| **Phase 2** | ✅ Hoàn tất |
| B1 – khu `admin/` chạy dữ liệu mẫu | ✅ Xong, **đã duyệt** (session 4), đã commit |
| Lỗi "không load được sản phẩm" | ✅ Đã sửa (cách b: CORS dev mọi cổng local), đã duyệt, đã commit |
| Commit tất cả phần trên | ✅ Session 4, dưới tài khoản git của người dùng, **chưa push** |
| **Chuyển sang dùng toàn bộ frontend mới** (quyết định của người dùng 2026-09-27) | ⏳ **Việc tiếp theo**, xem Việc 2 |
| B2 – nối `admin/users`, `admin/products` với API thật | ❌ Chưa làm |

---

## Việc 0: Kiểm tra đầu phiên (bắt buộc)

1. Docker Desktop đang chạy → `docker compose up -d`. Cần cả `techshopping-postgres` và `techshopping-redis`.
2. `git branch --show-current` phải là **`quang`**, HEAD là commit session 4 (xem `CLAUDE_CONTEXT.md` §9, `git log -1`), hơn `origin/quang` 1 commit nếu người dùng chưa push. `git status` phải sạch, trừ những gì đang làm dở của Việc 2.
3. `cd backend/Tech && ./mvnw clean test` → phải **229 tests, 0 failures**.
4. Kiểm tra cổng: `Get-NetTCPConnection -LocalPort 8080,5500,5501 -State Listen`.
   - Cổng 5500 do Oracle (`TNSLSNR`) chiếm, cổng 5501 do Antigravity (`agy.exe`) chiếm. Từ session 4 điều này không còn ảnh hưởng: CORS dev chấp nhận mọi cổng local.
5. Nếu có gì khác với ghi chép thì **báo người dùng trước**, không tự đoán.

## Việc 1: Sửa lỗi "không load được sản phẩm" ✅ XONG (session 4)

- Nguyên nhân là môi trường: backend không chạy, và CORS dev chỉ cho cổng 5500/5501, hai cổng này bị Oracle và Antigravity chiếm (`CLAUDE_CONTEXT.md` §7 #22).
- Người dùng chọn **cách (b)**, đã duyệt:
  - `application-dev.yml` dùng `allowed-origin-patterns` `http://localhost:[*]` và `http://127.0.0.1:[*]`, nên web mở từ cổng local nào cũng gọi được API;
  - prod vẫn chỉ nhận danh sách chính xác.
- `api.js`: khi chạy local mà không gọi được API, trang báo "Không thể kết nối tới máy chủ (localhost:8080). Hãy kiểm tra backend đã chạy chưa."
- Kết quả: 229 tests; E2E sản phẩm 48/48 từ cổng 5503; preflight từ `evil.example` vẫn bị 403.
- Người dùng đã cài Live Server. **Nên đặt `"liveServer.settings.port"` sang một cổng trống** (ví dụ 5510, hoặc `0` để lấy cổng ngẫu nhiên), vì mặc định là 5500, cổng của Oracle.

## Việc 2: Chuyển sang dùng TOÀN BỘ frontend mới (quyết định của người dùng, 2026-09-27)

**Người dùng yêu cầu:** phiên sau dùng **hết** frontend mới (`frontend-reference/frontend/`, giải nén từ `frontend_update.zip`), thay cho cách merge từng phần như session 3.

**Các quyết định cũ bị thay đổi** (sau khi làm xong phải cập nhật `CLAUDE_CONTEXT.md` §8 và §10.5):
- **Q2 "giữ giao diện gốc"** không còn áp dụng. Dùng giao diện mới:
  - design tokens trong `style.css`, màu nhấn đồng, bo góc;
  - header kính mờ, scroll reveal;
  - trang chủ mới (thứ tự section mới, lưới "Gợi ý cho bạn");
  - chatbot nổi.
- **Q6** không còn áp dụng: đưa luôn các trang giỏ hàng, thanh toán, đơn hàng, chi tiết đơn, khuyến nghị, chatbot, thanh bên trang tài khoản. Các trang này **chạy dữ liệu mẫu** cho tới khi có backend Phase 3–9.

**Bắt buộc giữ lại, không được mất khi thay** (bản mới chạy 100% dữ liệu giả, còn bản hiện tại đã nối API thật):
1. **Nối API thật đã có:**
   - đăng nhập / đăng ký (2.6), trang tài khoản (2.7): 5 file `api.js`, `layout.js`, `login.js`, `register.js`, `account.js` vốn đã giống hệt nhau giữa hai bản, nên chỉ cần giữ;
   - **trang sản phẩm (2.8)** và **chi tiết sản phẩm (A2)**: bản mới đọc catalogue giả (`mock-data.js`), nên phải nối lại API thật. Dùng lại logic của `js/customer/products.js`, `product-detail.js` và `js/core/ui.js` hiện tại: `fetchCategoryMaps`, lọc thương hiệu theo danh mục, ảnh qua `/images`, bảng thông số ghép từ dữ liệu thật;
   - lưới "Sản phẩm nổi bật" ở trang chủ lấy từ API.
2. **Khoá `poy_*`** và đoạn chuyển khoá cũ trong `api.js`. Bản mới vẫn dùng `lahy_*` ở `cart-store.js`, `mock-data.js`, `staff-auth.js`, `mock-staff-data.js`, nên phải đổi hết sang `poy_*`.
3. **`escapeHtml` cho mọi dữ liệu đưa vào `innerHTML`**: bản mới hầu như không escape, dễ bị XSS với dữ liệu thật.
4. **Giỏ hàng:**
   - `cart-store.js` của bản mới tra catalogue giả theo `productId`, nên sẽ **không thêm được sản phẩm thật** vào giỏ;
   - **Người dùng đã chọn (session 4): lưu snapshot** `{productId, variantId, name, price, image, quantity}` (lấy từ API);
   - giỏ cũ dạng `{name, price, quantity}` phải được chuyển đổi, và phải báo người dùng.
5. **Khu `admin/`:** bản B1 hiện tại chính là bản mới đã sửa (phiên `poy_staff_auth` cùng dạng `poy_auth`, kiểm tra lại tài khoản mỗi trang, escape, không phụ thuộc catalogue giả, không inline style). **Người dùng đã chọn (session 4): giữ B1, B1 đã duyệt.** Chỉ thêm `admin/users` và `admin/products` (sẽ nối API thật ở B2).
6. **Thiếu / giả cần xử lý:**
   - trang quên / đặt lại mật khẩu của bản mới báo "đã gửi email" trong khi backend chưa có chức năng này. **Người dùng đã chọn (session 4): bỏ hai trang này**, link "Quên mật khẩu?" giữ là `#`;
   - thông số sản phẩm và đánh giá giả (`estimateRatingDistribution`) không dùng với dữ liệu thật (rating đều bằng 0).

**Cách làm đề xuất (theo checkpoint, mỗi bước dừng chờ duyệt):**
1. ~~Hỏi người dùng xác nhận phạm vi các mục 4, 5, 6~~: ✅ đã trả lời ở session 4 (xem trên).
2. ~~Commit trước khi chép đè~~: ✅ đã commit ở session 4 (Việc 3).
3. Chép frontend mới vào `frontend/` (giữ `admin/` của B1), rồi nối lại lần lượt: chuyển khoá `poy_*` → escape → trang sản phẩm + chi tiết + trang chủ (API thật) → giỏ hàng.
4. Kiểm tra: chạy lại các E2E trong `docs/tools/e2e/`. Các script cần sửa selector cho markup mới; ví dụ bộ lọc giờ là radio, không còn nút.
5. Cập nhật `CLAUDE_CONTEXT.md`: §1 (cấu trúc, thứ tự script: bản mới nạp thêm `mock-data.js`, `cart-store.js`, `chatbot.js`), §8 (quyết định mới), §10.5.

## Việc 2b: Checkpoint B1 (khu `admin/` chạy dữ liệu mẫu) ✅ ĐÃ DUYỆT (session 4)

Người dùng giữ `admin/` của B1 khi chuyển sang frontend mới và đã duyệt B1.

Chi tiết: `CLAUDE_CONTEXT.md` §1 ("Admin area") và §6.
- Gồm: đăng nhập nội bộ và 8 trang (tổng quan, bảo hành / bảo trì / đổi trả, ticket hỗ trợ, đơn chi nhánh, báo cáo doanh thu, nhân viên, chi nhánh, lịch sử chatbot); phân quyền theo 3 vai trò; phiên `poy_staff_auth`.
- Mở: `frontend/admin/login.html`. Tài khoản thử:
  - `admin@poy.vn` / `admin123`;
  - `lan.quanly@poy.vn` / `quanly123`;
  - `hoa.nv@poy.vn` / `nhanvien123`.
- Kết quả: E2E admin 53/53 (`docs/tools/e2e/e2e-admin.mjs`, không cần backend); hồi quy phía khách hàng 48 + 39 + 32 + 31 đều đạt.
- Câu hỏi phụ cho người dùng: có muốn thêm link "Khu nội bộ" ở footer trỏ tới `admin/login.html` không?

## Việc 3: Commit ✅ XONG (session 4)

- Theo yêu cầu, đã commit dưới tài khoản git của người dùng (`Quang0408205`), **không** thêm dòng ghi công Claude.
- Nội dung: R, 2.8, A2, đổi khoá `poy_*`, B1 (kể cả xoá `frontend/admin/.gitkeep`), phần sửa CORS dev của Việc 1 và `docs/` (kể cả 3 script E2E mới).
- **Chưa push.** Chỉ push khi người dùng yêu cầu.
- **Tuyệt đối không commit:** `frontend-reference/` và `frontend_update.zip` (đã nằm trong `.git/info/exclude`).

## Việc 4: Checkpoint B2, nối khu admin với API thật (sau khi B1 được duyệt)

Chi tiết: `CLAUDE_CONTEXT.md` §10.2 và §10.5.
- `admin/login.html` gọi `POST /api/v1/auth/login` thật, chỉ nhận tài khoản có role STAFF hoặc ADMIN. Bỏ các tài khoản mẫu.
- `api.js` chọn khoá lưu trữ theo khu vực: trang `admin/` dùng `poy_staff_auth`, trang khác dùng `poy_auth`.
- `admin/users.html` nối `/api/v1/admin/users` (tìm kiếm, khoá / mở, gán role, xoá mềm, lỗi 409). `admin/products.html` nối CRUD catalogue.
- Vai trò EMPLOYEE / BRANCH_MANAGER và chi nhánh vẫn lấy từ dữ liệu mẫu tới Phase 7.
- **Điều kiện trước:** người dùng tạo tài khoản ADMIN trong DB dev (Việc 6.1).

## Việc 5: Phase 3 – Giỏ hàng (Cart)

Các bảng `carts`, `cart_items`, liên kết `product_variants`. Theo quy trình của người dùng:
1. **chỉ kiểm tra + báo cáo** (bảng, quan hệ, rủi ro, file dự kiến, câu hỏi cần chốt);
2. chờ duyệt, rồi làm theo checkpoint.

Giỏ hàng frontend đang lưu `poy_cart` theo **tên** sản phẩm, sẽ chuyển sang `variantId`. Không dùng `cart-store.js` của bản tham chiếu, vì nó tra catalogue mock.

## Việc 6: Người dùng cần tự làm (không phải Claude)

1. **Tạo tài khoản ADMIN trong DB dev.** DB dev vẫn có 0 user. Chạy backend một lần với `ADMIN_EMAIL` / `ADMIN_USERNAME` / `ADMIN_PASSWORD` (xem bên dưới). Cần cho B2.
2. **Đặt cổng cho Live Server** (đã cài): `"liveServer.settings.port": 5510` (hoặc `0`) trong settings của VS Code, vì mặc định 5500 là cổng của Oracle. Cổng nào cũng được, vì CORS dev đã chấp nhận mọi cổng local.
3. **Giữ một `JWT_SECRET` cố định** khi chạy dev. Đổi secret thì mọi token cũ mất hiệu lực.

## Việc 7: Tồn đọng và quyết định còn mở (chưa ai yêu cầu làm)

| # | Việc | Ghi chú |
|---|---|---|
| 7.1 | Dọn rác test trong **DB dev**: categories 10–11 `E2E …`, brand 56 `E2E Brand`, products 878–879 đã xoá mềm | Cần người dùng **cho phép** xoá dữ liệu |
| 7.2 | User thử trong **DB test**: `e2e-admin`, `e2e.customer`, `fe.e2e` … `fe.e2e16` (user tiếp theo: `fe.e2e17`) | Vô hại. Redis còn khoá của `fe.e2e8` (81) và `quang4805` (82, tài khoản của người dùng): để nguyên |
| 7.3 | **Mobile ≤ 700px:** `.login-btn` bị ẩn; menu mobile bị cắt chữ đầu ("ang chủ") | Có từ trước; đề xuất sửa nếu người dùng muốn |
| 7.4 | Link "Quên mật khẩu?" đang là `#` | Backend chưa có luồng reset; **không** merge trang mock của bản tham chiếu |
| 7.5 | Footer luôn có link "Đăng nhập", kể cả khi đã đăng nhập | Nhỏ |
| 7.6 | `alert()` còn trong `cart.js`, `contact.js`, `recommendation.js` | Thay bằng `showToast` / modal khi làm các phase đó |
| 7.7 | `recommendation.js` dùng ảnh không tồn tại `smartphone.jpg`; `services.css` / `services.js` rỗng; không có favicon | Có từ trước |
| 7.8 | Chất lượng dữ liệu crawl | brand bị tách ("iPhone (Apple)" / "Apple"); rating, tồn kho, mô tả, thông số đều trống; 116 sản phẩm không có ảnh; 11 sản phẩm giá 0 |
| 7.9 | Giới hạn thiết kế auth (token ≤ 30 phút, chưa có rate limit) | Đã chấp nhận; rate limit để Phase 10 |
| 7.10 | Trang Sản phẩm: 2 danh mục đồng hồ (195 sản phẩm) chưa có nút lọc riêng | Dễ thêm nếu người dùng muốn |
| 7.11 | ~~Q6~~ | Không còn áp dụng: người dùng chọn dùng toàn bộ frontend mới (Việc 2) |
| 7.12 | ~~`frontend/admin/.gitkeep`~~ | Đã xoá trong commit session 4 |

---

## Hướng dẫn chạy (tóm tắt)

**Backend (PowerShell, trong `backend\Tech`):**
```powershell
docker compose up -d                          # chạy ở thư mục gốc repo
$env:JWT_SECRET = "<chuỗi ngẫu nhiên ≥ 32 ký tự, giữ cố định>"
# Chỉ lần đầu, để tạo ADMIN (mật khẩu ≥ 8 ký tự):
$env:ADMIN_EMAIL = "admin@poy.vn"; $env:ADMIN_USERNAME = "admin"; $env:ADMIN_PASSWORD = "<mật khẩu của bạn>"
.\mvnw.cmd spring-boot:run                    # http://localhost:8080, Swagger: /swagger-ui/index.html
# Profile dev chấp nhận web ở mọi cổng localhost / 127.0.0.1, không cần override CORS.
```

**Frontend:**
- Mở `frontend/index.html` qua HTTP (Live Server, cổng nào cũng được, ví dụ 5510). Mở file trực tiếp (`file://`) sẽ không chạy được.
- Trang đăng nhập khách: `frontend/auth/login.html`. Khu nội bộ: `frontend/admin/login.html`.
