# Đánh giá tìm kiếm RAG (08/11/2026 10:58)

40 câu hỏi có đáp án: POLICY/semantic: 15, PRODUCT/exact: 17, PRODUCT/semantic: 8.

## Toàn bộ

| Cách tìm | Hit@1 | Hit@3 | Hit@5 | Hit@8 | Recall@3 | MRR | ms / câu |
|---|---|---|---|---|---|---|---|
| vector | 0.850 | 0.925 | 0.950 | 1.000 | 0.848 | 0.899 | 62 |
| keyword | 0.650 | 0.800 | 0.875 | 0.900 | 0.762 | 0.734 | 12 |
| hybrid w=1 | 0.750 | 0.900 | 0.950 | 0.950 | 0.840 | 0.832 | 74 |
| hybrid w=0.5 | 0.800 | 0.900 | 0.950 | 0.950 | 0.827 | 0.856 | 70 |
| hybrid w=0.25 | 0.800 | 0.900 | 0.925 | 0.950 | 0.827 | 0.854 | 83 |

## Theo nhóm câu hỏi (Hit@1 / Hit@3 / MRR)

| Nhóm | vector | keyword | hybrid w=1 | hybrid w=0.5 | hybrid w=0.25 |
|---|---|---|---|---|---|
| POLICY/semantic | 0.80 / 0.87 / 0.86 | 0.67 / 0.87 / 0.77 | 0.73 / 0.87 / 0.82 | 0.73 / 0.87 / 0.82 | 0.73 / 0.87 / 0.82 |
| PRODUCT/exact | 1.00 / 1.00 / 1.00 | 0.71 / 0.88 / 0.80 | 0.88 / 1.00 / 0.94 | 0.94 / 1.00 / 0.97 | 1.00 / 1.00 / 1.00 |
| PRODUCT/semantic | 0.62 / 0.88 / 0.77 | 0.50 / 0.50 / 0.53 | 0.50 / 0.75 / 0.62 | 0.62 / 0.75 / 0.69 | 0.50 / 0.75 / 0.60 |

## Câu chưa đúng ở vị trí 1

- **vector** (6): p-pickup (top1: Chính sách giao hàng – Chi nhánh xử lý đ), p-maintenance (top1: Bảo hành, bảo trì và trả hàng – Bảo hành), p-account (top1: Chính sách giao hàng – Chi nhánh xử lý đ), s-gaming-mouse (top1: 15310), s-watch-ultra (top1: 11817), s-cheap-tab (top1: 11808)
- **keyword** (14): p-ship-fee (top1: Chính sách giao hàng – Chi nhánh xử lý đ), p-pickup (top1: Bảo hành, bảo trì và trả hàng – Trả hàng), p-maintenance (top1: Bảo hành, bảo trì và trả hàng – Trả hàng), p-cancel (top1: Chính sách giao hàng – Chi nhánh xử lý đ), p-account (top1: Chính sách giao hàng – Phí giao hàng), e-s26plus (top1: 78), e-note15proplus (top1: 11725), e-gshock (top1: 14008), e-ipadmini (top1: 15362), e-taba11plus (top1: 11801), s-pencil (top1: 15251), s-watch-ultra (top1: 831), s-macbook-air (top1: 832), s-cheap-tab (top1: 11864)
- **hybrid w=1** (10): p-ship-fee (top1: Chính sách giao hàng – Chi nhánh xử lý đ), p-pickup (top1: Bảo hành, bảo trì và trả hàng – Trả hàng), p-maintenance (top1: Bảo hành, bảo trì và trả hàng – Bảo hành), p-account (top1: Chính sách giao hàng – Phí giao hàng), e-s26plus (top1: 12), e-gshock (top1: 14010), s-pencil (top1: 15362), s-gaming-mouse (top1: 15310), s-watch-ultra (top1: 11817), s-macbook-air (top1: 15280)
- **hybrid w=0.5** (8): p-ship-fee (top1: Chính sách giao hàng – Chi nhánh xử lý đ), p-pickup (top1: Chính sách giao hàng – Trạng thái đơn hà), p-maintenance (top1: Bảo hành, bảo trì và trả hàng – Bảo hành), p-account (top1: Chính sách giao hàng – Phí giao hàng), e-gshock (top1: 14010), s-pencil (top1: 15362), s-gaming-mouse (top1: 15310), s-watch-ultra (top1: 11817)
- **hybrid w=0.25** (8): p-ship-fee (top1: Chính sách giao hàng – Chi nhánh xử lý đ), p-pickup (top1: Chính sách giao hàng – Chi nhánh xử lý đ), p-maintenance (top1: Bảo hành, bảo trì và trả hàng – Bảo hành), p-account (top1: Chính sách giao hàng – Chi nhánh xử lý đ), s-pencil (top1: 15362), s-gaming-mouse (top1: 15310), s-watch-ultra (top1: 11817), s-cheap-tab (top1: 11808)
