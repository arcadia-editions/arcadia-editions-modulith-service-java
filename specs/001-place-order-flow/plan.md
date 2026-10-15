# Implementation Plan: Place Order Flow

**Branch**: `001-place-order-flow` (spec directory; work happens on `main`) | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-place-order-flow/spec.md`

## Summary

Implement the behavior of ZFL `PlaceOrderFlow` (`../arcadia-editions-architecture/business-flows/place-order-flow.zfl`)
across the five bounded contexts it touches. Structure is already generated from the ZDL models; this
plan only fills developer-owned code: service method bodies (including the `findByOrderId` loads of
the five `orderId`-keyed commands), the two `@calls`, MapStruct mappings, three stand-in external ports (payment provider,
fulfillment decision, notification sender), HTTP error mapping, test fixtures and tests.

The work inventory is `docs/zenwave-gap-report.md` (verdict **READY**; 38 G-, 8 X- findings). Every
task keeps its finding id. Design decisions not settled by the overlay are in [research.md](research.md);
the behavioral rules per aggregate (correlation keys, outcome → state, idempotency) are in
[data-model.md](data-model.md); the only interface surface not declared by the models is in
[contracts/](contracts/).

## Technical Context

**Language/Version**: Java 25 (run Maven with `JAVA_HOME=~/.sdkman/candidates/java/25.0.2-tem` in Git Bash, gap report T-02)

**Primary Dependencies**: Spring Boot 4.0.0, Spring Modulith 2.0.0 (JDBC event publication registry, `@ApplicationModuleListener`), Spring Data JPA / Hibernate, MapStruct, Lombok, jMolecules, ZenWave SDK (generator; jar pinned 2.6.0, `jbang zw` runs 2.8.0-SNAPSHOT, T-04)

**Storage**: PostgreSQL 16 (docker-compose / Testcontainers); in-memory repositories for service tests

**Testing**: JUnit 5; in-memory service tests (`ServicesInMemoryConfig`, `InMemoryJpaRepository`, `InMemoryEventPublisher`); repository integration tests on `data.sql`; web tests (`BaseWebTestClientTest`); flow tests with Spring Modulith `Scenario` (`spring-modulith-test` already on the test classpath); ArchUnit (`ArchitectureTest`, `JMoleculesArchitectureTest`)

**Target Platform**: Linux/JVM server (Spring Boot), one deployable modulith

**Project Type**: web service (Spring Modulith, one module per bounded context)

**Performance Goals**: no throughput target in the spec. Correctness under contention is the goal: no oversell when many customers order the last units concurrently (SC-002)

**Constraints**: no oversell (SC-002); no capture on a cancelled order (SC-003); exactly one notice per outcome under redelivery (SC-005; incomplete publications are resubmitted every 30 s, `config/ExternalizationConfiguration.java:22-23`); stock-unavailable answered in the same request (SC-004); models and regenerated files are not edited

**Scale/Scope**: 1 flow, 17 ZFL steps (step 15 deferred), 5 modules, 15 service commands + `getOrder`

No NEEDS CLARIFICATION remains: Q-01..Q-06 are decided in `docs/zenwave-sdd-overlay.md`, Q-07..Q-09, Q-11
and Q-12 are spec assumptions, Q-10 is baseline work. The remaining design choices are resolved in
[research.md](research.md).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle / rule | Check | Status |
|---|---|---|
| ZenWave: gap report verdict | `docs/zenwave-gap-report.md` says ✅ READY. Re-verified today: the ZDLs and OpenAPI in `../arcadia-editions-modulith-api` are at `166be74` (commands keyed by `orderId`, human model change) and identical to the `service-all-0.0.1` jar (staleness check passed); the code was regenerated from them (`2d5a0a3`) and the gap report re-run (READY); the ZFL is at `915d073`. | PASS |
| ZenWave: no model edits | The plan edits no `*.zfl` / `*.zdl` and does not regenerate. | PASS |
| ZenWave: no restated structure | `data-model.md` is a cross-reference index plus behavioral rules (correlation, outcome → state, idempotency); it defines no field, input, event or state. `contracts/` covers only what neither the ZDL nor the generated OpenAPI declare (error responses, stand-in adapter controls) and points to the OpenAPI for the rest. | PASS |
| ZenWave: model gaps stop work | No open M- gap for this flow (M-01 deferred by decision). Q-01 is settled in the models (R-02). One item for human review, not blocking: R-07 (409/400 responses undocumented in the generated OpenAPI). | PASS (1 item for human review) |
| II. Modules = bounded contexts | Cross-module sync calls only through the ZDL `@calls` (`reserveStock`, `confirmStockReservation`) via `InventoryService` (`@NamedInterface`). Everything else via existing `@ApplicationModuleListener`s. No new listener for any ZFL edge (all exist, gap report §2). `ArchitectureTest` and `JMoleculesArchitectureTest` stay green (listeners only map events to inputs; loads happen in the services). | PASS |
| III. Generated vs developer-owned | Edits go only to files the generator writes with skip-overwrite: `*ServiceImpl`, `*ServiceMapper`, `EventsMapper`, `*EventsListener` + mapper, `*Repository` (finders), in-memory repositories/configs, tests, `data*`. Verified in `backend-application-default-2.8.0-SNAPSHOT` (`BackendApplicationProjectTemplates`). Not touched: entities, DTOs, domain events, inbound service interfaces, `EventPublisher`/`DefaultEventPublisher`. New files are developer-owned additions (ports, stubs, exception, config properties, tests). Repositories gain finders and mappers gain mapping configuration, as constitution v1.0.1 allows (research R-10). | PASS |
| IV. Tests and test data | Fixtures come from `/zenwave-test-scenarios` (G-FT-05), not freehand; service tests stay in memory; `ServicesInMemoryConfig` updated with every new dependency (G-FT-06). | PASS |
| V. Traceability | Tasks will carry G-/X- ids; phases follow gap report §6. | PASS |

**Post-design re-check (after Phase 1)**: unchanged, PASS. The design adds no structure the ZDL owns;
the new types (external ports and stubs, `StockUnavailableException`, retry properties)
are technical seams for behavior the models delegate to code.

## Project Structure

### Documentation (this feature)

```text
specs/001-place-order-flow/
├── spec.md
├── plan.md              # this file
├── research.md          # Phase 0: decisions R-01..R-12
├── data-model.md        # Phase 1: model cross-reference + behavioral rules
├── quickstart.md        # Phase 1: validation guide
├── contracts/
│   ├── checkout-http.md         # HTTP behavior beyond the generated OpenAPI (201/400/404/409)
│   └── stand-in-adapters.md     # external ports and how tests force outcomes
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

