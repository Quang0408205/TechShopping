-- Phase 6 (2026-10-08): bảo hành / bảo trì / đổi trả. Chạy lại nhiều lần vẫn an toàn.
-- db tạo mới từ database/techshopping.sql đã có sẵn mọi thứ dưới đây.
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping < database/migrations/after_sales.sql
--   docker exec -i techshopping-postgres psql -U postgres -d techshopping_test < database/migrations/after_sales.sql

alter table warranty_requests
    add column if not exists received_at timestamp,
    add column if not exists rejection_reason text,
    add column if not exists cancelled_at timestamp;

alter table maintenance_requests
    add column if not exists estimated_completion_date date,
    add column if not exists estimated_cost decimal(15, 2),
    add column if not exists actual_cost decimal(15, 2),
    add column if not exists received_at timestamp,
    add column if not exists rejection_reason text,
    add column if not exists cancelled_at timestamp;

alter table return_requests
    add column if not exists reason_type varchar(30),
    add column if not exists assigned_to_employee bigint references users(user_id),
    add column if not exists received_at timestamp,
    add column if not exists rejection_reason text,
    add column if not exists cancelled_at timestamp;

-- null = chưa nhận hàng; true = đã cộng lại tồn kho chi nhánh
alter table return_items add column if not exists restocked boolean;

