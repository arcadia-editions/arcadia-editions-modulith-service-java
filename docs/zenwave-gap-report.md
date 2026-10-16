# ZenWave Gap Report — arcadia-editions-modulith-service-java

| | |
|---|---|
| Date | 2026-10-15 |
| Code baseline | `553445d` plus the uncommitted Phase 1-8 implementation of `001-place-order-flow` in the working tree (generated code in `2d5a0a3`) |
| Flows | `../arcadia-editions-architecture/business-flows/place-order-flow.zfl` |
| Models | `../arcadia-editions-modulith-api/*-domain-model.zdl` (6 files; API repo `085b930` committed). Staleness check: OK after the 2026-10-16 repackage (T-06) |
| Decisions | `docs/zenwave-sdd-overlay.md`, "Decisions already taken" |
| Deployment shape | Modulith (Spring Modulith 2.0, in-process events, JDBC event publication registry) |
| Produced with | `zenwave-gap-report` (read-only), 2026-10-15 re-run after Phase 1-8 (T090). Previous run 2026-10-14 |

Code paths are relative to `src/main/java/com/arcadiaeditions/` unless they start with `src/`.
ZDL references use `<bc>.zdl:<line>`, short for `../arcadia-editions-modulith-api/<bc>-domain-model.zdl`.
ZFL references use `zfl:<line>` for `place-order-flow.zfl`.

## Model readiness: ✅ READY

> **READY.** No open model gap. M-02 is resolved (2026-10-16): the ZDL linter/SDK now accepts a list in
> `@transition(to:)`, so `payments-processing.zdl:90` `to: [FAILED, RETRY_EXHAUSTED]` lints clean. Not regenerated
> by human decision: the generated `PaymentTransitions` only encodes `from` states, so `ensureCanRetryPayment`
> (`from FAILED`) is unchanged and the service sets `FAILED`/`RETRY_EXHAUSTED` in code.
>
> Not blocking, deferred by decision: M-01 (reservation expiry, Q-04, G-CI-05).

## Summary

1. **All 15 flow commands are implemented** and every ZFL edge has an `@ApplicationModuleListener` trigger in
   code, except the deferred reservation-expiry timer. Both `@calls` (`reserveStock`, `confirmStockReservation`)
   are real calls into `InventoryService`. Every blocker and major finding of the 2026-10-14 report that concerns
   the flow is resolved (section 2).
2. **Verification reported by the caller (not re-run by this audit):** `mvn test` 84 green (`ModularityTest`
   `@Disabled`, R-12); failsafe integration tests 42 green; after the 2026-10-16 repackage the empty-items 400
   test is enabled and green. The code was read in
   this audit: `PlaceOrderFlowTest` covers every ZFL `end` outcome (completed, completed with one retry,
   stockGone, cancelled via declined / retry exhausted / fulfillment failed + void / capture failed + void) and
   `InventoryConcurrencyTest` covers X-04.
3. **Open for the flow: 0 blocker, 0 major.** M-02 and G-OC-08 are resolved (2026-10-16). R-12 is accepted by
   the human as a Spring Modulith bug to be reported upstream; `ModularityTest` stays `@Disabled` until it is
   fixed. Minors: R-07, G-FT-07, G-PP-08, X-09. The quickstart manual validation (T089) is done.
4. **MapStruct:** 43 `Unmapped target` warnings (was 58), all expected: the 4 on
   `PaymentsProcessingServiceEventsListenerMapper` (X-08), `id`/`version`/`status` on entity create/update
   mappers, unused generated entity mappers `asX(<Input>)`, and baseline modules (`catalog.products`, inventory
   backoffice). No event, listener or flow-command mapper has an unmapped event field any more.

| Severity | Model (M) | Code (G) | Cross-cutting (X) / Generation (R) | Open questions (Q) |
|---|---|---|---|---|
| blocker | 0 | 0 | 0 | — |
| major | 0 | 0 | 0 | — |
| accepted (external bug) | — | — | 1 (R-12) | — |
| minor | 0 | 2 (G-FT-07, G-PP-08) | 2 (R-07, X-09) | — |
| deferred | 1 (M-01) | 1 (G-CI-05) | — | 1 (Q-04) |
| baseline (not the flow) | — | section 3 | — | Q-10 |

### Tooling notes (not findings)

- **T-01.** `lint-zdl.sh --strict`: 4 `zdl.data-lineage` warnings on `payments-processing.zdl:85`
  (`PaymentRetried` lacks `amount`, `currency`, `customerEmail`, `paymentMethodId`): accepted by decision Q-02; the
  service loads the stored `Payment`. No errors (M-02 resolved by the linter fix). The other five ZDLs are clean.
