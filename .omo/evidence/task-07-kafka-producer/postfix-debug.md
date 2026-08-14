# TASK-07 producer runtime postfix debug ledger

- HEAD verified before execution: `96893e420d3e963a816a3d2c986454c1403a8f20`
- Surface: HTTP `POST /api/v1/charging-events`; broker observation through Kafka console consumer.
- Runtime: `./gradlew bootRun`, PostgreSQL and Kafka from `docker compose`.
- Cleanup: API process stopped; Kafka restarted and healthy; PostgreSQL remained healthy.

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| S1 | TASK-07 completion: POST -> Kafka `charging-events`; sessionId key | HTTP + Kafka | `curl -i --max-time 8 -H 'Content-Type: application/json' -d '{...eventId=qa-debug-success-20260814-1838...,occurredAt=2026-08-14T18:38:00+09:00}' http://localhost:8080/api/v1/charging-events`; then `docker exec evcharging-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --from-beginning --timeout-ms 10000 --max-messages 5` | PASS | A1, A2 |
| S2 | ISO timestamp serialization | Kafka payload | Same Kafka console-consumer invocation as S1; inspect record `qa-debug-success-20260814-1838` | PASS | A2 |
| S3 | TASK-07 failure rule: broker publish failure returns 5xx | HTTP + broker outage | `docker compose stop kafka`; `curl -i --max-time 8 -H 'Content-Type: application/json' -d '{...eventId=qa-debug-outage-20260814-1839...,occurredAt=2026-08-14T18:39:00+09:00}' http://localhost:8080/api/v1/charging-events`; `docker compose start kafka` | PASS | A3 |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| A-S1 | POST must not acknowledge before broker publish succeeds | broker unavailable | Return bounded 5xx, not 202 | PASS | A3 |
| A-S2 | Kafka payload contract | non-UTC input offset / timestamp shape | Preserve instant and serialize `occurredAt` as ISO-8601 text | PASS | A1, A2 |
| A-S3 | Producer reliability | request timeout bound | Outage request completes around 3 seconds, below `curl --max-time 8`; no hang | PASS | A3 |

## Evidence artifacts

### A1 — successful HTTP publish

```text
HEAD 96893e420d3e963a816a3d2c986454c1403a8f20
POST payload eventId=qa-debug-success-20260814-1838 sessionId=qa-session-01 occurredAt=2026-08-14T18:38:00+09:00
HTTP/1.1 202
Content-Length: 0
```

### A2 — Kafka record

```text
topic=charging-events
record={"eventId":"qa-debug-success-20260814-1838","chargerId":"qa-charger-01","sessionId":"qa-session-01","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":"2026-08-14T09:38:00Z"}
key=sessionId=qa-session-01 (producer invocation used KafkaTemplate.send(TOPIC, command.sessionId(), ...))
```

### A3 — broker outage

```text
docker compose stop kafka -> Container evcharging-kafka Stopped
HTTP/1.1 503
Content-Length: 0
Connection: close
curl transfer completed at about 3 seconds (client cap was 8 seconds)
docker compose start kafka -> healthy
```

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| A1 | http-transcript | Live broker-success POST response | `.omo/evidence/task-07-kafka-producer/postfix-debug.md#A1` |
| A2 | kafka-transcript | Live `charging-events` record and ISO timestamp | `.omo/evidence/task-07-kafka-producer/postfix-debug.md#A2` |
| A3 | http-transcript | Live broker-stop POST response and bounded completion | `.omo/evidence/task-07-kafka-producer/postfix-debug.md#A3` |

