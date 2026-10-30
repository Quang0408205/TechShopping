-- Hoạt động mẫu của khách (2026-10-30): dữ liệu demo, KHÔNG dùng cho production. Chạy sau 07_sample_sales.sql.
--   * phiếu bảo hành cho mỗi dòng hàng đã giao (như backend cấp khi đơn thành "Đã giao");
--   * voucher (07_sample_promotions.sql) trên một phần đơn đặt trong thời gian áp dụng của voucher: tiền giảm chia
--     theo dòng hàng, tổng đơn / thanh toán / doanh số / hoa hồng / điểm thưởng / hạng tính lại cho khớp;
--   * đánh giá của khách đã nhận hàng (sao + nhận xét theo danh mục) kèm lượt thích, điểm sao của sản phẩm;
--   * danh sách yêu thích, tin nhắn Liên hệ, yêu cầu bảo hành / bảo trì (đủ các trạng thái).
--   Chỉ dùng tài khoản mẫu @example.test (email không gửi được, backend bỏ qua khi gửi mail).
--   Không có: combo trên đơn (combo mẫu mới bắt đầu, đơn mới trên web sẽ có), ảnh đánh giá / ảnh hậu mãi.
-- Docker chạy file này sau 07 / 08 khi tạo volume mới (docker-compose.yml). Chỉ chạy được một lần
-- (dev_sample_seed_registry); muốn nạp lại phải dựng lại volume.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/docker-init/09_sample_activity.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:*.

begin;

create table if not exists dev_sample_seed_registry (
    seed_key varchar(100) primary key,
    applied_at timestamp not null default current_timestamp
);

do $$
begin
    if current_database() <> 'techshopping' then
        raise exception 'Chỉ nạp hoạt động mẫu vào database techshopping.';
    end if;
    if exists (select 1 from dev_sample_seed_registry where seed_key = 'techshopping-sample-activity-v1') then
        raise exception 'Hoạt động mẫu đã được nạp (dev_sample_seed_registry: techshopping-sample-activity-v1).';
    end if;
end $$;

insert into dev_sample_seed_registry (seed_key) values ('techshopping-sample-activity-v1');

select setseed(0.3031);

-- khách mẫu (không bao giờ là tài khoản thật) và dòng hàng của họ
create temp table sa_customer on commit drop as
select u.user_id, u.fullname, u.email, u.phone, u.created_at
from users u
where u.email like '%@example.test' and u.is_active and u.deleted_at is null
  and exists (select 1 from user_roles ur join roles r on r.role_id = ur.role_id
              where ur.user_id = u.user_id and r.name = 'CUSTOMER')
  and not exists (select 1 from employees e where e.user_id = u.user_id);

create temp table sa_item on commit drop as
select o.order_id, o.user_id, o.store_id, o.status, o.order_date, o.delivered_at,
       oi.order_item_id, oi.variant_id, oi.subtotal, v.product_id, p.category_id, c.slug, p.warranty_months,
       p.is_active and p.deleted_at is null as product_active,
       exists (select 1 from return_requests r where r.order_id = o.order_id) as returned
from orders o
join sa_customer sc on sc.user_id = o.user_id
join order_items oi on oi.order_id = o.order_id
join product_variants v on v.variant_id = oi.variant_id
join products p on p.product_id = v.product_id
join categories c on c.category_id = p.category_id;

-- kỹ thuật viên / người bán của từng chi nhánh (users.user_id) để giao việc hậu mãi, trả lời liên hệ
create temp table sa_staff on commit drop as
select a.store_id, e.user_id, e.position
from employee_assignments a
join employees e on e.employee_id = a.employee_id
where a.is_active and a.end_date is null and e.is_active;

-- ---------- phiếu bảo hành: mỗi dòng hàng đã giao (không trả hàng), từ ngày giao, theo số tháng của sản phẩm

insert into warranties (order_item_id, warranty_start_date, warranty_end_date, is_active, created_at)
select i.order_item_id, i.delivered_at::date, (i.delivered_at::date + make_interval(months => i.warranty_months))::date,
       true, i.delivered_at
from sa_item i
where i.status = 'DELIVERED' and not i.returned and coalesce(i.warranty_months, 0) > 0
on conflict (order_item_id) do nothing;

