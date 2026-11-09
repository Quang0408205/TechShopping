# Đánh giá offline hệ thống khuyến nghị (2026-11-09 13:29:40)

- Chia theo thời gian từng người dùng: 70% hành vi đầu để huấn luyện, 10% tiếp theo để tinh chỉnh (validation), 20% cuối để kiểm thử (16429 / 2456 / 5713 lượt).
- Tham số chọn trên validation (347 người dùng): ALS 16 chiều, λ = 1.0, a = 5.0; α = 0.6; nửa đời giảm trọng số 90 ngày; MMR λ = 0.7; ngưỡng cold-start k = 2.
- Kiểm thử: học lại trên huấn luyện + validation, chấm 499 người dùng có ít nhất 2 lượt tương tác và có sản phẩm mới trong phần kiểm thử; 4302 sản phẩm đang bán. Khoảng tin cậy 95%: bootstrap 1000 lần theo người dùng.

## Kiểm thử (đúng = mọi tương tác)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 | Coverage | Diversity |
|---|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0066 | 0.0292 | 0.0174 (0.0112–0.0249) | 0.0621 | 0.0037 | 0.7198 |
| Item-kNN (baseline) | 0.0220 | 0.0689 | 0.0528 (0.0406–0.0661) | 0.1764 | 0.2836 | 0.6240 |
| CBF (α=1) | 0.0327 | 0.1034 | 0.0789 (0.0652–0.0941) | 0.2485 | 0.2736 | 0.1663 |
| CF/ALS (α=0) | 0.0267 | 0.0889 | 0.0609 (0.0494–0.0743) | 0.2084 | 0.0883 | 0.6129 |
| Hybrid α=0.6 | 0.0345 | 0.1065 | 0.0853 (0.0709–0.1006) | 0.2645 | 0.2383 | 0.2961 |
| Hybrid α=0.6 + MMR λ=0.7 | 0.0313 | 0.0984 | 0.0769 (0.0635–0.0922) | 0.2445 | 0.1736 | 0.5180 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.6 − CF/ALS (α=0): +0.0244 (+0.0148 đến +0.0340), có ý nghĩa
- NDCG@10: Hybrid α=0.6 − CBF (α=1): +0.0065 (-0.0042 đến +0.0166), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.6 − Item-kNN (baseline): +0.0325 (+0.0205 đến +0.0450), có ý nghĩa
- NDCG@10: Hybrid α=0.6 − Phổ biến (baseline): +0.0679 (+0.0515 đến +0.0841), có ý nghĩa

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.6 + MMR λ=0.7 − Hybrid α=0.6: -0.0085 (-0.0135 đến -0.0033), có ý nghĩa
- Diversity: Hybrid α=0.6 + MMR λ=0.7 − Hybrid α=0.6: +0.2218 (+0.2087 đến +0.2352), có ý nghĩa

## Kiểm thử (đúng = chỉ thêm giỏ / mua, 385 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0039 | 0.0249 | 0.0135 (0.0064–0.0223) | 0.0390 |
| Item-kNN (baseline) | 0.0075 | 0.0470 | 0.0252 (0.0152–0.0372) | 0.0675 |
| CBF (α=1) | 0.0096 | 0.0716 | 0.0386 (0.0249–0.0539) | 0.0961 |
| CF/ALS (α=0) | 0.0104 | 0.0593 | 0.0328 (0.0218–0.0455) | 0.0935 |
| Hybrid α=0.6 | 0.0117 | 0.0671 | 0.0417 (0.0283–0.0565) | 0.1065 |
| Hybrid α=0.6 + MMR λ=0.7 | 0.0104 | 0.0636 | 0.0362 (0.0246–0.0503) | 0.0987 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.6 − CF/ALS (α=0): +0.0089 (-0.0033 đến +0.0215), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.6 − CBF (α=1): +0.0031 (-0.0070 đến +0.0127), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.6 − Item-kNN (baseline): +0.0164 (+0.0044 đến +0.0299), có ý nghĩa
- NDCG@10: Hybrid α=0.6 − Phổ biến (baseline): +0.0282 (+0.0131 đến +0.0445), có ý nghĩa

