-- Trả góp qua đối tác trung gian: khách chọn đối tác, đối tác duyệt và thu nợ, cửa hàng nhận tiền sau khi trừ phí.
-- Chạy lại nhiều lần vẫn an toàn.

-- Đối tác trả góp (thẻ tín dụng, công ty tài chính) và điều kiện của từng đối tác
create table if not exists installment_partners (
    partner_code     varchar(30) primary key,
    partner_name     varchar(100) not null,
    partner_type     varchar(30) not null,
    min_order_total  numeric(15,2) not null default 3000000,
    max_order_total  numeric(15,2),
    is_active        boolean not null default true,
    display_order    integer not null default 0,
    constraint chk_installment_partners_type check (partner_type in ('CREDIT_CARD', 'FINANCE_COMPANY')),
    constraint chk_installment_partners_range check (
        min_order_total >= 0 and (max_order_total is null or max_order_total >= min_order_total))
);

-- Phí đối tác (% giá trị đơn) theo kỳ hạn; không có dòng cho kỳ hạn nào = đối tác không hỗ trợ kỳ hạn đó
create table if not exists installment_fee_rates (
    partner_code  varchar(30) not null references installment_partners(partner_code) on delete cascade,
    num_months    integer not null,
    fee_percent   numeric(5,2) not null,
    primary key (partner_code, num_months),
    constraint chk_installment_fee_rates_months check (num_months > 0),
    constraint chk_installment_fee_rates_percent check (fee_percent >= 0 and fee_percent <= 100)
);

insert into installment_partners (partner_code, partner_name, partner_type, min_order_total, max_order_total, display_order)
values ('CREDIT_CARD', 'Thẻ tín dụng', 'CREDIT_CARD', 3000000, null, 1),
       ('FINANCE_A', 'Công ty tài chính A', 'FINANCE_COMPANY', 3000000, null, 2),
       ('FINANCE_B', 'Công ty tài chính B', 'FINANCE_COMPANY', 3000000, 15000000, 3)
on conflict (partner_code) do nothing;

insert into installment_fee_rates (partner_code, num_months, fee_percent)
values ('CREDIT_CARD', 3, 2), ('CREDIT_CARD', 6, 3), ('CREDIT_CARD', 9, 4), ('CREDIT_CARD', 12, 5),
       ('FINANCE_A', 6, 4), ('FINANCE_A', 9, 5), ('FINANCE_A', 12, 6),
       ('FINANCE_B', 3, 3), ('FINANCE_B', 6, 4)
on conflict (partner_code, num_months) do nothing;

-- Hồ sơ trả góp: đối tác xử lý, phí và tiền thực nhận lưu cố định lúc duyệt, đối soát khi đối tác chuyển tiền
alter table installment_orders add column if not exists partner_code varchar(30) references installment_partners(partner_code);
alter table installment_orders add column if not exists partner_ref varchar(60);
alter table installment_orders add column if not exists fee_percent numeric(5,2);
alter table installment_orders add column if not exists fee_amount numeric(15,2);
alter table installment_orders add column if not exists net_amount numeric(15,2);
alter table installment_orders add column if not exists settled_at timestamp;
alter table installment_orders add column if not exists settled_by bigint references users(user_id);
alter table installment_orders add column if not exists attempt_count integer not null default 1;
alter table installment_orders add column if not exists last_event_id varchar(80);

-- đối tác thẻ tín dụng cần ngân hàng phát hành thẻ, công ty tài chính thì không
alter table installment_orders alter column card_bank_code drop not null;

alter table installment_orders drop constraint if exists chk_installment_orders_status;
alter table installment_orders add constraint chk_installment_orders_status check (status in (
    'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'SETTLED'));

create index if not exists idx_installment_orders_partner on installment_orders(partner_code, status);

-- hồ sơ cũ còn chờ duyệt (trước khi có đối tác) chuyển cho đối tác thẻ tín dụng xử lý
update installment_orders set partner_code = 'CREDIT_CARD'
where partner_code is null and status = 'PENDING_APPROVAL';

-- các hồ sơ vừa chuyển cho đối tác cần khoản thanh toán chính (chờ đối tác trả lời) như đơn trả góp mới
insert into payments (order_id, amount, payment_method, status)
select o.order_id, o.total_amount, 'INSTALLMENT', 'PENDING'
from installment_orders io
join orders o on o.order_id = io.order_id
where io.status = 'PENDING_APPROVAL' and io.partner_code is not null and o.total_amount > 0
  and not exists (select 1 from payments p where p.order_id = o.order_id and p.installment_payment_id is null);