-- ---------- voucher trên đơn: đơn đặt trong thời gian áp dụng, đủ giá trị tối thiểu, mỗi đơn một mã

create temp table sa_order on commit drop as
select o.order_id, o.user_id, o.order_date, o.status, o.delivered_at, o.points_earned,
       (select sum(oi.subtotal) from order_items oi where oi.order_id = o.order_id) as goods,
       o.order_id = (select min(o2.order_id) from orders o2 where o2.user_id = o.user_id) as first_order
from orders o
join sa_customer c on c.user_id = o.user_id
where o.status in ('DELIVERED', 'SHIPPING') and o.voucher_id is null
  and coalesce(o.combo_discount, 0) = 0 and coalesce(o.points_discount, 0) = 0
  and not exists (select 1 from return_requests r where r.order_id = o.order_id);

create temp table sa_voucher on commit drop as
select v.voucher_id, v.code, v.discount_percent, v.max_discount, v.min_order_amount, v.starts_at, v.ends_at,
       v.per_customer_limit, v.all_categories,
       coalesce(v.usage_limit, 1000000) - v.used_count as uses_left,
       case v.code when 'CHAOBAN5' then 0.45 when 'THANG9' then 0.30 when 'LAPTOP10' then 0.50
                   when 'PHUKIEN15' then 0.35 when 'DONGHO20' then 0.50 else 0.25 end as take_rate
from vouchers v
where v.is_active;

create temp table sa_pick on commit drop as
with eligible as (
    select so.order_id, so.user_id, so.order_date, so.status, so.goods, so.points_earned,
           v.voucher_id, v.code, v.discount_percent, v.max_discount, v.per_customer_limit, v.uses_left,
           v.all_categories, v.take_rate, sum(i.subtotal) as eligible_amount
    from sa_order so
    cross join sa_voucher v
    join sa_item i on i.order_id = so.order_id
    where so.order_date >= v.starts_at and so.order_date < v.ends_at
      and (v.code <> 'CHAOBAN5' or so.first_order)
      and (v.all_categories or i.category_id in (select vc.category_id from voucher_categories vc
                                                 where vc.voucher_id = v.voucher_id))
    group by so.order_id, so.user_id, so.order_date, so.status, so.goods, so.points_earned, v.voucher_id, v.code,
             v.discount_percent, v.max_discount, v.per_customer_limit, v.uses_left, v.all_categories, v.take_rate,
             v.min_order_amount
    having sum(i.subtotal) >= v.min_order_amount
),
taken as (
    select e.*, random() as r from eligible e
),
one_per_order as (
    select distinct on (t.order_id) t.* from taken t where t.r < t.take_rate order by t.order_id, t.r
),
limited as (
    select o.*,
           row_number() over (partition by o.voucher_id, o.user_id order by o.order_date) as customer_use,
           row_number() over (partition by o.voucher_id order by o.order_date) as voucher_use
    from one_per_order o
)
select l.order_id, l.user_id, l.order_date, l.status, l.goods, l.voucher_id, l.code, l.all_categories,
       l.eligible_amount, l.points_earned as old_points,
       least(floor(l.eligible_amount * l.discount_percent / 100), coalesce(l.max_discount, l.eligible_amount),
             l.eligible_amount) as discount
from limited l
where l.customer_use <= l.per_customer_limit and l.voucher_use <= l.uses_left;

alter table sa_pick add column new_points int;
update sa_pick set new_points = case when status = 'DELIVERED' then floor((goods - discount) / 10000)::int else 0 end;

-- tiền giảm chia theo dòng hàng thuộc phạm vi voucher (làm tròn xuống, phần dư vào dòng lớn nhất) như VoucherCalculator
create temp table sa_share on commit drop as
with lines as (
    select p.order_id, i.order_item_id, p.discount,
           floor(i.subtotal * p.discount / p.eligible_amount) as base,
           row_number() over (partition by p.order_id order by i.subtotal desc, i.order_item_id) as rk
    from sa_pick p
    join sa_item i on i.order_id = p.order_id
    where p.all_categories or i.category_id in (select vc.category_id from voucher_categories vc
                                                where vc.voucher_id = p.voucher_id)
)
select order_id, order_item_id,
       base + case when rk = 1 then discount - sum(base) over (partition by order_id) else 0 end as share
from lines;