## Khách mới (1–1 lượt tương tác, 136 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0074 | 0.0699 | 0.0271 (0.0123–0.0457) | 0.0735 |
| CBF (khách mới) | 0.0015 | 0.0110 | 0.0054 (0.0000–0.0144) | 0.0147 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: CBF (khách mới) − Phổ biến (baseline): -0.0217 (-0.0418 đến -0.0045), có ý nghĩa

## Cold-start: NDCG@10 theo số lượt của khách mới (kiểm thử)

Cắt lịch sử của 30% khách còn m lượt đầu rồi học lại; Hybrid được dùng ALS ngay từ lượt đầu để so với chỉ CBF.

| Số lượt m | Người dùng | Phổ biến | Chỉ CBF | Hybrid |
|---|---|---|---|---|
| 1 | 91 | 0.0486 | 0.0649 | 0.0707 |
| 2 | 91 | 0.0496 | 0.0822 | 0.0927 |
| 3 | 91 | 0.0444 | 0.0927 | 0.1095 |
| 5 | 90 | 0.0444 | 0.1085 | 0.1265 |
| 8 | 90 | 0.0414 | 0.1198 | 0.1473 |
| 12 | 90 | 0.0338 | 0.1176 | 0.1467 |

## Danh sách yêu thích (LIKE): có so với bỏ (709 lượt LIKE trong phần học)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Có LIKE (mọi tương tác) | 0.0313 | 0.0984 | 0.0769 (0.0646–0.0919) | 0.2445 |
| Bỏ LIKE (mọi tương tác) | 0.0317 | 0.0980 | 0.0742 (0.0620–0.0891) | 0.2425 |
| Có LIKE (thêm giỏ / mua) | 0.0104 | 0.0636 | 0.0362 (0.0234–0.0501) | 0.0987 |
| Bỏ LIKE (thêm giỏ / mua) | 0.0109 | 0.0645 | 0.0362 (0.0239–0.0496) | 0.1013 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Có LIKE (mọi tương tác) − Bỏ LIKE (mọi tương tác): +0.0027 (-0.0006 đến +0.0065), chưa đủ chắc chắn
- NDCG@10: Có LIKE (thêm giỏ / mua) − Bỏ LIKE (thêm giỏ / mua): +0.0000 (-0.0032 đến +0.0034), chưa đủ chắc chắn

## Mua kèm (625 đơn kiểm thử, 276 cặp đơn – sản phẩm)

Luật cùng đơn học trên 2500 đơn đặt trước; luật "khách mua A cũng mua B" học từ hành vi trước ngày cắt (103 sản phẩm có luật); 61% sản phẩm trong đơn kiểm thử có danh sách mua kèm.

| Cách gợi ý | HitRate@4 | Precision@4 | Có gợi ý |
|---|---|---|---|
| Mua kèm (đang dùng: cùng đơn + khách mua A cũng mua B + phụ kiện) | 0.0217 | 0.0054 | 61% |
| Chỉ luật cùng đơn (cách cũ) | 0.0217 | 0.0054 | 44% |
| Chỉ khách mua A cũng mua B | 0.0217 | 0.0054 | 59% |
| Bán chạy nhất (baseline) | 0.0217 | 0.0054 | 100% |

- HitRate@4: Mua kèm (đang dùng: cùng đơn + khách mua A cũng mua B + phụ kiện) − Bán chạy nhất (baseline): +0.0000 (KTC 95%: -0.0217 đến +0.0218), chưa đủ chắc chắn
- HitRate@4: Mua kèm (đang dùng: cùng đơn + khách mua A cũng mua B + phụ kiện) − Chỉ luật cùng đơn (cách cũ): +0.0000 (KTC 95%: +0.0000 đến +0.0000), chưa đủ chắc chắn

## Nâng cấp

98% trong 4302 sản phẩm đang bán có gợi ý nâng cấp; gợi ý đầu tiên đắt hơn trung bình 10%.

## Tinh chỉnh trên validation

Tham số ALS (chỉ ALS, NDCG@10):

