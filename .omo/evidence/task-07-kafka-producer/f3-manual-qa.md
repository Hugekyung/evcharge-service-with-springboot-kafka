# Task 7 F3 — real manual QA

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| F3-S1 | Task 7: POST publishes to `charging-events` with `sessionId` key and acknowledges only after broker send | HTTP API → Kafka | `java -jar build/libs/evcharging-0.0.1-SNAPSHOT.jar --server.port=18087`; `docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --group task7-f3-20260814182713-43877 --from-beginning --timeout-ms 15000 --max-messages 100 --property print.key=true`; exact POST: `curl -i --max-time 8 -X POST http://localhost:18087/api/v1/charging-events -H 'Content-Type: application/json' --data '{"eventId":"task7-f3-20260814182713-43877","chargerId":"charger-task7","sessionId":"session-task7-f3-20260814182713-43877","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":"2026-08-14T12:00:00+09:00"}'` | PASS | `AR-bootjar`, `AR-live-transcript`, `AR-happy-http`, `AR-consumer` |
| F3-S2 | Task 7: broker publish failure/timeout maps to HTTP 5xx and is bounded | HTTP API with Kafka stopped | `docker compose stop kafka`; exact second POST with `--max-time 5` and unique outage IDs; `docker compose start kafka` in cleanup | PASS | `AR-live-transcript`, `AR-outage-http`, `AR-recovery` |
| F3-S3 | Task 7 focused verification | Gradle/JUnit | `./gradlew test --tests com.example.charging.kafka.ChargingEventProducerTest --tests com.example.charging.controller.ChargingEventControllerTest --rerun-tasks --no-daemon` | PASS | `AR-focused-log`, `AR-producer-xml`, `AR-controller-xml` |

Happy-path binary observables: HTTP `202`, curl exit `0`; Kafka consumer contained the exact record:

```text
session-task7-f3-20260814182713-43877	{"eventId":"task7-f3-20260814182713-43877","chargerId":"charger-task7","sessionId":"session-task7-f3-20260814182713-43877","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":1786676400.000000000}
```

Outage binary observables: HTTP `503`, curl exit `0`, elapsed `3` seconds by the bounded shell wall-clock measurement, and no `202`. The application log showed the Started line; the live log did not print `max.block.ms`, so configuration proof is the current `src/main/resources/application.yml` value `spring.kafka.producer.properties.max.block.ms: 3000` (not inferred from the response).

