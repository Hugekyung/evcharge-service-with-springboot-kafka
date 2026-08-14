# task-06-event-api - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** A validated HTTP endpoint that accepts charging events and delegates them through a small publishing boundary. Successful broker acknowledgement returns `202 Accepted`; invalid input and publishing failures are handled predictably.

**Why this approach:** Keep the controller thin and separate HTTP DTOs from Kafka/domain models. Task 6 owns the boundary contract; Task 7 supplies the concrete Kafka producer behind it.

**What it will NOT do:** It will not write to the database, implement consumer/state-transition logic, or add GET endpoints. It will not duplicate Task 7's concrete Kafka topic/producer implementation.

**Effort:** Short
**Risk:** Medium - the public response must be gated by an asynchronous broker acknowledgement while the producer implementation is delivered in the next task.
**Decisions to sanity-check:** Use `Instant` in the boundary DTO; use a small publishing port; keep exact 5xx body/subtype unspecified beyond the documented contract.

Your next move: run `$omo:start-work task-06-event-api` in a task-owned feature branch.

---

> TL;DR (machine): Short/medium-risk API boundary work: request DTO + validation + thin controller + acknowledgement-gated publishing port, with focused slice tests; real Kafka startup is deferred to Task 7.

## Scope
### Must have
- `POST /api/v1/charging-events` in `com.example.charging.controller`.
- Separate immutable `ChargingEventRequest` DTO containing `eventId`, `chargerId`, `sessionId`, `eventType`, `sequence`, `batteryLevel`, `chargedKwh`, and `occurredAt`; use `Instant` for the time field and external ISO-8601 input with an offset.
- Boundary validation before delegation: nonblank `eventId`, `chargerId`, `sessionId`, `eventType`; positive `sequence`; non-null `occurredAt`. Do not invent undocumented ranges for battery or energy fields.
- A named synchronous `com.example.charging.application.ChargingEventPublisher` boundary with `void publish(ChargingEventPublishCommand command)`. The method returns only after its implementation confirms broker acknowledgement; it throws `ChargingEventPublishException` for publish failure/timeout. Task 7 supplies the concrete Kafka adapter.
- `202 Accepted` only after the publishing boundary reports broker acknowledgement; publishing exception/timeout maps to an HTTP `5xx` response.
- Controller has no repository/entity access and performs no session mutation.
- Update only the Task 6 marker in `docs/TASK.md` after verification.
### Must NOT have (guardrails, anti-slop, scope boundaries)
- No concrete `KafkaTemplate`, topic declaration, producer implementation, or Kafka message implementation owned by Task 7; Task 6 owns only `ChargingEventPublishCommand` and the publisher contract.
- No JPA queries, database writes, consumer/listener, state transitions, retry/DLT, GET APIs, authentication, new dependencies, or schema changes.
- No Entity exposure over HTTP, no broad exception framework, and no speculative validation rules.
- No full application boot or real broker publish is claimed in Task 6, because the concrete publisher is intentionally delivered in Task 7. No Testcontainers or broad end-to-end suite here.

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: tests-after + JUnit 5 / Spring Boot test with focused controller boundary checks; no broad integration suite.
- Evidence: `.omo/evidence/task-06-event-api/` with one artifact per todo and final-wave report.

## Execution strategy
### Parallel execution waves
- Wave 1: DTO contract and publishing boundary can be designed together, but implementation is sequential where the controller depends on both.
- Wave 2: controller + focused slice tests, then documentation marker. Real Kafka startup/publish verification is explicitly deferred to Task 7.
- Final wave: F1-F4 parallel review after all implementation todos.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | none | 2 | none |
| 2 | 1 | 3 | none |
| 3 | 1, 2 | 4 | none |
| 4 | 3 | 5 | none |
| 5 | 4 | F1-F4 | none |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [x] 1. Define the immutable HTTP request contract and boundary validation
  What to do / Must NOT do: Add `src/main/java/com/example/charging/controller/dto/ChargingEventRequest.java` as a record (or equivalent immutable DTO) with the eight documented fields, `Instant occurredAt`, and only the minimum validation annotations. Do not expose `ChargingEvent` entity or add undocumented field rules.
  Parallelization: Wave 1 | Blocked by: none | Blocks: 2 | References (executor has NO interview context - be exhaustive): `docs/PRD.md:198-225`, `docs/TASK.md:127-143`, `AGENTS.md:390-420`, `src/main/java/com/example/charging/domain/ChargingEvent.java:23-49`.
  Acceptance criteria (agent-executable): Compile succeeds; DTO binds the documented JSON shape; invalid blank IDs/type, non-positive sequence, or missing occurredAt are rejected before any publisher invocation; valid offset-bearing ISO-8601 time binds to `Instant`.
  QA scenarios (name the exact tool + invocation): happy: focused MockMvc/validator test with the PRD JSON; failure: same test submits each required-field violation and asserts `400` plus no publisher call. Evidence `.omo/evidence/task-06-event-api/todo-1-dto-validation.txt`.
  Commit: N | included in the task implementation commit

