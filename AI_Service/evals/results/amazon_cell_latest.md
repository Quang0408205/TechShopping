# Đánh giá trên dữ liệu công khai: Amazon Reviews 2023 – Cell Phones & Accessories (5-core)

Lấy mẫu: top 5000 sản phẩm, 5-core, 20000 người dùng ngẫu nhiên (seed 42). 4999 sản phẩm, 20000 người dùng, 126097 lượt (2009-09-01 → 2023-08-25). Mỗi đánh giá là một lượt mua; đơn hàng = sản phẩm cùng người, cùng ngày.

# Đánh giá offline hệ thống khuyến nghị (2026-11-08 20:08:53)

- Chia theo thời gian từng người dùng: 70% hành vi đầu để huấn luyện, 10% tiếp theo để tinh chỉnh (validation), 20% cuối để kiểm thử (79168 / 20465 / 26464 lượt).
- Tham số chọn trên validation (19998 người dùng): ALS 64 chiều, λ = 0.1, a = 10.0; α = 0.2; nửa đời giảm trọng số không giảm; MMR λ = 0.9; ngưỡng cold-start k = 2.
- Kiểm thử: học lại trên huấn luyện + validation, chấm 20000 người dùng có ít nhất 2 lượt tương tác và có sản phẩm mới trong phần kiểm thử; 4999 sản phẩm đang bán. Khoảng tin cậy 95%: bootstrap 1000 lần theo người dùng.

## Kiểm thử (đúng = mọi tương tác)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 | Coverage | Diversity |
|---|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0026 | 0.0200 | 0.0118 (0.0106–0.0129) | 0.0259 | 0.0030 | 0.7111 |
| Item-kNN (baseline) | 0.0043 | 0.0346 | 0.0208 (0.0193–0.0224) | 0.0423 | 0.9972 | 0.6718 |
| CBF (α=1) | 0.0014 | 0.0110 | 0.0062 (0.0053–0.0070) | 0.0137 | 0.7063 | 0.0523 |
| CF/ALS (α=0) | 0.0058 | 0.0462 | 0.0274 (0.0257–0.0291) | 0.0563 | 0.3621 | 0.6905 |
| Hybrid α=0.2 | 0.0060 | 0.0476 | 0.0280 (0.0262–0.0297) | 0.0583 | 0.4405 | 0.5877 |
| Hybrid α=0.6 | 0.0047 | 0.0371 | 0.0213 (0.0198–0.0229) | 0.0459 | 0.8174 | 0.2693 |
| Hybrid α=0.2 + MMR λ=0.9 | 0.0059 | 0.0472 | 0.0277 (0.0260–0.0295) | 0.0576 | 0.4285 | 0.6454 |
| Hybrid α=0.6 + MMR λ=0.7 | 0.0056 | 0.0437 | 0.0244 (0.0228–0.0259) | 0.0544 | 0.7095 | 0.5371 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.2 − CF/ALS (α=0): +0.0006 (-0.0000 đến +0.0012), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.2 − CBF (α=1): +0.0218 (+0.0199 đến +0.0237), có ý nghĩa
- NDCG@10: Hybrid α=0.2 − Item-kNN (baseline): +0.0072 (+0.0056 đến +0.0088), có ý nghĩa
- NDCG@10: Hybrid α=0.2 − Phổ biến (baseline): +0.0162 (+0.0143 đến +0.0182), có ý nghĩa

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.2 + MMR λ=0.9 − Hybrid α=0.2: -0.0002 (-0.0006 đến +0.0001), chưa đủ chắc chắn
- Diversity: Hybrid α=0.2 + MMR λ=0.9 − Hybrid α=0.2: +0.0577 (+0.0569 đến +0.0585), có ý nghĩa

