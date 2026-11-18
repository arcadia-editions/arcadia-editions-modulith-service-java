-- Scenario test data. Regenerate with $zenwave-test-scenarios (specs/001-place-order-flow, G-FT-05).
-- DDL exported 2026-11-17 (models 0.10.1) to target/ddl/schema.sql; models 0.10.2+: enum columns hold the ZDL enum id (ReservationStatus RESERVED(1)...),
-- the JSON mirrors and this header use the enum names. Fixed test 'now' (fixed Clock): 2026-10-14T10:00:00Z.
-- JSON mirrors: src/test/resources/data/jpa/<Entity>/<k>.json, k = row position, id = 1 + (k-1)*50.
--
-- SCENARIO MATRIX: chain | business ids | state per aggregate (order/reservations/payment/fulfillment/notification) | used by
-- ORD-001 | SKU-001x1, pm=PM-001, postal=28001 | CONFIRMED/CONFIRMED/CAPTURED(a1)/SCHEDULED(paymentCapturedAt)/ORDER_CONFIRMATION:SENT | completed; recordPaymentCaptured redelivery; markReadyToShip allowed
-- ORD-002 | SKU-002x2,SKU-003x1, pm=pm_ok, postal=28002 | CREATED/RESERVED/AUTHORIZATION_REQUESTED(a1)/-/- | order created, authorization requested (2 lines); voidPayment no-op; getPaymentStatus STARTED; expireReservation before deadline
-- ORD-003 | SKU-002x1, pm=pm_ok, postal=28003 | CREATED/RESERVED/AUTHORIZED(a1)/-/- | payment authorized, order not yet confirmed
-- ORD-004 | SKU-002x1,SKU-003x1, pm=pm_ok, postal=28004 | CONFIRMED/CONFIRMED/AUTHORIZED(a1)/-/- | order confirmed, fulfillment pending (happy)
-- ORD-005 | SKU-002x1, pm=pm_ok, postal=00000 | CONFIRMED/CONFIRMED/AUTHORIZED(a1)/-/- | order confirmed, fulfillment pending (carrier_unavailable trigger)
-- ORD-006 | SKU-002x1, pm=pm_capture_fail, postal=28006 | CONFIRMED/CONFIRMED/AUTHORIZED(a1)/SCHEDULED/- | fulfillment scheduled, capture will fail; markReadyToShip refused (no paymentCapturedAt)
-- ORD-007 | SKU-002x1, pm=pm_ok, postal=00000 | CONFIRMED/CONFIRMED/AUTHORIZED(a1)/FAILED/- | fulfillment failed, payment still authorized
-- ORD-008 | SKU-002x1, pm=pm_fail_once, postal=28008 | CREATED/RESERVED/FAILED(a1)/-/- | payment failed below retry limit; reauthorizePayment from FAILED; voidPayment no-op
-- ORD-009 | SKU-002x1, pm=pm_fail_always, postal=28009 | CREATED/RESERVED/FAILED(a3)/-/- | payment failed at retry limit
-- ORD-010 | SKU-002x1, pm=pm_decline, postal=28010 | CREATED/RESERVED/DECLINED(a1)/-/- | payment declined, stock still reserved
-- ORD-011 | SKU-002x1, pm=pm_fail_always, postal=28011 | CREATED/RESERVED/RETRY_EXHAUSTED(a3)/-/- | retries exhausted, stock still reserved
-- ORD-012 | SKU-002x1, pm=pm_capture_fail, postal=28012 | CONFIRMED/CONFIRMED/CAPTURE_FAILED(a1)/SCHEDULED/- | capture failed, void pending
-- ORD-013 | SKU-002x1, pm=pm_capture_fail, postal=28013 | CANCELLED/RELEASED/VOIDED(a1)/SCHEDULED/ORDER_CANCELLED:SENT | OrderCancelled after capture failure, cancelFulfillment pending (from SCHEDULED)
-- ORD-014 | SKU-002x1, pm=pm_decline, postal=28014 | CANCELLED/RELEASED/DECLINED(a1)/-/ORDER_CANCELLED:SENT | cancelled after payment declined
-- ORD-015 | SKU-002x1, pm=pm_fail_always, postal=28015 | CANCELLED/RELEASED/RETRY_EXHAUSTED(a3)/-/ORDER_CANCELLED:FAILED | cancelled after retries exhausted; cancellation e-mail failed
-- ORD-016 | SKU-002x1, pm=pm_ok, postal=00000 | CONFIRMED/CONFIRMED/VOIDED(a1)/FAILED/- | payment voided, stock still confirmed
-- ORD-017 | SKU-002x1, pm=pm_ok, postal=00000 | CONFIRMED/RELEASED/VOIDED(a1)/FAILED/- | stock released, order still confirmed
-- ORD-018 | SKU-002x1, pm=pm_decline, postal=28018 | CREATED/RELEASED/DECLINED(a1)/-/- | stock released, order still created
-- ORD-019 | SKU-002x1, pm=pm_ok, postal=00000 | CANCELLED/RELEASED/VOIDED(a1)/FAILED/ORDER_CANCELLED:SENT | cancelled after fulfillment failure
-- ORD-021 | SKU-002x1, pm=pm_ok, postal=28021 | CANCELLED(RESERVATION_EXPIRED)/RELEASED(RESERVATION_EXPIRED)/AUTHORIZED(a1, late)/-/- | confirmOrder on CANCELLED -> OrderConfirmationRejected ORDER_CANCELLED; voidPayment from AUTHORIZED (OrderConfirmationRejected)
-- ORD-022 | SKU-002x1, pm=pm_ok, postal=28022 | CREATED/RELEASED(RESERVATION_EXPIRED)/AUTHORIZED(a1, late)/-/- | confirmOrder -> confirmStockReservation NOT_HELD -> OrderConfirmationRejected STOCK_NOT_HELD
-- ORD-023 | SKU-005x1, pm=pm_ok, postal=28023 | CONFIRMED/CONFIRMED/CAPTURED(a1)/SCHEDULED(no paymentCapturedAt)/- | PaymentCaptured emitted: recordPaymentCaptured from SCHEDULED; markReadyToShip refused until then
-- ORD-024 | SKU-002x1, pm=pm_capture_fail, postal=28024 | CANCELLED/RELEASED/VOIDED(a1)/CANCELLED/ORDER_CANCELLED:SENT | end orderCancelled after capture failure; cancelFulfillment redelivery; fulfillment CANCELLED read side
-- ORD-025 | SKU-005x1, pm=pm_void_fail, postal=00000 | CONFIRMED/CONFIRMED/VOID_FAILED(a1)/FAILED/- | PaymentVoidFailed emitted: releaseStock PAYMENT_VOID_FAILED from CONFIRMED; voidPayment redelivery on VOID_FAILED
-- ORD-026 | SKU-002x1, pm=pm_void_fail, postal=00000 | CANCELLED(PAYMENT_VOID_FAILED)/RELEASED/VOID_FAILED(a1)/FAILED/ORDER_CANCELLED:SENT | end orderCancelled, cause void refused; payment VOID_FAILED read side
-- ORD-027 | SKU-005x1, pm=pm_ok, postal=28027 | CREATED/RESERVED(past deadline 09:55:27)/-/-/- | expireReservation, getPaymentStatus NO_PAYMENT -> release RESERVATION_EXPIRED
-- ORD-028 | SKU-005x1, pm=pm_ok, postal=28028 | CREATED/RESERVED(past deadline 09:55:28)/AUTHORIZATION_REQUESTED(a1)/-/- | expireReservation, getPaymentStatus STARTED -> no release (payment owns the end)
-- ORD-020 | SKU-004 | no order (overlay: stock unavailable: not persisted) / none / none / none / STOCK_UNAVAILABLE:SENT | stock-unavailable outcome, notification redelivery
-- Stock: SKU-001 plenty (avail 9, sold 1), SKU-002 plenty (avail 20), SKU-003 exactly-enough (avail 2), SKU-004 zero available,
--   SKU-005 (avail 6, reserved 2, sold 2) holds the RESERVED/CONFIRMED rows of ORD-023/025/027/028 so SKU-001..004 counters stay.
--
-- COVERAGE (need -> chain/row)
-- Order CREATED -> ORD-002/003/008..011/018; CONFIRMED -> ORD-001 (id 1), 004..007, 012, 016, 017; CANCELLED -> ORD-013/014/015/019
-- StockReservation RESERVED -> ORD-002 (2 lines), 003, 008..011; CONFIRMED -> ORD-001, 004 (2 lines), 005..007, 012, 016; RELEASED -> ORD-013/014/015/017/018/019
-- InventoryPosition plenty -> SKU-001/SKU-002; exactly-enough -> SKU-003 (avail 2); zero -> SKU-004
-- Payment AUTHORIZATION_REQUESTED -> ORD-002; AUTHORIZED -> ORD-003..007; FAILED a1 -> ORD-008; FAILED a3 -> ORD-009; DECLINED -> ORD-010/014/018; RETRY_EXHAUSTED -> ORD-011/015; CAPTURE_FAILED -> ORD-012; CAPTURED -> ORD-001; VOIDED -> ORD-013/016/017/019
-- Fulfillment SCHEDULED -> ORD-001/006/012/013; FAILED -> ORD-007/016/017/019; absent (scheduleFulfillment start) -> ORD-004 (happy), ORD-005 (postal 00000)
-- Notification SENT per type -> ORD-001 (ORDER_CONFIRMATION), ORD-013/014/019 (ORDER_CANCELLED), ORD-020 (STOCK_UNAVAILABLE); FAILED -> ORD-015; absent -> every other order (first-delivery)
-- Flow outcomes: completed ORD-001; cancelled by decline ORD-014, retries exhausted ORD-015, capture failure ORD-024 (ORD-013 pending cancelFulfillment), fulfillment failure ORD-019, void refused ORD-026, reservation expired ORD-021 (with a late authorization); stock unavailable ORD-020 + SKU-004/SKU-003; notificationFailed ORD-015 (cancellation e-mail FAILED)
-- 0.10.x commands: getPaymentStatus NO_PAYMENT ORD-027 / STARTED ORD-002, ORD-028; expireReservation ORD-027 (release), ORD-028 (no release), ORD-002 (before deadline);
--   confirmStockReservation NOT_HELD ORD-022; confirmOrder rejected ORD-021 / ORD-022; reauthorizePayment ORD-008 (a1), ORD-009 (a3: at limit, stale input no-op);
--   voidPayment no-op ORD-002 (AUTHORIZATION_REQUESTED), ORD-008 (FAILED); void refused stub-driven (pm_void_fail), finished ORD-025/026;
--   recordPaymentCaptured ORD-023 (pending), ORD-001 (redelivery); cancelFulfillment ORD-013 (SCHEDULED), ORD-024 (redelivery), ORD-014 (no Fulfillment: no-op);
--   cancelFulfillment from READY_TO_SHIP: no row (no flow path cancels an order after capture; a test builds it inline)
-- Multiplicity -> ORD-002 and ORD-004 (two reservations each, fulfillment/payment/order items agree)
-- Redelivery -> every order with a payment, fulfillment or notification row (e.g. ORD-002 payment exists for OrderCreated; ORD-001 fulfillment/notification)
-- Time: reservation window 10 minutes; past deadline ORD-027 (no payment), ORD-028 (payment started); every other RESERVED row expires after 'now' (reserved 09:50, expires 10:00+)
-- Notification PENDING: no row - transient state set and left within one command; not reachable as a start state
-- Stub-driven branches (provider decline/failure/capture, carrier, mailbox) -> forced by pm_* / postalCode 00000 / @fail.invalid on the rows above, not by row state alone
--
-- ASSUMPTIONS: assumption | value used | open question
-- Counting rule: sold = sum(CONFIRMED qty), reserved = sum(RESERVED qty); RELEASED qty is back in available (open: counting rule) | received = available+reserved+sold | open: counting rule
-- Per-SKU totals: SKU-001 10 (9+0+1), SKU-002 20 avail + reservations, SKU-003 exactly-enough avail 2, SKU-004 0 | see rows | -
-- Happy-path API test: POST start-checkout with SKU-001 quantity <= 9 (e.g. 1) succeeds | SKU-001 avail 9 | -
-- Stock-unavailable API/service tests: SKU-001 quantity 100 (avail 9), or SKU-003 quantity 3 (avail 2), or SKU-004 quantity 1 (avail 0) | see stock | overlay: stock unavailable
-- Exactly-enough: SKU-003 quantity 2 reserves everything; mutating tests must not rely on SKU-003 after others | avail 2 | -
-- Product editionSize is not enforced against inventory totals | 10/50/20/5 | -
-- Attempt limit 3 (arcadia.payments.max-attempts); FAILED a1 is below, FAILED a3 / RETRY_EXHAUSTED a3 at the limit | 3 | open: retry limit
-- Provider references: STANDIN-<orderId> normally, STANDIN-CAPFAIL-<orderId> for pm_capture_fail | as listed | stand-in contract (tests force external decisions through these values)
-- paymentMethodId pm_ok / PM-001 = any non-trigger value (happy path); triggers pm_decline, pm_fail_once, pm_fail_always, pm_capture_fail | as listed | stand-in contract (tests force external decisions through these values)
-- Payment.failureReason strings (card_declined, processor_unavailable, capture_rejected, carrier_unavailable) follow the stand-in reasons | as listed | -
-- Reservation window 10 minutes (expires_at = reserved_at + 10 min) | 600 s | open: reservation window
-- Order cancellation_reason / reservation release_reason use the PAYMENT_* enum names | as listed | -
-- ORD-001 payment changed from AUTHORIZED/PROV-001 to CAPTURED/STANDIN-ORD-001 so the completed chain is consistent (notification SENT implies capture); AUTHORIZED payments start at ORD-003..007 | - | -
-- Notification FAILED recipient uses @fail.invalid; ORD-015 order customerEmail is that address | customer015@fail.invalid | -
-- ORD-020 has no Order, reservation or payment rows (stock unavailable is never persisted); off-chain notification: sendStockUnavailableNotification does not read other contexts | - | overlay: stock unavailable
-- A payment can appear after the timer released the stock (late OrderCreated delivery): ORD-021/022 AUTHORIZED | - | P-06 B (query timer, Kotlin proposals)
-- Void refusal stand-in trigger: paymentMethodId pm_void_fail (the stand-in must refuse voids for it; a board task adds it) | pm_void_fail | stand-in contract
-- Fulfillment.cancellationReason = OrderCancelled.reason (the StockReleaseReason name) | PAYMENT_VOIDED | -
-- Void refused: Payment.voidFailedAt set, failureReason keeps the void reason (carrier_unavailable) | - | -
-- Timestamps: orders n>=2 created 2026-10-14T09:50:<n>Z, later steps +5..+70 s; ORD-001 keeps the 2026-09-26 times (payment captured 13:06:30, notification created 13:06:40)

