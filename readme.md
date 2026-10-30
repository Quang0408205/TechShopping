# TechShopping (POY)

Hệ thống bán lẻ và khuyến nghị mua sắm thiết bị công nghệ (điện thoại, laptop, máy tính bảng, đồng hồ thông minh, phụ kiện) dưới thương hiệu **POY**, làm cho đồ án tốt nghiệp. Hệ thống có hai khu vực dùng chung một backend:

- **Khu khách hàng:** xem và tìm sản phẩm, giỏ hàng, đặt hàng, thanh toán, đơn hàng, bảo hành / đổi trả, đánh giá, trợ lý mua sắm AI.
- **Khu nội bộ:** nhân viên, quản lý chi nhánh và quản trị viên xử lý đơn, tồn kho, trả góp, hậu mãi, khuyến mãi, người dùng, báo cáo doanh thu.

---

## 1. Kiến trúc

```
Trình duyệt ──► nginx (giao diện tĩnh + chuyển tiếp /api, /uploads)
                   │
                   ▼
              Spring Boot (REST API) ──► PostgreSQL 16 + pgvector
                   │                 └─► Redis 7 (cache đọc, giới hạn tốc độ)
                   ▼
              AI Service (FastAPI) ──► Ollama (mô hình Qwen3 chạy trên máy)
```

| Thành phần | Công nghệ | Vai trò |
|---|---|---|
| Frontend | HTML, CSS, JavaScript thuần | Giao diện khách (`customer/`, `auth/`, trang chủ) và khu nội bộ (`admin/`); gọi API qua `fetch` |
| Backend | Spring Boot 4, Java 21 | Toàn bộ nghiệp vụ, phân quyền, API REST `/api/v1` (tài liệu Swagger tự sinh) |
| Cơ sở dữ liệu | PostgreSQL 16 + pgvector | Dữ liệu nghiệp vụ và kho tri thức của chatbot (vector) |
| Cache / giới hạn | Redis 7 | Cache đọc, khoá đăng nhập, giới hạn tốc độ |
| AI Service | Python 3.11, FastAPI | Khuyến nghị sản phẩm (CBF + ALS + Hybrid) và chatbot (RAG + Function Calling) |
| Mô hình ngôn ngữ | Ollama + Qwen3 8B (đổi được sang Claude) | Sinh câu trả lời tiếng Việt cho chatbot |
| Triển khai | Docker Compose | Đóng gói cả hệ thống: nginx, backend, AI Service, PostgreSQL, Redis |

Trình duyệt **không** gọi thẳng AI Service; backend gọi nó kèm khoá nội bộ và chuyển kết quả cho trình duyệt. Khi AI Service hoặc mô hình ngừng, khuyến nghị trả danh sách dự phòng (phổ biến, cùng danh mục) và chatbot tự trả lời theo từ khoá nên trang vẫn dùng được.

---

## 2. Vai trò và phân quyền

Có bốn vai trò cố định. Tài khoản nội bộ có **đúng một** vai trò và không bao giờ đồng thời là khách hàng. Quyền được kiểm ở backend trên **mọi** request, đọc vai trò và phân công chi nhánh từ database chứ không tin vào token.

| Vai trò | Sinh ra bằng cách | Làm được |
|---|---|---|
| `CUSTOMER` | Tự đăng ký trên web | Mua hàng, giỏ hàng, đơn của mình, bảo hành / trả hàng, đánh giá |
| `STAFF` | ADMIN hoặc Quản lý chi nhánh tuyển (tạo tài khoản mới) | Xử lý đơn, trả góp, hậu mãi, tồn kho của chi nhánh được gán; không xem doanh thu |
| `BRANCH_MANAGER` | ADMIN tuyển hoặc nâng từ STAFF (tối đa 1 quản lý mỗi chi nhánh) | Như STAFF, thêm báo cáo doanh thu và quản lý nhân viên chi nhánh mình (tuyển STAFF, sửa hồ sơ, cho nghỉ) |
| `ADMIN` | Tài khoản khởi tạo đầu tiên, hoặc ADMIN khác tạo trong trang Người dùng | Toàn hệ thống; với đơn, trả góp, hậu mãi chỉ **xem** và điều phối (chuyển chi nhánh xử lý) |

