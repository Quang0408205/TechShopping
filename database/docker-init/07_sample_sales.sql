-- Doanh thu mẫu (2026-10-30): dữ liệu demo, KHÔNG dùng cho production.
--   Báo cáo doanh thu tính từ đơn DELIVERED theo ngày giao (SalesReportQueries), nên doanh thu mẫu = đơn hàng mẫu:
--   ~3.000 đơn từ 01/05/2026 đến 29/10/2026 ở 6 chi nhánh (05b), mua phiên bản thật với giá thật, ghi đủ như app:
--     orders + order_items, payments, order_status_history (PENDING → CONFIRMED → SHIPPING → DELIVERED),
--     sales_records (nhân viên xác nhận đơn, hoa hồng 1%), phiếu xuất kho OUT trừ tồn kho (06), điểm thưởng
--     (loyalty_transactions, customer_profiles), ~5% đơn khách huỷ, ~3% đơn trả hàng đã hoàn tiền (return_requests,
--     return_items, phiếu nhập lại RETURN), đơn 2 ngày cuối đang giao (SHIPPING).
--   Mùa vụ: tháng 9 (mở bán iPhone) đông nhất, cuối tuần đông hơn ngày thường; Quận 1 bán chạy nhất, Đà Nẵng ít nhất.
--   Người mua: 2.400 khách mẫu tạo ở đây + khách mẫu của 04, chỉ tài khoản @example.test (không đụng tài khoản
--   thật); ~80% khách mua 1 đơn nên đa số ở hạng Thành viên. Địa chỉ giao là đường phố thật gần chi nhánh, số nhà
--   giả lập.
--   Dòng hàng vượt tồn kho của chi nhánh bị bỏ (tồn kho không âm). setseed cố định nên máy nào cũng ra như nhau.
-- Docker chạy file này sau 06 (tồn kho mẫu) khi tạo volume mới (docker-compose.yml). Chỉ nạp được một lần
-- (dev_sample_seed_registry); muốn nạp lại phải dựng lại volume.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/07_sample_sales.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:*.

begin;

create table if not exists dev_sample_seed_registry (
    seed_key varchar(100) primary key,
    applied_at timestamp not null default current_timestamp
);

do $$
begin
    if current_database() <> 'techshopping' then
        raise exception 'Chỉ nạp doanh thu mẫu vào database techshopping.';
    end if;
    if exists (select 1 from dev_sample_seed_registry where seed_key = 'techshopping-sample-sales-v1') then
        raise exception 'Doanh thu mẫu đã được nạp (dev_sample_seed_registry: techshopping-sample-sales-v1).';
    end if;
end $$;

insert into dev_sample_seed_registry (seed_key) values ('techshopping-sample-sales-v1');

select setseed(0.2026);

-- ---------- dữ liệu nền: chi nhánh, phường giao hàng, người bán, khách, sản phẩm

create temp table ss_store on commit drop as
select s.store_id, s.name, s.address, s.district, s.city, w.weight, w.district_label
from stores s
join (values
    ('POY Quận 1', 1.30, 'Quận 1'),
    ('POY Quận 7', 1.00, 'Quận 7'),
    ('POY Thủ Đức', 0.90, 'TP. Thủ Đức'),
    ('POY Cầu Giấy', 1.10, 'Quận Cầu Giấy'),
    ('POY Hoàn Kiếm', 1.00, 'Quận Hoàn Kiếm'),
    ('POY Đà Nẵng', 0.70, 'Quận Hải Châu')
) w(name, weight, district_label) on w.name = s.name
where s.is_active;

