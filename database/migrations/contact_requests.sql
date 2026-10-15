-- Kế hoạch v2 / GĐ7 (2026-10-11): form Liên hệ lưu thật. Chạy lại nhiều lần vẫn an toàn.
-- db tạo mới từ database/techshopping.sql đã có sẵn mọi thứ dưới đây.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/contact_requests.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/contact_requests.sql
-- Khách (có thể chưa đăng nhập) gửi; nhân viên / quản lý chi nhánh xử lý, ADMIN chỉ xem.

create table if not exists contact_requests (
    contact_request_id bigserial primary key,
    user_id bigint references users(user_id),
    full_name varchar(100) not null,
    email varchar(255) not null,
    phone varchar(20),
    topic varchar(30) not null,
    message text not null,
    status varchar(20) not null default 'NEW',
    staff_note text,
    handled_by bigint references users(user_id),
    handled_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_contact_requests_topic') then
        alter table contact_requests add constraint chk_contact_requests_topic
            check (topic in ('ORDER', 'PRODUCT', 'AFTER_SALES', 'PAYMENT', 'OTHER'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_contact_requests_status') then
        alter table contact_requests add constraint chk_contact_requests_status
            check (status in ('NEW', 'IN_PROGRESS', 'RESOLVED'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_contact_requests_message_length') then
        alter table contact_requests add constraint chk_contact_requests_message_length
            check (char_length(message) between 10 and 2000);
    end if;
    -- "Đã xử lý" phải ghi lại đã làm gì
    if not exists (select 1 from pg_constraint where conname = 'chk_contact_requests_resolved_note') then
        alter table contact_requests add constraint chk_contact_requests_resolved_note
            check (status <> 'RESOLVED' or staff_note is not null);
    end if;
end $$;

create index if not exists idx_contact_requests_status_created on contact_requests(status, created_at);
create index if not exists idx_contact_requests_user on contact_requests(user_id);
