-- Chi nhánh mẫu (2026-10-30): dữ liệu demo, KHÔNG dùng cho production.
--   Cửa hàng mẫu do 04 tạo ("POY - Cửa hàng mẫu ...") thành chi nhánh Quận 1, rồi thêm các chi nhánh còn lại.
--   Đơn giao tận nhà chọn chi nhánh theo quận + thành phố trong địa chỉ (NearestStoreResolver), nên district / city
--   ghi đúng tên khách hay gõ ("Quận 7", "Cầu Giấy", "Đà Nẵng"). Địa chỉ là đường phố thật; số điện thoại, email
--   là số / hộp thư giả lập (hệ thống không gọi / gửi thư cho chi nhánh).
-- Docker chạy file này sau 05 và trước 06 (tồn kho mẫu) khi tạo volume mới (docker-compose.yml).
-- Chạy lại nhiều lần vẫn an toàn (chi nhánh trùng tên không thêm lại, không ghi đè chi nhánh đã sửa):
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/05b_sample_stores.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/06_sample_stock.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:*.

begin;

do $$
begin
    if current_database() <> 'techshopping' then
        raise exception 'Chỉ nạp chi nhánh mẫu vào database techshopping.';
    end if;
end $$;

create temp table sb_branch (
    no int, name varchar(120), address varchar(255), district varchar(100), city varchar(100),
    phone varchar(20), email varchar(120), latitude numeric(10, 8), longitude numeric(11, 8)
) on commit drop;

insert into sb_branch values
    (1, 'POY Quận 1', '86 Lê Lợi, Phường Bến Thành', 'Quận 1', 'TP. Hồ Chí Minh',
        '028 7300 0101', 'quan1@poyshop.vn', 10.77280000, 106.69990000),
    (2, 'POY Quận 7', '469 Nguyễn Hữu Thọ, Phường Tân Hưng', 'Quận 7', 'TP. Hồ Chí Minh',
        '028 7300 0107', 'quan7@poyshop.vn', 10.74080000, 106.70180000),
    (3, 'POY Thủ Đức', '216 Võ Văn Ngân, Phường Bình Thọ', 'Thủ Đức', 'TP. Hồ Chí Minh',
        '028 7300 0109', 'thuduc@poyshop.vn', 10.85060000, 106.76450000),
    (4, 'POY Cầu Giấy', '241 Xuân Thủy, Phường Dịch Vọng Hậu', 'Cầu Giấy', 'Hà Nội',
        '024 7300 0201', 'caugiay@poyshop.vn', 21.03680000, 105.78260000),
    (5, 'POY Hoàn Kiếm', '55 Tràng Tiền, Phường Tràng Tiền', 'Hoàn Kiếm', 'Hà Nội',
        '024 7300 0202', 'hoankiem@poyshop.vn', 21.02470000, 105.85490000),
    (6, 'POY Đà Nẵng', '255 Hùng Vương, Phường Vĩnh Trung', 'Hải Châu', 'Đà Nẵng',
        '0236 730 0301', 'danang@poyshop.vn', 16.06750000, 108.21250000);

-- Cửa hàng mẫu cũ (giữ store_id nên đơn hàng, nhân viên, tồn kho vẫn gắn đúng) → chi nhánh đầu tiên
update stores s
set name = b.name, address = b.address, district = b.district, city = b.city, phone = b.phone,
    email = b.email, latitude = b.latitude, longitude = b.longitude, updated_at = current_timestamp
from sb_branch b
where b.no = 1
  and s.store_id = (select min(store_id) from stores where name like 'POY - Cửa hàng mẫu%')
  and not exists (select 1 from stores x where x.name = b.name);

insert into stores (name, address, district, city, phone, email, latitude, longitude, is_active)
select b.name, b.address, b.district, b.city, b.phone, b.email, b.latitude, b.longitude, true
from sb_branch b
where not exists (select 1 from stores s where s.name = b.name)
order by b.no;

select store_id, name, district, city from stores order by store_id;

commit;
