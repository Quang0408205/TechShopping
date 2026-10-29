-- Kế hoạch v4 bước 5 (2026-10-17): gộp các sản phẩm thật ra là phiên bản của nhau, bỏ phiên bản giá 0.
--   1. Phiên bản giá 0 chưa từng bán: xoá (kèm tồn kho / sổ kho mẫu, dòng giỏ); sản phẩm không còn phiên bản → xoá.
--   2. Sản phẩm ĐANG BÁN cùng danh mục, tên giống nhau sau khi bỏ phần RAM/bộ nhớ ở cuối ("8GB/256GB", "256GB")
--      → một sản phẩm. Sản phẩm chính = phiên bản rẻ nhất (hoà: id nhỏ). Tên sản phẩm chính bỏ phần RAM/bộ nhớ.
--      Phiên bản, đánh giá, khuyến mãi, lịch sử xem / gợi ý / quan hệ AI chuyển sang sản phẩm chính; ảnh gộp (bỏ trùng
--      link), thông số giữ của sản phẩm chính, tên thông số còn thiếu lấy từ sản phẩm phụ; sau đó xoá sản phẩm phụ.
--   3. Phiên bản giống hệt nhau trong một sản phẩm (cùng tên, giá, giá KM, màu, bộ nhớ, RAM): giữ phiên bản id nhỏ,
--      cộng dồn tồn kho, chuyển sổ kho / dòng đơn / dòng giỏ sang nó.
--   4. Tính lại điểm / số đánh giá của sản phẩm chính.
-- Không gộp sản phẩm đang ẩn. Chạy lại nhiều lần vẫn an toàn (lần sau không còn gì để gộp). SAO LƯU DB TRƯỚC.
-- Docker cũng chạy file này sau 03/04 khi tạo volume mới (docker-compose.yml, 05_merge_product_variants.sql).
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/merge_product_variants.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/merge_product_variants.sql
-- Sau khi chạy trên DB dev: dọn Redis cache:* và chạy lại dữ liệu AI (build_relations, build_index, huấn luyện).

begin;

-- ---------- 1. phiên bản giá 0 ----------
create temp table v4_zero_variants on commit drop as
select v.variant_id, v.product_id
from product_variants v
where v.price <= 0
  and not exists (select 1 from order_items oi where oi.variant_id = v.variant_id);

delete from cart_items where variant_id in (select variant_id from v4_zero_variants);
delete from inventory where variant_id in (select variant_id from v4_zero_variants);
delete from stock_movements where variant_id in (select variant_id from v4_zero_variants);
delete from product_variants where variant_id in (select variant_id from v4_zero_variants);

create temp table v4_empty_products on commit drop as
select distinct z.product_id
from v4_zero_variants z
where not exists (select 1 from product_variants v where v.product_id = z.product_id);

delete from reviews where product_id in (select product_id from v4_empty_products);
delete from user_interactions where product_id in (select product_id from v4_empty_products);
delete from recommendations where product_id in (select product_id from v4_empty_products);
delete from product_relations
where product_id in (select product_id from v4_empty_products)
   or related_product_id in (select product_id from v4_empty_products);
delete from products where product_id in (select product_id from v4_empty_products);


-- ---------- 2. gộp sản phẩm ----------
create temp table v4_merge on commit drop as
with candidates as (
    select p.product_id, p.category_id,
           trim(regexp_replace(regexp_replace(p.name, '\s*\d+\s*GB\s*/\s*\d+\s*(GB|TB)\s*$', '', 'i'),
                '\s+\d+\s*(GB|TB)\s*$', '', 'i')) as base,
           (select min(coalesce(v.discount_price, v.price)) from product_variants v where v.product_id = p.product_id) as price
    from products p
    where p.deleted_at is null
      and coalesce(p.is_active, true)
      and exists (select 1 from product_variants v where v.product_id = p.product_id)
), grouped as (
    select c.*,
           count(*) over (partition by c.category_id, lower(c.base)) as size,
           first_value(c.product_id) over (partition by c.category_id, lower(c.base) order by c.price, c.product_id) as main_id
    from candidates c
)
select product_id, main_id, base from grouped where size > 1;

do $$
declare
    r record;
    merged int := 0;
