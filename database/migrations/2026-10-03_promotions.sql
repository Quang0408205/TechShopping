-- khuyến mãi sản phẩm (2026-10-03): chương trình khuyến mãi có thời hạn, chọn sản phẩm tham gia.
-- db tạo mới từ database/techshopping.sql đã có sẵn 2 bảng này; file này chỉ dành cho db tạo từ bản schema cũ
-- (volume postgres_data đã có từ trước). chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/2026-10-03_promotions.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/2026-10-03_promotions.sql

create table if not exists promotions (
    promotion_id bigserial primary key,
    name varchar(255) not null,
    description text,
    discount_type varchar(20) not null,
    discount_value decimal(15, 2) not null,
    max_discount_amount decimal(15, 2),
    start_date timestamp not null,
    end_date timestamp not null,
    is_active boolean not null default true,
    created_by bigint not null references users(user_id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists promotion_products (
    promotion_id bigint not null references promotions(promotion_id) on delete cascade,
    product_id bigint not null references products(product_id) on delete cascade,
    discount_type varchar(20),
    discount_value decimal(15, 2),
    created_at timestamp not null default current_timestamp,
    primary key (promotion_id, product_id)
);

create index if not exists idx_promotions_active_window on promotions(is_active, start_date, end_date);
create index if not exists idx_promotion_products_product on promotion_products(product_id);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_promotions_date_range') then
        alter table promotions add constraint chk_promotions_date_range check (end_date > start_date);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_promotions_discount_value_positive') then
        alter table promotions add constraint chk_promotions_discount_value_positive check (discount_value > 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_promotions_discount_type') then
        alter table promotions add constraint chk_promotions_discount_type
            check (discount_type in ('PERCENTAGE', 'FIXED_AMOUNT'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_promotions_percentage_range') then
        alter table promotions add constraint chk_promotions_percentage_range
            check (discount_type <> 'PERCENTAGE' or discount_value <= 100);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_promotion_products_discount_type') then
        alter table promotion_products add constraint chk_promotion_products_discount_type
            check (discount_type is null or discount_type in ('PERCENTAGE', 'FIXED_AMOUNT'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_promotion_products_discount_value_positive') then
        alter table promotion_products add constraint chk_promotion_products_discount_value_positive
            check (discount_value is null or discount_value > 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_promotion_products_percentage_range') then
        alter table promotion_products add constraint chk_promotion_products_percentage_range
            check (discount_type <> 'PERCENTAGE' or discount_value <= 100);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_promotion_products_pair') then
        alter table promotion_products add constraint chk_promotion_products_pair
            check ((discount_type is null) = (discount_value is null));
    end if;
end $$;
