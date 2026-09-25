# Hướng dẫn khởi động TechShopping

## 1. Khởi động Docker

Mở PowerShell tại thư mục gốc project:

```powershell
cd C:\Users\username\Desktop\TechShopping
docker compose up -d
```

Kiểm tra:

```powershell
docker ps
```

Đảm bảo PostgreSQL và Redis đang ở trạng thái `Up`.

---

## 2. Khởi động Backend

Mở PowerShell mới:

```powershell
cd C:\Users\username\Desktop\TechShopping\backend\Tech
```

Thiết lập JWT Secret:

```powershell
$env:JWT_SECRET="TechShopping_JWT_Secret_2026_Local_123456789"
```

Khởi động Backend:

```powershell
.\mvnw.cmd -q spring-boot:run "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.arguments=--app.cors.allowed-origins=http://127.0.0.1:5501"
```

Chờ đến khi Spring Boot khởi động thành công.

---

## 3. Khởi động Frontend

Mở PowerShell mới:

```powershell
cd C:\Users\username\Desktop\TechShopping
```

Chạy static server:

```powershell
node docs/tools/e2e/static-server.mjs "$(Get-Location)" 5501
```

---

## 4. Mở Frontend

Mở trình duyệt:

```text
http://127.0.0.1:5501/frontend/index.html
```

Backend:

```text
http://localhost:8080
```

---

## 5. Chạy kiểm thử Frontend

Mở PowerShell mới tại thư mục gốc:

```powershell
cd C:\Users\Quang\Desktop\TechShopping
```

### Kiểm thử Layout

```powershell
$env:E2E_USER="fe.e2e7"
node docs/tools/e2e/e2e-frontend-layout.mjs
```

Kết quả hiện tại:

```text
32/32 checks passed
```

### Kiểm thử Account

Tạo thư mục screenshot nếu chưa có:

```powershell
New-Item -ItemType Directory -Force ".\e2e-screenshots"
```

Sau đó dùng username mới:

```powershell
$env:E2E_USER="fe.e2e9"
node docs/tools/e2e/e2e-account.mjs ".\e2e-screenshots"
```

---

## 6. Dừng hệ thống

Tại các cửa sổ đang chạy Backend và Frontend, nhấn:

```text
Ctrl + C
```

Sau đó dừng Docker:

```powershell
cd C:\Users\username\Desktop\TechShopping
docker compose down
```
