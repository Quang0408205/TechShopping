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
    tgdd_rating decimal(3, 2),               -- điểm trên Thế Giới Di Động (crawl, chỉ hiển thị)
    tgdd_review_count int not null default 0, -- số lượt đánh giá trên Thế Giới Di Động (crawl)
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
    uploaded_at timestamp default current_timestamp
);

-- bảng product_specifications: lưu thông số kỹ thuật của sản phẩm
create table product_specifications (
    specification_id bigserial primary key,
    product_id bigint not null references products(product_id) on delete cascade,
    spec_name varchar(100) not null,
    spec_value text not null,
    spec_order int
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
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng order_items: lưu chi tiết sản phẩm trong đơn hàng
create table order_items (
    order_item_id bigserial primary key,
    order_id bigint not null references orders(order_id) on delete cascade,
    variant_id bigint not null references product_variants(variant_id),
    quantity int not null,
    unit_price decimal(15, 2) not null,
    discount_amount decimal(15, 2) default 0,
    subtotal decimal(15, 2) not null
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

-- bảng installment_orders: hợp đồng trả góp của đơn hàng (hồ sơ + kết quả duyệt)
create table installment_orders (
    installment_id bigserial primary key,
    order_id bigint unique not null references orders(order_id),
    num_months int not null,
    monthly_payment decimal(15, 2) not null,
    interest_rate decimal(5, 2),
    total_interest decimal(15, 2),
    status varchar(50) default 'PENDING_APPROVAL',
    citizen_id varchar(20) not null,
    card_bank_code varchar(20) not null,
    rejection_reason text,
    reviewed_by bigint references users(user_id),
    reviewed_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
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
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

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
    interaction_type varchar(50) not null,
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
    user_id bigint not null references users(user_id),
    topic varchar(255),
    assigned_to_employee bigint references employees(employee_id),
    status varchar(50) default 'open',
    created_at timestamp default current_timestamp,
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
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    unique (user_id, product_id)
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
create index idx_order_status_history_order on order_status_history(order_id, changed_at);
create index idx_order_status_history_changed_by on order_status_history(changed_by);
-- báo cáo (Phase 10): doanh thu theo ngày giao, tiền hoàn theo ngày hoàn, doanh số theo chi nhánh / nhân viên
create index idx_orders_delivered_at on orders(delivered_at) where status = 'DELIVERED';
create index idx_return_requests_refunded_at on return_requests(completed_at) where status = 'REFUNDED';
create index idx_sales_records_store_recorded on sales_records(store_id, recorded_at);
create index idx_sales_records_employee_recorded on sales_records(employee_id, recorded_at);

-- payment indexes
create index idx_payments_order_id on payments(order_id);
create index idx_payments_status on payments(status);
create unique index uq_payments_order_main on payments(order_id) where installment_payment_id is null;
create index idx_installment_orders_status on installment_orders(status);
create index idx_installment_payments_due on installment_payments(status, due_date);

-- interaction indexes
create index idx_user_interactions_user_id on user_interactions(user_id);
create index idx_user_interactions_product_id on user_interactions(product_id);
create index idx_user_interactions_timestamp on user_interactions(timestamp);

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
create index idx_chat_messages_session_id on chat_messages(session_id);

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

-- review indexes
create index idx_reviews_product_id on reviews(product_id, created_at desc);
create index idx_reviews_hidden_by on reviews(hidden_by);

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
alter table payments add constraint chk_payments_method check (payment_method in ('COD', 'BANK_TRANSFER', 'INSTALLMENT'));
alter table payments add constraint chk_payments_amount_positive check (amount > 0);
alter table installment_orders add constraint chk_installment_orders_status
    check (status in ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ACTIVE', 'COMPLETED', 'CANCELLED'));
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
-- end of schema
-- =====================================================