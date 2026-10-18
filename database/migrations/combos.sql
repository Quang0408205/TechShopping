-- Kế hoạch v5 GĐ10: combo "mua kèm giảm giá" (do ADMIN tạo): 1 sản phẩm chính + các món mua kèm, mỗi món một mức giảm
-- (% hoặc số tiền trên mỗi sản phẩm). Giỏ đủ món thì server tự giảm; không có mã nhập.
-- - combos / combo_items theo SẢN PHẨM (mọi phiên bản); thời hạn + bật / tạm dừng như voucher.
-- - orders.combo_discount + order_items.combo_discount: phần giảm combo chia theo dòng (trả hàng hoàn đúng số tiền thực trả).
-- - order_combos: combo nào đã áp cho đơn nào (tên chụp lại, số món, số tiền) để báo cáo theo combo.
-- Chạy lại được.

create table if not exists combos (
    combo_id bigserial primary key,
    name varchar(150) not null,
    description text,
    main_product_id bigint not null references products(product_id),
    starts_at timestamp not null,
    ends_at timestamp not null,
    is_active boolean not null default true,
    created_by bigint not null references users(user_id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists combo_items (
    combo_id bigint not null references combos(combo_id) on delete cascade,
    product_id bigint not null references products(product_id),
    discount_type varchar(10) not null,
    discount_value decimal(15, 2) not null,
    primary key (combo_id, product_id)
);

create table if not exists order_combos (
    order_combo_id bigserial primary key,
    order_id bigint not null references orders(order_id) on delete cascade,
    combo_id bigint not null references combos(combo_id),
    combo_name varchar(150) not null,
    units int not null,
    discount_amount decimal(15, 2) not null
);

alter table orders add column if not exists combo_discount decimal(15, 2) not null default 0;
alter table order_items add column if not exists combo_discount decimal(15, 2) not null default 0;

create index if not exists idx_combos_main on combos(main_product_id, is_active, starts_at, ends_at);
create index if not exists idx_combo_items_product on combo_items(product_id);
create index if not exists idx_order_combos_order on order_combos(order_id);
create index if not exists idx_order_combos_combo on order_combos(combo_id);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_combos_window') then
        alter table combos add constraint chk_combos_window check (ends_at > starts_at);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_combo_items_type') then
        alter table combo_items add constraint chk_combo_items_type check (discount_type in ('PERCENT', 'AMOUNT'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_combo_items_value') then
        alter table combo_items add constraint chk_combo_items_value
            check (discount_value > 0 and (discount_type <> 'PERCENT' or discount_value <= 100));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_orders_combo_discount') then
        alter table orders add constraint chk_orders_combo_discount check (combo_discount >= 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_order_items_combo_discount') then
        alter table order_items add constraint chk_order_items_combo_discount check (combo_discount >= 0);
    end if;
end $$;