create temp table ss_ward (store_name varchar(120), no int, ward varchar(100), street varchar(100)) on commit drop;
insert into ss_ward values
    ('POY Quận 1', 1, 'Phường Bến Nghé', 'Lê Thánh Tôn'), ('POY Quận 1', 2, 'Phường Đa Kao', 'Đinh Tiên Hoàng'),
    ('POY Quận 1', 3, 'Phường Nguyễn Cư Trinh', 'Trần Hưng Đạo'), ('POY Quận 1', 4, 'Phường Bến Thành', 'Lê Lai'),
    ('POY Quận 7', 1, 'Phường Tân Phong', 'Nguyễn Văn Linh'), ('POY Quận 7', 2, 'Phường Tân Phú', 'Nguyễn Lương Bằng'),
    ('POY Quận 7', 3, 'Phường Tân Thuận Đông', 'Huỳnh Tấn Phát'), ('POY Quận 7', 4, 'Phường Tân Hưng', 'Lê Văn Lương'),
    ('POY Thủ Đức', 1, 'Phường Linh Trung', 'Hoàng Diệu 2'), ('POY Thủ Đức', 2, 'Phường Hiệp Phú', 'Lê Văn Việt'),
    ('POY Thủ Đức', 3, 'Phường Linh Chiểu', 'Võ Văn Ngân'), ('POY Thủ Đức', 4, 'Phường Thảo Điền', 'Quốc Hương'),
    ('POY Cầu Giấy', 1, 'Phường Dịch Vọng', 'Trần Thái Tông'), ('POY Cầu Giấy', 2, 'Phường Nghĩa Đô', 'Hoàng Quốc Việt'),
    ('POY Cầu Giấy', 3, 'Phường Trung Hòa', 'Trần Duy Hưng'), ('POY Cầu Giấy', 4, 'Phường Quan Hoa', 'Nguyễn Khánh Toàn'),
    ('POY Hoàn Kiếm', 1, 'Phường Hàng Bài', 'Hàng Bài'), ('POY Hoàn Kiếm', 2, 'Phường Hàng Trống', 'Hàng Trống'),
    ('POY Hoàn Kiếm', 3, 'Phường Cửa Nam', 'Quán Sứ'), ('POY Hoàn Kiếm', 4, 'Phường Phan Chu Trinh', 'Lý Thường Kiệt'),
    ('POY Đà Nẵng', 1, 'Phường Thạch Thang', 'Lê Duẩn'), ('POY Đà Nẵng', 2, 'Phường Hòa Cường Bắc', 'Núi Thành'),
    ('POY Đà Nẵng', 3, 'Phường Phước Ninh', 'Nguyễn Văn Linh'), ('POY Đà Nẵng', 4, 'Phường Bình Hiên', 'Trưng Nữ Vương');

create temp table ss_seller on commit drop as
select a.store_id, e.employee_id, e.user_id,
       a.position_at_store = 'Quản lý chi nhánh' as manager,
       row_number() over (partition by a.store_id order by e.employee_id) as rn,
       count(*) over (partition by a.store_id) as n
from employee_assignments a
join employees e on e.employee_id = a.employee_id
where a.is_active and a.end_date is null and e.is_active
  and a.store_id in (select store_id from ss_store);

-- 2.400 khách mẫu mới (thêm vào khách của 04): tên tiếng Việt, đăng ký 03/2025–04/2026, email .test,
-- mật khẩu chung SampleOnly123!. 3.000 đơn chia cho ~2.800 khách thì đa số chỉ mua một lần như ngoài đời.
create temp table ss_new_customer on commit drop as
select n,
       (array['Nguyễn', 'Nguyễn', 'Nguyễn', 'Trần', 'Trần', 'Lê', 'Lê', 'Phạm', 'Hoàng', 'Huỳnh',
              'Phan', 'Vũ', 'Võ', 'Đặng', 'Bùi', 'Đỗ', 'Hồ', 'Ngô', 'Dương', 'Lý'])[1 + floor(random() * 20)::int] as family,
       random() < 0.5 as female, random() as r_middle, random() as r_given, random() as r_prefix,
       timestamp '2025-03-01' + random() * interval '420 days' as joined
from generate_series(1, 2400) n;

alter table ss_new_customer add column middle varchar(20), add column given varchar(20), add column username varchar(50);

update ss_new_customer
set middle = case when female
                  then (array['Thị', 'Ngọc', 'Thu', 'Kim', 'Thanh', 'Bảo', 'Mỹ', 'Phương'])[1 + floor(r_middle * 8)::int]
                  else (array['Văn', 'Minh', 'Quốc', 'Đức', 'Hoàng', 'Gia', 'Thanh', 'Hữu'])[1 + floor(r_middle * 8)::int] end,
    given = case when female
                 then (array['Anh', 'Châu', 'Chi', 'Giang', 'Hà', 'Hạnh', 'Hằng', 'Hoa', 'Hương', 'Lan', 'Linh', 'Ly',
                             'Mai', 'My', 'Ngân', 'Nhi', 'Nhung', 'Oanh', 'Phương', 'Quỳnh', 'Tâm', 'Thảo', 'Trang',
                             'Trâm', 'Uyên', 'Vy', 'Yến'])[1 + floor(r_given * 27)::int]
                 else (array['An', 'Bảo', 'Bình', 'Dũng', 'Duy', 'Hải', 'Hiếu', 'Hùng', 'Huy', 'Khang', 'Khoa', 'Long',
                             'Minh', 'Nam', 'Phong', 'Phúc', 'Quân', 'Quang', 'Sơn', 'Tài', 'Thắng', 'Thịnh', 'Trí',
                             'Trung', 'Tuấn', 'Việt'])[1 + floor(r_given * 26)::int] end;