- **T-02.** In Git Bash, run `mvn` with `JAVA_HOME=~/.sdkman/candidates/java/25.0.2-tem`.
- **T-03.** Write the compile log outside `target/` (`clean` deletes it).
- **T-04.** `jbang zw` runs SDK 2.8.0-SNAPSHOT while the pom pins `zenwave.version` 2.6.0; align before regenerating.
- **T-06.** Staleness: resolved 2026-10-16. `package-local-service-all.sh` was run; the jar now carries the
  committed `retryPayment` transition and the human fix `minItems: 1` on `StartOrderCheckoutInput.items` in
  `orders-checkout-openapi.yml` (the ZDL-to-OpenAPI generator bug that emitted `minLength: 1` inside the items is
  being fixed in zenwave-sdk). No regeneration was needed: `PaymentTransitions` is unchanged by the model edit.

## 1. Model gaps and generation changes (human decision; agents never edit the models)

**M-02 · resolved · `retryPayment` transition target** (`payments-processing.zdl:90`). See the verdict above.

**M-01 · deferred · `@timer(api: SchedulerApi, channel: "reservation-expired-event-v1")` refers to an
undeclared API** (`catalog-inventory.zdl:116`). Out of scope for this iteration (Q-04).

**R-07 · minor · generation change (human) · 400/404/409 are not declared in `orders-checkout-openapi.yml`.**
`startOrderCheckout` declares only `201` and `getOrder` only `200`, but the service answers 400 (validation), 409
(stock unavailable, invalid transition) and 404 (`getOrder`), through `WebExceptionsTranslator`. The contract
under-describes the API: clients and generated docs miss the error responses, and the 409 body with
`unavailableItems` is not described. Fix at the source (ZDL/generator), then regenerate. Agents don't make it.

**R-12 · accepted (external bug) · `ApplicationModules.verify()` fails.** 4 inter-module cycles reported on
generated listeners (orders<->payments, orders->fulfillment->payments->orders, via catalog) and access to
non-exposed domain/event types. Human decision (2026-10-16): this is a Spring Modulith bug, to be reported
upstream; `ModularityTest` stays `@Disabled` with the reason
(`src/test/java/com/arcadiaeditions/ModularityTest.java:12`) until it is fixed. See research.md R-12.

## 2. PlaceOrderFlow: traceability and status

| # | ZFL step | Trigger in code | Service body / evidence | Status |
|---|---|---|---|---|
| 1 | `StartOrderCheckout -> startOrderCheckout` (`zfl:82`) | `POST /api/orders/start-checkout`, `orders/checkout/adapters/web/OrdersCheckoutApiController.java:54` | `OrdersCheckoutServiceImpl.java:57`: ids, total, reserve, save `CREATED`, `OrderCreated`; `StockUnavailableException` (`noRollbackFor`) -> 409 | OK (G-OC-01/03/04) |
| 2 | `call reserveStock` (`zfl:84`) | `inventoryService.reserveStock` in `startOrderCheckout` | all-or-nothing, pessimistic lock in SKU order, `RESERVED`/`UNAVAILABLE` | OK (G-CI-01, X-04) |
| 3 | `OrderCreated -> authorizePayment` | `payments/.../OrdersCheckoutEventsListener.java:26` | get-or-create by `orderId`, provider port, exactly one of 3 outcomes | OK (G-PP-01) |
| 4 | `PaymentFailed -> retryPayment` | `PaymentsProcessingServiceEventsListener.java:32` | `findByOrderId`, limit from `FlowProperties`, `PaymentRetried` or `RETRY_EXHAUSTED` + `PaymentRetryExhausted` | OK (G-PP-02, G-PP-07) |
| 5 | `PaymentRetried -> authorizePayment` | `PaymentsProcessingServiceEventsListener.java:27` | stored payment, retry detected by null payment fields | OK, see G-PP-08 |
| 6 | `PaymentAuthorized -> confirmOrder` | `orders/.../PaymentsProcessingEventsListener.java:26` | `findByOrderId`, redelivery no-op, guard, `confirmedAt`, `OrderConfirmed` | OK (G-OC-02, G-OC-07) |
| 7 | `call confirmStockReservation` | called in `confirmOrder` | one event per order, reserved -> sold | OK (G-CI-02) |
| 8 | `OrderConfirmed -> scheduleFulfillment` | `fulfillment/shipping/checkout/OrdersCheckoutEventsListener.java:19` | idempotent by `orderId`, decision port, `SCHEDULED`/`FAILED` | OK (G-FS-01, Q-08) |
| 9 | `FulfillmentScheduled -> capturePayment` | `FulfillmentShippingEventsListener.java:26` | provider capture, `CAPTURED` or `CAPTURE_FAILED` | OK (G-PP-03) |
| 10 | `PaymentCaptured -> sendOrderConfirmation` | `notifications/.../PaymentsProcessingEventsListener.java:26` | `deliverOnce` by `orderId + type`, sender port | OK (G-NC-01) |
| 11 | `StockUnavailable -> sendStockUnavailableNotification` | `notifications/.../OrdersCheckoutEventsListener.java:26` | same, `skus` from the items | OK (G-NC-02, G-OC-04) |
| 12-13 | `FulfillmentFailed` / `PaymentCaptureFailed -> voidPayment` | `FulfillmentShippingEventsListener.java:32`, `PaymentsProcessingServiceEventsListener.java:37` | provider void only when something is held, `voidedAt`, reason | OK (G-PP-04) |
| 14 | `PaymentDeclined / RetryExhausted / Voided -> releaseStock` | `catalog/inventory/events/processing/PaymentsProcessingEventsListener.java:25-39` | constant `StockReleaseReason` per source, one `StockReleased` per order, nothing to release emits nothing | OK (G-CI-03/04) |
| 15 | `ReservationExpired -> releaseStock` | deferred | — | Deferred (G-CI-05, M-01) |
| 16 | `StockReleased -> cancelOrder` | `orders/.../CatalogInventoryEventsListener.java:26` | `findByOrderId`, redelivery no-op, `cancelledAt`, reason | OK (G-OC-05) |
| 17 | `OrderCancelled -> sendOrderCancelledNotification` | `notifications/.../OrdersCheckoutEventsListener.java:32` | reason in body and event | OK (G-NC-03/04) |
| 18 | Customer reads the order | `GET /api/orders/{id}` (404 for an absent id) | — | OK |

