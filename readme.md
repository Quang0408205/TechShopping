# TechShopping: hướng dẫn chạy dự án

Hệ thống quản lý và khuyến nghị mua sắm thiết bị công nghệ (thương hiệu **POY**):
- **backend:** Spring Boot 4 + PostgreSQL 16 + Redis 7;
- **frontend:** HTML/CSS/JS thuần.

Toàn bộ chạy bằng **Docker**: cài xong chỉ cần mở Docker Desktop là web chạy.

| Thành phần | Địa chỉ |
|---|---|
| Website | http://localhost:5510 |
| Khu quản trị nội bộ | http://localhost:5510/admin/login.html |
| API | http://localhost:8080/api/v1 |
| Tài liệu API (Swagger) | http://localhost:8080/swagger-ui/index.html |

---

## 1. Cần cài trước

1. **Git**: https://git-scm.com/downloads
2. **Docker Desktop**: https://www.docker.com/products/docker-desktop/
   - Cài xong, mở Docker Desktop và chờ tới khi nó báo **Engine running**.

Không cần cài Java, Maven hay Node để chạy web; Docker lo hết.

---

## 2. Chạy lần đầu (copy từng khối lệnh vào PowerShell)

### Bước 1: Tải code về

```powershell
git clone https://github.com/Quang0408205/TechShopping.git
cd TechShopping
```

Nhánh `main` luôn có bản chạy được mới nhất.

### Bước 2: Tạo file cấu hình `.env`

Lệnh dưới đây chép `.env.example` thành `.env` và tự sinh một `JWT_SECRET` ngẫu nhiên:

```powershell
Copy-Item .env.example .env
$secret = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))
(Get-Content .env) -replace '^JWT_SECRET=.*', "JWT_SECRET=$secret" | Set-Content .env -Encoding ascii
```

- File `.env` chỉ nằm trên máy bạn: nó **không** được đưa lên git.
- **Không đổi `JWT_SECRET` sau này**, nếu không mọi phiên đăng nhập cũ sẽ mất hiệu lực.

### Bước 3: Khởi động

```powershell
docker compose up -d --build
```

- Lần đầu mất vài phút, vì phải tải image và build backend. Các lần sau nhanh hơn nhiều.
- Kiểm tra: cả 4 container `techshopping-postgres`, `techshopping-redis`, `techshopping-backend`, `techshopping-frontend` phải ở trạng thái `Up`.
  ```powershell
  docker compose ps
  ```
- Backend cần thêm khoảng 10–20 giây sau khi container chạy. Xem log cho tới khi thấy dòng `Started TechApplication`, rồi nhấn `Ctrl + C` để thoát log (backend vẫn chạy):
  ```powershell
  docker compose logs -f backend
  ```

### Bước 4: Mở web

Mở trình duyệt vào **http://localhost:5510**.

> Database dùng schema trong `database/techshopping.sql`. Docker không tự nạp catalog để bạn có thể kiểm tra schema trước, sau đó chủ động import dữ liệu crawl. Chưa có tài khoản nào: tự đăng ký trên web, hoặc tạo ADMIN ở Bước 5.
>
> Schema init chỉ chạy tự động khi PostgreSQL khởi tạo volume `postgres_data` mới. Volume/database đã tồn tại sẽ không tự được cập nhật schema khi chạy lại Docker; dùng migration cho thay đổi schema và không xóa volume nếu chưa sao lưu dữ liệu.

### Crawl và import catalogue TGDĐ

Trước hết khởi động PostgreSQL và chờ healthy:

```powershell
docker compose up -d postgres
docker compose ps
```

Sau đó chạy từ thư mục gốc dự án trong PowerShell:

```powershell
python -m pip install -r requirements.txt
python -m playwright install chromium
python Raw_data/crawler.py
python Raw_data/clean_data.py
```

Trên macOS/Linux, các lệnh tương đương là:

```sh
python -m pip install -r requirements.txt
python -m playwright install chromium
python Raw_data/crawler.py
python Raw_data/clean_data.py
```

`crawler.py` lưu CSV cạnh các script trong `Raw_data/` và lấy thêm mô tả, ảnh, thông số kỹ thuật
và số đánh giá từ trang chi tiết. Quá trình có nghỉ giữa các trang; danh mục tải chưa hết hoặc trang
chi tiết lỗi được ghi trạng thái trong CSV để nhận biết. `import_catalog.py` kiểm tra schema trước,
chạy trong một transaction và có thể chạy lại để cập nhật danh mục, thương hiệu, sản phẩm, phiên bản,
ảnh, thông số và thuộc tính mà không tạo lặp ảnh/thông số đã nhập.

