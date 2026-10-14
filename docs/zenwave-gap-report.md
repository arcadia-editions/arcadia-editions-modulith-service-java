# ZenWave Gap Report — arcadia-editions-modulith-service-java

| | |
|---|---|
| Date | 2026-10-14 |
| Code baseline | `cabbe60` (chore: update README.md and zenwave-sdd-overlay.md). Generated code is in `a77adb1` (feat: Full modulith generated with zenwave-scripts.zw) |
| Flows | `../arcadia-editions-architecture/business-flows/place-order-flow.zfl` (at `915d073`) |
| Models | `../arcadia-editions-modulith-api/*-domain-model.zdl` (6 files, at `9cca14e`). Staleness check passed: identical to the `service-all-0.0.1` jar the code was generated from |
| Decisions | `docs/zenwave-sdd-overlay.md`, "Decisions already taken" (2026-10-14) |
| Deployment shape | Modulith (Spring Modulith 2.0, in-process events, JDBC event publication registry) |
| Produced with | `zenwave-gap-report` (read-only) |

Code paths are relative to `src/main/java/com/arcadiaeditions/` unless they start with `src/`.
ZDL references use `<bc>.zdl:<line>`, short for `../arcadia-editions-modulith-api/<bc>-domain-model.zdl`.
ZFL references use `zfl:<line>` for `place-order-flow.zfl`.

## Model readiness: ✅ READY

> ✅ **READY.** No open model gaps and no open model-affecting questions. Implementation of
> `001-place-order-flow` can start.
>
> Deferred by decision, not blocking: reservation expiry (`ReservationExpired`), so M-01, Q-04 and
> G-CI-05 are out of scope for this iteration. Q-01..Q-06 are answered (overlay, "Decisions already
> taken"). The open questions Q-07..Q-12 don't affect the models.

## Summary

1. **Six listeners crash at runtime.** `Long id = null` is passed to `findById` in `confirmOrder`,
   `cancelOrder`, `retryPayment`, `capturePayment` (×1) and `voidPayment` (×2): the triggering events
   carry only `orderId`. The target context can look the aggregate up, so these are **code** gaps
   (Q-01): resolve it with `findByOrderId(event.orderId)` (G-OC-06, G-PP-05). The same crash class also
   includes `OrderCreated.amount` being unmapped (G-OC-03) and `Enum.valueOf` on free-text reasons
   (G-CI-04).
2. **Every ZFL edge has a trigger in code**, except the reservation-expiry timer (deferred) and the two
   synchronous `@calls` (`reserveStock`, `confirmStockReservation`), which are `// TODO`s.
3. **The generated "not implemented" pattern is in 12 of 15 flow service methods.** They build a new
   empty entity, never save it, and emit every alternative. `confirmStockReservation` and
   `releaseStock` have the generated load/guard/save loop, but load `List.of()`; and they would emit
   one event per reservation instead of one per order.
4. **`authorizePayment` must be get-or-create by `orderId`.** It is triggered by both `OrderCreated`
   and `PaymentRetried`; on a retry the input's payment fields are null (Q-02), and the service has to
   use the stored `Payment`.
5. **No external ports exist** (payment provider, notification sender), and listeners aren't
   idempotent under the 30 s redelivery (X-01..X-03).
6. **Tests.** The flow service tests are `// TODO` stubs, there is no flow-level test, and both
   generated web integration tests assert the wrong things (G-FT-03).

| Severity | Model (M) | Code (G) | Cross-cutting (X) | Open questions (Q) |
|---|---|---|---|---|
| blocker | 0 | 7 | 0 | — |
| major | 0 | 20 | 4 | — |
| minor | 0 | 11 | 4 | — |
| deferred | 1 (M-01) | 1 (G-CI-05) | — | 1 (Q-04) |
| **total open** | **0** | **38** | **8** | **6** (Q-07..Q-12, not model-affecting) |

### Tooling notes (not findings)

- **T-01.** `lint-zdl.sh --strict` reports 4 `zdl.data-lineage` warnings on
  `payments-processing.zdl:85`: `PaymentRetried` lacks `amount`, `currency`, `customerEmail` and
  `paymentMethodId` for `AuthorizePaymentInput`. All four are stored on the `Payment` the retry
  belongs to, so they are derivable by a lookup in the same context: a service gap (G-PP-01, Q-02),
  not a model gap. The other five ZDLs are clean. The linter doesn't detect M-01 (an `@timer` API
  missing from `apis {}`).
