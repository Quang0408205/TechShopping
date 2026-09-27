# Kiểm thử frontend trên trình duyệt thật (headless Edge)

Các script dùng trong session 2 để kiểm tra frontend. Chúng được chép từ scratchpad tạm sang đây để không bị mất. Từ commit `0b720de`, `docs/` đã nằm trong git.
Yêu cầu: Node 24 (có sẵn `fetch` và `WebSocket`) và Microsoft Edge tại `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`.
Script điều khiển Edge qua Chrome DevTools Protocol; không cần cài thêm gói npm nào.

| File | Nội dung |
|---|---|
| `static-server.mjs` | Server tĩnh tối giản, thay cho Live Server. `node static-server.mjs <thư mục gốc> <port>` |
| `e2e-frontend-layout.mjs` | 32 kiểm tra: header/footer dùng chung, menu active, link theo `SITE_ROOT`, đăng ký, đăng nhập với `?redirect`, đăng xuất, chặn open redirect, không thiếu file, không lỗi JS |
| `e2e-account.mjs` | 31 kiểm tra trang `customer/account.html` (2.7). Dùng CDP `Fetch` để giả lập API 404/403 |
| `e2e-products.mjs` | 48 kiểm tra (thêm kiểm tra chuyển khoá `lahy_*` → `poy_*`) trang Sản phẩm + lưới nổi bật trang chủ (2.8). Chạy với backend profile **dev**, chỉ gửi GET. Số liệu mong đợi được lấy thẳng từ API; dùng CDP `Fetch` để giả lập ảnh rỗng, tên sản phẩm chứa mã độc, lỗi 500. Không đăng ký user |
| `e2e-product-detail.mjs` | 39 kiểm tra trang chi tiết sản phẩm (A2). Chạy với backend profile **dev**; dùng CDP `Fetch` để giả lập nhiều phiên bản / ảnh, chuỗi chứa mã độc, lỗi 500. Không đăng ký user |
| `e2e-admin.mjs` | 53 kiểm tra khu `admin/` (B1, dữ liệu mẫu): 3 vai trò, phân quyền menu / trang, thử sửa DOM, lưu thay đổi, khoá tài khoản, form thêm mới, XSS, mobile. **Không cần backend**, chỉ cần static server |

## Cách chạy

Chạy bằng Git Bash, từ gốc repo `TechShopping/`.

```bash
# 1. Docker phải đang chạy (postgres + redis)
docker compose up -d

# 2. Backend trên profile TEST (không tạo tài khoản trong DB dev), cho phép origin :5501
cd backend/Tech
JWT_SECRET="$(head -c 48 /dev/urandom | base64)" ./mvnw -q spring-boot:run \
  -Dspring-boot.run.profiles=test \
  "-Dspring-boot.run.arguments=--app.cors.allowed-origins=http://127.0.0.1:5501" &
cd ../..

# 3. Phục vụ GỐC REPO trên :5501, nên các trang nằm dưới /frontend/... giống Live Server mở cả workspace
node docs/tools/e2e/static-server.mjs "$(pwd)" 5501 &

# 4. Chạy test. Mỗi lần chạy sẽ ĐĂNG KÝ user mới, nên phải truyền tên chưa dùng
E2E_USER=fe.e2e17 node docs/tools/e2e/e2e-frontend-layout.mjs
E2E_USER=fe.e2e18 node docs/tools/e2e/e2e-account.mjs <thư-mục-lưu-ảnh>

# 5. Trang sản phẩm: chạy backend profile DEV (bỏ -Dspring-boot.run.profiles=test), cần 877 sản phẩm thật
node docs/tools/e2e/e2e-products.mjs <thư-mục-lưu-ảnh>
node docs/tools/e2e/e2e-product-detail.mjs <thư-mục-lưu-ảnh>

# 6. Khu admin (B1): chỉ cần static server, không cần backend
node docs/tools/e2e/e2e-admin.mjs <thư-mục-lưu-ảnh>
```

**Nếu cổng 5501 đã bị chiếm** (ví dụ bởi `agy.exe` của Antigravity): phục vụ trên cổng khác rồi truyền `E2E_ORIGIN`.
- Profile **dev** (từ session 4) chấp nhận mọi cổng `localhost` / `127.0.0.1`, nên **không cần** `--app.cors.allowed-origins`.
- Profile **test** vẫn cần override, vì `application-test.yml` không có origin nào.
```bash
node docs/tools/e2e/static-server.mjs "$(pwd)" 5503 &
# backend profile test: --app.cors.allowed-origins=http://127.0.0.1:5503
E2E_ORIGIN=http://127.0.0.1:5503 E2E_USER=fe.e2e17 node docs/tools/e2e/e2e-frontend-layout.mjs
```

Dừng server sau khi chạy xong (PowerShell):
`foreach ($p in 8080,5501) { $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue; if ($c) { Stop-Process -Id $c.OwningProcess -Force } }`

## Lưu ý

- Tên user đã dùng trong DB test: `fe.e2e`, `fe.e2e2` … `fe.e2e16`. Lần tới dùng từ `fe.e2e17` trở đi.
- Trên DB test (không có danh mục), `?category=phone` được đưa về "Tất cả". Vì vậy kiểm tra trang sản phẩm trong `e2e-frontend-layout.mjs` chỉ xác nhận bộ lọc đã sẵn sàng; phần lọc thật do `e2e-products.mjs` kiểm trên DB dev.
- `e2e-account.mjs` đổi mật khẩu của user thử thành `MatkhauMoi@456`.
- Nếu Redis còn sót key `auth:*` sau khi chạy, thu hồi theo từng user:
  `for h in $(docker exec techshopping-redis redis-cli smembers auth:user-refresh:<id>); do docker exec techshopping-redis redis-cli del auth:refresh:$h; done; docker exec techshopping-redis redis-cli del auth:user-refresh:<id>`
- Heredoc của Git Bash nuốt dấu `\` trong đường dẫn Windows. Khi viết script mới, dùng `/` trong đường dẫn.