Khi đã kiểm tra CSV, import vào database PostgreSQL Docker của TechShopping (`techshopping`):

```powershell
$env:PGHOST = "localhost"
$env:PGPORT = "5432"
$env:PGUSER = "postgres"
$env:PGPASSWORD = "postgres"
$env:PGDATABASE = "techshopping"
python Raw_data/import_catalog.py
```

Trên macOS/Linux, thay các dòng đặt biến môi trường phía trên bằng:

```sh
PGHOST=localhost PGPORT=5432 PGUSER=postgres PGPASSWORD=postgres \
PGDATABASE=techshopping python Raw_data/import_catalog.py
```

Đừng import chồng lên catalog seed cũ: khóa nhận diện catalog cũ khác với importer và có thể tạo sản phẩm trùng. File `database/docker-init/03_seed_catalog.sql` là dump cũ, không còn được Docker nạp và không khớp schema hiện tại.

Crawler công khai không thể lấy tồn kho thực theo chi nhánh, người dùng, đơn hàng hay thanh toán;
những bảng vận hành này không được tự điền bằng dữ liệu đoán.

### Bước 5 (tuỳ chọn): Tạo tài khoản ADMIN đầu tiên

Mở file `.env`, điền 3 dòng sau (mật khẩu ít nhất 8 ký tự):

```
ADMIN_EMAIL=admin@poy.vn
ADMIN_USERNAME=admin
ADMIN_PASSWORD=MatKhauCuaBan123
```

Rồi chạy:

```powershell
docker compose up -d backend
```

- ADMIN chỉ được tạo **một lần**, khi database chưa có ADMIN nào (database đã có ADMIN thì 3 dòng này bị bỏ qua). Sau đó có thể xoá 3 dòng này khỏi `.env`.
- Tài khoản khách hàng thì tự đăng ký trên web (nút **Đăng nhập** → **Đăng ký**).
- **Khu quản trị nội bộ:** http://localhost:5510/admin/login.html, đăng nhập bằng email hoặc tên đăng nhập của ADMIN.
  - Chỉ tài khoản có quyền **STAFF** hoặc **ADMIN** vào được.
  - ADMIN cấp quyền cho người khác ở trang **Người dùng & phân quyền**: người đó tự đăng ký trên web trước, sau đó ADMIN tick **Nhân viên** cho tài khoản đó.

---

## 3. Tính năng hiện có

| Khu vực | Tính năng | Dữ liệu |
|---|---|---|
| Khách hàng | Đăng ký, đăng nhập, trang tài khoản (hồ sơ, địa chỉ, đổi mật khẩu) | **Thật** (API) |
| Khách hàng | Trang chủ, danh sách sản phẩm (lọc, tìm kiếm, phân trang), chi tiết sản phẩm, trang khuyến nghị | **Thật** (877 sản phẩm) |
| Khách hàng | Giỏ hàng: phải đăng nhập mới thêm được, lưu theo từng tài khoản, hiệu ứng bay vào giỏ | Lưu trên trình duyệt (backend giỏ hàng: Phase 3) |
| Khách hàng | Thanh toán, đơn hàng, chi tiết đơn | Mô phỏng, lưu trên trình duyệt (backend đơn hàng: Phase 4) |
| Khách hàng | Chatbot hỗ trợ (câu trả lời dựng sẵn), liên hệ, dịch vụ | Mô phỏng (Phase 9) |
| Quản trị | Đăng nhập nội bộ (chỉ STAFF / ADMIN), **Người dùng & phân quyền**, **Sản phẩm** | **Thật** (API) |
| Quản trị | Tổng quan, bảo hành / đổi trả, hỗ trợ khách hàng, đơn chi nhánh, báo cáo, nhân viên, chi nhánh, lịch sử chatbot | Dữ liệu mẫu (Phase 4–9) |

---

## 4. Dùng hằng ngày