**Resolved since the last run** (ids kept): G-OC-01..05, G-OC-07, G-CI-01..04, G-CI-06, G-CI-07, G-PP-01..04,
G-PP-06, G-PP-07, G-FS-01, G-NC-01..04, G-FT-01..06 (service tests, `PlaceOrderFlowTest`, scenario fixtures, API
tests, in-memory config), X-01 (idempotency), X-02 (`PaymentProvider` + `StandInPaymentProvider`), X-03
(`NotificationSender` + `StandInEmailNotificationSender`), X-04 (`PESSIMISTIC_WRITE` finder +
`InventoryConcurrencyTest`), X-05 (`WebExceptionsTranslator`: 404, 409, stock-unavailable 409), X-06 (no
`@Validated` on `AuthorizePaymentInput`), X-07 (`BusinessIds`), X-08 (warnings down to the expected set).

### Open findings for PlaceOrderFlow

| Id | Sev | Owner | Where | Finding |
|---|---|---|---|---|
| R-12 | accepted | human | `src/test/java/com/arcadiaeditions/ModularityTest.java:12` | `verify()` fails; Spring Modulith bug to be reported upstream (section 1). |
| R-07 | minor | human | `orders-checkout-openapi.yml` | 400/404/409 undeclared (section 1). |
| G-FT-07 | minor | agent/human | `config/WebExceptionsTranslator.java:63`; `specs/001-place-order-flow/contracts/checkout-http.md:28` | The 409 problem body has no `"type": "about:blank"` property, which the contract shows. Align the contract (omit `type`) or set it explicitly in the translator. |
| G-PP-08 | minor | agent | `payments/processing/application/service/PaymentsProcessingServiceImpl.java:60` | Retry versus initial trigger of `authorizePayment` is inferred from null `amount` and `paymentMethodId`, because the input has no discriminator (Q-02). Works with the stand-in and the tests; a malformed `OrderCreated` without payment data would be taken as a retry (and fail not-found). Acceptable until the model carries a discriminator. |
| X-09 | minor | agent | `catalog/inventory/service/impl/InventoryServiceImpl.java:28` | `RESERVATION_TTL` (10 min) is a code constant; `expiresAt` is set but nothing expires reservations (deferred G-CI-05). Move it to `FlowProperties` when expiry comes into scope. |

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

The cross-cutting findings of the previous run (X-01..X-08) are resolved for the flow (section 2). Open: X-09;
R-12 accepted (upstream bug). X-08 stays as an accepted state: 43 expected unmapped-target warnings (summary, point 4).

## 5. Open questions (→ `/speckit-clarify`)

**Answered** (recorded in `docs/zenwave-sdd-overlay.md`, "Decisions already taken"):

| Id | Question | Answer | Affects |
|---|---|---|---|
| Q-01 | Five event-driven commands took a surrogate `(id)` (`confirmOrder`, `cancelOrder`, `retryPayment`, `capturePayment`, `voidPayment`), but their events carry only `orderId`. Change the models or look it up? | First: look it up in code. **Revised 2026-10-14: change the models.** The five commands take an input with `orderId` (models `166be74`); the service loads the aggregate with `findByOrderId(input.orderId)` | G-OC-06, G-PP-05 (resolved); G-OC-07, G-PP-07 |
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

**`001-place-order-flow`**: Done (2026-10-16). Remaining minors: R-07 (generation change, human), G-FT-07,
G-PP-08, X-09. R-12 accepted as an upstream Spring Modulith bug.

**`000-catalog-inventory-baseline`**: G-CI-10..12, Q-10. **`000-catalog-products-baseline`**:
G-CP-01..02. **`000-fulfillment-shipping-baseline`**: G-FS-10. **Deferred**: reservation expiry
(M-01, Q-04, G-CI-05).
