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

> **Dữ liệu có sẵn.** Lần chạy đầu tiên, Docker tự tạo database và nạp **877 sản phẩm mẫu** (crawl từ Thế Giới Di Động: danh mục, thương hiệu, phiên bản, ảnh) từ `database/docker-init/03_seed_catalog.sql`. Chưa có tài khoản nào: tự đăng ký trên web, hoặc tạo ADMIN ở Bước 5.
>
> Dữ liệu được lưu trong volume Docker `postgres_data`, nên tắt / mở máy hay `docker compose down` đều **không mất**. File seed chỉ được nạp khi database còn trống, nên không ghi đè dữ liệu bạn đã tạo.

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
| Khách hàng | Trang chủ, danh sách sản phẩm (lọc, tìm kiếm, phân trang), chi tiết sản phẩm (ảnh chính + ảnh phụ, giá và tên chương trình khuyến mãi đang chạy), trang khuyến nghị | **Thật** (877 sản phẩm) |
| Khách hàng | Giỏ hàng: phải đăng nhập mới thêm được, lưu trên server theo từng tài khoản, giá và phí vận chuyển do server tính | **Thật** (API) |
| Khách hàng | Thanh toán (đặt hàng từ giỏ), đơn hàng của tôi, chi tiết đơn, hủy đơn khi còn chờ xác nhận | **Thật** (API) |
| Khách hàng | **Phương thức thanh toán** (Phase 5, mô phỏng, không qua cổng thanh toán thật): COD (ghi nhận đã thu khi giao); chuyển khoản (trang đơn hiện STK demo + mã QR VietQR, nội dung = mã đơn, nhân viên xác nhận đã nhận tiền); **trả góp 0%** 3 / 6 / 9 / 12 tháng cho đơn từ 3.000.000đ (nhập CCCD 10 số + ngân hàng thẻ, chờ duyệt, lịch các kỳ tính từ ngày giao). Đơn đã trả tiền mà bị hủy thì chờ hoàn tiền | **Thật** (API) |
| Khách hàng | Chatbot hỗ trợ (câu trả lời dựng sẵn), liên hệ, dịch vụ | Mô phỏng (Phase 9) |
| Quản trị | Đăng nhập nội bộ (chỉ STAFF / ADMIN), **Người dùng & phân quyền**, **Sản phẩm** (kèm ảnh: upload từ máy hoặc dán link) | **Thật** (API) |
| Quản trị | **Đơn hàng** (STAFF và ADMIN): tìm theo mã / người nhận / khách, lọc trạng thái và ngày, đổi trạng thái theo đúng luồng, mã vận đơn; khối thanh toán: "Đã nhận tiền" (chuyển khoản, bắt buộc trước khi xác nhận đơn), "Đã hoàn tiền", duyệt / từ chối trả góp (có lý do, đơn tự hủy). Nhân viên chỉ thấy đơn của chi nhánh mình; xác nhận đơn trừ tồn kho chi nhánh (thiếu hàng thì không xác nhận được, ADMIN chuyển đơn sang chi nhánh khác), hủy đơn đã xác nhận thì hoàn kho | **Thật** (API) |
| Quản trị | **Trả góp** (STAFF và ADMIN): danh sách hợp đồng, lọc trạng thái / kỳ quá hạn, lịch các kỳ, ghi nhận lần lượt từng kỳ (kỳ cuối → hoàn tất) | **Thật** (API) |
| Quản trị | **Khuyến mãi** (chỉ ADMIN): chương trình có thời gian bắt đầu / kết thúc, giảm theo % (có mức giảm tối đa) hoặc số tiền, chọn sản phẩm và đặt mức giảm riêng từng sản phẩm, tạm dừng / bật lại. Một sản phẩm chỉ thuộc một chương trình đang bật tại cùng thời điểm; giỏ hàng và thanh toán lấy giá thấp hơn giữa "Giá khuyến mãi" của sản phẩm và giá chương trình; đơn đã đặt giữ nguyên giá | **Thật** (API) |
| Quản trị | **Chi nhánh** (chỉ ADMIN): thêm / sửa / tạm đóng / xoá (chỉ khi chưa dùng); quận / huyện + tỉnh / thành dùng để tự gán chi nhánh cho đơn giao tận nhà. **Nhân viên** (chỉ ADMIN): hồ sơ cho tài khoản có quyền Nhân viên, gán / chuyển / rút chi nhánh (giữ lịch sử), vị trí "Quản lý chi nhánh", đã nghỉ. Nhân viên thấy vai trò + chi nhánh của mình trên thanh trên cùng | **Thật** (API) |
| Quản trị | Tổng quan (trừ số chi nhánh / nhân viên), bảo hành / đổi trả, hỗ trợ khách hàng, báo cáo, lịch sử chatbot | Dữ liệu mẫu (Phase 6, 9, 10) |

