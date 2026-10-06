-- sql schema: hệ thống quản lý và khuyến nghị mua sắm thiết bị công nghệ (bản chốt của nhóm, đã rà soát)
-- database: postgresql 15+
-- encoding: utf-8
--
-- Chạy từ thư mục gốc dự án trên database trống:
-- psql -v ON_ERROR_STOP=1 -d tgdd -f database/techshopping.sql
-- file gồm: 43 bảng + index + trigger + view
--
-- so với bản chốt gốc:
--   * sắp xếp lại thứ tự tạo bảng (stores và nhóm khuyến mãi lên trước đơn hàng)
--   * bỏ cột trùng dữ liệu: products.stock_quantity, product_variants.stock_quantity (tồn kho nằm ở
--     inventory, xem v_variant_stock / v_product_stock), orders.payment_method (nằm ở payments),
--     products.discount_price và products.sku (nằm ở product_variants), product_variants.color /
--     storage / ram (lưu qua attributes -> attribute_values -> variant_attribute_values)
--   * thêm category_attributes (danh mục nào dùng thuộc tính nào)
--   * thêm order_items.promotion_id; order_items.subtotal thành cột tự tính
--   * email và username không phân biệt hoa thường, cho phép đăng ký lại sau khi xóa mềm
--   * check giá trị cho các cột trạng thái, index cho mọi khóa ngoại, trigger updated_at
--     (trừ reviews) và trigger chặn sửa/xóa stock_movements
--
-- =====================================================
-- CÁC CHỖ ĐÃ SỬA SAU KHI RÀ SOÁT (tìm "[SỬA #n]" trong file để thấy vị trí)
-- =====================================================
--   [SỬA #1]  Mọi cột status / priority có default nhưng thiếu NOT NULL -> thêm NOT NULL.
--             Lý do: check (status in ...) cho qua giá trị NULL; riêng payments, dòng status NULL còn
--             lọt qua unique index uq_payments_main_per_order (vì null <> 'CANCELLED' ra null).
--   [SỬA #2]  stock_movements: ép dấu quantity_change theo movement_type
--             (IN / RETURN > 0, OUT < 0, ADJUSTMENT <> 0).
--   [SỬA #3]  order_items: unit_price >= 0, discount_amount >= 0, subtotal >= 0.
--   [SỬA #4]  orders: đơn PICKUP (nhận tại cửa hàng) bắt buộc có store_id.
--   [SỬA #5]  warranties: warranty_end_date >= warranty_start_date.
--   [SỬA #6]  review_images: unique (review_id, display_order).
--   [SỬA #7]  v_product_stock: left join từ products, sản phẩm chưa có phiên bản hiện tồn = 0.
--   [SỬA #8]  assigned_to_employee thống nhất: warranty_requests, maintenance_requests, chat_sessions
--             đều trỏ employees(employee_id) (trước đây 2 bảng đầu trỏ users). Vì vậy bảng employees
--             được chuyển lên trước nhóm 7. LƯU Ý: code ứng dụng phải ghi employee_id, không phải user_id.
--   [SỬA #9]  payments: khoản thu của kỳ trả góp (installment_payment_id có giá trị) bắt buộc
--             payment_method = 'INSTALLMENT'.
--   [SỬA #10] Cột dẫn xuất: thêm check biên (rating 0-5, total_reviews / view_count / loyalty_points /
--             total_spent >= 0) và view v_inventory_mismatch để đối soát inventory với stock_movements.
--   [SỬA #11] Ràng buộc chéo bảng, ép bằng trigger: return_items.order_item_id phải thuộc đúng đơn của
--             return_requests và tổng số lượng trả không vượt số đã mua; sales_records.store_id phải
--             trùng orders.store_id (khi đơn có store_id).
--   [SỬA #12] reviews.comment 10-2000 ký tự ép bằng check; tối đa 5 ảnh / đánh giá ép bằng trigger;
--             hàm attach_updated_at_triggers() để bảng thêm sau gắn trigger updated_at (chạy lại được).
--   [SỬA #13] Thêm bảng order_status_history, tự ghi bằng trigger mỗi khi orders.status đổi
--             (người đổi lấy từ biến phiên app.current_user_id nếu ứng dụng có set).
--
-- =====================================================
-- GHI CHÚ THIẾT KẾ / VIỆC CỐ Ý CHƯA SỬA (cần nhóm quyết định)
-- =====================================================
--   [GHI CHÚ B] ĐÃ CHỐT: hai cơ chế giảm giá (discount_price và promotions) không cộng dồn, lấy giá
--               thấp nhất. Xem khối "QUY TẮC TÍNH GIÁ BÁN" ngay dưới phần ghi chú này.
--   [GHI CHÚ C] Dữ liệu nhạy cảm: installment_orders.citizen_id (CCCD) và employees.salary đang lưu
--               dạng văn bản thường. Nên mã hóa ở ứng dụng (hoặc pgcrypto) và hạn chế quyền truy cập;
--               việc mã hóa làm thay đổi cách ứng dụng đọc / ghi nên chưa làm ở schema.
--   [GHI CHÚ G] Quy ước trạng thái chưa thống nhất: orders, warranty_requests, maintenance_requests,
--               return_requests, chat_sessions, support_tickets dùng chữ thường + so sánh lower();
--               payments, installment_orders, installment_payments dùng chữ HOA, so sánh chính xác.
--               Chưa đổi vì ứng dụng và dữ liệu hiện tại đang dùng sẵn hai kiểu này.
--   [GHI CHÚ H] Cột thời gian dùng timestamp (không múi giờ). Chạy riêng ở Việt Nam thì ổn; đổi sang
--               timestamptz ảnh hưởng cách driver đọc / ghi nên chưa đổi.
--   [GHI CHÚ K] Dự án đang ở giai đoạn thiết kế, chưa có database nào được tạo. File này là nguồn
--               duy nhất của schema: chạy một lần trên database trống là có đủ 43 bảng. Khi sau này
--               đã có database chạy thật và có dữ liệu, mọi thay đổi schema mới nên viết thành file
--               migration riêng (ALTER TABLE ...) thay vì chạy lại file này.

-- =====================================================
-- QUY TẮC TÍNH GIÁ BÁN (đã chốt: lấy giá thấp nhất, KHÔNG cộng dồn)
-- =====================================================
-- Với mỗi phiên bản sản phẩm (product_variants) tại thời điểm đặt hàng:
--
--   1. giá_gốc        = product_variants.price
--   2. giá_phiên_bản  = coalesce(product_variants.discount_price, price)
--   3. giá_khuyến_mãi = giá_gốc - mức_giảm, với mức_giảm tính TRÊN GIÁ GỐC (không tính trên discount_price):
--        - chỉ xét chương trình đang chạy: promotions.is_active = true và
--          now() nằm trong [start_date, end_date], sản phẩm có trong promotion_products
--        - mức giảm của sản phẩm: lấy discount_type / discount_value trong promotion_products nếu có
--          (ghi đè riêng), nếu không thì lấy của promotions
--        - PERCENTAGE: mức_giảm = giá_gốc * discount_value / 100, rồi chặn tối đa max_discount_amount
--          (tính cho MỖI sản phẩm / đơn vị, không phải cho cả dòng)
--        - FIXED_AMOUNT: mức_giảm = discount_value
--        - giá_khuyến_mãi không được âm (tối thiểu 0)
--        - nếu sản phẩm thuộc nhiều chương trình đang chạy: lấy chương trình cho giá thấp nhất
--   4. giá_bán = least(giá_phiên_bản, giá_khuyến_mãi)   -- không cộng dồn
--
-- Cách ghi vào order_items (giữ nguyên công thức subtotal tự tính của DB):
--   unit_price      = giá_gốc (giá niêm yết lúc bán, copy cứng, KHÔNG tra lại bảng khác sau này)
--   discount_amount = (giá_gốc - giá_bán) * quantity      -- tổng giảm của cả dòng
--   promotion_id    = id chương trình nếu giá_khuyến_mãi < giá_phiên_bản (khuyến mãi thắng);
--                     null nếu discount_price thắng, bằng nhau, hoặc không có khuyến mãi
--   subtotal        = quantity * unit_price - discount_amount = giá_bán * quantity (DB tự tính)
--
-- Ví dụ: price 20.000.000, discount_price 18.500.000, khuyến mãi 10% tối đa 1.000.000, mua 2 cái
--   giá_phiên_bản = 18.500.000;  giá_khuyến_mãi = 20.000.000 - min(2.000.000, 1.000.000) = 19.000.000
--   giá_bán = 18.500.000  (discount_price thắng)  =>  unit_price 20.000.000, discount_amount 3.000.000,
--   promotion_id null, subtotal 37.000.000
-- Quy tắc này do ứng dụng thực hiện (DB chỉ lưu kết quả); đổi quy tắc thì sửa khối này cho khớp.

-- =====================================================
-- extensions (tìm kiếm tên sản phẩm tiếng việt không phân biệt dấu)
-- =====================================================

create extension if not exists unaccent;
create extension if not exists pg_trgm;

-- =====================================================
-- nhóm 1: người dùng và phân quyền
-- =====================================================

-- bảng roles: quản lý các vai trò trong hệ thống
create table roles (
    role_id serial primary key,
    name varchar(50) unique not null,
    description text,
    created_at timestamp default current_timestamp
);
comment on table roles is 'Vai trò hệ thống (ADMIN, STAFF, CUSTOMER...). Không có khóa ngoại; được tham chiếu bởi user_roles (role_id).';

-- bảng users: lưu thông tin tài khoản người dùng
create table users (
    user_id bigserial primary key,
    email varchar(120) not null,
    username varchar(50) not null,
    password_hash varchar(255) not null,
    fullname varchar(120) not null,
    phone varchar(20),
    avatar_url text,
    is_active boolean default true,
    last_login timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    deleted_at timestamp
);
comment on table users is 'Tài khoản người dùng. Là bảng gốc, được rất nhiều bảng khác tham chiếu qua user_id: user_roles, customer_profiles, carts, orders, employees, warranty_requests, maintenance_requests, return_requests, user_interactions, recommendations, chat_sessions, support_tickets, promotions, payments (confirmed_by / refunded_by), installment_orders (reviewed_by), stock_movements (created_by = người nhập / xuất kho), reviews (user_id, hidden_by — nhóm 11). email và username không phân biệt hoa thường, chỉ tính tài khoản chưa xóa mềm (unique index một phần uq_users_email_active, uq_users_username_active).';

-- email và username không phân biệt hoa thường, chỉ tính tài khoản chưa xóa mềm
-- (hai index này cũng là index tra cứu email, username; tra cứu phải dùng lower(...) để dùng được index)
create unique index uq_users_email_active    on users (lower(email))    where deleted_at is null;
create unique index uq_users_username_active on users (lower(username)) where deleted_at is null;

-- bảng user_roles: liên kết người dùng với vai trò
create table user_roles (
    user_id bigint not null references users(user_id) on delete cascade,
    role_id int not null references roles(role_id) on delete cascade,
    assigned_at timestamp default current_timestamp,
    primary key (user_id, role_id)
);
comment on table user_roles is 'Bảng nối nhiều-nhiều giữa users và roles (1 người dùng có thể có nhiều vai trò).';

-- bảng customer_profiles: lưu thông tin chi tiết của khách hàng
create table customer_profiles (
    customer_id bigint primary key references users(user_id) on delete cascade,
    date_of_birth date,
    gender varchar(10),
    address varchar(255),
    city varchar(100),
    district varchar(100),
    ward varchar(100),
    postal_code varchar(20),
    default_shipping_address text,
    loyalty_points int default 0,
    total_spent decimal(15, 2) default 0.00,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table customer_profiles is 'Hồ sơ khách hàng, quan hệ 1-1 với users (customer_id vừa là khóa chính vừa là khóa ngoại tới users.user_id).';

-- =====================================================
-- nhóm 2: danh mục và sản phẩm
-- =====================================================

-- bảng categories: quản lý danh mục sản phẩm
create table categories (
    category_id serial primary key,
    name varchar(100) not null,
    slug varchar(100) unique not null,
    description text,
    parent_id int references categories(category_id),
    icon_url text,
    display_order int,
    is_active boolean default true,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table categories is 'Danh mục sản phẩm. Tự tham chiếu qua parent_id (danh mục cha-con); được products.category_id tham chiếu.';

-- bảng brands: quản lý thương hiệu
create table brands (
    brand_id serial primary key,
    name varchar(100) not null unique,
    slug varchar(100) unique not null,
    logo_url text,
    description text,
    website_url text,
    is_active boolean default true,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table brands is 'Thương hiệu sản phẩm. Không có khóa ngoại; được products.brand_id tham chiếu.';

-- bảng products: quản lý thông tin sản phẩm
create table products (
    product_id bigserial primary key,
    name varchar(255) not null,
    slug varchar(255) unique not null,
    description text,
    brand_id int references brands(brand_id),
    category_id int not null references categories(category_id),
    base_price decimal(15, 2) not null check (base_price >= 0),
    weight decimal(10, 2),
    warranty_months int default 12,
    rating decimal(3, 2) default 0,
    total_reviews int default 0,
    view_count int default 0,
    is_active boolean default true,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    deleted_at timestamp
);
comment on table products is 'Sản phẩm. Nối tới brands (brand_id) và categories (category_id). Được tham chiếu bởi product_images, product_specifications, product_variants, promotion_products, user_interactions, recommendations, product_relations, reviews (nhóm 11). rating / total_reviews: điểm trung bình và số đánh giá đang hiện, sẽ do ứng dụng tính lại từ reviews. Giá bán thật, giá giảm và sku nằm ở product_variants; base_price chỉ là giá hiển thị.';

-- bảng product_images: lưu hình ảnh sản phẩm
create table product_images (
    image_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    image_url text not null,
    alt_text varchar(255),
    display_order int,
    is_primary boolean default false,
    uploaded_at timestamp default current_timestamp
);
comment on table product_images is 'Ảnh của sản phẩm. Nối tới products (product_id), xóa sản phẩm thì xóa luôn ảnh. Mỗi sản phẩm tối đa 1 ảnh chính (unique index một phần uq_product_primary_image).';

-- bảng product_specifications: lưu thông số kỹ thuật của sản phẩm
create table product_specifications (
    specification_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    spec_name varchar(100) not null,
    spec_value text not null,
    spec_order int
);
comment on table product_specifications is 'Thông số kỹ thuật của sản phẩm. Nối tới products (product_id), xóa sản phẩm thì xóa luôn thông số.';

-- bảng product_variants: quản lý các phiên bản sản phẩm
create table product_variants (
    variant_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    variant_name varchar(255) not null,
    sku_variant varchar(50) unique,
    price decimal(15, 2) not null check (price >= 0),
    discount_price decimal(15, 2),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table product_variants is 'Phiên bản của sản phẩm (mỗi phiên bản là một tổ hợp màu / dung lượng / RAM..., lưu qua variant_attribute_values, không có cột riêng). Nối tới products (product_id). Được tham chiếu bởi cart_items, order_items, variant_attribute_values, inventory, stock_movements. discount_price và promotions không cộng dồn, lấy giá thấp nhất (xem khối QUY TẮC TÍNH GIÁ BÁN ở đầu file).';

-- bảng attributes: quản lý thuộc tính sản phẩm
create table attributes (
    attribute_id serial primary key,
    name varchar(100) not null unique,
    description text,
    attribute_type varchar(50)
);
comment on table attributes is 'Loại thuộc tính sản phẩm (màu, RAM...). Không có khóa ngoại; được attribute_values.attribute_id tham chiếu.';

-- bảng attribute_values: lưu các giá trị của thuộc tính
create table attribute_values (
    value_id serial primary key,
    attribute_id int not null references attributes(attribute_id),
    value varchar(255) not null,
    unique(attribute_id, value)
);
comment on table attribute_values is 'Giá trị của một thuộc tính (ví dụ: attribute "Màu" có value "Đen"). Nối tới attributes (attribute_id). Được variant_attribute_values.value_id tham chiếu.';

-- bảng category_attributes: danh mục nào dùng những thuộc tính nào
-- (dùng để sinh form nhập sản phẩm và thanh lọc sản phẩm theo từng danh mục)
create table category_attributes (
    category_id int not null references categories(category_id) on delete cascade,
    attribute_id int not null references attributes(attribute_id) on delete cascade,
    is_required boolean not null default true,
    is_filterable boolean not null default true,
    display_order int,
    primary key (category_id, attribute_id)
);
comment on table category_attributes is 'Bảng nối nhiều-nhiều giữa categories và attributes: danh mục nào có những thuộc tính nào, bắt buộc hay không, có dùng để lọc không và thứ tự hiển thị.';

-- bảng variant_attribute_values: liên kết phiên bản sản phẩm với giá trị thuộc tính
create table variant_attribute_values (
    variant_id bigint not null references product_variants(variant_id) on delete cascade,
    value_id int not null references attribute_values(value_id) on delete cascade,
    primary key (variant_id, value_id)
);
comment on table variant_attribute_values is 'Bảng nối nhiều-nhiều giữa product_variants và attribute_values (mỗi phiên bản có nhiều cặp thuộc tính-giá trị).';

-- =====================================================
-- nhóm 3: chi nhánh
-- (đặt trước đơn hàng vì orders.store_id tham chiếu stores)
-- =====================================================

-- bảng stores: quản lý thông tin siêu thị
create table stores (
    store_id serial primary key,
    name varchar(120) not null,
    address varchar(255) not null,
    phone varchar(20),
    email varchar(120),
    city varchar(100),
    district varchar(100),
    latitude decimal(10, 8),
    longitude decimal(11, 8),
    is_active boolean default true,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table stores is 'Siêu thị / chi nhánh. Không có khóa ngoại; được tham chiếu bởi employee_assignments, sales_records, inventory, stock_movements và orders (store_id = chi nhánh xử lý đơn). district / city dùng để tự chọn chi nhánh gần nhất cho đơn giao tận nhà.';

-- =====================================================
-- nhóm 4: khuyến mãi
-- (đặt trước đơn hàng vì order_items.promotion_id tham chiếu promotions)
-- =====================================================

-- bảng promotions: chương trình khuyến mãi (có thời hạn, mức giảm mặc định)
create table promotions (
    promotion_id bigserial primary key,
    name varchar(255) not null,
    description text,
    discount_type varchar(20) not null check (discount_type in ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value decimal(15, 2) not null check (discount_value > 0),
    max_discount_amount decimal(15, 2),
    start_date timestamp not null,
    end_date timestamp not null,
    is_active boolean not null default true,
    created_by bigint not null references users(user_id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    check (end_date > start_date),
    check (discount_type <> 'PERCENTAGE' or discount_value <= 100)
);
comment on table promotions is 'Chương trình khuyến mãi. Nối tới users (created_by = admin tạo chương trình). Được tham chiếu bởi promotion_products và order_items (promotion_id = chương trình đã áp cho dòng đơn hàng). Mức giảm tính trên price (giá gốc), không cộng dồn với discount_price (xem khối QUY TẮC TÍNH GIÁ BÁN ở đầu file).';

-- bảng promotion_products: sản phẩm tham gia chương trình, có thể ghi đè mức giảm riêng
create table promotion_products (
    promotion_id bigint not null references promotions(promotion_id) on delete cascade,
    product_id bigint not null references products(product_id) on delete cascade,
    discount_type varchar(20) check (discount_type is null or discount_type in ('PERCENTAGE', 'FIXED_AMOUNT')),
    discount_value decimal(15, 2) check (discount_value is null or discount_value > 0),
    created_at timestamp not null default current_timestamp,
    primary key (promotion_id, product_id),
    check (discount_type <> 'PERCENTAGE' or discount_value <= 100),
    check ((discount_type is null) = (discount_value is null))
);
comment on table promotion_products is 'Bảng nối nhiều-nhiều giữa promotions và products (sản phẩm nào tham gia chương trình nào, có thể có mức giảm riêng).';

-- =====================================================
-- nhóm 5: giỏ hàng và mua hàng
-- =====================================================

-- bảng carts: quản lý giỏ hàng của người dùng
create table carts (
    cart_id bigserial primary key,
    user_id bigint unique not null references users(user_id) on delete cascade,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table carts is 'Giỏ hàng của người dùng, quan hệ 1-1 với users (user_id). Được tham chiếu bởi cart_items.';

-- bảng cart_items: lưu các sản phẩm trong giỏ hàng
create table cart_items (
    cart_item_id bigserial primary key,
    cart_id bigint not null references carts(cart_id) on delete cascade,
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null,
    added_at timestamp default current_timestamp,
    unique(cart_id, variant_id)
);
comment on table cart_items is 'Dòng sản phẩm trong giỏ hàng. Nối tới carts (cart_id) và product_variants (variant_id); mỗi giỏ không có 2 dòng cùng 1 phiên bản.';

-- bảng orders: quản lý đơn hàng
create table orders (
    order_id bigserial primary key,
    user_id bigint not null references users(user_id),
    order_date timestamp default current_timestamp,
    recipient_name varchar(120) not null,
    recipient_phone varchar(20) not null,
    shipping_address text not null,
    billing_address text,
    shipping_cost decimal(15, 2) default 0,
    tax_amount decimal(15, 2) default 0,
    total_amount decimal(15, 2) not null check (total_amount >= 0),
    status varchar(50) not null default 'pending',          -- [SỬA #1] thêm NOT NULL
    tracking_number varchar(100),
    notes text,
    delivered_at timestamp,
    cancelled_at timestamp,
    store_id int references stores(store_id), -- chi nhánh xử lý đơn (null = đơn đặt trước Phase 7); bảng stores ở nhóm 3, trên
    delivery_type varchar(20) not null default 'HOME_DELIVERY' check (delivery_type in ('HOME_DELIVERY', 'PICKUP')),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    -- [SỬA #4] đơn nhận tại cửa hàng bắt buộc phải biết cửa hàng nào
    constraint chk_orders_pickup_has_store check (delivery_type <> 'PICKUP' or store_id is not null)
);
comment on table orders is 'Đơn hàng. Nối tới users (user_id, người đặt) và stores (store_id, chi nhánh xử lý). Được tham chiếu bởi order_items, order_status_history, payments, installment_orders, return_requests, sales_records, stock_movements. Phương thức thanh toán nằm ở payments.payment_method. Đơn PICKUP bắt buộc có store_id (chk_orders_pickup_has_store).';

-- bảng order_items: lưu chi tiết sản phẩm trong đơn hàng
create table order_items (
    order_item_id bigserial primary key,
    order_id bigint not null references orders(order_id) on delete cascade,
    variant_id bigint not null references product_variants(variant_id),
    promotion_id bigint references promotions(promotion_id), -- chương trình khuyến mãi đã áp cho dòng này (null = không có)
    quantity int not null check (quantity > 0),
    unit_price decimal(15, 2) not null check (unit_price >= 0),                                  -- [SỬA #3]
    discount_amount decimal(15, 2) default 0 check (discount_amount is null or discount_amount >= 0), -- [SỬA #3]
    subtotal decimal(15, 2) generated always as
        (quantity * unit_price - coalesce(discount_amount, 0)) stored,
    -- [SỬA #3] giảm giá không được lớn hơn tiền hàng (subtotal không âm)
    constraint chk_order_items_subtotal_non_negative check (quantity * unit_price - coalesce(discount_amount, 0) >= 0)
);
comment on table order_items is 'Chi tiết từng dòng sản phẩm trong đơn hàng. Nối tới orders (order_id), product_variants (variant_id) và promotions (promotion_id, nếu dòng này được giảm theo chương trình). Được tham chiếu bởi warranties, maintenance_requests, return_items. subtotal là cột tự tính (quantity * unit_price - discount_amount), không ghi trực tiếp; không được âm. Quy ước ghi: unit_price = giá gốc lúc bán, discount_amount = tổng giảm của cả dòng, promotion_id chỉ có khi khuyến mãi thắng discount_price (xem khối QUY TẮC TÍNH GIÁ BÁN ở đầu file).';

-- bảng order_status_history: lịch sử đổi trạng thái đơn hàng [SỬA #13]
create table order_status_history (
    history_id bigserial primary key,
    order_id bigint not null references orders(order_id) on delete cascade,
    old_status varchar(50),                 -- null = dòng ghi lúc tạo đơn
    new_status varchar(50) not null,
    changed_by bigint references users(user_id), -- null nếu ứng dụng không set app.current_user_id
    changed_at timestamp not null default current_timestamp
);
comment on table order_status_history is 'Lịch sử đổi trạng thái của đơn hàng. Nối tới orders (order_id) và users (changed_by). Tự ghi bằng trigger trg_orders_status_history khi tạo đơn hoặc đổi orders.status; không cần ứng dụng ghi tay. Để biết người đổi, ứng dụng gọi select set_config(''app.current_user_id'', ''<user_id>'', true) trong transaction trước khi update.';

-- =====================================================
-- nhóm 6: thanh toán và trả góp
-- =====================================================

-- bảng installment_orders: hợp đồng trả góp của đơn hàng (hồ sơ + kết quả duyệt)
create table installment_orders (
    installment_id bigserial primary key,
    order_id bigint unique not null references orders(order_id),
    num_months int not null check (num_months > 0),
    monthly_payment decimal(15, 2) not null check (monthly_payment > 0),
    interest_rate decimal(5, 2),
    total_interest decimal(15, 2),
    status varchar(50) not null default 'PENDING_APPROVAL'   -- [SỬA #1] thêm NOT NULL
        check (status in ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    citizen_id varchar(20) not null check (citizen_id ~ '^[0-9]+$'),
    card_bank_code varchar(20) not null,
    rejection_reason text,
    reviewed_by bigint references users(user_id),
    reviewed_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    check (status <> 'REJECTED' or rejection_reason is not null)
);
comment on table installment_orders is 'Hợp đồng trả góp (CCCD, ngân hàng thẻ, kỳ hạn, kết quả duyệt), quan hệ 1-1 với orders (order_id). Nối tới users (reviewed_by = nhân viên duyệt / từ chối). Được tham chiếu bởi installment_payments. citizen_id là dữ liệu nhạy cảm, nên mã hóa / hạn chế quyền truy cập.';

-- bảng installment_payments: quản lý các kỳ thanh toán trả góp
create table installment_payments (
    installment_payment_id bigserial primary key,
    installment_id bigint not null references installment_orders(installment_id),
    payment_number int not null check (payment_number > 0),
    amount decimal(15, 2) not null check (amount > 0),
    due_date date not null,
    paid_date date,
    status varchar(50) not null default 'PENDING' check (status in ('PENDING', 'PAID')), -- [SỬA #1] thêm NOT NULL
    unique(installment_id, payment_number)
);
comment on table installment_payments is 'Từng kỳ thanh toán trả góp. Nối tới installment_orders (installment_id); mỗi đơn trả góp không có 2 kỳ trùng số. Được payments.installment_payment_id tham chiếu khi kỳ đã thu.';

-- bảng payments: quản lý các giao dịch thanh toán (khoản chính của đơn, hoặc khoản thu của 1 kỳ trả góp)
create table payments (
    payment_id bigserial primary key,
    order_id bigint not null references orders(order_id),
    amount decimal(15, 2) not null check (amount > 0),
    payment_method varchar(50) not null check (payment_method in ('COD', 'BANK_TRANSFER', 'INSTALLMENT')),
    transaction_id varchar(100),
    status varchar(50) not null default 'PENDING'            -- [SỬA #1] thêm NOT NULL
        check (status in ('PENDING', 'PAID', 'REFUND_PENDING', 'REFUNDED', 'CANCELLED')),
    paid_at timestamp,
    confirmed_by bigint references users(user_id),
    refunded_at timestamp,
    refunded_by bigint references users(user_id),
    installment_payment_id bigint unique references installment_payments(installment_payment_id),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    -- [SỬA #9] khoản thu của một kỳ trả góp phải mang phương thức INSTALLMENT
    constraint chk_payments_installment_method
        check (installment_payment_id is null or payment_method = 'INSTALLMENT')
);
comment on table payments is 'Giao dịch thanh toán. Nối tới orders (order_id), users (confirmed_by = nhân viên xác nhận nhận tiền, refunded_by = nhân viên hoàn tiền) và installment_payments (installment_payment_id, khoản thu của 1 kỳ trả góp). Mỗi đơn COD / chuyển khoản có đúng 1 khoản chính (installment_payment_id null) còn hiệu lực — unique index một phần uq_payments_main_per_order (bỏ qua khoản đã CANCELLED). Khoản có installment_payment_id bắt buộc payment_method = INSTALLMENT (chk_payments_installment_method).';

-- bảng employees: đặt trước nhóm 7 vì warranty_requests / maintenance_requests tham chiếu employees [SỬA #8]
create table employees (
    employee_id bigserial primary key,
    user_id bigint unique not null references users(user_id),
    employee_code varchar(50) unique,
    department varchar(100),
    position varchar(100),
    salary decimal(15, 2),
    hiring_date date,
    is_active boolean default true,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table employees is 'Nhân viên, quan hệ 1-1 với users (user_id). Được tham chiếu bởi warranty_requests, maintenance_requests, employee_assignments, sales_records, chat_sessions, support_tickets. salary là dữ liệu nhạy cảm, nên hạn chế quyền truy cập.';

-- =====================================================
-- nhóm 7: bảo hành, bảo trì và đổi trả
-- =====================================================

-- bảng warranties: quản lý thông tin bảo hành sản phẩm
create table warranties (
    warranty_id bigserial primary key,
    order_item_id bigint unique not null references order_items(order_item_id),
    warranty_start_date date not null,
    warranty_end_date date not null,
    warranty_type varchar(50),
    is_active boolean default true,
    created_at timestamp default current_timestamp,
    -- [SỬA #5] ngày kết thúc không được trước ngày bắt đầu
    constraint chk_warranties_dates check (warranty_end_date >= warranty_start_date)
);
comment on table warranties is 'Bảo hành của 1 dòng sản phẩm đã bán, quan hệ 1-1 với order_items (order_item_id). Được tham chiếu bởi warranty_requests.';

-- bảng warranty_requests: quản lý yêu cầu bảo hành
create table warranty_requests (
    warranty_request_id bigserial primary key,
    warranty_id bigint not null references warranties(warranty_id),
    user_id bigint not null references users(user_id),
    issue_description text not null,
    status varchar(50) not null default 'pending',           -- [SỬA #1] thêm NOT NULL
    assigned_to_employee bigint references employees(employee_id), -- [SỬA #8] nhân viên xử lý (employee_id)
    estimated_completion_date date,
    completed_at timestamp,
    notes text,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table warranty_requests is 'Yêu cầu bảo hành. Nối tới warranties (warranty_id), users (user_id = người yêu cầu) và employees (assigned_to_employee = nhân viên xử lý).';

-- bảng maintenance_requests: quản lý yêu cầu bảo trì
create table maintenance_requests (
    maintenance_id bigserial primary key,
    user_id bigint not null references users(user_id),
    order_item_id bigint not null references order_items(order_item_id),
    maintenance_type varchar(100),
    description text not null,
    status varchar(50) not null default 'pending',           -- [SỬA #1] thêm NOT NULL
    assigned_to_employee bigint references employees(employee_id), -- [SỬA #8] nhân viên xử lý (employee_id)
    completion_date date,
    notes text,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table maintenance_requests is 'Yêu cầu bảo trì. Nối tới users (user_id = người yêu cầu), employees (assigned_to_employee = nhân viên xử lý) và order_items (order_item_id).';

-- bảng return_requests: quản lý yêu cầu đổi trả
create table return_requests (
    return_request_id bigserial primary key,
    order_id bigint not null references orders(order_id),
    user_id bigint not null references users(user_id),
    reason text not null,
    status varchar(50) not null default 'pending',           -- [SỬA #1] thêm NOT NULL
    refund_amount decimal(15, 2),
    approved_at timestamp,
    completed_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table return_requests is 'Yêu cầu đổi trả. Nối tới orders (order_id) và users (user_id). Được tham chiếu bởi return_items.';

-- bảng return_items: lưu chi tiết sản phẩm trong yêu cầu đổi trả
create table return_items (
    return_item_id bigserial primary key,
    return_request_id bigint not null references return_requests(return_request_id),
    order_item_id bigint not null references order_items(order_item_id),
    quantity int not null,
    refund_amount decimal(15, 2)
);
comment on table return_items is 'Chi tiết dòng sản phẩm trong 1 yêu cầu đổi trả. Nối tới return_requests (return_request_id) và order_items (order_item_id). Việc order_item_id thuộc đúng đơn của yêu cầu và quantity không vượt số đã mua do ứng dụng kiểm soát.';

-- =====================================================
-- nhóm 8: nhân sự và kho
-- (stores đã được tạo ở nhóm 3)
-- =====================================================

-- bảng employee_assignments: quản lý phân công nhân viên tại siêu thị
create table employee_assignments (
    assignment_id bigserial primary key,
    employee_id bigint not null references employees(employee_id),
    store_id int not null references stores(store_id),
    start_date date not null,
    end_date date,
    position_at_store varchar(100),
    is_active boolean default true
);
comment on table employee_assignments is 'Phân công nhân viên tại siêu thị. Nối tới employees (employee_id) và stores (store_id). Mỗi nhân viên tối đa 1 phân công đang hiệu lực (is_active và end_date null) — unique index một phần uq_employee_assignments_active; chuyển chi nhánh = đóng dòng cũ (end_date) rồi thêm dòng mới, giữ lịch sử.';

-- bảng sales_records: ghi nhận thông tin doanh số
create table sales_records (
    sales_id bigserial primary key,
    store_id int not null references stores(store_id),
    employee_id bigint not null references employees(employee_id),
    order_id bigint unique not null references orders(order_id),
    sales_amount decimal(15, 2) not null,
    commission decimal(15, 2),
    recorded_at timestamp default current_timestamp
);
comment on table sales_records is 'Doanh số bán hàng, quan hệ 1-1 với orders (order_id). Nối tới stores (store_id) và employees (employee_id) để biết đơn đó bán tại chi nhánh nào, do nhân viên nào.';

-- bảng inventory: tồn kho của từng phiên bản sản phẩm tại từng chi nhánh
create table inventory (
    store_id int not null references stores(store_id),
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null default 0 check (quantity >= 0),
    updated_at timestamp default current_timestamp,
    primary key (store_id, variant_id)
);
comment on table inventory is 'Số lượng mỗi phiên bản sản phẩm (product_variants) tồn tại mỗi chi nhánh (stores). Không theo imei, không có bảng chuyển kho giữa chi nhánh. Tổng tồn theo phiên bản / sản phẩm: v_variant_stock, v_product_stock.';

-- bảng stock_movements: sổ kho, mỗi lần tồn thay đổi ghi 1 dòng, chỉ thêm không sửa/xóa
create table stock_movements (
    movement_id bigserial primary key,
    store_id int not null references stores(store_id),
    variant_id bigint not null references product_variants(variant_id),
    movement_type varchar(20) not null check (movement_type in ('IN', 'OUT', 'ADJUSTMENT', 'RETURN')),
    quantity_change int not null, -- dương cho IN/RETURN, âm cho OUT, khác 0 cho ADJUSTMENT (được ép bởi chk_stock_movements_sign)
    supplier_name varchar(150), -- chữ tự do, chỉ có nghĩa với IN; không có bảng suppliers riêng, không có phiếu nhập nhiều dòng
    note text,
    order_id bigint references orders(order_id), -- có giá trị với OUT (trừ lúc xác nhận đơn) / RETURN (hoàn lúc hủy đơn đã xác nhận)
    created_by bigint references users(user_id), -- nhân viên thực hiện; null nếu hệ thống tự ghi
    created_at timestamp default current_timestamp,
    -- [SỬA #2] ép dấu của quantity_change theo loại biến động
    constraint chk_stock_movements_sign check (
        (movement_type in ('IN', 'RETURN') and quantity_change > 0)
        or (movement_type = 'OUT' and quantity_change < 0)
        or (movement_type = 'ADJUSTMENT' and quantity_change <> 0)
    )
);
comment on table stock_movements is 'Lịch sử thay đổi tồn kho theo từng chi nhánh (nhập hàng / bán hàng / hoàn kho / điều chỉnh). Nối tới stores, product_variants, orders (khi có), users (người thực hiện). Dấu quantity_change được ép theo movement_type (chk_stock_movements_sign). Chỉ thêm, không sửa/xóa — trigger trg_stock_movements_immutable chặn update và delete.';

-- =====================================================
-- nhóm 9: hệ thống khuyến nghị
-- =====================================================

-- bảng user_interactions: lưu dữ liệu tương tác của người dùng
create table user_interactions (
    interaction_id bigserial primary key,
    user_id bigint not null references users(user_id),
    product_id bigint not null references products(product_id),
    interaction_type varchar(50) not null,
    interaction_score int,
    timestamp timestamp default current_timestamp
);
comment on table user_interactions is 'Lịch sử tương tác (xem, thích, mua...) của người dùng với sản phẩm. Nối tới users (user_id) và products (product_id).';

-- bảng recommendations: lưu kết quả khuyến nghị sản phẩm
create table recommendations (
    recommendation_id bigserial primary key,
    user_id bigint not null references users(user_id),
    product_id bigint not null references products(product_id),
    recommendation_type varchar(50),
    score decimal(5, 3),
    reason text,
    created_at timestamp default current_timestamp
);
comment on table recommendations is 'Kết quả gợi ý sản phẩm. Nối tới users (user_id) và products (product_id).';

-- bảng product_relations: lưu quan hệ giữa các sản phẩm
create table product_relations (
    relation_id bigserial primary key,
    product_id bigint not null references products(product_id),
    related_product_id bigint not null references products(product_id),
    relation_type varchar(50),
    strength decimal(5, 3),
    check (product_id <> related_product_id)
);
comment on table product_relations is 'Quan hệ giữa 2 sản phẩm (ví dụ: sản phẩm tương tự). Nối tới products 2 lần (product_id và related_product_id); 1 sản phẩm không được tự liên kết với chính nó.';

-- =====================================================
-- nhóm 10: chăm sóc khách hàng và chatbot
-- =====================================================

-- bảng chat_sessions: quản lý các phiên hội thoại
create table chat_sessions (
    session_id bigserial primary key,
    user_id bigint not null references users(user_id),
    topic varchar(255),
    assigned_to_employee bigint references employees(employee_id), -- nhân viên xử lý (employee_id)
    status varchar(50) not null default 'open',              -- [SỬA #1] thêm NOT NULL
    created_at timestamp default current_timestamp,
    closed_at timestamp
);
comment on table chat_sessions is 'Phiên hội thoại (chatbot / hỗ trợ). Nối tới users (user_id) và employees (assigned_to_employee). Được tham chiếu bởi chat_messages.';

-- bảng chat_messages: lưu nội dung các tin nhắn trong hội thoại
create table chat_messages (
    message_id bigserial primary key,
    session_id bigint not null references chat_sessions(session_id),
    sender varchar(50) not null,
    content text not null,
    message_type varchar(50),
    timestamp timestamp default current_timestamp
);
comment on table chat_messages is 'Tin nhắn trong 1 phiên hội thoại. Nối tới chat_sessions (session_id).';

-- bảng support_tickets: quản lý các yêu cầu hỗ trợ khách hàng
create table support_tickets (
    ticket_id bigserial primary key,
    user_id bigint not null references users(user_id),
    employee_id bigint references employees(employee_id),
    subject varchar(255) not null,
    description text not null,
    status varchar(50) not null default 'open',              -- [SỬA #1] thêm NOT NULL
    priority varchar(50) not null default 'medium',          -- [SỬA #1] thêm NOT NULL
    resolved_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);
comment on table support_tickets is 'Yêu cầu hỗ trợ của khách hàng. Nối tới users (user_id = người gửi) và employees (employee_id = nhân viên xử lý).';

-- =====================================================
-- nhóm 11: đánh giá sản phẩm
-- thêm ngày 2026-10-03 (giai đoạn thiết kế, nằm chung trong file schema này)
-- =====================================================

-- bảng reviews: đánh giá sao + bình luận của 1 tài khoản cho 1 sản phẩm (mỗi tài khoản 1 đánh giá / sản phẩm)
create table reviews (
    review_id bigserial primary key,
    user_id bigint not null references users(user_id),
    product_id bigint not null references products(product_id),
    rating smallint not null check (rating between 1 and 5),
    comment text not null,                                  -- 10–2000 ký tự (chk_reviews_comment_length)
    is_hidden boolean not null default false,               -- admin ẩn: không hiện, không tính vào điểm trung bình
    hidden_reason text,
    hidden_by bigint references users(user_id),
    hidden_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,         -- khác created_at → hiện "đã chỉnh sửa"
    unique (user_id, product_id),
    check (not is_hidden or hidden_reason is not null),
    -- [SỬA #12] độ dài bình luận 10-2000 ký tự ép ở DB
    constraint chk_reviews_comment_length check (char_length(comment) between 10 and 2000)
);
comment on table reviews is 'Đánh giá sản phẩm (1–5 sao + bình luận). Nối tới users (user_id = người viết, hidden_by = admin ẩn) và products (product_id). Được tham chiếu bởi review_images. Nhãn "Đã mua hàng" không lưu cột: tính lúc đọc từ orders DELIVERED có order_items thuộc sản phẩm này. products.rating / total_reviews do ứng dụng tính lại từ các đánh giá không bị ẩn mỗi khi tạo / sửa / xoá / ẩn / hiện. Khác v3: v3 có order_item_id, bản này không. Không gắn trigger tự cập nhật updated_at cho bảng này, vì ẩn/hiện của admin không được tính là "đã chỉnh sửa".';

-- bảng review_images: ảnh khách gửi kèm đánh giá (tối đa 5 ảnh, ép bằng trigger trg_review_images_limit)
create table review_images (
    image_id bigserial primary key,
    review_id bigint not null references reviews(review_id) on delete cascade,
    image_url text not null,                                -- chỉ URL do server lưu: /uploads/reviews/<uuid>.(jpg|png|webp)
    display_order int not null,
    created_at timestamp default current_timestamp,
    -- [SỬA #6] không có 2 ảnh cùng thứ tự trong một đánh giá
    constraint uq_review_images_order unique (review_id, display_order)
);
comment on table review_images is 'Ảnh của một đánh giá. Nối tới reviews (review_id), xoá đánh giá thì ảnh xoá theo (ON DELETE CASCADE). Mỗi đánh giá không có 2 ảnh trùng display_order.';

-- =====================================================
-- nhóm 12a: indexes
-- postgresql không tự tạo index cho cột khóa ngoại, nên mọi khóa ngoại phía "nhiều"
-- đều được liệt kê ở đây (khóa chính và unique đã có index sẵn)
-- =====================================================

-- người dùng
create index idx_user_roles_role_id on user_roles(role_id);

-- danh mục và sản phẩm
create index idx_categories_parent_id on categories(parent_id);
create index idx_category_attributes_attribute_id on category_attributes(attribute_id);
create index idx_products_category_id on products(category_id);
create index idx_products_brand_id on products(brand_id);
create index idx_product_images_product_id on product_images(product_id);
create index idx_product_specs_product_id on product_specifications(product_id);
create index idx_product_variants_product_id on product_variants(product_id);
create index idx_variant_attr_values_value_id on variant_attribute_values(value_id);

-- khuyến mãi
create index idx_promotions_created_by on promotions(created_by);
create index idx_promotion_products_product_id on promotion_products(product_id);

-- giỏ hàng và đơn hàng
create index idx_cart_items_variant_id on cart_items(variant_id);
create index idx_orders_user_id on orders(user_id, order_date desc);
create index idx_orders_store_date on orders(store_id, order_date);   -- báo cáo theo chi nhánh
create index idx_orders_order_date on orders(order_date);
create index idx_order_items_order_id on order_items(order_id);
create index idx_order_items_variant_id on order_items(variant_id);
create index idx_order_items_promotion_id on order_items(promotion_id);

create index idx_order_status_history_order on order_status_history(order_id, changed_at);
create index idx_order_status_history_changed_by on order_status_history(changed_by);

-- thanh toán và trả góp
create index idx_installment_orders_reviewed_by on installment_orders(reviewed_by);
create index idx_payments_order_id on payments(order_id);
create index idx_payments_confirmed_by on payments(confirmed_by);
create index idx_payments_refunded_by on payments(refunded_by);

-- bảo hành, bảo trì, đổi trả
create index idx_warranty_requests_warranty_id on warranty_requests(warranty_id);
create index idx_warranty_requests_user_id on warranty_requests(user_id);
create index idx_warranty_requests_assigned on warranty_requests(assigned_to_employee);
create index idx_maintenance_requests_user_id on maintenance_requests(user_id);
create index idx_maintenance_requests_order_item_id on maintenance_requests(order_item_id);
create index idx_maintenance_requests_assigned on maintenance_requests(assigned_to_employee);
create index idx_return_requests_order_id on return_requests(order_id);
create index idx_return_requests_user_id on return_requests(user_id);
create index idx_return_items_request_id on return_items(return_request_id);
create index idx_return_items_order_item_id on return_items(order_item_id);

-- nhân sự và kho
create index idx_employee_assignments_employee_id on employee_assignments(employee_id);
create index idx_employee_assignments_store_id on employee_assignments(store_id);
create index idx_sales_records_store_id on sales_records(store_id);
create index idx_sales_records_employee_id on sales_records(employee_id);
create index idx_inventory_variant_id on inventory(variant_id);
create index idx_stock_movements_store_variant_time on stock_movements(store_id, variant_id, created_at);
create index idx_stock_movements_variant_id on stock_movements(variant_id);
create index idx_stock_movements_order_id on stock_movements(order_id);
create index idx_stock_movements_created_by on stock_movements(created_by);

-- khuyến nghị
create index idx_user_interactions_user_time on user_interactions(user_id, timestamp);
create index idx_user_interactions_product_id on user_interactions(product_id);
create index idx_recommendations_user_id on recommendations(user_id, created_at);
create index idx_recommendations_product_id on recommendations(product_id);
create index idx_product_relations_product_id on product_relations(product_id);
create index idx_product_relations_related_id on product_relations(related_product_id);

-- chăm sóc khách hàng
create index idx_chat_sessions_user_id on chat_sessions(user_id);
create index idx_chat_sessions_assigned on chat_sessions(assigned_to_employee);
create index idx_chat_messages_session_time on chat_messages(session_id, timestamp);
create index idx_support_tickets_user_id on support_tickets(user_id);
create index idx_support_tickets_employee_id on support_tickets(employee_id);

-- đánh giá
create index idx_reviews_product_id on reviews(product_id, created_at desc);
create index idx_reviews_hidden_by on reviews(hidden_by);
-- (review_id, display_order) đã có index từ unique constraint uq_review_images_order [SỬA #6]

-- tìm kiếm tên sản phẩm không phân biệt hoa thường và dấu tiếng việt
-- unaccent() không "immutable" nên không dùng trực tiếp trong index được, cần bọc lại
create or replace function f_unaccent(text) returns text
    language sql immutable parallel safe strict
    as $$ select public.unaccent('public.unaccent', $1) $$;

-- truy vấn phải dùng đúng biểu thức này mới dùng được index:
--   where f_unaccent(lower(name)) like '%' || f_unaccent(lower('dien thoai')) || '%'
create index idx_products_name_trgm on products
    using gin (f_unaccent(lower(name)) gin_trgm_ops);

-- =====================================================
-- nhóm 12b: unique index một phần và check an toàn
-- =====================================================

-- mỗi nhân viên tối đa 1 phân công đang hiệu lực
create unique index uq_employee_assignments_active
    on employee_assignments (employee_id) where is_active and end_date is null;

-- mỗi sản phẩm tối đa 1 ảnh chính
create unique index uq_product_primary_image
    on product_images (product_id) where is_primary;

-- mỗi đơn có tối đa 1 khoản thanh toán chính còn hiệu lực (khoản thu của kỳ trả góp không tính)
create unique index uq_payments_main_per_order
    on payments (order_id) where installment_payment_id is null and status <> 'CANCELLED';

-- số lượng phải dương
alter table cart_items add constraint chk_cart_item_quantity_positive check (quantity > 0);
alter table return_items add constraint chk_return_item_quantity_positive check (quantity > 0);

-- giá giảm không được cao hơn giá gốc
alter table product_variants add constraint chk_variant_discount_price
    check (discount_price is null or discount_price <= price);

-- [SỬA #10] check biên cho các cột dẫn xuất do ứng dụng cập nhật
alter table products add constraint chk_products_rating check (rating between 0 and 5);
alter table products add constraint chk_products_counts_non_negative
    check (total_reviews >= 0 and view_count >= 0);
alter table customer_profiles add constraint chk_customer_profiles_non_negative
    check (loyalty_points >= 0 and total_spent >= 0);

-- check giá trị cho các cột trạng thái (so sánh không phân biệt hoa thường)
-- (các cột này đã NOT NULL ở phần tạo bảng, nên check không còn bị NULL lọt qua) [SỬA #1]
alter table orders add constraint chk_orders_status
    check (lower(status) in ('pending', 'confirmed', 'processing', 'shipping',
                             'delivered', 'completed', 'cancelled', 'returned'));
alter table warranty_requests add constraint chk_warranty_requests_status
    check (lower(status) in ('pending', 'received', 'in_progress', 'completed', 'rejected', 'cancelled'));
alter table maintenance_requests add constraint chk_maintenance_requests_status
    check (lower(status) in ('pending', 'received', 'in_progress', 'completed', 'rejected', 'cancelled'));
alter table return_requests add constraint chk_return_requests_status
    check (lower(status) in ('pending', 'approved', 'rejected', 'completed', 'cancelled'));
alter table chat_sessions add constraint chk_chat_sessions_status
    check (lower(status) in ('open', 'closed'));
alter table support_tickets add constraint chk_support_tickets_status
    check (lower(status) in ('open', 'in_progress', 'resolved', 'closed'));
alter table support_tickets add constraint chk_support_tickets_priority
    check (lower(priority) in ('low', 'medium', 'high', 'urgent'));

-- =====================================================
-- nhóm 12c: trigger
-- =====================================================

-- tự cập nhật updated_at khi sửa dòng
create or replace function set_updated_at() returns trigger
language plpgsql as $$
begin
    new.updated_at := current_timestamp;
    return new;
end $$;

-- gắn trigger updated_at cho mọi bảng có cột updated_at, trừ reviews [SỬA #12]
-- (reviews.updated_at khác created_at nghĩa là khách đã chỉnh sửa, ứng dụng tự ghi;
--  nếu gắn trigger thì mỗi lần admin ẩn / hiện đánh giá cũng bị tính là "đã chỉnh sửa")
-- hàm chạy lại được nhiều lần: bảng thêm sau này chỉ cần gọi  select attach_updated_at_triggers();
create or replace function attach_updated_at_triggers() returns void
language plpgsql as $$
declare
    t text;
begin
    for t in
        select c.table_name
        from information_schema.columns c
        join information_schema.tables tb
          on tb.table_schema = c.table_schema and tb.table_name = c.table_name
        where c.table_schema = 'public'
          and c.column_name = 'updated_at'
          and tb.table_type = 'BASE TABLE'
          and c.table_name <> 'reviews'
    loop
        execute format('drop trigger if exists trg_%s_updated_at on %I', t, t);
        execute format(
            'create trigger trg_%s_updated_at before update on %I
             for each row execute function set_updated_at()', t, t);
    end loop;
end $$;

select attach_updated_at_triggers();

-- sổ kho chỉ được thêm dòng mới
create or replace function forbid_stock_movement_change() returns trigger
language plpgsql as $$
begin
    raise exception 'stock_movements là sổ kho chỉ ghi thêm, không được sửa hoặc xóa';
end $$;

create trigger trg_stock_movements_immutable
before update or delete on stock_movements
for each row execute function forbid_stock_movement_change();

-- [SỬA #11] return_items: dòng trả phải thuộc đúng đơn của yêu cầu, và tổng số lượng trả
-- (không tính yêu cầu bị từ chối / hủy) không vượt số lượng đã mua
-- (lưu ý: kiểm tra trong trigger không chặn được hai giao dịch chạy song song cùng lúc)
create or replace function check_return_item() returns trigger
language plpgsql as $$
declare
    v_request_order bigint;
    v_item_order bigint;
    v_bought int;
    v_returned int;
begin
    select order_id into v_request_order
    from return_requests where return_request_id = new.return_request_id;

    select order_id, quantity into v_item_order, v_bought
    from order_items where order_item_id = new.order_item_id;

    if v_request_order is distinct from v_item_order then
        raise exception 'return_items: order_item % không thuộc đơn hàng % của yêu cầu đổi trả %',
            new.order_item_id, v_request_order, new.return_request_id;
    end if;

    select coalesce(sum(ri.quantity), 0) into v_returned
    from return_items ri
    join return_requests rr on rr.return_request_id = ri.return_request_id
    where ri.order_item_id = new.order_item_id
      and ri.return_item_id is distinct from new.return_item_id
      and lower(rr.status) not in ('rejected', 'cancelled');

    if v_returned + new.quantity > v_bought then
        raise exception 'return_items: tổng số lượng trả (%) vượt số lượng đã mua (%) của order_item %',
            v_returned + new.quantity, v_bought, new.order_item_id;
    end if;
    return new;
end $$;

create trigger trg_return_items_check
before insert or update of return_request_id, order_item_id, quantity on return_items
for each row execute function check_return_item();

-- [SỬA #11] sales_records.store_id phải trùng orders.store_id (khi đơn có store_id)
create or replace function check_sales_record_store() returns trigger
language plpgsql as $$
declare
    v_order_store int;
begin
    select store_id into v_order_store from orders where order_id = new.order_id;
    if v_order_store is not null and v_order_store <> new.store_id then
        raise exception 'sales_records: store_id % khác chi nhánh xử lý % của đơn %',
            new.store_id, v_order_store, new.order_id;
    end if;
    return new;
end $$;

create trigger trg_sales_records_check_store
before insert or update of store_id, order_id on sales_records
for each row execute function check_sales_record_store();

-- [SỬA #12] mỗi đánh giá tối đa 5 ảnh
create or replace function check_review_image_limit() returns trigger
language plpgsql as $$
begin
    if (select count(*) from review_images where review_id = new.review_id) >= 5 then
        raise exception 'review_images: mỗi đánh giá tối đa 5 ảnh (review_id = %)', new.review_id;
    end if;
    return new;
end $$;

create trigger trg_review_images_limit
before insert on review_images
for each row execute function check_review_image_limit();

-- [SỬA #13] tự ghi lịch sử trạng thái đơn hàng
create or replace function log_order_status_change() returns trigger
language plpgsql as $$
declare
    v_user bigint := nullif(current_setting('app.current_user_id', true), '')::bigint;
begin
    if tg_op = 'INSERT' then
        insert into order_status_history (order_id, old_status, new_status, changed_by)
        values (new.order_id, null, new.status, v_user);
    elsif new.status is distinct from old.status then
        insert into order_status_history (order_id, old_status, new_status, changed_by)
        values (new.order_id, old.status, new.status, v_user);
    end if;
    return null;
end $$;

create trigger trg_orders_status_history
after insert or update of status on orders
for each row execute function log_order_status_change();

-- =====================================================
-- nhóm 12d: views
-- =====================================================

-- tồn kho theo phiên bản (cộng dồn mọi chi nhánh)
create view v_variant_stock as
select
    v.variant_id,
    v.product_id,
    coalesce(sum(i.quantity), 0) as total_quantity
from product_variants v
left join inventory i on i.variant_id = v.variant_id
group by v.variant_id, v.product_id;

-- tồn kho theo sản phẩm (cộng dồn mọi phiên bản, mọi chi nhánh)
-- [SỬA #7] left join từ products: sản phẩm chưa có phiên bản vẫn hiện, tổng tồn = 0
create view v_product_stock as
select
    p.product_id,
    coalesce(sum(vs.total_quantity), 0) as total_quantity
from products p
left join v_variant_stock vs on vs.product_id = p.product_id
group by p.product_id;

-- [SỬA #10] đối soát inventory với sổ kho: liệt kê dòng mà tồn hiện tại khác tổng quantity_change
-- (rỗng = khớp; chỉ có nghĩa nếu mọi thay đổi tồn đều có dòng trong stock_movements và tồn bắt đầu từ 0)
create view v_inventory_mismatch as
select
    coalesce(i.store_id, m.store_id)     as store_id,
    coalesce(i.variant_id, m.variant_id) as variant_id,
    coalesce(i.quantity, 0)              as inventory_quantity,
    coalesce(m.net_change, 0)            as movements_total
from inventory i
full join (
    select store_id, variant_id, sum(quantity_change) as net_change
    from stock_movements
    group by store_id, variant_id
) m on m.store_id = i.store_id and m.variant_id = i.variant_id
where coalesce(i.quantity, 0) <> coalesce(m.net_change, 0);

-- =====================================================
-- end of schema
-- =====================================================