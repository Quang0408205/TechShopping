-- đánh giá sản phẩm: sao (1-5) + bình luận, admin ẩn/hiện có lý do, tối đa 5 ảnh/đánh giá.
-- Giới hạn 5 ảnh / đánh giá và việc tính lại products.rating / total_reviews do code Java kiểm tra lúc ghi, không dùng
-- trigger Postgres.
-- db tạo mới từ database/techshopping.sql đã có sẵn 2 bảng này; file này chỉ dành cho db tạo từ bản schema cũ
-- (volume postgres_data đã có từ trước). Chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/reviews.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/reviews.sql

create table if not exists reviews (
    review_id bigserial primary key,
    user_id bigint not null references users(user_id),
    product_id bigint not null references products(product_id),
    rating smallint not null,
    comment text not null,
    is_hidden boolean not null default false,
    hidden_reason text,
    hidden_by bigint references users(user_id),
    hidden_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    unique (user_id, product_id)
);

create table if not exists review_images (
    image_id bigserial primary key,
    review_id bigint not null references reviews(review_id) on delete cascade,
    image_url text not null,
    display_order int not null,
    created_at timestamp default current_timestamp,
    unique (review_id, display_order)
);

create index if not exists idx_reviews_product_id on reviews(product_id, created_at desc);
create index if not exists idx_reviews_hidden_by on reviews(hidden_by);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_reviews_rating_range') then
        alter table reviews add constraint chk_reviews_rating_range check (rating between 1 and 5);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_reviews_comment_length') then
        alter table reviews add constraint chk_reviews_comment_length check (char_length(comment) between 10 and 2000);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_reviews_hidden_reason') then
        alter table reviews add constraint chk_reviews_hidden_reason check (not is_hidden or hidden_reason is not null);
    end if;
end $$;