- [x] 2. Add the minimal publishing handoff contract
  What to do / Must NOT do: Add `ChargingEventPublishCommand` and `ChargingEventPublisher#publish(ChargingEventPublishCommand)` under `com.example.charging.application`. The synchronous method contract returns only after broker acknowledgement; `ChargingEventPublishException` represents publish failure or timeout. Keep KafkaTemplate/topic/serialization out of this task; Task 7 implements the concrete adapter.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 3 | References (executor has NO interview context - be exhaustive): `docs/TASK.md:147-161`, `docs/PRD.md:219-225`, `AGENTS.md:149-164`, `AGENTS.md:390-401`.
  Acceptance criteria (agent-executable): The exact method/value/exception types compile; the contract has no repository/entity dependency, no retry loop, and no Kafka infrastructure implementation. `eventType` is `ChargingEventType`; unknown enum text is rejected as HTTP `400` during binding.
  QA scenarios (name the exact tool + invocation): happy: `./gradlew test --tests '*ChargingEventPublisherContractTest'` asserts a fake implementation can return normally; failure: the same test asserts `ChargingEventPublishException` is the failure signal. Evidence `.omo/evidence/task-06-event-api/todo-2-publishing-port.txt`.
  Commit: N | included in the task implementation commit

- [x] 3. Implement the thin POST controller and response/error mapping
  What to do / Must NOT do: Add `ChargingEventController` for `POST /api/v1/charging-events`; validate/bind request, convert to the handoff value, delegate, and return `202` only on acknowledged success. Map publisher failure/timeout to a `5xx` response without inventing a broad error framework. Do not access repositories or mutate entities.
  Parallelization: Wave 2 | Blocked by: 1, 2 | Blocks: 4 | References (executor has NO interview context - be exhaustive): `docs/TASK.md:127-143`, `docs/PRD.md:67-80`, `docs/PRD.md:219-225`, `AGENTS.md:132-164`, `AGENTS.md:424-443`.
  Acceptance criteria (agent-executable): `POST /api/v1/charging-events` with valid JSON returns `202` only when the fake publisher returns normally; `ChargingEventPublishException` returns `5xx`; invalid JSON or offset-less timestamp returns `400` with zero publish calls; controller source contains no repository/entity access. Required event context is logged on publish failure without sensitive payload data.
  QA scenarios (name the exact tool + invocation): happy: `./gradlew test --tests '*ChargingEventControllerTest'` with acknowledged fake asserts `202`; failure: the same class covers publisher exception, missing required fields, unknown eventType, and offset-less timestamp with expected statuses/call counts. Evidence `.omo/evidence/task-06-event-api/todo-3-controller.txt`.
  Commit: N | included in the task implementation commit

- [x] 4. Add focused lightweight API tests and bounded application verification
  What to do / Must NOT do: Add only the minimal slice/unit tests needed for request validation, acknowledgement gating, and failure mapping. Run `./gradlew test`; do not run or claim full boot/broker delivery because Task 7 supplies the concrete publisher. Do not add Testcontainers or broad end-to-end tests here.
  Parallelization: Wave 2 | Blocked by: 3 | Blocks: 5 | References (executor has NO interview context - be exhaustive): `AGENTS.md:455-483`, `AGENTS.md:570-590`, `docs/TASK.md:141-143`, `docker-compose.yml:1-46`, `src/main/resources/application.yml:21-34`.
  Acceptance criteria (agent-executable): `./gradlew test` exits 0 and the named focused tests execute; test output is not `NO-SOURCE`; no application process or infrastructure teardown is needed because this is a slice/unit verification.
  QA scenarios (name the exact tool + invocation): happy: `./gradlew test --tests '*ChargingEventControllerTest' --tests '*ChargingEventPublisherContractTest'`; failure: assert invalid input and publisher failure paths remain covered. Evidence `.omo/evidence/task-06-event-api/todo-4-verification.txt`.
  Commit: N | included in the task implementation commit