## Kiểm thử (đúng = chỉ thêm giỏ / mua, 20000 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0026 | 0.0200 | 0.0118 (0.0106–0.0129) | 0.0259 |
| Item-kNN (baseline) | 0.0043 | 0.0346 | 0.0208 (0.0192–0.0224) | 0.0423 |
| CBF (α=1) | 0.0014 | 0.0110 | 0.0062 (0.0054–0.0069) | 0.0137 |
| CF/ALS (α=0) | 0.0058 | 0.0462 | 0.0274 (0.0256–0.0292) | 0.0563 |
| Hybrid α=0.2 | 0.0060 | 0.0476 | 0.0280 (0.0262–0.0297) | 0.0583 |
| Hybrid α=0.6 | 0.0047 | 0.0371 | 0.0213 (0.0199–0.0228) | 0.0459 |
| Hybrid α=0.2 + MMR λ=0.9 | 0.0059 | 0.0472 | 0.0277 (0.0260–0.0295) | 0.0576 |
| Hybrid α=0.6 + MMR λ=0.7 | 0.0056 | 0.0437 | 0.0244 (0.0229–0.0259) | 0.0544 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.2 − CF/ALS (α=0): +0.0006 (+0.0000 đến +0.0013), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.2 − CBF (α=1): +0.0218 (+0.0201 đến +0.0237), có ý nghĩa
- NDCG@10: Hybrid α=0.2 − Item-kNN (baseline): +0.0072 (+0.0056 đến +0.0089), có ý nghĩa
- NDCG@10: Hybrid α=0.2 − Phổ biến (baseline): +0.0162 (+0.0144 đến +0.0182), có ý nghĩa

## Khách mới (1–1 lượt tương tác, 0 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0000 | 0.0000 | 0.0000 | 0.0000 |
| CBF (khách mới) | 0.0000 | 0.0000 | 0.0000 | 0.0000 |

## Cold-start: NDCG@10 theo số lượt của khách mới (kiểm thử)

Cắt lịch sử của 30% khách còn m lượt đầu rồi học lại; Hybrid được dùng ALS ngay từ lượt đầu để so với chỉ CBF.

| Số lượt m | Người dùng | Phổ biến | Chỉ CBF | Hybrid |
|---|---|---|---|---|
| 1 | 63 | 0.0280 | 0.0000 | 0.0000 |
| 2 | 63 | 0.0280 | 0.0017 | 0.0020 |
| 3 | 63 | 0.0280 | 0.0000 | 0.0000 |
| 5 | 63 | 0.0280 | 0.0000 | 0.0024 |
| 8 | 63 | 0.0281 | 0.0000 | 0.0084 |
| 12 | 63 | 0.0281 | 0.0000 | 0.0142 |

## Mua kèm (19890 đơn kiểm thử, 6447 cặp đơn – sản phẩm)

Luật kết hợp học trên 79654 đơn đặt trước; 14% sản phẩm trong đơn kiểm thử có danh sách mua kèm.

| Cách gợi ý | HitRate@4 | Precision@4 |
|---|---|---|
| Mua kèm (luật kết hợp + phụ kiện) | 0.0104 | 0.0026 |
| Bán chạy nhất (baseline) | 0.0155 | 0.0041 |

## Nâng cấp

95% trong 4999 sản phẩm đang bán có gợi ý nâng cấp; gợi ý đầu tiên đắt hơn trung bình 12%.

## Tinh chỉnh trên validation

Tham số ALS (chỉ ALS, NDCG@10):

