# PlaceOrderFlow: tasks

flow: ../arcadia-editions-architecture/business-flows/place-order-flow.zfl
baseline: baseline/place-order-flow-1
branch: afk/place-order-flow

## S0 Baseline
review: [x] claude-opus PASS (reports/review-S0.md)

- [x] T-01 mechanical AFK Generated tests that should pass do: repository updateTest asserts stable values, getOrder and updateProduct stubs start from fixtures
      done: claude-sonnet sonnet
      test data: src/test/resources/data.sql, src/test/resources/data/jpa/Order/1.json, src/test/resources/data/jpa/Product/1.json
      tests: *RepositoryIntegrationTest, OrdersCheckoutServiceTest#getOrderTest, OrdersCheckoutApiControllerTest#getOrderTest, ProductCatalogServiceTest#updateProductTest, OrdersCheckoutApiIntegrationTest#testGetOrder_200
- [x] T-03 bounded AFK Shared config: injected Clock bean, FlowProperties (arcadia.payments.max-attempts 3, retry-delay), business ids PREFIX-UUID; fixed Clock at the test "now" in the in-memory configs
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:118-130
      test data: src/test/resources/data.sql
      tests: InventoryServiceTest, OrdersCheckoutServiceTest, PaymentsProcessingServiceTest, FulfillmentShippingServiceTest, NotificationsConsumerServiceTest
- [x] T-04 mechanical AFK Problem details: NoSuchElementException 404, invalid state transition 409
      done: claude-sonnet sonnet (2 attempts, P-01 A)
      model: orders-checkout-domain-model.zdl:92-111
      test data: data.sql ORD-001, ORD-013
      tests: OrdersCheckoutApiControllerTest, OrdersCheckoutApiIntegrationTest#testGetOrder_400
      blocked by: T-03

## S1 completed (tracer bullet)
path: StartOrderCheckout > startOrderCheckout > reserveStock[StockReservedResult] > OrderCreated > authorizePayment[authorized]
      > PaymentAuthorized > confirmOrder > confirmStockReservation > OrderConfirmed > scheduleFulfillment[FulfillmentScheduled]
      > capturePayment[PaymentCaptured] > sendOrderConfirmation > end completed
review: [x] claude-opus PASS round 2 (reports/review-S1-2.md; round 1 reports/review-S1.md)

- [x] T-05 bounded AFK reserveStock: all lines available, one RESERVED reservation per line, available moves to reserved under a write lock, expiresAt = reservedAt + 10 min, StockReservedResult
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:89-99, catalog-inventory-domain-model.zdl:25-38, catalog-inventory-domain-model.zdl:52-60, catalog-inventory-domain-model.zdl:80-96, catalog-inventory-domain-model.zdl:107-111
      test data: data.sql SKU-001, SKU-002, SKU-003
      tests: InventoryServiceTest
      blocked by: T-03
- [x] T-06 bounded AFK startOrderCheckout reserved branch: call reserveStock, create the Order CREATED with ORD id and totalAmount, emit OrderCreated (amount = totalAmount)
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:72-87, orders-checkout-domain-model.zdl:26-54, orders-checkout-domain-model.zdl:61-81, orders-checkout-domain-model.zdl:95-98, orders-checkout-domain-model.zdl:114-123
      test data: data.sql SKU-001, src/test/resources/data/jpa/Order/1.json
      tests: OrdersCheckoutServiceTest, OrdersCheckoutApiIntegrationTest
      blocked by: T-04, T-05
- [x] T-07 advanced AFK authorizePayment authorized branch: get-or-create Payment by orderId, PaymentProvider port with the stand-in adapter of the data.sql assumptions (pm_* triggers), emit PaymentAuthorized
      done: claude-opus opus
      model: place-order-flow.zfl:101-116, payments-processing-domain-model.zdl:26-44, payments-processing-domain-model.zdl:56-64, payments-processing-domain-model.zdl:84-87, payments-processing-domain-model.zdl:104-111
      test data: data.sql ORD-002, ORD-003
      tests: PaymentsProcessingServiceTest
      blocked by: T-03
