---

description: "Task list for 001-place-order-flow (ZFL PlaceOrderFlow)"
---

# Tasks: Place Order Flow

**Input**: Design documents from `specs/001-place-order-flow/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md) (R-01..R-12), [data-model.md](data-model.md) (behavioral rules per command), [contracts/](contracts/) (`checkout-http.md`, `stand-in-adapters.md`), [quickstart.md](quickstart.md)

**Work inventory**: `docs/zenwave-gap-report.md` (verdict READY). Every task carries its finding id(s) (constitution V).

**Tests**: Requested. FR-018 and gap-report findings G-FT-01..06 require service tests, API tests and one flow test per ZFL `end` outcome; constitution IV makes the generated test layers the standard. Within each story, write the tests first and see them fail.

**Organization**: Phases 1-2 build what every story needs. Phases 3-7 follow the user stories of spec.md in priority order; each one implements the ZFL branch that story covers, across all the modules it touches.

## Rules for every task (read before starting)

- **Before any task**: read `docs/zenwave-sdd-overlay.md`. Implement each code task with the `zenwave-fill-gaps` skill (constitution, Development Workflow §3), and use the per-command rules in [data-model.md](data-model.md) ("Resolve", "Outcomes → state", "No-op when").
- **Never edit** `*.zfl` / `*.zdl`, and never hand-edit regenerated structure: entities and enums under `*/domain/`, domain events, input/output DTOs (`*/dtos/`, `*/dto/`), inbound service interfaces (`InventoryService`, `OrdersCheckoutService`, `PaymentsProcessingService`, `FulfillmentShippingService`, `NotificationsConsumerService`), `EventPublisher`, `DefaultEventPublisher`, `InMemoryEventPublisher`, `*Transitions`. If a task can't be done without changing them, **stop** and hand it to a human with the finding id, the ZDL/ZFL lines and the options (constitution, "Model gaps stop work").
- **Command skeleton (R-01)** for event-driven commands: resolve by `orderId` → idempotency short-circuit (return the current aggregate, emit nothing) → generated `*Transitions.ensureCan…` guard → `@calls` → external port → set status, timestamps, counters → save → emit exactly one event.
- Timestamps use `Instant.now(clock)` with the injected `Clock` (R-11). Business ids use `BusinessIds` (R-08).
- Java paths below are from the repo root. Run Maven in Git Bash with `JAVA_HOME=~/.sdkman/candidates/java/25.0.2-tem` (T-02).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: the user story the task belongs to (US1..US5)

---

## Phase 1: Setup

**Purpose**: Confirm the models and the generated code are the ones planned against, and capture the baseline.

- [ ] T001 Run the model staleness check from `docs/zenwave-sdd-overlay.md` ("Which copy of the models to read"): extract the `service-all-0.0.1` jar into `target/models/` and `diff -q` every `../arcadia-editions-modulith-api/*-domain-model.zdl` and `*-openapi.yml` against it. If any line prints `STALE`, or `docs/zenwave-gap-report.md` no longer says READY, stop and hand over to a human (regeneration is a human action).
- [ ] T002 Run `mvn -DskipTests clean compile 2>&1 | tee ../compile.log` (log outside `target/`, T-03) and record the baseline count of `Unmapped target propert` warnings (gap report: 58) for the X-08 check in T086.
- [ ] T003 [P] [R-12] Add `src/test/java/com/arcadiaeditions/ModularityTest.java` with `ApplicationModules.of(Application.class).verify()` (Spring Modulith). Run it. If it fails on generated structure (package exposure), do not change generated files: mark the test `@Disabled("R-12: <violation>")` and append the violation as a note for the human under R-12 in `specs/001-place-order-flow/research.md`.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: Shared technical seams, repository finders, external ports with their stand-ins, constructor wiring, scenario fixtures and the base error mapping. Every story needs them.

**⚠️ CRITICAL**: no user story work starts until this phase is complete.

### Shared configuration

- [ ] T004 [P] [R-11] Create `src/main/java/com/arcadiaeditions/config/ClockConfiguration.java`: a `@Configuration` that exposes `@Bean Clock clock()` returning `Clock.systemUTC()`.
- [ ] T005 [P] [R-05] Create `src/main/java/com/arcadiaeditions/config/FlowProperties.java`: a `@ConfigurationProperties("arcadia")` record holding `payments.maxAttempts` (int, default `3`) and `payments.retryDelay` (`Duration`, default `200ms`), registered with `@EnableConfigurationProperties(FlowProperties.class)` on the same or a config class in `config/`. Add `arcadia.payments.retry-delay: 0` to `src/test/resources/application-test.yml`.
- [ ] T006 [P] [X-07] Create `src/main/java/com/arcadiaeditions/common/BusinessIds.java` with `static String newId(String prefix)` returning `<PREFIX>-<UUID v4 in upper case>` (R-08). Prefixes used: `ORD` (order), `RES` (reservation), `FUL` (fulfillment), `NOT` (notification). `Payment` has no business id.
- [ ] T007 [P] [X-05] In `src/main/java/com/arcadiaeditions/config/WebExceptionsTranslator.java` add RFC 9457 problem-details handlers (`application/problem+json`, as in [contracts/checkout-http.md](contracts/checkout-http.md) "Other mappings"): `NoSuchElementException` → `404`, title `Not found`; the `InvalidStateTransitionException` nested in each module's `*Transitions` (`OrderTransitions`, `StockReservationTransitions`, `PaymentTransitions`, `FulfillmentTransitions`) → `409`, title `Invalid state transition`. Keep the generated bean-validation (`400`, `fillBindingErrors`) handling as it is.

### Repository finders (developer-owned repositories + their in-memory mirrors)

- [ ] T008 [P] [G-OC-07] Add `Optional<Order> findByOrderId(String orderId)` to `src/main/java/com/arcadiaeditions/orders/checkout/core/outbound/jpa/OrderRepository.java` and implement it in `src/test/java/com/arcadiaeditions/orders/checkout/infrastructure/jpa/inmemory/OrderRepositoryInMemory.java`.
- [ ] T009 [P] [G-PP-07] Add `Optional<Payment> findByOrderId(String orderId)` (`orderId` is unique, one `Payment` per order) to `src/main/java/com/arcadiaeditions/payments/processing/application/port/out/jpa/PaymentRepository.java` and implement it in `src/test/java/com/arcadiaeditions/payments/processing/adapter/out/jpa/inmemory/PaymentRepositoryInMemory.java`.
- [ ] T010 [P] [G-CI-07] [X-04] Add to `src/main/java/com/arcadiaeditions/catalog/inventory/repository/jpa/InventoryPositionRepository.java` a pessimistic-write finder `@Lock(LockModeType.PESSIMISTIC_WRITE) List<InventoryPosition> findAllBySkuInOrderBySku(Collection<String> skus)` (R-04: lock all positions of an order in SKU order to avoid deadlocks). Implement it (sorted by `sku`, no locking) in `src/test/java/com/arcadiaeditions/catalog/inventory/repository/jpa/inmemory/InventoryPositionRepositoryInMemory.java`. Keep `@Version` on `InventoryPosition` untouched.
- [ ] T011 [P] [G-CI-07] Add `List<StockReservation> findByOrderIdAndStatusIn(String orderId, Collection<ReservationStatus> statuses)` to `src/main/java/com/arcadiaeditions/catalog/inventory/repository/jpa/StockReservationRepository.java` and implement it in `src/test/java/com/arcadiaeditions/catalog/inventory/repository/jpa/inmemory/StockReservationRepositoryInMemory.java`.
- [ ] T012 [P] [X-01] Add `Optional<Fulfillment> findByOrderId(String orderId)` (`orderId` unique) to `src/main/java/com/arcadiaeditions/fulfillment/shipping/FulfillmentRepository.java` and implement it in `src/test/java/com/arcadiaeditions/fulfillment/shipping/inmemory/FulfillmentRepositoryInMemory.java`.
- [ ] T013 [P] [X-01] Add `Optional<Notification> findByOrderIdAndType(String orderId, NotificationType type)` (idempotency key `orderId` + `type`) to `src/main/java/com/arcadiaeditions/notifications/consumer/repository/jpa/NotificationRepository.java` and implement it in `src/test/java/com/arcadiaeditions/notifications/consumer/repository/jpa/inmemory/NotificationRepositoryInMemory.java`.

### External ports and stand-ins (R-06, [contracts/stand-in-adapters.md](contracts/stand-in-adapters.md))

- [ ] T014 [P] [X-02] Create the port `src/main/java/com/arcadiaeditions/payments/processing/application/port/out/provider/PaymentProvider.java` with `authorize(orderId, attempt, amount, currency, paymentMethodId)` → `Authorized(providerReference)` | `Declined(reason)` | `Failed(reason)`; `capture(providerReference, amount, currency)` → `Captured` | `CaptureFailed(reason)`; `voidAuthorization(providerReference)` → `Voided` (results as a sealed interface of records per operation). Create the default bean `src/main/java/com/arcadiaeditions/payments/processing/adapter/out/provider/StandInPaymentProvider.java` (`@Component`), deterministic by `paymentMethodId`, exactly as the contract table: `pm_decline` → `Declined("card_declined")`; `pm_fail_once` → attempt 1 `Failed("processor_unavailable")`, attempt 2+ `Authorized`; `pm_fail_always` → every attempt `Failed("processor_unavailable")`; `pm_capture_fail` → authorize `Authorized`, capture `CaptureFailed("capture_rejected")`; anything else → `Authorized("STANDIN-<orderId>")` and capture `Captured`. `orderId` + `attempt` is the idempotency key (same key → same result). For `capture`, the stand-in needs the trigger: keep a map `providerReference → paymentMethodId` filled by `authorize`, or encode it in the reference (e.g. `STANDIN-CAPFAIL-<orderId>`).
- [ ] T015 [P] [Q-08] Create the port `src/main/java/com/arcadiaeditions/fulfillment/shipping/FulfillmentDecision.java` with `decide(ScheduleFulfillmentInput)` → `Scheduled` | `Failed(reason)`, and the default bean `src/main/java/com/arcadiaeditions/fulfillment/shipping/StandInFulfillmentDecision.java` (`@Component`): `shippingAddress.postalCode` = `00000` → `Failed("carrier_unavailable")`, anything else → `Scheduled`.
- [ ] T016 [P] [X-03] Create the port `src/main/java/com/arcadiaeditions/notifications/consumer/service/NotificationSender.java` with `send(Notification)` → `Delivered` | `DeliveryFailed(reason)`, and the default bean `src/main/java/com/arcadiaeditions/notifications/consumer/service/impl/StandInEmailNotificationSender.java` (`@Component`) that logs recipient, subject and body instead of sending e-mail: `recipient` ending with `@fail.invalid` → `DeliveryFailed("mailbox_unavailable")`, anything else → `Delivered`.

### Constructor wiring (service impl + in-memory config in the same change, constitution IV / G-FT-06)

- [ ] T017 [P] [G-CI-01] [G-FT-06] Add `InventoryPositionRepository` and `Clock` as constructor dependencies of `src/main/java/com/arcadiaeditions/catalog/inventory/service/impl/InventoryServiceImpl.java` (follow the existing constructor style), and update `src/test/java/com/arcadiaeditions/catalog/inventory/config/ServicesInMemoryConfig.java` to pass `inventoryPositionRepository()` and a fixed `Clock` (`Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC)`).
- [ ] T018 [P] [G-OC-01] [G-FT-06] Add `InventoryService` (the `catalog.inventory` exposed interface, the only `@calls` target) and `Clock` as constructor dependencies of `src/main/java/com/arcadiaeditions/orders/checkout/core/application/OrdersCheckoutServiceImpl.java`. Update `src/test/java/com/arcadiaeditions/orders/checkout/config/ServicesInMemoryConfig.java`: hold a `com.arcadiaeditions.catalog.inventory.config.ServicesInMemoryConfig inventoryContext` field, pass `inventoryContext.inventoryService()` and the fixed `Clock`, expose `inventoryContext` (getter) and call `inventoryContext.reloadTestData()` from `reloadTestData()`, so checkout tests reserve real in-memory stock.
- [ ] T019 [P] [X-02] [G-FT-06] Add `PaymentProvider`, `FlowProperties` and `Clock` as constructor dependencies of `src/main/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceImpl.java`; update `src/test/java/com/arcadiaeditions/payments/processing/config/ServicesInMemoryConfig.java` to pass `new StandInPaymentProvider()`, `new FlowProperties(...)` with `maxAttempts` 3 and `retryDelay` `Duration.ZERO`, and the fixed `Clock`.
- [ ] T020 [P] [Q-08] [G-FT-06] Add `FulfillmentDecision` and `Clock` as constructor dependencies of `src/main/java/com/arcadiaeditions/fulfillment/shipping/FulfillmentShippingServiceImpl.java`; update `src/test/java/com/arcadiaeditions/fulfillment/shipping/config/ServicesInMemoryConfig.java` to pass `new StandInFulfillmentDecision()` and the fixed `Clock`.
- [ ] T021 [P] [X-03] [G-FT-06] Add `NotificationSender` and `Clock` as constructor dependencies of `src/main/java/com/arcadiaeditions/notifications/consumer/service/impl/NotificationsConsumerServiceImpl.java`; update `src/test/java/com/arcadiaeditions/notifications/consumer/config/ServicesInMemoryConfig.java` to pass `new StandInEmailNotificationSender()` and the fixed `Clock`.
- [ ] T022 Compile (`mvn -DskipTests compile`) and run the existing tests under `src/test/java/com/arcadiaeditions/` (`mvn test`) to check the wiring of T017-T021 compiles and nothing that passed before now fails.

### Scenario fixtures (G-FT-05)

- [ ] T023 [G-FT-05] Regenerate `src/test/resources/data.sql` and `src/test/resources/data/jpa/<Entity>/<n>.json` with the `/zenwave-test-scenarios` skill (not freehand, constitution IV), JSON and `data.sql` agreeing. Required states (data-model.md, "Fixture states needed"): `Order` `CREATED`, `CONFIRMED`, `CANCELLED`; `StockReservation` `RESERVED`, `CONFIRMED`, `RELEASED` (several per order); `InventoryPosition` plenty, exactly-enough, zero available (incl. `SKU-001`); `Payment` `AUTHORIZATION_REQUESTED`, `AUTHORIZED`, `FAILED` below the attempt limit (`attemptCount` 1) and at it (`attemptCount` 3), `CAPTURE_FAILED`, `CAPTURED`, `VOIDED`; `Fulfillment` `SCHEDULED`, `FAILED`; `Notification` one `SENT` per `type` (for redelivery tests). Chains consistent across contexts by `orderId` (e.g. `ORD-001`), business ids in the `ORD-/RES-/FUL-/NOT-` format, `paymentMethodId` values from the stand-in triggers where a chain needs a forced outcome. Keep `Order` id `1` present (used by `getOrder` tests). Ask the skill to write its coverage matrix and assumptions next to the fixtures as it prescribes.

**Checkpoint**: foundation ready. `mvn test` compiles and the previously passing tests still pass; user stories can start.

---

## Phase 3: User Story 1 - Customer buys a limited edition and is charged only when it ships (Priority: P1) 🎯 MVP

**Goal**: The `completed` ZFL outcome: `startOrderCheckout` → `reserveStock` → `OrderCreated` → `authorizePayment` (with transient retry) → `PaymentAuthorized` → `confirmOrder` + `confirmStockReservation` → `OrderConfirmed` → `scheduleFulfillment` → `FulfillmentScheduled` → `capturePayment` → `PaymentCaptured` → `sendOrderConfirmation` → `OrderConfirmationSent`.

**Independent Test**: `POST /api/orders/start-checkout` for available stock with `paymentMethodId` `pm_ok` (and again with `pm_fail_once`): the order ends `CONFIRMED`, reservations `CONFIRMED`, stock moved reserved → sold, payment `CAPTURED` (`attemptCount` 2 with one retry), exactly one `ORDER_CONFIRMATION` notification `SENT`.

### Tests for User Story 1 (write first, see them fail)

- [ ] T024 [P] [US1] [G-FT-02] In `src/test/java/com/arcadiaeditions/catalog/inventory/service/impl/InventoryServiceTest.java` replace the `reserveStock` and `confirmStockReservation` TODO stubs: `reserveStock` with every line available → outcome `RESERVED`, one `StockReservation` per line in `RESERVED` with `reservationId` `RES-…`, `reservedAt` = clock, `expiresAt` = `reservedAt` + 10 minutes, available → reserved on each `InventoryPosition`, no event published; duplicate SKUs in one request summed; `confirmStockReservation({orderId})` → every `RESERVED` reservation of the order `CONFIRMED` + `confirmedAt`, reserved → sold, exactly **one** `StockReservationConfirmed` with all items; second call for the same `orderId` → no event (no `RESERVED` left).
- [ ] T025 [P] [US1] [G-FT-02] In `src/test/java/com/arcadiaeditions/orders/checkout/core/application/OrdersCheckoutServiceTest.java` replace the `startOrderCheckout` and `confirmOrder` TODO stubs: checkout with available stock → order saved `CREATED`, `orderId` `ORD-…`, `totalAmount` = Σ quantity × unitPrice, `createdAt` = clock, stock reserved in `inventoryContext`, exactly one `OrderCreated` with `amount` = `totalAmount`; `confirmOrder(ConfirmOrderInput{orderId})` on a `CREATED` fixture → `CONFIRMED`, `confirmedAt` set, its reservations `CONFIRMED`, one `OrderConfirmed`; redelivery on a `CONFIRMED` order → unchanged, no event; unknown `orderId` → `NoSuchElementException`.
- [ ] T026 [P] [US1] [G-FT-02] In `src/test/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceTest.java` replace the TODO stubs for the happy path: `authorizePayment` for a new `orderId` → `Payment` created with `attemptCount` 1, then `AUTHORIZED` + `providerReference` + `authorizedAt`, one `PaymentAuthorized`; redelivery of the same `OrderCreated` → no second payment, no event; `pm_fail_once`: `authorizePayment` → `FAILED` + `failedAt` + `failureReason`, one `PaymentFailed` (`reason` set) → `retryPayment({orderId})` → `attemptCount` 2, `AUTHORIZATION_REQUESTED`, one `PaymentRetried` (`retriedAt` set) → `authorizePayment` with only `orderId` (payment fields null, Q-02) → `AUTHORIZED`; `retryPayment` on a non-`FAILED` payment → no event; `capturePayment({orderId})` on an `AUTHORIZED` fixture → `CAPTURED` + `capturedAt`, one `PaymentCaptured`; redelivery → no event; `capturePayment` on an `AUTHORIZATION_REQUESTED` payment → `InvalidStateTransitionException`; unknown `orderId` → `NoSuchElementException`.
- [ ] T027 [P] [US1] [G-FT-02] In `src/test/java/com/arcadiaeditions/fulfillment/shipping/FulfillmentShippingServiceTest.java` replace the `scheduleFulfillment` TODO stub: new `orderId` → `Fulfillment` saved `SCHEDULED` with `fulfillmentId` `FUL-…` and `scheduledAt`, one `FulfillmentScheduled`; redelivery for the same `orderId` → no second fulfillment, no event.
- [ ] T028 [P] [US1] [G-FT-02] In `src/test/java/com/arcadiaeditions/notifications/consumer/service/impl/NotificationsConsumerServiceTest.java` replace the `sendOrderConfirmation` TODO stub: → `Notification` saved with `type` `ORDER_CONFIRMATION`, `channel` `EMAIL`, `recipient` = customer e-mail, `notificationId` `NOT-…`, `createdAt`, `SENT` + `sentAt`, one `OrderConfirmationSent`; recipient `x@fail.invalid` → `FAILED` + `failedAt` + `failureReason`, one `NotificationDeliveryFailed` (`reason` set); redelivery when a notification for (`orderId`, `ORDER_CONFIRMATION`) is `SENT` or `FAILED` → no event.
- [ ] T029 [P] [US1] [G-FT-03] [G-FT-04] Fix the happy-path API tests: in `src/test/java/com/arcadiaeditions/orders/checkout/adapters/web/OrdersCheckoutApiIntegrationTest.java` rewrite `testStartOrderCheckout_201` to send a valid body (`SKU-001` with stock from the fixtures, `currency` `EUR`, quantity within available) and assert `201`, `status` `CREATED`, `orderId` starting with `ORD-`, `totalAmount`, and no `confirmedAt`/`cancelledAt`/`cancellationReason`; in `src/test/java/com/arcadiaeditions/orders/checkout/adapters/web/OrdersCheckoutApiControllerTest.java` replace the `null` body posted at `:32` with a valid `StartOrderCheckoutInput`.
- [ ] T030 [US1] [G-FT-01] Create `src/test/java/com/arcadiaeditions/flows/PlaceOrderFlowTest.java` (R-09): `@SpringBootTest` with the `test` profile, `@EnableScenarios` (Spring Modulith `Scenario`), PostgreSQL through `DockerComposeInitializer` as the other integration tests do. Add a request-body builder (customer, e-mail, `paymentMethodId`, `EUR`, items, shipping address, as in quickstart.md) and an end-state assertion helper reading the repositories of every module (order status/reason, reservations, inventory counters, payment status, notifications per type). Scenarios: **completed** (`pm_ok`): wait for `OrderConfirmationSent` for the order's `orderId`, then assert order `CONFIRMED`, reservations `CONFIRMED`, reserved → sold, payment `CAPTURED`, one `ORDER_CONFIRMATION` `SENT`; **completed with one retry** (`pm_fail_once`): same end state and `attemptCount` 2.

### Implementation for User Story 1

- [ ] T031 [US1] [G-CI-01] [X-04] Implement `reserveStock` in `src/main/java/com/arcadiaeditions/catalog/inventory/service/impl/InventoryServiceImpl.java` (data-model.md, catalog-inventory; R-04), all or nothing: sum quantities per `sku`; lock the positions with `findAllBySkuInOrderBySku`; check every line first (unknown SKU = available `0`); if **all** fit, create one `StockReservation` per line (`RESERVED`, `reservationId` = `BusinessIds.newId("RES")`, `orderId`, `reservedAt`, `expiresAt` = `reservedAt` + 10 minutes), move available → reserved, save, return outcome `RESERVED` with `StockReservedResult`; otherwise write nothing and return `UNAVAILABLE` with `StockUnavailableResult` listing **every** short line with its requested and available quantity. Emits no event (the caller does). Runs in the caller's transaction.
- [ ] T032 [US1] [G-CI-01] Add the mappings `reserveStock` needs to `src/main/java/com/arcadiaeditions/catalog/inventory/service/impl/mappers/InventoryServiceMapper.java` (reservation lines → `StockReservedResult`, short lines → `StockUnavailableResult`, outcome into `ReserveStockResult`) so no `ReserveStockResult` field is left unmapped.
- [ ] T033 [US1] [G-CI-02] Implement `confirmStockReservation` in `src/main/java/com/arcadiaeditions/catalog/inventory/service/impl/InventoryServiceImpl.java`: replace the `List.of()` placeholder with `findByOrderIdAndStatusIn(input.orderId, [RESERVED])`; none left → return, emit nothing; for each: guard, `CONFIRMED`, `confirmedAt`, move reserved → sold on its `InventoryPosition`; save; emit **one** `StockReservationConfirmed` for the order (not one per reservation).
- [ ] T034 [US1] [G-CI-06] In `src/main/java/com/arcadiaeditions/catalog/inventory/service/impl/mappers/EventsMapper.java` add the reservations-list → `StockReservationConfirmed` mapping (`orderId` and `items` from the confirmed reservations).
- [ ] T035 [P] [US1] [G-OC-03] In `src/main/java/com/arcadiaeditions/orders/checkout/core/application/mappers/EventsMapper.java` map `OrderCreated.amount` ← `Order.totalAmount`.
- [ ] T036 [US1] [G-OC-01] Implement the `RESERVED` branch of `startOrderCheckout` in `src/main/java/com/arcadiaeditions/orders/checkout/core/application/OrdersCheckoutServiceImpl.java`: map the input to an `Order`; `orderId` = `BusinessIds.newId("ORD")`; `totalAmount` = Σ quantity × unitPrice (prices and currency as sent, Q-06); `createdAt`; call `inventoryService.reserveStock({orderId, items[sku, quantity]})` (`@calls`); on `RESERVED` save as `CREATED` and emit exactly one `OrderCreated`. Remove the generated emission of both events. Leave the `UNAVAILABLE` branch to T053 (until then throw `IllegalStateException("stock unavailable")`).
- [ ] T037 [US1] [G-OC-02] [G-OC-07] Implement `confirmOrder` in `src/main/java/com/arcadiaeditions/orders/checkout/core/application/OrdersCheckoutServiceImpl.java`: load with `orderRepository.findByOrderId(input.getOrderId())` in place of `Optional.empty()` (`TODO CUSTOM_REQUIRED`), `orElseThrow()`; status `CONFIRMED` → return as is, emit nothing; guard; call `inventoryService.confirmStockReservation({orderId})` (`@calls`, before the state change); `CONFIRMED`, `confirmedAt`; save; emit `OrderConfirmed`.
- [ ] T038 [US1] [G-PP-01] [G-PP-07] Implement `authorizePayment` in `src/main/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceImpl.java` as get-or-create by `orderId` (`findByOrderId`): new → create from the input (`AUTHORIZATION_REQUESTED`, `attemptCount` 1, `paymentMethodId`, amount, currency, e-mail) and save; existing and not `AUTHORIZATION_REQUESTED` → return, emit nothing; existing in `AUTHORIZATION_REQUESTED` (retry, Q-02) → ignore the null input payment fields and use the stored ones; never reset `attemptCount`. Call `paymentProvider.authorize(orderId, attemptCount, …)` and set exactly one outcome: `Authorized` → `AUTHORIZED`, `providerReference`, `authorizedAt`, emit `PaymentAuthorized`; `Declined` → `DECLINED`, `declinedAt`, `failureReason`, emit `PaymentDeclined`; `Failed` → `FAILED`, `failedAt`, `failureReason`, emit `PaymentFailed`. Remove the generated emission of all three events. Do not add `@Validated` on `AuthorizePaymentInput` (X-06).
- [ ] T039 [US1] [G-PP-02] [G-PP-07] Implement the under-limit branch of `retryPayment` in `src/main/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceImpl.java`: load with `findByOrderId(input.getOrderId())`; status ≠ `FAILED` → return, emit nothing (R-02); `attemptCount < flowProperties.payments().maxAttempts()` → `attemptCount + 1`, `AUTHORIZATION_REQUESTED`, save, emit only `PaymentRetried`. The at-limit branch is T062 (until then keep it unreachable with a `TODO [G-PP-02]`).
- [ ] T040 [US1] [G-PP-03] [G-PP-07] Implement the success branch of `capturePayment` in `src/main/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceImpl.java`: load with `findByOrderId(input.getOrderId())`; status `CAPTURED` or `CAPTURE_FAILED` → return, emit nothing; guard (from `AUTHORIZED`); `paymentProvider.capture(providerReference, amount, currency)`; `Captured` → `CAPTURED`, `capturedAt`, save, emit only `PaymentCaptured`. The `CaptureFailed` branch is T074.
- [ ] T041 [P] [US1] [G-PP-06] In `src/main/java/com/arcadiaeditions/payments/processing/application/service/mapper/EventsMapper.java` map `PaymentFailed.reason` ← `failureReason` and `PaymentRetried.retriedAt` (from the payment's update time / clock value passed in).
- [ ] T042 [US1] [R-05] In `src/main/java/com/arcadiaeditions/payments/processing/adapter/in/event/processing/PaymentsProcessingServiceEventsListener.java` inject `FlowProperties` and, in the `PaymentRetried` handler, wait `payments.retryDelay` (skip when zero) before calling `authorizePayment`. The listener stays a mapping plus this delay; it does not touch repositories.
- [ ] T043 [US1] [G-FS-01] [X-01] Implement the scheduled branch of `scheduleFulfillment` in `src/main/java/com/arcadiaeditions/fulfillment/shipping/FulfillmentShippingServiceImpl.java`: `findByOrderId` exists → return it, emit nothing; otherwise build the `Fulfillment` from the input with `fulfillmentId` = `BusinessIds.newId("FUL")`; `fulfillmentDecision.decide(input)`; `Scheduled` → `SCHEDULED`, `scheduledAt`, save, emit only `FulfillmentScheduled`. Remove the generated emission of both events; the `Failed` branch is T077.
- [ ] T044 [US1] [G-NC-01] [G-NC-04] [X-03] Implement `sendOrderConfirmation` in `src/main/java/com/arcadiaeditions/notifications/consumer/service/impl/NotificationsConsumerServiceImpl.java` with a private helper reused by the other two commands: `findByOrderIdAndType(orderId, type)` exists in `SENT` or `FAILED` → return, emit nothing; otherwise build the `Notification` (`type` `ORDER_CONFIRMATION`, `channel` `EMAIL`, `recipient` = customer e-mail, `notificationId` = `BusinessIds.newId("NOT")`, `createdAt`), call `notificationSender.send(…)`: `Delivered` → `SENT`, `sentAt`, emit `OrderConfirmationSent`; `DeliveryFailed` → `FAILED`, `failedAt`, `failureReason`, emit `NotificationDeliveryFailed`; save. Map `NotificationDeliveryFailed.reason` ← `failureReason` in `src/main/java/com/arcadiaeditions/notifications/consumer/service/impl/mappers/EventsMapper.java`. Remove the generated emission of both events.
- [ ] T045 [US1] Run (tests under `src/test/java/com/arcadiaeditions/`) `mvn test -Dtest='InventoryServiceTest,OrdersCheckoutServiceTest,PaymentsProcessingServiceTest,FulfillmentShippingServiceTest,NotificationsConsumerServiceTest,OrdersCheckoutApi*Test,PlaceOrderFlowTest'`: the US1 tests of T024-T030 pass; tests for later stories may still fail.

**Checkpoint**: US1 works end to end (MVP). An order with available stock and a good card ends `completed`.

---

## Phase 4: User Story 2 - Customer is told immediately when stock is gone (Priority: P2)

**Goal**: The `stockGone` ZFL outcome: `startOrderCheckout` with `reserveStock` = `UNAVAILABLE` → no order saved, `409` with the unavailable items, `StockUnavailable` → `sendStockUnavailableNotification` → `StockUnavailableNotificationSent`. No oversell under concurrency (SC-002).

**Independent Test**: request more units than available: `409` listing every short item with requested/available quantities, inventory unchanged, no `Order`/`Payment` row, one `STOCK_UNAVAILABLE` notification `SENT`.

### Tests for User Story 2 (write first, see them fail)

- [ ] T046 [P] [US2] [G-FT-02] In `src/test/java/com/arcadiaeditions/catalog/inventory/service/impl/InventoryServiceTest.java` add `reserveStock` unavailable cases: one line over the available quantity in a multi-line request → outcome `UNAVAILABLE`, **no** reservation created and no counter changed (all or nothing), every short line listed with requested and available quantities; unknown SKU → listed with available `0`.
- [ ] T047 [P] [US2] [G-FT-02] In `src/test/java/com/arcadiaeditions/orders/checkout/core/application/OrdersCheckoutServiceTest.java` add the unavailable checkout: `startOrderCheckout` with a short line → throws `StockUnavailableException` carrying a `StockUnavailable` (`orderId` `ORD-…`, `unavailableItems` = the short lines), no `Order` saved, exactly one `StockUnavailable` published, no `OrderCreated`.
- [ ] T048 [P] [US2] [G-FT-02] In `src/test/java/com/arcadiaeditions/notifications/consumer/service/impl/NotificationsConsumerServiceTest.java` replace the `sendStockUnavailableNotification` TODO stub: → `type` `STOCK_UNAVAILABLE`, `skus` = `input.unavailableItems[].sku`, `SENT`, one `StockUnavailableNotificationSent`; redelivery → no event.
- [ ] T049 [P] [US2] [G-FT-03] In `src/test/java/com/arcadiaeditions/orders/checkout/adapters/web/OrdersCheckoutApiIntegrationTest.java` add: `409` stock unavailable (quantity above the fixture's available) with problem details `title` `Stock unavailable`, `orderId` `ORD-…` and `unavailableItems[{sku, requestedQuantity, availableQuantity}]` as in [contracts/checkout-http.md](contracts/checkout-http.md); `400` for `"currency": "EURO"` (longer than 3) and for `"items": []`, inventory unchanged in both.
- [ ] T050 [US2] [G-FT-01] Add the **stockGone** scenario to `src/test/java/com/arcadiaeditions/flows/PlaceOrderFlowTest.java`: post a quantity above available, expect `409`, wait for `StockUnavailableNotificationSent` for the returned `orderId`; assert inventory counters unchanged, no `Order` and no `Payment` for that `orderId`, one `STOCK_UNAVAILABLE` notification `SENT`.

### Implementation for User Story 2

- [ ] T051 [US2] [G-OC-01] Create `src/main/java/com/arcadiaeditions/orders/checkout/core/inbound/StockUnavailableException.java` (R-03): a `RuntimeException` carrying the module's `StockUnavailable` event (getter), message `Some items are not available in the requested quantity`.
- [ ] T052 [US2] [G-OC-04] In `src/main/java/com/arcadiaeditions/orders/checkout/core/application/mappers/EventsMapper.java` add a two-source mapping to `StockUnavailable`: `orderId` (and customer fields) from the `Order` built from the input, `unavailableItems` from `ReserveStockResult.unavailable.unavailableItems` (`sku`, `requestedQuantity`, `availableQuantity`).
- [ ] T053 [US2] [G-OC-01] In `src/main/java/com/arcadiaeditions/orders/checkout/core/application/OrdersCheckoutServiceImpl.java` replace the `UNAVAILABLE` placeholder of T036: do **not** save the order (Q-05), publish `StockUnavailable` (T052), then throw `StockUnavailableException` with it. Annotate `startOrderCheckout` with `@Transactional(noRollbackFor = StockUnavailableException.class)` so the event publication commits (`reserveStock` wrote nothing).
- [ ] T054 [US2] [X-05] In `src/main/java/com/arcadiaeditions/config/WebExceptionsTranslator.java` map `StockUnavailableException` → `409` problem details: `title` `Stock unavailable`, `detail` `Some items are not available in the requested quantity`, `instance` = request path, properties `orderId` and `unavailableItems` (`sku`, `requestedQuantity`, `availableQuantity` for every short line) from the carried event, exactly as [contracts/checkout-http.md](contracts/checkout-http.md).
- [ ] T055 [US2] [G-NC-02] Implement `sendStockUnavailableNotification` in `src/main/java/com/arcadiaeditions/notifications/consumer/service/impl/NotificationsConsumerServiceImpl.java` with the T044 helper: `type` `STOCK_UNAVAILABLE`, `skus` ← `input.unavailableItems[].sku`, emit `StockUnavailableNotificationSent` on delivery; remove the generated emission of both events.
- [ ] T056 [US2] [X-04] Create `src/test/java/com/arcadiaeditions/flows/InventoryConcurrencyTest.java` (SC-002, R-04) against PostgreSQL (in-memory repositories cannot lock): seed an `InventoryPosition` with N available units, start more than N concurrent checkouts of 1 unit each (threads through `OrdersCheckoutService.startOrderCheckout` or the REST endpoint), and assert exactly N succeed, the rest get `StockUnavailableException` / `409`, and `availableQuantity + reservedQuantity + soldQuantity` is unchanged with `reservedQuantity` ≤ N; no deadlock or `ObjectOptimisticLockingFailureException`.
- [ ] T057 [US2] Run (`mvn test`, `src/test/java/com/arcadiaeditions/`) the US2 tests (T046-T050, T056) and the US1 tests again: all pass.

**Checkpoint**: US1 and US2 work independently.

---

## Phase 5: User Story 3 - Order is cancelled and stock returned when payment cannot be authorized (Priority: P3)

**Goal**: The `orderCancelled` outcome via `PaymentDeclined` and via `PaymentRetryExhausted`: `releaseStock` (reasons `PAYMENT_DECLINED` / `PAYMENT_RETRY_EXHAUSTED`) → `StockReleased` → `cancelOrder` → `OrderCancelled` → `sendOrderCancelledNotification` → `OrderCancelledNotificationSent`.

**Independent Test**: checkout with `pm_decline`, then with `pm_fail_always`: stock back in available, order `CANCELLED` with reason `PAYMENT_DECLINED` / `PAYMENT_RETRY_EXHAUSTED`, payment `DECLINED` / `RETRY_EXHAUSTED` (`attemptCount` 3), one `ORDER_CANCELLED` notification with the reason.

Depends on US1 (an order and its payment must exist for these events to fire).

### Tests for User Story 3 (write first, see them fail)

- [ ] T058 [P] [US3] [G-FT-02] In `src/test/java/com/arcadiaeditions/catalog/inventory/service/impl/InventoryServiceTest.java` replace the `releaseStock` TODO stub: an order with `RESERVED` reservations → each `RELEASED` + `releasedAt` + `releaseReason`, reserved → available; with `CONFIRMED` reservations → sold → available (Q-09); exactly **one** `StockReleased` with all items and the reason; second call (nothing left to release) → no event. Add a mapper test for `src/main/java/com/arcadiaeditions/catalog/inventory/events/processing/mappers/PaymentsProcessingEventsListenerMapper.java`: `PaymentDeclined` with any provider text (`"card_declined"`) → `reason` `PAYMENT_DECLINED`, `PaymentRetryExhausted` → `PAYMENT_RETRY_EXHAUSTED`, `PaymentVoided` → `PAYMENT_VOIDED`, never `Enum.valueOf` errors.
- [ ] T059 [P] [US3] [G-FT-02] In `src/test/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceTest.java` add: `pm_decline` → `DECLINED` + `declinedAt` + `failureReason`, one `PaymentDeclined` with `reason`, and no `retryPayment` effect (a `retryPayment` on `DECLINED` emits nothing); `retryPayment` on the `FAILED` fixture at the limit (`attemptCount` 3) → `RETRY_EXHAUSTED`, one `PaymentRetryExhausted` with `reason` and `exhaustedAt`, no `PaymentRetried`.
- [ ] T060 [P] [US3] [G-FT-02] In `src/test/java/com/arcadiaeditions/orders/checkout/core/application/OrdersCheckoutServiceTest.java` replace the `cancelOrder` TODO stub: from `CREATED` and from `CONFIRMED` fixtures → `CANCELLED`, `cancelledAt`, `cancellationReason` = `input.reason`, one `OrderCancelled` with `reason`; redelivery on a `CANCELLED` order → no exception, no event; unknown `orderId` → `NoSuchElementException`.
- [ ] T061 [P] [US3] [G-FT-02] In `src/test/java/com/arcadiaeditions/notifications/consumer/service/impl/NotificationsConsumerServiceTest.java` replace the `sendOrderCancelledNotification` TODO stub: → `type` `ORDER_CANCELLED`, the reason in the body, `SENT`, one `OrderCancelledNotificationSent` with `reason`; redelivery → no event.

### Implementation for User Story 3

- [ ] T062 [US3] [G-PP-02] In `src/main/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceImpl.java` implement the at-limit branch of `retryPayment` (replace the T039 `TODO [G-PP-02]`): `attemptCount >= maxAttempts` → `RETRY_EXHAUSTED`, save, emit only `PaymentRetryExhausted`.
- [ ] T063 [P] [US3] [G-PP-06] In `src/main/java/com/arcadiaeditions/payments/processing/application/service/mapper/EventsMapper.java` map `PaymentDeclined.reason` and `PaymentRetryExhausted.reason` ← `failureReason`, and `PaymentRetryExhausted.exhaustedAt`.
- [ ] T064 [P] [US3] [G-CI-04] In `src/main/java/com/arcadiaeditions/catalog/inventory/events/processing/mappers/PaymentsProcessingEventsListenerMapper.java` map `ReleaseStockInput.reason` to a constant per source event instead of `Enum.valueOf(StockReleaseReason, event.getReason())`: `PaymentDeclined` → `PAYMENT_DECLINED`, `PaymentRetryExhausted` → `PAYMENT_RETRY_EXHAUSTED`, `PaymentVoided` → `PAYMENT_VOIDED` (e.g. `@Mapping(target = "reason", constant = "…")`).
- [ ] T065 [US3] [G-CI-03] Implement `releaseStock` in `src/main/java/com/arcadiaeditions/catalog/inventory/service/impl/InventoryServiceImpl.java`: replace the `List.of()` placeholder with `findByOrderIdAndStatusIn(input.orderId, [RESERVED, CONFIRMED])`; none → return, emit nothing; for each: guard, `RELEASED`, `releasedAt`, `releaseReason` = `input.reason`; `RESERVED`: reserved → available, `CONFIRMED`: sold → available (Q-09) on its `InventoryPosition`; save; emit **one** `StockReleased` for the order.
- [ ] T066 [US3] [G-CI-06] In `src/main/java/com/arcadiaeditions/catalog/inventory/service/impl/mappers/EventsMapper.java` add the reservations-list + input → `StockReleased` mapping (`orderId`, `items`, `reason`). `StockReleased.items` is never initialized on the generated event class, so map the whole list (setter), not `addItems`; do not edit the event class.
- [ ] T067 [US3] [G-OC-05] [G-OC-07] Implement `cancelOrder` in `src/main/java/com/arcadiaeditions/orders/checkout/core/application/OrdersCheckoutServiceImpl.java`: load with `orderRepository.findByOrderId(input.getOrderId())` in place of `Optional.empty()`, `orElseThrow()`; status `CANCELLED` → return, emit nothing (X-01); guard (from `CREATED` or `CONFIRMED`); `CANCELLED`, `cancelledAt`, `cancellationReason` ← `input.reason` (the `StockReleased.reason` enum name, already mapped by the listener); save; emit `OrderCancelled`.
- [ ] T068 [P] [US3] [G-OC-05] Map `CancelOrderInput.reason` → `Order.cancellationReason` in `src/main/java/com/arcadiaeditions/orders/checkout/core/application/mappers/OrdersCheckoutServiceMapper.java` and `OrderCancelled.reason` ← `cancellationReason` in `src/main/java/com/arcadiaeditions/orders/checkout/core/application/mappers/EventsMapper.java`.
- [ ] T069 [US3] [G-NC-03] [G-NC-04] Implement `sendOrderCancelledNotification` in `src/main/java/com/arcadiaeditions/notifications/consumer/service/impl/NotificationsConsumerServiceImpl.java` with the T044 helper: `type` `ORDER_CANCELLED`, the reason in the body, emit `OrderCancelledNotificationSent`; map `OrderCancelledNotificationSent.reason` ← input `reason` in `src/main/java/com/arcadiaeditions/notifications/consumer/service/impl/mappers/EventsMapper.java`. Remove the generated emission of both events.
- [ ] T070 [US3] [G-FT-01] Add to `src/test/java/com/arcadiaeditions/flows/PlaceOrderFlowTest.java` the scenarios **orderCancelled via declined** (`pm_decline`: wait for `OrderCancelledNotificationSent`; order `CANCELLED` reason `PAYMENT_DECLINED`, payment `DECLINED`, reservations `RELEASED`, counters back to the start values, one `ORDER_CANCELLED` notification) and **orderCancelled via retry exhausted** (`pm_fail_always`: reason `PAYMENT_RETRY_EXHAUSTED`, payment `RETRY_EXHAUSTED` with `attemptCount` 3).
- [ ] T071 [US3] Run (`mvn test`, `src/test/java/com/arcadiaeditions/`) the US3 tests (T058-T061, T070) and the US1/US2 tests again: all pass.

**Checkpoint**: US1, US2 and US3 work.

---

## Phase 6: User Story 4 - Authorization is voided, never charged, when fulfillment or capture fails (Priority: P4)

**Goal**: The `orderCancelled` outcome via `FulfillmentFailed` → `voidPayment` and via `PaymentCaptureFailed` → `voidPayment`; `PaymentVoided` → `releaseStock` (`PAYMENT_VOIDED`) → `cancelOrder` → notice.

**Independent Test**: checkout with `postalCode` `00000`, then with `pm_capture_fail`: payment `VOIDED` and never `CAPTURED`, stock returned, order `CANCELLED` with reason `PAYMENT_VOIDED`, one cancellation notice.

Depends on US1 (authorization, confirmation, fulfillment) and US3 (release, cancel and notice chain).

### Tests for User Story 4 (write first, see them fail)

- [ ] T072 [P] [US4] [G-FT-02] In `src/test/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceTest.java` add: `capturePayment` with `pm_capture_fail` → `CAPTURE_FAILED` + `failureReason`, one `PaymentCaptureFailed` with `reason`, never `CAPTURED`; `voidPayment({orderId, reason})` from `AUTHORIZED` and from `CAPTURE_FAILED` → provider `voidAuthorization` called, `VOIDED`, `voidedAt`, `failureReason` = `input.reason`, one `PaymentVoided` with `reason`; from `AUTHORIZATION_REQUESTED` and `FAILED` → `VOIDED` **without** calling the provider (use a recording `PaymentProvider` wrapper in the test); on a `VOIDED` payment → no event; on a `CAPTURED` payment → `InvalidStateTransitionException`.
- [ ] T073 [P] [US4] [G-FT-02] In `src/test/java/com/arcadiaeditions/fulfillment/shipping/FulfillmentShippingServiceTest.java` add: `postalCode` `00000` → `Fulfillment` saved `FAILED` + `failedAt` + `failureReason`, one `FulfillmentFailed` with `reason`, no `FulfillmentScheduled`; redelivery → no event.

### Implementation for User Story 4

- [ ] T074 [US4] [G-PP-03] In `src/main/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceImpl.java` add the `CaptureFailed` branch of `capturePayment` (Q-03): `CAPTURE_FAILED`, `failureReason`, save, emit only `PaymentCaptureFailed`; no money moved.
- [ ] T075 [US4] [G-PP-04] [G-PP-07] Implement `voidPayment` in `src/main/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceImpl.java`: load with `findByOrderId(input.getOrderId())` in place of `Optional.empty()`; status `VOIDED` → return, emit nothing; guard (never voids `CAPTURED`); call `paymentProvider.voidAuthorization(providerReference)` only if the payment had been authorized (`AUTHORIZED`, `CAPTURE_FAILED`), skip it for `AUTHORIZATION_REQUESTED` / `FAILED`; `VOIDED`, `voidedAt`, `failureReason` ← `input.reason`; save; emit `PaymentVoided`.
- [ ] T076 [P] [US4] [G-PP-06] In `src/main/java/com/arcadiaeditions/payments/processing/application/service/mapper/EventsMapper.java` map `PaymentCaptureFailed.reason` and `PaymentVoided.reason` ← `failureReason`.
- [ ] T077 [US4] [G-FS-01] In `src/main/java/com/arcadiaeditions/fulfillment/shipping/FulfillmentShippingServiceImpl.java` add the `Failed` branch of `scheduleFulfillment`: `FAILED`, `failedAt`, `failureReason`, save, emit only `FulfillmentFailed`; map `FulfillmentFailed.reason` ← `failureReason` in `src/main/java/com/arcadiaeditions/fulfillment/shipping/mappers/EventsMapper.java`.
- [ ] T078 [US4] [G-FT-01] Add to `src/test/java/com/arcadiaeditions/flows/PlaceOrderFlowTest.java` the scenarios **orderCancelled via fulfillment failed → void** (`postalCode` `00000`) and **orderCancelled via capture failed → void** (`pm_capture_fail`): wait for `OrderCancelledNotificationSent`; assert order `CANCELLED` reason `PAYMENT_VOIDED`, payment `VOIDED` (and never `CAPTURED`, SC-003), reservations `RELEASED`, counters back to the start values (SC-006), one `ORDER_CANCELLED` notification.
- [ ] T079 [US4] Run (`mvn test`, `src/test/java/com/arcadiaeditions/`) the US4 tests (T072, T073, T078) and all earlier story tests: all pass. `PlaceOrderFlowTest` now has its 6 ZFL-outcome scenarios (plus the retry variant).

**Checkpoint**: every ZFL `end` outcome in scope is implemented and covered.

---

## Phase 7: User Story 5 - Customer checks the order (Priority: P5)

**Goal**: `GET /api/orders/{id}` (`getOrder`, already generated) returns the current status and cancellation reason, and `404` for an unknown id.

**Independent Test**: read fixture order id `1` and a cancelled fixture order; read an unknown id → `404`.

- [ ] T080 [P] [US5] [G-FT-03] In `src/test/java/com/arcadiaeditions/orders/checkout/adapters/web/OrdersCheckoutApiIntegrationTest.java` rewrite `testGetOrder_200` (`:91`, currently `id = ""`) to read fixture id `1` and assert its `status` as in the T023 fixtures (no `cancelledAt`/`cancellationReason` unless that fixture is `CANCELLED`); add a read of a `CANCELLED` fixture asserting `status` `CANCELLED` and `cancellationReason`; add `testGetOrder_404` for `GET /api/orders/999999`.
- [ ] T081 [P] [US5] Add a `getOrder` assertion to `src/test/java/com/arcadiaeditions/flows/PlaceOrderFlowTest.java` (SC-007): in the **completed** scenario `GET /api/orders/{id}` right after the `201` returns `CREATED` (or later), and after `OrderConfirmationSent` returns `CONFIRMED`; in one cancelled scenario it returns `CANCELLED` with the reason.
- [ ] T082 [US5] Run T080-T081: pass. If `GET` of an unknown id is not `404` with the generated controller plus T007, fix it in `src/main/java/com/arcadiaeditions/config/WebExceptionsTranslator.java` only (the controller body stays generated).

**Checkpoint**: all user stories are independently functional.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T083 [X-01] Review idempotency across the 15 flow commands against data-model.md "No-op when": every event-driven command returns without emitting when redelivered, and none throws on "already done". Add a missing redelivery test to the matching `*ServiceTest` for any command not yet covered.
- [ ] T084 [X-06] Verify no `@Validated` was added to `AuthorizePaymentInput` usage in `src/main/java/com/arcadiaeditions/payments/processing/adapter/in/event/processing/PaymentsProcessingServiceEventsListener.java` (partial by design on retry, Q-02); leave the other event-driven inputs unvalidated, as the gap report records.
- [ ] T085 [G-TST-01] Run `mvn test`; if the generated `*RepositoryIntegrationTest`s under `src/test/java/com/arcadiaeditions/**/` fail against the regenerated fixtures (they assert `Instant.now()` and empty strings), rewrite their assertions against the values loaded from `src/test/resources/data.sql`.
- [ ] T086 [X-08] Run `mvn -DskipTests clean compile 2>&1 | tee ../compile.log` and `grep "Unmapped target propert" ../compile.log`: only the expected warnings listed in gap report X-08 remain (the 4 on `PaymentsProcessingServiceEventsListenerMapper` for `PaymentRetried`, the `id`/`version`/`status` ones on entity create mappers, the generated `asOrder(<Input>)` / `update(Order, <Input>)` for `ConfirmOrderInput` / `CancelOrderInput`). Fix any other one in the mapper it points to; do not silence warnings.
- [ ] T087 Apply formatting with Spotless (Palantir Java Format): `mvn spotless:apply`, then confirm `git diff --stat` touches no regenerated file and no `*.zfl` / `*.zdl` (constitution III).
- [ ] T088 Run the full `mvn test`: green, including `src/test/java/com/arcadiaeditions/orders/checkout/ArchitectureTest.java`, `src/test/java/com/arcadiaeditions/JMoleculesArchitectureTest.java` and `src/test/java/com/arcadiaeditions/ModularityTest.java` (unless disabled under R-12 by T003).
- [ ] T089 Run the manual validation in [quickstart.md](quickstart.md) §2 (every row of its scenario table plus the SQL checks after the cancelled scenarios; 0 incomplete rows in `event_publication` once settled).
- [ ] T090 Re-run `/zenwave-gap-report` to refresh `docs/zenwave-gap-report.md`: no blocker or major finding left for `PlaceOrderFlow` (constitution, "Done"). Report R-07 (400/404/409 not declared in `orders-checkout-openapi.yml`) to the human as a generation change; agents don't make it.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none. T001 must pass before anything else.
- **Foundational (Phase 2)**: after Setup; blocks every story. T022 after T017-T021. T023 (fixtures) after T008-T013, so fixture loading matches the finders.
- **US1 (Phase 3)**: after Foundational. The MVP.
- **US2 (Phase 4)**: after Foundational; needs `reserveStock` (T031-T032) and the `RESERVED` checkout branch (T036) from US1, because it replaces the `UNAVAILABLE` placeholder in the same method.
- **US3 (Phase 5)**: after US1 (needs created orders and authorization outcomes from T036-T038).
- **US4 (Phase 6)**: after US1 and US3 (`PaymentVoided` → `releaseStock` → `cancelOrder` → notice is the US3 chain).
- **US5 (Phase 7)**: after Foundational for T080; T081 after the US1 and US3 flow scenarios.
- **Polish (Phase 8)**: after all stories.

### Story completion order

```text
Setup → Foundational → US1 (MVP) ─┬─→ US2
                                  ├─→ US3 ─→ US4
                                  └─→ US5
