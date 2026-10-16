-- Scenario test data. Regenerate with $zenwave-test-scenarios (specs/001-place-order-flow, G-FT-05).
-- DDL exported 2026-10-15 to target/ddl/schema.sql. Fixed test 'now' (fixed Clock): 2026-10-14T10:00:00Z.
-- JSON mirrors: src/test/resources/data/jpa/<Entity>/<k>.json, k = row position, id = 1 + (k-1)*50.
--
-- SCENARIO MATRIX: chain | business ids | state per aggregate (order/reservations/payment/fulfillment/notification) | used by
-- ORD-001 | SKU-001x1, pm=PM-001, postal=28001 | CONFIRMED/CONFIRMED/CAPTURED(a1)/SCHEDULED/ORDER_CONFIRMATION:SENT | completed
-- ORD-002 | SKU-002x2,SKU-003x1, pm=pm_ok, postal=28002 | CREATED/RESERVED/AUTHORIZATION_REQUESTED(a1)/-/- | order created, authorization requested (2 lines)
-- ORD-003 | SKU-002x1, pm=pm_ok, postal=28003 | CREATED/RESERVED/AUTHORIZED(a1)/-/- | payment authorized, order not yet confirmed
-- ORD-004 | SKU-002x1,SKU-003x1, pm=pm_ok, postal=28004 | CONFIRMED/CONFIRMED/AUTHORIZED(a1)/-/- | order confirmed, fulfillment pending (happy)
-- ORD-005 | SKU-002x1, pm=pm_ok, postal=00000 | CONFIRMED/CONFIRMED/AUTHORIZED(a1)/-/- | order confirmed, fulfillment pending (carrier_unavailable trigger)
-- ORD-006 | SKU-002x1, pm=pm_capture_fail, postal=28006 | CONFIRMED/CONFIRMED/AUTHORIZED(a1)/SCHEDULED/- | fulfillment scheduled, capture will fail
-- ORD-007 | SKU-002x1, pm=pm_ok, postal=00000 | CONFIRMED/CONFIRMED/AUTHORIZED(a1)/FAILED/- | fulfillment failed, payment still authorized
-- ORD-008 | SKU-002x1, pm=pm_fail_once, postal=28008 | CREATED/RESERVED/FAILED(a1)/-/- | payment failed below retry limit
-- ORD-009 | SKU-002x1, pm=pm_fail_always, postal=28009 | CREATED/RESERVED/FAILED(a3)/-/- | payment failed at retry limit
-- ORD-010 | SKU-002x1, pm=pm_decline, postal=28010 | CREATED/RESERVED/DECLINED(a1)/-/- | payment declined, stock still reserved
-- ORD-011 | SKU-002x1, pm=pm_fail_always, postal=28011 | CREATED/RESERVED/RETRY_EXHAUSTED(a3)/-/- | retries exhausted, stock still reserved
-- ORD-012 | SKU-002x1, pm=pm_capture_fail, postal=28012 | CONFIRMED/CONFIRMED/CAPTURE_FAILED(a1)/SCHEDULED/- | capture failed, void pending
-- ORD-013 | SKU-002x1, pm=pm_capture_fail, postal=28013 | CANCELLED/RELEASED/VOIDED(a1)/SCHEDULED/ORDER_CANCELLED:SENT | cancelled after capture failure
-- ORD-014 | SKU-002x1, pm=pm_decline, postal=28014 | CANCELLED/RELEASED/DECLINED(a1)/-/ORDER_CANCELLED:SENT | cancelled after payment declined
-- ORD-015 | SKU-002x1, pm=pm_fail_always, postal=28015 | CANCELLED/RELEASED/RETRY_EXHAUSTED(a3)/-/ORDER_CANCELLED:FAILED | cancelled after retries exhausted; cancellation e-mail failed
-- ORD-016 | SKU-002x1, pm=pm_ok, postal=00000 | CONFIRMED/CONFIRMED/VOIDED(a1)/FAILED/- | payment voided, stock still confirmed
-- ORD-017 | SKU-002x1, pm=pm_ok, postal=00000 | CONFIRMED/RELEASED/VOIDED(a1)/FAILED/- | stock released, order still confirmed
-- ORD-018 | SKU-002x1, pm=pm_decline, postal=28018 | CREATED/RELEASED/DECLINED(a1)/-/- | stock released, order still created
-- ORD-019 | SKU-002x1, pm=pm_ok, postal=00000 | CANCELLED/RELEASED/VOIDED(a1)/FAILED/ORDER_CANCELLED:SENT | cancelled after fulfillment failure
-- ORD-020 | SKU-004 | no order (Q-05: not persisted) / none / none / none / STOCK_UNAVAILABLE:SENT | stock-unavailable outcome, notification redelivery
-- Stock: SKU-001 plenty (avail 9, sold 1), SKU-002 plenty (avail 20), SKU-003 exactly-enough (avail 2), SKU-004 zero available.
--
-- COVERAGE (need -> chain/row)
-- Order CREATED -> ORD-002/003/008..011/018; CONFIRMED -> ORD-001 (id 1), 004..007, 012, 016, 017; CANCELLED -> ORD-013/014/015/019
-- StockReservation RESERVED -> ORD-002 (2 lines), 003, 008..011; CONFIRMED -> ORD-001, 004 (2 lines), 005..007, 012, 016; RELEASED -> ORD-013/014/015/017/018/019
-- InventoryPosition plenty -> SKU-001/SKU-002; exactly-enough -> SKU-003 (avail 2); zero -> SKU-004
-- Payment AUTHORIZATION_REQUESTED -> ORD-002; AUTHORIZED -> ORD-003..007; FAILED a1 -> ORD-008; FAILED a3 -> ORD-009; DECLINED -> ORD-010/014/018; RETRY_EXHAUSTED -> ORD-011/015; CAPTURE_FAILED -> ORD-012; CAPTURED -> ORD-001; VOIDED -> ORD-013/016/017/019
-- Fulfillment SCHEDULED -> ORD-001/006/012/013; FAILED -> ORD-007/016/017/019; absent (scheduleFulfillment start) -> ORD-004 (happy), ORD-005 (postal 00000)
-- Notification SENT per type -> ORD-001 (ORDER_CONFIRMATION), ORD-013/014/019 (ORDER_CANCELLED), ORD-020 (STOCK_UNAVAILABLE); FAILED -> ORD-015; absent -> every other order (first-delivery)
-- Flow outcomes: completed ORD-001; cancelled by decline ORD-014, retries exhausted ORD-015, capture failure ORD-013, fulfillment failure ORD-019; stock unavailable ORD-020 + SKU-004/SKU-003
-- Multiplicity -> ORD-002 and ORD-004 (two reservations each, fulfillment/payment/order items agree)
-- Redelivery -> every order with a payment, fulfillment or notification row (e.g. ORD-002 payment exists for OrderCreated; ORD-001 fulfillment/notification)
-- Time (Q-04 reservation expiry): no row - out of scope, no scheduler (M-01). RESERVED reservations expire after 'now' (reserved 09:50, expires 10:00+)
-- Notification PENDING: no row - transient state set and left within one command; not reachable as a start state
-- Stub-driven branches (provider decline/failure/capture, carrier, mailbox) -> forced by pm_* / postalCode 00000 / @fail.invalid on the rows above, not by row state alone
--
-- ASSUMPTIONS: assumption | value used | open question
-- Counting rule: sold = sum(CONFIRMED qty), reserved = sum(RESERVED qty); RELEASED qty is back in available (Q-09) | received = available+reserved+sold | Q-09
-- Per-SKU totals: SKU-001 10 (9+0+1), SKU-002 20 avail + reservations, SKU-003 exactly-enough avail 2, SKU-004 0 | see rows | -
-- Happy-path API test: POST start-checkout with SKU-001 quantity <= 9 (e.g. 1) succeeds | SKU-001 avail 9 | -
-- Stock-unavailable API/service tests: SKU-001 quantity 100 (avail 9), or SKU-003 quantity 3 (avail 2), or SKU-004 quantity 1 (avail 0) | see stock | Q-05
-- Exactly-enough: SKU-003 quantity 2 reserves everything; mutating tests must not rely on SKU-003 after others | avail 2 | -
-- Product editionSize is not enforced against inventory totals | 10/50/20/5 | -
-- Attempt limit 3 (arcadia.payments.max-attempts); FAILED a1 is below, FAILED a3 / RETRY_EXHAUSTED a3 at the limit | 3 | R-05
-- Provider references: STANDIN-<orderId> normally, STANDIN-CAPFAIL-<orderId> for pm_capture_fail | as listed | stand-in contract
-- paymentMethodId pm_ok / PM-001 = any non-trigger value (happy path); triggers pm_decline, pm_fail_once, pm_fail_always, pm_capture_fail | as listed | stand-in contract
-- Payment.failureReason strings (card_declined, processor_unavailable, capture_rejected, carrier_unavailable) follow the stand-in reasons | as listed | -
-- Reservation window 10 minutes (expires_at = reserved_at + 10 min) | 600 s | R-04
-- Order cancellation_reason / reservation release_reason use the PAYMENT_* enum names | as listed | G-CI-04
-- ORD-001 payment changed from AUTHORIZED/PROV-001 to CAPTURED/STANDIN-ORD-001 so the completed chain is consistent (notification SENT implies capture); AUTHORIZED payments start at ORD-003..007 | - | -
-- Notification FAILED recipient uses @fail.invalid; ORD-015 order customerEmail is that address | customer015@fail.invalid | X-03
-- ORD-020 has no Order, reservation or payment rows (stock unavailable is never persisted); off-chain notification: sendStockUnavailableNotification does not read other contexts | - | Q-05
-- Timestamps: orders n>=2 created 2026-10-14T09:50:<n>Z, later steps +5..+70 s; ORD-001 keeps the 2026-09-26 times (payment captured 13:06:30, notification created 13:06:40)

