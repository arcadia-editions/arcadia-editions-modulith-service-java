# Arcadia Editions Modulith Constitution

## Core Principles

### ZenWave: Models Own Structure

The ZenWave models are the single source of truth for structure. ZFL flows own
*when and why*, and ZDL models own *what*: aggregates, entities, lifecycle states and
transitions, commands, inputs and outputs, events, listeners, and calls between
contexts. Code generated from them by the ZenWave SDK (domain classes, repositories,
DTOs, mappers, controllers, event publishers and listeners) is a projection of the
models, not a design to revisit.

- Specs, plans and tasks MUST reference ZFL/ZDL elements by their exact names, and
  MUST NOT redefine, restate or re-derive them. `/speckit-plan` MUST NOT write a
  `data-model.md` or `contracts/` for structure that the ZDL or the generated
  OpenAPI/AsyncAPI already declare. It cross-references them instead.
- The ZFL and ZDL files are human-owned. Agents MUST NOT create, edit or
  "fix" them, not even to close a model gap, not even when the change looks
  obvious. Only a human changes the models, outside the agentic implementation.
- When a feature needs structure that the models lack (a field, input, event,
  command, state or transition), that is a **model gap**. The agent MUST stop
  work on that feature and hand it to a human with enough context to decide: the
  finding id, the ZFL/ZDL lines involved, what is missing and why the current
  models and generated code can't express it, and the options with their
  consequences. It is never closed by hand-writing the structure in code, and
  never by the agent editing the models.
- Implementation (`/speckit-plan` onward) MUST NOT start while the gap report's
  model readiness verdict is **NOT READY**.
- Behavior the models can't express (service method bodies, branching on
  outcomes, calls to other contexts, adapters to external systems, mapper
  configuration, tests and test data) is implemented in the generated-once,
  developer-owned files, and tracked as tasks.
- Regeneration is a deliberate human action. Agents may regenerate into a
  scratch folder to compare, but never overwrite the working tree on their own.

### II. Modules Are Bounded Contexts

This service is a Spring Modulith modulith. Each bounded context in the ZDL is one module
under `com.arcadiaeditions`: `catalog.inventory`, `catalog.products`, `fulfillment.shipping`,
`notifications.consumer`, `orders.checkout` and `payments.processing`. Shared code lives in
`com.arcadiaeditions.common` and configuration in `com.arcadiaeditions.config`.

- Modules communicate through in-process domain events (`@ApplicationModuleListener`, backed
  by the JDBC event publication registry), as declared by ZDL `@listener`. Synchronous calls
  between modules go only through what the ZDL declares as `@calls`, using the target
  module's exposed service interface.
- Each module keeps the package layout it was generated with. The generated
  `ArchitectureTest` (layering) and `JMoleculesArchitectureTest` (DDD stereotypes) MUST pass.
- There is no AsyncAPI and no broker: events stay in-process.

Rationale: module boundaries come from the ZDL bounded contexts. Keeping the generated layout
and architecture tests intact means regeneration and the module boundaries never fight
each other.

### III. Generated vs. Developer-Owned Code

Generated code falls into two groups, and they are treated differently:

- **Regenerated structure.** Domain classes (entities, enums, domain events), input/output
  DTOs, inbound service interfaces, event publishers, and the OpenAPI interfaces and models
  under `target/` MUST NOT be hand-edited. Change the model and regenerate.
- **Generated once, then developer-owned.** Service implementations, Spring Data
  repositories, MapStruct mappers (service, events and event-listener mappers), event
  listeners, in-memory test repositories and configs, tests, `src/test/resources/data*` and
  `DockerComposeInitializer`. Behavior is written here: repositories gain query methods
  (e.g. natural-key finders), mappers gain mapping configuration. Neither may add structure
  the model owns (fields, inputs, events, commands, states).
- The generator's overwrite setting decides which group a file belongs to: a file the
  generator writes with skip-overwrite is developer-owned, any other generated file is
  regenerated structure.
- Regeneration runs without `--force` unless the intent is to overwrite developer-owned
  files, and every regeneration is followed by a `git diff` review.

Rationale: this split is how the ZenWave generator works. Overwriting developer-owned files
loses behavior, and editing regenerated files loses changes at the next run.

### IV. Tests and Test Data

The generated test structure is the testing standard of this project:

