# Task 7 — Global Code Quality Review

## Result

- `codeQualityStatus`: **BLOCK**
- `recommendation`: **REQUEST_CHANGES**
- `blockers`: **Fix the Kafka payload's `occurredAt` serialization to the documented ISO-8601 string, and replace the detached mapper test with a test of the configured production serializer (or an end-to-end record assertion that checks the field type).**

## Scope and SHA-bound ledger

Reviewed current working-tree Task 7 product paths, not merely `HEAD` (Task 7 source and tests are untracked/ignored by Git). Review commit base: `46bfcfbbaa52e0c458668271f932ce420116b900`.

| Path | SHA-256 |
| --- | --- |
| `src/main/java/com/example/charging/kafka/ChargingEventMessage.java` | `146b9699d8029c7c243f05d9c5b71f35784f44f548c9b442b1d17f5e7caf8817` |
| `src/main/java/com/example/charging/kafka/ChargingEventProducer.java` | `3971cc79fcc7108798926f23bd973f0b974706e8a9f1da9061c9c8f1329ce6be` |
| `src/test/java/com/example/charging/kafka/ChargingEventMessageTest.java` | `232b91889e88b2fcf72ea18ac4d6c35d8b443b1ecd6843aac477382b99edaaf1` |
| `src/test/java/com/example/charging/kafka/ChargingEventProducerTest.java` | `18517206bda252a620d6592570286052215fbfbd12c60aac5a3b38ea8f7e553c` |
| `src/main/resources/application.yml` | `96dce306ce042878e99f344a1c6502f998f437033dcf4ed997141b0fe078a5b3` |

## Findings

### CRITICAL

None.

### HIGH

- **Kafka's production payload violates the documented timestamp contract.** The PRD's Kafka message requires `occurredAt` as an ISO-8601 string, but the inspected real Kafka consumer transcript records `"occurredAt":1786676400.000000000` instead. See [todo-4-compose-smoke.md](/Users/yanghaechan/orca/projects/evcharging/.omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.md:67) and the required payload definition in [PRD.md](/Users/yanghaechan/orca/projects/evcharging/docs/PRD.md:299). The current `JsonSerializer` configuration at [application.yml](/Users/yanghaechan/orca/projects/evcharging/src/main/resources/application.yml:27) therefore does not emit the wire format that [ChargingEventMessageTest.java](/Users/yanghaechan/orca/projects/evcharging/src/test/java/com/example/charging/kafka/ChargingEventMessageTest.java:15) claims to validate. This is a producer/consumer contract break, not a presentation issue.

  Required fix: configure or supply the Kafka value serializer so Java time is written as ISO-8601, then add a test that runs the actual configured Kafka serializer or inspects a produced record. The test must assert a textual `occurredAt`, not only the presence of the field.

### MEDIUM

None.

### LOW

None.

## Tests and checks run

- `./gradlew test --tests 'com.example.charging.kafka.*'` — PASS.
- `./gradlew test` — PASS.
- `git diff --check` — PASS.

Those runs establish compilation and existing regression health, but do not remove the HIGH finding: the message JSON unit test creates its own differently configured `ObjectMapper` with `WRITE_DATES_AS_TIMESTAMPS` disabled, whereas production uses Spring Kafka's `JsonSerializer`. The pre-existing live smoke artifact is the relevant end-to-end evidence and exposes the discrepancy.

## Required skill-perspective check

Ran the required `remove-ai-slops` and `programming` skill-perspective checks before judging test relevance and maintainability. The programming skill has no Java-specific reference, so its general criteria were applied directly.

- **remove-ai-slops: violation found.** The production serialization test is false confidence: it verifies an isolated mapper rather than the actual producer serializer, so it validates a wire-format result that production does not perform. No deletion-only tests, requested-removal-only tests, dead code, unnecessary parsing/normalization, or oversized source modules were found elsewhere.
- **programming: violation found.** The test is implementation-adjacent rather than boundary/contract-facing: it mirrors a local serializer configuration and misses the real boundary output. No untyped escape hatch, needless abstraction, or unnecessary production validation was found in the producer itself.

## Positive review notes

The producer remains small and scoped: constructor injection, explicit eight-field transport mapping, fixed `charging-events` topic, `sessionId` key, acknowledgement gating, interruption restoration, and a bounded pre-future send path via `max.block.ms` are all appropriate. The failure is specifically the timestamp wire format and the test seam that hid it.

## Final recommendation

**REQUEST_CHANGES** — one HIGH correctness finding remains.