- [x] T-08 bounded AFK confirmStockReservation: the order's RESERVED reservations become CONFIRMED, reserved moves to sold, emit StockReservationConfirmed
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:152-158, catalog-inventory-domain-model.zdl:61-65, catalog-inventory-domain-model.zdl:120-122, catalog-inventory-domain-model.zdl:138-146
      test data: data.sql ORD-003
      tests: InventoryServiceTest
      blocked by: T-05
- [x] T-09 bounded AFK confirmOrder: load by orderId, call confirmStockReservation, CREATED to CONFIRMED with confirmedAt, emit OrderConfirmed with items and shipping address
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:142-150, orders-checkout-domain-model.zdl:82-85, orders-checkout-domain-model.zdl:100-103, orders-checkout-domain-model.zdl:138-156
      test data: data.sql ORD-003
      tests: OrdersCheckoutServiceTest
      blocked by: T-06, T-08
- [x] T-10 advanced AFK scheduleFulfillment scheduled branch: idempotent by orderId, FulfillmentDecision port with the stand-in adapter (postal 00000 fails), Fulfillment SCHEDULED with FUL id, emit FulfillmentScheduled
      done: claude-opus opus
      model: place-order-flow.zfl:160-170, fulfillment-shipping-domain-model.zdl:25-50, fulfillment-shipping-domain-model.zdl:57-90, fulfillment-shipping-domain-model.zdl:82-84, fulfillment-shipping-domain-model.zdl:91-96
      test data: data.sql ORD-004, ORD-001
      tests: FulfillmentShippingServiceTest
      blocked by: T-03
- [x] T-11 bounded AFK capturePayment success branch: load by orderId, AUTHORIZED to CAPTURED through the provider port, emit PaymentCaptured with customerEmail
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:172-182, payments-processing-domain-model.zdl:71-75, payments-processing-domain-model.zdl:93-95, payments-processing-domain-model.zdl:150-158
      test data: data.sql ORD-004, ORD-001
      tests: PaymentsProcessingServiceTest
      blocked by: T-07
- [x] T-12 advanced AFK sendOrderConfirmation: idempotent by orderId and type, NotificationSender port with the stand-in adapter (@fail.invalid fails), Notification SENT, emit OrderConfirmationSent
      done: claude-opus opus
      model: place-order-flow.zfl:184-192, notifications-consumer-domain-model.zdl:26-42, notifications-consumer-domain-model.zdl:61-66, notifications-consumer-domain-model.zdl:88-90, notifications-consumer-domain-model.zdl:102-109
      test data: data.sql ORD-001
      tests: NotificationsConsumerServiceTest
      blocked by: T-03
- [x] T-13 advanced AFK Flow test completed: POST start-checkout to OrderConfirmationSent, Spring Modulith Scenario on PostgreSQL
      done: claude-opus opus
      model: place-order-flow.zfl:65-192, place-order-flow.zfl:262-266
      test data: data.sql SKU-001
      tests: PlaceOrderFlowTest
      blocked by: T-06, T-07, T-09, T-10, T-11, T-12

- [x] T-38 mechanical AFK OrdersCheckoutApiControllerTest#startOrderCheckoutTest sends a valid StartOrderCheckoutInput (SKU-001) and asserts 201 with an ORD id; it passes null since T-06
      done: claude-sonnet sonnet
      model: orders-checkout-domain-model.zdl:61-81
      test data: data.sql SKU-001
      tests: OrdersCheckoutApiControllerTest
      fixes: regression of T-06 found by the orchestrator while checking T-01 (NPE on null input)
      blocked by: T-06

- [x] T-41 bounded AFK Redelivery is a no-op: confirmOrder on an order already CONFIRMED and capturePayment on a payment already CAPTURED return it unchanged and emit nothing (as the other S1 steps)
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:142-150, place-order-flow.zfl:172-182
      test data: data.sql ORD-001, ORD-003
      tests: OrdersCheckoutServiceTest, PaymentsProcessingServiceTest
      fixes: review-S1 SHOULD FIX 1 (OrdersCheckoutServiceImpl.java:90-93, PaymentsProcessingServiceImpl.java:145-146)