- [x] 5. Mark Task 6 complete after verification
  What to do / Must NOT do: Change only the Task 6 heading in `docs/TASK.md` to its repository convention's completed marker after todos 1-4 pass. Do not mark completion on code presence alone.
  Parallelization: Wave 2 | Blocked by: 4 | Blocks: F1-F4 | References (executor has NO interview context - be exhaustive): `docs/TASK.md:127-145`, `AGENTS.md:594-610`, `AGENTS.md:646-663`.
  Acceptance criteria (agent-executable): A script first asserts Todo 4's focused test report exists and `./gradlew test` succeeded, then changes only the Task 6 heading to `[완료]`; `git diff --check` exits 0 and no out-of-scope product files changed. Real broker acknowledgement remains a Task 7 acceptance, not a Task 6 claim.
  QA scenarios (name the exact tool + invocation): happy: `test -s .omo/evidence/task-06-event-api/todo-4-verification.txt && rg -n '^### 6\. Charging Event API 구현 \[완료\]$' docs/TASK.md`; failure: run the precondition script with the evidence file absent in a disposable copy and assert non-zero/no mutation. Evidence `.omo/evidence/task-06-event-api/todo-5-task-marker.txt`.
  Commit: N | included in the task implementation commit

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [x] F1. Plan compliance audit
  References: `.omo/plans/task-06-event-api.md`, `docs/PRD.md:198-225`, `docs/TASK.md:127-161`.
  Acceptance: reviewer confirms all five todos and the Task 6/7 boundary, with no missing acceptance or evidence path; APPROVE only on exact match. QA/evidence: inspect plan structure and reports, write `.omo/evidence/task-06-event-api/f1-plan-compliance.md`; Commit: N.
- [x] F2. Code quality review
  References: changed Java files, `AGENTS.md:115-164,390-443`.
  Acceptance: thin controller, immutable DTO, explicit port, no slop/scope drift, diagnostics clean; APPROVE or list blocking findings. QA/evidence: run `git diff --check` and focused Gradle tests, write `.omo/evidence/task-06-event-api/f2-code-quality.md`; Commit: N.
- [x] F3. Real manual QA
  References: `docs/TASK.md:141-143`, todo-3/test evidence.
  Acceptance: execute the focused MockMvc scenarios against the real Spring MVC boundary; observe 202/400/5xx and no-call behavior; APPROVE only with captured output. QA/evidence: `./gradlew test --tests '*ChargingEventControllerTest'`, write `.omo/evidence/task-06-event-api/f3-manual-qa.md`; Commit: N.
- [x] F4. Scope fidelity
  References: Must NOT have section, `AGENTS.md:20-24,644-663`.
  Acceptance: changed product files are limited to DTO/controller/publisher contract/tests and Task 6 marker; no Kafka adapter, DB, consumer, dependency, or unrelated docs changes. QA/evidence: inspect `git diff --name-only` and forbidden-path scan, write `.omo/evidence/task-06-event-api/f4-scope.md`; Commit: N.

## Commit strategy

Use the task-owned branch `feature/task-06-event-api` created from the latest `main`. Keep one coherent implementation commit (or small commits matching todos 1-5) and do not commit unrelated worktree changes. No PR or merge is part of this plan.

## Success criteria

- Valid requests bind and pass documented validation; malformed requests return `400` before publishing.
- Acknowledged publishing returns `202`; publish failure/timeout returns `5xx`.
- Controller is thin and has no DB/entity access.
- Focused tests and Gradle verification pass; Task 6 marker is updated only afterward. Real Kafka startup and broker delivery are Task 7 criteria.
