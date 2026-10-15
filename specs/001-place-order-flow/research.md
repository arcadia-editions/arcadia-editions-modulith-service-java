# Research: Place Order Flow

Phase 0 of [plan.md](plan.md). The gap inventory is `docs/zenwave-gap-report.md` §2 (per-step
traceability and findings G-OC/G-CI/G-PP/G-FS/G-NC/G-FT) and §4 (X-01..X-08); it is not repeated here.
Decisions Q-01..Q-06 are in `docs/zenwave-sdd-overlay.md` ("Decisions already taken"); spec assumptions
cover Q-07..Q-09, Q-11, Q-12. This file records the design choices the implementation still needed.

Code paths are relative to `src/main/java/com/arcadiaeditions/`. ZDL references use `<bc>.zdl:<line>`.

## Preconditions verified (2026-10-14)

- Model staleness check: the 5 flow ZDLs, `catalog-products` and `orders-checkout-openapi.yml` in
  `../arcadia-editions-modulith-api` (at `166be74`) are identical to the copies in the `service-all-0.0.1`
  jar the code was generated from. The re-run gap report (same date, after the `orderId` model change)
  is READY.
- Generator ownership (checked in `backend-application-default-2.8.0-SNAPSHOT`,
  `BackendApplicationProjectTemplates`): written with skip-overwrite (generated once) are `*ServiceImpl`,
  `*ServiceMapper`, core `EventsMapper`, `BaseMapper`, `*EventsListener` and its mapper, `*Repository`,
  in-memory repositories, `*ServiceTest`, `ServicesInMemoryConfig`/`RepositoriesInMemoryConfig`.
  Regenerated: entities, inbound/outbound DTOs, inbound service interfaces (`core/inbound/Service.java`),
  `EventPublisher`, `DefaultEventPublisher`, `InMemoryEventPublisher`.

## R-01 Command skeleton for event-driven steps

- **Decision**: every event-driven command follows *resolve by `orderId` → idempotency short-circuit →
  generated `*Transitions.ensureCan…` guard → `@calls` → external port → set status, timestamps,
  counters → save → emit exactly one event* (the `zenwave-fill-gaps` pattern). Alternative outcomes
  (`withEvents [A | B]`) branch on a real result and emit one event; the alternative's lifecycle state
  is set in code (Q-03). Per-command rules are in [data-model.md](data-model.md).
- **Rationale**: `@ApplicationModuleListener` runs each listener asynchronously after commit in its own
  transaction; a listener that throws leaves its publication incomplete and it is resubmitted every 30 s
  (`config/ExternalizationConfiguration.java:22-23`). A redelivery therefore only follows a rolled-back
  attempt, but external side effects (provider call, e-mail) are not rolled back, and a guard that throws
  on "already done" would loop forever. Hence: no-op when already in the target state, idempotency keys
  on external calls.
- **Alternatives**: rely on the guards alone (rejected: an `InvalidStateTransitionException` on
  redelivery is resubmitted indefinitely); a processed-events table (rejected: the business keys already
  identify the work, X-01).

## R-02 Resolving event-driven commands by `orderId` (Q-01) — settled in the models

- **Decision**: no extra code structure. After the human's model change (API repo `166be74`),
  `confirmOrder(ConfirmOrderInput)`, `cancelOrder(CancelOrderInput)`, `retryPayment(RetryPaymentInput)`,
  `capturePayment(CapturePaymentInput)` and `voidPayment(VoidPaymentInput)` take `orderId`. The
  regenerated listeners map it from the event by name, and the regenerated service bodies keep guard,
  status, save and emit. Each command fills one generated placeholder (`Optional.empty()`,
  `TODO CUSTOM_REQUIRED`) with `findByOrderId(input.getOrderId())`, using the new repository finders
  `OrderRepository.findByOrderId` (G-OC-07) and `PaymentRepository.findByOrderId` (G-PP-07).
- **Rationale**: the events carry only `orderId`. Keying the commands by it keeps listeners as pure
  mappings, puts load, idempotency check and guard in one service method and one transaction, and
  respects the orders-checkout `ArchitectureTest` (listeners never touch repositories).
- **Superseded**: the earlier design (developer-owned `OrderLookup` / `PaymentLookup` ports called
  from the listeners) is dropped. G-OC-06 and G-PP-05 are resolved in the gap report.