- **T-02.** In Git Bash, `mvn` picked a JDK older than 25. Run it with
  `JAVA_HOME=~/.sdkman/candidates/java/25.0.2-tem`.
- **T-03.** `mvn clean compile | tee target/compile.log` loses the log, because `clean` deletes
  `target/` (and `target/zdl-lint/`, `target/models/`). Write the log outside `target/`.
- **T-04.** `jbang zw` runs SDK 2.8.0-SNAPSHOT while the pom pins `zenwave.version` 2.6.0. The newer
  SDK rewrites shared files (`common/mappers/BaseMapper`, the root `package-info`, the architecture
  tests, `DockerComposeInitializer`), and its output depends on which module ran last. Align the
  versions before regenerating, and review shared files in the `git diff`.
- `target/compile.log` holds the current compile: 56 MapStruct unmapped-target warnings.

## 1. Model gaps (human decision: ZFL / ZDL change + regeneration; agents never edit the models)

**M-01 · deferred · `@timer(api: SchedulerApi, channel: "reservation-expired-event-v1")` refers to an
undeclared API** (`catalog-inventory.zdl:116`; `apis {}` at `:19-21` declares only
`PaymentsProcessingZdl`). Nothing is generated for it, and the ZFL condition `not PaymentAuthorized or
…` (`zfl:236`) can't be evaluated from inventory data alone. Not blocking: reservation expiry is out
of scope for this iteration (Q-04). When it comes back into scope, the options are (a) declare the
scheduler API and decide which payment state the timer may read, or (b) replace the annotation with an
in-module scheduled job.

No other model gaps. The models express every other ZFL step: `PaymentRetried → authorizePayment`
is a declared `@listener` (`payments-processing.zdl:85`); `confirmStockReservation` and `releaseStock`
are keyed by `orderId`; exclusive outcomes are declared as `[A | B | C]`; `@transition(to:)` names
the happy-path state and the alternative outcomes have their own lifecycle states (`DECLINED`,
`FAILED`, `RETRY_EXHAUSTED`, `CAPTURE_FAILED`, …), set in code (Q-03).

## 2. PlaceOrderFlow — traceability

