-- Email đơn hàng.
-- - orders.contact_email: địa chỉ nhận mail của đơn (khách nhập ở popup thông tin nhận hàng; đơn cũ để trống)
-- - orders.tracking_token: mã ngẫu nhiên trong link theo dõi đơn công khai (đơn cũ được cấp mã khi chạy file này)
-- - email_log: mỗi mail đã gửi / thất bại (đơn, loại, người nhận, kết quả)
-- Chạy lại được.

alter table orders add column if not exists contact_email varchar(255);
alter table orders add column if not exists tracking_token varchar(64);

update orders
set tracking_token = md5(random()::text || clock_timestamp()::text || order_id::text)
                  || md5(random()::text || order_id::text)
where tracking_token is null;

-- giá trị mặc định để các lệnh INSERT trực tiếp (seed, script thử) vẫn có mã; ứng dụng tự sinh mã mạnh hơn bằng SecureRandom
alter table orders alter column tracking_token set default (md5(random()::text || clock_timestamp()::text)
                                                            || md5(random()::text || clock_timestamp()::text));
alter table orders alter column tracking_token set not null;
create unique index if not exists uq_orders_tracking_token on orders(tracking_token);

create table if not exists email_log (
    email_log_id bigserial primary key,
    order_id bigint references orders(order_id) on delete set null,
    kind varchar(30) not null,
    recipient varchar(255) not null,
    subject varchar(255) not null,
    status varchar(10) not null check (status in ('SENT', 'FAILED', 'SKIPPED')),
    error varchar(500),
    created_at timestamp not null default current_timestamp
);

create index if not exists idx_email_log_order on email_log(order_id);
create index if not exists idx_email_log_created on email_log(created_at);
