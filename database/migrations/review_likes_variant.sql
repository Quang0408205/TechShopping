-- giao diện đánh giá kiểu sàn thương mại.
--   * reviews.variant_label: "Phân loại hàng" của người viết (phiên bản trong đơn ĐÃ GIAO gần nhất có sản phẩm này),
--     ghi lại lúc viết đánh giá; đánh giá cũ được điền bù ở cuối file (không có đơn đã giao thì để trống);
--   * review_likes: nút "Hữu ích" — mỗi tài khoản một lượt cho mỗi đánh giá.
-- Chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/review_likes_variant.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/review_likes_variant.sql

alter table reviews add column if not exists variant_label varchar(255);

create table if not exists review_likes (
    review_id bigint not null references reviews(review_id) on delete cascade,
    user_id bigint not null references users(user_id),
    created_at timestamp default current_timestamp,
    primary key (review_id, user_id)
);

create index if not exists idx_review_likes_user on review_likes (user_id);

-- Điền bù "Phân loại hàng" cho đánh giá cũ: phiên bản trong đơn đã giao gần nhất của chính người viết
update reviews r
set variant_label = picked.label
from (
    select distinct on (rv.review_id) rv.review_id,
           coalesce(
               nullif(concat_ws(', ', nullif(trim(v.storage), ''), nullif(trim(v.ram), ''), nullif(trim(v.color), '')), ''),
               v.variant_name) as label
    from reviews rv
    join orders o on o.user_id = rv.user_id and o.status = 'DELIVERED'
    join order_items oi on oi.order_id = o.order_id
    join product_variants v on v.variant_id = oi.variant_id and v.product_id = rv.product_id
    where rv.variant_label is null
    order by rv.review_id, o.delivered_at desc nulls last, o.order_id desc
) picked
where r.review_id = picked.review_id;
