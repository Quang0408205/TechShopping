-- phase 7 (2026-10-03): chi nhánh/nhân viên có API thật, tồn kho theo chi nhánh, gán chi nhánh xử lý cho đơn.
-- db tạo mới từ database/techshopping.sql đã có sẵn mọi thay đổi này; file này chỉ dành cho db tạo từ bản schema cũ
-- (volume postgres_data đã có từ trước). chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/stores_inventory.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/stores_inventory.sql

-- orders: chi nhánh xử lý đơn (null cho đơn đặt trước phase 7) + hình thức nhận hàng.
alter table orders add column if not exists store_id int;
alter table orders add column if not exists delivery_type varchar(20) not null default 'HOME_DELIVERY';

-- tồn kho theo chi nhánh: số lượng mỗi phiên bản sản phẩm tại mỗi chi nhánh.
create table if not exists inventory (
    store_id int not null references stores(store_id),
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null default 0,
    updated_at timestamp default current_timestamp,
    primary key (store_id, variant_id)
);

-- sổ kho: mỗi lần tồn thay đổi ghi 1 dòng (nhập hàng / trừ khi xác nhận đơn / hoàn khi hủy đơn đã xác nhận).
create table if not exists stock_movements (
    movement_id bigserial primary key,
    store_id int not null references stores(store_id),
    variant_id bigint not null references product_variants(variant_id),
    movement_type varchar(20) not null,
    quantity_change int not null,
    supplier_name varchar(150),
    note text,
    order_id bigint references orders(order_id),
    created_by bigint references users(user_id),
    created_at timestamp default current_timestamp
);

create index if not exists idx_inventory_store on inventory(store_id);
create index if not exists idx_stock_movements_store_date on stock_movements(store_id, created_at);
create index if not exists idx_orders_store on orders(store_id);

-- mỗi nhân viên chỉ có 1 phân công chi nhánh đang hiệu lực tại 1 thời điểm.
create unique index if not exists uq_employee_assignments_active
    on employee_assignments (employee_id) where is_active and end_date is null;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'fk_orders_store') then
        alter table orders add constraint fk_orders_store foreign key (store_id) references stores(store_id);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_orders_delivery_type') then
        alter table orders add constraint chk_orders_delivery_type
            check (delivery_type in ('HOME_DELIVERY', 'PICKUP'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_inventory_quantity_non_negative') then
        alter table inventory add constraint chk_inventory_quantity_non_negative check (quantity >= 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_stock_movements_type') then
        alter table stock_movements add constraint chk_stock_movements_type
            check (movement_type in ('IN', 'OUT', 'ADJUSTMENT', 'RETURN'));
    end if;
end $$;