-- Catalog products
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-001', 'Arcadia First Edition', 'Numbered first edition', 49.99, 'EUR', 10, 1, '2026-09-26T10:00:00Z', 2);
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-002', 'Arcadia Second Edition', 'Numbered edition', 59.90, 'EUR', 50, 1, '2026-09-26T10:00:00Z', 2);
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-003', 'Arcadia Collector Edition', 'Numbered edition', 39.50, 'EUR', 20, 1, '2026-09-26T10:00:00Z', 2);
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-004', 'Arcadia Sold-Out Edition', 'Numbered edition', 79.00, 'EUR', 5, 1, '2026-09-26T10:00:00Z', 2);
INSERT INTO product (id, version, sku, name, description, price, currency, edition_size, tracking_mode, launch_at, status)
VALUES (nextval('product_seq'), 0, 'SKU-005', 'Arcadia Anniversary Edition', 'Numbered edition', 45.00, 'EUR', 10, 1, '2026-09-26T10:00:00Z', 2);

-- Catalog inventory
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-001', 9, 0, 1);
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-002', 20, 7, 6);
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-003', 2, 1, 1);
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-004', 0, 0, 0);
INSERT INTO inventory_position (id, version, sku, available_quantity, reserved_quantity, sold_quantity)
VALUES (nextval('inventory_position_seq'), 0, 'SKU-005', 6, 2, 2);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-001', 'ORD-001', 'SKU-001', 1, 3, '2026-09-26T13:01:00Z', '2026-09-26T13:11:00Z', '2026-09-26T13:04:00Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-002', 'ORD-002', 'SKU-002', 2, 1, '2026-10-14T09:50:02Z', '2026-10-14T10:00:02Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-003', 'ORD-002', 'SKU-003', 1, 1, '2026-10-14T09:50:02Z', '2026-10-14T10:00:02Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-004', 'ORD-003', 'SKU-002', 1, 1, '2026-10-14T09:50:03Z', '2026-10-14T10:00:03Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-005', 'ORD-004', 'SKU-002', 1, 3, '2026-10-14T09:50:04Z', '2026-10-14T10:00:04Z', '2026-10-14T09:50:14Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-006', 'ORD-004', 'SKU-003', 1, 3, '2026-10-14T09:50:04Z', '2026-10-14T10:00:04Z', '2026-10-14T09:50:14Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-007', 'ORD-005', 'SKU-002', 1, 3, '2026-10-14T09:50:05Z', '2026-10-14T10:00:05Z', '2026-10-14T09:50:15Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-008', 'ORD-006', 'SKU-002', 1, 3, '2026-10-14T09:50:06Z', '2026-10-14T10:00:06Z', '2026-10-14T09:50:16Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-009', 'ORD-007', 'SKU-002', 1, 3, '2026-10-14T09:50:07Z', '2026-10-14T10:00:07Z', '2026-10-14T09:50:17Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-010', 'ORD-008', 'SKU-002', 1, 1, '2026-10-14T09:50:08Z', '2026-10-14T10:00:08Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-011', 'ORD-009', 'SKU-002', 1, 1, '2026-10-14T09:50:09Z', '2026-10-14T10:00:09Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-012', 'ORD-010', 'SKU-002', 1, 1, '2026-10-14T09:50:10Z', '2026-10-14T10:00:10Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-013', 'ORD-011', 'SKU-002', 1, 1, '2026-10-14T09:50:11Z', '2026-10-14T10:00:11Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-014', 'ORD-012', 'SKU-002', 1, 3, '2026-10-14T09:50:12Z', '2026-10-14T10:00:12Z', '2026-10-14T09:50:22Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-015', 'ORD-013', 'SKU-002', 1, 2, '2026-10-14T09:50:13Z', '2026-10-14T10:00:13Z', '2026-10-14T09:51:03Z', 3);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-016', 'ORD-014', 'SKU-002', 1, 2, '2026-10-14T09:50:14Z', '2026-10-14T10:00:14Z', '2026-10-14T09:51:04Z', 1);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-017', 'ORD-015', 'SKU-002', 1, 2, '2026-10-14T09:50:15Z', '2026-10-14T10:00:15Z', '2026-10-14T09:51:05Z', 2);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-018', 'ORD-016', 'SKU-002', 1, 3, '2026-10-14T09:50:16Z', '2026-10-14T10:00:16Z', '2026-10-14T09:50:26Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-019', 'ORD-017', 'SKU-002', 1, 2, '2026-10-14T09:50:17Z', '2026-10-14T10:00:17Z', '2026-10-14T09:51:07Z', 3);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-020', 'ORD-018', 'SKU-002', 1, 2, '2026-10-14T09:50:18Z', '2026-10-14T10:00:18Z', '2026-10-14T09:51:08Z', 1);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-021', 'ORD-019', 'SKU-002', 1, 2, '2026-10-14T09:50:19Z', '2026-10-14T10:00:19Z', '2026-10-14T09:51:09Z', 3);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-022', 'ORD-021', 'SKU-002', 1, 2, '2026-10-14T09:40:21Z', '2026-10-14T09:50:21Z', '2026-10-14T09:50:31Z', 5);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-023', 'ORD-022', 'SKU-002', 1, 2, '2026-10-14T09:49:22Z', '2026-10-14T09:59:22Z', '2026-10-14T09:59:32Z', 5);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-024', 'ORD-023', 'SKU-005', 1, 3, '2026-10-14T09:50:23Z', '2026-10-14T10:00:23Z', '2026-10-14T09:50:33Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-025', 'ORD-024', 'SKU-002', 1, 2, '2026-10-14T09:50:24Z', '2026-10-14T10:00:24Z', '2026-10-14T09:51:04Z', 3);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, confirmed_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-026', 'ORD-025', 'SKU-005', 1, 3, '2026-10-14T09:50:25Z', '2026-10-14T10:00:25Z', '2026-10-14T09:50:35Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at, released_at, release_reason)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-027', 'ORD-026', 'SKU-002', 1, 2, '2026-10-14T09:50:26Z', '2026-10-14T10:00:26Z', '2026-10-14T09:51:06Z', 4);
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-028', 'ORD-027', 'SKU-005', 1, 1, '2026-10-14T09:45:27Z', '2026-10-14T09:55:27Z');
INSERT INTO stock_reservation (id, version, reservation_id, order_id, sku, quantity, status, reserved_at, expires_at)
VALUES (nextval('stock_reservation_seq'), 0, 'RES-029', 'ORD-028', 'SKU-005', 1, 1, '2026-10-14T09:45:28Z', '2026-10-14T09:55:28Z');