| Việc | Khách | Nhân viên | Quản lý chi nhánh | ADMIN |
|---|---|---|---|---|
| Mua hàng, giỏ hàng, đơn của mình, bảo hành / trả hàng của mình | ✅ | — | — | — |
| Xem đơn / trả góp / hậu mãi / tồn kho | — | Chi nhánh mình | Chi nhánh mình | Mọi chi nhánh |
| **Xử lý** đơn, thanh toán, trả góp, hậu mãi, tin liên hệ | — | ✅ | ✅ | ❌ chỉ xem |
| Đổi chi nhánh xử lý của đơn đang chờ | — | — | — | ✅ |
| Nhập kho | — | Chi nhánh mình | Chi nhánh mình | Mọi chi nhánh |
| Doanh thu (Tổng quan, Báo cáo, Excel) | — | ❌ | Chi nhánh mình | Mọi chi nhánh |
| Tuyển dụng | — | — | Chỉ nhân viên, vào chi nhánh mình | Nhân viên / quản lý, mọi chi nhánh; tạo ADMIN |
| Đổi STAFF ↔ BRANCH_MANAGER, khoá tài khoản | — | — | — | ✅ |
| Sản phẩm, danh mục, thương hiệu, khuyến mãi, voucher, combo, chi nhánh, đánh giá, người dùng | — | — | — | ✅ |

Quy tắc chung:
- Đổi vai trò, chuyển chi nhánh, cho nghỉ hoặc khoá tài khoản đều thu hồi phiên đăng nhập của người đó.
- Tài khoản mới của nhân sự có **mật khẩu tạm**: phải đổi ở lần đăng nhập đầu và hết hạn sau 7 ngày. Tạo ADMIN thì mật khẩu tạm là chuỗi ngẫu nhiên hiện đúng một lần; hệ thống không bao giờ tắt hay xoá ADMIN cuối cùng.
- Tạo ADMIN yêu cầu gõ lại tên đăng nhập để xác nhận; trang Người dùng cho lọc tài khoản theo ngày tạo.

---

## 3. Tính năng