-- "Trần Thu Hà" → hatran0123 (bỏ dấu bằng translate, không cần extension unaccent)
update ss_new_customer
set username = translate(lower(given || family),
                         'àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ',
                         'aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd')
               || lpad(n::text, 4, '0');

insert into users (email, username, password_hash, fullname, phone, is_active, created_at, updated_at)
select username || '@example.test', username, '$2a$10$nbH0tjwxfDyh2MbSSMFZhuuymQZ2ZiosLeYzvvFllcrS9SXbdL3bC',
       family || ' ' || middle || ' ' || given,
       (array['09', '03', '07', '08'])[1 + floor(r_prefix * 4)::int] || lpad(((n * 7919 + 4513) % 100000000)::text, 8, '0'),
       true, joined, joined
from ss_new_customer
on conflict do nothing;

insert into user_roles (user_id, role_id, assigned_at)
select u.user_id, r.role_id, u.created_at
from ss_new_customer n
join users u on u.username = n.username
join roles r on r.name = 'CUSTOMER'
on conflict do nothing;

-- người mua: chỉ tài khoản mẫu (.test), không bao giờ gắn đơn giả vào tài khoản thật
create temp table ss_customer on commit drop as
select u.user_id, u.email,
       trim(regexp_replace(u.fullname, '\s*[\[(][^\])]*[\])]\s*$', '')) as fullname,
       case when u.phone ~ '^0[1-9][0-9]{8}$' and u.phone !~ '^090000'
            then u.phone else '09' || lpad(((u.user_id * 7919 + 1234567) % 100000000)::text, 8, '0') end as phone
from users u
where u.is_active and u.deleted_at is null
  and u.email like '%@example.test'
  and exists (select 1 from user_roles ur join roles r on r.role_id = ur.role_id
              where ur.user_id = u.user_id and r.name = 'CUSTOMER')
  and not exists (select 1 from employees e where e.user_id = u.user_id);

-- số đơn mỗi khách: ~80% mua 1 đơn, ~18% mua 2–3 đơn, ~1,5% khách quen mua 6–11 đơn; mỗi đơn lấy một "lượt" ngẫu nhiên
create temp table ss_slot on commit drop as
select c.user_id, row_number() over (order by random()) as slot_no
from (select user_id, random() as r from ss_customer) c
cross join lateral generate_series(1, case when c.r < 0.015 then 6 + floor(c.r * 400)::int
                                           when c.r < 0.20 then 2 + floor((c.r - 0.015) * 10)::int
                                           else 1 end) g;

-- mỗi danh mục: phiên bản xếp theo thứ tự giả ngẫu nhiên cố định; chọn lệch về đầu danh sách (hàng bán chạy)
create temp table ss_variant on commit drop as
select v.variant_id, c.slug,
       coalesce(nullif(v.discount_price, 0), v.price) as unit_price,
       row_number() over (partition by c.slug order by md5('sales-' || v.variant_id)) as rn,
       count(*) over (partition by c.slug) as n
from product_variants v
join products p on p.product_id = v.product_id
join categories c on c.category_id = p.category_id
where p.is_active and p.deleted_at is null and v.price > 0;

-- tỉ lệ đơn theo danh mục (cộng dồn để bốc ngẫu nhiên)
create temp table ss_category (slug varchar(100), lo numeric, hi numeric) on commit drop;
insert into ss_category values
    ('dien-thoai', 0.00, 0.40), ('laptop', 0.40, 0.58), ('may-tinh-bang', 0.58, 0.70),
    ('dong-ho-thong-minh', 0.70, 0.79), ('dong-ho-thoi-trang', 0.79, 0.87), ('phu-kien', 0.87, 1.00);