-- Catalog products
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-001', 'Arcadia First Edition', 'Numbered first edition', 49.99, 'EUR', 10, 'QUANTITY', '2026-09-26T10:00:00Z', 'PUBLISHED');
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-002', 'Arcadia Second Edition', 'Numbered edition', 59.90, 'EUR', 50, 'QUANTITY', '2026-09-26T10:00:00Z', 'PUBLISHED');
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-003', 'Arcadia Collector Edition', 'Numbered edition', 39.50, 'EUR', 20, 'QUANTITY', '2026-09-26T10:00:00Z', 'PUBLISHED');
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-004', 'Arcadia Sold-Out Edition', 'Numbered edition', 79.00, 'EUR', 5, 'QUANTITY', '2026-09-26T10:00:00Z', 'PUBLISHED');

-- Catalog inventory
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-001', 9, 0, 1);
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-002', 20, 7, 6);
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-003', 2, 1, 1);
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-004', 0, 0, 0);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-001', 'ORD-001', 'SKU-001', 1, 'CONFIRMED', '2026-09-26T13:01:00Z', '2026-09-26T13:11:00Z', '2026-09-26T13:04:00Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-002', 'ORD-002', 'SKU-002', 2, 'RESERVED', '2026-10-14T09:50:02Z', '2026-10-14T10:00:02Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-003', 'ORD-002', 'SKU-003', 1, 'RESERVED', '2026-10-14T09:50:02Z', '2026-10-14T10:00:02Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-004', 'ORD-003', 'SKU-002', 1, 'RESERVED', '2026-10-14T09:50:03Z', '2026-10-14T10:00:03Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-005', 'ORD-004', 'SKU-002', 1, 'CONFIRMED', '2026-10-14T09:50:04Z', '2026-10-14T10:00:04Z', '2026-10-14T09:50:14Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-006', 'ORD-004', 'SKU-003', 1, 'CONFIRMED', '2026-10-14T09:50:04Z', '2026-10-14T10:00:04Z', '2026-10-14T09:50:14Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-007', 'ORD-005', 'SKU-002', 1, 'CONFIRMED', '2026-10-14T09:50:05Z', '2026-10-14T10:00:05Z', '2026-10-14T09:50:15Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-008', 'ORD-006', 'SKU-002', 1, 'CONFIRMED', '2026-10-14T09:50:06Z', '2026-10-14T10:00:06Z', '2026-10-14T09:50:16Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-009', 'ORD-007', 'SKU-002', 1, 'CONFIRMED', '2026-10-14T09:50:07Z', '2026-10-14T10:00:07Z', '2026-10-14T09:50:17Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-010', 'ORD-008', 'SKU-002', 1, 'RESERVED', '2026-10-14T09:50:08Z', '2026-10-14T10:00:08Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-011', 'ORD-009', 'SKU-002', 1, 'RESERVED', '2026-10-14T09:50:09Z', '2026-10-14T10:00:09Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-012', 'ORD-010', 'SKU-002', 1, 'RESERVED', '2026-10-14T09:50:10Z', '2026-10-14T10:00:10Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-013', 'ORD-011', 'SKU-002', 1, 'RESERVED', '2026-10-14T09:50:11Z', '2026-10-14T10:00:11Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-014', 'ORD-012', 'SKU-002', 1, 'CONFIRMED', '2026-10-14T09:50:12Z', '2026-10-14T10:00:12Z', '2026-10-14T09:50:22Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-015', 'ORD-013', 'SKU-002', 1, 'RELEASED', '2026-10-14T09:50:13Z', '2026-10-14T10:00:13Z', '2026-10-14T09:51:03Z', 'PAYMENT_VOIDED');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-016', 'ORD-014', 'SKU-002', 1, 'RELEASED', '2026-10-14T09:50:14Z', '2026-10-14T10:00:14Z', '2026-10-14T09:51:04Z', 'PAYMENT_DECLINED');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-017', 'ORD-015', 'SKU-002', 1, 'RELEASED', '2026-10-14T09:50:15Z', '2026-10-14T10:00:15Z', '2026-10-14T09:51:05Z', 'PAYMENT_RETRY_EXHAUSTED');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-018', 'ORD-016', 'SKU-002', 1, 'CONFIRMED', '2026-10-14T09:50:16Z', '2026-10-14T10:00:16Z', '2026-10-14T09:50:26Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-019', 'ORD-017', 'SKU-002', 1, 'RELEASED', '2026-10-14T09:50:17Z', '2026-10-14T10:00:17Z', '2026-10-14T09:51:07Z', 'PAYMENT_VOIDED');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-020', 'ORD-018', 'SKU-002', 1, 'RELEASED', '2026-10-14T09:50:18Z', '2026-10-14T10:00:18Z', '2026-10-14T09:51:08Z', 'PAYMENT_DECLINED');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-021', 'ORD-019', 'SKU-002', 1, 'RELEASED', '2026-10-14T09:50:19Z', '2026-10-14T10:00:19Z', '2026-10-14T09:51:09Z', 'PAYMENT_VOIDED');

