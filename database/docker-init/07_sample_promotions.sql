-- Khuyến mãi, voucher và combo mẫu (dữ liệu demo, KHÔNG dùng cho production).
--   Mốc thời gian tính từ lúc nạp: phần lớn đang diễn ra, mỗi loại có thêm một mục sắp diễn ra hoặc đã hết hạn để thử giao diện.
--   Người tạo: tài khoản ADMIN nếu đã có (backend tạo ADMIN sau khi khởi động), không thì tài khoản đầu tiên.
-- Docker chạy file này sau 06 khi tạo volume mới (docker-compose.yml). Chạy lại nhiều lần vẫn an toàn:
-- chương trình / mã / combo trùng tên đã có thì bỏ qua.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/07_sample_promotions.sql

begin;

do $$
begin
    if current_database() <> 'techshopping' then
        raise exception 'Chỉ nạp khuyến mãi mẫu vào database techshopping.';
    end if;
end $$;

create temp table sp_owner on commit drop as
select coalesce(
    (select u.user_id from users u join user_roles ur on ur.user_id = u.user_id join roles r on r.role_id = ur.role_id
      where r.name = 'ADMIN' order by u.user_id limit 1),
    (select min(user_id) from users)) as user_id;

-- khuyến mãi: loại giảm, giá trị, giảm tối đa, bắt đầu (ngày so với hôm nay), kết thúc, danh mục áp dụng (null = chọn sản phẩm theo tên)
create temp table sp_promo (
    name varchar(255), description text, dtype varchar(20), dvalue numeric, dmax numeric,
    from_day int, to_day int, category varchar(100), name_like varchar(100)
) on commit drop;

insert into sp_promo values
    ('Flash sale cuối tuần', 'Giảm 12% điện thoại trong dịp cuối tuần', 'PERCENTAGE', 12, 3000000, -2, 5, 'Điện Thoại', null),
    ('Mùa tựu trường - Laptop', 'Laptop giảm 8% cho học sinh, sinh viên, tối đa 2,5 triệu', 'PERCENTAGE', 8, 2500000, -10, 30, 'Laptop', null),
    ('Đồng hồ thời trang giảm sâu', 'Đồng hồ thời trang giảm 25%', 'PERCENTAGE', 25, null, -5, 20, 'Đồng Hồ Thời Trang', null),
    ('Đồng hồ thông minh mùa lễ', 'Đồng hồ thông minh giảm 10%', 'PERCENTAGE', 10, 1500000, -1, 40, 'Đồng Hồ Thông Minh', null),
    ('Phụ kiện Apple chính hãng', 'Giảm 100.000đ phụ kiện Apple chính hãng', 'FIXED_AMOUNT', 100000, null, -3, 25, 'Phụ Kiện', '%Apple%'),
    ('Siêu sale 11.11', 'Siêu sale 11.11, sắp diễn ra', 'PERCENTAGE', 15, 5000000, 12, 14, 'Điện Thoại', null);

insert into promotions (name, description, discount_type, discount_value, max_discount_amount, start_date, end_date, is_active, created_by)
select s.name, s.description, s.dtype, s.dvalue, s.dmax,
       date_trunc('day', now()) + make_interval(days => s.from_day),
       date_trunc('day', now()) + make_interval(days => s.to_day) + interval '23 hours 59 minutes',
       true, (select user_id from sp_owner)
from sp_promo s
where not exists (select 1 from promotions p where p.name = s.name);

insert into promotion_products (promotion_id, product_id)
select p.promotion_id, pr.product_id
from sp_promo s
join promotions p on p.name = s.name
join categories c on c.name = s.category
join products pr on pr.category_id = c.category_id
where pr.is_active and pr.deleted_at is null
  and (s.name_like is null or pr.name ilike s.name_like)
  and not exists (select 1 from promotion_products x where x.promotion_id = p.promotion_id and x.product_id = pr.product_id)
  -- mỗi chương trình lấy 40 sản phẩm đầu để bảng không phình quá lớn
  and pr.product_id in (
      select p2.product_id from products p2
      where p2.category_id = c.category_id and p2.is_active and p2.deleted_at is null
        and (s.name_like is null or p2.name ilike s.name_like)
      order by p2.product_id limit 40);

-- voucher: mã, tên, % giảm, giảm tối đa, đơn tối thiểu, bắt đầu, kết thúc, lượt dùng, lượt / khách, danh mục (null = mọi danh mục)
create temp table sp_voucher (
    code varchar(30), name varchar(150), pct numeric, vmax numeric, vmin numeric,
    from_day int, to_day int, ulimit int, per_customer int, category varchar(100)
) on commit drop;