| Việc | Lệnh / cách làm |
|---|---|
| Bật hệ thống | Mở Docker Desktop: các container tự chạy lại. Nếu lần trước đã tắt bằng `docker compose stop` / `down` thì chạy `docker compose up -d` |
| Sửa **frontend** (`frontend/`) | Chỉ cần tải lại trang (F5), không cần build lại |
| Sửa **backend** (`backend/Tech/`) | `docker compose up -d --build backend` |
| Xem log backend | `docker compose logs -f backend` |
| Tắt hệ thống (giữ dữ liệu) | `docker compose stop` hoặc `docker compose down` |
| Cập nhật code mới | `git pull` rồi `docker compose up -d --build` |

> ⚠️ **Không** dùng `docker compose down -v` trừ khi muốn **xoá sạch database**: `-v` xoá luôn volume dữ liệu (mọi tài khoản, đơn hàng…). Lần `up` tiếp theo sẽ tạo lại schema rỗng; cần chạy crawler/import riêng để nạp catalog.

---

## 5. Gặp lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `JWT_SECRET is missing - copy .env.example to .env and set it` | Chưa có file `.env`: làm lại **Bước 2** |
| `docker: command not found` / `failed to connect to the docker API` | Docker Desktop chưa chạy: mở Docker Desktop, chờ **Engine running** |
| `port is already allocated` với cổng **5510** | Cổng bị chương trình khác dùng: trong `.env` đổi `FRONTEND_PORT=5520` (cổng khác bất kỳ), rồi `docker compose up -d` và mở http://localhost:5520 |
| `port is already allocated` với cổng **8080**, **5432** hoặc **6379** | Tắt chương trình đang dùng cổng đó (ví dụ PostgreSQL / Redis cài sẵn trên máy, hoặc backend đang chạy bằng `mvnw`) |
| Bấm "Thêm vào giỏ" thì bị chuyển sang trang đăng nhập | Đúng thiết kế: phải đăng nhập mới thêm được vào giỏ; đăng nhập xong sẽ quay lại đúng trang |
| Khu quản trị báo "Tài khoản này không có quyền truy cập khu nội bộ." | Tài khoản chỉ là khách hàng: ADMIN cấp quyền **Nhân viên** ở trang Người dùng & phân quyền, hoặc tạo ADMIN đầu tiên ở **Bước 5** |
| Web báo "Không thể kết nối tới máy chủ (localhost:8080)…" | Backend chưa khởi động xong hoặc bị lỗi: xem `docker compose logs backend` |
| Trang Sản phẩm trống, không báo lỗi | Database được tạo **trước** khi có file seed nên chưa có sản phẩm. Nếu chưa có dữ liệu gì cần giữ: `docker compose down -v` rồi `docker compose up -d` để tạo lại database kèm dữ liệu mẫu |

---

## 6. Dành cho người phát triển

Phần này cần thêm **JDK 21** (để chạy test backend).

**Test backend** (chạy trên máy, dùng database `techshopping_test` trong container postgres):

```powershell
cd backend\Tech
.\mvnw.cmd clean test
```

**Chạy backend ngoài Docker** (ví dụ để debug trong IDE):
1. Dừng backend trong Docker trước, vì cổng 8080 chỉ một backend dùng được:
   ```powershell
   docker compose stop backend
   ```
2. Trong `backend\Tech`, đặt `JWT_SECRET` (dùng đúng giá trị trong `.env`) rồi chạy:
   ```powershell
   $env:JWT_SECRET = "<giá trị JWT_SECRET trong .env>"
   .\mvnw.cmd spring-boot:run
   ```
3. Chạy trong IDE thì thêm VM option `-Duser.timezone=Asia/Ho_Chi_Minh`.

**Frontend bằng VS Code Live Server:** vẫn dùng song song được, ở cổng nào cũng được (backend profile dev chấp nhận mọi cổng `localhost` / `127.0.0.1`). Mở Live Server trên thư mục `frontend/`, hoặc trên cả repo rồi vào `/frontend/index.html`.

**Cấu trúc thư mục chính:**

| Thư mục | Nội dung |
|---|---|
| `backend/Tech/` | Spring Boot API (Java 21) |
| `frontend/` | Website: `index.html`, `customer/`, `auth/`, `admin/` (khu nội bộ), `css/`, `js/` |
| `database/` | `techshopping.sql` (cấu trúc 35 bảng), `docker-init/` (tạo DB test, dữ liệu mẫu) |
| `docker/` | Cấu hình nginx cho frontend |
| `Raw_data/` | Dữ liệu crawl gốc (CSV) |
