-- thuộc tính theo danh mục (2026-10-05, khảo sát từ docs/techshopping_v3.sql nhóm 2): danh mục nào dùng
-- những thuộc tính nào, bắt buộc / lọc được, thứ tự hiển thị. Dùng để sinh form nhập sản phẩm và bộ lọc
-- theo từng danh mục; bước này chỉ thêm bảng (schema + entity + repository), chưa có API/giao diện dùng tới.
-- db tạo mới từ database/techshopping.sql đã có sẵn bảng này; file này chỉ dành cho db tạo từ bản schema cũ
-- (volume postgres_data đã có từ trước). Chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/category_attributes.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/category_attributes.sql

create table if not exists category_attributes (
    category_id int not null references categories(category_id) on delete cascade,
    attribute_id int not null references attributes(attribute_id) on delete cascade,
    is_required boolean not null default true,
    is_filterable boolean not null default true,
    display_order int,
    primary key (category_id, attribute_id)
);

create index if not exists idx_category_attributes_attribute_id on category_attributes(attribute_id);
