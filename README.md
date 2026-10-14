# Arcadia Editions Modulith


A Java + Spring Boot service created from the ZenWave archetype. Its structure is generated from a ZenWave domain model (ZDL), and the behavior the generator leaves out is completed with the zenwave-sdd agent skills and GitHub Spec Kit.

1. [Getting Started](#getting-started)
2. [Prerequisites](#prerequisites)
3. [Technologies](#technologies)
4. [Install the ZenWave tooling](#install-the-zenwave-tooling)
5. [Code Generation](#code-generation)
   1. [From the domain model: zenwave-scripts.zw](#from-the-domain-model-zenwave-scriptszw)
   2. [At build time: OpenAPI and AsyncAPI (Maven)](#at-build-time-openapi-and-asyncapi-maven)
6. [Spec-Driven Development with zenwave-sdd](#spec-driven-development-with-zenwave-sdd)
   1. [How this project was made](#how-this-project-was-made)
   2. [What's next](#whats-next)

## Getting Started

```bash
docker-compose up -d
mvn spring-boot:run
```

SwaggerUI: http://localhost:8080/swagger-ui/index.html

## Prerequisites

* Java 25+
* Maven 3.6+
* Docker / Docker Compose (local infrastructure and integration tests)
* Git and Git Bash (all commands below are Git Bash)
* JBang and the ZenWave CLI (`jbang zw`), see [Install the ZenWave tooling](#install-the-zenwave-tooling)
* For spec-driven development: `uv` (to install Spec Kit) and an AI coding agent (Claude Code, Codex, Grok…)
* SDKMAN! (optional, recommended for Java and Maven)

## Technologies

* Java and Spring Boot 4.0.x
* Spring Data JPA or MongoDB
* Spring Cloud Stream for Kafka (or other brokers), Spring Modulith (optional)
* Spring Security
* ZenWave SDK for domain modeling and code generation

## Install the ZenWave tooling

Follow **[ZenWave Getting Started](https://www.zenwave360.io/docs/getting-started/)**. It covers installing the ZenWave CLI with JBang and the IDE plugins for editing `.zdl` and `.zw` files and running the generators. Quick reference for the CLI:

```bash
jbang alias add --fresh --force --name=zw release@zenwave360/zenwave-sdk
jbang zw --help list
```

## Code Generation

Code comes from two places: the ZenWave SDK, run from `zenwave-scripts.zw` when the domain model changes, and Maven plugins that run on every build for the API contracts.

### From the domain model: zenwave-scripts.zw

The project ships with two files:

* `zenwave-model.zdl`: the domain model. It's a sample: replace it with your own, or import a remote, governed version of the model.
* `zenwave-scripts.zw`: the ZenWave plugins to run, with their configuration.

The plugins in `zenwave-scripts.zw`, in the order to run them:

| Plugin | Generates |
|---|---|
| `ZDLToOpenAPIPlugin` | `src/main/resources/public/apis/openapi.yml` from the ZDL services |
| `ZDLToAsyncAPIPlugin` | `src/main/resources/public/apis/asyncapi.yml` from the ZDL events |
| `BackendApplicationDefaultPlugin` | domain entities, repositories, services and their implementations, DTOs, mappers, events, listeners, in-memory test configs and tests |
| `OpenAPIControllersPlugin` | Spring MVC controllers implementing the OpenAPI interfaces |
| `SpringWebTestClientPlugin` | API tests (per operation, and per business flow) |

**How to run them:** open `zenwave-scripts.zw` in the IDE with the ZenWave plugin and use the play button next to each plugin. Hovering the button shows the equivalent command line (`jbang zw -p <Plugin> --deps <...> key=value ...`), which you can copy and run from a terminal. `jbang zw` doesn't take a `.zw` file directly: each plugin block is one command.

After generating:

```bash
mvn -DskipTests clean test-compile    # also runs the Maven generators below
git add -A && git commit -m "feat: generate code from zenwave-model.zdl"
```

**Regenerating safely.** Some files are *generated once* and then belong to you: service implementations, event listeners and their MapStruct mappers, tests, `src/test/resources/data*` and `DockerComposeInitializer`. A normal run doesn't touch them. A run with `--force` / `-f` overwrites them, so check the command the IDE generates, and run `git diff` after every regeneration.

### At build time: OpenAPI and AsyncAPI (Maven)

These run in `generate-sources` on every `mvn compile`, from the API files the plugins above wrote. Their output goes to `target/` and is never edited by hand.

**OpenAPI** (`openapi-generator-maven-plugin`, generator `spring`):
* reads `src/main/resources/public/apis/openapi.yml`;
* generates the Spring MVC API interfaces and the `*DTO` models into `target/generated-sources/openapi` (`interfaceOnly`: the controllers from `OpenAPIControllersPlugin` implement them);
* packages are set by the `openApiApiPackage` / `openApiModelPackage` properties in `pom.xml`.

**AsyncAPI** (`zenwave-sdk-maven-plugin`, generator `AsyncAPIGenerator`, profile `asyncapi-code-generation`):
* the profile **activates only when `src/main/resources/public/apis/asyncapi.yml` exists**. Without the file, nothing is generated;
* generates, as the API `provider`, the message DTOs and the Spring Cloud Stream producers and consumer interfaces into `target/generated-sources/zenwave`;
* with Spring Modulith, it also wires the transactional outbox (`transactionalOutbox=modulith`) and the application-event listener;
* packages are set by the `asyncApiModelPackage`, `asyncApiProducerApiPackage` and `asyncApiConsumerApiPackage` properties. See the [AsyncAPI generator docs](https://www.zenwave360.io/zenwave-sdk/plugins/asyncapi-generator/).

To serve more API files in SwaggerUI, add them to `application.yml` (`springdoc.swagger-ui.urls`, URLs relative to `src/main/resources/public`).

## Spec-Driven Development with zenwave-sdd

The generator creates the structure: entities, services, events, mappers and test skeletons. It leaves the behavior as `TODO`s. The [zenwave-sdd](https://github.com/ZenWave360/zenwave-sdd) skills find and fill those gaps, and [GitHub Spec Kit](https://github.com/github/spec-kit) gives the work a standard shape (`spec.md` → `plan.md` → `tasks.md`).

One rule above all: **models own structure.** If a behavior needs a field, event, command or state that the ZDL doesn't have, fix the ZDL and regenerate. Never hand-add it to code.

The generic, step-by-step guide is [zenwave-sdd GETTING-STARTED](https://github.com/ZenWave360/zenwave-sdd/blob/main/GETTING-STARTED.md) (the same guide is in the README of every project created from the ZenWave archetypes). This section is how it was applied to this project: what was done, with the commits, and what's next, with the exact commands for this repo.

**How to run a skill.** Open your AI agent in the root of this repo. In Claude Code and Grok, type `/skill-name`; in Codex, `$skill-name`. Lines starting with `/` are typed into the agent; `bash` blocks run in Git Bash, from the root of this repo.

### How this project was made

#### 1. The models ✅

Nothing in this repo is modeled here. The models live in two sibling repos, which must be cloned next to this one:

```bash
cd ..
git clone https://github.com/arcadia-editions/arcadia-editions-architecture.git   # ZFL: business-flows/place-order-flow.zfl
git clone https://github.com/arcadia-editions/arcadia-editions-modulith-api.git   # ZDL: one *-domain-model.zdl per module, plus orders-checkout-openapi.yml
cd -
```

The ZFL flow is written for the microservices layout (`@zdl("orders-checkout-api/domain-model.zdl")`). In this modulith, each of those maps to `../arcadia-editions-modulith-api/orders-checkout-domain-model.zdl`, and so on: the mapping is in the overlay.

The generator doesn't read the ZDLs from that repo directly, but from a jar that packages them, `com.arcadiaeditions.modulith.application.arcadia-editions-modulith:service-all:0.0.1`. Build and install it into `~/.m2`:

```bash
bash ../arcadia-editions-modulith-api/package-local-service-all.sh
```

#### 2. Created from the ZenWave Java archetype ✅ (`a348d77`)

With JPA and Spring Modulith, base package `com.arcadiaeditions`. See [the archetype command](https://github.com/ZenWave360/zenwave-sdd/blob/main/MANUAL.md#41-create-the-project-with-the-zenwave-archetype-).

#### 3. Pointed the generation at the models ✅ (`560ed22`)

- Deleted the sample `zenwave-model.zdl`.
- `zenwave-scripts.zw`: `@import("com.arcadiaeditions.modulith.application.arcadia-editions-modulith:service-all:0.0.1")`, and one `BackendApplicationDefaultPlugin` block per module, each with `zdlFile "classpath:/com/arcadiaeditions/modulith/application/arcadia-editions-modulith/<module>-domain-model.zdl"`. The OpenAPI plugins read `orders-checkout-openapi.yml` from the same jar.
- `pom.xml`: `service-all` as a dependency, also of `openapi-generator-maven-plugin`, which now reads its `inputSpec` from the jar. Removed the AsyncAPI properties: events are in-process, there's no AsyncAPI.

#### 4. Generated the code ✅ (`4e2dbcb`)

Ran every plugin in `zenwave-scripts.zw` ([Code Generation](#code-generation)).

#### 5. Installed zenwave-sdd and Spec Kit ✅ (`1bed3a5`)

zenwave-sdd `v0.1.0-rc1` for Claude Code, Codex and Grok (`.claude/skills/`, `.agents/skills/`, `.grok/skills/`), Spec Kit (`.specify/`), `AGENTS.md` and `CLAUDE.md`, with [the install commands of the guide](https://github.com/ZenWave360/zenwave-sdd/blob/main/GETTING-STARTED.md#01-install-the-skills-and-spec-kit). To upgrade, run them again with a new tag and review the `git diff`.

#### 6. Filled in the overlay ✅

[docs/zenwave-sdd-overlay.md](docs/zenwave-sdd-overlay.md) tells every skill where the models of step 1 are, how the ZFL `@zdl(...)` paths map to the ZDL files, how to extract the ZDLs from the jar and check them against the working copy, the shape (modulith, `@listener`, JPA), the module packages, and the build, regenerate and lint commands. Update it if a repo moves, or a flow or module is added.

Checks (each must list files, and the last one must print nothing):

```bash
ls ../arcadia-editions-architecture/business-flows/*.zfl
ls ../arcadia-editions-modulith-api/*-domain-model.zdl
ls ~/.m2/repository/com/arcadiaeditions/modulith/application/arcadia-editions-modulith/service-all/0.0.1/*.jar
grep -nE '<(path|org|commands|jdk|Project|link|list|\.\.\.)>' docs/zenwave-sdd-overlay.md
```

#### 7. Generated basic test data ✅ (`791fbb0`)

```text
/zenwave-test-data
```

One consistent example per entity in `src/test/resources/data.sql` and `src/test/resources/data/jpa/<Entity>/1.json`.

### What's next

#### 8. Create the Spec Kit constitution

`.specify/memory/constitution.md` is still the empty Spec Kit template.

```text
/speckit-constitution Capture this project's existing practices (don't invent standards).
Include the article in docs/constitution-zenwave.md verbatim.
```

Read it and commit it.

#### 9. Run the gap report

First check that the jar matches the ZDLs in `arcadia-editions-modulith-api` (the code was generated from the jar). This must print nothing; if it prints a file, do step 10 items 3–5 first:

```bash
P=com/arcadiaeditions/modulith/application/arcadia-editions-modulith
mkdir -p target/models && unzip -o -q -j ~/.m2/repository/$P/service-all/0.0.1/service-all-0.0.1.jar "$P/*-domain-model.zdl" -d target/models
for f in ../arcadia-editions-modulith-api/*-domain-model.zdl; do diff -q "$f" "target/models/$(basename "$f")"; done
```

Then:

```text
/zenwave-gap-report
```

You get `docs/zenwave-gap-report.md`: model gaps (M-\*), open questions (Q-\*), code gaps per module (G-\*) and cross-cutting ones (X-\*). Commit it.

#### 10. Fix the model gaps (repeat until none are left)

For each M-\* and Q-\*, decide the answer, then:

1. Edit the ZDL in `../arcadia-editions-modulith-api/` (or the ZFL in `../arcadia-editions-architecture/business-flows/`).
2. Lint, and fix every error:

   ```bash
   bash .claude/skills/zfl-to-zdl/scripts/lint-zdl.sh --strict ../arcadia-editions-modulith-api
   ```

3. Rebuild the jar. It packages git-tracked files, so add new ones first:

   ```bash
   (cd ../arcadia-editions-modulith-api && git add -A)
   bash ../arcadia-editions-modulith-api/package-local-service-all.sh
   ```

4. Regenerate with `zenwave-scripts.zw` ([Code Generation](#code-generation)), without `--force`.
5. `git diff --stat`: every module whose ZDL changed must have changes.
6. Check the baseline, refresh the test data and re-run the report:

   ```bash
   mvn -DskipTests clean compile 2>&1 | tee target/compile.log
   grep -i "unmapped target" target/compile.log
   grep -rn "TODO CUSTOM_REQUIRED\|TODO: implement" src/main
   ```

   ```text
   /zenwave-test-data
   /zenwave-gap-report
   ```

#### 11. Generate scenario test data

When the report has no model gaps left, before writing service or flow tests:

```text
/zenwave-test-scenarios
```

From then on, refresh the data with `/zenwave-test-scenarios`, never `/zenwave-test-data`.

#### 12. Spec and implement the place-order flow

Feature `001-place-order-flow` (and `000-<module>-baseline` for gaps outside the flow). In order, reading each result:

```text
/speckit-specify PlaceOrderFlow from ../arcadia-editions-architecture/business-flows/place-order-flow.zfl.
Behavior only: reference ZFL steps and ZDL elements by their exact names.
The findings for this flow are in docs/zenwave-gap-report.md.
```

```text
/speckit-clarify
```

```text
/speckit-plan Structure is fixed by the ZDL and the generated code (constitution).
Do NOT write data-model.md or contracts/: add a "ZDL cross-reference" table
(element | file:line | used by) instead. Copy this flow's slice of
docs/zenwave-gap-report.md into research.md.
```

```text
/speckit-tasks Seed tasks from the gap-report findings for this flow, keeping their ids
(e.g. "T012 [G-OC-03] confirmOrder: load by orderId, guard, CONFIRMED, save, emit").
One phase per bounded context in flow order, then flow-level tests.
```

```text
/speckit-analyze
/speckit-implement Use the zenwave-fill-gaps skill for each task.
/speckit-converge
```

If `/speckit-clarify` needs a new field, event or command, go back to step 10.

#### 13. Verify and close

```bash
mvn test
```

Then `/zenwave-gap-report` again: done when tests are green and there are no blockers or majors left for the flow.

If something goes wrong, see [If something goes wrong](https://github.com/ZenWave360/zenwave-sdd/blob/main/GETTING-STARTED.md#if-something-goes-wrong) in the guide.

Happy Coding!! 🚀🚀🚀