-- ---------- kế hoạch đơn: số đơn mỗi ngày mỗi chi nhánh theo mùa / cuối tuần / chi nhánh

create temp table ss_plan on commit drop as
with slots as (
    select d::date as day, s.store_id,
           floor(2.55 * s.weight
                 * case extract(month from d) when 5 then 0.85 when 6 then 0.90 when 7 then 0.95
                                              when 8 then 1.00 when 9 then 1.30 else 1.10 end
                 * case when extract(isodow from d) >= 6 then 1.30 else 1.00 end
                 * (0.55 + 0.9 * random()) + random())::int as orders
    from generate_series(date '2026-05-01', date '2026-10-29', interval '1 day') d
    cross join ss_store s
),
raw as (
    select sl.day, sl.store_id,
           sl.day + interval '8 hours' + (random() * 13.5) * interval '1 hour' as ordered_at,
           random() as r_customer, random() as r_delivery, random() as r_payment, random() as r_outcome,
           random() as r_seller, random() as r_ward, random() as r_lines
    from slots sl
    cross join lateral generate_series(1, sl.orders) g
)
select row_number() over (order by ordered_at, store_id) as order_no, raw.*
from raw;

alter table ss_plan add column order_id bigint, add column user_id bigint, add column seller_employee bigint,
    add column seller_user bigint, add column delivery_type varchar(20), add column payment_method varchar(50),
    add column status varchar(20), add column returned boolean, add column confirmed_at timestamp,
    add column shipping_at timestamp, add column delivered_at timestamp, add column cancelled_at timestamp,
    add column shipping_address text, add column province varchar(100), add column ward varchar(100);

update ss_plan
set delivery_type = case when r_delivery < 0.30 then 'PICKUP' else 'HOME_DELIVERY' end,
    payment_method = case when r_payment < 0.40 then 'COD' when r_payment < 0.75 then 'BANK_TRANSFER' else 'CARD' end;

update ss_plan p
set user_id = s.user_id
from ss_slot s
where s.slot_no = p.order_no;

update ss_plan p
set seller_employee = s.employee_id, seller_user = s.user_id
from ss_seller s
where s.store_id = p.store_id and s.rn = 1 + floor(p.r_seller * s.n);

-- trạng thái + mốc thời gian: huỷ ~5%, 2 ngày cuối đang giao, còn lại đã giao (~3% sau đó trả hàng)
update ss_plan
set status = case when r_outcome < 0.05 then 'CANCELLED'
                  when ordered_at >= timestamp '2026-10-28' then 'SHIPPING'
                  else 'DELIVERED' end,
    returned = r_outcome >= 0.05 and r_outcome < 0.08 and ordered_at < timestamp '2026-10-15',
    confirmed_at = ordered_at + (20 + 100 * r_seller) * interval '1 minute';

update ss_plan
set cancelled_at = case when status = 'CANCELLED' then ordered_at + (1 + 10 * r_ward) * interval '1 hour' end,
    confirmed_at = case when status = 'CANCELLED' then null else confirmed_at end,
    shipping_at = case when status <> 'CANCELLED' then confirmed_at + (2 + 6 * r_lines) * interval '1 hour' end;

update ss_plan
set delivered_at = case when status = 'DELIVERED' then
        shipping_at + case when delivery_type = 'PICKUP' then (1 + 3 * r_customer) * interval '1 hour'
                           else (18 + 50 * r_customer) * interval '1 hour' end end;

update ss_plan p
set shipping_address = case when p.delivery_type = 'PICKUP'
            then s.address || ', ' || s.district || ', ' || s.city
            else (1 + floor(p.r_lines * 400))::int || ' ' || w.street || ', ' || w.ward || ', '
                 || s.district_label || ', ' || s.city end,
    province = s.city,
    ward = case when p.delivery_type = 'PICKUP' then null else w.ward end
from ss_store s
join ss_ward w on w.store_name = s.name
where s.store_id = p.store_id and w.no = 1 + floor(p.r_ward * 4);

-- ---------- dòng hàng: 1 sản phẩm (lệch về hàng bán chạy), ~25% đơn mua thêm 1 phụ kiện (chọn đều)

