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