-- Orders checkout
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-001', 2, 'CUST-001', 'customer@example.com', 'PM-001', '[{"sku":"SKU-001","quantity":1,"unitPrice":49.99}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28001","country":"ES"}'::jsonb, 49.99, 'EUR', '2026-09-26T13:00:00Z', '2026-09-26T13:05:00Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-002', 1, 'CUST-002', 'customer002@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":2,"unitPrice":59.9},{"sku":"SKU-003","quantity":1,"unitPrice":39.5}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28002","country":"ES"}'::jsonb, 159.30, 'EUR', '2026-10-14T09:50:02Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-003', 1, 'CUST-003', 'customer003@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28003","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:03Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-004', 2, 'CUST-004', 'customer004@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9},{"sku":"SKU-003","quantity":1,"unitPrice":39.5}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28004","country":"ES"}'::jsonb, 99.40, 'EUR', '2026-10-14T09:50:04Z', '2026-10-14T09:50:14Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-005', 2, 'CUST-005', 'customer005@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:05Z', '2026-10-14T09:50:15Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-006', 2, 'CUST-006', 'customer006@example.com', 'pm_capture_fail', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28006","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:06Z', '2026-10-14T09:50:16Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-007', 2, 'CUST-007', 'customer007@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:07Z', '2026-10-14T09:50:17Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-008', 1, 'CUST-008', 'customer008@example.com', 'pm_fail_once', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28008","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:08Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-009', 1, 'CUST-009', 'customer009@example.com', 'pm_fail_always', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28009","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:09Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-010', 1, 'CUST-010', 'customer010@example.com', 'pm_decline', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28010","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:10Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-011', 1, 'CUST-011', 'customer011@example.com', 'pm_fail_always', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28011","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:11Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-012', 2, 'CUST-012', 'customer012@example.com', 'pm_capture_fail', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28012","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:12Z', '2026-10-14T09:50:22Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-013', 3, 'CUST-013', 'customer013@example.com', 'pm_capture_fail', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28013","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:13Z', '2026-10-14T09:50:23Z', '2026-10-14T09:51:13Z', 'PAYMENT_VOIDED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-014', 3, 'CUST-014', 'customer014@example.com', 'pm_decline', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28014","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:14Z', '2026-10-14T09:51:14Z', 'PAYMENT_DECLINED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-015', 3, 'CUST-015', 'customer015@fail.invalid', 'pm_fail_always', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28015","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:15Z', '2026-10-14T09:51:15Z', 'PAYMENT_RETRY_EXHAUSTED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-016', 2, 'CUST-016', 'customer016@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:16Z', '2026-10-14T09:50:26Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-017', 2, 'CUST-017', 'customer017@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:17Z', '2026-10-14T09:50:27Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-018', 1, 'CUST-018', 'customer018@example.com', 'pm_decline', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28018","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:18Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-019', 3, 'CUST-019', 'customer019@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:19Z', '2026-10-14T09:50:29Z', '2026-10-14T09:51:19Z', 'PAYMENT_VOIDED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-021', 3, 'CUST-021', 'customer021@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28021","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:40:21Z', '2026-10-14T09:50:41Z', 'RESERVATION_EXPIRED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-022', 1, 'CUST-022', 'customer022@example.com', 'pm_ok', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28022","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:49:22Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-023', 2, 'CUST-023', 'customer023@example.com', 'pm_ok', '[{"sku":"SKU-005","quantity":1,"unitPrice":45.0}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28023","country":"ES"}'::jsonb, 45.00, 'EUR', '2026-10-14T09:50:23Z', '2026-10-14T09:50:33Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-024', 3, 'CUST-024', 'customer024@example.com', 'pm_capture_fail', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28024","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:24Z', '2026-10-14T09:50:34Z', '2026-10-14T09:51:14Z', 'PAYMENT_VOIDED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-025', 2, 'CUST-025', 'customer025@example.com', 'pm_void_fail', '[{"sku":"SKU-005","quantity":1,"unitPrice":45.0}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 45.00, 'EUR', '2026-10-14T09:50:25Z', '2026-10-14T09:50:35Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at, confirmed_at, cancelled_at, cancellation_reason)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-026', 3, 'CUST-026', 'customer026@example.com', 'pm_void_fail', '[{"sku":"SKU-002","quantity":1,"unitPrice":59.9}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, 59.90, 'EUR', '2026-10-14T09:50:26Z', '2026-10-14T09:50:36Z', '2026-10-14T09:51:16Z', 'PAYMENT_VOID_FAILED');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-027', 1, 'CUST-027', 'customer027@example.com', 'pm_ok', '[{"sku":"SKU-005","quantity":1,"unitPrice":45.0}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28027","country":"ES"}'::jsonb, 45.00, 'EUR', '2026-10-14T09:45:27Z');
INSERT INTO checkout_order (id, version, order_id, status, customer_id, customer_email, payment_method_id, items, shipping_address, total_amount, currency, created_at)
VALUES (nextval('checkout_order_seq'), 0, 'ORD-028', 1, 'CUST-028', 'customer028@example.com', 'pm_ok', '[{"sku":"SKU-005","quantity":1,"unitPrice":45.0}]'::jsonb, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28028","country":"ES"}'::jsonb, 45.00, 'EUR', '2026-10-14T09:45:28Z');

-- Payments processing
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, captured_at)
VALUES (nextval('payment_seq'), 0, 'ORD-001', 'CUST-001', 'PM-001', 'customer@example.com', 49.99, 'EUR', 6, 'STANDIN-ORD-001', 1, '2026-09-26T13:03:00Z', '2026-09-26T13:06:30Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count)
VALUES (nextval('payment_seq'), 0, 'ORD-002', 'CUST-002', 'pm_ok', 'customer002@example.com', 159.30, 'EUR', 1, 1);
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-003', 'CUST-003', 'pm_ok', 'customer003@example.com', 59.90, 'EUR', 2, 'STANDIN-ORD-003', 1, '2026-10-14T09:50:08Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-004', 'CUST-004', 'pm_ok', 'customer004@example.com', 99.40, 'EUR', 2, 'STANDIN-ORD-004', 1, '2026-10-14T09:50:09Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-005', 'CUST-005', 'pm_ok', 'customer005@example.com', 59.90, 'EUR', 2, 'STANDIN-ORD-005', 1, '2026-10-14T09:50:10Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-006', 'CUST-006', 'pm_capture_fail', 'customer006@example.com', 59.90, 'EUR', 2, 'STANDIN-CAPFAIL-ORD-006', 1, '2026-10-14T09:50:11Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-007', 'CUST-007', 'pm_ok', 'customer007@example.com', 59.90, 'EUR', 2, 'STANDIN-ORD-007', 1, '2026-10-14T09:50:12Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-008', 'CUST-008', 'pm_fail_once', 'customer008@example.com', 59.90, 'EUR', 4, 1, '2026-10-14T09:50:13Z', 'processor_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-009', 'CUST-009', 'pm_fail_always', 'customer009@example.com', 59.90, 'EUR', 4, 3, '2026-10-14T09:50:14Z', 'processor_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, declined_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-010', 'CUST-010', 'pm_decline', 'customer010@example.com', 59.90, 'EUR', 3, 1, '2026-10-14T09:50:15Z', 'card_declined');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-011', 'CUST-011', 'pm_fail_always', 'customer011@example.com', 59.90, 'EUR', 5, 3, '2026-10-14T09:50:16Z', 'processor_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-012', 'CUST-012', 'pm_capture_fail', 'customer012@example.com', 59.90, 'EUR', 7, 'STANDIN-CAPFAIL-ORD-012', 1, '2026-10-14T09:50:17Z', 'capture_rejected');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-013', 'CUST-013', 'pm_capture_fail', 'customer013@example.com', 59.90, 'EUR', 8, 'STANDIN-CAPFAIL-ORD-013', 1, '2026-10-14T09:50:18Z', '2026-10-14T09:50:53Z', 'capture_rejected');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, declined_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-014', 'CUST-014', 'pm_decline', 'customer014@example.com', 59.90, 'EUR', 3, 1, '2026-10-14T09:50:19Z', 'card_declined');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-015', 'CUST-015', 'pm_fail_always', 'customer015@fail.invalid', 59.90, 'EUR', 5, 3, '2026-10-14T09:50:20Z', 'processor_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-016', 'CUST-016', 'pm_ok', 'customer016@example.com', 59.90, 'EUR', 8, 'STANDIN-ORD-016', 1, '2026-10-14T09:50:21Z', '2026-10-14T09:50:56Z', 'carrier_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-017', 'CUST-017', 'pm_ok', 'customer017@example.com', 59.90, 'EUR', 8, 'STANDIN-ORD-017', 1, '2026-10-14T09:50:22Z', '2026-10-14T09:50:57Z', 'carrier_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count, declined_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-018', 'CUST-018', 'pm_decline', 'customer018@example.com', 59.90, 'EUR', 3, 1, '2026-10-14T09:50:23Z', 'card_declined');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-019', 'CUST-019', 'pm_ok', 'customer019@example.com', 59.90, 'EUR', 8, 'STANDIN-ORD-019', 1, '2026-10-14T09:50:24Z', '2026-10-14T09:50:59Z', 'carrier_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-021', 'CUST-021', 'pm_ok', 'customer021@example.com', 59.90, 'EUR', 2, 'STANDIN-ORD-021', 1, '2026-10-14T09:51:01Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at)
VALUES (nextval('payment_seq'), 0, 'ORD-022', 'CUST-022', 'pm_ok', 'customer022@example.com', 59.90, 'EUR', 2, 'STANDIN-ORD-022', 1, '2026-10-14T09:59:42Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, captured_at)
VALUES (nextval('payment_seq'), 0, 'ORD-023', 'CUST-023', 'pm_ok', 'customer023@example.com', 45.00, 'EUR', 6, 'STANDIN-ORD-023', 1, '2026-10-14T09:50:28Z', '2026-10-14T09:50:48Z');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, voided_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-024', 'CUST-024', 'pm_capture_fail', 'customer024@example.com', 59.90, 'EUR', 8, 'STANDIN-CAPFAIL-ORD-024', 1, '2026-10-14T09:50:29Z', '2026-10-14T09:50:59Z', 'capture_rejected');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, void_failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-025', 'CUST-025', 'pm_void_fail', 'customer025@example.com', 45.00, 'EUR', 9, 'STANDIN-ORD-025', 1, '2026-10-14T09:50:30Z', '2026-10-14T09:51:00Z', 'carrier_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, provider_reference, attempt_count, authorized_at, void_failed_at, failure_reason)
VALUES (nextval('payment_seq'), 0, 'ORD-026', 'CUST-026', 'pm_void_fail', 'customer026@example.com', 59.90, 'EUR', 9, 'STANDIN-ORD-026', 1, '2026-10-14T09:50:31Z', '2026-10-14T09:51:01Z', 'carrier_unavailable');
INSERT INTO payment (id, version, order_id, customer_id, payment_method_id, customer_email, amount, currency, status, attempt_count)
VALUES (nextval('payment_seq'), 0, 'ORD-028', 'CUST-028', 'pm_ok', 'customer028@example.com', 45.00, 'EUR', 1, 1);

-- Fulfillment shipping
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at, payment_captured_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-001', 'ORD-001', 'CUST-001', 1, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28001","country":"ES"}'::jsonb, '[{"sku":"SKU-001","quantity":1}]'::jsonb, '2026-09-26T13:06:00Z', '2026-09-26T13:06:35Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-002', 'ORD-006', 'CUST-006', 1, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28006","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:26Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-003', 'ORD-007', 'CUST-007', 3, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:27Z', 'carrier_unavailable');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-004', 'ORD-012', 'CUST-012', 1, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28012","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:32Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-005', 'ORD-013', 'CUST-013', 1, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28013","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:33Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-006', 'ORD-016', 'CUST-016', 3, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:36Z', 'carrier_unavailable');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-007', 'ORD-017', 'CUST-017', 3, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:37Z', 'carrier_unavailable');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-008', 'ORD-019', 'CUST-019', 3, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:39Z', 'carrier_unavailable');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-009', 'ORD-023', 'CUST-023', 1, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28023","country":"ES"}'::jsonb, '[{"sku":"SKU-005","quantity":1}]'::jsonb, '2026-10-14T09:50:43Z');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, scheduled_at, cancelled_at, cancellation_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-010', 'ORD-024', 'CUST-024', 4, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"28024","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:44Z', '2026-10-14T09:51:24Z', 'PAYMENT_VOIDED');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-011', 'ORD-025', 'CUST-025', 3, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-005","quantity":1}]'::jsonb, '2026-10-14T09:50:45Z', 'carrier_unavailable');
INSERT INTO fulfillment (id, version, fulfillment_id, order_id, customer_id, status, shipping_address, items, failed_at, failure_reason)
VALUES (nextval('fulfillment_seq'), 0, 'FUL-012', 'ORD-026', 'CUST-026', 3, '{"name":"Arcadia Customer","line1":"1 Book Lane","city":"Madrid","postalCode":"00000","country":"ES"}'::jsonb, '[{"sku":"SKU-002","quantity":1}]'::jsonb, '2026-10-14T09:50:46Z', 'carrier_unavailable');

