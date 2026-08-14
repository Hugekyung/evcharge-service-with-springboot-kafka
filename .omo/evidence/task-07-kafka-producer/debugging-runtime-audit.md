# Task 7 debugging runtime audit

<verdict>PASS</verdict>

Commit under audit: `0a3b2d1f9747756dea647e7773dd80ab8cc208f6` (verified by `git rev-parse HEAD`). This was a read-only product audit. No product files were edited.

## Runtime conclusion

The Task 7 producer path is fixed and bounded at runtime. With Kafka healthy, the real HTTP API returned `202` and the exact event was observed in `charging-events`. With Kafka stopped after application startup, the same valid POST returned `503` in `3.02s` (below the 3-second producer deadline plus small HTTP overhead), not an unbounded hang or false `202`. The exact-HEAD source/config evidence records the monotonic deadline, bounded `get(...)`, and `max.block.ms: 3000`.

## Hypothesis results

- H1 confirmed: broker-up publish acknowledged and returned `202`; the exact `qa-task7-success-001` record was present in Kafka.
- H2 confirmed: broker-down publish returned `503` after `real 3.02` seconds; the application logged the publish failure for the exact event/session/charger identifiers, and Kafka client logs showed broker disconnects.
- H3 refuted: exact HEAD started against the configured PostgreSQL/Kafka endpoints; topic `charging-events` existed and the current-run record was readable. The initially backgrounded boot process was lost by the shell harness, so the audit was rerun with a live PTY-held `./gradlew bootRun`; this was a harness issue, not a product result.

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| DBG-S1 | Task 7: POST publishes to `charging-events` and returns 202 only after broker acknowledgement | HTTP API → Kafka | Live `./gradlew bootRun` held in PTY; `curl -i -sS -X POST http://localhost:8080/api/v1/charging-events -H 'Content-Type: application/json' --data-binary @/tmp/task7-success.json`; `docker exec evcharging-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --from-beginning --timeout-ms 5000` | PASS | AR-success-http, AR-success-topic, AR-config |
| DBG-S2 | Task 7: broker publish failure/timeout maps to bounded HTTP 5xx | HTTP API with Kafka stopped | `docker compose stop kafka`; `curl --max-time 10 -i -sS -X POST http://localhost:8080/api/v1/charging-events -H 'Content-Type: application/json' --data-binary @/tmp/task7-outage.json`; `docker compose start kafka` | PASS | AR-outage-http, AR-cleanup, AR-config |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| DBG-A1 | Task 7 success | stale_state | Fresh unique event/session IDs must produce and assert the current-run Kafka record, not an older record. | PASS | AR-success-http, AR-success-topic |
| DBG-A2 | Task 7 success | misleading-success-output | HTTP `202` must be paired with the actual Kafka payload; an empty success response alone is insufficient. | PASS | AR-success-http, AR-success-topic |
| DBG-A3 | Task 7 failure | failed_publish | Kafka stopped must produce HTTP `5xx`, no `202`, and a bounded completion. | PASS | AR-outage-http, AR-config |
| DBG-A4 | Task 7 failure | hung_or_long | Outage request must complete within the explicit client bound and near the configured producer deadline. | PASS | AR-outage-http |
| DBG-A5 | Verification hygiene | dirty_worktree | Pre-existing `.omo` changes are preserved; this audit writes only evidence artifacts. | PASS | AR-cleanup |
| DBG-A6 | Verification hygiene | repeated_interruptions | The live app is stopped after the outage and no app port remains; Kafka is restored healthy. | PASS | AR-cleanup |
| DBG-A7 | Verification hygiene | destructive_cleanup | Cleanup must stop only the owned app and Kafka service, preserve PostgreSQL and Compose volumes, and avoid `down -v`. | PASS | AR-cleanup |
| DBG-A8 | Task 7 scope | malformed_input | Not applicable: this audit targets broker publication and outage handling; request validation is a separate controller criterion. | NOT_APPLICABLE | AR-config |
| DBG-A9 | Task 7 scope | prompt_injection | Not applicable: all request JSON and shell commands were fixed local QA inputs; no instruction-bearing external content was consumed. | NOT_APPLICABLE | AR-cleanup |
| DBG-A10 | Task 7 scope | cancel_resume | Not applicable: this is a one-shot local HTTP publication flow with no resumable user workflow. | NOT_APPLICABLE | AR-cleanup |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| AR-success-http | HTTP capture | Raw `curl -i` broker-up response with HTTP 202, empty asynchronous body, and `real 0.22` seconds | `.omo/evidence/task-07-kafka-producer/runtime-success-curl.txt` |
| AR-success-topic | Kafka capture | Raw console-consumer output containing the exact `qa-task7-success-001` event on `charging-events` | `.omo/evidence/task-07-kafka-producer/runtime-success-topic.txt` |
| AR-outage-http | HTTP capture | Raw `curl -i` broker-down response with HTTP 503 and `real 3.02` seconds | `.omo/evidence/task-07-kafka-producer/runtime-broker-down-curl.txt` |
| AR-config | source/runtime evidence | Exact HEAD, producer deadline/get implementation, Kafka bootstrap, and `max.block.ms: 3000` | `.omo/evidence/task-07-kafka-producer/debugging-runtime-config.txt` |
| AR-cleanup | cleanup receipt | Kafka/PostgreSQL healthy, app port 8080 free, Kafka port restored, and exact HEAD after cleanup | `.omo/evidence/task-07-kafka-producer/debugging-runtime-cleanup.txt` |

## Cleanup receipt

Kafka was stopped only for DBG-S2 and then restarted; both Compose services were healthy at the final check. The live Spring process was interrupted after the test, port 8080 was free, no volumes were removed, and the exact audited HEAD remained unchanged. `tmux` was unavailable in this environment, so the application was held in a real PTY exec session and HTTP/Kafka artifacts were captured directly.
