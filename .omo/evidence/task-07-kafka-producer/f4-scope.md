# Task 7 F4 — scope fidelity

Reviewed baseline: `main` / `46bfcfbbaa52e0c458668271f932ce420116b900`

Recommendation: **PASS / APPROVE**

## Original intent

Implement Task 7 only: a dedicated Kafka event DTO and producer, publish to `charging-events` with `sessionId` as key, wait for broker acknowledgement within the bounded HTTP contract, cover the behavior with focused tests, and mark Task 7 complete after verification.

## Desired outcome

The Task 6 POST path reaches one production Kafka publisher and returns `202` only after an acknowledged send. Kafka failure or timeout reaches the existing `5xx` path. No consumer, database processing, retry/DLT, dependency, Compose, or volume redesign is introduced.

## User outcome review

PASS. The authoritative dirty-worktree product manifest contains exactly the allowed Task 7 surfaces:

- `src/main/java/com/example/charging/kafka/ChargingEventMessage.java`
- `src/main/java/com/example/charging/kafka/ChargingEventProducer.java`
- `src/test/java/com/example/charging/kafka/ChargingEventMessageTest.java`
- `src/test/java/com/example/charging/kafka/ChargingEventProducerTest.java`
- `src/main/resources/application.yml`
- `docs/TASK.md`

The `application.yml` delta is only `spring.kafka.producer.properties.max.block.ms: 3000`, which bounds the Kafka client's pre-future send path after the timeout repair. The documentation delta marks only Task 7 complete. The remaining `.omo` paths are workflow plan/evidence/state artifacts, not product behavior.

## Scope checks

| Check | Result | Evidence |
| --- | --- | --- |
| Complete product manifest | PASS | Combined `git diff --name-only` and `git ls-files --others --exclude-standard`; six paths listed above |
| Consumer/listener changes | PASS | No product diff under consumer/application surfaces; no `@KafkaListener` match in Task 7 files |
| DB/schema/repository changes | PASS | No diff under domain, repository, application, or `src/main/resources/db`; no JPA/JDBC/transaction match |
| Retry/DLT/custom retry changes | PASS | No Retry/DLT/loop/`Thread.sleep` match; producer has exactly one `kafkaTemplate.send` call |
| Dependency changes | PASS | Empty diff for `build.gradle` and `settings.gradle` |
| Compose/volume changes | PASS | Empty `docker-compose.yml` diff; declared `postgres-data` and `kafka-data` remain present; live `evcharging_postgres-data` and `evcharging_kafka-data` volumes remain |
| GET API or unrelated product changes | PASS | No controller/API product delta; manifest allowlist exact |
| Whitespace integrity | PASS | `git diff --check`, exit 0 |
| Focused verification | PASS | Forced Gradle run: message 1/1 and producer 6/6 tests passed, zero failures/errors; `BUILD SUCCESSFUL` |

## Direct overfit/slop and maintenance pass

`remove-ai-slops`: PASS. No deletion-only or requested-removal test, prose pin, tautological assertion, implementation-derived expected output, needless production parsing/normalization/extraction, dead code, speculative abstraction, oversized module, or scope drift. The tests assert JSON contract, topic/key/value mapping, typed failures, whole-attempt timeout, interruption restoration, and Spring bean wiring. The KafkaTemplate mock is the narrow adapter seam.

`programming`: PASS using its generic documented criteria; it has no Java-specific route. The product change is typed, immutable at the DTO boundary, constructor-injected, small, explicit, and dependency-free. The timeout fix uses one deadline and the existing Kafka client property rather than a custom retry or helper framework.

The code-quality report at `.omo/evidence/task-07-kafka-producer/f2-code-quality.md` explicitly records both the `remove-ai-slops` overfit categories and the generic `programming` perspective. This direct F4 pass independently reproduces that coverage.

## Adversarial state review

- `dirty_worktree`: PASS with disclosure. Task 7 Java source/tests remain untracked and configuration/docs remain modified, so HEAD alone is stale as a product manifest. Both tracked and untracked paths were inventoried and the six product files were hash-bound during this review.
- `stale_state`: PASS. Review used current file contents and a forced `--rerun-tasks --no-daemon` test run, not prior prose or cached Gradle success.
- `misleading_success_output`: PASS. Exact path allowlist, forbidden-path diffs, forbidden-pattern scans, JUnit XML counts, and live volume names back the verdict.
- `cleanup`: PASS. No app, consumer, container mutation, volume deletion, or product edit was performed. The Gradle single-use daemon exited normally.

## Checked artifacts

- `.omo/plans/task-07-kafka-producer.md`
- `.omo/evidence/task-07-kafka-producer/f1-plan-compliance-recheck.md`
- `.omo/evidence/task-07-kafka-producer/f2-code-quality.md`
- `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md`
- `.omo/evidence/task-07-kafka-producer/fix-send-timeout.txt`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventMessageTest.xml`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventProducerTest.xml`

## Blockers and evidence gaps

Blockers: none.

Exact evidence gaps: none for F4. The product files are still uncommitted/untracked, but commit creation is outside this scope criterion and is disclosed rather than hidden.
