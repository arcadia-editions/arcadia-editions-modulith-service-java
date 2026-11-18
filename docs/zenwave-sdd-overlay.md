# Arcadia Editions Modulith (Java): ZenWave SDD overlay

What every zenwave-sdd skill and agent needs to know about this project: where the models are, its shape, its
commands, and where its test data is. `AGENTS.md` points here.

The placeholders marked `<after generation: ...>` and `<after test data: ...>` are filled in by the skill that
produces the fact (`zenwave-test-data` / `zenwave-test-scenarios` the test data row; `zenwave-gate-check` suggests the
full-suite duration).

## Where things are

Paths are relative to this repository's root. The models live in sibling repositories that must be cloned next to
this one: `../arcadia-editions-architecture` (flows) and `../arcadia-editions-modulith-api` (ZDL, OpenAPI).

| What | Path |
|---|---|
| Business flows (ZFL) | `../arcadia-editions-architecture/business-flows/*.zfl` (today: `place-order-flow.zfl`) |
| Domain models (ZDL), one per bounded context | `../arcadia-editions-modulith-api/*-domain-model.zdl` |
| API contracts | `../arcadia-editions-modulith-api/orders-checkout-openapi.yml` (no AsyncAPI: events are in-process) |
| Glossary and ADRs | `GLOSSARY.md`, `docs/adr/` |
| Domain summary | `docs/domain.md` |
| Board of the flow in progress | `docs/tasks.md` (archived boards in `docs/tasks/`) |
| Model gap proposals | `docs/proposals/` |
| Test data and scenario matrix | `src/test/resources/data.sql` (header: scenario and coverage matrices, chains `ORD-001`..`ORD-028` with stock `SKU-001`..`SKU-005`, assumptions incl. the stand-in trigger values `pm_*` / postal `00000` / `@fail.invalid`, the fixed test "now" `2026-10-14T10:00:00Z` read by tests through a fixed `Clock`) and `src/test/resources/data/jpa/<Entity>/<n>.json` |
| Generation script | `zenwave-scripts.zw` |
| Protected files | root `package-info.java` (regenerates with the wrong displayName); every `*Repository.java` and its in-memory twin (custom finders); `OrdersCheckoutApiController` (409 handler, P-02); `JMoleculesArchitectureTest`, `ModularityTest`, `DockerComposeInitializer`, `BaseWebTestClientTest`, `data.sql` and fixtures (hand-written) |

**`zenwave-scripts.zw` reads the models from a packaged artifact** (`@import(...)` and `classpath:/...`):

- artifact and version: the `@import(...)` line of `zenwave-scripts.zw` (source of truth; `pom.xml`
  `arcadia-models.version` must match), from GitHub Packages (
  `https://maven.pkg.github.com/arcadia-editions/arcadia-editions-architecture`),
  folder `com/arcadiaeditions/modulith/application/arcadia-editions-modulith/`;
- extract the models when their repository is not cloned:

  ```bash
  P=com/arcadiaeditions/modulith/application/arcadia-editions-modulith
  V=$(grep -o 'service-all:[^")]*' zenwave-scripts.zw | head -1 | cut -d: -f2)
  JAR=~/.m2/repository/$P/service-all/$V/service-all-$V.jar
  mkdir -p target/models && unzip -o -q -j "$JAR" "$P/*.zdl" "$P/*.yml" -d target/models
  ```

- staleness check (prints nothing when the packaged copy matches the working copy):
  `for f in ../arcadia-editions-modulith-api/*.zdl ../arcadia-editions-modulith-api/*-openapi.yml; do diff -q --strip-trailing-cr "$f" "target/models/$(basename "$f")"; done`
- repackage: release a new `service-all` version with the `artifact-release.yml` workflow of
  `arcadia-editions-modulith-api` (workflow_dispatch, `artifact: service-all`), then bump `arcadia-models.version` in
  `pom.xml` and the `@import` of `zenwave-scripts.zw`. For local experiments only,
  `bash ../arcadia-editions-modulith-api/package-local-service-all.sh` installs `0.0.1` into `~/.m2` (packages
  git-tracked files only).

**The ZFL's `@zdl(...)` paths are written for the microservices layout.** In this modulith:

| ZFL `@zdl(...)` | ZDL file |
|---|---|
| `orders-checkout-api/domain-model.zdl` | `../arcadia-editions-modulith-api/orders-checkout-domain-model.zdl` |
| `catalog-inventory-api/domain-model.zdl` | `../arcadia-editions-modulith-api/catalog-inventory-domain-model.zdl` |
| `payments-processing-api/domain-model.zdl` | `../arcadia-editions-modulith-api/payments-processing-domain-model.zdl` |
| `fulfillment-shipping-api/domain-model.zdl` | `../arcadia-editions-modulith-api/fulfillment-shipping-domain-model.zdl` |
| `notifications-consumer-api/domain-model.zdl` | `../arcadia-editions-modulith-api/notifications-consumer-domain-model.zdl` |

