You are the implementer of task T-42 of the board .zenwave/work/integration/docs/tasks.md (same content as docs/tasks.md in your clone).
Work only in .zenwave/work/T-42, on the branch task/T-42. Do not commit or push. Do not touch anything outside the clone.
Follow the skill zenwave-implement-task: <clone>/.claude/skills/zenwave-implement-task/SKILL.md. If the approved models cannot express the task, follow zenwave-model-gap: <clone>/.claude/skills/zenwave-model-gap/SKILL.md (write the proposal in <clone>/docs/proposals/).

The task:
- [/] T-42 bounded AFK capturePayment redelivery on a payment already CAPTURE_FAILED or VOIDED is a no-op (return it, no provider call, no event)
      model: place-order-flow.zfl:172-182, payments-processing-domain-model.zdl:93-95
      test data: data.sql ORD-006, ORD-012
      tests: PaymentsProcessingServiceTest
      fixes: review-S1-2 MODEL NOTE for T-33 (PaymentsProcessingServiceImpl.java:156)


Models (the relative `../` paths of the overlay do not resolve from the clone; use these absolute ones):
- ZFL: ~/workspace/arcadia-editions/arcadia-editions-architecture/business-flows/place-order-flow.zfl
- ZDL and OpenAPI: ~/workspace/arcadia-editions/arcadia-editions-modulith-api/<context>-domain-model.zdl, orders-checkout-openapi.yml
Already in place from T-03: Clock bean (config/ClockConfiguration, Clock.systemUTC), FlowProperties (arcadia.payments.max-attempts/retry-delay), common/BusinessIds.newId(prefix) -> PREFIX-UUID; each test ServicesInMemoryConfig has a public field `clock` = config/TestClock.FIXED (2026-10-14T10:00:00Z). When your service needs time, inject Clock in the impl constructor and pass `clock` from the in-memory config. Use BusinessIds for new business ids.
Other tasks run in parallel in other bounded contexts: stay inside the context of your task and the shared code it needs; do not refactor shared code.
Fix from the S1 review round 2: .zenwave/handoff/place-order-flow/reports/review-S1-2.md (MODEL NOTE for T-33). Merged: T-41 made capturePayment on CAPTURED a no-op (early return before ensureCanCapturePayment); T-33 added the CAPTURE_FAILED branch. Extend the same early return to CAPTURE_FAILED and VOIDED (a redelivered FulfillmentScheduled after the capture failed or the payment was voided): return unchanged, no provider call, no event. Other states (AUTHORIZATION_REQUESTED, FAILED, DECLINED, RETRY_EXHAUSTED) still throw.
Test first, for real: write the test, run it, and record in RED the failing output before you change any main code. The orchestrator re-runs your test against the old code.
Focused tests: mvn -q test -Dtest='PaymentsProcessingServiceTest'. Run only these and the compile (mvn -q -DskipTests clean compile 2>&1 | tee target/compile.log), never the whole suite.
Never change: *.zfl *.zdl *openapi*.yml *asyncapi*.yml src/test/resources/data.sql src/test/resources/data/*
Environment: Windows, Git Bash; JAVA_HOME=~/.sdkman/candidates/java/current (Java 25), mvn on PATH, Docker running (Testcontainers PostgreSQL via docker-compose for *IntegrationTest).
Write your final report, in the shape of the skill, to .zenwave/handoff/place-order-flow/reports/T-42.md, and end your final message with it.

Handoff from the orchestrator: a previous worker (claude-sonnet) ran out of quota in the middle of this task. Do not redo what is done; build on the changes in the tree. State of the clone at handoff:
 M src/main/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceImpl.java
 M src/test/java/com/arcadiaeditions/payments/processing/application/service/PaymentsProcessingServiceTest.java
 2 files changed, 45 insertions(+), 2 deletions(-)
Its last message: "RED confirmed. Now the code." Check what of the code is already there (git diff), finish it, run the focused tests, write the report.