| Số chiều | λ | a | NDCG |
|---|---|---|---|
| 64 | 0.1 | 10.0 | 0.0325 |
| 64 | 0.1 | 5.0 | 0.0324 |
| 64 | 1.0 | 5.0 | 0.0324 |
| 64 | 1.0 | 10.0 | 0.0322 |
| 32 | 0.1 | 5.0 | 0.0317 |
| 32 | 1.0 | 5.0 | 0.0317 |
| 64 | 0.1 | 20.0 | 0.0314 |
| 64 | 1.0 | 20.0 | 0.0312 |
| 32 | 1.0 | 10.0 | 0.0308 |
| 32 | 0.1 | 10.0 | 0.0307 |
| 32 | 1.0 | 20.0 | 0.0291 |
| 32 | 0.1 | 20.0 | 0.0289 |
| 16 | 0.1 | 5.0 | 0.0276 |
| 16 | 1.0 | 5.0 | 0.0275 |
| 16 | 0.1 | 10.0 | 0.0272 |
| 16 | 1.0 | 10.0 | 0.0272 |
| 16 | 0.1 | 20.0 | 0.0263 |
| 16 | 1.0 | 20.0 | 0.0262 |

| Mô hình | Precision@10 | Recall@10 | NDCG@10 | HitRate@10 | Coverage |
|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0022 | 0.0219 | 0.0125 | 0.0223 | 0.0028 |
| CF/ALS (α=0) | 0.0060 | 0.0589 | 0.0325 | 0.0594 | 0.3725 |
| Hybrid α=0.1 | 0.0061 | 0.0608 | 0.0335 | 0.0614 | 0.4015 |
| Hybrid α=0.2 | 0.0063 | 0.0622 | 0.0341 | 0.0627 | 0.4555 |
| Hybrid α=0.3 | 0.0062 | 0.0610 | 0.0335 | 0.0616 | 0.5183 |
| Hybrid α=0.4 | 0.0059 | 0.0587 | 0.0322 | 0.0593 | 0.6179 |
| Hybrid α=0.5 | 0.0055 | 0.0541 | 0.0299 | 0.0547 | 0.7393 |
| Hybrid α=0.6 | 0.0048 | 0.0469 | 0.0266 | 0.0476 | 0.8296 |
| Hybrid α=0.7 | 0.0041 | 0.0402 | 0.0227 | 0.0407 | 0.8710 |
| Hybrid α=0.8 | 0.0034 | 0.0339 | 0.0197 | 0.0343 | 0.8858 |
| Hybrid α=0.9 | 0.0032 | 0.0311 | 0.0183 | 0.0316 | 0.8894 |
| CBF (α=1) | 0.0012 | 0.0115 | 0.0056 | 0.0117 | 0.7590 |

Nửa đời giảm trọng số hành vi cũ (α đã chọn):

| Nửa đời | NDCG |
|---|---|
| không giảm | 0.0341 |
| 30 ngày | 0.0224 |
| 90 ngày | 0.0232 |

MMR (α và nửa đời đã chọn; chọn λ đa dạng nhất mà NDCG ≥ 95% của λ = 1):

| λ | NDCG@10 | Diversity |
|---|---|---|
| 1.0 | 0.0341 | 0.5953 |
| 0.9 | 0.0335 | 0.6526 |
| 0.8 | 0.0321 | 0.7150 |
| 0.7 | 0.0301 | 0.7712 |
| 0.6 | 0.0277 | 0.8061 |
| 0.5 | 0.0255 | 0.8246 |

Ngưỡng cold-start (chọn k = m nhỏ nhất mà từ đó Hybrid ≥ CBF; được k = 2):

| Số lượt m | Người dùng | Phổ biến | Chỉ CBF | Hybrid |
|---|---|---|---|---|
| 1 | 27 | 0.0088 | 0.0174 | 0.0000 |
| 2 | 27 | 0.0088 | 0.0068 | 0.0174 |
| 3 | 27 | 0.0088 | 0.0000 | 0.0197 |
| 5 | 27 | 0.0088 | 0.0066 | 0.0174 |
| 8 | 27 | 0.0088 | 0.0000 | 0.0067 |
| 12 | 27 | 0.0088 | 0.0076 | 0.0000 |

Hệ thống đang dùng: α = 0.6, ALS 16 chiều, λ = 1.0, a = 5.0, nửa đời 90.0, MMR λ = 0.7, k = 2.
