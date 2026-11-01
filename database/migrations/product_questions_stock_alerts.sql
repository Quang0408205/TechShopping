-- Hỏi đáp sản phẩm và đăng ký báo khi có hàng lại.
-- - product_questions: câu hỏi của khách đã đăng nhập; chỉ hiện công khai khi đã có trả lời và không bị ẩn.
-- - stock_subscriptions: khách bấm "Báo tôi khi có hàng" ở phiên bản đang hết hàng; khi phiên bản có hàng lại
--   thì gửi một email rồi xoá đăng ký. Mỗi khách một dòng cho mỗi phiên bản.
-- Chạy lại được.

create table if not exists product_questions (
    question_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    user_id bigint not null references users(user_id) on delete cascade,
    question varchar(500) not null,
    answer varchar(1000),
    answered_by bigint references users(user_id) on delete set null,
    answered_at timestamp,
    is_hidden boolean not null default false,
    created_at timestamp not null default current_timestamp,
    check ((answer is null) = (answered_at is null))
);

create index if not exists idx_product_questions_product on product_questions (product_id, answered_at desc);
create index if not exists idx_product_questions_user on product_questions (user_id, created_at desc);
create index if not exists idx_product_questions_open on product_questions (created_at desc) where answer is null;

create table if not exists stock_subscriptions (
    subscription_id bigserial primary key,
    user_id bigint not null references users(user_id) on delete cascade,
    variant_id bigint not null references product_variants(variant_id) on delete cascade,
    created_at timestamp not null default current_timestamp,
    unique (user_id, variant_id)
);

create index if not exists idx_stock_subscriptions_variant on stock_subscriptions (variant_id);
