# Đánh giá offline hệ thống khuyến nghị (2026-11-27 01:24:02)

- Chia theo thời gian từng người dùng: 70% hành vi đầu để huấn luyện, 10% tiếp theo để tinh chỉnh (validation), 20% cuối để kiểm thử (16643 / 2508 / 5831 lượt).
- Tham số chọn trên validation (354 người dùng): ALS 32 chiều, λ = 0.1, a = 5.0; α = 0.4; nửa đời giảm trọng số 90 ngày; MMR λ = 0.8; ngưỡng cold-start k = 1.
- Kiểm thử: học lại trên huấn luyện + validation, chấm 548 người dùng có ít nhất 2 lượt tương tác và có sản phẩm mới trong phần kiểm thử; 4302 sản phẩm đang bán. Khoảng tin cậy 95%: bootstrap 1000 lần theo người dùng.

## Kiểm thử (đúng = mọi tương tác)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 | Coverage | Diversity |
|---|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0075 | 0.0436 | 0.0231 (0.0161–0.0308) | 0.0748 | 0.0040 | 0.6387 |
| Item-kNN (baseline) | 0.0254 | 0.0785 | 0.0596 (0.0474–0.0723) | 0.1898 | 0.2494 | 0.6241 |
| CBF (α=1) | 0.0259 | 0.0861 | 0.0656 (0.0540–0.0790) | 0.2117 | 0.2429 | 0.1716 |
| CF/ALS (α=0) | 0.0307 | 0.0945 | 0.0742 (0.0607–0.0881) | 0.2190 | 0.1258 | 0.6286 |
| Hybrid α=0.4 | 0.0356 | 0.1110 | 0.0878 (0.0723–0.1030) | 0.2500 | 0.1653 | 0.4305 |
| Hybrid α=0.6 | 0.0305 | 0.0969 | 0.0808 (0.0657–0.0966) | 0.2281 | 0.2327 | 0.3128 |
| Hybrid α=0.4 + MMR λ=0.8 | 0.0316 | 0.0995 | 0.0817 (0.0670–0.0969) | 0.2336 | 0.1625 | 0.5483 |
| Hybrid α=0.6 + MMR λ=0.7 | 0.0310 | 0.0965 | 0.0801 (0.0656–0.0958) | 0.2336 | 0.1906 | 0.5150 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.4 − CF/ALS (α=0): +0.0135 (+0.0065 đến +0.0199), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − CBF (α=1): +0.0221 (+0.0117 đến +0.0337), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − Item-kNN (baseline): +0.0281 (+0.0186 đến +0.0390), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − Phổ biến (baseline): +0.0647 (+0.0477 đến +0.0820), có ý nghĩa

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.4 + MMR λ=0.8 − Hybrid α=0.4: -0.0061 (-0.0092 đến -0.0028), có ý nghĩa
- Diversity: Hybrid α=0.4 + MMR λ=0.8 − Hybrid α=0.4: +0.1178 (+0.1099 đến +0.1253), có ý nghĩa

## Kiểm thử (đúng = chỉ thêm giỏ / mua, 400 người dùng)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Phổ biến (baseline) | 0.0058 | 0.0496 | 0.0243 (0.0146–0.0360) | 0.0575 |
| Item-kNN (baseline) | 0.0080 | 0.0702 | 0.0451 (0.0289–0.0624) | 0.0800 |
| CBF (α=1) | 0.0100 | 0.0848 | 0.0548 (0.0382–0.0739) | 0.1000 |
| CF/ALS (α=0) | 0.0105 | 0.0940 | 0.0502 (0.0348–0.0668) | 0.1050 |
| Hybrid α=0.4 | 0.0123 | 0.1079 | 0.0579 (0.0416–0.0764) | 0.1200 |
| Hybrid α=0.6 | 0.0102 | 0.0854 | 0.0513 (0.0353–0.0685) | 0.1000 |
| Hybrid α=0.4 + MMR λ=0.8 | 0.0110 | 0.0960 | 0.0541 (0.0377–0.0713) | 0.1100 |
| Hybrid α=0.6 + MMR λ=0.7 | 0.0105 | 0.0898 | 0.0513 (0.0354–0.0680) | 0.1050 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Hybrid α=0.4 − CF/ALS (α=0): +0.0077 (+0.0000 đến +0.0165), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.4 − CBF (α=1): +0.0030 (-0.0099 đến +0.0157), chưa đủ chắc chắn
- NDCG@10: Hybrid α=0.4 − Item-kNN (baseline): +0.0128 (+0.0008 đến +0.0258), có ý nghĩa
- NDCG@10: Hybrid α=0.4 − Phổ biến (baseline): +0.0336 (+0.0146 đến +0.0543), có ý nghĩa

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
| 1 | 91 | 0.0368 | 0.1407 | 0.1579 |
| 2 | 91 | 0.0358 | 0.1599 | 0.1817 |
| 3 | 91 | 0.0359 | 0.1623 | 0.1888 |
| 5 | 90 | 0.0343 | 0.1641 | 0.2121 |
| 8 | 88 | 0.0275 | 0.1652 | 0.2228 |
| 12 | 88 | 0.0276 | 0.1635 | 0.2100 |

## Danh sách yêu thích (LIKE): có so với bỏ (2 lượt LIKE trong phần học)