- **Consequence for `retryPayment` idempotency**: `RetryPaymentInput` carries only `orderId`, so a
  stale `PaymentFailed` from an earlier attempt cannot be told apart by `attemptCount`. The command
  acts only when the payment is `FAILED`. The registry redelivers only after the previous listener
  transaction rolled back, which leaves the payment unchanged, so this is enough for this iteration.
  Stricter protection would mean adding `attemptCount` to `RetryPaymentInput`: a model change (human).

## R-03 Stock-unavailable answer without persisting the order (Q-05, G-OC-01)

- **Decision**: on `ReserveStockOutcome.UNAVAILABLE`, `startOrderCheckout` publishes `StockUnavailable`
  (mapped from the reserve result plus the input, G-OC-04) and throws
  `orders/checkout/core/inbound/StockUnavailableException`, which carries that `StockUnavailable` event.
  The method is `@Transactional(noRollbackFor = StockUnavailableException.class)` so the event
  publication commits. `WebExceptionsTranslator` maps it to `409` problem details listing the unavailable
  items ([contracts/checkout-http.md](contracts/checkout-http.md)). The controller body stays generated.
- **Rationale**: the generated controller returns `ResponseEntity<OrderDTO>` and the service returns
  `Order`, so the outcome cannot travel as a return value without changing generated signatures.
  `reserveStock` writes nothing on `UNAVAILABLE` (all-or-nothing check before any write, R-04), so
  committing the transaction commits only the `StockUnavailable` publication. The exception lives in
  `core/inbound` so both the adapter and the translator in `config` may reference it, and it reuses the
  module's own event type rather than the inventory DTO.
- **Alternatives**: plain throw with rollback (rejected: loses `StockUnavailable`, so no notice, US2-2);
  return a half-built `Order` and detect it in the controller (rejected: ambiguous, and `Order` has no
  "unavailable" state); publish after commit by hand (rejected: bypasses the generated `EventPublisher`).

## R-04 Reservation concurrency during a drop (X-04, G-CI-01, SC-002)

- **Decision**: `InventoryPositionRepository` gets a pessimistic-write finder
  (`@Lock(PESSIMISTIC_WRITE)`, e.g. `findAllBySkuInOrderBySku`). `reserveStock` locks all positions of the
  order in SKU order, checks every line first, and only if all fit creates one `StockReservation` per line
  and moves available → reserved. Duplicate SKUs in one request are summed before the check. Unknown SKU =
  unavailable with `availableQuantity` 0. `@Version` on `InventoryPosition` stays as a safety net.
- **Rationale**: the hot row is contended by design; a lock serializes the check-and-decrement inside the
  checkout transaction, and SKU ordering prevents deadlocks between multi-line orders. Optimistic locking
  alone would surface as `ObjectOptimisticLockingFailureException` at commit of `startOrderCheckout`,
  needing a retry loop around the whole checkout (and its id generation).
- **Alternatives**: optimistic retry (rejected, above); conditional `UPDATE … SET available = available - ?
  WHERE available >= ?` (rejected: bypasses the entity and is harder to make all-or-nothing across lines).
- **Test**: `flows/InventoryConcurrencyTest` against PostgreSQL (in-memory repositories cannot lock).

## R-05 Payment retry policy (Q-07, G-PP-02)

- **Decision**: `arcadia.payments.max-attempts` (default 3) and `arcadia.payments.retry-delay` (default
  `200ms`, `0` in `application-test.yml`) in a `@ConfigurationProperties` record (`config/FlowProperties`).
  `retryPayment`: `attemptCount < max` → `attemptCount + 1`, `AUTHORIZATION_REQUESTED`, emit
  `PaymentRetried`; otherwise `RETRY_EXHAUSTED`, emit `PaymentRetryExhausted`. The delay is applied in the
  `PaymentRetried` listener before calling `authorizePayment`.
- **Rationale**: matches the spec assumption (3 attempts, short delay, configurable). A short sleep on the
  async listener thread is acceptable for the stand-in provider; tests run with 0.
- **Alternatives**: scheduled re-attempt from a persisted `nextAttemptAt` (rejected for this iteration: needs
  a field the ZDL does not have, i.e. a model change); no delay (rejected: the spec states one).

## R-06 Stand-in external ports (X-02, X-03, Q-08, Q-11)

