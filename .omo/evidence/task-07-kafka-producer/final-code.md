# Task 7 final code-quality review

## Result

- SHA reviewed: `0a3b2d1f9747756dea647e7773dd80ab8cc208f6`
- `codeQualityStatus`: **BLOCK**
- `recommendation`: **REQUEST_CHANGES**
- `blockers`: The real Kafka payload serializes `occurredAt` as a numeric timestamp instead of the PRD-required ISO-8601 string. The production serializer must be configured or supplied with the appropriate `ObjectMapper`, and a test must exercise that real serializer path.

## Scope and correctness

Reviewed the complete Task 7 commit diff against `46bfcfb`, including `docs/TASK.md`, the Kafka message record, the producer adapter, Kafka producer configuration, and both new test classes. Scope remains confined to the requested Kafka producer handoff; it adds no database writes, consumer logic, transaction boundary, retry loop, dependency, or Compose change.

`ChargingEventProducer` is the sole production `ChargingEventPublisher`. It sends to `charging-events`, keys records with `sessionId`, maps every command field to the separate Kafka DTO, and waits for acknowledgement before returning. At [ChargingEventProducer.java](../../../src/main/java/com/example/charging/kafka/ChargingEventProducer.java#L25), a monotonic deadline begins before `KafkaTemplate.send`; the future receives only the remaining duration. At [application.yml](../../../src/main/resources/application.yml#L29), `max.block.ms: 3000` bounds Kafka producer work that can occur before a future is returned. This closes the previously demonstrated four-second failure mode. Interrupt status is restored before the typed publish exception is raised.

The timeout repair is correct, but Task 7 is not approvable because the actual JSON serializer does not preserve the documented timestamp representation.

## Findings

### CRITICAL

None.

### HIGH

- [application.yml](../../../src/main/resources/application.yml#L27) configures Spring Kafka's `JsonSerializer` without an ISO-8601 timestamp configuration. The real Kafka consumer capture in `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md:16` shows `"occurredAt":1786676400.000000000`, while [docs/PRD.md](../../../docs/PRD.md#L293) requires external JSON timestamps to be timezone-bearing ISO-8601 and its Kafka message example specifies a string. A subsequent consumer expecting the documented contract will fail to deserialize or will silently receive the wrong wire format. Fix the configured serializer and demonstrate the actual record has a textual ISO-8601 `occurredAt` field.

- [ChargingEventMessageTest.java](../../../src/test/java/com/example/charging/kafka/ChargingEventMessageTest.java#L15) creates an independent `ObjectMapper`, manually disables `WRITE_DATES_AS_TIMESTAMPS`, and tests that mapper rather than the configured Spring Kafka `JsonSerializer`. It therefore passes while the production payload violates the contract. Replace or supplement it with a test that serializes using the application-configured Kafka serializer (or a real broker integration assertion).

### MEDIUM

None.

### LOW

None.

## Verification

Fresh, SHA-head source verification:

```text
./gradlew test --rerun-tasks --no-daemon \
  --tests 'com.example.charging.application.ChargingEventPublisherContractTest' \
  --tests 'com.example.charging.controller.ChargingEventControllerTest' \
  --tests 'com.example.charging.kafka.ChargingEventMessageTest' \
  --tests 'com.example.charging.kafka.ChargingEventProducerTest'
```

Result: `BUILD SUCCESSFUL` (exit 0).

JUnit XML inspected immediately after the run:

| Class | Tests | Failures | Errors |
| --- | ---: | ---: | ---: |
| `ChargingEventPublisherContractTest` | 2 | 0 | 0 |
| `ChargingEventControllerTest` | 16 | 0 | 0 |
| `ChargingEventMessageTest` | 1 | 0 | 0 |
| `ChargingEventProducerTest` | 6 | 0 | 0 |

The producer tests cover observable topic/key/message mapping, failed acknowledgement, acknowledgement timeout, pre-future send blocking, interrupt restoration, and application wiring. The pre-future test is relevant: it would fail under the original future-only timeout. Its bounded latch and 500 ms margin make it a proportionate regression test, not a deletion-only, tautological, or implementation-mirroring test. However, the standalone message serialization test is not relevant enough to establish the production wire contract because it substitutes a differently configured mapper.

`git diff --check 46bfcfb 0a3b2d1` also passed. An initial concurrent focused Gradle attempt failed only while writing its XML result file; it was not used as evidence. The isolated rerun above succeeded and generated the listed XML artifacts.

## Required skill-perspective check

Ran both required perspectives before assessing test relevance and maintainability.

- `remove-ai-slops`: **VIOLATION.** The mapper test gives false confidence: it verifies a hand-configured serialization path that production does not use. This is a HIGH finding because live evidence proves the result violates the Kafka wire contract. No deletion-only test, tautology, needless parsing/normalization, speculative abstraction, or oversized changed module was found.
- `programming`: **VIOLATION.** This skill has no Java-specific reference, so its general strictness, boundary ownership, test-contract, and needless-abstraction criteria were applied. The serialization test does not test the actual boundary configuration, leaving the production wire contract broken. No untyped escape hatch, brittle prose/prompt assertion, redundant interior validation, or needless abstraction was found.

## Evidence inspected

- `.omo/evidence/task-07-kafka-producer/fix-send-timeout.txt` — prior live Kafka-down result: HTTP 503 in 3.150482 seconds.
- `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md:16` — actual Kafka record with numeric `occurredAt`, the basis for the HIGH finding.
- `build/test-results/test/TEST-com.example.charging.application.ChargingEventPublisherContractTest.xml`
- `build/test-results/test/TEST-com.example.charging.controller.ChargingEventControllerTest.xml`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventMessageTest.xml`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventProducerTest.xml`

The repository worktree contains pre-existing/unrelated `.omo` changes. They were not treated as product changes and were preserved.