`catalog-products-domain-model.zdl` is not in the flow; it is still a module of this service.

## Shape

- Deployment: **modulith** (bounded contexts are Spring Modulith modules; in-process events; ZDL consumers use
  `@listener`).
- Code already in place: yes (`zenwave-tasks-from-code-and-models`). Generated with ZenWave SDK: yes
  (2.9.0-SNAPSHOT, from archetype `zenwave-java-archetype` 2026.11.11.1434).
- Stack: Java 25, Spring Boot 4.1.2, Spring Modulith 2.1.2, jMolecules, MapStruct, Maven (no wrapper: `mvn`).
- Persistence: `jpa` (PostgreSQL; Testcontainers via docker-compose in tests).
- Organization prefix and packages: `com.arcadiaeditions` (ids `urn:com.arcadiaeditions:...`), modules
  `catalog.inventory`, `catalog.products`, `fulfillment.shipping`, `notifications.consumer`, `orders.checkout`,
  `payments.processing`; shared code in `com.arcadiaeditions.common`, config in `com.arcadiaeditions.config`.

## Commands

```bash
# JDK and tools: Java 25 and Maven on PATH (SDKMAN), JBang with the `zw` alias, Docker
export JAVA_HOME=~/.sdkman/candidates/java/current
# regenerate after a model change: a HITL step, by the person or assisted by zenwave-regenerate (scratch worktree,
# --force there, merge only what the change adds; never the Protected files). Never the orchestrator or its workers.
# The 8 plugin blocks of zenwave-scripts.zw (`jbang zw -f -p <Plugin> ...`, the IDE play button shows each command)
# compile, and collect MapStruct warnings
mkdir -p target && mvn -q -DskipTests clean compile 2>&1 | tee target/compile.log
# focused tests of a task (the implementer runs only these)
mvn -q test -Dtest='<TestClass>'
# full suite with integration tests and containers (once per slice review)
# about 2.5 to 5.5 minutes (136 s 2026-11-12, 316 s 2026-11-17); needs Docker (Testcontainers starts the docker-compose PostgreSQL).
# Surefire failures stop the build before the integration tests: add -Dmaven.test.failure.ignore=true to see both.
mvn -q verify
# known failing at the baseline (2026-11-17, models 0.10.1; unchanged with 0.10.3 2026-11-18), each fixed by a board task:
#   PaymentsProcessingServiceTest.voidPaymentFromFailedTest, .voidPaymentFromAuthorizationRequestedTest: assert the old
#   void from FAILED / AUTHORIZATION_REQUESTED; 0.10.x narrows voidPayment to [AUTHORIZED, CAPTURE_FAILED], anything
#   else a no-op -> the voidPayment task of the board
# lint the models
bash .zenwave/bin/lint-zdl.sh --strict ../arcadia-editions-modulith-api
# export the Hibernate DDL (JPA)
bash .claude/skills/zenwave-test-data/scripts/export-ddl.sh
```

The orchestrator gives the workers the focused command (with the task's `tests:`) and the reviewer the full one.

## Project rules

- Naming conventions (ids, channels, topics): `.claude/skills/zenwave-grill-models/references/zenwave-languages/asyncapi-naming-conventions.md`
- ZDL/ZFL comments were LLM-generated and are not authoritative: cite structure (fields, inputs, annotations, ZFL
  edges) as evidence, never a comment alone.
- Decisions already taken that the models don't show (2026-10-14):
  - **Correlation by `orderId`.** `confirmOrder`, `cancelOrder`, `retryPayment`, `capturePayment` and `voidPayment`
    take an input carrying `orderId`; the service loads the aggregate with `findByOrderId(input.orderId)`.
  - **Payment retry data.** On `PaymentRetried`, `authorizePayment`'s input carries no payment data (the
    `zdl.data-lineage` lint warnings are accepted): the service loads the stored `Payment` by `orderId`
    (get-or-create). One `Payment` per order.
  - **Branch outcomes.** `@transition(to:)` names the happy-path target; alternative outcomes set their lifecycle
    state in code (e.g. capture failure → `CAPTURE_FAILED`).
  - **Reservation expiry.** Out of scope for this iteration; no scheduler.
  - **Stock unavailable.** The order is not persisted; the request is answered 409 with the unavailable items.
    `StockUnavailable` still carries a generated `orderId` as correlation id.
  - **Prices.** The client's `items[].unitPrice` and `currency` are accepted as sent; `totalAmount` =
    Σ quantity × unitPrice.