- **Decision**: one port interface per external capability, in the layer the module's layout uses for
  outbound ports, plus a deterministic stand-in as the default bean:
  - payments: `application/port/out/provider/PaymentProvider` (`authorize`, `capture`, `voidAuthorization`), stand-in `adapter/out/provider/StandInPaymentProvider`;
  - fulfillment: `FulfillmentDecision` (`decide(ScheduleFulfillmentInput)`), stand-in `StandInFulfillmentDecision` (flat layout, module root);
  - notifications: `service/NotificationSender` (`send(Notification)`), stand-in `service/impl/StandInEmailNotificationSender` (the layered layout has no infrastructure package; `JMoleculesArchitectureTest` enforces no layering rule).

  Outcomes are forced by data so REST-driven flow tests need no bean overrides (trigger values in
  [contracts/stand-in-adapters.md](contracts/stand-in-adapters.md)). In-memory service tests inject the
  same stand-ins through `ServicesInMemoryConfig` (G-FT-06).
- **Rationale**: Q-11/Q-08 chose stand-ins; data-driven triggers (as payment providers' test cards do)
  make every ZFL `end` outcome reachable through the public API.
- **Alternatives**: Mockito beans per test (rejected: flow tests would need different contexts per outcome
  and could not run against a running app for the quickstart).

## R-07 HTTP error responses not declared in the OpenAPI (X-05, G-FT-03) — for human review

- **Decision**: `WebExceptionsTranslator` maps `StockUnavailableException` → 409 (R-03),
  `*Transitions.InvalidStateTransitionException` → 409, `NoSuchElementException` → 404; bean validation
  → 400 is already generated. Bodies are RFC 9457 problem details ([contracts/checkout-http.md](contracts/checkout-http.md)).
- **Finding for the human**: `orders-checkout-openapi.yml` declares only `201` for `startOrderCheckout`
  and `200` for `getOrder`. The 400/404/409 responses work at runtime but are undocumented. This does not
  block the feature (no ZDL structure is missing; error mapping is behavior), but documenting them is an
  OpenAPI/ZDL generation change, which only a human makes. Suggest adding it as a minor item when the gap
  report is re-run.

## R-08 Business ids (Q-12, X-07)

- **Decision**: `<PREFIX>-<UUID v4, upper case>` with prefixes `ORD`, `RES`, `FUL`, `NOT` (fixtures use
  `ORD-001`, `RES-001`, `FUL-001`, `NOT-001`). Generated in the service that creates the aggregate.
  `Payment` has no business id; it is keyed by unique `orderId`.
- **Alternatives**: sequences (rejected: the order reference is returned to the customer; guessable).

## R-09 Flow-level tests (G-FT-01, FR-018)

- **Decision**: `src/test/java/com/arcadiaeditions/flows/PlaceOrderFlowTest`, `@SpringBootTest` +
  `@EnableDockerCompose` (PostgreSQL), driving `POST /api/orders/start-checkout` and waiting with
  Spring Modulith `Scenario` (`@EnableScenarios`, `spring-modulith-test` already in the pom) for the
  terminal event of each ZFL `end` outcome: `completed`, `stockGone`, `orderCancelled` × 4 paths (declined,
  retry exhausted, fulfillment failed → void, capture failed → void). Each test also asserts the
  cross-module end state (order status/reason, reservations, inventory counters, payment status, one
  notification). Reservation expiry is excluded (deferred).
- **Rationale**: one JVM, real transactions and the real event publication registry; the `fill-gaps`
  skill prescribes `Scenario` for moduliths.
- **Alternatives**: Awaitility polling on repositories (kept as fallback inside `Scenario.andWaitForStateChange`).

## R-10 Constitution wording vs generator behavior — resolved

Constitution v1.0.1 (PATCH, 2026-10-14) lists `*Repository` and the MapStruct mappers as generated once
(developer-owned, limited to query methods and mapping configuration), matching the generator's
skip-overwrite setting. Adding finders and mappings there complies with principle III.

## R-11 Time

- **Decision**: a `Clock` bean (`config/ClockConfiguration`), injected into the five service impls;
  timestamps use `Instant.now(clock)`. Tests use a fixed clock. `StockReservation.expiresAt` =
  `reservedAt + 10 minutes` (ZFL `@time("10 minutes after OrderCreated …")`), stored even though expiry is
  deferred.

## R-12 Open notes carried into tasks

- `ModularityTest` (`ApplicationModules.of(Application.class).verify()`) does not exist yet; add it. If it
  fails on generated structure (package exposure), record it as a finding instead of changing generated files.
- X-06: do not add `@Validated` on `AuthorizePaymentInput` (partial by design on retry, Q-02).
- X-08: after the mapping tasks, the only remaining MapStruct warnings are the expected ones listed in the gap report.
- `markReadyToShip` (G-FS-10) and baselines (G-CI-10..12, G-CP-*) are out of this feature. G-CI-10 is a
  precondition for creating inventory positions through the API; fixtures cover it for tests.
