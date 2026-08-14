# task-07-kafka-producer - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** A real Spring Kafka Producer behind the existing event-publishing boundary. Charging events will be sent to the configured topic with session-based partitioning, and the HTTP API will only acknowledge after Kafka confirms the send.

**Why this approach:** Keep the existing Controller contract unchanged, map to a dedicated Kafka DTO, and centralize acknowledgement/failure handling in one producer adapter.

**What it will NOT do:** It will not implement the Kafka Consumer, database processing, retry/DLT, or business state transitions. It will not redesign the existing topic initializer or add speculative infrastructure.

**Effort:** Medium
**Risk:** Medium - producer acknowledgement and local broker behavior must be observed, while failure/timeout mapping must preserve the HTTP contract.
**Decisions to sanity-check:** Separate Kafka DTO, `sessionId` key, 3-second acknowledgement bound, and synchronous port implementation.

Your next move: run `$omo:start-work task-07-kafka-producer` on a task-owned feature branch.

---

> TL;DR (machine): Medium Kafka adapter task: dedicated message DTO, KafkaTemplate producer, bounded acknowledgement/error mapping, focused tests, and one Compose publication smoke check.

## Scope
### Must have
- `ChargingEventMessage` as a separate immutable Kafka DTO with the eight documented event fields.
- `ChargingEventProducer` as the single Spring implementation of `ChargingEventPublisher`.
- `KafkaTemplate<String, ChargingEventMessage>` send to `charging-events` with `command.sessionId()` as key.
- Wait for broker acknowledgement for at most 3 seconds; map execution, timeout, and interruption failures to `ChargingEventPublishException`, restoring the interrupt flag when interrupted.
- Preserve the existing Controller contract: successful send returns through the port and yields `202`; producer failure reaches the existing `5xx` mapping.
- Focused producer tests for exact topic/key/payload mapping, acknowledgement success, exception, timeout, and interruption behavior.
- One Docker Compose smoke path proving a valid POST produces a Kafka record with key `sessionId` and expected JSON payload.
- Mark only Task 7 complete after verification.
### Must NOT have (guardrails, anti-slop, scope boundaries)
- No consumer/listener, application service, repository/entity access, DB writes, idempotency/order/state logic, retry/DLT, GET API, schema/config redesign, or new dependency.
- No producer-side custom retry loop, `while`, `Thread.sleep`, random Kafka key, or swallowed exception.
- No Testcontainers full-flow test; that belongs to the final integration-test task.
- Do not rewrite the existing topic initializer or Controller unless a direct wiring defect blocks this task.

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: tests-after + JUnit 5, Spring Boot test, and Mockito/`CompletableFuture` producer-future seams; one real Docker Compose Kafka smoke check.
- Evidence: `.omo/evidence/task-07-kafka-producer/` with one artifact per todo and final-wave reports.

## Execution strategy
### Parallel execution waves
- Wave 1: define Kafka DTO and producer adapter contract/implementation.
- Wave 2: focused producer tests and real Compose smoke verification.
- Wave 3: Task checklist update after all evidence.
- Final wave: F1-F4 parallel review.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | none | 2 | none |
| 2 | 1 | 3 | none |
| 3 | 2 | 4 | none |
| 4 | 3 | 5 | none |
| 5 | 4 | F1-F4 | none |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [x] 1. Define the dedicated Kafka message DTO
  What to do / Must NOT do: Add `src/main/java/com/example/charging/kafka/ChargingEventMessage.java` as an immutable record with `eventId`, `chargerId`, `sessionId`, `eventType`, `sequence`, `batteryLevel`, `chargedKwh`, and `occurredAt`. Do not expose JPA entities or serialize the application command directly.
  Parallelization: Wave 1 | Blocked by: none | Blocks: 2 | References: `docs/PRD.md:297-322`, `docs/TASK.md:147-161`, `AGENTS.md:390-401`, `src/main/java/com/example/charging/application/ChargingEventPublishCommand.java:7-15`.
  Acceptance criteria: DTO compiles, has exactly the documented payload fields/types, and maps every command field without loss or renamed key.
  QA scenarios: happy `./gradlew test --tests '*ChargingEventMessageTest'` checks field mapping; failure test asserts no missing/extra payload field through serialized JSON shape. Evidence `.omo/evidence/task-07-kafka-producer/todo-1-message.txt`.
  Commit: N | included in one Task 7 commit

- [x] 2. Implement the acknowledgement-gated Kafka producer adapter
  What to do / Must NOT do: Add `ChargingEventProducer` under `com.example.charging.kafka` as `@Component` implementing `ChargingEventPublisher`. Inject `KafkaTemplate<String, ChargingEventMessage>`, map command to message, call `send("charging-events", command.sessionId(), message)`, and block with `get(3, TimeUnit.SECONDS)`. Translate execution/timeout/interruption failures to `ChargingEventPublishException`; restore interrupt status. Do not add retry loops or DB access.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 3 | References: `src/main/java/com/example/charging/application/ChargingEventPublisher.java:3-8`, `src/main/java/com/example/charging/application/ChargingEventPublishException.java:3-11`, `docs/PRD.md:219-225,297-322`, `AGENTS.md:149-155,213-229,380-386`, `src/main/java/com/example/charging/config/KafkaTopicConfig.java:27-33`.
  Acceptance criteria: a successful future completes normally; topic and key are exact; all command fields map; failed/timeout/interrupted futures throw the typed exception; interrupted thread remains interrupted; the class is the sole production Publisher bean.
  QA scenarios: happy Mockito future completes and asserts exact send topic/key/value; failure futures for execution, timeout, and interruption assert typed exception and interrupt behavior. Evidence `.omo/evidence/task-07-kafka-producer/todo-2-producer.txt`.
  Commit: N | included in one Task 7 commit