update order_items oi set voucher_discount = s.share
from sa_share s
where oi.order_item_id = s.order_item_id;

update orders o
set voucher_id = p.voucher_id, voucher_code = p.code, voucher_discount = p.discount,
    total_amount = o.total_amount - p.discount,
    points_earned = case when p.status = 'DELIVERED' then p.new_points else o.points_earned end
from sa_pick p
where o.order_id = p.order_id;

update payments pm set amount = pm.amount - p.discount
from sa_pick p
where pm.order_id = p.order_id;

update sales_records sr
set sales_amount = sr.sales_amount - p.discount, commission = round((sr.sales_amount - p.discount) * 0.01)
from sa_pick p
where sr.order_id = p.order_id;

insert into voucher_usages (voucher_id, user_id, order_id, discount_amount, used_at)
select voucher_id, user_id, order_id, discount, order_date from sa_pick;

update vouchers v set used_count = v.used_count + t.n, updated_at = current_timestamp
from (select voucher_id, count(*) as n from sa_pick group by voucher_id) t
where v.voucher_id = t.voucher_id;

-- điểm: đơn đã giao tích ít điểm hơn → sửa giao dịch EARN, số dư các giao dịch sau đó, hồ sơ khách + hạng
create temp table sa_earn on commit drop as
select lt.user_id, lt.loyalty_transaction_id, lt.created_at, lt.type, p.old_points - p.new_points as delta
from sa_pick p
join loyalty_transactions lt on lt.order_id = p.order_id and lt.type = 'EARN'
where p.status = 'DELIVERED' and p.old_points <> p.new_points;

update loyalty_transactions lt set points = lt.points - e.delta
from sa_earn e
where lt.loyalty_transaction_id = e.loyalty_transaction_id;

-- số dư sau mỗi giao dịch = cộng dồn theo thời gian (mọi điểm của khách mẫu đều nằm trong bảng); cũng sửa thứ tự
-- số dư khi 07 thêm giao dịch cũ hơn giao dịch của 04
update loyalty_transactions lt set balance_after = s.balance
from (
    select t.loyalty_transaction_id,
           sum(t.points) over (partition by t.user_id order by t.created_at, t.type, t.loyalty_transaction_id) as balance
    from loyalty_transactions t
    where t.user_id in (select user_id from sa_customer)
) s
where lt.loyalty_transaction_id = s.loyalty_transaction_id and lt.balance_after is distinct from s.balance;

update customer_profiles cp
set loyalty_points = cp.loyalty_points - coalesce(t.points, 0),
    total_spent = cp.total_spent - t.spent,
    updated_at = current_timestamp
from (
    select p.user_id,
           sum(case when p.status = 'DELIVERED' then p.discount else 0 end) as spent,
           (select sum(e.delta) from sa_earn e where e.user_id = p.user_id) as points
    from sa_pick p
    group by p.user_id
) t
where cp.customer_id = t.user_id;

update customer_profiles cp
set tier_code = (select t.tier_code from loyalty_tiers t where t.min_spent <= cp.total_spent
                 order by t.sort_order desc limit 1)
where cp.customer_id in (select user_id from sa_pick);

-- ---------- đánh giá: ~1/4 sản phẩm đã nhận (phụ kiện ít hơn), 1–14 ngày sau khi giao, mỗi khách một lần / sản phẩm

