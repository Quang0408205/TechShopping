# Kiểm thử frontend trên trình duyệt thật (headless Edge)

Các script dùng trong session 2 để kiểm tra frontend. Chúng được chép từ scratchpad tạm sang đây để không bị mất. `docs/` không nằm trong git.
Yêu cầu: Node 24 (có sẵn `fetch` và `WebSocket`) và Microsoft Edge tại `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`.
Script điều khiển Edge qua Chrome DevTools Protocol; không cần cài thêm gói npm nào.

| File | Nội dung |
|---|---|
| `static-server.mjs` | Server tĩnh tối giản, thay cho Live Server. `node static-server.mjs <thư mục gốc> <port>` |
| `e2e-frontend-layout.mjs` | 32 kiểm tra: header/footer dùng chung, menu active, link theo `SITE_ROOT`, đăng ký, đăng nhập với `?redirect`, đăng xuất, chặn open redirect, không thiếu file, không lỗi JS |
| `e2e-account.mjs` | 31 kiểm tra trang `customer/account.html` (2.7). Dùng CDP `Fetch` để giả lập API 404/403 |

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
E2E_USER=fe.e2e7 node docs/tools/e2e/e2e-frontend-layout.mjs
E2E_USER=fe.e2e8 node docs/tools/e2e/e2e-account.mjs <thư-mục-lưu-ảnh>
```

Dừng server sau khi chạy xong (PowerShell):
`foreach ($p in 8080,5501) { $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue; if ($c) { Stop-Process -Id $c.OwningProcess -Force } }`

## Lưu ý

- Tên user đã dùng trong DB test: `fe.e2e`, `fe.e2e2` … `fe.e2e6`. Lần tới dùng từ `fe.e2e7` trở đi.
- `e2e-account.mjs` đổi mật khẩu của user thử thành `MatkhauMoi@456`.
- Nếu Redis còn sót key `auth:*` sau khi chạy, thu hồi theo từng user:
  `for h in $(docker exec techshopping-redis redis-cli smembers auth:user-refresh:<id>); do docker exec techshopping-redis redis-cli del auth:refresh:$h; done; docker exec techshopping-redis redis-cli del auth:user-refresh:<id>`
- Heredoc của Git Bash nuốt dấu `\` trong đường dẫn Windows. Khi viết script mới, dùng `/` trong đường dẫn.
