# Task 7 final manual QA

Verified against exact HEAD `0a3b2d1f9747756dea647e7773dd80ab8cc208f6` (`feat: implement Kafka event producer`). Product source was read-only during QA.

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| FINAL-S1 | Task 7 producer: successful broker publish returns 202 | HTTP API → Kafka | `java -jar build/libs/evcharging-0.0.1-SNAPSHOT.jar --server.port=18087`; fresh `docker compose exec -T kafka ... kafka-console-consumer.sh ... --group task7-final-20260814183205-49757 ... --property print.key=true`; `curl -sS -i --max-time 8 -X POST http://localhost:18087/api/v1/charging-events ...` | PASS | `FINAL-E1` |
| FINAL-S2 | Task 7 producer: Kafka key is sessionId and payload is forwarded | Kafka console consumer | `/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --group task7-final-20260814183205-49757 --from-beginning --timeout-ms 12000 --max-messages 100 --property print.key=true --property print.value=true` via `docker compose exec -T kafka` | PASS | `FINAL-E1` |
| FINAL-S3 | Task 7 producer: publish failure maps to bounded HTTP 5xx | HTTP API with Kafka stopped | `docker compose stop kafka`; valid `curl -sS -i --max-time 5 -X POST ...`; `docker compose start kafka` | PASS | `FINAL-E1` |
| FINAL-S4 | Task 7 verification hygiene: runtime cleanup and Compose preservation | Docker Compose/process surface | `docker compose ps --all`; `lsof -nP -iTCP:18087 -sTCP:LISTEN`; `pgrep -af 'ChargingApplication|evcharging-0.0.1-SNAPSHOT|kafka-console-consumer'` | PASS | `FINAL-E1` |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| FINAL-A1 | Task 7 success | stale_state | Fresh jar, unique event/session IDs, fresh consumer group, and healthy Compose precondition; assert only the fresh matching record. | PASS | `FINAL-E1` |
| FINAL-A2 | Task 7 success | misleading_success_output | A 202 is accepted only when the exact keyed Kafka record and payload are observed. | PASS | `FINAL-E1` |
| FINAL-A3 | Task 7 failure | failed_publish | With Kafka stopped, valid publish returns 5xx and does not claim 202; completion remains bounded. | PASS | `FINAL-E1` |
| FINAL-A4 | Verification hygiene | hung_or_long | Consumer and curl have explicit timeouts; outage response completes in 3024 ms. | PASS | `FINAL-E1` |
| FINAL-A5 | Verification hygiene | destructive_cleanup | Cleanup must preserve Compose volumes and restart Kafka healthy. | PASS | `FINAL-E1` |
| FINAL-A6 | Verification hygiene | cleanup_leak | No QA-owned app/consumer process or port-18087 listener remains after the run; unrelated shared processes are preserved. | PASS | `FINAL-E1` |
| FINAL-A7 | Task 7 scope | malformed_input | Not applicable: this final lane exercises producer publish and broker outage; malformed-input validation is outside the producer criterion and already covered by the focused controller suite. | NOT_APPLICABLE | `FINAL-E1` |
| FINAL-A8 | Task 7 scope | prompt_injection | Not applicable: no untrusted instruction or external content was part of this local QA input. | NOT_APPLICABLE | `FINAL-E1` |
| FINAL-A9 | Task 7 scope | cancel_resume | Not applicable: this is a one-shot local HTTP/Kafka probe with no resumable workflow. | NOT_APPLICABLE | `FINAL-E1` |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| FINAL-E1 | live QA transcript | Exact HEAD, Compose precondition/recovery, raw 202 response, exact Kafka key/payload, raw Kafka-down 503 response, 3024 ms bound, and final cleanup observables | `.omo/evidence/task-07-kafka-producer/final-live-evidence.md` |

## Verdict

PASS. The required producer behavior was exercised live at the requested SHA: successful publish returned 202 and produced the exact session-keyed Kafka record; Kafka-down publish returned 503 within 3024 ms; Compose services were restored healthy; no owned runtime process remained; no product changes or volume deletion occurred.
