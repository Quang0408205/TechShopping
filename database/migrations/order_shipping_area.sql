-- Kế hoạch v5 GĐ1: địa chỉ giao hàng 2 cấp (từ 01/07/2025: Tỉnh / Thành → Phường / Xã, không còn quận / huyện).
-- Đơn giao tận nhà lưu thêm tỉnh + phường riêng; shipping_address vẫn là chuỗi đầy đủ. Đơn cũ / nhận tại cửa hàng: NULL.
-- Chạy lại được.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_province VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_ward VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_orders_shipping_province ON orders (shipping_province);
