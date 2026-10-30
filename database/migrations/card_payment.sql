-- Hình thức "Thẻ quốc tế" (Stripe Checkout, chế độ Test).
-- - payments.payment_method thêm 'CARD'.
-- - payments.checkout_session_id: phiên Stripe Checkout mới nhất của khoản thanh toán (để hỏi lại Stripe khi khách quay về,
--   khớp webhook, và huỷ phiên khi đơn bị huỷ). Mã giao dịch Stripe (payment_intent) ghi vào transaction_id khi đã trả.
-- Chạy lại được.

alter table payments drop constraint if exists chk_payments_method;
alter table payments add constraint chk_payments_method
    check (payment_method in ('COD', 'BANK_TRANSFER', 'INSTALLMENT', 'CARD'));

alter table payments add column if not exists checkout_session_id varchar(100);

create unique index if not exists uq_payments_checkout_session on payments(checkout_session_id)
    where checkout_session_id is not null;