-- Notifications consumer
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-001', 'CUST-001', 'ORD-001', ARRAY['SKU-001'], 1, 1, 2, 'Order confirmed', 'Your order ORD-001 is confirmed.', 'customer@example.com', '2026-09-26T13:06:40Z', '2026-09-26T13:07:00Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-002', 'CUST-013', 'ORD-013', ARRAY['SKU-002'], 3, 1, 2, 'Order cancelled', 'Your order ORD-013 was cancelled: PAYMENT_VOIDED.', 'customer013@example.com', '2026-10-14T09:51:13Z', '2026-10-14T09:51:23Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-003', 'CUST-014', 'ORD-014', ARRAY['SKU-002'], 3, 1, 2, 'Order cancelled', 'Your order ORD-014 was cancelled: PAYMENT_DECLINED.', 'customer014@example.com', '2026-10-14T09:51:14Z', '2026-10-14T09:51:24Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, failed_at, failure_reason)
VALUES (nextval('notification_seq'), 0, 'NOT-004', 'CUST-015', 'ORD-015', ARRAY['SKU-002'], 3, 1, 3, 'Order cancelled', 'Your order ORD-015 was cancelled: PAYMENT_RETRY_EXHAUSTED.', 'customer015@fail.invalid', '2026-10-14T09:51:15Z', '2026-10-14T09:51:25Z', 'mailbox_unavailable');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-005', 'CUST-019', 'ORD-019', ARRAY['SKU-002'], 3, 1, 2, 'Order cancelled', 'Your order ORD-019 was cancelled: PAYMENT_VOIDED.', 'customer019@example.com', '2026-10-14T09:51:19Z', '2026-10-14T09:51:29Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-006', 'CUST-020', 'ORD-020', ARRAY['SKU-004'], 2, 1, 2, 'Items unavailable', 'Your order ORD-020 could not be placed: SKU-004 is out of stock.', 'customer020@example.com', '2026-10-14T09:55:00Z', '2026-10-14T09:55:10Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-007', 'CUST-024', 'ORD-024', ARRAY['SKU-002'], 3, 1, 2, 'Order cancelled', 'Your order ORD-024 was cancelled: PAYMENT_VOIDED.', 'customer024@example.com', '2026-10-14T09:51:20Z', '2026-10-14T09:51:30Z');
INSERT INTO notification (id, version, notification_id, customer_id, order_id, skus, type, channel, status, subject, body, recipient, created_at, sent_at)
VALUES (nextval('notification_seq'), 0, 'NOT-008', 'CUST-026', 'ORD-026', ARRAY['SKU-002'], 3, 1, 2, 'Order cancelled', 'Your order ORD-026 was cancelled: PAYMENT_VOID_FAILED.', 'customer026@example.com', '2026-10-14T09:51:22Z', '2026-10-14T09:51:32Z');