Existing generated layout, kept as is (constitution II). Only the paths this feature touches or adds
are listed; `(new)` marks developer-owned additions. Paths below `src/main/java/com/arcadiaeditions/`.

```text
config/
├── WebExceptionsTranslator.java                    # X-05: 404 / 409 mapping
├── ClockConfiguration.java                         # (new) Clock bean (R-11)
└── FlowProperties.java                             # (new) arcadia.payments.* / stand-in settings (R-05)

catalog/inventory/                                   # layered layout
├── repository/jpa/InventoryPositionRepository.java # G-CI-07: locking findBySku… (R-04)
├── repository/jpa/StockReservationRepository.java  # G-CI-07: findByOrderIdAndStatusIn
├── service/impl/InventoryServiceImpl.java          # G-CI-01..03
├── service/impl/mappers/{InventoryServiceMapper,EventsMapper}.java      # G-CI-01, G-CI-06
└── events/processing/mappers/PaymentsProcessingEventsListenerMapper.java # G-CI-04

orders/checkout/                                     # clean hexagonal layout
├── core/inbound/StockUnavailableException.java     # (new) R-03
├── core/application/OrdersCheckoutServiceImpl.java # G-OC-01, -02, -05, -07 (findByOrderId loads)
├── core/application/mappers/{EventsMapper,OrdersCheckoutServiceMapper}.java # G-OC-03..05
└── core/outbound/jpa/OrderRepository.java          # G-OC-07

payments/processing/                                 # hexagonal ports/adapters layout
├── application/port/out/provider/PaymentProvider.java          # (new) X-02
├── adapter/out/provider/StandInPaymentProvider.java            # (new) X-02
├── application/port/out/jpa/PaymentRepository.java # G-PP-07
├── application/service/PaymentsProcessingServiceImpl.java      # G-PP-01..04, -07 (findByOrderId loads)
├── application/service/mapper/EventsMapper.java    # G-PP-06
└── adapter/in/event/processing/PaymentsProcessingServiceEventsListener.java # R-05 retry delay

fulfillment/shipping/                                # flat layout
├── FulfillmentDecision.java + StandInFulfillmentDecision.java  # (new) R-06
├── FulfillmentRepository.java                      # findByOrderId (X-01)
├── FulfillmentShippingServiceImpl.java             # G-FS-01
└── mappers/EventsMapper.java

notifications/consumer/                              # layered layout
├── service/NotificationSender.java                 # (new) X-03
├── service/impl/StandInEmailNotificationSender.java # (new) X-03 (layer per R-06)
├── repository/jpa/NotificationRepository.java      # findByOrderIdAndType (X-01)
├── service/impl/NotificationsConsumerServiceImpl.java # G-NC-01..03
└── service/impl/mappers/EventsMapper.java          # G-NC-04

src/test/java/com/arcadiaeditions/
├── */config/ServicesInMemoryConfig.java            # G-FT-06: new deps + stubs
├── */repository/**/inmemory/*RepositoryInMemory.java # finders mirrored
├── */*ServiceTest.java                             # G-FT-02
├── orders/checkout/adapters/web/*Test.java         # G-FT-03, G-FT-04
├── flows/PlaceOrderFlowTest.java                   # (new) G-FT-01, Scenario per ZFL end outcome
└── flows/InventoryConcurrencyTest.java             # (new) SC-002 / X-04
src/test/resources/data.sql, data/jpa/**            # G-FT-05 via /zenwave-test-scenarios
```

