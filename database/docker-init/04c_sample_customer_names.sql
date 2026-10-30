-- Khách mẫu của 04 trông như người thật (2026-10-30): dữ liệu demo, KHÔNG dùng cho production.
--   04 tạo khách "Minh Anh Nguyễn [TÀI KHOẢN MẪU 001]", số 09000000NN, địa chỉ "[ĐỊA CHỈ MẪU ...]". File này đổi thành
--   họ tên đúng thứ tự ("Nguyễn Minh Anh"), số di động, giới tính theo tên, địa chỉ phố thật ở Quận 1 / Quận 3
--   (TP.HCM); đơn hàng của các khách đó đổi tên / số / địa chỉ người nhận theo. Tên đăng nhập, email, mật khẩu giữ nguyên.
--   Vẫn là tài khoản mẫu: email @example.test, đơn 04 vẫn có ghi chú SAMPLE-DATA.
-- Docker chạy file này sau 04 và trước 07 (doanh thu mẫu chép tên / số của khách vào đơn) khi tạo volume mới.
-- Chạy lại nhiều lần vẫn an toàn (chỉ đổi tài khoản còn dấu "TÀI KHOẢN MẪU"):
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/04c_sample_customer_names.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:*.

begin;

do $$
begin
    if current_database() <> 'techshopping' then
        raise exception 'Chỉ sửa khách mẫu trong database techshopping.';
    end if;
end $$;

-- tên (không họ) → giới tính; tên khác giữ giới tính đang có
create temp table sn_given (given varchar(40), gender varchar(10)) on commit drop;
insert into sn_given values
    ('Minh Anh', 'Nữ'), ('Thu Hà', 'Nữ'), ('Bảo Ngọc', 'Nữ'), ('Phương Linh', 'Nữ'), ('Khánh Vy', 'Nữ'),
    ('Quốc Bảo', 'Nam'), ('Gia Huy', 'Nam'), ('Tuấn Kiệt', 'Nam'), ('Đức Anh', 'Nam'), ('Hoàng Nam', 'Nam');

create temp table sn_street (district varchar(40), no int, ward varchar(60), street varchar(60)) on commit drop;
insert into sn_street values
    ('Quận 1', 0, 'Phường Bến Nghé', 'Lê Thánh Tôn'), ('Quận 1', 1, 'Phường Đa Kao', 'Đinh Tiên Hoàng'),
    ('Quận 1', 2, 'Phường Nguyễn Cư Trinh', 'Trần Hưng Đạo'), ('Quận 1', 3, 'Phường Bến Thành', 'Lê Lai'),
    ('Quận 3', 0, 'Phường Võ Thị Sáu', 'Nam Kỳ Khởi Nghĩa'), ('Quận 3', 1, 'Phường 9', 'Trần Quốc Thảo'),
    ('Quận 3', 2, 'Phường 5', 'Nguyễn Đình Chiểu'), ('Quận 3', 3, 'Phường 11', 'Cách Mạng Tháng Tám');

-- "[TÀI KHOẢN MẪU 001]" đứng sau "Tên Họ" (họ là chữ cuối); "(TÀI KHOẢN MẪU)" đứng sau "Họ Tên"
create temp table sn_account on commit drop as
select u.user_id, u.fullname as old_fullname, u.phone as old_phone,
       trim(regexp_replace(u.fullname, '\s*[\[(]TÀI KHOẢN MẪU[^\])]*[\])]\s*$', '')) as plain_name,
       u.fullname like '%[TÀI KHOẢN MẪU%' as given_first
from users u
where u.email like '%@example.test'
  and u.fullname like '%TÀI KHOẢN MẪU%';

alter table sn_account add column fullname varchar(120), add column given varchar(40), add column phone varchar(20);

update sn_account
set fullname = case when given_first
                    then regexp_replace(plain_name, '^(.*)\s+(\S+)$', '\2 \1')
                    else plain_name end,
    -- cùng công thức số người nhận của 07_sample_sales.sql, nên đơn đã có khớp với hồ sơ
    phone = '09' || lpad(((user_id * 7919 + 1234567) % 100000000)::text, 8, '0');

update sn_account set given = regexp_replace(fullname, '^\S+\s+', '');

update users u
set fullname = a.fullname, phone = a.phone, updated_at = current_timestamp
from sn_account a
where u.user_id = a.user_id;

update customer_profiles cp
set gender = coalesce(g.gender, cp.gender),
    address = (abs(hashtext('addr-' || cp.customer_id)) % 240 + 1) || ' ' || s.street,
    ward = s.ward,
    district = d.district,
    city = 'TP. Hồ Chí Minh',
    postal_code = '700000',
    default_shipping_address = (abs(hashtext('addr-' || cp.customer_id)) % 240 + 1) || ' ' || s.street
                               || ', ' || s.ward || ', ' || d.district || ', TP. Hồ Chí Minh',
    updated_at = current_timestamp
from sn_account a
left join sn_given g on g.given = a.given
cross join lateral (select case when cp2.district in ('Quận 1', 'Quận 3') then cp2.district else 'Quận 1' end as district
                    from customer_profiles cp2 where cp2.customer_id = a.user_id) d
join sn_street s on s.district = d.district and s.no = abs(hashtext('ward-' || a.user_id)) % 4
where cp.customer_id = a.user_id;

update orders o
set recipient_name = case when o.recipient_name in (a.old_fullname, a.plain_name) then a.fullname
                          else o.recipient_name end,
    recipient_phone = case when o.recipient_phone = a.old_phone then a.phone else o.recipient_phone end,
    shipping_address = case when o.shipping_address like '%ĐỊA CHỈ MẪU%' then cp.default_shipping_address
                            else o.shipping_address end,
    billing_address = case when o.billing_address like '%ĐỊA CHỈ MẪU%' then cp.default_shipping_address
                           else o.billing_address end,
    shipping_province = case when o.shipping_address like '%ĐỊA CHỈ MẪU%' then cp.city else o.shipping_province end,
    shipping_ward = case when o.shipping_address like '%ĐỊA CHỈ MẪU%' then cp.ward else o.shipping_ward end
from sn_account a
join customer_profiles cp on cp.customer_id = a.user_id
where o.user_id = a.user_id;

select count(*) as renamed_customers from sn_account;

commit;
