-- Local demonstration data for a brand-new Docker techshopping database.
-- Loaded after the schema and product catalogue. Never use in production.

BEGIN;

CREATE TABLE IF NOT EXISTS dev_sample_seed_registry (
    seed_key varchar(100) PRIMARY KEY,
    applied_at timestamp NOT NULL DEFAULT current_timestamp
);

DO $$
BEGIN
    IF current_database() <> 'techshopping' THEN
        RAISE EXCEPTION 'Refusing local sample seed outside the techshopping database.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM dev_sample_seed_registry
        WHERE seed_key = 'techshopping-docker-init-sample-data-v1'
    ) THEN
        RAISE EXCEPTION 'The Docker sample-data seed has already been applied.';
    END IF;

    IF EXISTS (SELECT 1 FROM users)
       OR EXISTS (SELECT 1 FROM orders)
       OR EXISTS (SELECT 1 FROM stores)
       OR EXISTS (SELECT 1 FROM reviews)
       OR EXISTS (SELECT 1 FROM inventory)
       OR EXISTS (SELECT 1 FROM installment_orders)
       OR EXISTS (SELECT 1 FROM installment_payments)
       OR EXISTS (SELECT 1 FROM maintenance_requests)
       OR EXISTS (SELECT 1 FROM product_relations)
       OR EXISTS (SELECT 1 FROM review_images)
       OR EXISTS (SELECT 1 FROM stock_movements) THEN
        RAISE EXCEPTION 'The sample-data seed is only for a fresh Docker database.';
    END IF;

    IF (
        SELECT count(*)
        FROM product_variants v
        JOIN products p ON p.product_id = v.product_id
        WHERE p.is_active
          AND p.deleted_at IS NULL
          AND v.price > 0
    ) < 200 THEN
        RAISE EXCEPTION 'The product catalogue must contain at least 200 active variants.';
    END IF;

    IF (
        SELECT count(*)
        FROM products
        WHERE category_id = 4
          AND is_active
          AND deleted_at IS NULL
    ) < 10 THEN
        RAISE EXCEPTION 'At least ten active category-4 products are required.';
    END IF;
END
$$;

INSERT INTO roles (name, description)
VALUES ('CUSTOMER', 'Customer account')
ON CONFLICT (name) DO NOTHING;

CREATE TEMP TABLE local_sample_store ON COMMIT DROP AS
WITH inserted_store AS (
    INSERT INTO stores (
        name, address, phone, email, city, district, is_active
    )
    VALUES (
        'POY - Cửa hàng mẫu [SAMPLE-DATA]',
        '[ĐỊA CHỈ MẪU - không dùng để giao hàng]',
        '0900000000',
        'store@example.test',
        'TP. Hồ Chí Minh',
        'Quận 1',
        true
    )
    RETURNING store_id
)
SELECT store_id FROM inserted_store;

