-- điểm tích luỹ + hạng thành viên.
-- - loyalty_tiers: 4 hạng cố định (mã), ADMIN sửa ngưỡng tổng chi tiêu + hệ số nhân điểm. Mặc định Thành viên 0 /
--   Bạc 50tr / Vàng 150tr / Kim cương 300tr; x1 / x1,2 / x1,5 / x2.
-- - customer_profiles.tier_code: hạng đã đạt (không tụt khi trả hàng hay khi ADMIN nâng ngưỡng).
-- - loyalty_transactions: lịch sử điểm (+ / −), số dư sau giao dịch; số dư hiện tại = customer_profiles.loyalty_points.
-- - orders: điểm đã dùng + số tiền giảm nhờ điểm + điểm được cộng khi giao; order_items: phần giảm nhờ điểm theo dòng
--   (trả hàng hoàn đúng số tiền thực trả).
-- - Cộng bù: mỗi đơn đã giao chưa có điểm được cộng 1 điểm / 10.000đ tiền hàng thực trả (không tính ship, trừ tiền
--   đã hoàn khi trả hàng), nhân hệ số của hạng khách đạt được trước đơn đó; hạng tính lại theo total_spent.
-- Chạy lại được.

create table if not exists loyalty_tiers (
    tier_code varchar(20) primary key,
    name varchar(50) not null,
    min_spent decimal(15, 2) not null,
    point_multiplier decimal(4, 2) not null,
    sort_order int not null unique,
    updated_at timestamp not null default current_timestamp,
    constraint chk_loyalty_tiers_code check (tier_code in ('MEMBER', 'SILVER', 'GOLD', 'DIAMOND')),
    constraint chk_loyalty_tiers_min check (min_spent >= 0),
    constraint chk_loyalty_tiers_multiplier check (point_multiplier >= 1 and point_multiplier <= 5)
);

insert into loyalty_tiers (tier_code, name, min_spent, point_multiplier, sort_order) values
    ('MEMBER', 'Thành viên', 0, 1.00, 1),
    ('SILVER', 'Bạc', 50000000, 1.20, 2),
    ('GOLD', 'Vàng', 150000000, 1.50, 3),
    ('DIAMOND', 'Kim cương', 300000000, 2.00, 4)
on conflict (tier_code) do nothing;

update customer_profiles set loyalty_points = 0 where loyalty_points is null;
update customer_profiles set total_spent = 0 where total_spent is null;
alter table customer_profiles alter column loyalty_points set not null;
alter table customer_profiles alter column total_spent set not null;
alter table customer_profiles add column if not exists tier_code varchar(20) not null default 'MEMBER'
    references loyalty_tiers(tier_code);
alter table customer_profiles drop constraint if exists chk_customer_profiles_points;
alter table customer_profiles add constraint chk_customer_profiles_points check (loyalty_points >= 0);

create table if not exists loyalty_transactions (
    loyalty_transaction_id bigserial primary key,
    user_id bigint not null references users(user_id) on delete cascade,
    order_id bigint references orders(order_id) on delete cascade,
    return_request_id bigint references return_requests(return_request_id) on delete set null,
    type varchar(20) not null,
    points int not null,
    balance_after int not null,
    note varchar(255),
    created_at timestamp not null default current_timestamp,
    constraint chk_loyalty_transactions_type check (type in ('EARN', 'REDEEM', 'REDEEM_BACK', 'REVOKE', 'ADJUST'))
);
create index if not exists idx_loyalty_transactions_user on loyalty_transactions(user_id, loyalty_transaction_id desc);
create index if not exists idx_loyalty_transactions_order on loyalty_transactions(order_id);

alter table orders add column if not exists points_used int not null default 0;
alter table orders add column if not exists points_discount decimal(15, 2) not null default 0;
alter table orders add column if not exists points_earned int not null default 0;
alter table orders drop constraint if exists chk_orders_points;
alter table orders add constraint chk_orders_points check (points_used >= 0 and points_discount >= 0 and points_earned >= 0);
alter table order_items add column if not exists points_discount decimal(15, 2) not null default 0;
alter table order_items drop constraint if exists chk_order_items_points_discount;
alter table order_items add constraint chk_order_items_points_discount check (points_discount >= 0);

-- ---- cộng bù cho đơn đã giao ----
with delivered as (
    select o.order_id, o.user_id, coalesce(o.delivered_at, o.order_date) as at, o.total_amount,
           greatest(o.total_amount - coalesce(o.shipping_cost, 0)
                    - coalesce((select sum(r.refund_amount) from return_requests r
                                where r.order_id = o.order_id and r.status = 'REFUNDED'), 0), 0) as base,
           coalesce(sum(o.total_amount) over (partition by o.user_id order by coalesce(o.delivered_at, o.order_date), o.order_id
                                              rows between unbounded preceding and 1 preceding), 0) as spent_before
    from orders o
    where o.status = 'DELIVERED'
      and not exists (select 1 from loyalty_transactions t where t.order_id = o.order_id and t.type = 'EARN')
),
earned as (
    select d.*, floor(floor(d.base / 10000) * (select t.point_multiplier from loyalty_tiers t
                                                where t.min_spent <= d.spent_before order by t.min_spent desc limit 1))::int as points
    from delivered d
)
insert into loyalty_transactions (user_id, order_id, type, points, balance_after, note, created_at)
select e.user_id, e.order_id, 'EARN', e.points,
       (select coalesce(sum(t.points), 0) from loyalty_transactions t where t.user_id = e.user_id)
           + sum(e.points) over (partition by e.user_id order by e.at, e.order_id),
       'Cộng bù khi mở chương trình điểm', e.at
from earned e
where e.points > 0;

update orders o set points_earned = t.points
from loyalty_transactions t
where t.order_id = o.order_id and t.type = 'EARN' and o.points_earned = 0;

update customer_profiles c set loyalty_points = greatest(coalesce((select sum(t.points) from loyalty_transactions t
                                                                   where t.user_id = c.customer_id), 0), 0);

update customer_profiles c set tier_code = (select t.tier_code from loyalty_tiers t where t.min_spent <= c.total_spent
                                            order by t.min_spent desc limit 1)
where (select t.sort_order from loyalty_tiers t where t.min_spent <= c.total_spent order by t.min_spent desc limit 1)
      > (select t.sort_order from loyalty_tiers t where t.tier_code = c.tier_code);