-- Orders checkout
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-001', 'CONFIRMED', 'CUST-001', 'customer@example.com', 'PM-001', '[{"sku":"SKU-001","quantity":1,"unitPrice":49.99}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28001","country":"ES"}'::jsonb, 49.99, 'EUR', '2026-09-26T13:00:00Z', '2026-09-26T13:05:00Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-002', 'CREATED', 'CUST-002', 'customer002@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":2,"unitPrice":59.9},{"sku":"SKU-003","quantity":1,"unitPrice":39.5}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28002","country":"ES"}'::jsonb, 159.30, 'EUR', '2026-10-14T09:50:02Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-003', 'CREATED', 'CUST-003', 'customer003@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28003","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:03Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-004', 'CONFIRMED', 'CUST-004', 'customer004@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9},{"sku":"SKU-003","quantity":1,"unitPrice":39.5}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28004","country":"ES"}'::jsonb, 99.40, 'EUR', '2026-10-14T09:50:04Z', '2026-10-14T09:50:14Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-005', 'CONFIRMED', 'CUST-005', 'customer005@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:05Z', '2026-10-14T09:50:15Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-006', 'CONFIRMED', 'CUST-006', 'customer006@example.com', 'pm_capture_fail', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28006","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:06Z', '2026-10-14T09:50:16Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-007', 'CONFIRMED', 'CUST-007', 'customer007@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:07Z', '2026-10-14T09:50:17Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-008', 'CREATED', 'CUST-008', 'customer008@example.com', 'pm_fail_once', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28008","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:08Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-009', 'CREATED', 'CUST-009', 'customer009@example.com', 'pm_fail_always', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28009","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:09Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-010', 'CREATED', 'CUST-010', 'customer010@example.com', 'pm_decline', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28010","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:10Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-011', 'CREATED', 'CUST-011', 'customer011@example.com', 'pm_fail_always', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28011","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:11Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-012', 'CONFIRMED', 'CUST-012', 'customer012@example.com', 'pm_capture_fail', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28012","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:12Z', '2026-10-14T09:50:22Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-013', 'CANCELLED', 'CUST-013', 'customer013@example.com', 'pm_capture_fail', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28013","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:13Z', '2026-10-14T09:50:23Z', '2026-10-14T09:51:13Z', 'PAYMENT_VOIDED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-014', 'CANCELLED', 'CUST-014', 'customer014@example.com', 'pm_decline', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28014","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:14Z', '2026-10-14T09:51:14Z', 'PAYMENT_DECLINED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-015', 'CANCELLED', 'CUST-015', 'customer015@fail.invalid', 'pm_fail_always', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28015","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:15Z', '2026-10-14T09:51:15Z', 'PAYMENT_RETRY_EXHAUSTED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-016', 'CONFIRMED', 'CUST-016', 'customer016@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:16Z', '2026-10-14T09:50:26Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-017', 'CONFIRMED', 'CUST-017', 'customer017@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:17Z', '2026-10-14T09:50:27Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-018', 'CREATED', 'CUST-018', 'customer018@example.com', 'pm_decline', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28018","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:18Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-019', 'CANCELLED', 'CUST-019', 'customer019@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:19Z', '2026-10-14T09:50:29Z', '2026-10-14T09:51:19Z', 'PAYMENT_VOIDED');