### Khu khách hàng
- **Tài khoản:** đăng ký, đăng nhập, hồ sơ, ảnh đại diện, đổi mật khẩu, địa chỉ. Nhập sai mật khẩu nhiều lần thì bị khoá đăng nhập tạm thời.
- **Sản phẩm:** trang chủ, danh sách có tab danh mục, lọc thương hiệu và khoảng giá, tìm kiếm, phân trang, bộ lọc thông số theo danh mục (RAM, bộ nhớ, chip, pin, CPU, card đồ hoạ, kích thước…) và theo nhu cầu (gaming, học tập – văn phòng, đồ hoạ…); thẻ sản phẩm hiện nút **phiên bản** (dung lượng / RAM) và đổi giá theo phiên bản; chi tiết có ảnh, thông số kỹ thuật, giá và tên khuyến mãi đang chạy; chọn màu thì ảnh chính và dãy ảnh nhỏ đổi sang bộ ảnh của màu đó.
- **Giỏ hàng và đặt hàng:** giỏ lưu trên server theo tài khoản; thông tin nhận hàng nhập trong một cửa sổ ngay ở giỏ hàng (giao tận nhà hoặc nhận tại cửa hàng), sau đó sang bước thanh toán.
- **Tài khoản:** thẻ thành viên theo màu hạng (điểm, tiến độ lên hạng), thống kê đơn và chi tiêu, tab "Hạng & điểm" (các hạng, cách tính điểm, lịch sử điểm), hồ sơ, địa chỉ, bảo mật; trên điện thoại menu là hàng tab cuộn ngang.
- **Điểm tích luỹ và hạng thành viên:** 1 điểm cho mỗi 10.000đ tiền hàng thực trả khi đơn đã giao, nhân hệ số của hạng (Thành viên / Bạc từ 50 triệu / Vàng từ 150 triệu / Kim cương từ 300 triệu tổng chi tiêu; ADMIN chỉnh được); dùng ở trang đặt hàng 1 điểm = 1.000đ, tối đa 10% giá trị hàng; huỷ đơn hoàn điểm đã dùng, trả hàng hoàn điểm đã dùng và trừ điểm đã tích tương ứng; hạng đã đạt không bị hạ.
- **Yêu thích và so sánh:** nút trái tim trên thẻ sản phẩm và trang chi tiết lưu sản phẩm vào danh sách yêu thích của tài khoản (trang riêng, số lượng hiện cạnh biểu tượng trái tim ở đầu trang); khi giá một sản phẩm đã lưu giảm, hệ thống gửi email báo cho khách. Nút cái cân chọn tối đa 4 sản phẩm cùng danh mục để so sánh trên một bảng: giá (đánh dấu rẻ nhất), thương hiệu, đánh giá, bảo hành, tồn kho và từng thông số kỹ thuật, tô nổi các dòng khác nhau và có thể chỉ hiện điểm khác nhau.
- **Lịch sử mua hàng:** lọc theo trạng thái (kể cả đơn đã huỷ), tìm theo mã đơn hoặc tên sản phẩm; nút "Mua lại" đưa sản phẩm của đơn đã giao / đã huỷ vào giỏ với giá hiện tại và báo sản phẩm nào không còn bán.
- **Thanh toán:** COD, chuyển khoản qua mã QR VietQR có trang thanh toán riêng (sao chép số tài khoản / số tiền / nội dung, logo ngân hàng, đếm ngược hạn), thẻ quốc tế qua Stripe Checkout ở chế độ thử, trả góp 0% (3 / 6 / 9 / 12 tháng cho đơn từ 3.000.000đ). Đơn chuyển khoản / thẻ chưa trả có nút **Huỷ thanh toán**: đơn bị huỷ, voucher được trả lượt và đúng các sản phẩm của đơn quay lại giỏ. Khách bấm Back sau khi đặt đơn thì giỏ hàng và trang đặt hàng nhắc "Đơn DH… đang chờ thanh toán" kèm hai lựa chọn tiếp tục thanh toán hoặc huỷ đơn lấy lại giỏ.
- **Đơn hàng:** theo dõi trạng thái, huỷ khi còn chờ xác nhận, xem hồ sơ trả góp và đối tác đang xét. Khi đặt hàng khách nhập email nhận thông tin đơn; mỗi bước của đơn (đã đặt, đã nhận thanh toán, đã xác nhận, đang giao hoặc sẵn sàng tại cửa hàng, đã giao, đã huỷ) gửi một email kèm liên kết mở trang theo dõi đơn không cần đăng nhập.
- **Hậu mãi:** gửi yêu cầu bảo hành (còn hạn), bảo trì hoặc trả hàng hoàn tiền trong 7 ngày kèm ảnh, theo dõi trạng thái ở trang Yêu cầu dịch vụ.
- **Đánh giá:** 1–5 sao, nhận xét, tối đa 5 ảnh, viết nhiều đánh giá cho một sản phẩm; hiện nhãn "Đã mua hàng", **phân loại hàng** đã mua, nút **Hữu ích**, lọc theo số sao hoặc có ảnh. Điểm sản phẩm chỉ tính từ đánh giá thật.
- **Khuyến nghị và trợ lý AI:** gợi ý dành cho bạn, tương tự, mua kèm, nâng cấp (mỗi thẻ có lý do gợi ý); trợ lý chat trả lời bằng tiếng Việt, tra chính sách, tìm sản phẩm theo nhu cầu và ngân sách, tra đơn / giỏ / bảo hành của chính khách.
- **Liên hệ:** form gửi tin thật, có giới hạn số lần gửi theo IP và email.