create temp table ss_line_plan on commit drop as
select p.order_no, p.store_id, p.ordered_at, p.status, 1 as line_no, random() as r_cat, random() as r_pick, random() as r_qty
from ss_plan p
union all
select p.order_no, p.store_id, p.ordered_at, p.status, 2, 0.87 + 0.13 * random(), random(), random()
from ss_plan p
where p.r_lines < 0.25;

create temp table ss_line on commit drop as
select lp.order_no, lp.store_id, lp.ordered_at, lp.status, lp.line_no, v.variant_id, v.unit_price,
       case when v.slug = 'phu-kien' and lp.r_qty < 0.25 then 2 else 1 end as quantity
from ss_line_plan lp
join ss_category c on lp.r_cat >= c.lo and lp.r_cat < c.hi
join ss_variant v on v.slug = c.slug and v.rn = 1 + floor(v.n * power(lp.r_pick, case when lp.line_no = 1 then 2.0 else 1.0 end));

delete from ss_line l
using ss_line first
where l.line_no = 2 and first.line_no = 1 and first.order_no = l.order_no and first.variant_id = l.variant_id;

-- không bán quá tồn kho chi nhánh (đơn huỷ chưa trừ kho nên không tính)
delete from ss_line l
using (
    select x.order_no, x.line_no
    from (
        select l.order_no, l.line_no, coalesce(i.quantity, 0) as stock,
               sum(l.quantity) over (partition by l.store_id, l.variant_id
                                     order by l.ordered_at, l.order_no, l.line_no) as used
        from ss_line l
        left join inventory i on i.store_id = l.store_id and i.variant_id = l.variant_id
        where l.status <> 'CANCELLED'
    ) x
    where x.used > x.stock
) over_stock
where l.order_no = over_stock.order_no and l.line_no = over_stock.line_no;

delete from ss_line l where l.status = 'CANCELLED'
  and not exists (select 1 from inventory i where i.store_id = l.store_id and i.variant_id = l.variant_id);

delete from ss_plan p where not exists (select 1 from ss_line l where l.order_no = p.order_no);
delete from ss_plan p where p.user_id is null or p.seller_employee is null or p.shipping_address is null;

alter table ss_plan add column subtotal numeric(15, 2), add column shipping_cost numeric(15, 2),
    add column total numeric(15, 2), add column points int;

update ss_plan p
set subtotal = t.subtotal,
    shipping_cost = case when p.delivery_type = 'HOME_DELIVERY' and t.subtotal < 10000000 then 30000 else 0 end
from (select order_no, sum(unit_price * quantity) as subtotal from ss_line group by order_no) t
where t.order_no = p.order_no;

-- mã đơn tăng theo thời gian đặt, nối tiếp mã đơn đang có
update ss_plan
set total = subtotal + shipping_cost,
    points = case when status = 'DELIVERED' then floor(subtotal / 10000)::int else 0 end,
    order_id = greatest((select coalesce(max(order_id), 0) from orders),
                        (select last_value from orders_order_id_seq)) + order_no;

select setval('orders_order_id_seq', (select max(order_id) from ss_plan));

-- ---------- ghi vào các bảng của app

insert into orders (order_id, user_id, order_date, shipping_address, billing_address, shipping_cost, tax_amount,
                    total_amount, status, payment_method, tracking_number, delivered_at, cancelled_at, created_at,
                    updated_at, store_id, delivery_type, recipient_name, recipient_phone, shipping_province,
                    shipping_ward, points_earned)
select p.order_id, p.user_id, p.ordered_at, p.shipping_address, p.shipping_address, p.shipping_cost, 0,
       p.total, p.status, p.payment_method,
       case when p.delivery_type = 'HOME_DELIVERY' and p.status <> 'CANCELLED' then 'POY' || lpad(p.order_id::text, 9, '0') end,
       p.delivered_at, p.cancelled_at, p.ordered_at,
       coalesce(p.delivered_at, p.cancelled_at, p.shipping_at), p.store_id, p.delivery_type, c.fullname, c.phone,
       p.province, p.ward, p.points
from ss_plan p
join ss_customer c on c.user_id = p.user_id;

create temp table ss_item on commit drop as
select l.*, p.order_id, nextval('order_items_order_item_id_seq') as order_item_id
from ss_line l
join ss_plan p on p.order_no = l.order_no;