| Mô hình | Precision@10 | Recall@10 | NDCG@10 (KTC 95%) | HitRate@10 |
|---|---|---|---|---|
| Có LIKE (mọi tương tác) | 0.0316 | 0.0995 | 0.0817 (0.0669–0.0963) | 0.2336 |
| Bỏ LIKE (mọi tương tác) | 0.0316 | 0.0995 | 0.0818 (0.0672–0.0964) | 0.2336 |
| Có LIKE (thêm giỏ / mua) | 0.0110 | 0.0960 | 0.0541 (0.0377–0.0720) | 0.1100 |
| Bỏ LIKE (thêm giỏ / mua) | 0.0110 | 0.0960 | 0.0543 (0.0379–0.0722) | 0.1100 |

Chênh lệch trên cùng người dùng (KTC 95%; không chứa 0 = chênh lệch có ý nghĩa):

- NDCG@10: Có LIKE (mọi tương tác) − Bỏ LIKE (mọi tương tác): -0.0001 (-0.0002 đến +0.0000), chưa đủ chắc chắn
- NDCG@10: Có LIKE (thêm giỏ / mua) − Bỏ LIKE (thêm giỏ / mua): -0.0002 (-0.0006 đến +0.0000), chưa đủ chắc chắn

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
| 32 | 0.1 | 5.0 | 0.1012 |
| 32 | 1.0 | 5.0 | 0.1000 |
| 32 | 1.0 | 10.0 | 0.0871 |
| 32 | 0.1 | 10.0 | 0.0859 |
| 64 | 0.1 | 5.0 | 0.0859 |
| 64 | 1.0 | 5.0 | 0.0859 |
| 64 | 0.1 | 10.0 | 0.0799 |
| 16 | 0.1 | 5.0 | 0.0795 |
| 16 | 1.0 | 5.0 | 0.0778 |
| 64 | 1.0 | 10.0 | 0.0758 |
| 64 | 0.1 | 20.0 | 0.0712 |
| 32 | 1.0 | 20.0 | 0.0684 |
| 32 | 0.1 | 20.0 | 0.0679 |
| 64 | 1.0 | 20.0 | 0.0634 |
| 16 | 0.1 | 10.0 | 0.0628 |
| 16 | 1.0 | 10.0 | 0.0620 |
| 16 | 0.1 | 20.0 | 0.0450 |
| 16 | 1.0 | 20.0 | 0.0433 |

| Mô hình | Precision@10 | Recall@10 | NDCG@10 | HitRate@10 | Coverage |
|---|---|---|---|---|---|
| Phổ biến (baseline) | 0.0042 | 0.0188 | 0.0102 | 0.0424 | 0.0037 |
| CF/ALS (α=0) | 0.0274 | 0.1455 | 0.1012 | 0.2514 | 0.1246 |
| Hybrid α=0.1 | 0.0302 | 0.1557 | 0.1070 | 0.2684 | 0.1278 |
| Hybrid α=0.2 | 0.0333 | 0.1709 | 0.1172 | 0.2910 | 0.1369 |
| Hybrid α=0.3 | 0.0328 | 0.1689 | 0.1169 | 0.2853 | 0.1471 |
| Hybrid α=0.4 | 0.0347 | 0.1815 | 0.1251 | 0.3079 | 0.1629 |
| Hybrid α=0.5 | 0.0325 | 0.1662 | 0.1184 | 0.2910 | 0.1894 |
| Hybrid α=0.6 | 0.0316 | 0.1622 | 0.1149 | 0.2825 | 0.2157 |
| Hybrid α=0.7 | 0.0297 | 0.1508 | 0.1089 | 0.2655 | 0.2306 |
| Hybrid α=0.8 | 0.0271 | 0.1392 | 0.1036 | 0.2429 | 0.2355 |
| Hybrid α=0.9 | 0.0260 | 0.1360 | 0.0964 | 0.2345 | 0.2378 |
| CBF (α=1) | 0.0237 | 0.1214 | 0.0842 | 0.2090 | 0.2292 |

Nửa đời giảm trọng số hành vi cũ (α đã chọn):

| Nửa đời | NDCG |
|---|---|
| không giảm | 0.1251 |
| 30 ngày | 0.1267 |
| 90 ngày | 0.1285 |

MMR (α và nửa đời đã chọn; chọn λ đa dạng nhất mà NDCG ≥ 95% của λ = 1):

| λ | NDCG@10 | Diversity |
|---|---|---|
| 1.0 | 0.1285 | 0.4199 |
| 0.9 | 0.1272 | 0.4698 |
| 0.8 | 0.1246 | 0.5402 |
| 0.7 | 0.1128 | 0.6324 |
| 0.6 | 0.1018 | 0.7354 |
| 0.5 | 0.0885 | 0.7972 |

Ngưỡng cold-start (chọn k = m nhỏ nhất mà từ đó Hybrid ≥ CBF; được k = 1):

| Số lượt m | Người dùng | Phổ biến | Chỉ CBF | Hybrid |
|---|---|---|---|---|
| 1 | 88 | 0.0320 | 0.1105 | 0.1232 |
| 2 | 88 | 0.0235 | 0.1212 | 0.1265 |
| 3 | 88 | 0.0237 | 0.1218 | 0.1633 |
| 5 | 88 | 0.0236 | 0.1361 | 0.1907 |
| 8 | 88 | 0.0228 | 0.1267 | 0.2021 |
| 12 | 88 | 0.0186 | 0.1495 | 0.2021 |

Hệ thống đang dùng: α = 0.6, ALS 16 chiều, λ = 1.0, a = 5.0, nửa đời 90.0, MMR λ = 0.7, k = 2.
