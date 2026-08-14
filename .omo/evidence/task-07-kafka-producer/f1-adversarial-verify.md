# Task 7 F1 Adversarial Verification

AdversarialVerify.verdict: confirmed

## Decision

The F1 re-audit approval is independently confirmed. Current production code, configuration, forced fresh tests, recorded real HTTP evidence, scope scans, Task 7 marker, and final cleanup state satisfy the Task 7 acceptance criteria. No criterion-linked blocker was found.

## Exact source evidence

- `src/main/java/com/example/charging/kafka/ChargingEventProducer.java:26` creates a monotonic deadline with `System.nanoTime()` before `KafkaTemplate.send(...)` at line 28.
- Lines 28-29 invoke `send("charging-events", command.sessionId(), message)` and call `get(remainingNanos(deadline), NANOSECONDS)`. Lines 42-47 compute only the time left and throw `TimeoutException` if none remains.
- Lines 30-32 catch `InterruptedException`, restore the thread interrupt flag, and throw `ChargingEventPublishException`.
- Lines 33-39 map execution, timeout, and pre-future `RuntimeException` failures to `ChargingEventPublishException`.
- `src/main/resources/application.yml:25-29` applies `spring.kafka.producer.properties.max.block.ms: 3000`, bounding Kafka client blocking before a future is returned.
- `src/main/java/com/example/charging/kafka/ChargingEventMessage.java:7-15` is a separate immutable record with exactly the required eight fields.

## Reproduction commands and binary results

```text
./gradlew test --tests com.example.charging.kafka.ChargingEventProducerTest --tests com.example.charging.controller.ChargingEventControllerTest --rerun-tasks --no-daemon
```

Result: exit 0. Fresh XML after the final forced run:

- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventProducerTest.xml`: tests=6, failures=0, errors=0, skipped=0, timestamp=2026-08-14T09:23:05.605Z, suite time=3.153s.
- `build/test-results/test/TEST-com.example.charging.controller.ChargingEventControllerTest.xml`: tests=16, failures=0, errors=0, skipped=0, timestamp=2026-08-14T09:23:05.173Z, suite time=0.429s.

The timed pre-future regression was also forced twice independently:

```text
/usr/bin/time -p ./gradlew test --tests 'com.example.charging.kafka.ChargingEventProducerTest.keepsTheWholePublishAttemptWithinThreeSecondsWhenSendingBlocksBeforeReturningAFuture' --rerun-tasks --no-daemon
```

Both runs exited 0 (`real 7.43s` and `real 7.21s`, including Gradle startup/compilation). The testcase itself waits one second inside mocked `KafkaTemplate.send`, returns an incomplete future, and asserts total producer-call time below 3.5 seconds. A defective implementation that waits a fresh three seconds after the one-second send delay takes about four seconds and fails this assertion. This is a real behavioral distinction, not a mock-call, deletion, prose, or pinned-constant test. The fresh combined XML records this testcase at 3.01 seconds.

## Real HTTP manual-QA evidence

Inspected `.omo/evidence/task-07-kafka-producer/fix-send-timeout.txt` for the specified run:

```text
java -jar build/libs/evcharging-0.0.1-SNAPSHOT.jar --server.port=18087
docker compose stop kafka
curl ... --max-time 5 ... task7-outage-live-20260814 ...
```

Recorded binary outcome: `HTTP/1.1 503`, `HTTP_CODE=503 TIME_TOTAL=3.150482`, `CURL_EXIT=0 ELAPSED_MS=3183`, plus runtime `max.block.ms = 3000`. This is within the requested approximately four-second live bound. Current source/config directly support that outcome. The artifact records application termination, Kafka restart/health polling, both Compose services healthy, and no volume deletion.

## UltraQA classes

- stale state: PASS — the required focused command used `--rerun-tasks --no-daemon`; fresh XML timestamps and exact counts were parsed.
- dirty worktree: PASS — tracked modifications and untracked Task 7 files were inventoried; untracked source/tests were inspected directly. No Git-history claim was used as a substitute.
- misleading success output: PASS — Gradle prose was cross-checked against JUnit XML; manual QA was checked against HTTP status/header, curl exit, and timing fields.
- hung/long commands: PASS — producer regression testcase is approximately 3.01 seconds; live HTTP capture is 3.150482 seconds and curl exit 0.
- flaky tests: PASS — the timing regression passed two forced independent runs, then passed in the final combined forced run.
- repeated interruptions: PASS — source restores the flag; `restoresTheInterruptedFlagWhenBrokerPublishIsInterrupted()` passes; `@AfterEach` clears test-thread residue.
- malformed input: PASS — controller uses `@Valid`; fresh controller suite includes required-field, unknown enum, offset timestamp, control-character, and malformed-JSON rejection coverage.
- prompt injection: N/A — this typed HTTP/Kafka path has no instruction-following or prompt channel.
- cancel/resume: N/A — publishing is one bounded operation with no checkpoint or resumable workflow.

## Slop and programming direct pass

PASS. The producer tests cover observable topic/key/payload, typed failure, whole-attempt timing, interrupt restoration, and Spring wiring. No deletion-only test, requested-removal test, tautology, implementation-mirroring-only test, needless production extraction/parsing/normalization, dead code, speculative abstraction, or oversized changed module was found. The deadline and `max.block.ms` have separate required roles. The available programming skill has no Java-specific route; its general typed-boundary, minimality, test-quality, and maintenance criteria were applied directly. The F1 report explicitly contains the same overfit/slop categories and programming perspective; its coverage matches this independent pass.

## Scope, plan, and marker

Commands:

```text
git diff --check
rg -n '@KafkaListener|KafkaConsumer|ChargingSessionService|Repository|EntityManager|JpaRepository|DataSource|JdbcTemplate|Retry|DeadLetter|DLT|Thread\.sleep|while\s*\(|UUID\.randomUUID|Testcontainers|@GetMapping' src/main/java/com/example/charging/kafka src/test/java/com/example/charging/kafka src/main/resources/application.yml
rg -n 'implements ChargingEventPublisher' src/main/java
rg -n '^### 7\. Kafka Producer 구현 \[완료\]$' docs/TASK.md
```

Results: `git diff --check` exit 0; prohibited-scope scan returned no match; exactly one production implementation of `ChargingEventPublisher`; Task 7 marker is `[완료]`. Plan todos 1-5 are checked and ledger/evidence entries exist. No consumer, DB, retry/DLT, custom retry loop, random key, GET API, dependency, or Compose-definition scope drift was found.

## Cleanup receipt

This verifier did not start an application, stop a container, run a Kafka consumer, or touch volumes. After tests:

- no Gradle Test Executor/Worker, application, `bootRun`, or Kafka console-consumer process remained;
- no listener remained on TCP port 18087;
- `docker compose ps` showed Kafka and PostgreSQL `Up (healthy)`;
- volumes `evcharging_kafka-data` and `evcharging_postgres-data` remained present.

A concurrent shared-worktree QA app/consumer was briefly observed during the audit and was preserved; it exited through its own cleanup before the final receipt above.

## Evidence gaps

None that violates a stated criterion. The outage run is verified from its requested recorded artifact rather than repeated, as instructed. Product files remain untracked/modified in the shared worktree, but commit creation is not a Task 7 acceptance criterion for this gate.