## S2 stockGone
path: StartOrderCheckout > startOrderCheckout > reserveStock[StockUnavailableResult] > StockUnavailable
      > sendStockUnavailableNotification > end stockGone
review: [x] claude-opus PASS (reports/review-S2.md)

- [x] T-14 bounded AFK reserveStock unavailable branch: no reservation when any line is short, StockUnavailableResult with requested and available per sku
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:89-99, catalog-inventory-domain-model.zdl:97-106, catalog-inventory-domain-model.zdl:110-111
      test data: data.sql SKU-003, SKU-004
      tests: InventoryServiceTest
      blocked by: T-05
- [x] T-15 bounded AFK startOrderCheckout unavailable branch: the order is not persisted, emit StockUnavailable with a generated orderId, answer 409 with the unavailable items
      done: claude-opus opus
      model: place-order-flow.zfl:82-87, place-order-flow.zfl:194-197, orders-checkout-domain-model.zdl:95-98, orders-checkout-domain-model.zdl:126-135
      test data: data.sql SKU-004
      tests: OrdersCheckoutServiceTest, OrdersCheckoutApiIntegrationTest
      blocked by: T-06, T-14
- [x] T-16 bounded AFK sendStockUnavailableNotification: type STOCK_UNAVAILABLE with the skus, idempotent, emit StockUnavailableNotificationSent
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:194-202, notifications-consumer-domain-model.zdl:67-77, notifications-consumer-domain-model.zdl:92-94, notifications-consumer-domain-model.zdl:112-120
      test data: data.sql ORD-020
      tests: NotificationsConsumerServiceTest
      blocked by: T-12
- [x] T-17 bounded AFK Flow test stockGone: 409 and StockUnavailableNotificationSent, no order stored
      done: claude-opus opus
      model: place-order-flow.zfl:82-87, place-order-flow.zfl:199-202, place-order-flow.zfl:262-266
      test data: data.sql SKU-004
      tests: PlaceOrderFlowTest
      blocked by: T-13, T-15, T-16

## S3 orderCancelled by payment declined
path: OrderCreated > authorizePayment[declined] > PaymentDeclined > releaseStock > StockReleased > cancelOrder > OrderCancelled
      > sendOrderCancelledNotification > end orderCancelled
review: [x] claude-opus PASS (reports/review-S3.md)

- [x] T-18 bounded AFK authorizePayment declined branch: DECLINED with declinedAt and reason, emit PaymentDeclined, no retry
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:101-116, payments-processing-domain-model.zdl:84-87, payments-processing-domain-model.zdl:114-121
      test data: data.sql ORD-010
      tests: PaymentsProcessingServiceTest
      blocked by: T-07
- [x] T-19 bounded AFK releaseStock on PaymentDeclined: RESERVED or CONFIRMED reservations become RELEASED with releaseReason PAYMENT_DECLINED, counters back to available, idempotent, emit StockReleased
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:216-227, catalog-inventory-domain-model.zdl:66-70, catalog-inventory-domain-model.zdl:113-118, catalog-inventory-domain-model.zdl:126-135
      test data: data.sql ORD-010, ORD-018
      tests: InventoryServiceTest
      blocked by: T-08
- [x] T-20 bounded AFK cancelOrder: load by orderId, CREATED or CONFIRMED to CANCELLED with cancellationReason and cancelledAt, idempotent, emit OrderCancelled
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:241-250, orders-checkout-domain-model.zdl:86-91, orders-checkout-domain-model.zdl:105-107, orders-checkout-domain-model.zdl:159-167
      test data: data.sql ORD-018, ORD-017
      tests: OrdersCheckoutServiceTest
      blocked by: T-09
