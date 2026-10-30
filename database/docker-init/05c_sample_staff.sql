-- Nhân viên mẫu (2026-10-30): dữ liệu demo, KHÔNG dùng cho production.
--   Mỗi chi nhánh mẫu (05b) có 1 quản lý (vai trò BRANCH_MANAGER, "Quản lý chi nhánh") và 2 nhân viên (STAFF,
--   "Nhân viên chi nhánh"): tư vấn bán hàng và kỹ thuật viên — giống tài khoản do trang Nhân viên tuyển.
--   Mật khẩu chung của tài khoản mẫu: SampleOnly123! (không bắt đổi ở lần đăng nhập đầu, để demo).
--   Tên, số điện thoại, email (@poyshop.vn) là giả lập; hệ thống không gửi thư cho tài khoản nhân viên.
-- Docker chạy file này sau 05b (chi nhánh) khi tạo volume mới (docker-compose.yml). Chạy lại nhiều lần vẫn an toàn:
-- tài khoản / mã nhân viên đã có thì bỏ qua, chi nhánh đã có quản lý đang làm thì không thêm quản lý.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/05c_sample_staff.sql

begin;

do $$
begin
    if current_database() <> 'techshopping' then
        raise exception 'Chỉ nạp nhân viên mẫu vào database techshopping.';
    end if;
end $$;

insert into roles (name, description)
values ('STAFF', 'Store staff'), ('BRANCH_MANAGER', 'Branch manager')
on conflict (name) do nothing;

create temp table st_staff (
    store_name varchar(120), username varchar(50), fullname varchar(120), phone varchar(20), role varchar(30),
    code varchar(50), department varchar(100), position varchar(100), salary numeric(15, 2), hired date
) on commit drop;

insert into st_staff values
    ('POY Quận 1',    'longnguyen',     'Nguyễn Hoàng Long',   '0901001101', 'BRANCH_MANAGER', 'NV-Q1-01', 'Quản lý',  'Quản lý cửa hàng', 18000000, '2024-03-04'),
    ('POY Quận 1',    'anhtran',        'Trần Ngọc Ánh',       '0901001102', 'STAFF',          'NV-Q1-02', 'Bán hàng', 'Tư vấn bán hàng',   9500000, '2025-01-06'),
    ('POY Quận 1',    'khoile',         'Lê Minh Khôi',        '0901001103', 'STAFF',          'NV-Q1-03', 'Kỹ thuật', 'Kỹ thuật viên',    11000000, '2025-06-02'),
    ('POY Quận 7',    'tungpham',       'Phạm Thanh Tùng',     '0901001201', 'BRANCH_MANAGER', 'NV-Q7-01', 'Quản lý',  'Quản lý cửa hàng', 18000000, '2024-05-13'),
    ('POY Quận 7',    'duyenvo',        'Võ Thị Mỹ Duyên',     '0901001202', 'STAFF',          'NV-Q7-02', 'Bán hàng', 'Tư vấn bán hàng',   9500000, '2025-02-17'),
    ('POY Quận 7',    'vietdang',       'Đặng Quốc Việt',      '0901001203', 'STAFF',          'NV-Q7-03', 'Kỹ thuật', 'Kỹ thuật viên',    11000000, '2025-08-11'),
    ('POY Thủ Đức',   'baohuynh',       'Huỳnh Gia Bảo',       '0901001301', 'BRANCH_MANAGER', 'NV-TD-01', 'Quản lý',  'Quản lý cửa hàng', 17000000, '2024-09-02'),
    ('POY Thủ Đức',   'vyngo',          'Ngô Thảo Vy',         '0901001302', 'STAFF',          'NV-TD-02', 'Bán hàng', 'Tư vấn bán hàng',   9000000, '2025-03-10'),
    ('POY Thủ Đức',   'thinhbui',       'Bùi Đức Thịnh',       '0901001303', 'STAFF',          'NV-TD-03', 'Kỹ thuật', 'Kỹ thuật viên',    10500000, '2025-09-15'),
    ('POY Cầu Giấy',  'hungdo',         'Đỗ Mạnh Hùng',        '0901002101', 'BRANCH_MANAGER', 'NV-CG-01', 'Quản lý',  'Quản lý cửa hàng', 17500000, '2024-04-08'),
    ('POY Cầu Giấy',  'trangnguyen',    'Nguyễn Thu Trang',    '0901002102', 'STAFF',          'NV-CG-02', 'Bán hàng', 'Tư vấn bán hàng',   9500000, '2025-01-13'),
    ('POY Cầu Giấy',  'datvu',          'Vũ Tiến Đạt',         '0901002103', 'STAFF',          'NV-CG-03', 'Kỹ thuật', 'Kỹ thuật viên',    11000000, '2025-07-07'),
    ('POY Hoàn Kiếm', 'yentrinh',       'Trịnh Hải Yến',       '0901002201', 'BRANCH_MANAGER', 'NV-HK-01', 'Quản lý',  'Quản lý cửa hàng', 18000000, '2024-06-03'),
    ('POY Hoàn Kiếm', 'huyphan',        'Phan Quang Huy',      '0901002202', 'STAFF',          'NV-HK-02', 'Bán hàng', 'Tư vấn bán hàng',   9500000, '2025-04-14'),
    ('POY Hoàn Kiếm', 'hieuluong',      'Lương Văn Hiếu',      '0901002203', 'STAFF',          'NV-HK-03', 'Kỹ thuật', 'Kỹ thuật viên',    11000000, '2025-10-06'),
    ('POY Đà Nẵng',   'phucle',         'Lê Văn Phúc',         '0901003101', 'BRANCH_MANAGER', 'NV-DN-01', 'Quản lý',  'Quản lý cửa hàng', 16500000, '2024-08-05'),
    ('POY Đà Nẵng',   'oanhho',         'Hồ Thị Kim Oanh',     '0901003102', 'STAFF',          'NV-DN-02', 'Bán hàng', 'Tư vấn bán hàng',   8500000, '2025-02-24'),
    ('POY Đà Nẵng',   'danhtruong',     'Trương Công Danh',    '0901003103', 'STAFF',          'NV-DN-03', 'Kỹ thuật', 'Kỹ thuật viên',    10000000, '2025-11-03');

