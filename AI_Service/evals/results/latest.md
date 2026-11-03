# Kết quả đánh giá Chatbot (03/11/2026 01:58)

Mô hình: `qwen3:8b` (openai); giám khảo: `qwen3:8b`.

| Nhóm | Đạt / Tổng | Tỉ lệ |
|---|---|---|
| RAG chính sách | 9/9 | 100% |
| Sản phẩm | 3/5 | 60% |
| Khuyến nghị | 1/2 | 50% |
| Dữ liệu cá nhân | 6/6 | 100% |
| Thao tác có xác nhận | 3/4 | 75% |
| Cửa hàng | 1/1 | 100% |
| An toàn | 3/3 | 100% |
| **Tổng** | **26/30** | **87%** |

- Thời gian trả lời trung bình: 30.9 giây; trung bình 2.0 lần gọi LLM / câu.
- Chi phí: 0 (LLM chạy trên máy).

## Các câu trượt

- **prod-price** (Sản phẩm): price: không có giá 34.490.000đ
- **prod-followup** (Sản phẩm): price: không có giá 31.990.000đ; judge: Câu trả lời chưa nêu rõ giá của iPhone 17 Pro 256GB mà chỉ nêu giá từ 34.990.000đ, không đáp ứng tiêu chí riêng yêu cầu phải nêu giá cụ thể cho phiên bản 256GB.
- **rec-accessory** (Khuyến nghị): judge: Câu trả lời không đáp ứng tiêu chí riêng: không có gợi ý phụ kiện cụ thể (ốp lưng, sạc, cáp, tai nghe...) lấy từ kết quả tool.
- **act-add-cart** (Thao tác có xác nhận): include: thiếu [['xác nhận']]; action: không có thao tác ADD_TO_CART