- [x] T-21 bounded AFK sendOrderCancelledNotification: type ORDER_CANCELLED with the reason, idempotent, emit OrderCancelledNotificationSent
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:252-260, notifications-consumer-domain-model.zdl:78-85, notifications-consumer-domain-model.zdl:96-98, notifications-consumer-domain-model.zdl:123-131
      test data: data.sql ORD-014
      tests: NotificationsConsumerServiceTest
      blocked by: T-12
- [x] T-22 bounded AFK Flow test orderCancelled by decline (pm_decline): stock back, order CANCELLED, OrderCancelledNotificationSent
      done: claude-opus opus
      model: place-order-flow.zfl:108-116, place-order-flow.zfl:223-260, place-order-flow.zfl:262-266
      test data: data.sql SKU-002
      tests: PlaceOrderFlowTest
      blocked by: T-17, T-18, T-19, T-20, T-21

## S4 payment retried, then completed
path: OrderCreated > authorizePayment[failed] > PaymentFailed > retryPayment[retried] > PaymentRetried > authorizePayment[authorized]
      > PaymentAuthorized > ... > end completed
review: [x] claude-opus PASS round 2 for scope (reports/review-S4-2.md; round 1 reports/review-S4.md); suite flake -> T-47

- [x] T-23 bounded AFK authorizePayment failed branch: FAILED with attemptCount and failureReason, emit PaymentFailed
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:108-116, payments-processing-domain-model.zdl:84-87, payments-processing-domain-model.zdl:124-130
      test data: data.sql ORD-008
      tests: PaymentsProcessingServiceTest
      blocked by: T-07
- [x] T-24 bounded AFK retryPayment retried branch below the attempt limit, emit PaymentRetried; on PaymentRetried, authorizePayment reloads the stored Payment by orderId after retry-delay
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:118-140, payments-processing-domain-model.zdl:65-70, payments-processing-domain-model.zdl:85, payments-processing-domain-model.zdl:89-91, payments-processing-domain-model.zdl:133-138
      test data: data.sql ORD-008
      tests: PaymentsProcessingServiceTest
      blocked by: T-23
- [x] T-25 bounded AFK Flow test retry then completed (pm_fail_once)
      done: claude-opus opus
      model: place-order-flow.zfl:118-140, place-order-flow.zfl:262-266
      test data: data.sql SKU-002
      tests: PlaceOrderFlowTest
      blocked by: T-22, T-24

- [x] T-46 bounded AFK retryPayment redelivery is a no-op: a PaymentFailed redelivered when the payment is no longer FAILED (AUTHORIZED, CAPTURED, VOIDED, RETRY_EXHAUSTED, ...) returns it unchanged and emits nothing
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:118-130, payments-processing-domain-model.zdl:89-91
      test data: data.sql ORD-003, ORD-009
      tests: PaymentsProcessingServiceTest
      fixes: review-S4 SHOULD FIX (PaymentsProcessingServiceImpl.java:125-126) and STANDARDS NOTE (task id in a production comment, :110)

## S5 orderCancelled by retries exhausted
path: PaymentFailed > retryPayment[retry-exhausted] > PaymentRetryExhausted > releaseStock > StockReleased > cancelOrder
      > OrderCancelled > sendOrderCancelledNotification > end orderCancelled
review: [x] claude-opus PASS (reports/review-S5.md)

- [x] T-26 bounded AFK retryPayment retry-exhausted branch at the attempt limit: RETRY_EXHAUSTED, emit PaymentRetryExhausted
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:124-130, payments-processing-domain-model.zdl:89-91, payments-processing-domain-model.zdl:141-147
      test data: data.sql ORD-009
      tests: PaymentsProcessingServiceTest
      blocked by: T-24
- [x] T-27 mechanical AFK releaseStock on PaymentRetryExhausted: releaseReason PAYMENT_RETRY_EXHAUSTED
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:223-227, catalog-inventory-domain-model.zdl:114-118
      test data: data.sql ORD-011
      tests: InventoryServiceTest
      blocked by: T-19
- [x] T-28 bounded AFK Flow test orderCancelled by retries exhausted (pm_fail_always)
      done: claude-opus opus
      model: place-order-flow.zfl:124-130, place-order-flow.zfl:223-260, place-order-flow.zfl:262-266
      test data: data.sql SKU-002
      tests: PlaceOrderFlowTest
      blocked by: T-25, T-26, T-27