create temp table sa_phrase (slug varchar(40), tone varchar(10), phrases text[]) on commit drop;
insert into sa_phrase values
    ('dien-thoai', 'good', array[
        'Máy chạy mượt, pin dùng cả ngày vẫn còn khoảng 30%.',
        'Camera chụp đẹp, nhất là ảnh chân dung và chụp đêm.',
        'Màn hình sáng, màu đẹp, cầm chắc tay.',
        'Mua cho bố mẹ dùng, chữ to, dễ sử dụng.',
        'Chơi game ổn định, máy chỉ hơi ấm sau một tiếng.',
        'Sạc nhanh, loa to và rõ, rất hài lòng.',
        'Lên đời từ máy cũ, thấy khác biệt rõ về tốc độ.']),
    ('laptop', 'good', array[
        'Máy mỏng nhẹ, bỏ balo mang đi học rất tiện.',
        'Bàn phím gõ êm, màn hình sắc nét.',
        'Chạy Word, Excel, họp Zoom mượt, pin được 7–8 tiếng.',
        'Hiệu năng tốt, xuất video nhanh hơn máy cũ nhiều.',
        'Tản nhiệt ổn, chơi game không bị nóng bàn phím.',
        'Loa nghe nhạc hay, webcam rõ khi học online.']),
    ('may-tinh-bang', 'good', array[
        'Màn hình lớn xem phim rất đã.',
        'Mua cho con học online, rất ổn.',
        'Ghi chú bằng bút mượt, pin dùng hai ngày.',
        'Máy nhẹ, cầm đọc sách lâu không mỏi tay.']),
    ('dong-ho-thong-minh', 'good', array[
        'Đo nhịp tim và giấc ngủ khá chính xác.',
        'Pin được gần một tuần mới phải sạc.',
        'Dây đeo êm, đeo cả ngày không khó chịu.',
        'Nhận thông báo tin nhắn nhanh, chạy bộ có GPS tiện.']),
    ('dong-ho-thoi-trang', 'good', array[
        'Đồng hồ đẹp, nhìn ngoài sang hơn trong ảnh.',
        'Dây da mềm, mặt kính sáng.',
        'Mua làm quà tặng, người nhận rất thích.',
        'Đeo đi làm hợp với áo sơ mi, chạy giờ chuẩn.']),
    ('phu-kien', 'good', array[
        'Sạc nhanh, dây chắc chắn.',
        'Âm thanh hay, bass ổn so với giá.',
        'Ốp vừa khít máy, cầm không bị trơn.',
        'Kết nối Bluetooth nhanh, không bị trễ tiếng.']),
    ('*', 'service', array[
        'Nhân viên tư vấn nhiệt tình.',
        'Giao hàng nhanh, đóng gói cẩn thận.',
        'Hàng chính hãng, đầy đủ phụ kiện.',
        'Giá tốt hơn mấy chỗ khác.',
        'Nhận tại cửa hàng nhanh, nhân viên cài đặt giúp luôn.',
        'Sẽ ủng hộ shop lần sau.']),
    ('*', 'ok', array[
        'Dùng tạm ổn, pin hơi nhanh hết.',
        'Máy ổn nhưng hơi nóng khi dùng lâu.',
        'Chất lượng tương xứng giá, không có gì nổi bật.',
        'Giao hơi chậm so với dự kiến, sản phẩm thì ổn.',
        'Đúng mô tả, nhưng hộp hơi móp.']),
    ('*', 'bad', array[
        'Máy bị lỗi loa sau vài ngày, phải mang ra bảo hành.',
        'Giao hàng chậm, gọi mấy lần mới có người nghe.',
        'Pin tụt nhanh hơn quảng cáo khá nhiều.',
        'Màu thực tế khác ảnh trên web.']);

create temp table sa_review on commit drop as
select distinct on (i.user_id, i.product_id)
       i.user_id, i.product_id, i.variant_id, i.slug, i.delivered_at,
       random() as r_take, random() as r_star, random() as r_a, random() as r_b, random() as r_c, random() as r_day
from sa_item i
where i.status = 'DELIVERED' and not i.returned and i.product_active
order by i.user_id, i.product_id, i.delivered_at;

delete from sa_review where r_take >= case slug when 'phu-kien' then 0.14 else 0.26 end;

alter table sa_review add column rating smallint, add column created_at timestamp, add column comment text;

update sa_review
set rating = case when r_star < 0.60 then 5 when r_star < 0.84 then 4 when r_star < 0.93 then 3
                  when r_star < 0.97 then 2 else 1 end,
    created_at = delivered_at + (1 + floor(r_day * 14)) * interval '1 day' + floor(r_c * 12) * interval '1 hour';

delete from sa_review where created_at > current_timestamp - interval '1 hour';

update sa_review r
set comment = case
    when r.rating >= 4 then
        g.phrases[1 + floor(r.r_a * array_length(g.phrases, 1))::int]
        || case when r.r_b < 0.55 then ' ' || s.phrases[1 + floor(r.r_b / 0.55 * array_length(s.phrases, 1))::int] else '' end
    when r.rating = 3 then
        k.phrases[1 + floor(r.r_a * array_length(k.phrases, 1))::int]
    else
        b.phrases[1 + floor(r.r_a * array_length(b.phrases, 1))::int]
    end
