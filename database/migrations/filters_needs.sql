-- Bộ lọc thông số nâng cao + "Nhu cầu".
-- product_filter_values: giá trị lọc đã chuẩn hoá của sản phẩm (nhóm + nhãn, vd chip = "Snapdragon 8 series");
--   được dựng lại từ thông số / phiên bản bởi backend (ProductFilterService.reindex), cùng nhóm = HOẶC, khác nhóm = VÀ.
-- needs / product_needs: nhu cầu của từng danh mục (vd Laptop: Gaming, Đồ hoạ...), 1 sản phẩm nhiều nhu cầu.
-- Chạy lại được. Sau khi chạy: POST /api/v1/admin/products/reindex-filters (ADMIN) để dựng giá trị lọc cho dữ liệu cũ.

create table if not exists product_filter_values (
    filter_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    group_key varchar(30) not null,
    label varchar(60) not null,
    sort_order int not null default 0,
    constraint uq_product_filter_values unique (product_id, group_key, label)
);

create index if not exists idx_product_filter_values_lookup on product_filter_values (group_key, label, product_id);

create table if not exists needs (
    need_id serial primary key,
    category_id int not null references categories(category_id) on delete cascade,
    slug varchar(60) not null,
    name varchar(100) not null,
    display_order int not null default 0,
    constraint uq_needs_category_slug unique (category_id, slug)
);

create table if not exists product_needs (
    product_need_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    need_id int not null references needs(need_id) on delete cascade,
    constraint uq_product_needs unique (product_id, need_id)
);

create index if not exists idx_product_needs_need on product_needs (need_id, product_id);