## S6 orderCancelled by fulfillment failed
path: OrderConfirmed > scheduleFulfillment[FulfillmentFailed] > voidPayment > PaymentVoided > releaseStock > StockReleased
      > cancelOrder > OrderCancelled > sendOrderCancelledNotification > end orderCancelled
review: [x] claude-opus PASS (reports/review-S6.md)

- [x] T-29 bounded AFK scheduleFulfillment failed branch: Fulfillment FAILED with failureReason, emit FulfillmentFailed
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:160-170, fulfillment-shipping-domain-model.zdl:82-84, fulfillment-shipping-domain-model.zdl:99-105
      test data: data.sql ORD-005, ORD-007
      tests: FulfillmentShippingServiceTest
      blocked by: T-10
- [x] T-30 bounded AFK voidPayment on FulfillmentFailed: load by orderId, AUTHORIZED to VOIDED through the provider port, idempotent, emit PaymentVoided
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:204-214, payments-processing-domain-model.zdl:76-80, payments-processing-domain-model.zdl:97-100, payments-processing-domain-model.zdl:169-175
      test data: data.sql ORD-007, ORD-016
      tests: PaymentsProcessingServiceTest
      blocked by: T-11
- [x] T-31 mechanical AFK releaseStock on PaymentVoided: CONFIRMED reservations released with PAYMENT_VOIDED, sold back to available
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:223-227, catalog-inventory-domain-model.zdl:115-118
      test data: data.sql ORD-016
      tests: InventoryServiceTest
      blocked by: T-19
- [x] T-32 bounded AFK Flow test orderCancelled by fulfillment failure (postal 00000)
      done: claude-opus opus
      model: place-order-flow.zfl:160-170, place-order-flow.zfl:204-260, place-order-flow.zfl:262-266
      test data: data.sql SKU-002
      tests: PlaceOrderFlowTest
      blocked by: T-28, T-29, T-30, T-31

## S7 orderCancelled by capture failed
path: FulfillmentScheduled > capturePayment[PaymentCaptureFailed] > voidPayment > PaymentVoided > releaseStock > StockReleased
      > cancelOrder > OrderCancelled > sendOrderCancelledNotification > end orderCancelled
review: [x] claude-opus PASS (reports/review-S7.md)

- [x] T-33 bounded AFK capturePayment failure branch: CAPTURE_FAILED with failureReason (capture_rejected per data.sql), emit PaymentCaptureFailed; voidPayment from CAPTURE_FAILED, AUTHORIZATION_REQUESTED and FAILED (the other from-states of payments-processing-domain-model.zdl:99)
      done: claude-sonnet sonnet
      model: place-order-flow.zfl:172-182, place-order-flow.zfl:210-214, payments-processing-domain-model.zdl:93-100, payments-processing-domain-model.zdl:161-166
      test data: data.sql ORD-006, ORD-012
      tests: PaymentsProcessingServiceTest
      blocked by: T-30
- [x] T-34 bounded AFK Flow test orderCancelled by capture failure (pm_capture_fail)
      done: claude-opus opus
      model: place-order-flow.zfl:172-182, place-order-flow.zfl:204-260, place-order-flow.zfl:262-266
      test data: data.sql SKU-002
      tests: PlaceOrderFlowTest
      blocked by: T-32, T-33

- [x] T-42 bounded AFK capturePayment redelivery on a payment already CAPTURE_FAILED or VOIDED is a no-op (return it, no provider call, no event)
      done: claude-sonnet sonnet (RED), claude-opus opus (handoff)
      model: place-order-flow.zfl:172-182, payments-processing-domain-model.zdl:93-95
      test data: data.sql ORD-012, ORD-016
      tests: PaymentsProcessingServiceTest
      fixes: review-S1-2 MODEL NOTE for T-33 (PaymentsProcessingServiceImpl.java:156)

