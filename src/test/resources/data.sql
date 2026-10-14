-- Basic ZenWave test data. Regenerate with $zenwave-test-data.
-- DDL exported 2026-10-13 to target/ddl/schema.sql.
-- chain | business ids | state per aggregate | used by
-- checkout | ORD-001, SKU-001, CUST-001 | order CONFIRMED; reservation CONFIRMED;
--          payment AUTHORIZED; fulfillment SCHEDULED; notification SENT;
--          product PUBLISHED | repository and in-memory service tests

-- Catalog products
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-001', 'Arcadia First Edition', 'Numbered first edition', 49.99, 'EUR', 10, 'QUANTITY', '2026-09-26T10:00:00Z', 'PUBLISHED');

-- Catalog inventory
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-001', 9, 0, 1);

INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-001', 'ORD-001', 'SKU-001', 1, 'CONFIRMED', '2026-09-26T13:01:00Z', '2026-09-26T13:31:00Z', '2026-09-26T13:04:00Z');

-- Orders checkout
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-001', 'CONFIRMED', 'CUST-001', 'customer@example.com', 'PM-001',
        '[{"sku":"SKU-001","quantity":1,"unitPrice":49.99}]'::jsonb,
        '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28001","country":"ES"}'::jsonb,
        49.99, 'EUR', '2026-09-26T13:00:00Z', '2026-09-26T13:05:00Z');

-- Payments processing
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-001', 'CUST-001', 'PM-001', 'customer@example.com', 49.99, 'EUR', 'AUTHORIZED', 'PROV-001', 1, '2026-09-26T13:03:00Z');

-- Fulfillment shipping
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-001', 'ORD-001', 'CUST-001', 'SCHEDULED',
        '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28001","country":"ES"}'::jsonb,
        '[{"sku":"SKU-001","quantity":1}]'::jsonb, '2026-09-26T13:06:00Z');

-- Notifications consumer
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-001', 'CUST-001', 'ORD-001', ARRAY['SKU-001'], 'ORDER_CONFIRMATION', 'EMAIL', 'SENT',
        'Order confirmed', 'Your order ORD-001 is confirmed.', 'customer@example.com', '2026-09-26T13:05:00Z', '2026-09-26T13:07:00Z');
