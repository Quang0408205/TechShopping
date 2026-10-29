-- Thanh toán chuyển khoản (2026-10-16): nhật ký biến động số dư do webhook ngân hàng gửi về, dùng để tự xác nhận
-- đơn chuyển khoản và để ADMIN đối soát các giao dịch chưa khớp. Chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/bank_transactions.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/bank_transactions.sql
-- external_id là mã giao dịch của ngân hàng / nhà cung cấp: duy nhất, nên webhook gửi lặp không tính hai lần.
-- status: MATCHED (đã tự xác nhận đơn), UNMATCHED (không tìm thấy đơn), REVIEW (tìm thấy đơn nhưng có vấn đề),
--         RESOLVED (ADMIN gán tay vào đơn), IGNORED (ADMIN bỏ qua).

create table if not exists bank_transactions (
    bank_transaction_id bigserial primary key,
    external_id varchar(100) not null,
    provider varchar(30) not null default 'WEBHOOK',
    amount decimal(15, 2) not null,
    content varchar(500),
    account_number varchar(30),
    transferred_at timestamp,
    received_at timestamp not null default current_timestamp,
    status varchar(20) not null,
    reason varchar(255),
    order_id bigint references orders(order_id),
    resolved_by bigint references users(user_id),
    resolved_at timestamp,
    note varchar(500),
    constraint uq_bank_transactions_external unique (external_id),
    constraint chk_bank_transactions_status check (status in ('MATCHED', 'UNMATCHED', 'REVIEW', 'RESOLVED', 'IGNORED')),
    constraint chk_bank_transactions_amount check (amount > 0)
);

create index if not exists idx_bank_transactions_status_received on bank_transactions(status, received_at);
create index if not exists idx_bank_transactions_order on bank_transactions(order_id);