from sa_phrase g, sa_phrase s, sa_phrase k, sa_phrase b
where g.slug = r.slug and g.tone = 'good'
  and s.slug = '*' and s.tone = 'service'
  and k.slug = '*' and k.tone = 'ok'
  and b.slug = '*' and b.tone = 'bad';

-- nhãn phiên bản như ReviewServiceImpl.boughtVariantLabel: bộ nhớ, RAM, màu
insert into reviews (user_id, product_id, rating, comment, is_hidden, created_at, updated_at, variant_label)
select r.user_id, r.product_id, r.rating, r.comment, false, r.created_at, r.created_at,
       coalesce(nullif(concat_ws(', ', nullif(trim(v.storage), ''), nullif(trim(v.ram), ''), nullif(trim(v.color), '')), ''),
                v.variant_name)
from sa_review r
join product_variants v on v.variant_id = r.variant_id
where r.comment is not null
order by r.created_at;

-- lượt thích: phần lớn đánh giá 0–3 lượt, vài đánh giá nhiều lượt; người thích là khách mẫu khác
create temp table sa_ids on commit drop as
select array_agg(user_id order by user_id) as ids from sa_customer;

insert into review_likes (review_id, user_id, created_at)
select rv.review_id, a.ids[1 + floor(random() * array_length(a.ids, 1))::int],
       least(rv.created_at + random() * interval '12 days', current_timestamp - interval '5 minutes')
from reviews rv
join sa_customer c on c.user_id = rv.user_id
cross join sa_ids a
cross join lateral generate_series(1, floor(power(random(), 2.5) * 14)::int + 0 * rv.review_id::int) g
where rv.comment not like 'ĐÁNH GIÁ MẪU%'
on conflict do nothing;

delete from review_likes l using reviews rv where rv.review_id = l.review_id and rv.user_id = l.user_id;

-- điểm sao + số đánh giá của sản phẩm = đánh giá đang hiện (như ProductReviewStats.refresh)
update products p
set rating = s.rating, total_reviews = s.n
from (select product_id, round(avg(rating)::numeric, 2) as rating, count(*) as n
      from reviews where not is_hidden group by product_id) s
where p.product_id = s.product_id;

-- ---------- yêu thích: ~22% khách mẫu lưu 1–4 sản phẩm, lệch về sản phẩm bán chạy

create temp table sa_popular on commit drop as
select array_agg(product_id order by sold desc, product_id) as ids
from (
    select p.product_id, count(oi.order_item_id) as sold
    from products p
    join product_variants v on v.product_id = p.product_id
    left join order_items oi on oi.variant_id = v.variant_id
    where p.is_active and p.deleted_at is null and v.price > 0
    group by p.product_id
) t;

insert into wishlist_items (user_id, product_id, price_at_add, created_at)
select w.user_id, w.product_id,
       (select min(coalesce(nullif(v.discount_price, 0), v.price)) from product_variants v
        where v.product_id = w.product_id and v.price > 0),
       w.created_at
from (
    select c.user_id,
           pp.ids[1 + floor(power(random(), 2.2) * least(array_length(pp.ids, 1), 600))::int + 0 * g] as product_id,
           greatest(c.created_at, current_timestamp - interval '90 days')
               + random() * (current_timestamp - greatest(c.created_at, current_timestamp - interval '90 days')) as created_at
    from (select user_id, created_at, random() as r from sa_customer) c
    cross join sa_popular pp
    cross join lateral generate_series(1, 1 + floor(random() * 4)::int + 0 * c.user_id::int) g
    where c.r < 0.22
) w
on conflict (user_id, product_id) do nothing;

-- ---------- tin nhắn Liên hệ: 2 tháng gần đây; cũ đã xử lý, mới đang xử lý / chưa xem

