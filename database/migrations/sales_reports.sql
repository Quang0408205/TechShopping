-- Phase 10 (2026-10-09): báo cáo doanh thu + doanh số nhân viên. Chạy lại nhiều lần vẫn an toàn.
-- db tạo mới từ database/techshopping.sql đã có sẵn mọi thứ dưới đây.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/sales_reports.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/sales_reports.sql

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_sales_records_amounts') then
        alter table sales_records add constraint chk_sales_records_amounts
            check (sales_amount >= 0 and (commission is null or commission >= 0));
    end if;
end $$;

-- doanh thu = đơn đã giao, lọc theo ngày giao; tiền hoàn = trả hàng đã hoàn tiền, theo ngày hoàn
create index if not exists idx_orders_delivered_at on orders(delivered_at) where status = 'DELIVERED';
create index if not exists idx_return_requests_refunded_at on return_requests(completed_at) where status = 'REFUNDED';
create index if not exists idx_sales_records_store_recorded on sales_records(store_id, recorded_at);
create index if not exists idx_sales_records_employee_recorded on sales_records(employee_id, recorded_at);