Focused XML counts: `ChargingEventProducerTest tests=6 skipped=0 failures=0 errors=0`; `ChargingEventControllerTest tests=16 skipped=0 failures=0 errors=0`.

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| F3-A1 | Task 7 success | stale_state | Fresh jar, fresh unique event/session IDs, fresh consumer group, and current healthy Compose services are used; only the exact current-run record is asserted. | PASS | `AR-live-transcript`, `AR-consumer`, `AR-recovery` |
| F3-A2 | Task 7 success | misleading-success-output | A `202` must be paired with the actual Kafka record, exact session key, and matching event payload. | PASS | `AR-happy-http`, `AR-consumer` |
| F3-A3 | Task 7 failure | failed_publish | With Kafka stopped, valid publish must return `503`, curl must exit `0`, and completion must be bounded near the 3-second producer deadline. | PASS | `AR-outage-http`, `AR-live-transcript` |
| F3-A4 | Verification hygiene | hung_or_long | Jar startup, consumer, and both curl calls use bounded polling/timeouts; outage completed in 3 seconds, below the approximately 4-second bound. | PASS | `AR-live-transcript`, `AR-outage-http` |
| F3-A5 | Verification hygiene | flaky_tests | Forced fresh focused Gradle run completed successfully; XML counts were inspected directly. | PASS | `AR-focused-log`, `AR-producer-xml`, `AR-controller-xml` |
| F3-A6 | Verification hygiene | repeated_interruptions | Cleanup trap terminated owned app/consumer; after two harness-side shell portability failures, a separate bounded recovery restarted Kafka and verified final state. | PASS | `AR-live-transcript`, `AR-recovery` |
| F3-A7 | Verification hygiene | dirty_worktree | Worktree was inventoried before QA; existing unrelated changes were preserved and no product files were edited. | PASS | `AR-worktree` |
| F3-A8 | Verification hygiene | destructive_cleanup | No `docker compose down`, volume deletion, or destructive filesystem operation was run; only `docker compose stop kafka`/`start kafka` was used. | PASS | `AR-live-transcript`, `AR-recovery` |
| F3-A9 | Task 7 scope | malformed_input | Not applicable: this run verifies producer publication and broker outage; controller validation is covered by the focused controller suite. | NOT_APPLICABLE | `AR-controller-xml` |
| F3-A10 | Task 7 scope | prompt_injection | Not applicable: no untrusted instruction channel or external content was used; request JSON and commands were fixed QA inputs. | NOT_APPLICABLE | `AR-live-transcript` |
| F3-A11 | Task 7 scope | cancel_resume | Not applicable: this is a one-shot local manual-QA request with no resumable user workflow. | NOT_APPLICABLE | `AR-live-transcript` |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| AR-bootjar | build log | Fresh `bootJar` build, exit 0 | `/tmp/task7-f3-bootjar.log` |
| AR-live-transcript | transcript | Single bounded live scenario, startup, happy POST, outage POST, and cleanup output | `/tmp/task7-f3-20260814182713-43877/transcript.txt` |
| AR-happy-http | HTTP capture | Raw `curl -i` happy response showing HTTP 202 | `/tmp/task7-f3-20260814182713-43877/happy-http.txt` |
| AR-consumer | Kafka capture | Raw console consumer output containing exact current-run key and JSON payload | `/tmp/task7-f3-20260814182713-43877/consumer.txt` |
| AR-outage-http | HTTP capture | Raw `curl -i` Kafka-down response showing HTTP 503 | `/tmp/task7-f3-20260814182713-43877/outage-http.txt` |
| AR-app-log | application log | Fresh app Started log and live runtime observables | `/tmp/task7-f3-20260814182713-43877/app.log` |
| AR-recovery | cleanup receipt | Kafka restarted healthy; PostgreSQL healthy; port 18087 closed; no owned app/consumer remained | `/tmp/task7-f3-recovery-final.txt` |
| AR-focused-log | test log | Fresh forced producer/controller test run, exit 0 | `/tmp/task7-f3-focused.txt` |
| AR-producer-xml | JUnit XML | Producer suite: 6 tests, zero skipped/failures/errors | `/Users/yanghaechan/orca/projects/evcharging/build/test-results/test/TEST-com.example.charging.kafka.ChargingEventProducerTest.xml` |
| AR-controller-xml | JUnit XML | Controller suite: 16 tests, zero skipped/failures/errors | `/Users/yanghaechan/orca/projects/evcharging/build/test-results/test/TEST-com.example.charging.controller.ChargingEventControllerTest.xml` |
| AR-worktree | inventory | Pre-run dirty-worktree inventory; changes preserved | `/tmp/task7-f3-worktree.txt` |

### cleanupReceipt

The owned Java process and console consumer were terminated. Kafka was restarted and reached `healthy`; PostgreSQL remained `healthy`; TCP port `18087` was closed; no owned app or consumer process remained. No Compose volumes were touched. The first shell attempt exposed a date-format portability error and the second exposed a zsh reserved-variable name; both were recovered without product changes, and the final recovery check is recorded in `AR-recovery`.

## DoneClaim

```yaml
task: task-07-kafka-producer
subtask: F3 real manual QA
verdict: PASS
product_changes: none
evidence: [AR-bootjar, AR-live-transcript, AR-happy-http, AR-consumer, AR-outage-http, AR-recovery, AR-focused-log, AR-producer-xml, AR-controller-xml]
summary: HTTP 202 was paired with the exact Kafka sessionId key and payload; Kafka-down publish returned HTTP 503 in 3 seconds; focused producer/controller tests passed 6/6 and 16/16; Compose services and owned-process cleanup were verified.
```
