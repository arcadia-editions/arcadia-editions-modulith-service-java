# Contract: External ports and stand-in adapters

Three external capabilities are outside the models (gap report X-02, X-03; spec assumptions Q-08,
Q-11). Each gets a developer-owned port interface and a deterministic stand-in that is the default bean
(research R-06). Real adapters are a later iteration.

Outcomes are forced by request data, so every ZFL `end` outcome can be reached through
`POST /api/orders/start-checkout` without overriding beans. Any other value takes the happy path.

## `PaymentProvider` (payments-processing)

`payments/processing/application/port/out/provider/PaymentProvider`

| Operation | Called by | Result |
|---|---|---|
| `authorize(orderId, attempt, amount, currency, paymentMethodId)` | `authorizePayment` | `Authorized(providerReference)` / `Declined(reason)` / `Failed(reason)` |
| `capture(providerReference, amount, currency)` | `capturePayment` | `Captured` / `CaptureFailed(reason)` |
| `voidAuthorization(providerReference)` | `voidPayment` (only when the payment had been authorized) | `Voided` |

`orderId` + `attempt` is the idempotency key: the same key returns the same result.

Stand-in triggers, by `paymentMethodId`:

| `paymentMethodId` | Behavior | ZFL path |
|---|---|---|
| `pm_decline` | authorize → `Declined("card_declined")` | `orderCancelled` via `PaymentDeclined` |
| `pm_fail_once` | attempt 1 → `Failed("processor_unavailable")`, attempt 2 → `Authorized` | `completed` via one retry |
| `pm_fail_always` | every attempt → `Failed("processor_unavailable")` | `orderCancelled` via `PaymentRetryExhausted` |
| `pm_capture_fail` | authorize → `Authorized`; capture → `CaptureFailed("capture_rejected")` | `orderCancelled` via `PaymentCaptureFailed` → void |
| anything else | authorize → `Authorized("STANDIN-<orderId>")`; capture → `Captured` | `completed` |

## `FulfillmentDecision` (fulfillment-shipping)

`fulfillment/shipping/FulfillmentDecision`: `decide(ScheduleFulfillmentInput)` → `Scheduled` / `Failed(reason)`.

| Trigger | Behavior | ZFL path |
|---|---|---|
| `shippingAddress.postalCode` = `00000` | `Failed("carrier_unavailable")` | `orderCancelled` via `FulfillmentFailed` → void |
| anything else | `Scheduled` | continues to capture |

## `NotificationSender` (notifications-consumer)

`notifications/consumer/service/NotificationSender`: `send(Notification)` → `Delivered` / `DeliveryFailed(reason)`.
The stand-in logs the message (recipient, subject, body) instead of sending e-mail.

| Trigger | Behavior |
|---|---|
| `recipient` ends with `@fail.invalid` | `DeliveryFailed("mailbox_unavailable")` → `NotificationDeliveryFailed` |
| anything else | `Delivered` |

## Test wiring

- In-memory service tests: `ServicesInMemoryConfig` of each module constructs the service with the same
  stand-in (G-FT-06).
- Flow tests and the quickstart: the stand-ins are the default beans; no profile needed.
- The retry delay is `arcadia.payments.retry-delay` (`0` in `application-test.yml`); the attempt limit is
  `arcadia.payments.max-attempts` (default 3), research R-05.