- [x] 3. Verify Controller wiring and focused producer behavior
  What to do / Must NOT do: Replace only the Task 6 test-only publisher assumption with the production producer being discoverable by Spring; retain Controller behavior. Add focused tests for producer and a minimal context/wiring check. Do not change API paths/status semantics or add consumer logic.
  Parallelization: Wave 2 | Blocked by: 2 | Blocks: 4 | References: `src/main/java/com/example/charging/controller/ChargingEventController.java:23-53`, `src/test/java/com/example/charging/controller/ChargingEventControllerTest.java`, `src/main/resources/application.yml:21-34`, `build.gradle:20-38`.
  Acceptance criteria: focused tests execute (not `NO-SOURCE`); Spring can discover one `ChargingEventPublisher` production bean when KafkaTemplate is supplied; valid acknowledged publish still yields `202` and typed producer failure still yields `5xx`.
  QA scenarios: happy `./gradlew test --tests '*ChargingEventProducerTest' --tests '*ChargingEventControllerTest'`; failure removes/throws the send future and asserts no false `202`. Evidence `.omo/evidence/task-07-kafka-producer/todo-3-wiring.txt`.
  Commit: N | included in one Task 7 commit

- [x] 4. Run the minimal Docker Compose Kafka publication smoke test
  What to do / Must NOT do: Start existing Compose services, run bounded Spring Boot startup on fixed port `18087`, start a uniquely grouped console consumer before the POST, submit one unique valid event, and inspect the matching record from `charging-events`. Do not delete volumes or alter Compose/topic initializer.
  Parallelization: Wave 2 | Blocked by: 3 | Blocks: 5 | References: `docker-compose.yml:19-43`, `src/main/java/com/example/charging/config/KafkaTopicInitializer.java:13-68`, `docs/TASK.md:156-161`, `docs/PRD.md:297-322`.
  Acceptance criteria: Compose healthy; application starts after existing topic initialization; POST returns `202`; Kafka consumer observes key equal to the unique request `sessionId` and JSON containing all expected fields; teardown leaves no app process and preserves volumes.
  QA scenarios: happy `docker compose up -d && docker compose ps`, bounded `./gradlew bootRun --args='--server.port=18087'`, then start `docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --group task7-smoke-$RUN_ID --from-beginning --timeout-ms 15000 --max-messages 100 --property print.key=true` in the background, POST with `curl -i -X POST http://localhost:18087/api/v1/charging-events -H 'Content-Type: application/json' -d '{"eventId":"task7-<RUN_ID>","chargerId":"charger-task7","sessionId":"session-task7-<RUN_ID>","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":"2026-08-14T12:00:00+09:00"}'`, wait for consumer completion, and assert its captured output contains the unique eventId plus printed key `session-task7-<RUN_ID>`. Failure: stop Kafka or use a failed send and assert API returns `5xx`; capture cleanup receipt. Evidence `.omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.txt`.
  Commit: N | included in one Task 7 commit

- [x] 5. Mark Task 7 complete after verification
  What to do / Must NOT do: Change only the Task 7 heading in `docs/TASK.md` to `[완료]` after todos 1-4 evidence passes. Do not mark completion on unit tests without the broker smoke record.
  Parallelization: Wave 3 | Blocked by: 4 | Blocks: F1-F4 | References: `docs/TASK.md:147-163`, `AGENTS.md:594-610,646-663`.
  Acceptance criteria: precondition script finds all todo evidence and successful smoke marker, then only Task 7 heading changes; `git diff --check` passes and no out-of-scope files are changed.
  QA scenarios: happy `test -s .omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.txt && rg -n '^### 7\\. Kafka Producer 구현 \\[완료\\]$' docs/TASK.md`; failure missing smoke evidence causes nonzero precondition and no marker mutation. Evidence `.omo/evidence/task-07-kafka-producer/todo-5-task-marker.txt`.
  Commit: N | included in one Task 7 commit

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [x] F1. Plan compliance audit
  References: plan, PRD Kafka/API sections, TASK Task 7. Acceptance: all todos/evidence and exact handoff contract match; QA/evidence `.omo/evidence/task-07-kafka-producer/f1-plan-compliance.md`; Commit: N.
- [x] F2. Code quality review
  References: Kafka DTO/producer/tests and AGENTS architecture rules. Acceptance: explicit mapping, bounded wait, typed errors, no retries/DB/scope creep; QA `git diff --check` + focused tests; evidence `.omo/evidence/task-07-kafka-producer/f2-code-quality.md`; Commit: N.
- [x] F3. Real manual QA
  References: todo-4 Compose smoke. Acceptance: real POST returns 202 and Kafka record has expected key/payload; failure path returns 5xx; evidence `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md`; Commit: N.
- [x] F4. Scope fidelity
  References: Must NOT have and TASK 7. Acceptance: only DTO/producer/tests/Task marker/evidence changed; no consumer/DB/retry/DLT/dependency/Compose redesign; evidence `.omo/evidence/task-07-kafka-producer/f4-scope.md`; Commit: N.

## Commit strategy

Use a task-owned branch `feature/task-07-kafka-producer` from the latest `main`. Prefer one coherent commit for DTO, producer, tests, and Task marker; do not include unrelated Task 6 evidence or review cleanup.

## Success criteria

- Production Spring context has one `ChargingEventPublisher` implementation.
- POST events publish to `charging-events` with `sessionId` key and the documented JSON payload.
- HTTP `202` follows broker acknowledgement; publish failure/timeout becomes `5xx`.
- Focused tests and one Compose broker smoke flow pass; no producer-side DB or consumer behavior is introduced.
