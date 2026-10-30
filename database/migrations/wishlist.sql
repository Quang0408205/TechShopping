-- Danh sách yêu thích của khách hàng (theo tài khoản).
-- - price_at_add: giá bán lúc khách bấm yêu thích (để hiện "đã giảm bao nhiêu từ lúc bạn lưu")
-- - last_alert_price: mốc giá để báo giảm giá qua email: giá hiện tại thấp hơn mốc thì gửi mail rồi hạ mốc xuống;
--   giá tăng lên thì nâng mốc lên (lần giảm sau lại được báo)
-- Chạy lại được.

create table if not exists wishlist_items (
    wishlist_item_id bigserial primary key,
    user_id bigint not null references users(user_id),
    product_id bigint not null references products(product_id),
    price_at_add decimal(15, 2),
    last_alert_price decimal(15, 2),
    created_at timestamp not null default current_timestamp,
    unique (user_id, product_id)
);

create index if not exists idx_wishlist_items_product on wishlist_items(product_id);