CREATE TEMP TABLE local_sample_accounts ON COMMIT DROP AS
SELECT n::int AS account_no,
       (
           (ARRAY['minhanh','quocbao','thuha','giahuy','baongoc',
                  'tuankiet','phuonglinh','ducanh','hoangnam','khanhvy'])[((n - 1) % 10) + 1]
           ||
           (ARRAY['nguyen','tran','le','pham','hoang'])[((n - 1) / 10) + 1]
       )::varchar(50) AS username,
       format(
           '%s %s [TÀI KHOẢN MẪU %s]',
           (ARRAY['Minh Anh','Quốc Bảo','Thu Hà','Gia Huy','Bảo Ngọc',
                  'Tuấn Kiệt','Phương Linh','Đức Anh','Hoàng Nam','Khánh Vy'])[((n - 1) % 10) + 1],
           (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng'])[((n - 1) / 10) + 1],
           lpad(n::text, 3, '0')
       )::varchar(120) AS fullname,
       format('09%s', lpad(n::text, 8, '0'))::varchar(20) AS phone,
       NULL::bigint AS user_id
FROM generate_series(1, 50) AS n;

INSERT INTO users (email, username, password_hash, fullname, phone, is_active, created_at)
SELECT username || '@example.test',
       username,
       '$2a$10$nbH0tjwxfDyh2MbSSMFZhuuymQZ2ZiosLeYzvvFllcrS9SXbdL3bC',
       fullname,
       phone,
       true,
       current_timestamp - ((account_no * 7) % 60) * interval '1 day'
FROM local_sample_accounts
ORDER BY account_no;

UPDATE local_sample_accounts a
SET user_id = u.user_id
FROM users u
WHERE u.username = a.username;

INSERT INTO user_roles (user_id, role_id)
SELECT a.user_id, r.role_id
FROM local_sample_accounts a
CROSS JOIN roles r
WHERE r.name = 'CUSTOMER';

INSERT INTO customer_profiles (
    customer_id, date_of_birth, gender, address, city, district, ward,
    postal_code, default_shipping_address, loyalty_points, total_spent, created_at
)
SELECT a.user_id,
       DATE '1988-01-01' + ((a.account_no * 137) % 9000),
       CASE WHEN a.account_no % 2 = 0 THEN 'Nam' ELSE 'Nữ' END,
       '[ĐỊA CHỈ MẪU - không dùng để giao hàng]',
       'TP. Hồ Chí Minh',
       CASE WHEN a.account_no % 2 = 0 THEN 'Quận 1' ELSE 'Quận 3' END,
       'Phường mẫu',
       '00000',
       '[ĐỊA CHỈ MẪU - không dùng để giao hàng]',
       0,
       0,
       current_timestamp - ((a.account_no * 7) % 60) * interval '1 day'
FROM local_sample_accounts a;

CREATE TEMP TABLE local_sample_variants ON COMMIT DROP AS
SELECT row_number() OVER (ORDER BY v.variant_id)::int AS variant_no,
       v.variant_id,
       v.product_id,
       v.price
FROM product_variants v
JOIN products p ON p.product_id = v.product_id
WHERE p.is_active
  AND p.deleted_at IS NULL
  AND v.price > 0;

CREATE TEMP TABLE local_sample_order_plan ON COMMIT DROP AS
SELECT n::int AS order_no,
       ((n - 1) % 50 + 1)::int AS account_no,
       v.variant_id,
       v.product_id,
       v.price,
       CASE WHEN n % 2 = 0 THEN 'BANK_TRANSFER' ELSE 'COD' END AS payment_method,
       CASE WHEN n = 1 THEN 'DELIVERED' ELSE 'PENDING' END AS status,
       current_timestamp - ((n * 5) % 30) * interval '1 day'
           - (n % 12) * interval '1 hour' AS ordered_at
FROM generate_series(1, 200) AS n
JOIN local_sample_variants v
  ON v.variant_no = ((n * 37 - 1) % (SELECT count(*) FROM local_sample_variants)) + 1;

CREATE TEMP TABLE local_sample_inventory_variants ON COMMIT DROP AS
SELECT DISTINCT variant_id
FROM (
    SELECT variant_id
    FROM local_sample_order_plan
    WHERE order_no = 1
    UNION ALL
    SELECT variant_id
    FROM local_sample_variants
    WHERE variant_no <= 2
) sample_variants;

INSERT INTO inventory (store_id, variant_id, quantity)
SELECT s.store_id, v.variant_id, 8
FROM local_sample_store s
CROSS JOIN local_sample_inventory_variants v;

INSERT INTO stock_movements (
    store_id, variant_id, movement_type, quantity_change,
    supplier_name, note, created_at
)
SELECT i.store_id,
       i.variant_id,
       'IN',
       i.quantity,
       'LOCAL SAMPLE DATA',
       'SAMPLE-DATA: illustrative opening stock.',
       current_timestamp - interval '30 days'
FROM inventory i
JOIN local_sample_store s ON s.store_id = i.store_id;

INSERT INTO orders (
    user_id, order_date, recipient_name, recipient_phone, shipping_address,
    billing_address, shipping_cost, tax_amount, total_amount, status,
    payment_method, notes, store_id, delivery_type, delivered_at, created_at, updated_at
)
SELECT a.user_id,
       p.ordered_at,
       a.fullname,
       a.phone,
       '[ĐỊA CHỈ MẪU - không dùng để giao hàng]',
       '[ĐỊA CHỈ MẪU - không dùng để giao hàng]',
       0,
       0,
       p.price,
       p.status,
       p.payment_method,
       format('SAMPLE-DATA: TS-WEB-ORDER-%s - dữ liệu mẫu local',
              lpad(p.order_no::text, 3, '0')),
       s.store_id,
       'HOME_DELIVERY',
       CASE WHEN p.status = 'DELIVERED' THEN p.ordered_at + interval '2 days' END,
       p.ordered_at,
       p.ordered_at
FROM local_sample_order_plan p
JOIN local_sample_accounts a ON a.account_no = p.account_no
CROSS JOIN local_sample_store s
ORDER BY p.order_no;

CREATE TEMP TABLE local_sample_orders ON COMMIT DROP AS
SELECT o.order_id,
       p.order_no,
       p.account_no,
       p.variant_id,
       p.product_id,
       p.price,
       p.payment_method,
       p.status,
       p.ordered_at,
       o.delivered_at,
       a.user_id
FROM local_sample_order_plan p
JOIN orders o
  ON o.notes = format('SAMPLE-DATA: TS-WEB-ORDER-%s - dữ liệu mẫu local',
                      lpad(p.order_no::text, 3, '0'))
JOIN local_sample_accounts a ON a.account_no = p.account_no;

INSERT INTO order_items (
    order_id, variant_id, quantity, unit_price, discount_amount, subtotal
)
SELECT order_id, variant_id, 1, price, 0, price
FROM local_sample_orders;

INSERT INTO payments (
    order_id, amount, payment_method, transaction_id, status, paid_at, created_at, updated_at
)
SELECT order_id,
       price,
       payment_method,
       CASE WHEN status = 'DELIVERED' THEN 'SAMPLE-PAID-001' END,
       CASE WHEN status = 'DELIVERED' THEN 'PAID' ELSE 'PENDING' END,
       delivered_at,
       ordered_at,
       ordered_at
FROM local_sample_orders;

INSERT INTO order_status_history (order_id, old_status, new_status, changed_at)
SELECT order_id, NULL, 'PENDING', ordered_at
FROM local_sample_orders;

INSERT INTO order_status_history (order_id, old_status, new_status, changed_at)
SELECT order_id, 'PENDING', 'DELIVERED', delivered_at
FROM local_sample_orders
WHERE status = 'DELIVERED';

UPDATE inventory i
SET quantity = quantity - 1,
    updated_at = current_timestamp
FROM local_sample_orders o
JOIN local_sample_store s ON true
WHERE o.order_no = 1
  AND i.store_id = s.store_id
  AND i.variant_id = o.variant_id;

INSERT INTO stock_movements (
    store_id, variant_id, movement_type, quantity_change,
    note, order_id, created_at
)
SELECT s.store_id,
       o.variant_id,
       'OUT',
       -1,
       'SAMPLE-DATA: one unit shipped for the delivered demonstration order.',
       o.order_id,
       o.delivered_at
FROM local_sample_orders o
CROSS JOIN local_sample_store s
WHERE o.order_no = 1;

-- same backfill rule as migrations/after_sales.sql: warranty from delivery date + product.warranty_months
INSERT INTO warranties (order_item_id, warranty_start_date, warranty_end_date, warranty_type)
SELECT oi.order_item_id,
       o.delivered_at::date,
       (o.delivered_at::date + make_interval(months => p.warranty_months))::date,
       'STANDARD'
FROM local_sample_orders o
JOIN order_items oi ON oi.order_id = o.order_id
JOIN product_variants v ON v.variant_id = oi.variant_id
JOIN products p ON p.product_id = v.product_id
WHERE o.order_no = 1
  AND coalesce(p.warranty_months, 0) > 0
ON CONFLICT (order_item_id) DO NOTHING;

-- app logic adds to total_spent only when an order reaches DELIVERED (no DB trigger)
UPDATE customer_profiles cp
SET total_spent = o.price
FROM local_sample_orders o
WHERE o.order_no = 1
  AND cp.customer_id = o.user_id;

INSERT INTO reviews (user_id, product_id, rating, comment, is_hidden, created_at)
SELECT user_id,
       product_id,
       5,
       'ĐÁNH GIÁ MẪU - dùng kiểm thử giao diện, không phải ý kiến khách hàng thật.',
       false,
       delivered_at
FROM local_sample_orders
WHERE order_no = 1;

-- rating / total_reviews are recomputed by Java on every write, not a DB trigger
UPDATE products p
SET rating = r.rating,
    total_reviews = 1
FROM reviews r
WHERE p.product_id = r.product_id
  AND r.comment LIKE 'ĐÁNH GIÁ MẪU%';

INSERT INTO review_images (review_id, image_url, display_order)
SELECT r.review_id,
       image.image_url,
       image.display_order
FROM reviews r
CROSS JOIN LATERAL (
    SELECT pi.image_url,
           row_number() OVER (ORDER BY pi.display_order NULLS LAST, pi.image_id)::int AS display_order
    FROM product_images pi
    JOIN local_sample_orders o ON o.product_id = pi.product_id AND o.order_no = 1
    ORDER BY pi.display_order NULLS LAST, pi.image_id
    LIMIT 2
) image
WHERE r.comment LIKE 'ĐÁNH GIÁ MẪU%';

WITH ranked_products AS (
    SELECT product_id,
           row_number() OVER (ORDER BY product_id) AS product_no
    FROM products
    WHERE category_id = 4
      AND is_active
      AND deleted_at IS NULL
)
INSERT INTO product_relations (
    product_id, related_product_id, relation_type, strength
)
SELECT first_product.product_id,
       second_product.product_id,
       'SIMILAR',
       0.850
FROM ranked_products first_product
JOIN ranked_products second_product
  ON second_product.product_no = first_product.product_no + 1
WHERE first_product.product_no % 2 = 1
  AND second_product.product_no <= 10;

INSERT INTO maintenance_requests (
    user_id, order_item_id, maintenance_type, description, status, notes, created_at
)
SELECT o.user_id,
       oi.order_item_id,
       'OTHER',
       'SAMPLE-DATA: request to inspect a delivered sample purchase.',
       'PENDING',
       'SAMPLE-DATA: local demonstration only; not a real customer service request.',
       o.delivered_at
FROM local_sample_orders o
JOIN order_items oi ON oi.order_id = o.order_id
WHERE o.order_no = 1;

WITH chosen_orders AS (
    SELECT o.order_id,
           o.price,
           row_number() OVER (ORDER BY o.order_no)::int AS plan_no
    FROM local_sample_orders o
    WHERE o.order_no IN (2, 3)
)
INSERT INTO installment_orders (
    order_id, num_months, monthly_payment, interest_rate, total_interest,
    status, citizen_id, card_bank_code
)
SELECT order_id,
       12,
       round(price / 12, 2),
       0,
       0,
       'PENDING_APPROVAL',
       lpad(plan_no::text, 12, '0'),
       'SAMPLE'
FROM chosen_orders;

INSERT INTO installment_payments (
    installment_id, payment_number, amount, due_date, status
)
SELECT io.installment_id,
       payment_no,
       CASE
           WHEN payment_no = io.num_months
               THEN o.total_amount - io.monthly_payment * (io.num_months - 1)
           ELSE io.monthly_payment
       END,
       (current_date + payment_no * interval '1 month')::date,
       'PENDING'
FROM installment_orders io
JOIN orders o ON o.order_id = io.order_id
CROSS JOIN generate_series(1, io.num_months) AS payment_no
WHERE io.card_bank_code = 'SAMPLE'
  AND o.notes LIKE 'SAMPLE-DATA: TS-WEB-ORDER-%';

INSERT INTO dev_sample_seed_registry (seed_key)
VALUES ('techshopping-docker-init-sample-data-v1');

COMMIT;
