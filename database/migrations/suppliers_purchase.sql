-- Nhà cung cấp, phiếu nhập kho nhiều dòng có giá nhập, giá vốn bình quân.
-- - suppliers: nhà cung cấp do ADMIN quản lý.
-- - purchase_receipts / purchase_receipt_items: phiếu nhập của một chi nhánh (DRAFT -> COMPLETED hoặc CANCELLED);
--   chỉ khi COMPLETED mới cộng tồn kho và ghi sổ kho; mỗi phiên bản xuất hiện một dòng trong phiếu.
-- - product_variants.average_cost: giá vốn bình quân gia quyền toàn hệ thống, cập nhật khi hoàn tất phiếu nhập.
-- - order_items.unit_cost: giá vốn của phiên bản tại lúc xác nhận đơn (null = chưa biết giá vốn).
-- - stock_movements.receipt_id: phiếu nhập sinh ra dòng sổ kho.
-- Chạy lại được.

create table if not exists suppliers (
    supplier_id serial primary key,
    name varchar(150) not null,
    phone varchar(20),
    email varchar(120),
    address text,
    tax_code varchar(20),
    note text,
    is_active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create unique index if not exists uq_suppliers_name on suppliers (lower(name));

create table if not exists purchase_receipts (
    receipt_id bigserial primary key,
    store_id int not null references stores(store_id),
    supplier_id int not null references suppliers(supplier_id),
    status varchar(12) not null default 'DRAFT' check (status in ('DRAFT', 'COMPLETED', 'CANCELLED')),
    note text,
    created_by bigint references users(user_id),
    created_at timestamp not null default current_timestamp,
    completed_by bigint references users(user_id),
    completed_at timestamp,
    cancelled_at timestamp
);

create index if not exists idx_purchase_receipts_store on purchase_receipts (store_id, created_at desc);
create index if not exists idx_purchase_receipts_supplier on purchase_receipts (supplier_id);

create table if not exists purchase_receipt_items (
    item_id bigserial primary key,
    receipt_id bigint not null references purchase_receipts(receipt_id) on delete cascade,
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null check (quantity > 0),
    unit_cost decimal(15, 2) not null check (unit_cost >= 0),
    unique (receipt_id, variant_id)
);

alter table product_variants add column if not exists average_cost decimal(15, 2);
alter table order_items add column if not exists unit_cost decimal(15, 2);
alter table stock_movements add column if not exists receipt_id bigint references purchase_receipts(receipt_id);

create index if not exists idx_stock_movements_receipt on stock_movements (receipt_id) where receipt_id is not null;
