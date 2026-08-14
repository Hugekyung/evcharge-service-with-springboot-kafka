# Task 7 Todo 4 — Compose smoke evidence

## Verdict

PASS. Existing Compose Kafka and PostgreSQL were healthy. A fresh Spring Boot app started on fixed port `18087`; the exact POST returned `202 Accepted`; and a fresh Kafka console consumer observed the matching `sessionId` key and JSON event. Total bounded run time was 22 seconds (under the 90-second cap). The app and consumer were cleaned up; Compose services were left running and healthy.

## Surface evidence (`manualQa.surfaceEvidence`)

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| T7-T4-HAPPY | Task 7 Todo 4; `docker-compose.yml` | Docker Compose service state | `docker compose ps --all` | PASS — Kafka and PostgreSQL both `Up (healthy)` before and after the run | `a1`, `a6` |
| T7-T4-HAPPY | Task 7 Todo 4; API publish path | Spring Boot HTTP API | `./gradlew bootRun --args="--server.port=18087"`; then `curl -sS -i --max-time 8 -X POST http://localhost:18087/api/v1/charging-events -H "Content-Type: application/json" --data '{"eventId":"task7-t4-20260814180746-32493","chargerId":"charger-task7-t4","sessionId":"session-task7-t4-20260814180746-32493","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":"2026-08-14T12:00:00+09:00"}'` | PASS — `HTTP/1.1 202`, `Content-Length: 0` | `a2`, `a3` |
| T7-T4-HAPPY | Task 7 Todo 4; Kafka publication/key | Kafka console consumer in Compose | `/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --group task7-t4-20260814180746-32493 --from-beginning --timeout-ms 15000 --max-messages 100 --property print.key=true` via `docker compose exec -T kafka` (started before POST) | PASS — observed exact key `session-task7-t4-20260814180746-32493` and exact event ID in JSON | `a4` |

## Adversarial cases (`manualQa.adversarialCases`)

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| T7-T4-ADV-1 | Task 7 Todo 4 | stale_state | Verify current broker/app state immediately before exercising the path. | PASS — fresh `docker compose ps --all`, fresh app PID, fresh consumer group, and unique IDs. | `a1`, `a2`, `a4` |
| T7-T4-ADV-2 | Task 7 Todo 4 | misleading_success_output | HTTP success must be paired with an actual Kafka record. | PASS — `202` was paired with the consumer record containing the exact request key and event ID. | `a3`, `a4` |
| T7-T4-ADV-3 | Task 7 Todo 4 | hung_or_long_commands | Bounded consumer and app run must terminate within the smoke limit. | PASS — consumer used `--timeout-ms 15000`; complete run elapsed 22 seconds; app was stopped by cleanup. | `a4`, `a5` |
| T7-T4-ADV-4 | Task 7 Todo 4 | cleanup_leak | No app or consumer process/listener may remain after the run. | PASS — final port/process check had no `18087` listener and no matching app/consumer process; Compose services remained healthy. | `a5`, `a6` |

## Captured evidence

### a1 — Compose precondition

Exact invocation: `docker compose ps --all`

```text
evcharging-kafka      apache/kafka:3.9.0   Up 5 minutes (healthy)   0.0.0.0:9092->9092/tcp
evcharging-postgres   postgres:16-alpine   Up 42 hours (healthy)    0.0.0.0:5432->5432/tcp
```

### a2 — Application startup

Exact invocation: `./gradlew bootRun --args="--server.port=18087"`.

```text
Tomcat started on port 18087 (http) with context path '/'
Started ChargingApplication in 1.425 seconds (process running for 1.527)
Database: jdbc:postgresql://localhost:5432/evcharging (PostgreSQL 16.14)
Schema "public" is up to date. No migration necessary.
```

### a3 — HTTP publication response

Unique values: `eventId=task7-t4-20260814180746-32493`, `sessionId=session-task7-t4-20260814180746-32493`.

```text
HTTP/1.1 202
Content-Length: 0
Date: Fri, 14 Aug 2026 09:07:51 GMT
```

### a4 — Consumer invocation and observed record

Consumer exact invocation:

```text
/opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --group task7-t4-20260814180746-32493 --from-beginning --timeout-ms 15000 --max-messages 100 --property print.key=true
```

Observed matching record:

```text
session-task7-t4-20260814180746-32493	{"eventId":"task7-t4-20260814180746-32493","chargerId":"charger-task7-t4","sessionId":"session-task7-t4-20260814180746-32493","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":1786676400.000000000}
Processed a total of 3 messages
```

The other two records were prior retained topic records; the fresh unique record is present with the exact matching key and event ID.

### a5 — Bounded execution and cleanup

Run summary: `RUN_ID=20260814180746-32493`, `READY=1`, `ELAPSED=22s`. Consumer bounded itself with `--timeout-ms 15000`; app and consumer were terminated in the shell cleanup trap.

### a6 — Final state

Exact invocations: `lsof -nP -iTCP:18087 -sTCP:LISTEN`; `pgrep -af "ChargingApplication|bootRun|kafka-console-consumer"`; `docker compose ps --all`.

```text
--- listener
(no output)
--- compose
evcharging-kafka      apache/kafka:3.9.0   Up 7 minutes (healthy)   0.0.0.0:9092->9092/tcp
evcharging-postgres   postgres:16-alpine   Up 42 hours (healthy)    0.0.0.0:5432->5432/tcp
```

No `docker compose down`, volume deletion, or Compose-file modification was performed.

## Artifact refs (`manualQa.artifactRefs`)

| id | kind | description | path |
|---|---|---|---|
| a1 | terminal transcript | Fresh Compose health precondition | `.omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.md` |
| a2 | terminal transcript | Fixed-port Spring Boot startup and Flyway readiness | `.omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.md` |
| a3 | HTTP transcript | Exact POST and `202 Accepted` response | `.omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.md` |
| a4 | CLI transcript | Exact consumer invocation and matching Kafka key/payload | `.omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.md` |
| a5 | execution receipt | 22-second bounded run and cleanup trap | `.omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.md` |
| a6 | terminal transcript | Final no-listener check and healthy Compose state | `.omo/evidence/task-07-kafka-producer/todo-4-compose-smoke.md` |
