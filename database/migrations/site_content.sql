-- Nội dung do ADMIN quản lý: banner trang chủ (ảnh, tiêu đề, liên kết, thứ tự, bật / tắt, khung giờ hiện) và các
-- trang tĩnh (giới thiệu, điều khoản, chính sách...). Nội dung trang là HTML đã được backend lọc theo danh sách
-- thẻ cho phép. Nạp sẵn 8 trang; chạy lại được (không ghi đè trang đã sửa).

create table if not exists banners (
    banner_id serial primary key,
    title varchar(150) not null,
    subtitle varchar(300),
    image_url varchar(500) not null,
    link_url varchar(500),
    display_order int not null default 0,
    is_active boolean not null default true,
    starts_at timestamp,
    ends_at timestamp,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    check (ends_at is null or starts_at is null or ends_at > starts_at)
);

create index if not exists idx_banners_order on banners (display_order, banner_id);

create table if not exists content_pages (
    slug varchar(80) primary key,
    title varchar(200) not null,
    body text not null,
    is_published boolean not null default true,
    updated_by bigint references users(user_id) on delete set null,
    updated_at timestamp not null default current_timestamp
);

insert into content_pages (slug, title, body) values
('gioi-thieu', 'Giới thiệu POY',
 '<p>POY là cửa hàng công nghệ bán điện thoại, laptop, đồng hồ thông minh, màn hình, PC và phụ kiện chính hãng, có nhiều chi nhánh và bán trực tuyến trên toàn quốc.</p><h2>Chúng tôi cam kết</h2><ul><li>Sản phẩm chính hãng, có bảo hành rõ ràng theo từng sản phẩm.</li><li>Giá niêm yết minh bạch, trả góp 0% cho đơn từ 3.000.000đ.</li><li>Giao tận nhà hoặc nhận tại cửa hàng gần bạn.</li><li>Hỗ trợ bảo hành, bảo trì và trả hàng ngay trên tài khoản của bạn.</li></ul><p>Cần tư vấn, bạn có thể gửi tin ở trang <a href="/customer/contact.html">Liên hệ</a> hoặc đến trực tiếp một chi nhánh trong trang <a href="/customer/stores.html">Hệ thống cửa hàng</a>.</p>'),
('dieu-khoan', 'Điều khoản sử dụng',
 '<p>Khi truy cập và mua hàng tại POY, bạn đồng ý với các điều khoản dưới đây.</p><h2>Tài khoản</h2><p>Bạn chịu trách nhiệm giữ kín mật khẩu và các hoạt động diễn ra trên tài khoản của mình. Thông tin đăng ký cần chính xác để chúng tôi liên hệ giao hàng và hỗ trợ.</p><h2>Đặt hàng và giá</h2><p>Giá và khuyến mãi hiển thị tại thời điểm đặt hàng. Đơn hàng chỉ được xác nhận khi cửa hàng còn đủ hàng; nếu thiếu hàng, chúng tôi sẽ liên hệ để bạn đổi sản phẩm hoặc huỷ đơn.</p><h2>Thanh toán</h2><p>POY nhận thanh toán khi nhận hàng, chuyển khoản ngân hàng, thẻ quốc tế và trả góp qua đối tác. Đơn đã thanh toán trực tuyến mà bị huỷ sẽ được hoàn tiền theo cách bạn đã thanh toán.</p><h2>Thay đổi nội dung</h2><p>POY có thể cập nhật điều khoản khi cần; phiên bản mới có hiệu lực từ lúc được đăng trên trang này.</p>'),
('bao-mat', 'Chính sách bảo mật',
 '<p>POY tôn trọng và bảo vệ thông tin cá nhân của bạn.</p><h2>Thông tin chúng tôi thu thập</h2><p>Họ tên, email, số điện thoại, địa chỉ nhận hàng và lịch sử đơn hàng mà bạn cung cấp khi đăng ký và mua hàng.</p><h2>Mục đích sử dụng</h2><ul><li>Xử lý đơn hàng, giao hàng, bảo hành và trả hàng.</li><li>Gửi email về đơn hàng của bạn (xác nhận, giao hàng, thanh toán).</li><li>Gợi ý sản phẩm phù hợp dựa trên các sản phẩm bạn đã xem hoặc mua.</li></ul><h2>Bảo vệ thông tin</h2><p>Mật khẩu được mã hoá, thông tin thẻ do cổng thanh toán xử lý và không lưu tại POY. Chúng tôi không bán thông tin của bạn cho bên thứ ba; chỉ chia sẻ với đơn vị vận chuyển và đối tác thanh toán, trả góp trong phạm vi cần để hoàn tất đơn hàng.</p><h2>Quyền của bạn</h2><p>Bạn có thể xem và sửa thông tin trong trang Tài khoản, hoặc <a href="/customer/contact.html">liên hệ</a> để yêu cầu xoá tài khoản.</p>'),