-- Payments processing
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, captured_at)
VALUES (nextval('payment_seq'), 0, 'ORD-001', 'CUST-001', 'PM-001', 'customer@example.com', 49.99, 'EUR', 'CAPTURED', 'STANDIN-ORD-001', 1, '2026-09-26T13:03:00Z', '2026-09-26T13:06:30Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count)
VALUES (nextval('payment_seq'), 0, 'ORD-002', 'CUST-002', 'pm_ok', 'customer002@example.com', 159.30, 'EUR', 'AUTHORIZATION_REQUESTED', 1);
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-003', 'CUST-003', 'pm_ok', 'customer003@example.com', 59.90, 'EUR', 'AUTHORIZED', 'STANDIN-ORD-003', 1, '2026-10-14T09:50:08Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-004', 'CUST-004', 'pm_ok', 'customer004@example.com', 99.40, 'EUR', 'AUTHORIZED', 'STANDIN-ORD-004', 1, '2026-10-14T09:50:09Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-005', 'CUST-005', 'pm_ok', 'customer005@example.com', 59.90, 'EUR', 'AUTHORIZED', 'STANDIN-ORD-005', 1, '2026-10-14T09:50:10Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-006', 'CUST-006', 'pm_capture_fail', 'customer006@example.com', 59.90, 'EUR', 'AUTHORIZED', 'STANDIN-CAPFAIL-ORD-006', 1, '2026-10-14T09:50:11Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-007', 'CUST-007', 'pm_ok', 'customer007@example.com', 59.90, 'EUR', 'AUTHORIZED', 'STANDIN-ORD-007', 1, '2026-10-14T09:50:12Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-008', 'CUST-008', 'pm_fail_once', 'customer008@example.com', 59.90, 'EUR', 'FAILED', 1, '2026-10-14T09:50:13Z', 'processor_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-009', 'CUST-009', 'pm_fail_always', 'customer009@example.com', 59.90, 'EUR', 'FAILED', 3, '2026-10-14T09:50:14Z', 'processor_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, declined_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-010', 'CUST-010', 'pm_decline', 'customer010@example.com', 59.90, 'EUR', 'DECLINED', 1, '2026-10-14T09:50:15Z', 'card_declined');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-011', 'CUST-011', 'pm_fail_always', 'customer011@example.com', 59.90, 'EUR', 'RETRY_EXHAUSTED', 3, '2026-10-14T09:50:16Z', 'processor_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-012', 'CUST-012', 'pm_capture_fail', 'customer012@example.com', 59.90, 'EUR', 'CAPTURE_FAILED', 'STANDIN-CAPFAIL-ORD-012', 1, '2026-10-14T09:50:17Z', 'capture_rejected');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-013', 'CUST-013', 'pm_capture_fail', 'customer013@example.com', 59.90, 'EUR', 'VOIDED', 'STANDIN-CAPFAIL-ORD-013', 1, '2026-10-14T09:50:18Z', '2026-10-14T09:50:53Z', 'capture_rejected');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, declined_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-014', 'CUST-014', 'pm_decline', 'customer014@example.com', 59.90, 'EUR', 'DECLINED', 1, '2026-10-14T09:50:19Z', 'card_declined');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-015', 'CUST-015', 'pm_fail_always', 'customer015@fail.invalid', 59.90, 'EUR', 'RETRY_EXHAUSTED', 3, '2026-10-14T09:50:20Z', 'processor_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-016', 'CUST-016', 'pm_ok', 'customer016@example.com', 59.90, 'EUR', 'VOIDED', 'STANDIN-ORD-016', 1, '2026-10-14T09:50:21Z', '2026-10-14T09:50:56Z', 'carrier_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-017', 'CUST-017', 'pm_ok', 'customer017@example.com', 59.90, 'EUR', 'VOIDED', 'STANDIN-ORD-017', 1, '2026-10-14T09:50:22Z', '2026-10-14T09:50:57Z', 'carrier_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, declined_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-018', 'CUST-018', 'pm_decline', 'customer018@example.com', 59.90, 'EUR', 'DECLINED', 1, '2026-10-14T09:50:23Z', 'card_declined');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-019', 'CUST-019', 'pm_ok', 'customer019@example.com', 59.90, 'EUR', 'VOIDED', 'STANDIN-ORD-019', 1, '2026-10-14T09:50:24Z', '2026-10-14T09:50:59Z', 'carrier_unavailable');

