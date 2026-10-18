-- Kế hoạch v5 GĐ8: ảnh sản phẩm theo màu.
-- product_images.color = tên màu (trùng product_variants.color) của ảnh; NULL = ảnh chung cho mọi màu.
-- Chạy lại được.

alter table product_images add column if not exists color varchar(50);

create index if not exists idx_product_images_product_color on product_images (product_id, color);