| # | ZFL step | Trigger in code | Input mapping | Service body |
|---|---|---|---|---|
| 1 | `StartOrderCheckout → startOrderCheckout` (`zfl:82`) | ✅ `POST /api/orders/start-checkout` `orders/checkout/adapters/web/OrdersCheckoutApiController.java:54` | ✅ | ❌ G-OC-01 |
| 2 | `startOrderCheckout` `call reserveStock` (`zfl:84`) | ❌ `// TODO: call …reserveStock` in `OrdersCheckoutServiceImpl.java:45` | — | ❌ G-CI-01 |
| 3 | `OrderCreated → authorizePayment` (`zfl:108`) | ✅ `payments/processing/adapter/in/event/checkout/OrdersCheckoutEventsListener.java:26` | ⚠️ `amount` null upstream (G-OC-03) | ❌ G-PP-01 |
| 4 | `PaymentFailed → retryPayment` (`zfl:124`) | ✅ `payments/processing/adapter/in/event/processing/PaymentsProcessingServiceEventsListener.java:32` | ❌ `id = null` (G-PP-05) | ❌ G-PP-02 |
| 5 | `PaymentRetried → authorizePayment` (`zfl:132`) | ✅ `PaymentsProcessingServiceEventsListener.java:27` | ⚠️ payment fields null by design; loaded by `orderId` | ❌ G-PP-01 |
| 6 | `PaymentAuthorized → confirmOrder` (`zfl:146`) | ✅ `orders/checkout/adapters/events/processing/PaymentsProcessingEventsListener.java:24` | ❌ `id = null` (G-OC-06) | ⚠️ G-OC-02 |
| 7 | `confirmOrder` `call confirmStockReservation` (`zfl:148`) | ❌ `// TODO` in `OrdersCheckoutServiceImpl.java:60` | ✅ `ConfirmStockReservationInput { orderId }` | ❌ G-CI-02 |
| 8 | `OrderConfirmed → scheduleFulfillment` (`zfl:166`) | ✅ `fulfillment/shipping/checkout/OrdersCheckoutEventsListener.java:19` | ✅ | ❌ G-FS-01 |
| 9 | `FulfillmentScheduled → capturePayment` (`zfl:178`) | ✅ `payments/processing/adapter/in/event/shipping/FulfillmentShippingEventsListener.java:26` | ❌ `id = null` (G-PP-05) | ❌ G-PP-03 |
| 10 | `PaymentCaptured → sendOrderConfirmation` (`zfl:189`) | ✅ `notifications/consumer/events/processing/PaymentsProcessingEventsListener.java:26` | ✅ | ❌ G-NC-01 |
| 11 | `StockUnavailable → sendStockUnavailableNotification` (`zfl:199`) | ✅ `notifications/consumer/events/checkout/OrdersCheckoutEventsListener.java:26` | ⚠️ `unavailableItems` null upstream (G-OC-04) | ❌ G-NC-02 |
| 12 | `FulfillmentFailed → voidPayment` (`zfl:211`) | ✅ `FulfillmentShippingEventsListener.java:34` | ❌ `id = null` (G-PP-05) | ❌ G-PP-04 |
| 13 | `PaymentCaptureFailed → voidPayment` (`zfl:211`) | ✅ `PaymentsProcessingServiceEventsListener.java:39` | ❌ `id = null` (G-PP-05) | ❌ G-PP-04 |
| 14 | `PaymentDeclined / PaymentRetryExhausted / PaymentVoided → releaseStock` (`zfl:224`) | ✅ `catalog/inventory/events/processing/PaymentsProcessingEventsListener.java:25-39` | ❌ `Enum.valueOf(reason)` (G-CI-04) | ❌ G-CI-03 |
| 15 | `ReservationExpired → releaseStock` (`zfl:235-239`) | ⏸ deferred (G-CI-05) | — | — |
| 16 | `StockReleased → cancelOrder` (`zfl:247`) | ✅ `orders/checkout/adapters/events/inventory/CatalogInventoryEventsListener.java:26` | ❌ `id = null` (G-OC-06) | ⚠️ G-OC-05 |
| 17 | `OrderCancelled → sendOrderCancelledNotification` (`zfl:257`) | ✅ `notifications/consumer/events/checkout/OrdersCheckoutEventsListener.java:32` | ⚠️ `reason` null upstream (G-OC-05) | ❌ G-NC-03 |
| 18 | Customer reads the order (`getOrder`, `orders-checkout.zdl:109-110`; not a ZFL step) | ✅ `GET /api/orders/{id}` `OrdersCheckoutApiController.java:63` (404 when absent) | ✅ | ✅ `OrdersCheckoutServiceImpl.java:103` |

**Data lineage summary (listener inputs).** Fully covered by the event: `OrderCreated →
AuthorizePaymentInput`, `OrderConfirmed → ScheduleFulfillmentInput`, `PaymentCaptured →
SendOrderConfirmationInput`, `StockUnavailable → SendStockUnavailableNotificationInput`,
`OrderCancelled → SendOrderCancelledNotificationInput`, `PaymentFailed → RetryPaymentInput`,
`FulfillmentScheduled → CapturePaymentInput`. Resolved by a lookup by `orderId` in the target
context (decision): every `id`, and the payment fields of `PaymentRetried → AuthorizePaymentInput`.
Needs a constant: `ReleaseStockInput.reason` (one `StockReleaseReason` per source event).

### 2.1 orders-checkout (steps 1, 6, 16, 18)

