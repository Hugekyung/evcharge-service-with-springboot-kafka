# Global Security / Safety Review — Task 7

## recommendation

PASS / APPROVE

## Review binding

- Git HEAD SHA: `46bfcfbbaa52e0c458668271f932ce420116b900`
- Git HEAD tree: `cfad67cdce47bb58cc9bf3300313fcf90d8ef91e`
- Task 7 product-surface SHA-256: `11377c8f1fa1e0cecafa3e299d7a9ed32c7c6313e5231c9bed2c28c3e2be0c81`
- Digest input: binary working-tree diffs for `docs/TASK.md` and `src/main/resources/application.yml`, followed by SHA-256 lines for the two new Kafka production files and two new Kafka test files, in report order.

## originalIntent

Implement the Task 7 Kafka producer only: a separate message DTO, one Spring publisher bean, `charging-events` topic, `sessionId` key, and broker-acknowledgement-gated HTTP behavior with a three-second failure bound.

## desiredOutcome

A safe producer boundary that publishes the documented payload, returns normally only after acknowledgement, maps send failure/timeout/interruption to the existing typed exception, and adds no secret exposure, unsafe logging, custom retry, consumer, database work, or destructive QA.

## userOutcomeReview

PASS. Direct source inspection, fresh focused tests, and the real happy/outage artifacts reproduce the desired outcome. The current product surface contains only the DTO, producer, focused tests, `max.block.ms: 3000`, and the verified Task 7 marker.

## Security and safety findings

### Secrets and unsafe logging

- PASS — no API key, token, authorization value, private key, remote credential, or new password appears in the Task 7 delta. The `evcharging` database password is an unchanged local PoC default.
- PASS — the producer logs no payload or exception. The existing controller warning logs only the required diagnostic identifiers (`eventId`, `sessionId`, `chargerId`, `eventType`, `sequence`), not `batteryLevel`, `chargedKwh`, request bodies, stack traces, broker details, or exception messages.
- PASS — the HTTP failure response is empty `503`; internal broker causes are not exposed to the client.

### Kafka failure handling

- PASS — exactly one `KafkaTemplate.send` uses fixed topic `charging-events` and `command.sessionId()` as key; there is no random key, retry loop, `while`, or `Thread.sleep` in the Task 7 producer.
- PASS — a monotonic deadline covers send plus acknowledgement, and Kafka `max.block.ms: 3000` bounds the pre-future metadata/buffer wait. `ExecutionException`, `TimeoutException`, synchronous runtime failure, and interruption become `ChargingEventPublishException`; interruption restores the thread flag.
- PASS — fresh focused execution passed message `1/1`, producer `6/6`, and controller `16/16`, zero failures/errors. The real outage artifact records HTTP `503` in about `3.02s`; the happy artifact pairs HTTP `202` with the exact Kafka key and payload.

### QA operational safety

- PASS — reviewed QA used bounded application startup, consumer timeout, curl timeout, and targeted Kafka `stop`/`start` for the outage scenario.
- PASS — no `docker compose down`, `down -v`, volume deletion, database drop/truncate/delete, destructive Git command, recursive deletion, or broad process kill appears in Task 7 evidence.
- PASS — cleanup evidence and current inspection show Kafka and PostgreSQL healthy, volumes preserved, port/app/consumer workloads gone. The process probe's lone match was the probe command itself.

### Direct remove-ai-slops / programming pass

- PASS — no deletion-only/requested-removal/prose-pin/tautological/implementation-derived test, useless production parsing or normalization, needless abstraction, dead code, oversized module, or scope drift found. The timeout regression distinguishes the previously false-success pre-future blocking case.
- PASS — typed immutable DTO, explicit boundary mapping, constructor injection, typed failure propagation, and minimal dependency-free implementation create no maintenance or safety burden.
- PASS — `.omo/evidence/task-07-kafka-producer/f2-code-quality.md` explicitly covers the same skill perspectives and overfit/slop classes; this review independently reproduced them.

## blockers

None.

## checkedArtifactPaths

- `AGENTS.md`
- `docs/PRD.md`
- `docs/TASK.md`
- `.omo/plans/task-07-kafka-producer.md`
- `.omo/start-work/ledger.jsonl`
- `src/main/java/com/example/charging/kafka/ChargingEventMessage.java`
- `src/main/java/com/example/charging/kafka/ChargingEventProducer.java`
- `src/main/resources/application.yml`
- `src/main/java/com/example/charging/controller/ChargingEventController.java`
- `src/test/java/com/example/charging/kafka/ChargingEventMessageTest.java`
- `src/test/java/com/example/charging/kafka/ChargingEventProducerTest.java`
- `.omo/evidence/task-07-kafka-producer/f1-plan-compliance-recheck.md`
- `.omo/evidence/task-07-kafka-producer/f2-code-quality.md`
- `.omo/evidence/task-07-kafka-producer/f3-manual-qa.md`
- `.omo/evidence/task-07-kafka-producer/f3-live-outage.md`
- `.omo/evidence/task-07-kafka-producer/f3-cleanup-receipt.md`
- `.omo/evidence/task-07-kafka-producer/f4-scope.md`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventMessageTest.xml`
- `build/test-results/test/TEST-com.example.charging.kafka.ChargingEventProducerTest.xml`
- `build/test-results/test/TEST-com.example.charging.controller.ChargingEventControllerTest.xml`

## exactEvidenceGaps

- Static/security scanner: N/A — no scanner is configured. Direct secret, logging, unsafe-operation, and failure-path scans cover the stated Task 7 safety criteria.
- Task 7 files remain uncommitted/untracked, so the HEAD SHA alone does not identify them. The product-surface digest above binds the exact reviewed working-tree contents.
- One fresh test attempt collided with another reviewer writing the same JUnit XML and failed only while writing results; an immediate isolated rerun passed all 23 focused tests. This is disclosed as shared-workspace evidence noise, not a product failure.

## Cleanup

Read-only product review. No product, Git state, Docker service, volume, database, or long-lived process mutation performed. Only this required report and its SHA-bound ledger entry were written.
