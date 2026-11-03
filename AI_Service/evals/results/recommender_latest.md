# Đánh giá offline hệ thống khuyến nghị (2026-11-03 14:04:26)

- Chia theo thời gian từng người dùng: 70% hành vi đầu để huấn luyện, 10% tiếp theo để tinh chỉnh (validation), 20% cuối để kiểm thử (14726 / 2230 / 4335 lượt).
- Tham số chọn trên validation (256 người dùng): ALS 32 chiều, λ = 0.1, a = 5.0; α = 0.5; nửa đời giảm trọng số 30 ngày; MMR λ = 0.8.
- Kiểm thử: học lại trên huấn luyện + validation, chấm 288 người dùng có ít nhất 5 lượt tương tác và có sản phẩm mới trong phần kiểm thử; 4302 sản phẩm đang bán. Khoảng tin cậy 95%: bootstrap 1000 lần theo người dùng.

## Kiểm thử (đúng = mọi tương tác)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 | Coverage | Diversity |
|---|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0063 | 0.0183 | 0.0122 (0.0069–0.0185) | 0.0625 | 0.0040 | 0.6657 |
| Item-kNN (baseline) | 0.0465 | 0.1412 | 0.1130 (0.0897–0.1376) | 0.3438 | 0.2015 | 0.6317 |
| CBF (α=1) | 0.0462 | 0.1314 | 0.0987 (0.0793–0.1197) | 0.3403 | 0.1806 | 0.1748 |
| CF/ALS (α=0) | 0.0635 | 0.1871 | 0.1536 (0.1288–0.1812) | 0.4167 | 0.1213 | 0.5612 |
| Hybrid α=0.5 | 0.0674 | 0.1976 | 0.1609 (0.1349–0.1890) | 0.4514 | 0.1409 | 0.3625 |
| Hybrid α=0.5 + MMR λ=0.8 | 0.0670 | 0.1979 | 0.1633 (0.1370–0.1911) | 0.4479 | 0.1330 | 0.4517 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.5 − CF/ALS (α=0): +0.0073 (-0.0044 đến +0.0193), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.5 − CBF (α=1): +0.0622 (+0.0419 đến +0.0846), có ý nghĩa
- NDCG@10: Hybrid α=0.5 − Item-kNN (baseline): +0.0479 (+0.0299 đến +0.0665), có ý nghĩa
- NDCG@10: Hybrid α=0.5 − Phổ biến (baseline): +0.1486 (+0.1234 đến +0.1747), có ý nghĩa

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.5 + MMR λ=0.8 − Hybrid α=0.5: +0.0024 (-0.0037 đến +0.0087), chưa đủ chắc chắn
- Diversity: Hybrid α=0.5 + MMR λ=0.8 − Hybrid α=0.5: +0.0892 (+0.0803 đến +0.0981), có ý nghĩa

## Kiểm thử (đúng = chỉ thêm giỏ / mua, 138 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0022 | 0.0145 | 0.0068 (0.0000–0.0148) | 0.0217 |
| Item-kNN (baseline) | 0.0174 | 0.1582 | 0.0984 (0.0608–0.1444) | 0.1739 |
| CBF (α=1) | 0.0254 | 0.2065 | 0.1262 (0.0859–0.1713) | 0.2464 |
| CF/ALS (α=0) | 0.0261 | 0.2234 | 0.1301 (0.0888–0.1752) | 0.2464 |
| Hybrid α=0.5 | 0.0275 | 0.2319 | 0.1474 (0.1042–0.1945) | 0.2609 |
| Hybrid α=0.5 + MMR λ=0.8 | 0.0275 | 0.2331 | 0.1457 (0.1032–0.1933) | 0.2536 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.5 − CF/ALS (α=0): +0.0173 (-0.0020 đến +0.0380), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.5 − CBF (α=1): +0.0213 (-0.0108 đến +0.0524), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.5 − Item-kNN (baseline): +0.0490 (+0.0197 đến +0.0808), có ý nghĩa
- NDCG@10: Hybrid α=0.5 − Phổ biến (baseline): +0.1406 (+0.1006 đến +0.1845), có ý nghĩa

## Khách mới (1–4 lượt tương tác, 49 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0000 | 0.0000 | 0.0000 (0.0000–0.0000) | 0.0000 |
| CBF (khách mới) | 0.0000 | 0.0000 | 0.0000 (0.0000–0.0000) | 0.0000 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: CBF (khách mới) − Phổ biến (baseline): +0.0000 (+0.0000 đến +0.0000), chưa đủ chắc chắn

