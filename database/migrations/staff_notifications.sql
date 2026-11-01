-- Thông báo nội bộ cho nhân sự: đơn mới, khách huỷ đơn, hàng sắp hết, yêu cầu bảo hành / trả hàng, phiếu chuyển kho đến.
-- store_id null = mọi nhân sự thấy; có store_id = nhân sự của chi nhánh đó và ADMIN thấy.
-- Trạng thái đã đọc tính riêng cho từng người (staff_notification_reads). Chạy lại được.

create table if not exists staff_notifications (
    notification_id bigserial primary key,
    store_id int references stores(store_id) on delete cascade,
    kind varchar(30) not null,
    title varchar(200) not null,
    message text,
    link varchar(200),
    created_at timestamp not null default current_timestamp
);

create index if not exists idx_staff_notifications_store on staff_notifications (store_id, created_at desc);
create index if not exists idx_staff_notifications_created on staff_notifications (created_at desc);

create table if not exists staff_notification_reads (
    notification_id bigint not null references staff_notifications(notification_id) on delete cascade,
    user_id bigint not null references users(user_id) on delete cascade,
    read_at timestamp not null default current_timestamp,
    primary key (notification_id, user_id)
);