| Id | Sev | Where | Gap → expected behavior | Model ref |
|---|---|---|---|---|
| G-OC-01 | blocker | `orders/checkout/core/application/OrdersCheckoutServiceImpl.java:45-58` | Generated pattern: new empty `Order`, never saved, `orderId`/`totalAmount`/`createdAt` not set, `reserveStock` not called, **both** `OrderCreated` and `StockUnavailable` emitted. Expected: generate `orderId`, compute `totalAmount` = Σ quantity × unitPrice (prices as sent, decision Q-06), call `InventoryService.reserveStock({orderId, items})`. On `RESERVED`: save as `CREATED`, emit `OrderCreated`. On `UNAVAILABLE`: **don't save** (decision Q-05), emit `StockUnavailable`, and answer 409 with the unavailable items. Throwing to get the 409 rolls back the transaction, and with it the `StockUnavailable` publication, so use `@Transactional(noRollbackFor = …)` or return the outcome to the controller. | `orders-checkout.zdl:95-98`; `zfl:82-99` |
| G-OC-02 | major | `OrdersCheckoutServiceImpl.java:60-80` | Guard, status and save are present. Missing: call `confirmStockReservation({orderId})` (`@calls`, TODO at `:61`) and set `confirmedAt`. | `orders-checkout.zdl:100-103`; `zfl:146-158` |
| G-OC-03 | blocker | `orders/checkout/core/application/mappers/EventsMapper.java:14` | `OrderCreated.amount` is unmapped, so `AuthorizePaymentInput.amount` is null and the `payment.amount NOT NULL` insert fails. Its source is not stated by the model: the matching name in `AuthorizePaymentInput.amount` and `Order.totalAmount` suggest the total. | `orders-checkout.zdl:111-120`; `payments-processing.zdl:55-62` |
| G-OC-04 | major | `EventsMapper.java:20` | `StockUnavailable.unavailableItems` is unmapped. Its source is `ReserveStockResult.unavailable.unavailableItems`, not the `Order`, so the mapper needs a second source. | `orders-checkout.zdl:123-132`; `catalog-inventory.zdl:97-104` |
| G-OC-05 | major | `OrdersCheckoutServiceImpl.java:82-100`; `EventsMapper.java:18`; `mappers/OrdersCheckoutServiceMapper.java:23-26` | `cancelledAt` isn't set; `CancelOrderInput.reason` isn't mapped to `Order.cancellationReason`; `OrderCancelled.reason` is unmapped. `StockReleased.reason` is a `StockReleaseReason` and `CancelOrderInput.reason` a `String`: map the enum name. Must be idempotent (X-01): a second `StockReleased` must not throw. | `orders-checkout.zdl:105-107`, `155-164`; `catalog-inventory.zdl:126-135` |
| G-OC-06 | blocker | `orders/checkout/adapters/events/processing/PaymentsProcessingEventsListener.java:26`; `adapters/events/inventory/CatalogInventoryEventsListener.java:28` | `Long id = null` → `findById(null)` throws, and the event is resubmitted forever. Resolve the id with `findByOrderId(event.orderId)` (decision). | decision "Correlation by `orderId`" |
| G-OC-07 | minor | `orders/checkout/core/outbound/jpa/OrderRepository.java:17` | No `findByOrderId` (natural key), needed by G-OC-06. | `orders-checkout.zdl:27` |

### 2.2 catalog-inventory (steps 2, 7, 14, 15)

| Id | Sev | Where | Gap → expected behavior | Model ref |
|---|---|---|---|---|
| G-CI-01 | blocker | `catalog/inventory/service/impl/InventoryServiceImpl.java:45-52`; `mappers/InventoryServiceMapper.java` | Does nothing; `ReserveStockResult` maps no fields. `InventoryPositionRepository` is not injected. Expected, all-or-nothing: load each `InventoryPosition` by `sku`, check `availableQuantity ≥ quantity`. If all fit: one `StockReservation` per line (`reservationId`, `reservedAt`, `expiresAt`), move available → reserved, save, return `RESERVED` with `StockReservedResult`. Otherwise return `UNAVAILABLE` with requested/available per line. Concurrency: X-04. | `catalog-inventory.zdl:23-37`, `76-111`, `151-156`; `zfl:89-99` |
| G-CI-02 | blocker | `InventoryServiceImpl.java:75-95` | The generated body loads `List.of()` (`TODO CUSTOM_REQUIRED`), so it confirms nothing, and it would emit one `StockReservationConfirmed` **per reservation**, but the event is order-level. Expected: load the order's `RESERVED` reservations by `input.orderId`, guard, `CONFIRMED` + `confirmedAt`, move reserved → sold on each `InventoryPosition`, save, emit **one** event with `items`. | `catalog-inventory.zdl:60-63`, `121-122`, `138-146`; `zfl:155-158` |
| G-CI-03 | major | `InventoryServiceImpl.java:54-73` | Loads `List.of()` (`TODO CUSTOM_REQUIRED`), so it never releases. Expected: load by `orderId` in `RESERVED`/`CONFIRMED`, set `releasedAt` + `releaseReason`, return the quantity to available (from reserved, or for a confirmed reservation from the counter Q-09 decides), save, emit **one** `StockReleased` per order. Nothing to release (redelivery) → emit nothing. | `catalog-inventory.zdl:113-118`, `126-135`; `zfl:223-227` |
| G-CI-04 | blocker | `catalog/inventory/events/processing/mappers/PaymentsProcessingEventsListenerMapper.java` (generated `…MapperImpl.java:27`, `:43`, `:59`) | `reason` is mapped with `Enum.valueOf(StockReleaseReason, event.getReason())`, which throws for provider text. Map a constant per source: `PAYMENT_DECLINED`, `PAYMENT_RETRY_EXHAUSTED`, `PAYMENT_VOIDED`. | `catalog-inventory.zdl:45-50`, `66-69` |
| G-CI-05 | deferred | — | Reservation-expiry job (`ReservationExpired → releaseStock`). Out of scope for this iteration (decision Q-04). | `catalog-inventory.zdl:116`; `zfl:229-239` |
| G-CI-06 | minor | `catalog/inventory/service/impl/mappers/EventsMapper.java:17`, `:20` | `StockReservationConfirmed.items`, `StockReleased.items`/`reason` unmapped: both events need a reservations-list → event mapping plus the input's `reason`. `StockReleased.items` is never initialized on the event class, so `addItems` throws. | `catalog-inventory.zdl:126-146` |
| G-CI-07 | minor | `catalog/inventory/repository/jpa/StockReservationRepository.java:17`; `InventoryPositionRepository.java:17` | No finders: `findByOrderIdAndStatusIn`, `findBySku` (ideally locking, X-04). | — |

