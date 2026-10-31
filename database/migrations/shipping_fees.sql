-- Phí giao hàng theo tỉnh / thành: một dòng cho mỗi tỉnh có phí riêng do ADMIN đặt. Tỉnh không có dòng riêng tính phí
-- mặc định (30.000đ, ShippingPolicy.FLAT_FEE); nhận tại cửa hàng luôn miễn phí. Tên tỉnh theo cách gọi của ô chọn địa
-- chỉ 2 cấp ở trang thanh toán (không phân biệt hoa thường).
-- Chạy lại được.

create table if not exists shipping_fee_rules (
    rule_id serial primary key,
    province_name varchar(100) not null,
    fee decimal(15, 2) not null check (fee >= 0),
    updated_at timestamp not null default current_timestamp
);

create unique index if not exists uq_shipping_fee_rules_province on shipping_fee_rules (lower(province_name));
