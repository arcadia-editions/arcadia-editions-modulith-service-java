# Data Model: Place Order Flow

The structure (entities, fields, enums, inputs, outputs, events, lifecycle states and transitions) is
owned by the ZDL and is **not restated here** (constitution, "Models own structure"). This file is an
index into the models plus the behavioral rules the models leave to code: how each command finds its
aggregate, which state each outcome sets, and what makes it idempotent.

ZDL references use `<bc>.zdl:<line>`, short for `../arcadia-editions-modulith-api/<bc>-domain-model.zdl`
(models at `166be74`). ZFL references use `zfl:<line>` for `place-order-flow.zfl`.

## Model index

| Aggregate | Defined at | Lifecycle (`@lifecycle`, states) | Business key | Commands in this flow |
|---|---|---|---|---|
| `Order` | `orders-checkout.zdl:24-59` | `status`, initial `CREATED`; `OrderStatus` `:55-59` | `orderId` (unique) | `startOrderCheckout`, `confirmOrder`, `cancelOrder`, `getOrder` (`:92-110`) |
| `StockReservation` | `catalog-inventory.zdl:23-43` | `status`, initial `RESERVED`; `ReservationStatus` `:39-43` | `reservationId`; grouped by `orderId` (one per line) | `reserveStock`, `confirmStockReservation`, `releaseStock` (`:107-122`) |
| `InventoryPosition` | `catalog-inventory.zdl:150-156` | none (counters) | `sku` (unique) | changed by the three reservation commands |
| `Payment` | `payments-processing.zdl:24-54` | `status`, initial `AUTHORIZATION_REQUESTED`; `PaymentStatus` `:45-54` | `orderId` (unique, one per order) | `authorizePayment`, `retryPayment`, `capturePayment`, `voidPayment` (`:82-100`) |
| `Fulfillment` | `fulfillment-shipping.zdl:23-55` | `status`, initial `SCHEDULED`; `FulfillmentStatus` `:51-55` | `fulfillmentId`; `orderId` unique | `scheduleFulfillment` (`:80-84`) |
| `Notification` | `notifications-consumer.zdl:24-59` | `status`, initial `PENDING`; `NotificationStatus` `:55-59` | `notificationId`; idempotency by `orderId` + `type` | `sendOrderConfirmation`, `sendStockUnavailableNotification`, `sendOrderCancelledNotification` (`:86-98`) |

Events, inputs and outputs: each ZDL file, after its `service` block. The HTTP shapes are in
`../arcadia-editions-modulith-api/orders-checkout-openapi.yml`.

## Behavioral rules per command

"Resolve" = how the command finds its aggregate (Q-01: by `orderId`; for the five `orderId`-keyed
commands, `findByOrderId(input.orderId)` fills the generated `Optional.empty()` placeholder). "No-op when" = idempotent redelivery (X-01): return the current
aggregate, emit nothing. Guards are the generated `*Transitions.ensureCan…` methods.

### orders-checkout

| Command (trigger) | Resolve | Outcomes → state set in code, event | No-op when | Other rules |
|---|---|---|---|---|
| `startOrderCheckout` (REST, `zfl:82`) | creates | `reserveStock` = `RESERVED` → save `CREATED`, `orderId` (R-08), `totalAmount` = Σ quantity × unitPrice, `createdAt`; emit `OrderCreated` (`amount` ← `totalAmount`, G-OC-03). `UNAVAILABLE` → **not saved**; emit `StockUnavailable` (items from `StockUnavailableResult`, G-OC-04); throw `StockUnavailableException` → 409 (R-03) | — (synchronous request) | `@calls reserveStock` with `{orderId, items[sku, quantity]}` |
| `confirmOrder(ConfirmOrderInput)` (`PaymentAuthorized`, `zfl:146`) | `findByOrderId(input.orderId)` | `CONFIRMED`, `confirmedAt`; emit `OrderConfirmed` | status `CONFIRMED` | `@calls confirmStockReservation({orderId})` before the state change |
| `cancelOrder(CancelOrderInput)` (`StockReleased`, `zfl:247`) | `findByOrderId(input.orderId)` | `CANCELLED`, `cancelledAt`, `cancellationReason` ← `input.reason` (the `StockReleased.reason` enum name); emit `OrderCancelled` (`reason`) | status `CANCELLED` | from `CREATED` or `CONFIRMED` |
| `getOrder(id)` (REST) | `findById` | — | — | 404 when absent (already generated) |