### Khu nội bộ
- **Tổng quan theo vai trò:** ADMIN thấy toàn hệ thống (doanh thu, đơn cần xử lý, hàng sắp hết, so sánh chi nhánh); quản lý thấy số liệu chi nhánh; nhân viên thấy "Việc của tôi hôm nay".
- **Đơn hàng:** lọc theo trạng thái và ngày, đổi trạng thái đúng luồng, mã vận đơn, ghi nhận đã nhận tiền / hoàn tiền; xác nhận đơn trừ tồn kho chi nhánh, huỷ đơn hoàn kho.
- **Trả góp:** hồ sơ do đối tác xét, theo dõi phí đối tác và tiền thực nhận, ghi nhận khi đối tác chuyển tiền; ADMIN chỉnh điều kiện và bảng phí của từng đối tác.
- **Bảo hành / Bảo trì / Đổi trả, Liên hệ khách hàng:** các hộp việc có lọc, ghi chú xử lý và lý do từ chối.
- **Đối soát chuyển khoản:** nhật ký tiền về, gán thủ công hoặc bỏ qua (có lý do), ô giả lập ngân hàng báo tiền để trình diễn.
- **Tồn kho:** tồn theo chi nhánh, cảnh báo sắp hết / hết hàng, nhập kho, lịch sử nhập xuất, nhập kho từ Excel.
- **Sản phẩm, Danh mục, Thương hiệu (ADMIN):** thêm sửa, ảnh tải lên, phiên bản và thông số theo danh mục, nhập sản phẩm từ Excel (kiểm tra trước, có lỗi thì không ghi gì).
- **Khuyến mãi (ADMIN):** chương trình theo thời gian, giảm theo phần trăm hoặc số tiền, mức riêng từng sản phẩm; giỏ hàng lấy giá thấp hơn.
- **Voucher (ADMIN):** mã giảm % cho đơn hàng, có mức giảm tối đa, đơn tối thiểu, phạm vi toàn bộ hoặc theo danh mục, tổng lượt và lượt mỗi khách; ở trang thanh toán khách nhập mã hoặc chọn từ danh sách (mã chưa đủ điều kiện hiện lý do). Voucher tính trên giá đã khuyến mãi, không giảm phí ship; đơn huỷ trước khi giao được trả lại lượt, trả hàng hoàn đúng số tiền đã trả.
- **Menu danh mục:** rê vào "Sản phẩm" mở bảng rộng bằng cả đầu trang; hàng danh mục nằm ngang phía trên, rê từng danh mục thì bên dưới hiện hãng, sản phẩm hot (nhãn Mới / Hot), mức giá và nhu cầu; bấm vào hãng, mức giá hay nhu cầu sẽ mở trang Sản phẩm đã lọc sẵn. Trên điện thoại, danh mục nằm trong ngăn kéo và chạm để mở rộng từng danh mục.
- **Sản phẩm đã xem:** trang chủ có khối nhớ tối đa 10 sản phẩm khách vừa mở (lưu trên trình duyệt), hiện giá mới nhất, cho xoá từng cái hoặc xoá hết; không có gì thì khối ẩn.
- **Chuyển trang và liên hệ:** khi chuyển sang trang khác, một thanh mảnh màu nâu chạy ở mép trên cửa sổ cho tới khi trang mới mở xong. Nút liên hệ ở góc phải mở ra Chat Zalo, Trợ lý AI và Gọi hotline; rê chuột vào từng mục có vòng sáng nhấp nháy quanh biểu tượng.
- **Trang chủ:** lần đầu mỗi phiên có màn hình mở đầu: chữ POY được vẽ nét, đổ màu nâu ánh kim rồi bay đúng vào logo trên đầu trang trước khi màn đen tách đôi (có nút Bỏ qua, không hiện khi bật giảm chuyển động). Ảnh đầu trang chiếm gần cả màn hình, chuyển slide bằng hiệu ứng quét, có bộ đếm và thanh thời gian. Bên dưới là dải thương hiệu chạy ngang, Flash sale với đồng hồ đếm ngược (sản phẩm đang khuyến mãi thật), danh mục dạng ô to nhỏ kèm số sản phẩm, sản phẩm đã xem ngay dưới dải thương hiệu, khu trưng bày sản phẩm được xem nhiều (dải cuộn ngang có nút và kéo chuột), combo nổi bật, các con số thật của cửa hàng, gợi ý cho bạn, sản phẩm đã xem và đánh giá thật của khách hàng; khối nào chưa có dữ liệu thật thì ẩn.
- **Trang sản phẩm:** dải danh mục dạng thẻ ngang có biểu tượng và số sản phẩm, dính dưới thanh đầu trang và thu gọn khi cuộn. Trang chi tiết có mũi tên đổi ảnh ngay trên ảnh chính, số thứ tự ảnh, vuốt trên điện thoại và xem ảnh toàn màn hình; trang đặt hàng cho cột thông tin và cột tóm tắt đơn cuộn cùng nhau.
- **Nút liên hệ:** một nút ở góc phải mở ra Chat Zalo (khi đã cấu hình số), Trợ lý AI và Gọi hotline.
- **Combo mua kèm (ADMIN):** một sản phẩm chính cùng các món mua kèm, mỗi món có mức giảm riêng (theo % hoặc số tiền), có thời hạn và bật / tạm dừng như voucher; trang Combo gợi ý món kèm từ quan hệ "thường mua cùng" của phần AI. Trang chi tiết sản phẩm có khối "Mua kèm tiết kiệm" (tick món, chọn phiên bản, giá sau giảm, thêm cả combo vào giỏ). Giỏ đủ món thì tự giảm, kể cả khi thêm riêng lẻ; bỏ sản phẩm chính thì món kèm về giá thường. Mỗi sản phẩm chính cho giảm một món kèm cùng loại; thứ tự giảm: khuyến mãi sản phẩm, combo, voucher, điểm; miễn phí giao hàng tính trên tiền hàng trước mọi khoản giảm. Giỏ, thanh toán và chi tiết đơn có nhãn combo + dòng "Giảm combo"; trả hàng hoàn đúng số tiền đã trả; báo cáo doanh thu có bảng theo combo.
- **Hạng thành viên (ADMIN):** sửa ngưỡng tổng chi tiêu và hệ số nhân điểm của 4 hạng; hạ ngưỡng thì khách đủ điều kiện được nâng hạng ngay.
- **Chi nhánh, Nhân viên:** quản lý chi nhánh, tuyển và gán nhân sự, lịch sử phân công.
- **Người dùng và phân quyền (ADMIN):** tách tab Khách hàng / Nội bộ, lọc theo vai trò, trạng thái và ngày tạo, khoá / mở khoá, đổi vai trò, xoá mềm, tạo ADMIN.
- **Báo cáo doanh thu:** theo chi nhánh, ngày, danh mục, phương thức thanh toán, nhân viên (kèm hoa hồng), top sản phẩm; xuất Excel; báo cáo hàng nhập kho.
- **Đánh giá (ADMIN):** xem mọi đánh giá, ẩn có lý do (người viết thấy lý do) hoặc hiện lại, điểm sản phẩm tính lại ngay.
- **Lịch sử chatbot (ADMIN):** xem các phiên chat để theo dõi chất lượng trả lời.

