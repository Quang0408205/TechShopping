-- sql schema: hệ thống quản lý và khuyến nghị mua sắm thiết bị công nghệ
-- database: postgresql 15+
-- encoding: utf-8

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
    rating decimal(3, 2) default 0,
    total_reviews int default 0,
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

-- =====================================================
-- nhóm 4: thanh toán và trả góp
-- =====================================================

-- bảng payments: quản lý các giao dịch thanh toán
create table payments (
    payment_id bigserial primary key,
    order_id bigint not null references orders(order_id),
    amount decimal(15, 2) not null,
    payment_method varchar(50) not null,
    transaction_id varchar(100),
    status varchar(50) default 'pending',
    paid_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng installment_orders: quản lý thông tin đơn hàng trả góp
create table installment_orders (
    installment_id bigserial primary key,
    order_id bigint unique not null references orders(order_id),
    num_months int not null,
    monthly_payment decimal(15, 2) not null,
    interest_rate decimal(5, 2),
    total_interest decimal(15, 2),
    status varchar(50) default 'active',
    created_at timestamp default current_timestamp
);

-- bảng installment_payments: quản lý các kỳ thanh toán trả góp
create table installment_payments (
    installment_payment_id bigserial primary key,
    installment_id bigint not null references installment_orders(installment_id),
    payment_number int not null,
    amount decimal(15, 2) not null,
    due_date date not null,
    paid_date date,
    status varchar(50) default 'pending',
    unique(installment_id, payment_number)
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
    status varchar(50) default 'pending',
    assigned_to_employee bigint references users(user_id),
    estimated_completion_date date,
    completed_at timestamp,
    notes text,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng maintenance_requests: quản lý yêu cầu bảo trì
create table maintenance_requests (
    maintenance_id bigserial primary key,
    user_id bigint not null references users(user_id),
    order_item_id bigint not null references order_items(order_item_id),
    maintenance_type varchar(100),
    description text not null,
    status varchar(50) default 'pending',
    assigned_to_employee bigint references users(user_id),
    completion_date date,
    notes text,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng return_requests: quản lý yêu cầu đổi trả
create table return_requests (
    return_request_id bigserial primary key,
    order_id bigint not null references orders(order_id),
    user_id bigint not null references users(user_id),
    reason text not null,
    status varchar(50) default 'pending',
    refund_amount decimal(15, 2),
    approved_at timestamp,
    completed_at timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

-- bảng return_items: lưu chi tiết sản phẩm trong yêu cầu đổi trả
create table return_items (
    return_item_id bigserial primary key,
    return_request_id bigint not null references return_requests(return_request_id),
    order_item_id bigint not null references order_items(order_item_id),
    quantity int not null,
    refund_amount decimal(15, 2)
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

-- order indexes
create index idx_orders_user_id on orders(user_id);
create index idx_orders_order_date on orders(order_date);
create index idx_orders_status on orders(status);

-- payment indexes
create index idx_payments_order_id on payments(order_id);
create index idx_payments_status on payments(status);

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

-- chat indexes
create index idx_chat_sessions_user_id on chat_sessions(user_id);
create index idx_chat_sessions_status on chat_sessions(status);
create index idx_chat_messages_session_id on chat_messages(session_id);

-- employee indexes
create index idx_employee_assignments_employee_id on employee_assignments(employee_id);
create index idx_employee_assignments_store_id on employee_assignments(store_id);

-- =====================================================
-- constraints bổ sung
-- =====================================================

-- thêm các ràng buộc check để đảm bảo dữ liệu hợp lệ
alter table products add constraint chk_base_price_positive check (base_price >= 0);
alter table product_variants add constraint chk_variant_price_positive check (price >= 0);
alter table orders add constraint chk_total_amount_positive check (total_amount >= 0);
alter table order_items add constraint chk_order_item_quantity_positive check (quantity > 0);
alter table installment_payments add constraint chk_installment_amount_positive check (amount > 0);

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

-- =====================================================
-- end of schema
-- =====================================================