**Structure Decision**: single Spring Modulith service; each module keeps the package layout it was
generated with (layered, clean hexagonal, ports/adapters, flat). New types go into the layer that the
module's existing architecture rules allow (see research R-06). Flow-level tests live in a
`flows` test package because they span all modules.

### Implementation order (from gap report §6)

1. **Foundations**: R-11 `Clock`, R-05 properties, X-07 id generation, X-05 error mapping; fixtures with
   `/zenwave-test-scenarios` (G-FT-05) so every later test has its precondition states.
2. **catalog-inventory**: G-CI-07, G-CI-01 (first: checkout calls it), X-04, G-CI-02, G-CI-03, G-CI-04, G-CI-06.
3. **orders-checkout**: G-OC-07 (finder + loads), G-OC-03, G-OC-04, G-OC-01 (+R-03), G-OC-02, G-OC-05.
4. **payments-processing**: G-PP-07 (finder + loads), X-02, G-PP-01, G-PP-02, G-PP-03, G-PP-04, G-PP-06.
5. **fulfillment-shipping**: G-FS-01 (+R-06 stand-in, X-01).
6. **notifications-consumer**: X-03, G-NC-01..04.
7. **Flow and cross-cutting**: X-01 review, X-06, X-08, G-FT-01 (one Scenario per ZFL end outcome), G-FT-03/04, SC-002 concurrency test; re-run `/zenwave-gap-report`.

Each module phase finishes with its in-memory service tests (G-FT-02) green before the next one.

## Complexity Tracking

No constitution violation to justify. One design item is recorded for human review rather than as a
violation: R-07 (the generated OpenAPI declares only 201/200). The earlier R-02 item (lookup ports) was
removed when the human keyed the five event-driven commands by `orderId` in the models.