---

## 4. Cách các phần hoạt động

**Chi nhánh và tồn kho.** Mỗi chi nhánh có tồn kho riêng theo từng phiên bản sản phẩm. Đơn giao tận nhà được tự gán cho chi nhánh gần địa chỉ nhất mà còn đủ hàng cho cả đơn; đơn nhận tại cửa hàng do khách chọn cửa hàng (miễn phí vận chuyển). Hết hàng ở mọi chi nhánh đang mở thì sản phẩm hiện "Hết hàng" và không đặt được. Xác nhận đơn trừ kho, huỷ đơn đã xác nhận hoàn kho.

**Giá.** Giá hiển thị và giá tính tiền do server quyết định: giá thấp hơn giữa giá giảm thủ công và giá của chương trình khuyến mãi đang chạy. Phí vận chuyển tính theo giá trị đơn (miễn phí từ một mức nhất định hoặc khi nhận tại cửa hàng). Đơn đã đặt giữ nguyên giá.

**Thanh toán chuyển khoản.** Tiền về **một** tài khoản của công ty; doanh thu theo chi nhánh vẫn lấy từ đơn đã giao. Trang thanh toán hiện mã QR VietQR với nội dung là mã đơn (`DH` + 8 số). Khi ngân hàng báo tiền về qua webhook (định dạng giống SePay, xác thực bằng khoá bí mật), hệ thống đọc mã đơn trong nội dung: khớp mã và đúng số tiền thì đơn tự thành "đã nhận tiền"; giao dịch lạ (sai số tiền, đơn đã huỷ, không có mã…) được giữ ở trang Đối soát để ADMIN gán hoặc bỏ qua. Ở chế độ trình diễn, hệ thống tự xác nhận sau khi QR hiện một phút và ADMIN có thể giả lập tiền về.

