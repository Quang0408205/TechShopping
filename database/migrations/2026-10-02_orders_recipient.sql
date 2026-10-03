-- phase 4 (2026-10-02): tên và số điện thoại người nhận trên đơn hàng.
-- db tạo mới từ database/techshopping.sql đã có sẵn 2 cột này; file này chỉ dành cho db tạo từ bản schema cũ
-- (volume postgres_data đã có từ trước). chạy lại nhiều lần vẫn an toàn.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/2026-10-02_orders_recipient.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/2026-10-02_orders_recipient.sql

alter table orders add column if not exists recipient_name varchar(120);
alter table orders add column if not exists recipient_phone varchar(20);

-- đơn cũ (nếu có) chưa có người nhận: để chuỗi rỗng rồi mới bắt buộc not null
update orders set recipient_name = '' where recipient_name is null;
update orders set recipient_phone = '' where recipient_phone is null;

alter table orders alter column recipient_name set not null;
alter table orders alter column recipient_phone set not null;
