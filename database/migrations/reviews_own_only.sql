-- chỉ dùng đánh giá thật của khách trên web.
--   * bỏ điểm / số lượt đánh giá crawl từ Thế Giới Di Động (products.tgdd_rating, tgdd_review_count);
--   * mỗi tài khoản được đánh giá một sản phẩm nhiều lần (bỏ unique user_id + product_id); giãn cách 1 phút
--     giữa hai đánh giá do ứng dụng kiểm tra (app.review.min-interval-seconds).
-- Chạy lại nhiều lần vẫn an toàn. Sao lưu DB trước khi chạy.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/reviews_own_only.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/reviews_own_only.sql

alter table products drop column if exists tgdd_rating;
alter table products drop column if exists tgdd_review_count;

alter table reviews drop constraint if exists reviews_user_id_product_id_key;

-- "Đánh giá của tôi" và kiểm tra giãn cách tìm theo tài khoản + thời gian
create index if not exists idx_reviews_user_created on reviews (user_id, created_at desc);
