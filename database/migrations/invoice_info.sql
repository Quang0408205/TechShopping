-- Thông tin xuất hoá đơn công ty (tuỳ chọn, khách nhập ở trang thanh toán): chỉ lưu và in lên hoá đơn, không nối hệ
-- thống hoá đơn điện tử. Cả ba cột cùng có hoặc cùng trống.
-- Chạy lại được.

alter table orders add column if not exists invoice_company varchar(200);
alter table orders add column if not exists invoice_tax_code varchar(20);
alter table orders add column if not exists invoice_address varchar(500);