| Số chiều | λ | a | NDCG |
|---|---|---|---|
| 16 | 1.0 | 5.0 | 0.0827 |
| 16 | 0.1 | 5.0 | 0.0821 |
| 32 | 1.0 | 5.0 | 0.0775 |
| 64 | 0.1 | 5.0 | 0.0775 |
| 64 | 1.0 | 5.0 | 0.0747 |
| 32 | 1.0 | 10.0 | 0.0743 |
| 32 | 0.1 | 5.0 | 0.0738 |
| 32 | 0.1 | 10.0 | 0.0726 |
| 16 | 0.1 | 10.0 | 0.0683 |
| 64 | 0.1 | 10.0 | 0.0679 |
| 32 | 0.1 | 20.0 | 0.0677 |
| 64 | 1.0 | 10.0 | 0.0658 |
| 32 | 1.0 | 20.0 | 0.0645 |
| 16 | 1.0 | 10.0 | 0.0642 |
| 64 | 0.1 | 20.0 | 0.0627 |
| 64 | 1.0 | 20.0 | 0.0603 |
| 16 | 0.1 | 20.0 | 0.0508 |
| 16 | 1.0 | 20.0 | 0.0466 |

| Mô hình | Precision@10 | Recall@10 | NDCG@10 | HitRate@10 | Coverage |
|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0052 | 0.0365 | 0.0186 | 0.0519 | 0.0037 |
| CF/ALS (α=0) | 0.0233 | 0.1460 | 0.0827 | 0.2017 | 0.0844 |
| Hybrid α=0.1 | 0.0245 | 0.1500 | 0.0898 | 0.2104 | 0.0925 |
| Hybrid α=0.2 | 0.0239 | 0.1472 | 0.0893 | 0.2075 | 0.1048 |
| Hybrid α=0.3 | 0.0256 | 0.1541 | 0.0945 | 0.2219 | 0.1197 |
| Hybrid α=0.4 | 0.0274 | 0.1657 | 0.1029 | 0.2392 | 0.1434 |
| Hybrid α=0.5 | 0.0259 | 0.1519 | 0.1034 | 0.2305 | 0.1834 |
| Hybrid α=0.6 | 0.0256 | 0.1535 | 0.1038 | 0.2277 | 0.2157 |
| Hybrid α=0.7 | 0.0251 | 0.1548 | 0.1026 | 0.2305 | 0.2345 |
| Hybrid α=0.8 | 0.0242 | 0.1494 | 0.1015 | 0.2248 | 0.2448 |
| Hybrid α=0.9 | 0.0233 | 0.1454 | 0.0960 | 0.2133 | 0.2483 |
| CBF (α=1) | 0.0225 | 0.1374 | 0.0793 | 0.2075 | 0.2315 |

Nửa đời giảm trọng số hành vi cũ (α đã chọn):

| Nửa đời | NDCG |
|---|---|
| không giảm | 0.1038 |
| 30 ngày | 0.1067 |
| 90 ngày | 0.1082 |

MMR (α và nửa đời đã chọn; chọn λ đa dạng nhất mà NDCG ≥ 95% của λ = 1):

| λ | NDCG@10 | Diversity |
|---|---|---|
| 1.0 | 0.1082 | 0.2824 |
| 0.9 | 0.1048 | 0.3295 |
| 0.8 | 0.1076 | 0.4067 |
| 0.7 | 0.1032 | 0.5142 |
| 0.6 | 0.0932 | 0.6450 |
| 0.5 | 0.0776 | 0.7554 |

Ngưỡng cold-start (chọn k = m nhỏ nhất mà từ đó Hybrid ≥ CBF; được k = 2):

| Số lượt m | Người dùng | Phổ biến | Chỉ CBF | Hybrid |
|---|---|---|---|---|
| 1 | 90 | 0.0515 | 0.0838 | 0.0816 |
| 2 | 89 | 0.0439 | 0.0792 | 0.0876 |
| 3 | 88 | 0.0420 | 0.0918 | 0.1028 |
| 5 | 88 | 0.0375 | 0.0982 | 0.1122 |
| 8 | 87 | 0.0389 | 0.0935 | 0.1332 |
| 12 | 84 | 0.0355 | 0.0942 | 0.1284 |

Hệ thống đang dùng: α = 0.6, ALS 16 chiều, λ = 1.0, a = 5.0, nửa đời 90.0, MMR λ = 0.7, k = 2.
