-- Giá vốn mẫu (dữ liệu mô phỏng, KHÔNG dùng cho production). Chạy sau 07 / 09 (docker-compose.yml).
--   * product_variants.average_cost: 80-90% giá bán, cố định theo từng phiên bản (80% + variant_id mod 11 điểm %),
--     làm tròn đến nghìn đồng; phiên bản đã có giá vốn thật (từ phiếu nhập) được giữ nguyên.
--   * order_items.unit_cost: giá vốn tại lúc xác nhận cho các dòng của đơn đã qua bước xác nhận (không tính đơn
--     đang chờ và đơn huỷ); dòng đã có giá vốn được giữ nguyên.
-- Chạy lại được.

update product_variants
set average_cost = case when price < 10000 then round(price * (80 + variant_id % 11) / 100.0, 2)
                       else round(price * (80 + variant_id % 11) / 100.0, -3) end
where average_cost is null and price > 0;

update order_items oi
set unit_cost = v.average_cost
from orders o, product_variants v
where o.order_id = oi.order_id
  and v.variant_id = oi.variant_id
  and oi.unit_cost is null
  and o.status in ('CONFIRMED', 'SHIPPING', 'DELIVERED');
