-- db tạo mới từ database/techshopping.sql đã có sẵn mọi thay đổi này; file này chỉ dành cho db tạo từ bản schema cũ
-- (volume postgres_data đã có từ trước). chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/payments.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/payments.sql

-- installment_orders: hồ sơ trả góp (CCCD, ngân hàng thẻ) + kết quả duyệt.
alter table installment_orders add column if not exists citizen_id varchar(20);
alter table installment_orders add column if not exists card_bank_code varchar(20);
alter table installment_orders add column if not exists rejection_reason text;
alter table installment_orders add column if not exists reviewed_by bigint references users(user_id);
alter table installment_orders add column if not exists reviewed_at timestamp;
alter table installment_orders add column if not exists updated_at timestamp default current_timestamp;
alter table installment_orders alter column citizen_id set not null;
alter table installment_orders alter column card_bank_code set not null;
alter table installment_orders alter column status set default 'PENDING_APPROVAL';

alter table installment_payments alter column status set default 'PENDING';

-- payments: người xác nhận nhận tiền / hoàn tiền; khoản thu của 1 kỳ trả góp trỏ tới kỳ đó.
alter table payments add column if not exists confirmed_by bigint references users(user_id);
alter table payments add column if not exists refunded_at timestamp;
alter table payments add column if not exists refunded_by bigint references users(user_id);
alter table payments add column if not exists installment_payment_id bigint unique
    references installment_payments(installment_payment_id);
alter table payments alter column status set default 'PENDING';

-- mỗi đơn có tối đa 1 khoản thanh toán chính (COD / chuyển khoản); khoản thu theo kỳ trả góp thì không giới hạn
create unique index if not exists uq_payments_order_main on payments(order_id) where installment_payment_id is null;
create index if not exists idx_installment_orders_status on installment_orders(status);
create index if not exists idx_installment_payments_due on installment_payments(status, due_date);

-- đơn COD / chuyển khoản đặt trước phase 5 chưa có bản ghi thanh toán: tạo theo trạng thái đơn
insert into payments (order_id, amount, payment_method, status, paid_at)
select o.order_id,
       o.total_amount,
       o.payment_method,
       case
           when o.status = 'CANCELLED' then 'CANCELLED'
           when o.status = 'DELIVERED' and o.payment_method = 'COD' then 'PAID'
           else 'PENDING'
       end,
       case when o.status = 'DELIVERED' and o.payment_method = 'COD' then o.delivered_at end
from orders o
where o.payment_method in ('COD', 'BANK_TRANSFER')
  and o.total_amount > 0
  and not exists (select 1 from payments p where p.order_id = o.order_id and p.installment_payment_id is null);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_payments_status') then
        alter table payments add constraint chk_payments_status
            check (status in ('PENDING', 'PAID', 'REFUND_PENDING', 'REFUNDED', 'CANCELLED'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_payments_method') then
        alter table payments add constraint chk_payments_method
            check (payment_method in ('COD', 'BANK_TRANSFER', 'INSTALLMENT'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_payments_amount_positive') then
        alter table payments add constraint chk_payments_amount_positive check (amount > 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_installment_orders_status') then
        alter table installment_orders add constraint chk_installment_orders_status
            check (status in ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ACTIVE', 'COMPLETED', 'CANCELLED'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_installment_orders_months_positive') then
        alter table installment_orders add constraint chk_installment_orders_months_positive check (num_months > 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_installment_orders_monthly_positive') then
        alter table installment_orders add constraint chk_installment_orders_monthly_positive
            check (monthly_payment > 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_installment_orders_citizen_id_digits') then
        alter table installment_orders add constraint chk_installment_orders_citizen_id_digits
            check (citizen_id ~ '^[0-9]+$');
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_installment_orders_rejection_reason') then
        alter table installment_orders add constraint chk_installment_orders_rejection_reason
            check (status <> 'REJECTED' or rejection_reason is not null);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_installment_payments_status') then
        alter table installment_payments add constraint chk_installment_payments_status
            check (status in ('PENDING', 'PAID'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_installment_payments_number_positive') then
        alter table installment_payments add constraint chk_installment_payments_number_positive
            check (payment_number > 0);
    end if;
end $$;