create temp table sa_contact_text (topic varchar(20), message text, note text) on commit drop;
insert into sa_contact_text values
    ('ORDER', 'Đơn hàng của tôi đặt 3 ngày rồi vẫn chưa thấy giao, nhờ shop kiểm tra giúp.', 'Đã gọi khách: đơn đang trên đường giao, dự kiến tới trong ngày mai.'),
    ('ORDER', 'Tôi muốn đổi địa chỉ nhận hàng sang công ty trong giờ hành chính được không?', 'Đã cập nhật địa chỉ giao mới cho đơn của khách.'),
    ('ORDER', 'Cho tôi hỏi có xuất hoá đơn VAT cho đơn vừa đặt được không?', 'Đã gửi hoá đơn điện tử qua email khách cung cấp.'),
    ('PRODUCT', 'iPhone màu cam bản 256GB bao giờ có hàng lại ở chi nhánh Quận 1 vậy shop?', 'Đã báo khách: hàng về chi nhánh Quận 1 trong tuần này.'),
    ('PRODUCT', 'Laptop này có nâng cấp thêm RAM được không ạ?', 'Đã tư vấn: máy có 1 khe RAM trống, nâng tối đa 32GB.'),
    ('PRODUCT', 'Đồng hồ này đeo đi bơi được không?', 'Đã tư vấn: chống nước 5ATM, đi bơi được, không lặn sâu.'),
    ('AFTER_SALES', 'Tai nghe mua tháng trước bị rè một bên, tôi mang tới chi nhánh nào để bảo hành?', 'Đã hướng dẫn khách tạo yêu cầu bảo hành và mang tới chi nhánh gần nhất.'),
    ('AFTER_SALES', 'Máy cập nhật phần mềm xong hay tự khởi động lại, nhờ shop hỗ trợ.', 'Kỹ thuật viên đã hướng dẫn khôi phục cài đặt, máy hoạt động bình thường.'),
    ('PAYMENT', 'Tôi đã chuyển khoản nhưng đơn vẫn báo chờ thanh toán.', 'Đã đối soát: tiền về sau 15 phút, đơn đã chuyển sang đã thanh toán.'),
    ('PAYMENT', 'Trả góp qua thẻ tín dụng cần những giấy tờ gì ạ?', 'Đã tư vấn: chỉ cần thẻ tín dụng chính chủ và CCCD.'),
    ('OTHER', 'Shop có tuyển cộng tác viên bán hàng không?', 'Đã gửi khách thông tin tuyển dụng của chi nhánh.'),
    ('OTHER', 'Góp ý: web nên thêm bộ lọc theo kích thước màn hình.', 'Đã ghi nhận góp ý, chuyển bộ phận phát triển web.');

create temp table sa_contact on commit drop as
select n, random() as r_customer, random() as r_text, random() as r_staff, random() as r_guest,
       current_timestamp - (0.1 + power(random(), 1.4) * 60) * interval '1 day' as created_at
from generate_series(1, 36) n;

insert into contact_requests (user_id, full_name, email, phone, topic, message, status, staff_note, handled_by,
                              handled_at, created_at, updated_at)
select case when x.r_guest < 0.2 then null else c.user_id end, c.fullname, c.email, c.phone, t.topic, t.message,
       case when x.created_at < current_timestamp - interval '7 days' then 'RESOLVED'
            when x.created_at < current_timestamp - interval '2 days' then 'IN_PROGRESS' else 'NEW' end,
       case when x.created_at < current_timestamp - interval '7 days' then t.note
            when x.created_at < current_timestamp - interval '2 days' then 'Đang kiểm tra, sẽ phản hồi khách sớm.' end,
       case when x.created_at < current_timestamp - interval '2 days' then st.user_id end,
       case when x.created_at < current_timestamp - interval '2 days' then x.created_at + interval '5 hours' end,
       x.created_at,
       case when x.created_at < current_timestamp - interval '2 days' then x.created_at + interval '5 hours' else x.created_at end
from sa_contact x
cross join sa_ids a
join sa_customer c on c.user_id = a.ids[1 + floor(x.r_customer * array_length(a.ids, 1))::int]
join lateral (select * from sa_contact_text order by md5(topic || message || x.n) limit 1) t on true
join lateral (select s.user_id from sa_staff s
              order by md5(s.user_id::text || x.n) limit 1) st on true
order by x.created_at;

-- ---------- bảo hành / bảo trì: từ phiếu bảo hành của khách mẫu; trạng thái theo tuổi yêu cầu