begin
    for r in select product_id, main_id from v4_merge where product_id <> main_id order by main_id, product_id loop

        insert into product_images (product_id, image_url, alt_text, display_order, is_primary)
        select r.main_id, i.image_url, i.alt_text,
               coalesce((select max(m.display_order) from product_images m where m.product_id = r.main_id), 0)
                   + row_number() over (order by i.display_order, i.image_id),
               false
        from product_images i
        where i.product_id = r.product_id
          and not exists (select 1 from product_images m where m.product_id = r.main_id and m.image_url = i.image_url);

        insert into product_specifications (product_id, spec_name, spec_value, spec_order)
        select r.main_id, s.spec_name, s.spec_value,
               coalesce((select max(m.spec_order) from product_specifications m where m.product_id = r.main_id), 0)
                   + row_number() over (order by s.spec_order nulls last, s.specification_id)
        from product_specifications s
        where s.product_id = r.product_id
          and not exists (select 1 from product_specifications m
                          where m.product_id = r.main_id and lower(m.spec_name) = lower(s.spec_name));

        update product_variants set product_id = r.main_id, updated_at = current_timestamp where product_id = r.product_id;
        update reviews set product_id = r.main_id where product_id = r.product_id;

        insert into promotion_products (promotion_id, product_id, discount_type, discount_value)
        select promotion_id, r.main_id, discount_type, discount_value from promotion_products where product_id = r.product_id
        on conflict do nothing;

        update user_interactions set product_id = r.main_id where product_id = r.product_id;
        update recommendations set product_id = r.main_id where product_id = r.product_id;

        -- quan hệ AI: bỏ cặp chính ↔ phụ, chuyển phần còn lại nếu chưa có cặp giống vậy
        delete from product_relations
        where (product_id = r.product_id and related_product_id = r.main_id)
           or (product_id = r.main_id and related_product_id = r.product_id);
        update product_relations x set product_id = r.main_id
        where x.product_id = r.product_id
          and not exists (select 1 from product_relations y where y.product_id = r.main_id
                          and y.related_product_id = x.related_product_id and y.relation_type = x.relation_type);
        update product_relations x set related_product_id = r.main_id
        where x.related_product_id = r.product_id
          and not exists (select 1 from product_relations y where y.related_product_id = r.main_id
                          and y.product_id = x.product_id and y.relation_type = x.relation_type);
        delete from product_relations where product_id = r.product_id or related_product_id = r.product_id;

        delete from products where product_id = r.product_id;
        merged := merged + 1;
    end loop;
    raise notice 'merged % product(s) into % main product(s)', merged,
        (select count(distinct main_id) from v4_merge);
end $$;

update products p
set name = m.base, updated_at = current_timestamp
from v4_merge m
where m.product_id = m.main_id and p.product_id = m.main_id and p.name <> m.base;


-- ---------- 3. phiên bản giống hệt nhau ----------
create temp table v4_dup_variants on commit drop as
with ranked as (
    select v.variant_id,
           first_value(v.variant_id) over (
               partition by v.product_id, lower(v.variant_name), v.price, v.discount_price, v.color, v.storage, v.ram
               order by v.variant_id) as keep_id
    from product_variants v
)
select variant_id, keep_id from ranked where variant_id <> keep_id;

insert into inventory (store_id, variant_id, quantity, updated_at)
select i.store_id, d.keep_id, sum(i.quantity), current_timestamp
from inventory i join v4_dup_variants d on d.variant_id = i.variant_id
group by i.store_id, d.keep_id
on conflict (store_id, variant_id) do update
    set quantity = inventory.quantity + excluded.quantity, updated_at = current_timestamp;
delete from inventory where variant_id in (select variant_id from v4_dup_variants);

update stock_movements m set variant_id = d.keep_id from v4_dup_variants d where m.variant_id = d.variant_id;
update order_items o set variant_id = d.keep_id from v4_dup_variants d where o.variant_id = d.variant_id;
update cart_items c set variant_id = d.keep_id
from v4_dup_variants d
where c.variant_id = d.variant_id
  and not exists (select 1 from cart_items k where k.cart_id = c.cart_id and k.variant_id = d.keep_id);
delete from cart_items where variant_id in (select variant_id from v4_dup_variants);
delete from product_variants where variant_id in (select variant_id from v4_dup_variants);


-- ---------- 4. điểm đánh giá của sản phẩm chính ----------
update products p
set total_reviews = s.total, rating = s.average
from (
    select m.main_id,
           count(r.review_id) as total,
           coalesce(round(avg(r.rating)::numeric, 2), 0) as average
    from (select distinct main_id from v4_merge) m
    left join reviews r on r.product_id = m.main_id and not r.is_hidden
    group by m.main_id
) s
where p.product_id = s.main_id;

select (select count(*) from v4_zero_variants) as zero_price_variants_removed,
       (select count(*) from v4_empty_products) as products_removed_with_them,
       (select count(*) from v4_merge where product_id <> main_id) as products_merged,
       (select count(distinct main_id) from v4_merge) as main_products,
       (select count(*) from v4_dup_variants) as duplicate_variants_removed;

commit;
