-- điểm đánh giá Thế Giới Di Động tách riêng (2026-10-07, bước R2 của "Đánh giá sản phẩm").
-- Catalogue crawl từ TGDĐ (Raw_data/import_catalog.py) từng ghi điểm + số lượt đánh giá của TGDĐ vào
-- products.rating / total_reviews. Từ R2, hai cột đó chỉ còn là số liệu của đánh giá THẬT trên web (bảng reviews,
-- chỉ tính đánh giá đang hiện; Java tính lại mỗi lần ghi), còn số của TGDĐ chuyển sang 2 cột mới dưới đây và
-- chỉ để hiển thị riêng ("Trên Thế Giới Di Động: ★ 4.6 (1.234)"). Importer từ nay ghi vào 2 cột mới.
-- db tạo mới từ database/techshopping.sql đã có sẵn 2 cột này; file này chỉ dành cho db tạo từ bản schema cũ
-- (volume postgres_data đã có từ trước). Chạy lại nhiều lần vẫn an toàn: chỉ chép số TGDĐ một lần (khi cột mới
-- còn trống và sản phẩm chưa có đánh giá thật nào), rồi tính lại rating / total_reviews từ bảng reviews.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/product_tgdd_rating.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/product_tgdd_rating.sql

alter table products add column if not exists tgdd_rating decimal(3, 2);
alter table products add column if not exists tgdd_review_count int not null default 0;

update products p
set tgdd_rating = p.rating,
    tgdd_review_count = p.total_reviews
where p.tgdd_rating is null
  and coalesce(p.total_reviews, 0) > 0
  and not exists (select 1 from reviews r where r.product_id = p.product_id);

update products p
set rating = coalesce((select round(avg(r.rating), 2) from reviews r
                       where r.product_id = p.product_id and not r.is_hidden), 0),
    total_reviews = (select count(*) from reviews r where r.product_id = p.product_id and not r.is_hidden)
where coalesce(p.rating, 0) <> coalesce((select round(avg(r.rating), 2) from reviews r
                                         where r.product_id = p.product_id and not r.is_hidden), 0)
   or coalesce(p.total_reviews, 0) <> (select count(*) from reviews r
                                       where r.product_id = p.product_id and not r.is_hidden);
