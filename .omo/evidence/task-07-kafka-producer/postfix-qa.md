# Task 7 postfix manual QA

## Run binding

- Exact HEAD: `96893e420d3e963a816a3d2c986454c1403a8f20`
- Product changes: none. Existing worktree changes were preserved.
- Infrastructure precondition: `docker compose ps --all` showed Kafka and PostgreSQL healthy before the happy path.
- App surface: `java -jar build/libs/evcharging-0.0.1-SNAPSHOT.jar --server.port=18089`

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| POSTFIX-S1 | Task 7: successful broker publish returns 202 and publishes to `charging-events` with `sessionId` key | HTTP API -> Kafka | Start jar on `18089`; `docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --group task7-postfix-qa-proof-202608141841 --from-beginning --timeout-ms 5000 --max-messages 100 --property print.key=true --property print.value=true`; `curl -sS -i --max-time 8 -X POST http://localhost:18089/api/v1/charging-events` with fresh event/session IDs and `occurredAt=2026-08-14T12:34:56+09:00` | PASS | AR-HAPPY-HTTP, AR-KAFKA |
| POSTFIX-S2 | Task 7: broker publish failure maps to bounded HTTP 5xx | HTTP API with Kafka stopped | `docker compose stop kafka`; `curl -sS -i --max-time 8 -X POST http://localhost:18089/api/v1/charging-events` with fresh outage IDs; `docker compose start kafka`; wait for healthy | PASS | AR-OUTAGE-HTTP, AR-OUTAGE-META, AR-CLEANUP |

Observed happy record: key `task7-postfix-session`; payload matched the request. `occurredAt` was textual ISO-8601 (`"2026-08-14T03:34:56Z"`), not a numeric epoch.

Observed outage: HTTP `503`, curl exit `0`, elapsed `3` seconds, and no `202`.

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| POSTFIX-A1 | Task 7 success | stale_state | Fresh event ID, session ID, consumer group, and port must be used; only the exact fresh key/payload counts. | PASS | AR-HAPPY-HTTP, AR-KAFKA |
| POSTFIX-A2 | Task 7 success | misleading-success-output | A 202 is valid only when the same fresh event is visible in Kafka with the exact session key and textual ISO timestamp. | PASS | AR-HAPPY-HTTP, AR-KAFKA |
| POSTFIX-A3 | Task 7 failure | failed_publish | With Kafka stopped, valid publish must return 503 within the producer bound. | PASS | AR-OUTAGE-HTTP, AR-OUTAGE-META |
| POSTFIX-A4 | Verification hygiene | hung_or_long | HTTP and Kafka consumer commands use explicit timeouts; outage completed in 3 seconds. | PASS | AR-HAPPY-HTTP, AR-KAFKA, AR-OUTAGE-META |
| POSTFIX-A5 | Verification hygiene | destructive_cleanup | Cleanup may stop/start only the Kafka service; no compose down or volume deletion. | PASS | AR-CLEANUP |
| POSTFIX-A6 | Verification hygiene | dirty_worktree | Existing unrelated worktree changes must remain untouched. | PASS | AR-CLEANUP |
| POSTFIX-A7 | Task 7 scope | malformed_input | Not applicable: this postfix run targets successful publication and Kafka outage; controller validation is outside this producer criterion and covered by prior focused evidence. | NOT_APPLICABLE | AR-HAPPY-HTTP |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| AR-HAPPY-HTTP | HTTP capture | Raw `curl -i` response showing HTTP 202 and empty body | `.omo/evidence/task-07-kafka-producer/postfix-qa-happy-http.txt` |
| AR-KAFKA | Kafka capture | Fresh record showing exact `sessionId` key, payload, and textual ISO `occurredAt` | `.omo/evidence/task-07-kafka-producer/postfix-qa-kafka-record.txt` |
| AR-OUTAGE-HTTP | HTTP capture | Raw `curl -i` response with Kafka stopped showing HTTP 503 | `.omo/evidence/task-07-kafka-producer/postfix-qa-outage-http.txt` |
| AR-OUTAGE-META | timing capture | Curl exit code 0 and 3-second bounded outage elapsed time | `.omo/evidence/task-07-kafka-producer/postfix-qa-outage-meta.txt` |
| AR-CLEANUP | cleanup receipt | Exact HEAD, healthy Compose services, no QA app listener, no QA consumer | `.omo/evidence/task-07-kafka-producer/postfix-qa-cleanup.txt` |

## DoneClaim

```yaml
task: task-07-kafka-producer
subtask: postfix final hands-on QA
head: 96893e420d3e963a816a3d2c986454c1403a8f20
verdict: PASS
product_changes: none
evidence: [AR-HAPPY-HTTP, AR-KAFKA, AR-OUTAGE-HTTP, AR-OUTAGE-META, AR-CLEANUP]
summary: Fresh live POST returned 202 and the exact Kafka record used the sessionId key with textual ISO occurredAt; Kafka-down POST returned 503 in 3 seconds; Kafka/PostgreSQL were restored healthy and owned processes were cleaned up.
```
