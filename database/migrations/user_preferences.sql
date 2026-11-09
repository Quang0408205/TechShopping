-- Bảng hỏi sở thích cho khách mới (2026-11-09): khách chưa có hành vi trả lời danh mục quan tâm (bắt buộc), tầm giá,
-- thương hiệu, nhu cầu; AI Service gợi ý theo câu trả lời thay cho danh sách phổ biến.
-- Mỗi dòng là một lựa chọn: đúng một trong category_id / brand_id / need_id / price_level.
-- Chạy cho cả techshopping và techshopping_test; chạy lại nhiều lần vẫn an toàn.

create table if not exists user_preferences (
    user_preference_id bigserial primary key,
    user_id bigint not null references users(user_id) on delete cascade,
    category_id int references categories(category_id) on delete cascade,
    brand_id int references brands(brand_id) on delete cascade,
    need_id int references needs(need_id) on delete cascade,
    price_level varchar(10)
        constraint chk_user_preferences_price check (price_level in ('LOW', 'MID', 'HIGH')),
    created_at timestamp not null default current_timestamp,
    constraint chk_user_preferences_one_answer check (num_nonnulls(category_id, brand_id, need_id, price_level) = 1),
    constraint uq_user_preferences unique nulls not distinct (user_id, category_id, brand_id, need_id, price_level)
);
