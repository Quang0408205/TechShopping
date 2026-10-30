-- Voucher giảm % cho đơn hàng (do ADMIN tạo), nhập mã hoặc chọn ở trang thanh toán.
-- - Giảm % trên tiền hàng đã trừ khuyến mãi sản phẩm (không giảm phí ship), có mức giảm tối đa (NULL = không giới hạn),
--   đơn tối thiểu tính trên phần hàng thuộc phạm vi; phạm vi = toàn đơn hoặc một số danh mục (voucher_categories).
-- - Lượt dùng: tổng lượt (NULL = không giới hạn) + mỗi khách; đơn huỷ trước khi giao trả lại lượt (released_at).
-- - orders lưu mã + số tiền giảm; order_items lưu phần voucher chia theo dòng (để hoàn tiền khi trả hàng đúng số tiền thực trả).
-- Chạy lại được.

create table if not exists vouchers (
    voucher_id bigserial primary key,
    code varchar(30) not null unique,
    name varchar(150) not null,
    description text,
    discount_percent decimal(5, 2) not null,
    max_discount decimal(15, 2),
    min_order_amount decimal(15, 2) not null default 0,
    starts_at timestamp not null,
    ends_at timestamp not null,
    usage_limit int,
    per_customer_limit int not null default 1,
    used_count int not null default 0,
    all_categories boolean not null default true,
    is_active boolean not null default true,
    created_by bigint not null references users(user_id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists voucher_categories (
    voucher_id bigint not null references vouchers(voucher_id) on delete cascade,
    category_id int not null references categories(category_id) on delete cascade,
    primary key (voucher_id, category_id)
);

create table if not exists voucher_usages (
    usage_id bigserial primary key,
    voucher_id bigint not null references vouchers(voucher_id),
    user_id bigint not null references users(user_id),
    order_id bigint not null unique references orders(order_id) on delete cascade,
    discount_amount decimal(15, 2) not null,
    used_at timestamp not null default current_timestamp,
    released_at timestamp
);

alter table orders add column if not exists voucher_id bigint references vouchers(voucher_id);
alter table orders add column if not exists voucher_code varchar(30);
alter table orders add column if not exists voucher_discount decimal(15, 2) not null default 0;
alter table order_items add column if not exists voucher_discount decimal(15, 2) not null default 0;

create index if not exists idx_vouchers_window on vouchers(is_active, starts_at, ends_at);
create index if not exists idx_voucher_usages_voucher_user on voucher_usages(voucher_id, user_id);
create index if not exists idx_orders_voucher on orders(voucher_id);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'chk_vouchers_percent') then
        alter table vouchers add constraint chk_vouchers_percent check (discount_percent > 0 and discount_percent <= 100);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_vouchers_window') then
        alter table vouchers add constraint chk_vouchers_window check (ends_at > starts_at);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_vouchers_amounts') then
        alter table vouchers add constraint chk_vouchers_amounts
            check ((max_discount is null or max_discount > 0) and min_order_amount >= 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_vouchers_limits') then
        alter table vouchers add constraint chk_vouchers_limits
            check ((usage_limit is null or usage_limit > 0) and per_customer_limit > 0 and used_count >= 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_vouchers_code') then
        alter table vouchers add constraint chk_vouchers_code check (code ~ '^[A-Z0-9_-]{3,30}$');
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_orders_voucher_discount') then
        alter table orders add constraint chk_orders_voucher_discount check (voucher_discount >= 0);
    end if;
    if not exists (select 1 from pg_constraint where conname = 'chk_order_items_voucher_discount') then
        alter table order_items add constraint chk_order_items_voucher_discount check (voucher_discount >= 0);
    end if;
end $$;
