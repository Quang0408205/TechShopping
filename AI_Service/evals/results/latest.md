# Kết quả đánh giá Chatbot (25/11/2026 21:37)

Mô hình: `qwen3:8b` (openai); giám khảo: `qwen3:8b`.

| Nhóm | Đạt / Tổng | Tỉ lệ |
|---|---|---|
| RAG chính sách | 8/9 | 89% |
| Sản phẩm | 5/5 | 100% |
| Khuyến nghị | 2/2 | 100% |
| Dữ liệu cá nhân | 5/6 | 83% |
| Thao tác có xác nhận | 3/4 | 75% |
| Cửa hàng | 1/1 | 100% |
| An toàn | 3/3 | 100% |
| **Tổng** | **27/30** | **90%** |

- Thời gian trả lời trung bình: 27.8 giây; trung bình 2.0 lần gọi LLM / câu.
- Chi phí: 0 (LLM chạy trên máy).

## Các câu trượt

- **pol-shipping-fee** (RAG chính sách): include: thiếu [['tỉnh', 'thành']]
- **acc-order-code** (Dữ liệu cá nhân): include: thiếu [['6.220.000', '6,220,000', '6220000']]
- **act-cancel** (Thao tác có xác nhận): include: thiếu [['xác nhận']]; action: không có thao tác CANCEL_ORDER
