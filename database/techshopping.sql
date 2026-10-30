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

-- bảng users: lưu thông tin tài khoản người dùng
create table users (
    user_id bigserial primary key,
    email varchar(120) unique not null,
    username varchar(50) unique not null,
    password_hash varchar(255) not null,
    fullname varchar(120) not null,
    phone varchar(20),
    avatar_url text,
    is_active boolean default true,
    must_change_password boolean not null default false,
    password_expires_at timestamp,
    last_login timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    deleted_at timestamp
);

-- bảng user_roles: liên kết người dùng với vai trò
create table user_roles (
    user_id bigint not null references users(user_id) on delete cascade,
    role_id int not null references roles(role_id) on delete cascade,
    assigned_at timestamp default current_timestamp,
    primary key (user_id, role_id)
);

-- bảng loyalty_tiers: 4 hạng thành viên, ADMIN sửa ngưỡng tổng chi tiêu + hệ số nhân điểm
create table loyalty_tiers (
    tier_code varchar(20) primary key,
    name varchar(50) not null,
    min_spent decimal(15, 2) not null,
    point_multiplier decimal(4, 2) not null,
    sort_order int not null unique,
    updated_at timestamp not null default current_timestamp,
    constraint chk_loyalty_tiers_code check (tier_code in ('MEMBER', 'SILVER', 'GOLD', 'DIAMOND')),
    constraint chk_loyalty_tiers_min check (min_spent >= 0),
    constraint chk_loyalty_tiers_multiplier check (point_multiplier >= 1 and point_multiplier <= 5)
);

insert into loyalty_tiers (tier_code, name, min_spent, point_multiplier, sort_order) values
    ('MEMBER', 'Thành viên', 0, 1.00, 1),
    ('SILVER', 'Bạc', 50000000, 1.20, 2),
    ('GOLD', 'Vàng', 150000000, 1.50, 3),
    ('DIAMOND', 'Kim cương', 300000000, 2.00, 4);

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
    loyalty_points int not null default 0,
    total_spent decimal(15, 2) not null default 0.00,
    tier_code varchar(20) not null default 'MEMBER' references loyalty_tiers(tier_code),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

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

-- bảng products: quản lý thông tin sản phẩm
create table products (
    product_id bigserial primary key,
    name varchar(255) not null,
    slug varchar(255) unique not null,
    description text,
    brand_id int references brands(brand_id),
    category_id int not null references categories(category_id),
    base_price decimal(15, 2) not null,
    discount_price decimal(15, 2),
    stock_quantity int default 0,
    sku varchar(50) unique,
    weight decimal(10, 2),
    warranty_months int default 12,
    rating decimal(3, 2) default 0,          -- điểm trung bình đánh giá thật (bảng reviews, đang hiện)
    total_reviews int default 0,             -- số đánh giá thật đang hiện
    view_count int default 0,
    is_active boolean default true,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    deleted_at timestamp
);

-- bảng product_images: lưu hình ảnh sản phẩm
create table product_images (
    image_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    image_url text not null,
    alt_text varchar(255),
    display_order int,
    is_primary boolean default false,
    uploaded_at timestamp default current_timestamp,
    color varchar(50)
);

create index idx_product_images_product_color on product_images (product_id, color);

-- bảng product_specifications: lưu thông số kỹ thuật của sản phẩm
create table product_specifications (
    specification_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    spec_name varchar(100) not null,
    spec_value text not null,
    spec_order int,
    variant_id bigint
);