('giao-hang', 'Chính sách giao hàng',
 '<h2>Phí giao hàng</h2><ul><li>Giao tận nhà: phí mặc định 30.000đ; một số tỉnh / thành có phí riêng và được hiện ở bước thanh toán.</li><li>Nhận tại cửa hàng: miễn phí vận chuyển.</li></ul><h2>Cách xử lý đơn</h2><p>Đơn được giao từ chi nhánh gần địa chỉ của bạn nhất còn đủ hàng. Bạn nhận email khi đơn được xác nhận, đang giao và đã giao; có thể theo dõi tiến độ bằng liên kết trong email hoặc ở trang Đơn hàng của tôi.</p><h2>Khi nhận hàng</h2><p>Vui lòng kiểm tra sản phẩm và tình trạng niêm phong trước khi thanh toán (với đơn thanh toán khi nhận hàng). Nếu có vấn đề, hãy từ chối nhận và liên hệ POY ngay.</p>'),
('huong-dan-mua-hang', 'Hướng dẫn mua hàng',
 '<ol><li>Chọn sản phẩm, phiên bản và màu sắc bạn muốn rồi bấm <strong>Thêm vào giỏ</strong>.</li><li>Mở giỏ hàng, kiểm tra số lượng và mã giảm giá (nếu có), bấm <strong>Thanh toán</strong>.</li><li>Điền thông tin nhận hàng, chọn giao tận nhà hoặc nhận tại cửa hàng, chọn cách thanh toán.</li><li>Bấm <strong>Đặt hàng</strong>. Với chuyển khoản hoặc thẻ, làm theo hướng dẫn ở trang thanh toán.</li><li>Theo dõi đơn ở trang <a href="/customer/orders.html">Đơn hàng của tôi</a> hoặc qua email.</li></ol><p>Bạn cần đăng nhập để đặt hàng. Chưa có tài khoản? Đăng ký chỉ mất một phút.</p>'),
('huong-dan-tra-gop', 'Hướng dẫn mua trả góp',
 '<h2>Điều kiện</h2><ul><li>Đơn hàng từ 3.000.000đ, lãi suất 0% cho khách, không cần trả trước.</li><li>Bạn chọn đối tác trả góp: thẻ tín dụng hoặc công ty tài chính; mỗi đối tác có kỳ hạn và khoảng giá trị đơn hỗ trợ riêng.</li></ul><h2>Các bước</h2><ol><li>Ở trang thanh toán, chọn <strong>Trả góp</strong>, chọn đối tác và kỳ hạn.</li><li>Nhập thông tin theo yêu cầu của đối tác (với công ty tài chính cần số CCCD).</li><li>Đối tác xét hồ sơ và trả lời trên trang chi tiết đơn. Được duyệt thì đơn chuyển sang đã thanh toán.</li><li>Nếu bị từ chối, bạn có thể gửi hồ sơ cho đối tác khác hoặc chọn cách thanh toán khác.</li></ol><p>Đơn trả góp cần trả hàng vui lòng <a href="/customer/contact.html">liên hệ</a> cửa hàng.</p>'),
('bao-hanh', 'Chính sách bảo hành',
 '<h2>Bảo hành</h2><ul><li>Thời hạn theo từng sản phẩm (ghi ở trang sản phẩm), tính từ ngày bạn nhận hàng.</li><li>Còn hạn bảo hành: sửa chữa miễn phí tại chi nhánh đã bán đơn hàng.</li><li>Mỗi sản phẩm xử lý một yêu cầu bảo hành tại một thời điểm.</li></ul><h2>Bảo trì</h2><p>Vệ sinh, cài đặt phần mềm, sửa chữa cho mọi sản phẩm đã mua tại POY, kể cả khi hết bảo hành. Có thể mất phí: cửa hàng báo chi phí dự kiến trước khi làm.</p><h2>Cách gửi yêu cầu</h2><p>Vào <a href="/customer/orders.html">Đơn hàng của tôi</a>, mở đơn đã giao và chọn <strong>Gửi bảo hành</strong> hoặc <strong>Gửi bảo trì</strong>. Theo dõi tiến độ ở trang <a href="/customer/service-requests.html">Yêu cầu dịch vụ</a>.</p>'),
('doi-tra', 'Chính sách đổi trả',
 '<ul><li>Trả hàng trong 7 ngày kể từ khi nhận hàng, với mọi lý do.</li><li>Hoàn tiền theo giá bạn đã trả cho từng sản phẩm, không hoàn phí giao hàng.</li><li>Muốn đổi sang sản phẩm khác: trả hàng rồi đặt đơn mới.</li><li>Đơn trả góp: vui lòng <a href="/customer/contact.html">liên hệ</a> cửa hàng để trả hàng.</li></ul><p>Gửi yêu cầu ở trang <a href="/customer/orders.html">Đơn hàng của tôi</a>: mở đơn đã giao và chọn <strong>Trả hàng</strong>.</p>')
on conflict (slug) do nothing;
