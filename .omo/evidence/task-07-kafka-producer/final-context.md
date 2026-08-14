# Task 7 Final Context / History Recheck

## recommendation

**REJECT**

## reviewedHead

- Exact commit: `0a3b2d1f9747756dea647e7773dd80ab8cc208f6`
- Branch: `feature/task-07-kafka-producer`
- Merge base with `main`: `46bfcfbbaa52e0c458668271f932ce420116b900`
- Current `main`: `46bfcfbbaa52e0c458668271f932ce420116b900`
- Commit subject: `feat: implement Kafka event producer`

## originalIntent

Implement Task 7 only: add a dedicated immutable Kafka event message and the sole production `ChargingEventPublisher`; publish once to `charging-events` with `sessionId` as the message key; keep the existing Controller path; return `202` only after broker acknowledgement; convert publish failure, timeout, and interruption into the existing `5xx` path. Do not add consumer, DB, state, idempotency, ordering, retry/DLT, or unrelated infrastructure work.

## desiredOutcome

A valid `POST /api/v1/charging-events` produces the documented eight-field Kafka JSON record under the request's `sessionId` key. Acknowledged sends return `202`; unavailable or timed-out publication returns bounded `5xx`. Task 7 alone is marked complete after focused tests and real broker verification.

## contextAndHistoryReview

FAIL. The exact HEAD is one coherent Task 7 commit directly on the current `main`, and its six-file product diff matches the task boundary. However, the prior live Kafka capture contradicts the required payload contract and the detached serialization unit test.

The source-of-truth documents agree:

- `AGENTS.md` requires topic `charging-events`, key `sessionId`, broker-confirmed `202`, and `5xx` on publish failure/timeout. It also keeps Kafka Producer free of DB state changes and custom retry loops.
- `docs/PRD.md` repeats the HTTP-to-Kafka flow, broker acknowledgement rule, exact eight-field payload, and `sessionId` partitioning decision.
- `docs/TASK.md` defines the same Task 7 deliverables and now marks only Task 7 `[완료]`.
- The approved work plan preserves Task 6's existing Controller boundary, chooses a separate Kafka DTO, a three-second acknowledgement bound, one real Compose publication check, and explicitly excludes Task 8+ behavior.

The producer maps all eight command fields explicitly, sends to the exact topic with the exact key, restores the interrupt flag, wraps synchronous and asynchronous Kafka failures, and uses one monotonic deadline. `max.block.ms: 3000` closes the pre-future Kafka client blocking gap identified during adversarial review. But the production Kafka serializer does not match the test's separately constructed `ObjectMapper`: the live record serializes `occurredAt` as `1786676400.000000000`, while the PRD and Task 7 message acceptance criterion require the documented ISO-8601 string shape.

## userOutcomeReview

FAIL. Prior runtime evidence proves topic/key publication, acknowledgement, bounded outage handling, recovery, and cleanup. It also proves the payload mismatch: `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md` records `"occurredAt":1786676400.000000000` on the real Kafka surface. The unit test passes only because it creates its own `ObjectMapper` and disables timestamp serialization; it therefore does not verify the configured Spring Kafka `JsonSerializer`. Green JUnit XML and `git diff --check` do not cure this user-visible contract failure.

## directQualityRecheck

- `remove-ai-slops`: FAIL at criterion level. `ChargingEventMessageTest` uses a detached mapper configured differently from production, giving false confidence about the real Kafka JSON boundary. Other overfit/slop classes are clear.
- `programming`: FAIL at criterion level under its shared boundary/test criteria; no Java-specific route exists. The typed DTO is sound, but the test does not exercise the actual serializer configuration consumed by Kafka.
- `.omo/evidence/task-07-kafka-producer/global-code-review.md` independently reports the same blocker. Its ledger SHA field is stale (`main`), but its product hashes match the exact Task 7 files and its observation is directly corroborated by the exact-HEAD live QA artifact.

## blockers

1. `SC-PAYLOAD-TYPE` — Task 7 requires the dedicated message to retain the documented payload field types on the real Kafka surface. The real broker record emits numeric `occurredAt`, not the documented ISO-8601 string. Evidence: `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md` exact record; `docs/PRD.md:297-322`; `.omo/plans/task-07-kafka-producer.md` Todo 1 acceptance criterion.

## exactEvidenceGaps

Missing passing real-surface proof that `occurredAt` is serialized as the documented ISO-8601 string by Spring Kafka's configured `JsonSerializer`. The existing detached-`ObjectMapper` unit test is not that proof.

## checkedArtifactPaths

- `AGENTS.md`
- `docs/PRD.md`
- `docs/TASK.md`
- commit `0a3b2d1f9747756dea647e7773dd80ab8cc208f6` and its full six-file diff
- `.omo/plans/task-07-kafka-producer.md`
- `.omo/evidence/task-07-kafka-producer/global-goal-review.md`
- `.omo/evidence/task-07-kafka-producer/f1-plan-compliance-recheck.md`
- `.omo/evidence/task-07-kafka-producer/f2-code-quality.md`
- `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md`
- `.omo/evidence/task-07-kafka-producer/f4-scope.md`
- `.omo/evidence/task-07-kafka-producer/fix-send-timeout.txt`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventMessageTest.xml`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventProducerTest.xml`

## shaBoundPassLedger

Verdict binding: **REJECT** for exact HEAD `0a3b2d1f9747756dea647e7773dd80ab8cc208f6` on `feature/task-07-kafka-producer`, based on current source-of-truth and evidence contents below.

| Artifact | SHA-256 |
|---|---|
| `AGENTS.md` | `22f1d3f7856b3af01073ec94dbc6292c77fe00db26d3cea6634aa505b301158e` |
| `docs/PRD.md` | `31b45550c1086c160ae7d50429a870268bab64f6a0159530ebc49948c635b465` |
| `docs/TASK.md` | `2467d1b7b396540c37ddc7f6942529dab511f65f262b20c4d78e71d198b411c6` |
| `.omo/plans/task-07-kafka-producer.md` | `638cc3d3b1a6c2e25fadcc45690d2e4478881bea36e38882997f4ccb74b0462d` |
| `.omo/evidence/task-07-kafka-producer/global-goal-review.md` | `731e5371fc50b8de3614deabe071a89adc204c8d6949583eee1d47481f1a3b8b` |
| `.omo/evidence/task-07-kafka-producer/f1-plan-compliance-recheck.md` | `d53daf0cc48458595843ebd3653c5dad3bae221ddb25e6225d21b5c303fa711e` |
| `.omo/evidence/task-07-kafka-producer/f2-code-quality.md` | `fd9ec6037905acafdfee74f16ac4da0e0109686a6af93b93821522e984f9f98a` |
| `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md` | `932883b33d0fecb7a53e9d31a9dbdd120486c2be6f36f4481fab23fc94ae98b2` |
| `.omo/evidence/task-07-kafka-producer/f4-scope.md` | `cc8d737f2396c43176f6fe86dbadc3227d3c3812cce3b21e591d70dc45b960ba` |

Ledger result: **REJECT — real Kafka evidence contradicts the required `occurredAt` JSON type, and the unit test does not exercise production serialization.**