insert into order_items (order_item_id, order_id, variant_id, quantity, unit_price, discount_amount, subtotal)
select order_item_id, order_id, variant_id, quantity, unit_price, 0, unit_price * quantity
from ss_item;

insert into order_status_history (order_id, old_status, new_status, changed_by, changed_at)
select order_id, null, 'PENDING', user_id, ordered_at from ss_plan
union all
select order_id, 'PENDING', 'CANCELLED', user_id, cancelled_at from ss_plan where status = 'CANCELLED'
union all
select order_id, 'PENDING', 'CONFIRMED', seller_user, confirmed_at from ss_plan where status <> 'CANCELLED'
union all
select order_id, 'CONFIRMED', 'SHIPPING', seller_user, shipping_at from ss_plan where status <> 'CANCELLED'
union all
select order_id, 'SHIPPING', 'DELIVERED', seller_user, delivered_at from ss_plan where status = 'DELIVERED';

-- COD trả khi nhận hàng; chuyển khoản / thẻ trả ngay khi đặt (chuyển khoản do nhân viên xác nhận)
insert into payments (order_id, amount, payment_method, transaction_id, status, paid_at, created_at, updated_at, confirmed_by)
select p.order_id, p.total, p.payment_method,
       case p.payment_method when 'BANK_TRANSFER' then 'FT' || to_char(p.ordered_at, 'YYMMDD') || lpad(p.order_id::text, 6, '0')
                             when 'CARD' then 'CARD-' || to_char(p.ordered_at, 'YYMMDD') || lpad(p.order_id::text, 6, '0') end,
       case when p.status = 'CANCELLED' then 'CANCELLED'
            when p.payment_method = 'COD' and p.status <> 'DELIVERED' then 'PENDING'
            else 'PAID' end,
       case when p.status = 'CANCELLED' then null
            when p.payment_method = 'COD' then p.delivered_at
            else p.ordered_at + interval '5 minutes' end,
       p.ordered_at,
       coalesce(p.delivered_at, p.cancelled_at, p.shipping_at),
       case when p.payment_method = 'BANK_TRANSFER' and p.status <> 'CANCELLED' then p.seller_user end
from ss_plan p;

-- xuất kho khi xác nhận đơn
insert into stock_movements (store_id, variant_id, movement_type, quantity_change, order_id, created_by, created_at)
select i.store_id, i.variant_id, 'OUT', -sum(i.quantity), i.order_id, p.seller_user, p.confirmed_at
from ss_item i
join ss_plan p on p.order_no = i.order_no
where p.status <> 'CANCELLED'
group by i.store_id, i.variant_id, i.order_id, p.seller_user, p.confirmed_at;

update inventory inv
set quantity = inv.quantity - t.quantity, updated_at = current_timestamp
from (
    select i.store_id, i.variant_id, sum(i.quantity) as quantity
    from ss_item i join ss_plan p on p.order_no = i.order_no
    where p.status <> 'CANCELLED'
    group by i.store_id, i.variant_id
) t
where inv.store_id = t.store_id and inv.variant_id = t.variant_id;

insert into sales_records (store_id, employee_id, order_id, sales_amount, commission, recorded_at)
select store_id, seller_employee, order_id, total, round(total * 0.01), delivered_at
from ss_plan
where status = 'DELIVERED';

-- ---------- trả hàng đã hoàn tiền (~3% đơn đã giao trước 15/10)

create temp table ss_return on commit drop as
select p.order_no, p.order_id, p.user_id, p.store_id, p.subtotal,
       nextval('return_requests_return_request_id_seq') as return_request_id,
       (array['DEFECTIVE', 'NOT_AS_DESCRIBED', 'CHANGED_MIND', 'DEFECTIVE'])[1 + floor(p.r_ward * 4)::int] as reason_type,
       p.delivered_at + (1 + 4 * p.r_payment) * interval '1 day' as created_at,
       (select s.user_id from ss_seller s where s.store_id = p.store_id order by s.manager desc, s.rn limit 1) as handler
from ss_plan p
where p.status = 'DELIVERED' and p.returned;

insert into return_requests (return_request_id, order_id, user_id, reason, status, refund_amount, approved_at,
                             received_at, completed_at, created_at, updated_at, reason_type, assigned_to_employee)
