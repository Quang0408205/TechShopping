# Đánh giá offline hệ thống khuyến nghị (2026-11-28 18:51:05)

- Chia theo thời gian từng người dùng: 70% hành vi đầu để huấn luyện, 10% tiếp theo để tinh chỉnh (validation), 20% cuối để kiểm thử (16644 / 2508 / 5831 lượt).
- Tham số chọn trên validation (354 người dùng): ALS 32 chiều, λ = 0.1, a = 5.0; α = 0.4; nửa đời giảm trọng số 90 ngày; MMR λ = 0.8; ngưỡng cold-start k = 1.
- Kiểm thử: học lại trên huấn luyện + validation, chấm 548 người dùng có ít nhất 2 lượt tương tác và có sản phẩm mới trong phần kiểm thử; 4302 sản phẩm đang bán. Khoảng tin cậy 95%: bootstrap 1000 lần theo người dùng.

## Kiểm thử (đúng = mọi tương tác)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 | Coverage | Diversity |
|---|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0075 | 0.0436 | 0.0232 (0.0162–0.0311) | 0.0748 | 0.0040 | 0.6387 |
| Item-kNN (baseline) | 0.0254 | 0.0785 | 0.0596 (0.0474–0.0723) | 0.1898 | 0.2494 | 0.6244 |
| CBF (α=1) | 0.0259 | 0.0861 | 0.0656 (0.0540–0.0790) | 0.2117 | 0.2429 | 0.1718 |
| CF/ALS (α=0) | 0.0305 | 0.0940 | 0.0743 (0.0605–0.0880) | 0.2172 | 0.1260 | 0.6289 |
| Hybrid α=0.4 | 0.0358 | 0.1128 | 0.0883 (0.0728–0.1036) | 0.2536 | 0.1648 | 0.4305 |
| Hybrid α=0.6 | 0.0307 | 0.0972 | 0.0809 (0.0656–0.0969) | 0.2281 | 0.2322 | 0.3127 |
| Hybrid α=0.4 + MMR λ=0.8 | 0.0318 | 0.1013 | 0.0823 (0.0676–0.0975) | 0.2354 | 0.1618 | 0.5482 |
| Hybrid α=0.6 + MMR λ=0.7 | 0.0308 | 0.0978 | 0.0802 (0.0656–0.0957) | 0.2336 | 0.1892 | 0.5145 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.4 − CF/ALS (α=0): +0.0141 (+0.0070 đến +0.0206), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − CBF (α=1): +0.0227 (+0.0121 đến +0.0339), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − Item-kNN (baseline): +0.0287 (+0.0189 đến +0.0392), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − Phổ biến (baseline): +0.0651 (+0.0475 đến +0.0821), có ý nghĩa

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.4 + MMR λ=0.8 − Hybrid α=0.4: -0.0060 (-0.0091 đến -0.0028), có ý nghĩa
- Diversity: Hybrid α=0.4 + MMR λ=0.8 − Hybrid α=0.4: +0.1177 (+0.1099 đến +0.1253), có ý nghĩa

## Kiểm thử (đúng = chỉ thêm giỏ / mua, 399 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0055 | 0.0472 | 0.0233 (0.0141–0.0339) | 0.0551 |
| Item-kNN (baseline) | 0.0080 | 0.0704 | 0.0452 (0.0301–0.0626) | 0.0802 |
| CBF (α=1) | 0.0100 | 0.0850 | 0.0550 (0.0390–0.0735) | 0.1003 |
| CF/ALS (α=0) | 0.0103 | 0.0904 | 0.0492 (0.0349–0.0669) | 0.1028 |
| Hybrid α=0.4 | 0.0128 | 0.1119 | 0.0592 (0.0439–0.0779) | 0.1253 |
| Hybrid α=0.6 | 0.0103 | 0.0856 | 0.0515 (0.0374–0.0687) | 0.1003 |
| Hybrid α=0.4 + MMR λ=0.8 | 0.0113 | 0.0988 | 0.0550 (0.0393–0.0728) | 0.1128 |
| Hybrid α=0.6 + MMR λ=0.7 | 0.0108 | 0.0925 | 0.0522 (0.0370–0.0696) | 0.1078 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.4 − CF/ALS (α=0): +0.0100 (+0.0026 đến +0.0196), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − CBF (α=1): +0.0042 (-0.0077 đến +0.0173), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.4 − Item-kNN (baseline): +0.0140 (+0.0027 đến +0.0269), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − Phổ biến (baseline): +0.0359 (+0.0183 đến +0.0565), có ý nghĩa