-- ứng dụng luôn ghi trạng thái chữ hoa
alter table warranty_requests alter column status set default 'PENDING';
alter table warranty_requests alter column status set not null;
alter table maintenance_requests alter column status set default 'PENDING';
alter table maintenance_requests alter column status set not null;
alter table return_requests alter column status set default 'PENDING';
alter table return_requests alter column status set not null;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_warranties_dates') then
        alter table warranties add constraint chk_warranties_dates check (warranty_end_date >= warranty_start_date);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_warranty_requests_status') then
        alter table warranty_requests add constraint chk_warranty_requests_status
            check (status in ('PENDING', 'RECEIVED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'CANCELLED'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_warranty_requests_rejection') then
        alter table warranty_requests add constraint chk_warranty_requests_rejection
            check (status <> 'REJECTED' or rejection_reason is not null);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_maintenance_requests_status') then
        alter table maintenance_requests add constraint chk_maintenance_requests_status
            check (status in ('PENDING', 'RECEIVED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'CANCELLED'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_maintenance_requests_rejection') then
        alter table maintenance_requests add constraint chk_maintenance_requests_rejection
            check (status <> 'REJECTED' or rejection_reason is not null);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_maintenance_requests_type') then
        alter table maintenance_requests add constraint chk_maintenance_requests_type
            check (maintenance_type in ('CLEANING', 'SOFTWARE', 'REPAIR', 'OTHER'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_maintenance_requests_costs') then
        alter table maintenance_requests add constraint chk_maintenance_requests_costs
            check ((estimated_cost is null or estimated_cost >= 0) and (actual_cost is null or actual_cost >= 0));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_return_requests_status') then
        alter table return_requests add constraint chk_return_requests_status
            check (status in ('PENDING', 'APPROVED', 'RECEIVED', 'REFUNDED', 'REJECTED', 'CANCELLED'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_return_requests_rejection') then
        alter table return_requests add constraint chk_return_requests_rejection
            check (status <> 'REJECTED' or rejection_reason is not null);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_return_requests_reason_type') then
        alter table return_requests add constraint chk_return_requests_reason_type
            check (reason_type in ('DEFECTIVE', 'NOT_AS_DESCRIBED', 'CHANGED_MIND', 'OTHER'));
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_return_requests_refund') then
        alter table return_requests add constraint chk_return_requests_refund check (refund_amount is null or refund_amount >= 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_return_items_quantity') then
        alter table return_items add constraint chk_return_items_quantity check (quantity > 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'uq_return_items_request_item') then
        alter table return_items add constraint uq_return_items_request_item unique (return_request_id, order_item_id);
    end if;
end $$;

-- mỗi dòng đơn chỉ có 1 yêu cầu bảo hành / bảo trì đang mở (chặn cả 2 lần gửi cùng lúc)
create unique index if not exists uq_warranty_requests_open on warranty_requests(warranty_id)
    where status in ('PENDING', 'RECEIVED', 'PROCESSING');
create unique index if not exists uq_maintenance_requests_open on maintenance_requests(order_item_id)
    where status in ('PENDING', 'RECEIVED', 'PROCESSING');

create index if not exists idx_maintenance_requests_user_id on maintenance_requests(user_id);
create index if not exists idx_maintenance_requests_status on maintenance_requests(status);
create index if not exists idx_return_requests_user_id on return_requests(user_id);
create index if not exists idx_return_requests_order_id on return_requests(order_id);
create index if not exists idx_return_requests_status on return_requests(status);
create index if not exists idx_return_items_order_item_id on return_items(order_item_id);

-- ảnh khách đính kèm (tối đa 5 / yêu cầu, kiểm tra ở ứng dụng); mỗi ảnh thuộc đúng 1 loại yêu cầu
create table if not exists service_request_images (
    image_id bigserial primary key,
    warranty_request_id bigint references warranty_requests(warranty_request_id) on delete cascade,
    maintenance_id bigint references maintenance_requests(maintenance_id) on delete cascade,
    return_request_id bigint references return_requests(return_request_id) on delete cascade,
    image_url text not null,
    display_order int not null,
    created_at timestamp default current_timestamp,
    constraint chk_service_request_images_owner
        check (num_nonnulls(warranty_request_id, maintenance_id, return_request_id) = 1)
);
create unique index if not exists uq_service_request_images_warranty
    on service_request_images(warranty_request_id, display_order) where warranty_request_id is not null;
create unique index if not exists uq_service_request_images_maintenance
    on service_request_images(maintenance_id, display_order) where maintenance_id is not null;
create unique index if not exists uq_service_request_images_return
    on service_request_images(return_request_id, display_order) where return_request_id is not null;

-- danh sách gộp 3 loại cho trang quản trị / "Yêu cầu dịch vụ" (lọc + phân trang ở DB)
create or replace view service_requests_view as
select 'WARRANTY' || '-' || wr.warranty_request_id as view_id, 'WARRANTY' as request_type,
       wr.warranty_request_id as request_id, wr.user_id, o.order_id, o.store_id, oi.order_item_id,
       wr.status, wr.issue_description as description, wr.assigned_to_employee, wr.created_at, wr.updated_at
from warranty_requests wr
join warranties w on w.warranty_id = wr.warranty_id
join order_items oi on oi.order_item_id = w.order_item_id
join orders o on o.order_id = oi.order_id
union all
select 'MAINTENANCE' || '-' || mr.maintenance_id, 'MAINTENANCE', mr.maintenance_id, mr.user_id, o.order_id,
       o.store_id, oi.order_item_id, mr.status, mr.description, mr.assigned_to_employee, mr.created_at, mr.updated_at
from maintenance_requests mr
join order_items oi on oi.order_item_id = mr.order_item_id
join orders o on o.order_id = oi.order_id
union all
select 'RETURN' || '-' || rr.return_request_id, 'RETURN', rr.return_request_id, rr.user_id, o.order_id,
       o.store_id, null, rr.status, rr.reason, rr.assigned_to_employee, rr.created_at, rr.updated_at
from return_requests rr
join orders o on o.order_id = rr.order_id;

-- phiếu bảo hành cho đơn đã giao trước Phase 6 (từ ngày giao, theo số tháng bảo hành hiện tại của sản phẩm)
insert into warranties (order_item_id, warranty_start_date, warranty_end_date, warranty_type)
select oi.order_item_id, o.delivered_at::date, (o.delivered_at::date + make_interval(months => p.warranty_months))::date,
       'STANDARD'
from order_items oi
join orders o on o.order_id = oi.order_id
join product_variants v on v.variant_id = oi.variant_id
join products p on p.product_id = v.product_id
where o.status = 'DELIVERED' and o.delivered_at is not null and coalesce(p.warranty_months, 0) > 0
on conflict (order_item_id) do nothing;