select r.return_request_id, r.order_id, r.user_id,
       case r.reason_type when 'DEFECTIVE' then 'Máy lỗi sau vài ngày sử dụng, cửa hàng kiểm tra xác nhận lỗi phần cứng.'
                          when 'NOT_AS_DESCRIBED' then 'Sản phẩm nhận được không đúng màu / cấu hình đã đặt.'
                          else 'Khách không còn nhu cầu, máy còn nguyên hộp, chưa kích hoạt.' end,
       'REFUNDED', r.subtotal,
       r.created_at + interval '4 hours', r.created_at + interval '1 day', r.created_at + interval '2 days',
       r.created_at, r.created_at + interval '2 days', r.reason_type, r.handler
from ss_return r;

-- máy lỗi không nhập lại kho; còn lại nhập lại chi nhánh đã bán
insert into return_items (return_request_id, order_item_id, quantity, refund_amount, restocked)
select r.return_request_id, i.order_item_id, i.quantity, i.unit_price * i.quantity, r.reason_type <> 'DEFECTIVE'
from ss_return r
join ss_item i on i.order_no = r.order_no;

insert into stock_movements (store_id, variant_id, movement_type, quantity_change, order_id, created_by, created_at)
select r.store_id, i.variant_id, 'RETURN', sum(i.quantity), r.order_id, r.handler, r.created_at + interval '2 days'
from ss_return r
join ss_item i on i.order_no = r.order_no
where r.reason_type <> 'DEFECTIVE'
group by r.store_id, i.variant_id, r.order_id, r.handler, r.created_at;

update inventory inv
set quantity = inv.quantity + t.quantity, updated_at = current_timestamp
from (
    select r.store_id, i.variant_id, sum(i.quantity) as quantity
    from ss_return r join ss_item i on i.order_no = r.order_no
    where r.reason_type <> 'DEFECTIVE'
    group by r.store_id, i.variant_id
) t
where inv.store_id = t.store_id and inv.variant_id = t.variant_id;

-- ---------- điểm thưởng: cộng khi giao (1 điểm / 10.000đ hàng), thu hồi khi hoàn tiền; hồ sơ khách + hạng

insert into customer_profiles (customer_id)
select distinct p.user_id from ss_plan p
where not exists (select 1 from customer_profiles cp where cp.customer_id = p.user_id);

create temp table ss_points on commit drop as
select p.user_id, p.order_id, null::bigint as return_request_id, 'EARN' as type, p.points,
       'Tích điểm đơn #' || p.order_id as note, p.delivered_at as at
from ss_plan p where p.status = 'DELIVERED' and p.points > 0
union all
select r.user_id, r.order_id, r.return_request_id, 'REVOKE', -p.points,
       'Thu hồi điểm do hoàn tiền đơn #' || r.order_id, r.created_at + interval '2 days'
from ss_return r join ss_plan p on p.order_no = r.order_no
where p.points > 0;

insert into loyalty_transactions (user_id, order_id, return_request_id, type, points, balance_after, note, created_at)
select pt.user_id, pt.order_id, pt.return_request_id, pt.type, pt.points,
       cp.loyalty_points + sum(pt.points) over (partition by pt.user_id order by pt.at, pt.type rows unbounded preceding),
       pt.note, pt.at
from ss_points pt
join customer_profiles cp on cp.customer_id = pt.user_id;

update customer_profiles cp
set loyalty_points = cp.loyalty_points + t.points,
    total_spent = cp.total_spent + t.spent,
    updated_at = current_timestamp
from (
    select p.user_id,
           sum(case when p.status = 'DELIVERED' then p.total else 0 end) as spent,
           coalesce((select sum(pt.points) from ss_points pt where pt.user_id = p.user_id), 0) as points
    from ss_plan p
    group by p.user_id
) t
where cp.customer_id = t.user_id;

update customer_profiles cp
set tier_code = (select t.tier_code from loyalty_tiers t where t.min_spent <= cp.total_spent
                 order by t.sort_order desc limit 1)
where cp.customer_id in (select user_id from ss_plan);

select count(*) as orders,
       count(*) filter (where status = 'DELIVERED') as delivered,
       count(*) filter (where status = 'CANCELLED') as cancelled,
       count(*) filter (where status = 'SHIPPING') as shipping,
       (select count(*) from ss_return) as refunded,
       sum(total) filter (where status = 'DELIVERED') as revenue
from ss_plan;

commit;