**Email đơn hàng.** Mọi lần đổi trạng thái của đơn đều đi qua một điểm ghi lịch sử, nơi phát sự kiện gửi mail; nơi ghi nhận đã nhận tiền (nhân viên, webhook ngân hàng, Stripe) phát thêm mail "đã nhận thanh toán". Mail chỉ được dựng và gửi sau khi giao dịch của đơn đã commit, trên luồng riêng, nên đơn bị huỷ giao dịch không gửi gì và mail lỗi không làm hỏng đơn; mỗi mail được ghi vào bảng `email_log` (đã gửi, thất bại hoặc bỏ qua). Mail gửi qua SMTP (Gmail kèm App Password), có bản HTML và bản văn bản thuần, chỉ chứa mã đơn, sản phẩm, tiền và nơi nhận ở mức phường / tỉnh hoặc tên cửa hàng, không có số điện thoại hay địa chỉ chi tiết. Một địa chỉ nhận tối đa vài chục mail mỗi giờ để không ai bị làm phiền bằng cách nhập email của họ. Liên kết trong mail chứa mã ngẫu nhiên 64 ký tự, mở trang theo dõi đơn công khai chỉ hiện tiến trình và sản phẩm.

**Báo giảm giá yêu thích.** Mỗi sản phẩm đã lưu giữ một mốc giá. Mỗi giờ một tác vụ định kỳ so giá bán hiện tại (đã tính khuyến mãi) với mốc: thấp hơn thì gom các sản phẩm giảm của cùng một khách vào một email rồi hạ mốc xuống; giá tăng lại thì nâng mốc lên để lần giảm sau lại được báo. Email dùng chung đường gửi, giới hạn theo địa chỉ và nhật ký với email đơn hàng.

**Ảnh đại diện.** Khi có khoá Cloudinary, ảnh đại diện được tải lên Cloudinary và được cắt vuông quanh khuôn mặt, nén ngay lúc tải; ảnh cũ bị xoá khi đổi. Không có khoá thì ảnh đại diện lưu trên máy chủ như ảnh sản phẩm, đánh giá và hậu mãi (các ảnh này luôn lưu trên máy chủ). Tài khoản chỉ được dùng ảnh do chính hệ thống lưu.

**Thanh toán thẻ (Stripe, chế độ thử).** Stripe không mở tài khoản người bán ở Việt Nam và không chuyển tiền về ngân hàng Việt Nam, nên hình thức "Thẻ quốc tế" chỉ dùng khoá thử (thẻ 4242…, không trừ tiền thật); không có khoá thì hình thức này ẩn. Đặt đơn xong, backend tạo một phiên Stripe Checkout bằng VND với mã đơn và đưa khách sang trang của Stripe; bấm lại nút thanh toán dùng lại phiên còn mở thay vì tạo phiên mới. Stripe chỉ được đưa khách về trang thanh toán của chính cửa hàng (địa chỉ phải thuộc nguồn được phép). Khi khách quay về, backend hỏi Stripe phiên đã trả chưa nên vẫn chạy trên máy cá nhân không có webhook; khi web được chia sẻ qua tên miền công khai, webhook Stripe (kiểm chữ ký, chống gửi lại sau 5 phút) xác nhận cả khi khách đóng trình duyệt. Chỉ ghi nhận đã trả khi đúng số tiền và đơn vị tiền; đơn bị huỷ thì phiên Stripe bị đóng, nếu tiền vẫn về thì khoản thanh toán chuyển sang chờ hoàn tiền.