-- Chi nhánh có thật, tài khoản / mã chưa dùng; quản lý chỉ khi chi nhánh chưa có quản lý đang làm (mỗi chi nhánh một)
create temp table st_new on commit drop as
select t.*, t.username || '@poyshop.vn' as email, s.store_id
from st_staff t
join stores s on s.name = t.store_name
where not exists (select 1 from users u where u.username = t.username or u.email = t.username || '@poyshop.vn')
  and not exists (select 1 from employees e where e.employee_code = t.code)
  and (t.role <> 'BRANCH_MANAGER' or not exists (
      select 1
      from employee_assignments a
      join employees e on e.employee_id = a.employee_id
      join user_roles ur on ur.user_id = e.user_id
      join roles r on r.role_id = ur.role_id
      where a.store_id = s.store_id and a.is_active and a.end_date is null and e.is_active
        and r.name = 'BRANCH_MANAGER'));

insert into users (email, username, password_hash, fullname, phone, is_active, created_at)
select email, username, '$2a$10$nbH0tjwxfDyh2MbSSMFZhuuymQZ2ZiosLeYzvvFllcrS9SXbdL3bC', fullname, phone, true, hired
from st_new;

insert into user_roles (user_id, role_id)
select u.user_id, r.role_id
from st_new n
join users u on u.username = n.username
join roles r on r.name = n.role;

insert into employees (user_id, employee_code, department, position, salary, hiring_date, is_active)
select u.user_id, n.code, n.department, n.position, n.salary, n.hired, true
from st_new n
join users u on u.username = n.username;

insert into employee_assignments (employee_id, store_id, start_date, position_at_store, is_active)
select e.employee_id, n.store_id, n.hired,
       case when n.role = 'BRANCH_MANAGER' then 'Quản lý chi nhánh' else 'Nhân viên chi nhánh' end, true
from st_new n
join users u on u.username = n.username
join employees e on e.user_id = u.user_id;

select n.store_name, count(*) as hired from st_new n group by n.store_name order by min(n.store_id);

commit;
