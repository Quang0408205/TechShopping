-- Thông số kỹ thuật riêng của từng phiên bản.
-- product_specifications.variant_id: NULL = thông số chung của sản phẩm; có giá trị = thông số riêng của phiên bản đó
-- (ghi đè dòng chung cùng tên khi khách chọn phiên bản). Xoá phiên bản thì xoá luôn các dòng riêng của nó.
-- Chạy lại được.

alter table product_specifications
    add column if not exists variant_id bigint references product_variants(variant_id) on delete cascade;

create index if not exists idx_product_specifications_variant on product_specifications (variant_id)
    where variant_id is not null;
