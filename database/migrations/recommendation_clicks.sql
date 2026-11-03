-- Đo hiệu quả gợi ý trên web. recommendations đã là nhật ký mỗi sản phẩm được gợi ý cho khách đã đăng nhập (lượt
-- hiển thị); clicked_at ghi lúc khách bấm vào chính sản phẩm đó trong khu gợi ý. Tỉ lệ bấm (CTR) = lượt bấm / lượt
-- hiển thị; chuyển đổi = lượt bấm mà sau đó khách thêm vào giỏ / mua sản phẩm trong 7 ngày (user_interactions).
-- Chạy lại được.

alter table recommendations add column if not exists clicked_at timestamp;

create index if not exists idx_recommendations_created on recommendations(created_at);
create index if not exists idx_recommendations_user_product on recommendations(user_id, product_id, created_at desc);
