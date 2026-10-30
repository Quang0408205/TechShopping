# Migration của database

Database Docker chỉ được tạo từ `database/techshopping.sql` **một lần** (khi volume còn trống). Khi cấu trúc đổi, database cũ cần chạy các file migration **theo đúng thứ tự dưới đây** (mỗi file chạy lại nhiều lần vẫn an toàn; file nào ghi "sao lưu" thì sao lưu database trước). Mỗi file chạy cho cả `techshopping` và `techshopping_test`.

| # | File | Nội dung |
|---|---|---|
| 1 | `orders_recipient.sql` | Đơn hàng: người nhận, địa chỉ |
| 2 | `promotions.sql` | Khuyến mãi |
| 3 | `payments.sql` | Thanh toán + trả góp |
| 4 | `stores_inventory.sql` | Chi nhánh, nhân viên, tồn kho |
| 5 | `category_attributes.sql` | Thuộc tính / thông số theo danh mục |
| 6 | `order_status_history.sql` | Lịch sử trạng thái đơn hàng |
| 7 | `reviews.sql` | Đánh giá sản phẩm |
| 8 | `product_tgdd_rating.sql` | Điểm đánh giá Thế Giới Di Động tách khỏi điểm thật (đã bỏ ở bước 16) |
| 9 | `after_sales.sql` | Bảo hành / bảo trì / đổi trả |
| 10 | `sales_reports.sql` | Báo cáo doanh thu, doanh số nhân viên |
| 11 | `branch_manager_role.sql` | Vai trò Quản lý chi nhánh (đổi role dữ liệu thật, sao lưu trước) |
| 12 | `contact_requests.sql` | Form Liên hệ lưu thật |
| 13 | `staff_position_label.sql` | Gộp vị trí nhân viên thành "Nhân viên chi nhánh" |
| 14 | `temporary_password.sql` | Mật khẩu tạm: bắt đổi ở lần đăng nhập đầu + hết hạn |
| 15 | `bank_transactions.sql` | Nhật ký chuyển khoản ngân hàng (webhook + đối soát) |
| 16 | `reviews_own_only.sql` | Chỉ đánh giá thật, mỗi tài khoản đánh giá nhiều lần (sao lưu trước) |
| 17 | `merge_product_variants.sql` | Gộp sản phẩm chỉ khác RAM / bộ nhớ thành một sản phẩm nhiều phiên bản, bỏ phiên bản giá 0 (sao lưu trước; sau đó dọn Redis `cache:*` và chạy lại dữ liệu AI) |
| 18 | `review_likes_variant.sql` | Đánh giá: "Phân loại hàng" + nút "Hữu ích" (`review_likes`) |
| 19 | `order_shipping_area.sql` | Địa chỉ giao hàng 2 cấp: đơn lưu thêm tỉnh / thành + phường / xã |
| 20 | `vouchers.sql` | Voucher giảm % cho đơn hàng (lượt dùng, danh mục áp dụng; đơn + dòng hàng lưu số tiền giảm) |
| 21 | `card_payment.sql` | Thanh toán thẻ quốc tế (Stripe Checkout, chế độ Test): thêm hình thức `CARD` + mã phiên Stripe của khoản thanh toán |
| 22 | `loyalty.sql` | Điểm tích luỹ + hạng thành viên (bảng hạng, lịch sử điểm, điểm dùng / được cộng của đơn; cộng bù đơn đã giao; sao lưu trước) |
| 23 | `product_images_color.sql` | Ảnh sản phẩm theo màu (`product_images.color`, NULL = ảnh chung) |
| 24 | `variant_specifications.sql` | Thông số riêng của từng phiên bản (`product_specifications.variant_id`, NULL = chung) |
| 25 | `filters_needs.sql` | Bộ lọc thông số nâng cao (`product_filter_values`) + nhu cầu (`needs`, `product_needs`); sau khi chạy gọi `POST /api/v1/admin/products/reindex-filters` |
| 26 | `combos.sql` | Combo mua kèm giảm giá (`combos`, `combo_items`, `order_combos`; đơn + dòng hàng lưu `combo_discount`) |
| 27 | `order_email.sql` | Email đơn hàng: `orders.contact_email`, `orders.tracking_token` (link theo dõi đơn), bảng `email_log` |
| 28 | `wishlist.sql` | Danh sách yêu thích: `wishlist_items` (giá lúc thêm + mốc giá để báo giảm giá qua email) |
| 29 | `installment_partners.sql` | Trả góp qua đối tác: bảng `installment_partners`, `installment_fee_rates`; hồ sơ `installment_orders` thêm đối tác, phí / tiền thực nhận, mốc đối soát; hồ sơ cũ còn chờ duyệt chuyển cho đối tác thẻ tín dụng |
| 30 | `product_base_names.sql` | Tên sản phẩm chỉ còn tên máy: dung lượng / màu / GPU / sạc ở cuối tên chuyển vào phiên bản (chọn ở trang chi tiết); sau đó chạy lại `merge_product_variants.sql` để gộp các tên trùng (sao lưu trước; dọn Redis `cache:*`, chạy lại dữ liệu AI) |
| 31 | `dedupe_product_images.sql` | Bỏ ảnh trùng sau khi gộp phiên bản: mỗi (sản phẩm, màu) chỉ giữ bộ ảnh của một trang nguồn TGDĐ, không lặp theo bản dung lượng (dọn Redis `cache:*`) |
| — | `ai_recommendation.sql` | Bảng hành vi người dùng, quan hệ sản phẩm, kho tri thức (pgvector) của phần AI |

Cách chạy một file (PowerShell, từ gốc repo):

```powershell
Get-Content database\migrations\<tên-file>.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping
Get-Content database\migrations\<tên-file>.sql -Raw | docker exec -i techshopping-postgres psql -U postgres -d techshopping_test
```

## Cập nhật catalogue của database đã có

File seed chỉ nạp vào database trống. Với database đã có dữ liệu (ví dụ còn catalogue mẫu cũ, hoặc vừa crawl lại), dùng `Raw_data/import_catalog.py` (cần Python 3 và `psycopg[binary]`; không cần Playwright):

```powershell
$env:PGHOST = "localhost"; $env:PGPORT = "5432"; $env:PGUSER = "postgres"; $env:PGPASSWORD = "postgres"; $env:PGDATABASE = "techshopping"
python Raw_data/import_catalog.py --dry-run   # chạy thử rồi rollback, chỉ in số liệu
python Raw_data/import_catalog.py
```

- Chạy lại được: mỗi sản phẩm nhận diện bằng mã `TGDD-<id nguồn>`, lần sau chỉ cập nhật (tên, giá, mô tả, thông số, thêm ảnh mới). Không nạp điểm đánh giá của Thế Giới Di Động.
- Sản phẩm mẫu cũ cùng trang nguồn được cập nhật tại chỗ (giữ id nên giỏ hàng, đơn hàng, khuyến mãi, tồn kho vẫn đúng); sản phẩm cũ không còn trong dữ liệu mới bị ẩn (không xoá). Thương hiệu bị tách ("iPhone (Apple)", "MacBook"…) được gộp về tên chuẩn. Các phiên bản chỉ khác RAM / bộ nhớ được giữ gộp và script tự chạy file gộp ở cuối.
- Crawl lại từ đầu: `python Raw_data/crawler.py` rồi `python Raw_data/clean_data.py` (cần Playwright) trước khi import.