### 2.3 payments-processing (steps 3, 4, 5, 9, 12, 13)

| Id | Sev | Where | Gap → expected behavior | Model ref |
|---|---|---|---|---|
| G-PP-01 | major | `payments/processing/application/service/PaymentsProcessingServiceImpl.java:46-60` | Generated pattern: new empty payment, not saved, `attemptCount` not set, no provider call, all three outcomes emitted. Expected **get-or-create by `orderId`**: on `OrderCreated` create (`AUTHORIZATION_REQUESTED`, `attemptCount = 1`, `paymentMethodId` from the input). On `PaymentRetried` load the stored payment and ignore the null input fields, refusing payments already `AUTHORIZED`/`CAPTURED`/`VOIDED` (there is no `from` guard). Then call the provider (X-02) and set exactly one outcome: `AUTHORIZED` (+`providerReference`, `authorizedAt`) / `DECLINED` (+`declinedAt`, `failureReason`) / `FAILED` (+`failedAt`, `failureReason`). | `payments-processing.zdl:84-87`; `zfl:101-140` |
| G-PP-02 | major | `PaymentsProcessingServiceImpl.java:62-82` | Emits both `PaymentRetried` and `PaymentRetryExhausted`; `attemptCount` untouched. Expected: under the limit (Q-07; suggested: 3 attempts), increment `attemptCount`, `AUTHORIZATION_REQUESTED`, emit `PaymentRetried`. At the limit: `RETRY_EXHAUSTED`, emit `PaymentRetryExhausted`. Don't reset `attemptCount` in G-PP-01. | `payments-processing.zdl:89-91`; `zfl:118-130` |
| G-PP-03 | major | `PaymentsProcessingServiceImpl.java:84-104` | Sets `CAPTURED` unconditionally and emits both events. Expected: call provider capture. Success: `CAPTURED` + `capturedAt`, emit `PaymentCaptured`. Failure: **`CAPTURE_FAILED`** + `failureReason`, emit `PaymentCaptureFailed`. | `payments-processing.zdl:46-54`, `93-95`; `zfl:172-182` |
| G-PP-04 | major | `PaymentsProcessingServiceImpl.java:106-124` | Guard (now incl. `CAPTURE_FAILED`), status and save are present. Missing: provider void (skip it when nothing was authorized: `AUTHORIZATION_REQUESTED`/`FAILED`), `voidedAt`, `input.reason → failureReason` (unmapped). | `payments-processing.zdl:97-100`; `zfl:204-214` |
| G-PP-05 | blocker | `payments/processing/adapter/in/event/processing/PaymentsProcessingServiceEventsListener.java:34`, `:41`; `adapter/in/event/shipping/FulfillmentShippingEventsListener.java:29`, `:36` | Four listeners pass `Long id = null`, and `findById(null)` throws. Resolve with `findByOrderId(event.orderId)` (decision). | decision "Correlation by `orderId`" |
| G-PP-06 | minor | `payments/processing/application/service/mapper/EventsMapper.java:14-28` | Unmapped: `reason` on `PaymentDeclined`/`PaymentFailed`/`PaymentRetryExhausted`/`PaymentCaptureFailed`/`PaymentVoided` (from `failureReason`), `PaymentRetryExhausted.exhaustedAt`, `PaymentRetried.retriedAt`. | `payments-processing.zdl:102-178` |
| G-PP-07 | minor | `payments/processing/application/port/out/jpa/PaymentRepository.java:17` | No `findByOrderId` (`orderId` is unique and is now the payment's only business key). | `payments-processing.zdl:27` |

### 2.4 fulfillment-shipping (step 8)

| Id | Sev | Where | Gap → expected behavior | Model ref |
|---|---|---|---|---|
| G-FS-01 | major | `fulfillment/shipping/FulfillmentShippingServiceImpl.java:43-55`; `mappers/EventsMapper.java:17` | Generated pattern: not saved, `fulfillmentId`/`scheduledAt` not set, both events emitted. Expected: create the fulfillment and decide through a stand-in (Q-08). Success: `SCHEDULED` + `scheduledAt`, emit `FulfillmentScheduled`. Failure: `FAILED` + `failedAt` + `failureReason`, emit `FulfillmentFailed` (`reason` unmapped). Idempotent on `orderId`. | `fulfillment-shipping.zdl:80-105`; `zfl:160-170` |

### 2.5 notifications-consumer (steps 10, 11, 17)

| Id | Sev | Where | Gap → expected behavior | Model ref |
|---|---|---|---|---|
| G-NC-01 | major | `notifications/consumer/service/impl/NotificationsConsumerServiceImpl.java:47-59` | Generated pattern: new empty `Notification`, never saved; required fields unset; no sender call (X-03); both outcomes emitted. Expected: build `ORDER_CONFIRMATION`/`EMAIL`, send, then `SENT` + `sentAt` with `OrderConfirmationSent`, or `FAILED` + `failedAt` + `failureReason` with `NotificationDeliveryFailed`. | `notifications-consumer.zdl:88-90`; `zfl:184-192` |
| G-NC-02 | major | `NotificationsConsumerServiceImpl.java:61-73` | Same pattern, `STOCK_UNAVAILABLE`; `skus` from `input.unavailableItems[].sku`. | `notifications-consumer.zdl:92-94`; `zfl:194-202` |
| G-NC-03 | major | `NotificationsConsumerServiceImpl.java:75-87` | Same pattern, `ORDER_CANCELLED`; the reason goes into the body. | `notifications-consumer.zdl:96-98`; `zfl:252-260` |
| G-NC-04 | minor | `notifications/consumer/service/impl/mappers/EventsMapper.java:14`, `:24` | `OrderCancelledNotificationSent.reason` unmapped (from the input); `NotificationDeliveryFailed.reason` from `failureReason`. | `notifications-consumer.zdl:122-144` |

### 2.6 Flow-level tests and fixtures

| Id | Sev | Gap |
|---|---|---|
| G-FT-01 | major | No flow-level test. Needed, one per ZFL `end` outcome (`zfl:262-266`): **completed**, **stockGone**, and **orderCancelled** through declined, retry exhausted, fulfillment failed → void, and capture failed → void. Reservation expiry is deferred. |
| G-FT-02 | major | The flow service tests are `// TODO` stubs: `OrdersCheckoutServiceTest` (3), `InventoryServiceTest` (3), `PaymentsProcessingServiceTest` (4), `FulfillmentShippingServiceTest` (`scheduleFulfillment`), `NotificationsConsumerServiceTest` (3). Each needs: happy path, every alternative outcome, invalid transition, not found by `orderId`, idempotent redelivery; payments also the retry path through `authorizePayment` and capture-failed → void. |
| G-FT-03 | major | `src/test/java/.../orders/checkout/adapters/web/OrdersCheckoutApiIntegrationTest.java`: `testStartOrderCheckout_201` sends `"currency": "currency-6cayd2rb9h3o"` (violates `maxLength: 3`) and an unknown SKU × 44, yet expects 201, and asserts `confirmedAt`/`cancelledAt`/`cancellationReason` on a new order. `testGetOrder_200` (`:91`) uses `id = ""` and asserts `cancelledAt`/`cancellationReason` on the fixture order, which is confirmed and not cancelled. Replace with a valid happy path (`SKU-001`, `EUR`), a 400 case, a 409 stock-unavailable case (decision Q-05), and `getOrder` for id 1 plus a 404. |
| G-FT-04 | minor | `OrdersCheckoutApiControllerTest.java:32` posts a `null` body and expects 201. (`getOrderTest` was aligned to fixture id 1.) |
| G-FT-05 | major | The fixtures (`src/test/resources/data.sql`, `data/jpa/*/1.json`) hold one record per entity, in post-confirmation states, and the chain is inconsistent (fulfillment `SCHEDULED` and confirmation `SENT` while the payment is `AUTHORIZED`). Missing command preconditions: `Order` `CREATED`; `StockReservation` `RESERVED`; `Payment` `AUTHORIZATION_REQUESTED`, `FAILED` below and at the retry limit, `CAPTURE_FAILED`, `CAPTURED`. Regenerate with `/zenwave-test-scenarios`. |
| G-FT-06 | minor | `orders/checkout/config/ServicesInMemoryConfig.java` builds `new OrdersCheckoutServiceImpl(orderRepository(), eventPublisher)`. After G-OC-01/02 it also needs an `InventoryService`; payments and notifications need their port stubs (X-02, X-03). |

## 3. Baseline: not touched by any flow

| Id | Sev | Where | Gap |
|---|---|---|---|
| G-CI-10 | major | `catalog/inventory/service/impl/InventoryBackofficeServiceImpl.java:47-54`; `mappers/InventoryBackofficeServiceMapper.java:18-21` | `createInventoryPosition`: `initialQuantity → availableQuantity` unmapped, `reservedQuantity`/`soldQuantity` not set to 0 (NOT NULL), no duplicate-`sku` check. Precondition for G-CI-01. |
| G-CI-11 | major | `InventoryBackofficeServiceImpl.java:56-78`; `mappers/EventsMapper.java:14`, `:23` | `receiveStock`/`adjustStock`: generated pattern, counters not changed, never saved; `StockReceived`/`StockAdjusted` fields unmapped. Negative-stock guard → Q-10. |
| G-CI-12 | major | `catalog-inventory.zdl:186-197` | `@rest("/inventory")` with no controller: the inventory `ZDLToOpenAPIPlugin` is commented out and `zenwave-scripts.zw` has no `OpenAPIControllersPlugin` for it. A generation step (human). |
| G-CP-01 | major | `catalog-products.zdl:69-87` | `@rest("/products")` with no controller: `catalog-products-openapi.yml` doesn't exist and `zenwave-scripts.zw` has no controller block for it. A generation step (human). |
| G-CP-02 | minor | `catalog/products/mappers/EventsMapper.java:14`, `:16`; `src/test/java/.../catalog/products/ProductCatalogServiceTest.java` | `ProductPublished`/`ProductRetired`: `productId`, `publishedAt`, `retiredAt` unmapped. Tests are TODO. |
| G-FS-10 | minor | `fulfillment/shipping/FulfillmentShippingServiceImpl.java:57-74` | `markReadyToShip` is unreachable (no `@rest`, no listener; the flow captures on `FulfillmentScheduled`, `zfl:178`, without waiting for it). |
| G-TST-01 | minor | `src/test/java/.../*RepositoryIntegrationTest.java` | Generated repository tests assert `Instant.now()` and empty strings. Rewrite them against the loaded fixtures. |

## 4. Cross-cutting

| Id | Sev | Gap |
|---|---|---|
| X-01 | major | **Idempotency under redelivery.** `config/ExternalizationConfiguration.java:22-23` resubmits incomplete publications every 30 s. Create-type commands (`authorizePayment`, `scheduleFulfillment`) must find the existing record by `orderId`; notifications key on `orderId + type`; transition commands treat "already in target state" as a no-op. |
| X-02 | major | **Payment provider port** (authorize / capture / void) plus a stand-in adapter whose outcome can be forced in tests (Q-11). |
| X-03 | major | **Notification sender port** (e-mail) plus a stand-in adapter (Q-11). |
| X-04 | major | **Inventory concurrency during a drop.** Reservations decrement a hot `InventoryPosition`: optimistic retry on `version`, or a locking `findBySku`, so it never oversells (`zfl:49-50`, `89-93`). |
| X-05 | minor | `config/WebExceptionsTranslator.java` only handles bean validation. Map `NoSuchElementException` → 404, the per-module `InvalidStateTransitionException`s → 409, and stock-unavailable → 409 (G-OC-01). |
| X-06 | minor | Event-driven inputs aren't validated. Note that `AuthorizePaymentInput` from `PaymentRetried` is partial by design: don't add `@Validated` there without a separate validation group. |
| X-07 | minor | No business-id generation (`orderId`, `reservationId`, `fulfillmentId`, `notificationId`). Format: Q-12 (suggested: prefixed, as in the fixtures). |
| X-08 | minor | `mvn compile` prints 56 MapStruct unmapped-target warnings (`target/compile.log`). The event-mapper and listener-mapper ones are the G-* mapping findings. Expected, so don't silence them: the 4 on `PaymentsProcessingServiceEventsListenerMapper` (`PaymentRetried`, Q-02) and the `id`/`version`/`status` ones on entity create mappers. |

## 5. Open questions (→ `/speckit-clarify`)

**Answered** (recorded in `docs/zenwave-sdd-overlay.md`, "Decisions already taken"):

| Id | Question | Answer | Affects |
|---|---|---|---|
| Q-01 | Five event-driven commands take a surrogate `(id)` (`confirmOrder`, `cancelOrder`, `retryPayment`, `capturePayment`, `voidPayment`), but their events carry only `orderId`. Change the models or look it up? | Look it up in code with `findByOrderId(event.orderId)` | G-OC-06, G-PP-05 |
| Q-02 | `PaymentRetried` lacks the payment data `AuthorizePaymentInput` requires (T-01). Add it to the event or load it? | Load the stored `Payment` by `orderId`; one `Payment` per order | G-PP-01 |
| Q-03 | `@transition(to:)` holds one state; how are alternative outcomes represented? | `to` names the happy-path state; alternative outcomes set their lifecycle state in code | all `[A \| B]` commands |
| Q-04 | Is reservation expiry in scope? | No, deferred | M-01, G-CI-05 |
| Q-05 | Is an order persisted when stock is unavailable (the ZFL says no order record exists)? | No; answer 409 with the unavailable items | G-OC-01, G-FT-03 |
| Q-06 | Do prices come from the request or from `catalog.products`? | From the request, for now | G-OC-01 |

**Open** (not model-affecting; settle them in `/speckit-clarify` or as spec assumptions):

| Id | Question | Suggested | Blocks |
|---|---|---|---|
| Q-07 | Maximum payment attempts and backoff? | 3 attempts, short delay, configurable | G-PP-02 |
| Q-08 | What decides `FulfillmentFailed`? | A stand-in, forced in tests | G-FS-01 |
| Q-09 | Releasing a `CONFIRMED` reservation returns stock from which counter? | sold → available | G-CI-03 |
| Q-10 | How is an `InventoryPosition` created for a new product; can `adjustStock` go below 0? | — (baseline) | G-CI-10, G-CI-11 |
| Q-11 | Which payment provider and notification channel? | Stand-in adapters | X-02, X-03 |
| Q-12 | Business-id format? | Prefixed (`ORD-…`), as in the fixtures | X-07 |

## 6. Suggested Spec Kit mapping

**`001-place-order-flow`**: `/speckit-specify` from the ZFL flow, then `/speckit-clarify` for Q-07..Q-12.
The verdict is READY, so `/speckit-plan` can follow; its `research.md` takes §2 of this report.

- **Phase 1, catalog-inventory:** G-CI-01..04, G-CI-06..07, X-04 (G-CI-01 first: checkout calls it).
- **Phase 2, orders-checkout:** G-OC-01..07.
- **Phase 3, payments-processing:** G-PP-01..07, X-02.
- **Phase 4, fulfillment-shipping:** G-FS-01.
- **Phase 5, notifications-consumer:** G-NC-01..04, X-03.
- **Phase 6, cross-cutting and tests:** X-01, X-05..X-08, G-FT-01..06, G-TST-01 (fixtures first, with
  `/zenwave-test-scenarios`).

Keep the ids in tasks, e.g. `T012 [G-CI-04] Map StockReleaseReason constants in PaymentsProcessingEventsListenerMapper`.

**`000-catalog-inventory-baseline`**: G-CI-10..12, Q-10. **`000-catalog-products-baseline`**:
G-CP-01..02. **`000-fulfillment-shipping-baseline`**: G-FS-10. **Deferred**: reservation expiry
(M-01, Q-04, G-CI-05).