### catalog-inventory

| Command (trigger) | Resolve | Outcomes → state, event | No-op when | Other rules |
|---|---|---|---|---|
| `reserveStock` (`@calls` from checkout) | locks `InventoryPosition`s by `sku`, SKU order (R-04) | all lines fit → one `StockReservation` per line `RESERVED`, `reservationId`, `reservedAt`, `expiresAt` = +10 min; available → reserved; return outcome `RESERVED` + `StockReservedResult`. Any line short → nothing written; return `UNAVAILABLE` + `StockUnavailableResult` (requested/available per short line) | — | duplicate SKUs summed; unknown SKU → available 0; emits no event (the caller does) |
| `confirmStockReservation` (`@calls` from `confirmOrder`) | `findByOrderIdAndStatusIn(orderId, [RESERVED])` | each → `CONFIRMED`, `confirmedAt`; reserved → sold; emit **one** `StockReservationConfirmed` with all items | no `RESERVED` reservation left | runs in the caller's transaction |
| `releaseStock` (`PaymentDeclined` / `PaymentRetryExhausted` / `PaymentVoided`, `zfl:224`) | `findByOrderIdAndStatusIn(orderId, [RESERVED, CONFIRMED])` | each → `RELEASED`, `releasedAt`, `releaseReason`; `RESERVED`: reserved → available; `CONFIRMED`: sold → available (Q-09); emit **one** `StockReleased` with items and reason | nothing to release → emit nothing | reason is a constant per trigger: `PAYMENT_DECLINED`, `PAYMENT_RETRY_EXHAUSTED`, `PAYMENT_VOIDED` (G-CI-04) |

### payments-processing

| Command (trigger) | Resolve | Outcomes → state, event | No-op when | Other rules |
|---|---|---|---|---|
| `authorizePayment` (`OrderCreated`, `zfl:108`) | get-or-create by `orderId` | create: `AUTHORIZATION_REQUESTED`, `attemptCount` 1, fields from input. Then provider `authorize` (idempotency key `orderId:attemptCount`): authorized → `AUTHORIZED`, `providerReference`, `authorizedAt`, emit `PaymentAuthorized`; declined → `DECLINED`, `declinedAt`, `failureReason`, emit `PaymentDeclined`; failed → `FAILED`, `failedAt`, `failureReason`, emit `PaymentFailed` | existing payment not in `AUTHORIZATION_REQUESTED` | never resets `attemptCount` |
| `authorizePayment` (`PaymentRetried`, `zfl:132`) | load by `orderId` (Q-02); input payment fields are null and ignored | same three outcomes as above | status ≠ `AUTHORIZATION_REQUESTED` (covers `AUTHORIZED`, `CAPTURED`, `VOIDED` …) | retry delay applied in the listener (R-05) |
| `retryPayment(RetryPaymentInput)` (`PaymentFailed`, `zfl:124`) | `findByOrderId(input.orderId)` | `attemptCount < max-attempts` → `attemptCount + 1`, `AUTHORIZATION_REQUESTED`, emit `PaymentRetried`; else `RETRY_EXHAUSTED`, emit `PaymentRetryExhausted` | status ≠ `FAILED` | a stale `PaymentFailed` cannot be detected by `attemptCount` (not in the input); see research R-02 |
| `capturePayment(CapturePaymentInput)` (`FulfillmentScheduled`, `zfl:178`) | `findByOrderId(input.orderId)` | provider `capture`: ok → `CAPTURED`, `capturedAt`, emit `PaymentCaptured`; fails → `CAPTURE_FAILED`, `failureReason`, emit `PaymentCaptureFailed` (Q-03) | status `CAPTURED` or `CAPTURE_FAILED` | guard: from `AUTHORIZED` |
| `voidPayment(VoidPaymentInput)` (`FulfillmentFailed` / `PaymentCaptureFailed`, `zfl:211`) | `findByOrderId(input.orderId)` | `VOIDED`, `voidedAt`, `failureReason` ← `input.reason`; provider `voidAuthorization` only if the payment had been authorized (`AUTHORIZED`, `CAPTURE_FAILED`); emit `PaymentVoided` | status `VOIDED` | never voids a `CAPTURED` payment (guard) |

