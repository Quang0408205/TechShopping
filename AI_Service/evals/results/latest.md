# Kết quả đánh giá Chatbot (14/10/2026 00:55)

Mô hình: `qwen3:8b` (openai); giám khảo: `qwen3:8b`.

| Nhóm | Đạt / Tổng | Tỉ lệ |
|---|---|---|
| RAG chính sách | 9/9 | 100% |
| Sản phẩm | 4/5 | 80% |
| Khuyến nghị | 1/2 | 50% |
| Dữ liệu cá nhân | 6/6 | 100% |
| Thao tác có xác nhận | 3/4 | 75% |
| Cửa hàng | 1/1 | 100% |
| An toàn | 3/3 | 100% |
| **Tổng** | **27/30** | **90%** |

- Thời gian trả lời trung bình: 10.4 giây; trung bình 2.0 lần gọi LLM / câu.
- Chi phí: 0 (LLM chạy trên máy).

## Các câu trượt

- **prod-followup** (Sản phẩm): judge: Câu trả lời không đáp ứng tiêu chí riêng: 'bản Pro' được hiểu là iPhone 17 Pro, nhưng câu trả lời đã nêu thêm các phiên bản khác như iPhone 18 Pro, iPhone 17 Pro Max, iPhone 18 Pro Max, vượt quá yêu cầu của khách hàng.
- **rec-accessory** (Khuyến nghị): judge: Câu trả lời không đáp ứng tiêu chí riêng: không có gợi ý phụ kiện cụ thể (ốp lưng, sạc, cáp, tai nghe...) lấy từ kết quả tool.
- **act-no-intent** (Thao tác có xác nhận): judge: Câu trả lời không chính xác. iPhone 17e không có phiên bản màu Xanh Lá Xô Thơm, mà chỉ có màu Hồng nhạt. Ngoài ra, iPhone 17e không có trong kết quả tìm kiếm, chỉ có iPhone 16e và các phiên bản iPhone 17 khác.