-- bảng product_variants: quản lý các phiên bản sản phẩm
create table product_variants (
    variant_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    variant_name varchar(255) not null,
    sku_variant varchar(50) unique,
    price decimal(15, 2) not null,
    discount_price decimal(15, 2),
    stock_quantity int default 0,
    color varchar(50),
    storage varchar(50),
    ram varchar(50),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- thông số riêng của phiên bản (NULL = thông số chung của sản phẩm); product_variants đứng sau product_specifications nên FK thêm ở đây
alter table product_specifications add constraint product_specifications_variant_id_fkey
    foreign key (variant_id) references product_variants(variant_id) on delete cascade;
create index idx_product_specifications_variant on product_specifications (variant_id) where variant_id is not null;

-- bộ lọc thông số nâng cao + nhu cầu (migration 25 filters_needs.sql)
create table product_filter_values (
    filter_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    group_key varchar(30) not null,
    label varchar(60) not null,
    sort_order int not null default 0,
    constraint uq_product_filter_values unique (product_id, group_key, label)
);

create index idx_product_filter_values_lookup on product_filter_values (group_key, label, product_id);

create table needs (
    need_id serial primary key,
    category_id int not null references categories(category_id) on delete cascade,
    slug varchar(60) not null,
    name varchar(100) not null,
    display_order int not null default 0,
    constraint uq_needs_category_slug unique (category_id, slug)
);

create table product_needs (
    product_need_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    need_id int not null references needs(need_id) on delete cascade,
    constraint uq_product_needs unique (product_id, need_id)
);

create index idx_product_needs_need on product_needs (need_id, product_id);


-- bảng attributes: quản lý thuộc tính sản phẩm
create table attributes (
    attribute_id serial primary key,
    name varchar(100) not null unique,
    description text,
    attribute_type varchar(50)
);

-- bảng attribute_values: lưu các giá trị của thuộc tính
create table attribute_values (
    value_id serial primary key,
    attribute_id int not null references attributes(attribute_id),
    value varchar(255) not null,
    unique(attribute_id, value)
);

-- bảng category_attributes: danh mục nào dùng những thuộc tính nào (sinh form nhập sản phẩm + bộ lọc)
create table category_attributes (
    category_id int not null references categories(category_id) on delete cascade,
    attribute_id int not null references attributes(attribute_id) on delete cascade,
    is_required boolean not null default true,
    is_filterable boolean not null default true,
    display_order int,
    primary key (category_id, attribute_id)
);

-- bảng variant_attribute_values: liên kết phiên bản sản phẩm với giá trị thuộc tính
create table variant_attribute_values (
    variant_id bigint not null references product_variants(variant_id) on delete cascade,
    value_id int not null references attribute_values(value_id) on delete cascade,
    primary key (variant_id, value_id)
);

-- =====================================================
-- nhóm 3: giỏ hàng và mua hàng
-- =====================================================

-- bảng carts: quản lý giỏ hàng của người dùng
create table carts (
    cart_id bigserial primary key,
    user_id bigint unique not null references users(user_id) on delete cascade,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng cart_items: lưu các sản phẩm trong giỏ hàng
create table cart_items (
    cart_item_id bigserial primary key,
    cart_id bigint not null references carts(cart_id) on delete cascade,
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null,
    added_at timestamp default current_timestamp,
    unique(cart_id, variant_id)
);

-- bảng orders: quản lý đơn hàng
create table orders (
    order_id bigserial primary key,
    user_id bigint not null references users(user_id),
    order_date timestamp default current_timestamp,
    recipient_name varchar(120) not null,
    recipient_phone varchar(20) not null,
    shipping_address text not null,
    shipping_province varchar(100),
    shipping_ward varchar(100),
    billing_address text,
    shipping_cost decimal(15, 2) default 0,
    tax_amount decimal(15, 2) default 0,
    total_amount decimal(15, 2) not null,
    status varchar(50) default 'pending',
    payment_method varchar(50),
    tracking_number varchar(100),
    notes text,
    delivered_at timestamp,
    cancelled_at timestamp,
    store_id int,
    delivery_type varchar(20) not null default 'HOME_DELIVERY',
    voucher_id bigint,
    voucher_code varchar(30),
    voucher_discount decimal(15, 2) not null default 0,
    combo_discount decimal(15, 2) not null default 0,
    points_used int not null default 0,
    points_discount decimal(15, 2) not null default 0,
    points_earned int not null default 0,
    contact_email varchar(255),
    tracking_token varchar(64) not null default (md5(random()::text || clock_timestamp()::text)
                                                 || md5(random()::text || clock_timestamp()::text)),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create unique index uq_orders_tracking_token on orders(tracking_token);

-- bảng email_log: mail đơn hàng đã gửi / thất bại
create table email_log (
    email_log_id bigserial primary key,
    order_id bigint references orders(order_id) on delete set null,
    kind varchar(30) not null,
    recipient varchar(255) not null,
    subject varchar(255) not null,
    status varchar(10) not null check (status in ('SENT', 'FAILED', 'SKIPPED')),
    error varchar(500),
    created_at timestamp not null default current_timestamp
);

-- bảng wishlist_items: sản phẩm khách yêu thích; mốc giá để báo giảm giá qua email
create table wishlist_items (
    wishlist_item_id bigserial primary key,
    user_id bigint not null references users(user_id),
    product_id bigint not null references products(product_id),
    price_at_add decimal(15, 2),
    last_alert_price decimal(15, 2),
    created_at timestamp not null default current_timestamp,
    unique (user_id, product_id)
);

create index idx_wishlist_items_product on wishlist_items(product_id);

create index idx_email_log_order on email_log(order_id);
create index idx_email_log_created on email_log(created_at);

-- bảng order_items: lưu chi tiết sản phẩm trong đơn hàng
create table order_items (
    order_item_id bigserial primary key,
    order_id bigint not null references orders(order_id) on delete cascade,
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null,
    unit_price decimal(15, 2) not null,
    discount_amount decimal(15, 2) default 0,
    subtotal decimal(15, 2) not null,
    voucher_discount decimal(15, 2) not null default 0,
    combo_discount decimal(15, 2) not null default 0,
    points_discount decimal(15, 2) not null default 0
);

-- bảng order_status_history: lịch sử đổi trạng thái đơn hàng, ghi bằng code java mỗi lần orders.status đổi
create table order_status_history (
    history_id bigserial primary key,
    order_id bigint not null references orders(order_id) on delete cascade,
    old_status varchar(50),
    new_status varchar(50) not null,
    changed_by bigint references users(user_id),
    changed_at timestamp not null default current_timestamp
);

-- =====================================================
-- nhóm 4: thanh toán và trả góp
-- =====================================================

-- bảng installment_partners: đối tác trả góp (thẻ tín dụng, công ty tài chính) và điều kiện của từng đối tác
create table installment_partners (
    partner_code varchar(30) primary key,
    partner_name varchar(100) not null,
    partner_type varchar(30) not null,
    min_order_total decimal(15, 2) not null default 3000000,
    max_order_total decimal(15, 2),
    is_active boolean not null default true,
    display_order int not null default 0
);

-- bảng installment_fee_rates: phí đối tác (% giá trị đơn) theo kỳ hạn; không có dòng = không hỗ trợ kỳ hạn đó
create table installment_fee_rates (
    partner_code varchar(30) not null references installment_partners(partner_code) on delete cascade,
    num_months int not null,
    fee_percent decimal(5, 2) not null,
    primary key (partner_code, num_months)
);

-- bảng installment_orders: hồ sơ trả góp của đơn hàng (đối tác xử lý, phí và tiền thực nhận, đối soát)
create table installment_orders (
    installment_id bigserial primary key,
    order_id bigint unique not null references orders(order_id),
    num_months int not null,
    monthly_payment decimal(15, 2) not null,
    interest_rate decimal(5, 2),
    total_interest decimal(15, 2),
    status varchar(50) default 'PENDING_APPROVAL',
    citizen_id varchar(20) not null,
    card_bank_code varchar(20),
    rejection_reason text,
    reviewed_by bigint references users(user_id),
    reviewed_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    partner_code varchar(30) references installment_partners(partner_code),
    partner_ref varchar(60),
    fee_percent decimal(5, 2),
    fee_amount decimal(15, 2),
    net_amount decimal(15, 2),
    settled_at timestamp,
    settled_by bigint references users(user_id),
    attempt_count int not null default 1,
    last_event_id varchar(80)
);

-- bảng installment_payments: quản lý các kỳ thanh toán trả góp
create table installment_payments (
    installment_payment_id bigserial primary key,
    installment_id bigint not null references installment_orders(installment_id),
    payment_number int not null,
    amount decimal(15, 2) not null,
    due_date date not null,
    paid_date date,
    status varchar(50) default 'PENDING',
    unique(installment_id, payment_number)
);

-- bảng payments: quản lý các giao dịch thanh toán (khoản chính của đơn, hoặc khoản thu của 1 kỳ trả góp)
create table payments (
    payment_id bigserial primary key,
    order_id bigint not null references orders(order_id),
    amount decimal(15, 2) not null,
    payment_method varchar(50) not null,
    transaction_id varchar(100),
    status varchar(50) default 'PENDING',
    paid_at timestamp,
    confirmed_by bigint references users(user_id),
    refunded_at timestamp,
    refunded_by bigint references users(user_id),
    installment_payment_id bigint unique references installment_payments(installment_payment_id),
    checkout_session_id varchar(100),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng bank_transactions: biến động số dư từ webhook ngân hàng (tự xác nhận đơn chuyển khoản + đối soát)
create table bank_transactions (
    bank_transaction_id bigserial primary key,
    external_id varchar(100) not null,
    provider varchar(30) not null default 'WEBHOOK',
    amount decimal(15, 2) not null,
    content varchar(500),
    account_number varchar(30),
    transferred_at timestamp,
    received_at timestamp not null default current_timestamp,
    status varchar(20) not null,
    reason varchar(255),
    order_id bigint references orders(order_id),
    resolved_by bigint references users(user_id),
    resolved_at timestamp,
    note varchar(500),
    constraint uq_bank_transactions_external unique (external_id),
    constraint chk_bank_transactions_status check (status in ('MATCHED', 'UNMATCHED', 'REVIEW', 'RESOLVED', 'IGNORED')),
    constraint chk_bank_transactions_amount check (amount > 0)
);

create index idx_bank_transactions_status_received on bank_transactions(status, received_at);
create index idx_bank_transactions_order on bank_transactions(order_id);

-- =====================================================
-- nhóm 5: bảo hành, bảo trì và đổi trả
-- =====================================================

-- bảng warranties: quản lý thông tin bảo hành sản phẩm
create table warranties (
    warranty_id bigserial primary key,
    order_item_id bigint unique not null references order_items(order_item_id),
    warranty_start_date date not null,
    warranty_end_date date not null,
    warranty_type varchar(50),
    is_active boolean default true,
    created_at timestamp default current_timestamp
);

-- bảng warranty_requests: quản lý yêu cầu bảo hành
create table warranty_requests (
    warranty_request_id bigserial primary key,
    warranty_id bigint not null references warranties(warranty_id),
    user_id bigint not null references users(user_id),
    issue_description text not null,
    status varchar(50) not null default 'PENDING',
    assigned_to_employee bigint references users(user_id),
    estimated_completion_date date,
    completed_at timestamp,
    notes text,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    received_at timestamp,
    rejection_reason text,
    cancelled_at timestamp
);

-- bảng maintenance_requests: quản lý yêu cầu bảo trì
create table maintenance_requests (
    maintenance_id bigserial primary key,
    user_id bigint not null references users(user_id),
    order_item_id bigint not null references order_items(order_item_id),
    maintenance_type varchar(100),
    description text not null,
    status varchar(50) not null default 'PENDING',
    assigned_to_employee bigint references users(user_id),
    completion_date date,
    notes text,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    estimated_completion_date date,
    estimated_cost decimal(15, 2),
    actual_cost decimal(15, 2),
    received_at timestamp,
    rejection_reason text,
    cancelled_at timestamp
);

-- bảng return_requests: quản lý yêu cầu đổi trả
create table return_requests (
    return_request_id bigserial primary key,
    order_id bigint not null references orders(order_id),
    user_id bigint not null references users(user_id),
    reason text not null,
    status varchar(50) not null default 'PENDING',
    refund_amount decimal(15, 2),
    approved_at timestamp,
    completed_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    reason_type varchar(30),
    assigned_to_employee bigint references users(user_id),
    received_at timestamp,
    rejection_reason text,
    cancelled_at timestamp
);

-- bảng return_items: lưu chi tiết sản phẩm trong yêu cầu đổi trả
create table return_items (
    return_item_id bigserial primary key,
    return_request_id bigint not null references return_requests(return_request_id),
    order_item_id bigint not null references order_items(order_item_id),
    quantity int not null,
    refund_amount decimal(15, 2),
    -- null = chưa nhận hàng; true = đã cộng lại tồn kho chi nhánh
    restocked boolean
);

-- bảng service_request_images: ảnh khách đính kèm (tối đa 5 / yêu cầu); mỗi ảnh thuộc đúng 1 loại yêu cầu
create table service_request_images (
    image_id bigserial primary key,
    warranty_request_id bigint references warranty_requests(warranty_request_id) on delete cascade,
    maintenance_id bigint references maintenance_requests(maintenance_id) on delete cascade,
    return_request_id bigint references return_requests(return_request_id) on delete cascade,
    image_url text not null,
    display_order int not null,
    created_at timestamp default current_timestamp,
    constraint chk_service_request_images_owner
        check (num_nonnulls(warranty_request_id, maintenance_id, return_request_id) = 1)
);

-- =====================================================
-- nhóm 6: siêu thị và nhân sự
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

-- bảng employees: quản lý thông tin nhân viên
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

-- bảng inventory: tồn kho của từng phiên bản sản phẩm tại từng chi nhánh
create table inventory (
    store_id int not null references stores(store_id),
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null default 0,
    updated_at timestamp default current_timestamp,
    primary key (store_id, variant_id)
);

-- bảng stock_movements: sổ kho, mỗi lần tồn thay đổi ghi 1 dòng (nhập hàng / bán hàng lúc xác nhận đơn /
-- hoàn kho khi hủy đơn đã xác nhận); không có phiếu nhập nhiều dòng, không theo imei
create table stock_movements (
    movement_id bigserial primary key,
    store_id int not null references stores(store_id),
    variant_id bigint not null references product_variants(variant_id),
    movement_type varchar(20) not null,
    quantity_change int not null,
    supplier_name varchar(150),
    note text,
    order_id bigint references orders(order_id),
    created_by bigint references users(user_id),
    created_at timestamp default current_timestamp
);

-- =====================================================
-- nhóm 7: hệ thống khuyến nghị
-- =====================================================

-- bảng user_interactions: lưu dữ liệu tương tác của người dùng
create table user_interactions (
    interaction_id bigserial primary key,
    user_id bigint not null references users(user_id),
    product_id bigint not null references products(product_id),
    interaction_type varchar(50) not null
        constraint chk_user_interactions_type
        check (interaction_type in ('VIEW', 'CLICK', 'LIKE', 'ADD_TO_CART', 'PURCHASE')),
    interaction_score int,
    timestamp timestamp default current_timestamp
);

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

-- bảng product_relations: lưu quan hệ giữa các sản phẩm
create table product_relations (
    relation_id bigserial primary key,
    product_id bigint not null references products(product_id),
    related_product_id bigint not null references products(product_id),
    relation_type varchar(50),
    strength decimal(5, 3),
    check (product_id <> related_product_id)
);

-- =====================================================
-- nhóm 8: chăm sóc khách hàng và chatbot
-- =====================================================

-- bảng chat_sessions: quản lý các phiên hội thoại
create table chat_sessions (
    session_id bigserial primary key,
    -- null: khách chưa đăng nhập (chỉ hỏi câu chung)
    user_id bigint references users(user_id),
    -- trình duyệt giữ token ngẫu nhiên này thay cho session_id tăng dần
    session_token varchar(64) unique,
    topic varchar(255),
    assigned_to_employee bigint references employees(employee_id),
    status varchar(50) default 'open',
    created_at timestamp default current_timestamp,
    last_message_at timestamp,
    closed_at timestamp
);

-- bảng chat_messages: lưu nội dung các tin nhắn trong hội thoại
create table chat_messages (
    message_id bigserial primary key,
    session_id bigint not null references chat_sessions(session_id),
    sender varchar(50) not null,
    content text not null,
    message_type varchar(50),
    timestamp timestamp default current_timestamp
);

-- bảng support_tickets: quản lý các yêu cầu hỗ trợ khách hàng
create table support_tickets (
    ticket_id bigserial primary key,
    user_id bigint not null references users(user_id),
    employee_id bigint references employees(employee_id),
    subject varchar(255) not null,
    description text not null,
    status varchar(50) default 'open',
    priority varchar(50) default 'medium',
    resolved_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng contact_requests: tin nhắn form Liên hệ (khách có thể chưa đăng nhập); nhân viên / quản lý chi nhánh xử lý
create table contact_requests (
    contact_request_id bigserial primary key,
    user_id bigint references users(user_id),
    full_name varchar(100) not null,
    email varchar(255) not null,
    phone varchar(20),
    topic varchar(30) not null,
    message text not null,
    status varchar(20) not null default 'NEW',
    staff_note text,
    handled_by bigint references users(user_id),
    handled_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- =====================================================
-- nhóm 9: khuyến mãi
-- =====================================================

-- bảng promotions: chương trình khuyến mãi (có thời hạn, mức giảm mặc định)
create table promotions (
    promotion_id bigserial primary key,
    name varchar(255) not null,
    description text,
    discount_type varchar(20) not null,
    discount_value decimal(15, 2) not null,
    max_discount_amount decimal(15, 2),
    start_date timestamp not null,
    end_date timestamp not null,
    is_active boolean not null default true,
    created_by bigint not null references users(user_id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

-- bảng promotion_products: sản phẩm tham gia chương trình, có thể ghi đè mức giảm riêng
create table promotion_products (
    promotion_id bigint not null references promotions(promotion_id) on delete cascade,
    product_id bigint not null references products(product_id) on delete cascade,
    discount_type varchar(20),
    discount_value decimal(15, 2),
    created_at timestamp not null default current_timestamp,
    primary key (promotion_id, product_id)
);

-- bảng vouchers: mã giảm % cho đơn hàng, đơn tối thiểu, giảm tối đa, lượt dùng, phạm vi danh mục
create table vouchers (
    voucher_id bigserial primary key,
    code varchar(30) not null unique,
    name varchar(150) not null,
    description text,
    discount_percent decimal(5, 2) not null,
    max_discount decimal(15, 2),
    min_order_amount decimal(15, 2) not null default 0,
    starts_at timestamp not null,
    ends_at timestamp not null,
    usage_limit int,
    per_customer_limit int not null default 1,
    used_count int not null default 0,
    all_categories boolean not null default true,
    is_active boolean not null default true,
    created_by bigint not null references users(user_id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

-- bảng voucher_categories: danh mục áp dụng khi vouchers.all_categories = false
create table voucher_categories (
    voucher_id bigint not null references vouchers(voucher_id) on delete cascade,
    category_id int not null references categories(category_id) on delete cascade,
    primary key (voucher_id, category_id)
);

-- bảng voucher_usages: mỗi đơn dùng voucher một dòng; đơn huỷ trước khi giao → released_at (trả lượt)
create table voucher_usages (
    usage_id bigserial primary key,
    voucher_id bigint not null references vouchers(voucher_id),
    user_id bigint not null references users(user_id),
    order_id bigint not null unique references orders(order_id) on delete cascade,
    discount_amount decimal(15, 2) not null,
    used_at timestamp not null default current_timestamp,
    released_at timestamp
);

-- bảng combos: mua kèm giảm giá = 1 sản phẩm chính + món mua kèm, tự áp khi giỏ đủ món
create table combos (
    combo_id bigserial primary key,
    name varchar(150) not null,
    description text,
    main_product_id bigint not null references products(product_id),
    starts_at timestamp not null,
    ends_at timestamp not null,
    is_active boolean not null default true,
    created_by bigint not null references users(user_id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

-- bảng combo_items: món mua kèm + mức giảm riêng (PERCENT hoặc AMOUNT trên mỗi sản phẩm)
create table combo_items (
    combo_id bigint not null references combos(combo_id) on delete cascade,
    product_id bigint not null references products(product_id),
    discount_type varchar(10) not null,
    discount_value decimal(15, 2) not null,
    primary key (combo_id, product_id)
);

-- bảng order_combos: combo đã áp cho đơn (tên chụp lại) để báo cáo theo combo
create table order_combos (
    order_combo_id bigserial primary key,
    order_id bigint not null references orders(order_id) on delete cascade,
    combo_id bigint not null references combos(combo_id),
    combo_name varchar(150) not null,
    units int not null,
    discount_amount decimal(15, 2) not null
);

-- bảng loyalty_transactions: lịch sử điểm tích luỹ (+ / −) kèm số dư sau giao dịch
create table loyalty_transactions (
    loyalty_transaction_id bigserial primary key,
    user_id bigint not null references users(user_id) on delete cascade,
    order_id bigint references orders(order_id) on delete cascade,
    return_request_id bigint references return_requests(return_request_id) on delete set null,
    type varchar(20) not null,
    points int not null,
    balance_after int not null,
    note varchar(255),
    created_at timestamp not null default current_timestamp,
    constraint chk_loyalty_transactions_type check (type in ('EARN', 'REDEEM', 'REDEEM_BACK', 'REVOKE', 'ADJUST'))
);
create index idx_loyalty_transactions_user on loyalty_transactions(user_id, loyalty_transaction_id desc);
create index idx_loyalty_transactions_order on loyalty_transactions(order_id);

-- =====================================================
-- nhóm 10: đánh giá sản phẩm
-- =====================================================

-- bảng reviews: đánh giá sao + bình luận của 1 tài khoản cho 1 sản phẩm (mỗi tài khoản 1 đánh giá / sản phẩm)
create table reviews (
    review_id bigserial primary key,
    user_id bigint not null references users(user_id),
    product_id bigint not null references products(product_id),
    rating smallint not null,
    comment text not null,
    is_hidden boolean not null default false,
    hidden_reason text,
    hidden_by bigint references users(user_id),
    hidden_at timestamp,
    variant_label varchar(255),              -- "Phân loại hàng" của người viết (từ đơn đã giao), ghi lúc viết
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng review_likes: nút "Hữu ích", mỗi tài khoản một lượt cho mỗi đánh giá
create table review_likes (
    review_id bigint not null references reviews(review_id) on delete cascade,
    user_id bigint not null references users(user_id),
    created_at timestamp default current_timestamp,
    primary key (review_id, user_id)
);

-- bảng review_images: ảnh khách gửi kèm đánh giá (tối đa 5 ảnh, kiểm tra ở ứng dụng)
create table review_images (
    image_id bigserial primary key,
    review_id bigint not null references reviews(review_id) on delete cascade,
    image_url text not null,
    display_order int not null,
    created_at timestamp default current_timestamp,
    unique (review_id, display_order)
);

-- =====================================================
-- indexes để tối ưu hiệu suất
-- =====================================================

-- user indexes
create index idx_users_email on users(email);
create index idx_users_username on users(username);
create index idx_users_is_active on users(is_active);

-- product indexes
create index idx_products_category_id on products(category_id);
create index idx_products_brand_id on products(brand_id);
create index idx_products_slug on products(slug);
create index idx_products_is_active on products(is_active);
create index idx_products_name on products using gin(to_tsvector('english', name));
create index idx_category_attributes_attribute_id on category_attributes(attribute_id);

-- order indexes
create index idx_orders_user_id on orders(user_id);
create index idx_orders_order_date on orders(order_date);
create index idx_orders_status on orders(status);
create index idx_orders_store on orders(store_id);
create index idx_orders_shipping_province on orders(shipping_province);
create index idx_order_status_history_order on order_status_history(order_id, changed_at);
create index idx_order_status_history_changed_by on order_status_history(changed_by);
-- báo cáo: doanh thu theo ngày giao, tiền hoàn theo ngày hoàn, doanh số theo chi nhánh / nhân viên
create index idx_orders_delivered_at on orders(delivered_at) where status = 'DELIVERED';
create index idx_return_requests_refunded_at on return_requests(completed_at) where status = 'REFUNDED';
create index idx_sales_records_store_recorded on sales_records(store_id, recorded_at);
create index idx_sales_records_employee_recorded on sales_records(employee_id, recorded_at);

insert into installment_partners (partner_code, partner_name, partner_type, min_order_total, max_order_total, display_order) values
    ('CREDIT_CARD', 'Thẻ tín dụng', 'CREDIT_CARD', 3000000, null, 1),
    ('FINANCE_A', 'Công ty tài chính A', 'FINANCE_COMPANY', 3000000, null, 2),
    ('FINANCE_B', 'Công ty tài chính B', 'FINANCE_COMPANY', 3000000, 15000000, 3);
insert into installment_fee_rates (partner_code, num_months, fee_percent) values
    ('CREDIT_CARD', 3, 2), ('CREDIT_CARD', 6, 3), ('CREDIT_CARD', 9, 4), ('CREDIT_CARD', 12, 5),
    ('FINANCE_A', 6, 4), ('FINANCE_A', 9, 5), ('FINANCE_A', 12, 6),
    ('FINANCE_B', 3, 3), ('FINANCE_B', 6, 4);
-- payment indexes
create index idx_payments_order_id on payments(order_id);
create index idx_payments_status on payments(status);
create unique index uq_payments_order_main on payments(order_id) where installment_payment_id is null;
create unique index uq_payments_checkout_session on payments(checkout_session_id) where checkout_session_id is not null;
create index idx_installment_orders_status on installment_orders(status);
create index idx_installment_orders_partner on installment_orders(partner_code, status);
create index idx_installment_payments_due on installment_payments(status, due_date);

-- interaction indexes
create index idx_user_interactions_user_id on user_interactions(user_id);
create index idx_user_interactions_product_id on user_interactions(product_id);
create index idx_user_interactions_timestamp on user_interactions(timestamp);
create index idx_user_interactions_user_product_type
    on user_interactions(user_id, product_id, interaction_type, timestamp);
create index idx_recommendations_user_created on recommendations(user_id, created_at);
create unique index uq_product_relations_pair_type
    on product_relations(product_id, related_product_id, relation_type);

-- warranty indexes
create index idx_warranty_requests_user_id on warranty_requests(user_id);
create index idx_warranty_requests_status on warranty_requests(status);

-- return indexes
create index idx_return_requests_user_id on return_requests(user_id);
create index idx_return_requests_status on return_requests(status);
create index idx_return_requests_order_id on return_requests(order_id);
create index idx_return_items_order_item_id on return_items(order_item_id);
create index idx_maintenance_requests_user_id on maintenance_requests(user_id);
create index idx_maintenance_requests_status on maintenance_requests(status);
-- mỗi dòng đơn chỉ có 1 yêu cầu bảo hành / bảo trì đang mở
create unique index uq_warranty_requests_open on warranty_requests(warranty_id)
    where status in ('PENDING', 'RECEIVED', 'PROCESSING');
create unique index uq_maintenance_requests_open on maintenance_requests(order_item_id)
    where status in ('PENDING', 'RECEIVED', 'PROCESSING');
create unique index uq_service_request_images_warranty
    on service_request_images(warranty_request_id, display_order) where warranty_request_id is not null;
create unique index uq_service_request_images_maintenance
    on service_request_images(maintenance_id, display_order) where maintenance_id is not null;
create unique index uq_service_request_images_return
    on service_request_images(return_request_id, display_order) where return_request_id is not null;

-- chat indexes
create index idx_chat_sessions_user_id on chat_sessions(user_id);
create index idx_chat_sessions_status on chat_sessions(status);
create index idx_chat_sessions_last_message on chat_sessions(last_message_at);
create index idx_chat_messages_session_id on chat_messages(session_id);
create index idx_contact_requests_status_created on contact_requests(status, created_at);
create index idx_contact_requests_user on contact_requests(user_id);

-- employee indexes
create index idx_employee_assignments_employee_id on employee_assignments(employee_id);
create index idx_employee_assignments_store_id on employee_assignments(store_id);

-- inventory indexes
create index idx_inventory_store on inventory(store_id);
create index idx_stock_movements_store_date on stock_movements(store_id, created_at);

-- mỗi nhân viên chỉ có 1 phân công chi nhánh đang hiệu lực tại 1 thời điểm
create unique index uq_employee_assignments_active
    on employee_assignments (employee_id) where is_active and end_date is null;

-- promotion indexes
create index idx_promotions_active_window on promotions(is_active, start_date, end_date);
create index idx_promotion_products_product on promotion_products(product_id);
create index idx_combos_main on combos(main_product_id, is_active, starts_at, ends_at);
create index idx_combo_items_product on combo_items(product_id);
create index idx_order_combos_order on order_combos(order_id);
create index idx_order_combos_combo on order_combos(combo_id);
create index idx_vouchers_window on vouchers(is_active, starts_at, ends_at);
create index idx_voucher_usages_voucher_user on voucher_usages(voucher_id, user_id);
create index idx_orders_voucher on orders(voucher_id);

-- review indexes
create index idx_reviews_product_id on reviews(product_id, created_at desc);
create index idx_reviews_hidden_by on reviews(hidden_by);
create index idx_review_likes_user on review_likes(user_id);
create index idx_reviews_user_created on reviews(user_id, created_at desc);

-- =====================================================
-- constraints bổ sung
-- =====================================================

-- thêm các ràng buộc check để đảm bảo dữ liệu hợp lệ
alter table products add constraint chk_base_price_positive check (base_price >= 0);
alter table product_variants add constraint chk_variant_price_positive check (price >= 0);
alter table orders add constraint chk_total_amount_positive check (total_amount >= 0);
alter table order_items add constraint chk_order_item_quantity_positive check (quantity > 0);
alter table installment_payments add constraint chk_installment_amount_positive check (amount > 0);
alter table payments add constraint chk_payments_status
    check (status in ('PENDING', 'PAID', 'REFUND_PENDING', 'REFUNDED', 'CANCELLED'));
alter table payments add constraint chk_payments_method check (payment_method in ('COD', 'BANK_TRANSFER', 'INSTALLMENT', 'CARD'));
alter table payments add constraint chk_payments_amount_positive check (amount > 0);
alter table installment_orders add constraint chk_installment_orders_status
    check (status in ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'SETTLED'));
alter table installment_partners add constraint chk_installment_partners_type
    check (partner_type in ('CREDIT_CARD', 'FINANCE_COMPANY'));
alter table installment_partners add constraint chk_installment_partners_range
    check (min_order_total >= 0 and (max_order_total is null or max_order_total >= min_order_total));
alter table installment_fee_rates add constraint chk_installment_fee_rates_months check (num_months > 0);
alter table installment_fee_rates add constraint chk_installment_fee_rates_percent check (fee_percent >= 0 and fee_percent <= 100);
alter table installment_orders add constraint chk_installment_orders_months_positive check (num_months > 0);
alter table installment_orders add constraint chk_installment_orders_monthly_positive check (monthly_payment > 0);
alter table installment_orders add constraint chk_installment_orders_citizen_id_digits check (citizen_id ~ '^[0-9]+$');
alter table installment_orders add constraint chk_installment_orders_rejection_reason
    check (status <> 'REJECTED' or rejection_reason is not null);
alter table installment_payments add constraint chk_installment_payments_status check (status in ('PENDING', 'PAID'));
alter table installment_payments add constraint chk_installment_payments_number_positive check (payment_number > 0);
alter table promotions add constraint chk_promotions_date_range check (end_date > start_date);
alter table promotions add constraint chk_promotions_discount_value_positive check (discount_value > 0);
alter table promotions add constraint chk_promotions_discount_type check (discount_type in ('PERCENTAGE', 'FIXED_AMOUNT'));
alter table promotions add constraint chk_promotions_percentage_range
    check (discount_type <> 'PERCENTAGE' or discount_value <= 100);
alter table promotion_products add constraint chk_promotion_products_discount_type
    check (discount_type is null or discount_type in ('PERCENTAGE', 'FIXED_AMOUNT'));
alter table promotion_products add constraint chk_promotion_products_discount_value_positive
    check (discount_value is null or discount_value > 0);
alter table promotion_products add constraint chk_promotion_products_percentage_range
    check (discount_type <> 'PERCENTAGE' or discount_value <= 100);
alter table promotion_products add constraint chk_promotion_products_pair
    check ((discount_type is null) = (discount_value is null));
alter table orders add constraint fk_orders_store foreign key (store_id) references stores(store_id);
alter table orders add constraint fk_orders_voucher foreign key (voucher_id) references vouchers(voucher_id);
alter table orders add constraint chk_orders_voucher_discount check (voucher_discount >= 0);
alter table order_items add constraint chk_order_items_voucher_discount check (voucher_discount >= 0);
alter table orders add constraint chk_orders_combo_discount check (combo_discount >= 0);
alter table order_items add constraint chk_order_items_combo_discount check (combo_discount >= 0);
alter table combos add constraint chk_combos_window check (ends_at > starts_at);
alter table combo_items add constraint chk_combo_items_type check (discount_type in ('PERCENT', 'AMOUNT'));
alter table combo_items add constraint chk_combo_items_value check (discount_value > 0 and (discount_type <> 'PERCENT' or discount_value <= 100));
alter table customer_profiles add constraint chk_customer_profiles_points check (loyalty_points >= 0);
alter table orders add constraint chk_orders_points check (points_used >= 0 and points_discount >= 0 and points_earned >= 0);
alter table order_items add constraint chk_order_items_points_discount check (points_discount >= 0);
alter table vouchers add constraint chk_vouchers_percent check (discount_percent > 0 and discount_percent <= 100);
alter table vouchers add constraint chk_vouchers_window check (ends_at > starts_at);
alter table vouchers add constraint chk_vouchers_amounts check ((max_discount is null or max_discount > 0) and min_order_amount >= 0);
alter table vouchers add constraint chk_vouchers_limits
    check ((usage_limit is null or usage_limit > 0) and per_customer_limit > 0 and used_count >= 0);
alter table vouchers add constraint chk_vouchers_code check (code ~ '^[A-Z0-9_-]{3,30}$');
alter table orders add constraint chk_orders_delivery_type check (delivery_type in ('HOME_DELIVERY', 'PICKUP'));
alter table inventory add constraint chk_inventory_quantity_non_negative check (quantity >= 0);
alter table stock_movements add constraint chk_stock_movements_type
    check (movement_type in ('IN', 'OUT', 'ADJUSTMENT', 'RETURN'));
alter table reviews add constraint chk_reviews_rating_range check (rating between 1 and 5);
alter table reviews add constraint chk_reviews_comment_length check (char_length(comment) between 10 and 2000);
alter table reviews add constraint chk_reviews_hidden_reason check (not is_hidden or hidden_reason is not null);
alter table warranties add constraint chk_warranties_dates check (warranty_end_date >= warranty_start_date);
alter table warranty_requests add constraint chk_warranty_requests_status
    check (status in ('PENDING', 'RECEIVED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'CANCELLED'));
alter table warranty_requests add constraint chk_warranty_requests_rejection
    check (status <> 'REJECTED' or rejection_reason is not null);
alter table maintenance_requests add constraint chk_maintenance_requests_status
    check (status in ('PENDING', 'RECEIVED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'CANCELLED'));
alter table maintenance_requests add constraint chk_maintenance_requests_rejection
    check (status <> 'REJECTED' or rejection_reason is not null);
alter table maintenance_requests add constraint chk_maintenance_requests_type
    check (maintenance_type in ('CLEANING', 'SOFTWARE', 'REPAIR', 'OTHER'));
alter table maintenance_requests add constraint chk_maintenance_requests_costs
    check ((estimated_cost is null or estimated_cost >= 0) and (actual_cost is null or actual_cost >= 0));
alter table return_requests add constraint chk_return_requests_status
    check (status in ('PENDING', 'APPROVED', 'RECEIVED', 'REFUNDED', 'REJECTED', 'CANCELLED'));
alter table return_requests add constraint chk_return_requests_rejection
    check (status <> 'REJECTED' or rejection_reason is not null);
alter table return_requests add constraint chk_return_requests_reason_type
    check (reason_type in ('DEFECTIVE', 'NOT_AS_DESCRIBED', 'CHANGED_MIND', 'OTHER'));
alter table return_requests add constraint chk_return_requests_refund check (refund_amount is null or refund_amount >= 0);
alter table return_items add constraint chk_return_items_quantity check (quantity > 0);
alter table return_items add constraint uq_return_items_request_item unique (return_request_id, order_item_id);
alter table sales_records add constraint chk_sales_records_amounts
    check (sales_amount >= 0 and (commission is null or commission >= 0));
alter table contact_requests add constraint chk_contact_requests_topic
    check (topic in ('ORDER', 'PRODUCT', 'AFTER_SALES', 'PAYMENT', 'OTHER'));
alter table contact_requests add constraint chk_contact_requests_status check (status in ('NEW', 'IN_PROGRESS', 'RESOLVED'));
alter table contact_requests add constraint chk_contact_requests_message_length
    check (char_length(message) between 10 and 2000);
alter table contact_requests add constraint chk_contact_requests_resolved_note
    check (status <> 'RESOLVED' or staff_note is not null);

-- =====================================================
-- tạo các view hữu ích
-- =====================================================

-- view: danh sách sản phẩm với thông tin thương hiệu và danh mục
create view v_products_full as
select
    p.product_id,
    p.name,
    p.base_price,
    p.discount_price,
    p.stock_quantity,
    b.name as brand_name,
    c.name as category_name,
    p.rating,
    p.created_at
from products p
left join brands b on p.brand_id = b.brand_id
left join categories c on p.category_id = c.category_id
where p.deleted_at is null;

-- view: thống kê doanh số theo siêu thị
create view v_sales_by_store as
select
    s.store_id,
    s.name as store_name,
    count(o.order_id) as total_orders,
    sum(o.total_amount) as total_revenue,
    avg(o.total_amount) as avg_order_value,
    max(o.order_date) as last_order_date
from stores s
left join sales_records sr on s.store_id = sr.store_id
left join orders o on sr.order_id = o.order_id
group by s.store_id, s.name;

-- view: chi tiết khách hàng với thống kê mua hàng
create view v_customer_stats as
select
    u.user_id,
    u.email,
    u.fullname,
    cp.total_spent,
    cp.loyalty_points,
    count(distinct o.order_id) as total_orders,
    max(o.order_date) as last_order_date,
    count(distinct ui.interaction_id) as total_interactions
from users u
left join customer_profiles cp on u.user_id = cp.customer_id
left join orders o on u.user_id = o.user_id
left join user_interactions ui on u.user_id = ui.user_id
where u.deleted_at is null
group by u.user_id, u.email, u.fullname, cp.total_spent, cp.loyalty_points;

-- view: bảo hành / bảo trì / đổi trả gộp 1 danh sách (lọc + phân trang ở DB)
create view service_requests_view as
select 'WARRANTY' || '-' || wr.warranty_request_id as view_id, 'WARRANTY' as request_type,
       wr.warranty_request_id as request_id, wr.user_id, o.order_id, o.store_id, oi.order_item_id,
       wr.status, wr.issue_description as description, wr.assigned_to_employee, wr.created_at, wr.updated_at
from warranty_requests wr
join warranties w on w.warranty_id = wr.warranty_id
join order_items oi on oi.order_item_id = w.order_item_id
join orders o on o.order_id = oi.order_id
union all
select 'MAINTENANCE' || '-' || mr.maintenance_id, 'MAINTENANCE', mr.maintenance_id, mr.user_id, o.order_id,
       o.store_id, oi.order_item_id, mr.status, mr.description, mr.assigned_to_employee, mr.created_at, mr.updated_at
from maintenance_requests mr
join order_items oi on oi.order_item_id = mr.order_item_id
join orders o on o.order_id = oi.order_id
union all
select 'RETURN' || '-' || rr.return_request_id, 'RETURN', rr.return_request_id, rr.user_id, o.order_id,
       o.store_id, null, rr.status, rr.reason, rr.assigned_to_employee, rr.created_at, rr.updated_at
from return_requests rr
join orders o on o.order_id = rr.order_id;

-- =====================================================
-- kho tri thức RAG cho chatbot (cần image pgvector/pgvector:pg16)
-- =====================================================
create extension if not exists vector;

-- bảng kb_chunks: mỗi dòng là một đoạn tài liệu (chunk) + vector embedding của nó
create table kb_chunks (
    chunk_id bigserial primary key,
    doc_type varchar(30) not null,
    source varchar(255) not null,
    product_id bigint references products(product_id) on delete cascade,
    title varchar(500),
    content text not null,
    -- metadata để lọc khi truy xuất: {"category": ..., "brand": ..., "price": ...}
    metadata jsonb not null default '{}',
    embedding vector(384) not null,
    created_at timestamp default current_timestamp
);

create index idx_kb_chunks_doc_type on kb_chunks(doc_type);
create index idx_kb_chunks_embedding on kb_chunks using hnsw (embedding vector_cosine_ops);

-- tìm kiếm từ khoá (full-text) cho RAG lai: bỏ dấu tiếng Việt, tên sản phẩm (title) nặng hơn nội dung
create extension unaccent;

create function f_unaccent(text) returns text
    language sql immutable parallel safe strict
as $$ select public.unaccent('public.unaccent', $1) $$;

alter table kb_chunks add column search_tsv tsvector
    generated always as (
        setweight(to_tsvector('simple', f_unaccent(coalesce(title, ''))), 'A') ||
        setweight(to_tsvector('simple', f_unaccent(content)), 'D')
    ) stored;

create index idx_kb_chunks_search on kb_chunks using gin (search_tsv);

-- =====================================================
-- end of schema
-- =====================================================