-- Fulfillment shipping
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-001', 'ORD-001', 'CUST-001', 'SCHEDULED', '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28001","country":"ES"}'::jsonb, '[{"sku":"SKU-001","quantity":1}]'::jsonb, '2026-09-26T13:06:00Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-002', 'ORD-006', 'CUST-006', 'SCHEDULED', '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28006","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:26Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-003', 'ORD-007', 'CUST-007', 'FAILED', '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:27Z', 'carrier_unavailable');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-004', 'ORD-012', 'CUST-012', 'SCHEDULED', '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28012","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:32Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-005', 'ORD-013', 'CUST-013', 'SCHEDULED', '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28013","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:33Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-006', 'ORD-016', 'CUST-016', 'FAILED', '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:36Z', 'carrier_unavailable');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-007', 'ORD-017', 'CUST-017', 'FAILED', '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:37Z', 'carrier_unavailable');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-008', 'ORD-019', 'CUST-019', 'FAILED', '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:39Z', 'carrier_unavailable');

-- Notifications consumer
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-001', 'CUST-001', 'ORD-001', ARRAY['SKU-001'], 'ORDER_CONFIRMATION', 'EMAIL', 'SENT', 'Order confirmed', 'Your order ORD-001 is confirmed.', 'customer@example.com', '2026-09-26T13:06:40Z', '2026-09-26T13:07:00Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-002', 'CUST-013', 'ORD-013', ARRAY['SKU-002'], 'ORDER_CANCELLED', 'EMAIL', 'SENT', 'Order cancelled', 'Your order ORD-013 was cancelled: PAYMENT_VOIDED.', 'customer013@example.com', '2026-10-14T09:51:13Z', '2026-10-14T09:51:23Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-003', 'CUST-014', 'ORD-014', ARRAY['SKU-002'], 'ORDER_CANCELLED', 'EMAIL', 'SENT', 'Order cancelled', 'Your order ORD-014 was cancelled: PAYMENT_DECLINED.', 'customer014@example.com', '2026-10-14T09:51:14Z', '2026-10-14T09:51:24Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, failed_at, failure_reason)
VALUES (nextval('notification_seq'), 0, 'NOT-004', 'CUST-015', 'ORD-015', ARRAY['SKU-002'], 'ORDER_CANCELLED', 'EMAIL', 'FAILED', 'Order cancelled', 'Your order ORD-015 was cancelled: PAYMENT_RETRY_EXHAUSTED.', 'customer015@fail.invalid', '2026-10-14T09:51:15Z', '2026-10-14T09:51:25Z', 'mailbox_unavailable');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-005', 'CUST-019', 'ORD-019', ARRAY['SKU-002'], 'ORDER_CANCELLED', 'EMAIL', 'SENT', 'Order cancelled', 'Your order ORD-019 was cancelled: PAYMENT_VOIDED.', 'customer019@example.com', '2026-10-14T09:51:19Z', '2026-10-14T09:51:29Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-006', 'CUST-020', 'ORD-020', ARRAY['SKU-004'], 'STOCK_UNAVAILABLE', 'EMAIL', 'SENT', 'Items unavailable', 'Your order ORD-020 could not be placed: SKU-004 is out of stock.', 'customer020@example.com', '2026-10-14T09:55:00Z', '2026-10-14T09:55:10Z');
