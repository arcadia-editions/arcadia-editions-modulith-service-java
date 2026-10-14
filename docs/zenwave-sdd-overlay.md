# Arcadia Editions Modulith (Java) — ZenWave SDD Overlay

Project-specific inputs for the zenwave-sdd skills (`zenwave-gap-report`,
`zenwave-fill-gaps`, `zenwave-test-data`, `zenwave-test-scenarios`). Agents read
this file first, then the skill. Referenced from `AGENTS.md` / `CLAUDE.md`.

All paths are relative to this repo's root (`arcadia-editions-modulith-service-java/`).
The models live in **sibling repos**, not in this one.

## Where things are

| What | Path |
|---|---|
| Business flows (ZFL) | `../arcadia-editions-architecture/business-flows/*.zfl` (today: `place-order-flow.zfl`) |
| Domain models (ZDL) | `../arcadia-editions-modulith-api/*-domain-model.zdl` (one per bounded context) |
| Generated API contracts | `../arcadia-editions-modulith-api/orders-checkout-openapi.yml` (no AsyncAPI: events are in-process) |
| Models as the generator sees them | jar `com.arcadiaeditions.modulith.application.arcadia-editions-modulith:service-all:0.0.1`, in `~/.m2/repository/com/arcadiaeditions/modulith/application/arcadia-editions-modulith/service-all/0.0.1/service-all-0.0.1.jar`, under `com/arcadiaeditions/modulith/application/arcadia-editions-modulith/` |
| Implementation repo | `.` (this repo) |
| Generation script | `zenwave-scripts.zw` (reads every ZDL/OpenAPI from the jar via `classpath:/…`) |
| Gap report | `docs/zenwave-gap-report.md` |

### Which copy of the models to read

1. **`../arcadia-editions-modulith-api/` (preferred).** The editable source. Read and
   edit ZDLs here.
2. **The `service-all` jar (fallback, and the source of truth for generation).**
   `zenwave-scripts.zw` imports the jar, so the generated code reflects the jar,
   not the working copy. If the sibling repo isn't checked out, extract the models:

   ```bash
   P=com/arcadiaeditions/modulith/application/arcadia-editions-modulith
   JAR=~/.m2/repository/$P/service-all/0.0.1/service-all-0.0.1.jar
   mkdir -p target/models && unzip -o -j "$JAR" "$P/*-domain-model.zdl" "$P/*-openapi.yml" -d target/models
   ```

   and read the ZDLs from `target/models/`.

If both exist, extract the jar as above and check they agree before auditing (a
difference means the code was generated from older models; repackage and
regenerate first, see below):

```bash
for f in ../arcadia-editions-modulith-api/*-domain-model.zdl ../arcadia-editions-modulith-api/*-openapi.yml; do
  diff -q "$f" "target/models/$(basename "$f")" || echo "STALE: $(basename "$f")"
done
```

### Resolving ZFL `@zdl(...)` paths

The ZFL is written for the microservices layout (`@zdl("<bc>-api/domain-model.zdl")`).
In this modulith every bounded context is one file in `arcadia-editions-modulith-api`:

| ZFL `@zdl(...)` | ZDL file in this deployment |
|---|---|
| `orders-checkout-api/domain-model.zdl` | `orders-checkout-domain-model.zdl` |
| `catalog-inventory-api/domain-model.zdl` | `catalog-inventory-domain-model.zdl` |
| `payments-processing-api/domain-model.zdl` | `payments-processing-domain-model.zdl` |
| `fulfillment-shipping-api/domain-model.zdl` | `fulfillment-shipping-domain-model.zdl` |
| `notifications-consumer-api/domain-model.zdl` | `notifications-consumer-domain-model.zdl` |

Rule: `<bc>-api/domain-model.zdl` → `../arcadia-editions-modulith-api/<bc>-domain-model.zdl`.
`catalog-products-domain-model.zdl` isn't in the flow; it is still a module of this service.

## Shape

- Deployment shape: **modulith** (bounded contexts = Spring Modulith modules;
  in-process events; ZDL consumers use `@listener`). Audit whole flows.
- Organization prefix (ids `urn:com.arcadiaeditions:...`, packages): `com.arcadiaeditions`
- Base package: `com.arcadiaeditions`. Modules: `catalog.inventory`, `catalog.products`,
  `fulfillment.shipping`, `notifications.consumer`, `orders.checkout`,
  `payments.processing`; shared code in `com.arcadiaeditions.common`, config in
  `com.arcadiaeditions.config`.
- Persistence: `jpa`
- Spec Kit feature rule: one feature per ZFL flow (`NNN-<flow>`, e.g.
  `001-place-order-flow`), plus `000-<bc>-baseline` for work outside any flow.

## Build, generate, test

```bash
# JDK / tools: Java 25 and Maven on PATH (SDKMAN recommended), JBang + `jbang zw`

# 1. after editing a ZDL in ../arcadia-editions-modulith-api: repackage the models
#    into the local service-all jar (it packages git-tracked files: `git add` new ones)
bash ../arcadia-editions-modulith-api/package-local-service-all.sh

# 2. regenerate: run each plugin block of zenwave-scripts.zw from the IDE play button
#    (hover it for the equivalent `jbang zw -p <Plugin> ...` command). Never use --force
#    unless you mean to overwrite generated-once files; check `git diff` afterwards.

# compile and collect MapStruct warnings
mvn -DskipTests clean compile 2>&1 | tee target/compile.log

# lint the models (gap-report step 1)
bash .claude/skills/zfl-to-zdl/scripts/lint-zdl.sh --strict ../arcadia-editions-modulith-api

# export the Hibernate DDL (JPA only; no project config needed)
bash .claude/skills/zenwave-test-data/scripts/export-ddl.sh    # -> target/ddl/schema.sql
```

## Project rules

- Naming conventions (ids, channels, topics): `.claude/skills/zfl-to-zdl/references/zdl-asyncapi-naming-conventions.md`
- ZDL/ZFL comments were LLM-generated and are not authoritative. Cite structure
  (fields, inputs, annotations, ZFL edges) as evidence, never a comment alone.
- Decisions already taken (2026-10-14, open questions of the gap report):
  - **Correlation by `orderId` (Q-01).** Event-driven commands that take an `(id)` parameter
    (`confirmOrder`, `cancelOrder`, `retryPayment`, `capturePayment`, `voidPayment`)
    resolve the aggregate in code with a `findByOrderId(event.orderId)` lookup.
  - **Payment retry data (Q-02).** On `PaymentRetried`, `authorizePayment`'s input carries no
    payment data (the 4 `zdl.data-lineage` lint warnings are accepted): the service loads the
    stored `Payment` by `orderId` (get-or-create). One `Payment` per order (`orderId unique`).
  - **Branch outcomes (Q-03).** `@transition(to:)` holds one state (the generator renders a
    single enum constant), so it names the happy-path target. Alternative outcomes set their
    lifecycle state in code (e.g. capture failure → `CAPTURE_FAILED`).
  - **Reservation expiry (Q-04, M-01, G-CI-05).** Out of scope for this iteration; no scheduler.
  - **Stock unavailable (Q-05).** The order is not persisted (as the ZFL says); the request is
    answered 409 with the unavailable items. `StockUnavailable` still carries a generated
    `orderId` as correlation id.
  - **Prices (Q-06).** For now the client's `items[].unitPrice` and `currency` are accepted
    as sent; `totalAmount` = Σ quantity × unitPrice. A later iteration may fetch prices from
    `catalog.products` (needs a products query plus `@calls`).
