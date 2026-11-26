# Kết quả đánh giá Chatbot (27/11/2026 02:05)

Mô hình: `qwen3:8b` (openai); giám khảo: `qwen3:8b`.

| Nhóm | Đạt / Tổng | Tỉ lệ |
|---|---|---|
| RAG chính sách | 9/9 | 100% |
| Sản phẩm | 5/5 | 100% |
| Khuyến nghị | 2/2 | 100% |
| Dữ liệu cá nhân | 5/6 | 83% |
| Thao tác có xác nhận | 3/4 | 75% |
| Cửa hàng | 1/1 | 100% |
| An toàn | 3/3 | 100% |
| **Tổng** | **28/30** | **93%** |

- Thời gian trả lời trung bình: 27.1 giây; trung bình 2.0 lần gọi LLM / câu.
- Chi phí: 0 (LLM chạy trên máy).

## Các câu trượt

- **acc-order-code** (Dữ liệu cá nhân): include: thiếu [['6.220.000', '6,220,000', '6220000']]
- **act-cancel** (Thao tác có xác nhận): include: thiếu [['xác nhận']]; action: không có thao tác CANCEL_ORDER