## Khách mới (1–1 lượt tương tác, 171 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0053 | 0.0497 | 0.0181 (0.0071–0.0310) | 0.0526 |
| CBF (khách mới) | 0.0029 | 0.0292 | 0.0174 (0.0034–0.0350) | 0.0292 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: CBF (khách mới) − Phổ biến (baseline): -0.0006 (-0.0197 đến +0.0211), chưa đủ chắc chắn

## Cold-start: NDCG@10 theo số lượt của khách mới (kiểm thử)

Cắt lịch sử của 30% khách còn m lượt đầu rồi học lại; Hybrid được dùng ALS ngay từ lượt đầu để so với chỉ CBF.

| Số lượt m | Người dùng | Phổ biến | Chỉ CBF | Hybrid |
|---|---|---|---|---|
| 1 | 91 | 0.0368 | 0.1407 | 0.1581 |
| 2 | 91 | 0.0358 | 0.1599 | 0.1818 |
| 3 | 91 | 0.0359 | 0.1623 | 0.1879 |
| 5 | 90 | 0.0343 | 0.1641 | 0.2113 |
| 8 | 88 | 0.0276 | 0.1652 | 0.2219 |
| 12 | 88 | 0.0275 | 0.1635 | 0.2085 |

## Danh sách yêu thích (LIKE): có so với bỏ (2 lượt LIKE trong phần học)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Có LIKE (mọi tương tác) | 0.0318 | 0.1013 | 0.0823 (0.0673–0.0974) | 0.2354 |
| Bỏ LIKE (mọi tương tác) | 0.0318 | 0.1001 | 0.0822 (0.0678–0.0967) | 0.2354 |
| Có LIKE (thêm giỏ / mua) | 0.0113 | 0.0988 | 0.0550 (0.0385–0.0721) | 0.1128 |
| Bỏ LIKE (thêm giỏ / mua) | 0.0110 | 0.0963 | 0.0545 (0.0380–0.0714) | 0.1103 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Có LIKE (mọi tương tác) − Bỏ LIKE (mọi tương tác): +0.0001 (-0.0011 đến +0.0015), chưa đủ chắc chắn
- NDCG@10: Có LIKE (thêm giỏ / mua) − Bỏ LIKE (thêm giỏ / mua): +0.0006 (-0.0005 đến +0.0022), chưa đủ chắc chắn

## Mua kèm (638 đơn kiểm thử, 199 cặp đơn – sản phẩm)

Luật cùng đơn học trên 2514 đơn đặt trước; luật "khách mua A cũng mua B" học từ hành vi trước ngày cắt (111 sản phẩm có luật); 51% sản phẩm trong đơn kiểm thử có danh sách mua kèm.

| Cách gợi ý | HitRate@4 | Precision@4 | Có gợi ý |
|---|---|---|---|
| Mua kèm (đang dùng: cùng đơn + khách mua A cũng mua B + phụ kiện) | 0.0302 | 0.0088 | 51% |
| Chỉ luật cùng đơn (cách cũ) | 0.0251 | 0.0075 | 42% |
| Chỉ khách mua A cũng mua B | 0.0302 | 0.0088 | 49% |
| Bán chạy nhất (baseline) | 0.0603 | 0.0151 | 100% |

- HitRate@4: Mua kèm (đang dùng: cùng đơn + khách mua A cũng mua B + phụ kiện) − Bán chạy nhất (baseline): -0.0302 (KTC 95%: -0.0704 đến +0.0101), chưa đủ chắc chắn
- HitRate@4: Mua kèm (đang dùng: cùng đơn + khách mua A cũng mua B + phụ kiện) − Chỉ luật cùng đơn (cách cũ): +0.0050 (KTC 95%: +0.0000 đến +0.0201), chưa đủ chắc chắn

## Nâng cấp

98% trong 4302 sản phẩm đang bán có gợi ý nâng cấp; gợi ý đầu tiên đắt hơn trung bình 10%.

## Tinh chỉnh trên validation

Tham số ALS (chỉ ALS, NDCG@10):

