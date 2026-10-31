-- Chuyển kho giữa các chi nhánh.
-- - stock_transfers / stock_transfer_items: phiếu chuyển nhiều dòng, DRAFT -> IN_TRANSIT (trừ tồn chi nhánh gửi)
--   -> RECEIVED (cộng tồn chi nhánh nhận); phiếu có thể bị huỷ khi nháp hoặc đang chuyển (đang chuyển thì hoàn lại
--   chi nhánh gửi). Mỗi phiên bản xuất hiện một dòng trong phiếu.
-- - stock_movements: thêm loại TRANSFER_OUT / TRANSFER_IN (không tính là "hàng nhập") và cột transfer_id.
-- Chạy lại được.

create table if not exists stock_transfers (
    transfer_id bigserial primary key,
    from_store_id int not null references stores(store_id),
    to_store_id int not null references stores(store_id),
    status varchar(12) not null default 'DRAFT' check (status in ('DRAFT', 'IN_TRANSIT', 'RECEIVED', 'CANCELLED')),
    note text,
    created_by bigint references users(user_id),
    created_at timestamp not null default current_timestamp,
    sent_by bigint references users(user_id),
    sent_at timestamp,
    received_by bigint references users(user_id),
    received_at timestamp,
    cancelled_by bigint references users(user_id),
    cancelled_at timestamp,
    check (from_store_id <> to_store_id)
);

create index if not exists idx_stock_transfers_from on stock_transfers (from_store_id, created_at desc);
create index if not exists idx_stock_transfers_to on stock_transfers (to_store_id, created_at desc);

create table if not exists stock_transfer_items (
    item_id bigserial primary key,
    transfer_id bigint not null references stock_transfers(transfer_id) on delete cascade,
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null check (quantity > 0),
    unique (transfer_id, variant_id)
);

alter table stock_movements add column if not exists transfer_id bigint references stock_transfers(transfer_id);

create index if not exists idx_stock_movements_transfer on stock_movements (transfer_id) where transfer_id is not null;

alter table stock_movements drop constraint if exists chk_stock_movements_type;
alter table stock_movements add constraint chk_stock_movements_type
    check (movement_type in ('IN', 'OUT', 'ADJUSTMENT', 'RETURN', 'TRANSFER_OUT', 'TRANSFER_IN'));
