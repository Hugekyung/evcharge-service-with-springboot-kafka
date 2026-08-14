# Task 7 Postfix Security Review

## recommendation

PASS / APPROVE

## Review binding and SHA ledger

- Exact Git HEAD: `96893e420d3e963a816a3d2c986454c1403a8f20`
- HEAD tree: `7b4788c4edfe56ca1a8b422eaafc0adb21c90cdc`
- Review scope: `46bfcfbbaa52e0c458668271f932ce420116b900..96893e420d3e963a816a3d2c986454c1403a8f20`

| Reviewed path | SHA-256 |
| --- | --- |
| `docs/TASK.md` | `2467d1b7b396540c37ddc7f6942529dab511f65f262b20c4d78e71d198b411c6` |
| `src/main/java/com/example/charging/kafka/ChargingEventMessage.java` | `98ef1bee96c4d70fc9b68b857cf12a570b3abc22bfee87be11631e64f60d1420` |
| `src/main/java/com/example/charging/kafka/ChargingEventProducer.java` | `3971cc79fcc7108798926f23bd973f0b974706e8a9f1da9061c9c8f1329ce6be` |
| `src/main/resources/application.yml` | `96dce306ce042878e99f344a1c6502f998f437033dcf4ed997141b0fe078a5b3` |
| `src/test/java/com/example/charging/kafka/ChargingEventMessageTest.java` | `6e3db0350c80f56fa728191c391812456c0990ef630dd436dca3e05cb6805407` |
| `src/test/java/com/example/charging/kafka/ChargingEventProducerTest.java` | `18517206bda252a620d6592570286052215fbfbd12c60aac5a3b38ea8f7e553c` |

## originalIntent

Implement Task 7's Kafka producer boundary: publish the documented event to `charging-events` using `sessionId` as key, return only after broker acknowledgement, and convert publish failures or timeout into the existing safe HTTP 5xx path.

## desiredOutcome

The producer emits the documented JSON contract, including textual ISO-8601 `occurredAt`; exposes no secrets or broker internals; uses no unsafe retry loop; and preserves bounded, typed failure handling.

## userOutcomeReview

PASS. Exact-HEAD inspection confirms the timestamp postfix is placed on the transport DTO boundary via `@JsonFormat(shape = STRING)`. A fresh test of Spring Kafka's actual `JsonSerializer` proves `occurredAt` is textual and equals `2026-08-12T03:00:00Z`. Producer acknowledgement, timeout, execution failure, synchronous failure, and interruption behavior remain unchanged.

## Security and safety review

- PASS — fixed topic and `sessionId` key; no user-controlled topic selection, random key, producer retry loop, `Thread.sleep`, database access, or consumer behavior.
- PASS — send and acknowledgement share a three-second monotonic deadline; `max.block.ms: 3000` bounds Kafka's synchronous metadata/buffer wait.
- PASS — interruption restores the interrupt flag. Execution, timeout, interruption, and runtime failures become `ChargingEventPublishException`; exception details are not returned in the HTTP response.
- PASS — no new logging, payload logging, stack-trace printing, token, API key, private key, authorization value, or remote credential in the changed surface. The existing `evcharging` datasource password is an unchanged local Compose PoC value.
- PASS — `@JsonFormat` changes only the wire representation of the required timestamp. It performs no parsing, normalization, reflection enablement, polymorphic typing, or trust-boundary widening.
- PASS — consumer trusted packages remain limited to `com.example.charging`; the postfix does not broaden deserialization trust.

## Direct remove-ai-slops and programming pass

- PASS — the new serializer regression is contract-facing: it invokes Spring Kafka's production serializer class and would fail if the annotation were removed. It is not deletion-only, removal-only, tautological, prose-pinning, or implementation-mirroring false confidence.
- PASS — no needless extraction, duplicate validation, dead code, broad new catch, normalization layer, speculative abstraction, or source file over 250 pure LOC. Reviewed pure LOC: message 16, producer 53, message test 71, producer test 156.
- PASS — immutable typed DTO, constructor injection, explicit mapping, typed errors, and boundary-local serialization remain maintainable and scoped. The programming skill has no Java reference, so its shared type/boundary/test/slop criteria were applied directly.
- PASS — the existing code-quality report explicitly identified the prior detached-mapper false-confidence test and required an actual Kafka serializer assertion. Exact HEAD now supplies that assertion; this review independently reproduced it.

## Verification

- `git diff --check 46bfcfbbaa52e0c458668271f932ce420116b900..96893e4` — PASS.
- Fresh `./gradlew test --tests 'com.example.charging.kafka.ChargingEventMessageTest' --tests 'com.example.charging.kafka.ChargingEventProducerTest' --rerun-tasks --no-daemon` — PASS, 8 tests, 0 skipped/failures/errors.
- JUnit XML: `ChargingEventMessageTest` 2/2 and `ChargingEventProducerTest` 6/6.
- Static/security scanner — N/A; none is configured. Direct secret, logging, deserialization-trust, failure-path, unsafe-operation, and retry scans found no new issue.
- One prior concurrent run failed only while writing the shared JUnit XML. Immediate isolated rerun passed; no test assertion or product execution failed.

## blockers

None.

## checkedArtifactPaths

- `AGENTS.md`
- `docs/PRD.md`
- `docs/TASK.md`
- `.omo/plans/task-07-kafka-producer.md`
- `.omo/evidence/task-07-kafka-producer/global-code-review.md`
- `.omo/evidence/task-07-kafka-producer/global-security-review.md`
- `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md`
- All six SHA-ledger paths above
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventMessageTest.xml`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventProducerTest.xml`

## exactEvidenceGaps

- No fresh real-broker smoke was required for this security postfix review. The exact production serializer was exercised directly, while the earlier real-broker artifact establishes the old numeric behavior that this regression prevents.
- No project security scanner is configured; therefore scanner evidence is unavailable and marked N/A rather than inferred.

## cleanup

Read-only product review. No product code, Git state, service, process, database, or volume was changed. Only this required evidence report was added.