create temp table sa_issue (slug varchar(40), issue text, fix text) on commit drop;
insert into sa_issue values
    ('dien-thoai', 'Máy tự tắt nguồn khi pin còn khoảng 20%.', 'Đã thay pin chính hãng, kiểm tra 24 giờ ổn định.'),
    ('dien-thoai', 'Loa ngoài bị rè khi mở âm lượng lớn.', 'Đã thay cụm loa ngoài, khách nhận lại máy.'),
    ('dien-thoai', 'Cảm ứng thỉnh thoảng không nhận ở góc màn hình.', 'Đã thay màn hình theo bảo hành.'),
    ('laptop', 'Bàn phím bị liệt vài phím hàng trên.', 'Đã thay bàn phím mới, khách kiểm tra và nhận máy.'),
    ('laptop', 'Quạt kêu to, máy nóng khi chỉ lướt web.', 'Đã vệ sinh, thay keo tản nhiệt và quạt.'),
    ('laptop', 'Màn hình xuất hiện một sọc dọc.', 'Đã thay tấm nền màn hình theo bảo hành.'),
    ('may-tinh-bang', 'Máy sạc không vào pin, cắm sạc báo chập chờn.', 'Đã thay cổng sạc, sạc ổn định.'),
    ('dong-ho-thong-minh', 'Đồng hồ không đo được nhịp tim.', 'Đã đổi máy mới cùng mẫu cho khách.'),
    ('dong-ho-thoi-trang', 'Đồng hồ chạy chậm khoảng 5 phút mỗi ngày.', 'Đã chỉnh máy và thay pin.'),
    ('phu-kien', 'Tai nghe bên trái không có tiếng.', 'Đã đổi sản phẩm mới cho khách.'),
    ('phu-kien', 'Củ sạc nóng bất thường và sạc chậm.', 'Đã đổi củ sạc mới cùng loại.');

-- ngày gửi lệch về gần đây (như ngoài đời: yêu cầu cũ đã xong, vài yêu cầu đang làm / mới gửi)
create temp table sa_wreq on commit drop as
select w.warranty_id, i.user_id, i.store_id, i.slug,
       current_timestamp - (0.3 + power(random(), 1.25)
                                  * least(75, current_date - w.warranty_start_date - 8)) * interval '1 day' as created_at,
       random() as r_status, random() as r_text
from warranties w
join sa_item i on i.order_item_id = w.order_item_id
where w.warranty_start_date < current_date - 9
-- chọn theo md5, không theo random(): order by random() dùng lại số ngẫu nhiên của cột ngày ở trên
order by md5('wreq-' || w.warranty_id)
limit 48;

delete from sa_wreq where created_at > current_timestamp - interval '2 hours';

alter table sa_wreq add column status varchar(20);
update sa_wreq
set status = case
    when created_at > current_timestamp - interval '2 days' then 'PENDING'
    when created_at > current_timestamp - interval '5 days' then 'RECEIVED'
    when created_at > current_timestamp - interval '12 days' then 'PROCESSING'
    when r_status < 0.75 then 'COMPLETED'
    when r_status < 0.87 then 'REJECTED'
    else 'CANCELLED' end;

insert into warranty_requests (warranty_id, user_id, issue_description, status, assigned_to_employee,
                               estimated_completion_date, completed_at, notes, created_at, updated_at, received_at,
                               rejection_reason, cancelled_at)
select r.warranty_id, r.user_id, t.issue, r.status,
       case when r.status not in ('PENDING', 'CANCELLED') then tech.user_id end,
       case when r.status in ('RECEIVED', 'PROCESSING', 'COMPLETED') then (r.created_at + interval '10 days')::date end,
       case when r.status = 'COMPLETED' then r.created_at + (4 + r.r_status * 6) * interval '1 day' end,
       case when r.status = 'COMPLETED' then t.fix end,
       r.created_at,
       case r.status when 'COMPLETED' then r.created_at + (4 + r.r_status * 6) * interval '1 day'
                     when 'CANCELLED' then r.created_at + interval '1 day'
                     when 'PENDING' then r.created_at
                     else r.created_at + interval '1 day' end,
       case when r.status not in ('PENDING', 'CANCELLED') then r.created_at + interval '1 day' end,
       case when r.status = 'REJECTED' then 'Máy có dấu hiệu rơi vỡ / vào nước, không thuộc diện bảo hành miễn phí.' end,
       case when r.status = 'CANCELLED' then r.created_at + interval '1 day' end
