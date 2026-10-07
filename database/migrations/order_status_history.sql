-- lịch sử đổi trạng thái đơn hàng (2026-10-05, khảo sát từ docs/techshopping_v3.sql nhóm 5): mỗi lần
-- orders.status đổi thì có 1 dòng ở đây. Khác bản thiết kế v3: ghi bằng CODE JAVA khi service đổi trạng
-- thái (bước sau), KHÔNG dùng trigger Postgres — dự án không dùng trigger ở migration nào khác. Bước này
-- chỉ thêm bảng (schema + entity + repository), chưa có code nào ghi vào bảng.
-- db tạo mới từ database/techshopping.sql đã có sẵn bảng này; file này chỉ dành cho db tạo từ bản schema cũ
-- (volume postgres_data đã có từ trước). Chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/order_status_history.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/order_status_history.sql

create table if not exists order_status_history (
    history_id bigserial primary key,
    order_id bigint not null references orders(order_id) on delete cascade,
    old_status varchar(50),
    new_status varchar(50) not null,
    changed_by bigint references users(user_id),
    changed_at timestamp not null default current_timestamp
);

create index if not exists idx_order_status_history_order on order_status_history(order_id, changed_at);
create index if not exists idx_order_status_history_changed_by on order_status_history(changed_by);