---

## 4. Dùng hằng ngày

| Việc | Lệnh / cách làm |
|---|---|
| Bật hệ thống | Mở Docker Desktop: các container tự chạy lại. Nếu lần trước đã tắt bằng `docker compose stop` / `down` thì chạy `docker compose up -d` |
| Sửa **frontend** (`frontend/`) | Chỉ cần tải lại trang (F5), không cần build lại |
| Sửa **backend** (`backend/Tech/`) | `docker compose up -d --build backend` |
| Xem log backend | `docker compose logs -f backend` |
| Tắt hệ thống (giữ dữ liệu) | `docker compose stop` hoặc `docker compose down` |
| Cập nhật code mới | `git pull` rồi chạy các file **mới** trong `database/migrations/` (xem dưới), rồi `docker compose up -d --build` |

> **Cập nhật database đã có:** database chỉ được tạo từ `database/techshopping.sql` **một lần** (khi volume còn trống), nên khi schema đổi, database cũ cần chạy các file migration **theo đúng thứ tự dưới đây** (chạy lại nhiều lần vẫn an toàn):
> ```powershell
> # 1. Đơn hàng
> Get-Content database\migrations\orders_recipient.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
> Get-Content database\migrations\orders_recipient.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
> # 2. Khuyến mãi
> Get-Content database\migrations\promotions.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
> Get-Content database\migrations\promotions.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
> # 3. Thanh toán + trả góp
> Get-Content database\migrations\payments.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
> Get-Content database\migrations\payments.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
> # 4. Chi nhánh / tồn kho
> Get-Content database\migrations\stores_inventory.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
> Get-Content database\migrations\stores_inventory.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
> # 5. Thuộc tính theo danh mục
> Get-Content database\migrations\category_attributes.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
> Get-Content database\migrations\category_attributes.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
> # 6. Lịch sử trạng thái đơn hàng
> Get-Content database\migrations\order_status_history.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
> Get-Content database\migrations\order_status_history.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
> # 7. Đánh giá sản phẩm
> Get-Content database\migrations\reviews.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
> Get-Content database\migrations\reviews.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
> ```

> ⚠️ **Không** dùng `docker compose down -v` trừ khi muốn **xoá sạch database**: `-v` xoá luôn volume dữ liệu (mọi tài khoản, đơn hàng…). Lần `up` tiếp theo sẽ tạo lại database với 877 sản phẩm mẫu.

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
| Log backend có `Schema-validation: missing column` (ví dụ `recipient_name`, `citizen_id`, `confirmed_by`) hoặc `missing table [promotions]` | Database tạo từ schema cũ: chạy các file trong `database/migrations/` (mục **4. Dùng hằng ngày**), rồi `docker compose restart backend` |
| Backend cứ khởi động lại, log có `UnknownHostException: postgres` | Container backend bị rơi khỏi mạng Docker: `docker compose up -d --force-recreate --no-deps backend` |
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
| `database/` | `techshopping.sql` (cấu trúc 43 bảng), `migrations/` (cập nhật database cũ), `docker-init/` (tạo DB test, dữ liệu mẫu) |
| `docker/` | Cấu hình nginx cho frontend |
| `Raw_data/` | Dữ liệu crawl gốc (CSV) |
