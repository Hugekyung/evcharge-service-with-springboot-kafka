# F2 Code-Quality Review — Task 06 Event API

## Result

- `codeQualityStatus`: **WATCH**
- `recommendation`: **APPROVE**
- `blockers`: none for Task 6's explicitly staged scope.

## Scope and independent checks

Reviewed the complete Task 6 Java/test additions in the current dirty worktree (the files are untracked, so a commit-range diff is empty):

- `src/main/java/com/example/charging/controller/ChargingEventController.java`
- `src/main/java/com/example/charging/controller/dto/ChargingEventRequest.java`
- `src/main/java/com/example/charging/application/ChargingEventPublishCommand.java`
- `src/main/java/com/example/charging/application/ChargingEventPublisher.java`
- `src/main/java/com/example/charging/application/ChargingEventPublishException.java`
- `src/test/java/com/example/charging/controller/ChargingEventControllerTest.java`
- `src/test/java/com/example/charging/application/ChargingEventPublisherContractTest.java`

I read `AGENTS.md`, `docs/PRD.md`, `docs/TASK.md`, current source, and supplied evidence. Evidence was treated as untrusted until direct inspection/reproduction.

- Fresh focused test: `./gradlew cleanTest test --tests '*ChargingEventControllerTest' --tests '*ChargingEventPublisherContractTest'` — **PASS**, 15 tests (13 controller, 2 publisher-contract), zero failures/errors.
- `git diff --check` — **PASS**, but it does not inspect the untracked Task 6 Java/test files; this is only a tracked-file whitespace check.
- Fresh `./gradlew bootRun --args='--server.port=0'` — **expected staged failure**. Spring aborts because Task 7 has not yet supplied the intentionally deferred concrete `ChargingEventPublisher` bean. The Task 6 plan expressly forbids claiming full application boot or broker delivery before Task 7, so this is recorded as a handoff limitation, not a Task 6 failure.
- Source scan — no repository/entity/`KafkaTemplate`/consumer/transaction imports or behavior in the Task 6 controller/application additions. No Kafka/DB scope creep found.

## Findings

### CRITICAL

None.

### HIGH

None.

### MEDIUM

1. **The publisher contract tests are tautological and give false confidence.** [ChargingEventPublisherContractTest.java](../../../src/test/java/com/example/charging/application/ChargingEventPublisherContractTest.java:14) constructs lambdas whose behavior is asserted immediately. They test neither a production publisher nor the controller mapping, and any interface implementation would make them irrelevant. Remove or replace them when Task 7 supplies a concrete producer with observable acknowledgement/timeout behavior. This is not the startup blocker by itself.

2. **The claimed `git diff --check` verification is incomplete for this task's actual source scope.** All Task 6 Java/test files are untracked; standard `git diff --check` sees none of them. The supplied evidence paths exist, so this is not an artifact-less success claim, but its scope should be described accurately or paired with an untracked-file whitespace check.

### LOW

None.

## Correctness, scope, and tests

- The controller is otherwise thin: request binding/validation, command mapping, publisher invocation, and HTTP mapping only. It does not access persistence or Kafka directly.
- DTO validation covers all required minimum fields: nonblank IDs, nonnull enum/timestamp, and positive sequence. `Instant` binding correctly rejects an offset-less timestamp.
- The controller tests are relevant for accepted/5xx behavior and malformed HTTP input. They check that invalid requests do not invoke the publisher.
- No needless production parsing, normalization, data extraction, generic framework, untyped escape hatch, or module-size issue was found. Each changed source file is well below 250 pure LOC.

## Required skill-perspective check

Ran: **yes** — explicitly loaded `omo:remove-ai-slops` and `omo:programming` before judging maintainability and test relevance.

- **`remove-ai-slops`: violation found in tests.** The two publisher-contract tests are tautological hand-written-fake tests rather than behavior tests. No deletion-only test, removal-verification test, production over-parsing/normalization, dead production code, needless production extraction, or other slop category was found.
- **`programming`: violation found in tests.** The same tests mirror the test implementation rather than an externally observable contract. No brittle prompt test, untyped escape hatch, needless production abstraction, or misplaced production validation/parsing was found. This skill has no Java-specific language reference, so its applicable generic design and test criteria were used.

## Quality gates

- Focused MockMvc/contract suite: **PASS** (fresh, 15 tests).
- Full application startup: **N/A by plan** — the concrete publisher belongs to Task 7. The observed missing-bean failure is a required handoff condition to resolve there, not acceptance evidence for Task 6.
- Lint, separate Java typecheck/LSP diagnostics, static/security scan: **N/A** — no configured applicable gate found.
- Tracked diff whitespace check: **PASS**, with the untracked-scope limitation noted above.

## Follow-up

Task 7 must supply the real acknowledgement-gated publisher bean and replace the lambda-only contract tests with concrete producer behavior coverage. That is intentionally deferred work, not a Task 6 approval blocker.

No product code was changed by this review; this report and the ledger entry are the only writes.