from sa_wreq r
join lateral (select * from sa_issue s where s.slug = r.slug order by md5(s.issue || r.warranty_id) limit 1) t on true
left join lateral (select s.user_id from sa_staff s where s.store_id = r.store_id
                   order by (s.position = 'Kỹ thuật viên') desc, s.user_id limit 1) tech on true
order by r.created_at;

create temp table sa_mreq on commit drop as
select i.order_item_id, i.user_id, i.store_id, i.slug,
       current_timestamp - (0.3 + power(random(), 1.25)
                                  * least(60, extract(day from current_timestamp - i.delivered_at) - 21)) * interval '1 day'
           as created_at,
       random() as r_status, random() as r_type
from sa_item i
where i.status = 'DELIVERED' and not i.returned and i.slug in ('dien-thoai', 'laptop', 'may-tinh-bang')
  and i.delivered_at < current_timestamp - interval '25 days'
order by md5('mreq-' || i.order_item_id)
limit 24;

delete from sa_mreq where created_at > current_timestamp - interval '2 hours';

alter table sa_mreq add column status varchar(20), add column mtype varchar(20);
update sa_mreq
set status = case
        when created_at > current_timestamp - interval '2 days' then 'PENDING'
        when created_at > current_timestamp - interval '5 days' then 'RECEIVED'
        when created_at > current_timestamp - interval '9 days' then 'PROCESSING'
        when r_status < 0.85 then 'COMPLETED'
        else 'CANCELLED' end,
    mtype = case when r_type < 0.40 then 'CLEANING' when r_type < 0.70 then 'SOFTWARE'
                 when r_type < 0.90 then 'REPAIR' else 'OTHER' end;

insert into maintenance_requests (user_id, order_item_id, maintenance_type, description, status, assigned_to_employee,
                                  completion_date, notes, created_at, updated_at, estimated_completion_date,
                                  estimated_cost, actual_cost, received_at, cancelled_at)
select m.user_id, m.order_item_id, m.mtype,
       case m.mtype when 'CLEANING' then 'Vệ sinh máy, máy bám bụi và nóng hơn trước.'
                    when 'SOFTWARE' then 'Cài lại hệ điều hành, máy chạy chậm và hay treo.'
                    when 'REPAIR' then 'Thay màn hình bị nứt do rơi (ngoài bảo hành).'
                    else 'Kiểm tra tổng quát trước khi đi công tác.' end,
       m.status,
       case when m.status not in ('PENDING', 'CANCELLED') then tech.user_id end,
       case when m.status = 'COMPLETED' then (m.created_at + interval '3 days')::date end,
       case when m.status = 'COMPLETED' then 'Đã hoàn tất, khách nhận lại máy và thanh toán tại quầy.' end,
       m.created_at,
       case m.status when 'COMPLETED' then m.created_at + interval '3 days'
                     when 'CANCELLED' then m.created_at + interval '1 day'
                     when 'PENDING' then m.created_at
                     else m.created_at + interval '1 day' end,
       case when m.status in ('RECEIVED', 'PROCESSING', 'COMPLETED') then (m.created_at + interval '4 days')::date end,
       case m.mtype when 'CLEANING' then 150000 when 'SOFTWARE' then 200000 when 'REPAIR' then 2500000 else 100000 end,
       case when m.status = 'COMPLETED' then
            case m.mtype when 'CLEANING' then 150000 when 'SOFTWARE' then 200000 when 'REPAIR' then 2350000 else 100000 end end,
       case when m.status not in ('PENDING', 'CANCELLED') then m.created_at + interval '1 day' end,
       case when m.status = 'CANCELLED' then m.created_at + interval '1 day' end
from sa_mreq m
left join lateral (select s.user_id from sa_staff s where s.store_id = m.store_id
                   order by (s.position = 'Kỹ thuật viên') desc, s.user_id limit 1) tech on true
order by m.created_at;

select (select count(*) from warranties) as warranties,
       (select count(*) from sa_pick) as voucher_orders,
       (select coalesce(sum(discount), 0) from sa_pick) as voucher_discount,
       (select count(*) from reviews) as reviews,
       (select count(*) from review_likes) as review_likes,
       (select count(*) from wishlist_items) as wishlist_items,
       (select count(*) from contact_requests) as contacts,
       (select count(*) from warranty_requests) as warranty_requests,
       (select count(*) from maintenance_requests) as maintenance_requests;

commit;