```

### Same-file sequences (not parallel)

- `InventoryServiceImpl.java`: T017 → T031 → T033 → T065
- `OrdersCheckoutServiceImpl.java`: T018 → T036 → T037 → T053 → T067
- `PaymentsProcessingServiceImpl.java`: T019 → T038 → T039 → T040 → T062 → T074 → T075
- `FulfillmentShippingServiceImpl.java`: T020 → T043 → T077
- `NotificationsConsumerServiceImpl.java`: T021 → T044 → T055 → T069
- `orders/checkout/.../mappers/EventsMapper.java`: T035 → T052 → T068; `payments/.../mapper/EventsMapper.java`: T041 → T063 → T076
- `WebExceptionsTranslator.java`: T007 → T054 → T082
- `PlaceOrderFlowTest.java`: T030 → T050 → T070 → T078 → T081
- Each `*ServiceTest.java` and `OrdersCheckoutApiIntegrationTest.java`: in story order.

### Within each story

- Tests first (they fail), then mappers and repository use, then service bodies, then the flow scenario, then the story's test run.

## Parallel Opportunities

- Phase 2: T004-T016 are all different files ([P]); T017-T021 are one module each ([P]).
- Each story's test tasks marked [P] touch different test files.
- Mapper tasks marked [P] (T035, T041, T063, T064, T068, T076) touch files no other open task in the story touches.
- After US1, US2 and US5 can proceed in parallel with US3; US4 waits for US3.

## Parallel Example: User Story 1

```bash
# Tests, one per module (different files):
Task: "T024 InventoryServiceTest reserveStock / confirmStockReservation"
Task: "T025 OrdersCheckoutServiceTest startOrderCheckout / confirmOrder"
Task: "T026 PaymentsProcessingServiceTest happy path + one retry"
Task: "T027 FulfillmentShippingServiceTest scheduleFulfillment"
Task: "T028 NotificationsConsumerServiceTest sendOrderConfirmation"
Task: "T029 OrdersCheckoutApi*Test happy path"