### fulfillment-shipping

| Command (trigger) | Resolve | Outcomes → state, event | No-op when | Other rules |
|---|---|---|---|---|
| `scheduleFulfillment` (`OrderConfirmed`, `zfl:166`) | get-or-create by `orderId` | `FulfillmentDecision` (R-06): scheduled → `SCHEDULED`, `fulfillmentId`, `scheduledAt`, emit `FulfillmentScheduled`; failed → `FAILED`, `failedAt`, `failureReason`, emit `FulfillmentFailed` (`reason`) | a fulfillment for the `orderId` exists | `markReadyToShip` not in the flow (G-FS-10) |

### notifications-consumer

| Command (trigger) | Resolve | Outcomes → state, event | No-op when | Other rules |
|---|---|---|---|---|
| `sendOrderConfirmation` (`PaymentCaptured`, `zfl:189`) | get-or-create by `orderId` + `ORDER_CONFIRMATION` | `NotificationSender` ok → `SENT`, `sentAt`, emit `OrderConfirmationSent`; fails → `FAILED`, `failedAt`, `failureReason`, emit `NotificationDeliveryFailed` | a notification with that key is `SENT` or `FAILED` | `channel` `EMAIL`, `recipient` = customer e-mail, `notificationId` (R-08), `createdAt` |
| `sendStockUnavailableNotification` (`StockUnavailable`, `zfl:199`) | `orderId` + `STOCK_UNAVAILABLE` | same, `StockUnavailableNotificationSent` | same | `skus` ← `unavailableItems[].sku` |
| `sendOrderCancelledNotification` (`OrderCancelled`, `zfl:257`) | `orderId` + `ORDER_CANCELLED` | same, `OrderCancelledNotificationSent` (`reason`) | same | reason in the body |

A delivery failure does not change the order outcome (spec edge case); `NotificationDeliveryFailed` has
no consumer in the flow.

## Cross-aggregate invariants (checked by tests)

- Per `sku`: `availableQuantity + reservedQuantity + soldQuantity` is constant across reserve, confirm
  and release (no oversell, SC-002; stock returned, SC-006).
- Per `orderId`: at most one `Payment`, one `Fulfillment`, one `Notification` per `type`; `Payment` is
  never `CAPTURED` when the `Order` is `CANCELLED` (SC-003).
- Per `orderId`: exactly one ZFL terminal event among `OrderConfirmationSent`,
  `StockUnavailableNotificationSent`, `OrderCancelledNotificationSent` (SC-001, SC-005), except when
  delivery fails (`NotificationDeliveryFailed` instead).

## Fixture states needed (G-FT-05, generated with `/zenwave-test-scenarios`)

`Order`: `CREATED`, `CONFIRMED`, `CANCELLED`. `StockReservation`: `RESERVED`, `CONFIRMED`, `RELEASED`
(several per order). `InventoryPosition`: plenty, exactly-enough, zero available. `Payment`:
`AUTHORIZATION_REQUESTED`, `AUTHORIZED`, `FAILED` below and at the attempt limit, `CAPTURE_FAILED`,
`CAPTURED`, `VOIDED`. `Fulfillment`: `SCHEDULED`, `FAILED`. `Notification`: one `SENT` per type for the
redelivery tests. Chains must be consistent across contexts by `orderId`.