- Service tests run in memory (`ServicesInMemoryConfig`, `InMemoryJpaRepository`,
  `InMemoryEventPublisher`) and load fixtures from `src/test/resources/data/jpa/<Entity>/*.json`.
- Repository integration tests run against the database seeded by `src/test/resources/data.sql`.
- API tests exercise the generated REST endpoints.
- Test data is generated with the zenwave-sdd skills, not written freehand:
  `/zenwave-test-data` until the gap report has no model gaps left, then
  `/zenwave-test-scenarios` from then on. The JSON fixtures and `data.sql` MUST agree.
- When a service gains a dependency, its in-memory config is updated in the same change.

Rationale: these are the test layers the archetype generates, and the skills keep fixtures
consistent with the DDL Hibernate actually runs.

### V. Gap-Report Traceability

`docs/zenwave-gap-report.md`, produced by `/zenwave-gap-report`, is the inventory of what is
missing. Its finding ids (M-\*, G-\*, X-\*, Q-\*) are the shared vocabulary of the work.

- The report opens with a model readiness verdict. While it is NOT READY for a flow, no
  agent plans or implements that flow.
- Model gaps (M-\*) and open questions (Q-\*) go through `/speckit-clarify` to a human, who
  applies the model-changing answers to the ZFL/ZDL. Agents never edit the models, and gaps
  are not resolved in specs or code.
- Tasks keep the finding ids in their titles (e.g. `T012 [G-OC-03] …`).
- The report is re-run after every regeneration, and ids are kept stable.

Rationale: one id per gap links the report, the spec, the tasks and the commits.

## Technology and Repository Constraints

- Java 25, Spring Boot 4.0.x, Spring Modulith 2.0, Spring Data JPA, MapStruct, ZenWave SDK.
  Formatting uses Spotless with Palantir Java Format.
- The models live in sibling repositories, not here:
  `../arcadia-editions-architecture/business-flows/*.zfl` (flows) and
  `../arcadia-editions-modulith-api/*-domain-model.zdl` (one ZDL per module).
- Code is generated from the `service-all` jar that packages those models, through
  `zenwave-scripts.zw`. The jar MUST match the ZDL working copy before auditing or
  implementing. If they differ, repackage and regenerate first.
- ZFL `@zdl("<bc>-api/domain-model.zdl")` paths resolve to
  `../arcadia-editions-modulith-api/<bc>-domain-model.zdl`.
- `docs/zenwave-sdd-overlay.md` is the authoritative source for paths, commands and the
  project shape. Every zenwave-* skill reads it first.

## Development Workflow

1. **Model loop (human-only)**, repeated until the gap report's verdict is READY. Agents
   never run it: they stop and hand over with the finding, the model lines and the options.
   A human edits ZFL/ZDL → `lint-zdl.sh --strict` → `package-local-service-all.sh` → regenerate with
   `zenwave-scripts.zw` (no `--force`) → `git diff --stat` → compile and collect MapStruct
   warnings → refresh test data → `/zenwave-gap-report`.
2. **Spec Kit features**: one feature per ZFL flow (`specs/NNN-<flow>/`, e.g.
   `001-place-order-flow`), plus `000-<bc>-baseline` for work outside any flow. Phases follow
   the bounded contexts in flow order, then flow-level tests.
3. **Implementation** of each task uses the `zenwave-fill-gaps` skill.
4. **Done** for a flow means `mvn test` is green and a fresh gap report has no blocker or
   major findings left for that flow.
5. Commits use conventional prefixes (`feat:`, `fix:`, `test:`, `chore:`).

## Governance

This constitution governs every spec, plan, task and implementation in this repository.
Where it conflicts with other guidance, it wins. `AGENTS.md` / `CLAUDE.md` and
`docs/zenwave-sdd-overlay.md` hold the operational detail.

- **Amendments** are made through `/speckit-constitution`, reviewed as a diff and committed.
  The ZenWave article is maintained upstream in `docs/constitution-zenwave.md` and is copied
  here verbatim. It changes only when that source changes.
- **Versioning** follows semantic versioning. MAJOR: a principle is removed or redefined.
  MINOR: a principle or section is added, or guidance is materially expanded. PATCH:
  clarifications and wording.
- **Compliance**: `/speckit-plan` checks the plan against these principles (Constitution
  Check), `/speckit-analyze` reports violations, and reviews check that no structure was
  hand-written outside the models.

**Version**: 1.0.1 | **Ratified**: 2026-10-14 | **Last Amended**: 2026-10-14