# Then, per module in parallel (each chain is one module):
Task: "T031 → T032 → T033 → T034 catalog-inventory"
Task: "T035, T036 → T037 orders-checkout (T036 needs T031 for a real reserve in tests)"
Task: "T038 → T039 → T040, T041, T042 payments-processing"
Task: "T043 fulfillment-shipping"
Task: "T044 notifications-consumer"
```

## Implementation Strategy

### MVP first (User Story 1)

1. Phase 1 (T001-T003), then Phase 2 (T004-T023).
2. Phase 3 (T024-T045).
3. **Stop and validate**: `PlaceOrderFlowTest` completed scenarios green; quickstart row "US1 completed".

### Incremental delivery

1. US1 → the happy path works end to end.
2. US2 → stock-gone answer and no oversell under contention.
3. US3 → payment failures cancel and return stock.
4. US4 → fulfillment/capture failures void instead of charging.
5. US5 → read-back verified.
6. Polish → `mvn test` green, fresh gap report with no blocker/major for the flow.

## Notes

- Out of scope: reservation expiry (M-01, Q-04, G-CI-05), `markReadyToShip` (G-FS-10), baselines G-CI-10..12 and G-CP-*; fixtures stand in for G-CI-10.
- Commit per task or logical group with conventional prefixes (`feat:`, `fix:`, `test:`, `chore:`) and the finding id in the message.
- If a task turns out to need a field, input, event, command, state or transition the ZDL lacks, it is a model gap: stop and hand it over (constitution).