insert into sp_voucher values
    ('CHAOBAN5', 'Chào bạn mới giảm 5%', 5, 500000, 1000000, -30, 60, 500, 1, null),
    ('LAPTOP10', 'Laptop giảm 10%', 10, 3000000, 10000000, -10, 30, 200, 1, 'Laptop'),
    ('PHUKIEN15', 'Phụ kiện giảm 15%', 15, 300000, 200000, -10, 30, 300, 2, 'Phụ Kiện'),
    ('DONGHO20', 'Đồng hồ giảm 20%', 20, 1000000, 1000000, -5, 25, 100, 1, 'Đồng Hồ Thời Trang'),
    ('BLACKFRIDAY12', 'Black Friday giảm 12%, sắp diễn ra', 12, 2000000, 2000000, 20, 25, 300, 1, null),
    ('THANG9', 'Ưu đãi tháng 9, đã hết hạn', 7, 700000, 1000000, -60, -30, 100, 1, null);

insert into vouchers (code, name, discount_percent, max_discount, min_order_amount, starts_at, ends_at,
                      usage_limit, per_customer_limit, used_count, all_categories, is_active, created_by)
select s.code, s.name, s.pct, s.vmax, s.vmin,
       date_trunc('day', now()) + make_interval(days => s.from_day),
       date_trunc('day', now()) + make_interval(days => s.to_day) + interval '23 hours 59 minutes',
       s.ulimit, s.per_customer, 0, s.category is null, true, (select user_id from sp_owner)
from sp_voucher s
where not exists (select 1 from vouchers v where v.code = s.code);

insert into voucher_categories (voucher_id, category_id)
select v.voucher_id, c.category_id
from sp_voucher s
join vouchers v on v.code = s.code
join categories c on c.name = s.category
where not exists (select 1 from voucher_categories x where x.voucher_id = v.voucher_id and x.category_id = c.category_id);

-- combo: tên, sản phẩm chính (so khớp tên), các món mua kèm (so khớp tên) với mức giảm
create temp table sp_combo (name varchar(255), description text, main_like varchar(150), from_day int, to_day int) on commit drop;
create temp table sp_combo_item (combo_name varchar(255), item_like varchar(150), dtype varchar(20), dvalue numeric) on commit drop;

insert into sp_combo values
    ('iPhone 16 Plus trọn bộ', 'iPhone 16 Plus kèm ốp lưng MagSafe và củ sạc 20W chính hãng', 'iPhone 16 Plus', -7, 45),
    ('MacBook Air làm việc di động', 'MacBook Air 13 inch M5 kèm chuột Apple Magic Mouse', 'MacBook Air 13 inch M5', -7, 45),
    ('Galaxy S25 Ultra sạc nhanh', 'Galaxy S25 Ultra kèm sạc nhanh PD 20W Baseus', 'Samsung Galaxy S25 Ultra 5G', -7, 45),
    ('Góc gaming Lenovo LOQ', 'Laptop Lenovo LOQ kèm chuột gaming Asus TUF', 'Lenovo LOQ 15IRH11E Core 5 205H%', -7, 45);

insert into sp_combo_item values
    ('iPhone 16 Plus trọn bộ', 'Ốp lưng MagSafe iPhone 16 Nhựa Trong Apple', 'PERCENT', 15),
    ('iPhone 16 Plus trọn bộ', 'Sạc nhanh Type-C PD 20W Apple', 'AMOUNT', 50000),
    ('MacBook Air làm việc di động', 'Chuột Apple Magic Mouse USB C', 'PERCENT', 10),
    ('Galaxy S25 Ultra sạc nhanh', 'Sạc nhanh 2 cổng USB Type-C PD 20W Baseus Palm CCZC20US', 'PERCENT', 20),
    ('Góc gaming Lenovo LOQ', 'Chuột Bluetooth Gaming Asus TUF M4 WL', 'PERCENT', 15);

insert into combos (name, description, main_product_id, starts_at, ends_at, is_active, created_by)
select s.name, s.description,
       (select p.product_id from products p where p.name ilike s.main_like and p.is_active and p.deleted_at is null order by p.product_id limit 1),
       date_trunc('day', now()) + make_interval(days => s.from_day),
       date_trunc('day', now()) + make_interval(days => s.to_day) + interval '23 hours 59 minutes',
       true, (select user_id from sp_owner)
from sp_combo s
where not exists (select 1 from combos c where c.name = s.name)
  and exists (select 1 from products p where p.name ilike s.main_like and p.is_active and p.deleted_at is null);

insert into combo_items (combo_id, product_id, discount_type, discount_value)
select c.combo_id,
       (select p.product_id from products p where p.name ilike i.item_like and p.is_active and p.deleted_at is null order by p.product_id limit 1),
       i.dtype, i.dvalue
from sp_combo_item i
join combos c on c.name = i.combo_name
where exists (select 1 from products p where p.name ilike i.item_like and p.is_active and p.deleted_at is null)
  and not exists (select 1 from combo_items x where x.combo_id = c.combo_id);

commit;