## S8 orderCancelled by reservation expired
path: ReservationExpired > releaseStock > StockReleased > cancelOrder > OrderCancelled > sendOrderCancelledNotification > end orderCancelled
review: [ ]

- [!] T-35 advanced HITL ReservationExpired timer: a scheduler on StockReservation.expiresAt with the injected Clock
      model: place-order-flow.zfl:229-239, catalog-inventory-domain-model.zdl:116-118
      blocked: out of scope for this iteration (overlay decision "Reservation expiry"); a person reopens it

## S9 Polish
review: [x] claude-opus PASS round 2 (reports/review-S9-2.md; round 1 reports/review-S9.md)

- [x] T-36 bounded AFK Notification delivery failure: FAILED with failureReason, emit NotificationDeliveryFailed, for the three send commands
      done: claude-sonnet sonnet
      model: notifications-consumer-domain-model.zdl:88-98, notifications-consumer-domain-model.zdl:134-144
      test data: data.sql ORD-015
      tests: NotificationsConsumerServiceTest
      blocked by: T-12, T-16, T-21
- [x] T-37 mechanical AFK ModularityTest: module boundaries verified with cycle detection disabled (cycles come from the generated event listener topology; upstream will not change, person 2026-11-13); jMolecules layering stays out (as JMoleculesArchitectureTest)
      done: claude-opus opus
      tests: ModularityTest
- [x] T-39 mechanical AFK Integration tests start their own PostgreSQL: DockerComposeInitializer uses local compose (withLocalCompose(true)) and BaseWebTestClientTest enables it, so mvn verify is self-contained from any clone
      done: claude-sonnet sonnet
      tests: OrdersCheckoutApiIntegrationTest, OrderRepositoryIntegrationTest
      fixes: review-S0 SHOULD FIX (BaseWebTestClientTest.java:3,14, DockerComposeInitializer.java:44)
- [x] T-40 mechanical AFK Repository updateTest asserts what was persisted (flush, clear, findById) and uses TestClock.NOW instead of the hard-coded instant
      done: claude-sonnet sonnet (2 attempts)
      tests: *RepositoryIntegrationTest
      fixes: review-S0 SHOULD FIX (TestClock duplication) and NOTE (updateTest asserts the saved instance)
      blocked by: T-39
- [x] T-43 mechanical AFK InventoryPositionRepositoryIntegrationTest.updateTest asserts the reloaded entity (flush, clear, findById) and uses TestClock.NOW, as T-40 did for the others
      done: claude-sonnet sonnet
      tests: InventoryPositionRepositoryIntegrationTest
      fixes: review-S9 SHOULD FIX 1 (InventoryPositionRepositoryIntegrationTest.java:52-65)
- [x] T-44 mechanical AFK Tests do not stall on Kafka: Spring Cloud Stream Kafka binding and Modulith event externalization are off in tests (no Kafka container), so no context waits ~60 s on localhost:9092
      done: claude-sonnet sonnet
      tests: OrderRepositoryIntegrationTest, PlaceOrderFlowTest
      fixes: review-S9 SHOULD FIX 2 (DockerComposeInitializer.java:34-53); orchestrator decision: switch off in tests rather than start Kafka
- [x] T-45 bounded AFK ModularityTest detects catalog.inventory and catalog.products as separate modules (as the overlay lists them), so the boundary between them is verified
      done: claude-sonnet sonnet
      tests: ModularityTest
      fixes: review-S9 SHOULD FIX 3 (ModularityTest.java:35)
- [x] T-47 mechanical AFK PlaceOrderFlowTest reads repository state only after the commit: every flow test's state asserts wait (Awaitility untilAsserted) instead of reading once when the terminal event arrives; the class is green 5 runs in a row
      done: claude-sonnet sonnet (5 runs green per worker; orchestrator 1 run 7/7; verified in the exit full suite)
      tests: PlaceOrderFlowTest
      fixes: review-S4-2 SUITE SHOULD FIX (flaky PlaceOrderFlowTest.java:367-368, same pattern :452)
      blocked by: T-34