**Trả góp.** Đơn từ 3.000.000đ trả góp 0% qua đối tác do khách chọn: thẻ tín dụng (kèm ngân hàng phát hành thẻ) hoặc công ty tài chính (chỉ cần CCCD), mỗi đối tác có kỳ hạn và khoảng giá trị đơn riêng. Đối tác xét hồ sơ và thu tiền hằng tháng của khách; được duyệt thì đơn coi như đã thanh toán và cửa hàng mới xác nhận, bị từ chối thì khách gửi cho đối tác khác hoặc đổi cách thanh toán. Cửa hàng chịu phí đối tác (phần trăm giá trị đơn theo kỳ hạn, chốt lúc duyệt) và nhận số tiền còn lại sau khi giao hàng. Đối tác ở đây là mô phỏng, trả lời qua một cổng chung nên có thể thay bằng đối tác thật.

**Hậu mãi.** Bảo hành dựa trên phiếu bảo hành sinh khi đơn được giao (theo số tháng bảo hành của sản phẩm); bảo trì có thể mất phí; trả hàng trong 7 ngày: duyệt → nhận hàng (chọn món còn bán được để cộng lại kho) → hoàn tiền.

**Đánh giá.** Đánh giá hiện ngay khi gửi; điểm trung bình và số đánh giá của sản phẩm tính lại sau mỗi thay đổi. "Đã mua hàng" và "Phân loại hàng" suy ra từ đơn **đã giao** của chính người viết. ADMIN có thể ẩn đánh giá vi phạm kèm lý do.

**Khuyến nghị.** Hệ thống ghi hành vi xem, bấm, thêm giỏ, mua. Mô hình lai giữa lọc theo nội dung (đặc trưng sản phẩm) và phân rã ma trận ALS (hành vi), tự huấn luyện lại mỗi ngày; khách chưa đăng nhập nhận sản phẩm phổ biến và mới. Phụ kiện "mua kèm" và "nâng cấp" dựa trên quan hệ sản phẩm.

**Trợ lý AI.** Chatbot dùng RAG: chính sách của cửa hàng được cắt thành đoạn, mã hoá thành vector (mô hình `multilingual-e5-small`) và lưu trong pgvector; mô hình ngôn ngữ truy hồi đoạn liên quan và có thể gọi các công cụ (tìm sản phẩm, xem chi tiết, tra đơn, giỏ hàng, bảo hành, tìm cửa hàng) dưới quyền của chính khách. Việc thêm giỏ hoặc huỷ đơn luôn cần khách bấm xác nhận. Câu trả lời hiện dần; hội thoại được lưu để giữ ngữ cảnh. Chatbot có giới hạn số tin theo tài khoản và theo IP.

**Bảo mật và hiệu năng.** Đăng nhập bằng JWT ngắn hạn kèm refresh token có thể thu hồi; mật khẩu băm; nhập sai 5 lần trong 15 phút thì khoá 15 phút; đăng ký và làm mới token bị giới hạn theo IP; mọi dữ liệu người dùng đưa vào giao diện đều được escape. Redis cache các dữ liệu đọc nhiều (danh mục, thương hiệu, trang chi tiết sản phẩm, báo cáo) và tự xoá khi dữ liệu đổi; tồn kho luôn đọc mới; Redis lỗi thì hệ thống đọc thẳng database.

**Chạy cho nhiều người dùng thử.** nginx là cổng vào duy nhất: phục vụ giao diện và chuyển tiếp `/api`, `/uploads` sang backend, nên trình duyệt chỉ thấy một địa chỉ và không cần CORS. Có thể mở riêng cổng này ra internet bằng một đường hầm (ngrok) trong khi cơ sở dữ liệu, Redis, AI Service và Ollama vẫn nằm trên máy chủ. Địa chỉ công khai được cấu hình cho backend để các link ảnh tải lên đúng địa chỉ.

---

## 5. Dữ liệu

