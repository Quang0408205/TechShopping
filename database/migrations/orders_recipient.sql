alter table orders add column if not exists recipient_name varchar(120);
alter table orders add column if not exists recipient_phone varchar(20);

-- đơn cũ (nếu có) chưa có người nhận: để chuỗi rỗng rồi mới bắt buộc not null
update orders set recipient_name = '' where recipient_name is null;
update orders set recipient_phone = '' where recipient_phone is null;

alter table orders alter column recipient_name set not null;
alter table orders alter column recipient_phone set not null;