## Mua kèm (625 đơn kiểm thử, 276 cặp đơn – sản phẩm)

Luật kết hợp học trên 2499 đơn đặt trước; 43% sản phẩm trong đơn kiểm thử có danh sách mua kèm.

| Cách gợi ý | HitRate@4 | Precision@4 |
|---|---|---|
| Mua kèm (luật kết hợp + phụ kiện) | 0.0181 | 0.0045 |
| Bán chạy nhất (baseline) | 0.0217 | 0.0054 |

## Nâng cấp

98% trong 4302 sản phẩm đang bán có gợi ý nâng cấp; gợi ý đầu tiên đắt hơn trung bình 10%.

## Tinh chỉnh trên validation

Tham số ALS (chỉ ALS, NDCG@10):

| Số chiều | λ | a | NDCG |
|---|---|---|---|
| 32 | 0.1 | 5.0 | 0.1267 |
| 32 | 1.0 | 5.0 | 0.1241 |
| 32 | 0.1 | 10.0 | 0.1218 |
| 32 | 1.0 | 10.0 | 0.1200 |
| 64 | 1.0 | 5.0 | 0.1151 |
| 32 | 0.1 | 20.0 | 0.1124 |
| 64 | 0.1 | 5.0 | 0.1123 |
| 16 | 0.1 | 5.0 | 0.1102 |
| 32 | 1.0 | 20.0 | 0.1091 |
| 16 | 1.0 | 5.0 | 0.1090 |
| 64 | 1.0 | 10.0 | 0.1065 |
| 64 | 0.1 | 10.0 | 0.1063 |
| 64 | 1.0 | 20.0 | 0.1053 |
| 64 | 0.1 | 20.0 | 0.1017 |
| 16 | 1.0 | 10.0 | 0.0993 |
| 16 | 0.1 | 10.0 | 0.0992 |
| 16 | 0.1 | 20.0 | 0.0811 |
| 16 | 1.0 | 20.0 | 0.0789 |

| Mô hình | Precision@10 | Recall@10 | NDCG@10 | HitRate@10 | Coverage |
|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0043 | 0.0167 | 0.0124 | 0.0430 | 0.0037 |
| CF/ALS (α=0) | 0.0387 | 0.2027 | 0.1267 | 0.3359 | 0.1376 |
| Hybrid α=0.1 | 0.0383 | 0.1992 | 0.1290 | 0.3320 | 0.1355 |
| Hybrid α=0.2 | 0.0406 | 0.2100 | 0.1374 | 0.3477 | 0.1392 |
| Hybrid α=0.3 | 0.0410 | 0.2102 | 0.1393 | 0.3477 | 0.1409 |
| Hybrid α=0.4 | 0.0402 | 0.2064 | 0.1353 | 0.3398 | 0.1427 |
| Hybrid α=0.5 | 0.0414 | 0.2115 | 0.1399 | 0.3438 | 0.1506 |
| Hybrid α=0.6 | 0.0402 | 0.2005 | 0.1349 | 0.3320 | 0.1623 |
| Hybrid α=0.7 | 0.0391 | 0.1949 | 0.1350 | 0.3281 | 0.1715 |
| Hybrid α=0.8 | 0.0359 | 0.1786 | 0.1271 | 0.3008 | 0.1801 |
| Hybrid α=0.9 | 0.0336 | 0.1639 | 0.1158 | 0.2812 | 0.1855 |
| CBF (α=1) | 0.0281 | 0.1358 | 0.0976 | 0.2461 | 0.1736 |

Nửa đời giảm trọng số hành vi cũ (α đã chọn):

| Nửa đời | NDCG |
|---|---|
| không giảm | 0.1399 |
| 30 ngày | 0.1466 |
| 90 ngày | 0.1394 |

MMR (α và nửa đời đã chọn; chọn λ đa dạng nhất mà NDCG ≥ 95% của λ = 1):

| λ | NDCG@10 | Diversity |
|---|---|---|
| 1.0 | 0.1466 | 0.3537 |
| 0.9 | 0.1485 | 0.3921 |
| 0.8 | 0.1452 | 0.4464 |
| 0.7 | 0.1380 | 0.5209 |
| 0.6 | 0.1251 | 0.6549 |
| 0.5 | 0.1022 | 0.7589 |

Hệ thống đang dùng: α = 0.5, ALS 32 chiều, λ = 0.1, a = 5.0, nửa đời 30.0, MMR λ = 0.8.