- **Cấu trúc:** `database/techshopping.sql` gồm khoảng 50 bảng chia theo nhóm: sản phẩm (danh mục, thương hiệu, sản phẩm, phiên bản, ảnh, thông số), người dùng và vai trò, giỏ hàng, đơn hàng (kèm lịch sử trạng thái), thanh toán và trả góp, chuyển khoản, chi nhánh / nhân viên / tồn kho, khuyến mãi, voucher, combo, hậu mãi, đánh giá, liên hệ, hành vi và chatbot. Các thay đổi sau này nằm ở `database/migrations/` (thứ tự chạy ghi trong `database/migrations/README.md`).
- **Catalogue thật:** sản phẩm crawl từ Thế Giới Di Động (`Raw_data/`), làm sạch rồi nạp vào database; mã sản phẩm dạng `TGDD-<id nguồn>`, nên nạp lại chỉ cập nhật. Các sản phẩm chỉ khác RAM / bộ nhớ được gộp thành một sản phẩm nhiều phiên bản. Với các danh mục cào theo từng trang sản phẩm (màn hình, máy tính để bàn, máy tính bảng, đồng hồ), mỗi sản phẩm có đủ tổ hợp phiên bản × màu và ảnh riêng theo màu (`product_images.color`, rỗng = ảnh chung).
- **Dữ liệu mẫu:** khi database Docker được tạo lần đầu, hệ thống nạp thêm khách hàng, đơn hàng và đánh giá mẫu có dấu `SAMPLE-DATA` để minh hoạ. Hành vi người dùng cho phần khuyến nghị có thể được **mô phỏng** (khách `sim.customer.NNN@example.test`, không đăng nhập được) để mô hình học được; dữ liệu này được ghi rõ trong báo cáo và xoá được.
- **Ảnh tải lên** (sản phẩm, danh mục, đánh giá, ảnh đại diện, ảnh hậu mãi) lưu trên đĩa của backend; đường dẫn lưu trong database.

---

## 6. Cấu trúc thư mục

| Thư mục | Nội dung |
|---|---|
| `backend/Tech/` | Spring Boot API (Java 21): `controller`, `service`, `repository`, `entity`, `dto`, `security`, `config`, test |
| `frontend/` | Website: `index.html`, `customer/`, `auth/`, `admin/` (khu nội bộ), `css/`, `js/` (`core/` dùng chung, `customer/`, `admin/`) |
| `AI_Service/` | Python/FastAPI: `app/recommender/` (khuyến nghị), `app/chatbot/` (chatbot), `knowledge/` (tài liệu RAG), `scripts/` (dữ liệu, chỉ mục, đánh giá) |
| `database/` | `techshopping.sql` (cấu trúc), `migrations/` (cập nhật database cũ), `docker-init/` (tạo database test, dữ liệu mẫu) |
| `docker/` | Cấu hình nginx (giao diện + chuyển tiếp API) |
| `Raw_data/` | Crawl (cả phiên bản × màu + ảnh theo màu) và làm sạch catalogue Thế Giới Di Động, nạp vào database |
| `docker-compose.yml` | Mô tả các dịch vụ và mạng nội bộ |

---

## 7. Kiểm thử

- **Backend:** hơn 760 test (đơn vị, tích hợp với database thật qua MockMvc) cho xác thực, phân quyền, giỏ hàng, đơn hàng, thanh toán, trả góp, tồn kho, khuyến mãi, hậu mãi, đánh giá, báo cáo, nhập Excel, cache và giới hạn tốc độ.
- **Giao diện:** các kịch bản chạy trên trình duyệt Edge không giao diện (điều khiển qua Chrome DevTools Protocol) cho từng khu vực: trang khách, giỏ hàng và thanh toán (kể cả thẻ quốc tế với một Stripe giả chạy cục bộ), từng trang quản trị, bố cục ở nhiều kích thước màn hình, an toàn trước chèn mã độc.
- **Ma trận quyền:** một kịch bản gọi hàng chục hành động quan trọng bằng năm kiểu người gọi (chưa đăng nhập, khách, nhân viên, quản lý, ADMIN) và so với luật phân quyền.
- **Chatbot và khuyến nghị:** có bộ câu hỏi đánh giá chatbot và đo Precision / Recall / NDCG cho khuyến nghị.

---

## 8. Tài liệu liên quan

- `AI_Service/README.md`: công nghệ, thuật toán, file, API và kiểm thử của phần AI.
- `database/migrations/README.md`: thứ tự và nội dung các migration.
- `.env.example`: danh sách biến cấu hình (khoá bí mật, tài khoản nhận tiền, mô hình chatbot, địa chỉ công khai).