| Số chiều | λ | a | NDCG |
|---|---|---|---|
| 32 | 0.1 | 5.0 | 0.1001 |
| 32 | 1.0 | 5.0 | 0.0995 |
| 32 | 1.0 | 10.0 | 0.0865 |
| 64 | 0.1 | 5.0 | 0.0863 |
| 64 | 1.0 | 5.0 | 0.0860 |
| 32 | 0.1 | 10.0 | 0.0850 |
| 64 | 0.1 | 10.0 | 0.0815 |
| 16 | 0.1 | 5.0 | 0.0788 |
| 64 | 1.0 | 10.0 | 0.0784 |
| 16 | 1.0 | 5.0 | 0.0777 |
| 64 | 0.1 | 20.0 | 0.0725 |
| 32 | 0.1 | 20.0 | 0.0674 |
| 32 | 1.0 | 20.0 | 0.0674 |
| 64 | 1.0 | 20.0 | 0.0651 |
| 16 | 1.0 | 10.0 | 0.0637 |
| 16 | 0.1 | 10.0 | 0.0634 |
| 16 | 0.1 | 20.0 | 0.0456 |
| 16 | 1.0 | 20.0 | 0.0440 |

| Mô hình | Precision@10 | Recall@10 | NDCG@10 | HitRate@10 | Coverage |
|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0045 | 0.0216 | 0.0114 | 0.0452 | 0.0037 |
| CF/ALS (α=0) | 0.0271 | 0.1441 | 0.1001 | 0.2486 | 0.1248 |
| Hybrid α=0.1 | 0.0302 | 0.1576 | 0.1082 | 0.2684 | 0.1285 |
| Hybrid α=0.2 | 0.0333 | 0.1709 | 0.1166 | 0.2910 | 0.1364 |
| Hybrid α=0.3 | 0.0328 | 0.1689 | 0.1168 | 0.2853 | 0.1471 |
| Hybrid α=0.4 | 0.0347 | 0.1815 | 0.1254 | 0.3079 | 0.1616 |
| Hybrid α=0.5 | 0.0328 | 0.1671 | 0.1193 | 0.2938 | 0.1885 |
| Hybrid α=0.6 | 0.0316 | 0.1622 | 0.1152 | 0.2825 | 0.2143 |
| Hybrid α=0.7 | 0.0297 | 0.1508 | 0.1089 | 0.2655 | 0.2297 |
| Hybrid α=0.8 | 0.0271 | 0.1392 | 0.1039 | 0.2429 | 0.2341 |
| Hybrid α=0.9 | 0.0260 | 0.1360 | 0.0962 | 0.2345 | 0.2373 |
| CBF (α=1) | 0.0237 | 0.1214 | 0.0842 | 0.2090 | 0.2285 |

Nửa đời giảm trọng số hành vi cũ (α đã chọn):

| Nửa đời | NDCG |
|---|---|
| không giảm | 0.1254 |
| 30 ngày | 0.1262 |
| 90 ngày | 0.1302 |

MMR (α và nửa đời đã chọn; chọn λ đa dạng nhất mà NDCG ≥ 95% của λ = 1):

| λ | NDCG@10 | Diversity |
|---|---|---|
| 1.0 | 0.1302 | 0.4181 |
| 0.9 | 0.1276 | 0.4695 |
| 0.8 | 0.1254 | 0.5395 |
| 0.7 | 0.1143 | 0.6314 |
| 0.6 | 0.1021 | 0.7349 |
| 0.5 | 0.0898 | 0.7970 |

Ngưỡng cold-start (chọn k = m nhỏ nhất mà từ đó Hybrid ≥ CBF; được k = 1):

| Số lượt m | Người dùng | Phổ biến | Chỉ CBF | Hybrid |
|---|---|---|---|---|
| 1 | 88 | 0.0320 | 0.1105 | 0.1219 |
| 2 | 88 | 0.0235 | 0.1212 | 0.1263 |
| 3 | 88 | 0.0237 | 0.1218 | 0.1638 |
| 5 | 88 | 0.0236 | 0.1361 | 0.1896 |
| 8 | 88 | 0.0228 | 0.1267 | 0.2022 |
| 12 | 88 | 0.0186 | 0.1495 | 0.2018 |

Hệ thống đang dùng: α = 0.6, ALS 16 chiều, λ = 1.0, a = 5.0, nửa đời 90.0, MMR λ = 0.7, k = 2.
