# Task 7 final live QA evidence

- Verified HEAD: `0a3b2d1f9747756dea647e7773dd80ab8cc208f6`
- Run: `task7-final-20260814183205-49757`
- Product changes: none

## Preconditions and cleanup

Invocation: `docker compose ps --all`

Observed before and after the run: `evcharging-kafka` and `evcharging-postgres` both `Up (healthy)`. Kafka was restored with `docker compose start kafka`; no `docker compose down`, volume deletion, or volume reset was used.

Owned-process cleanup invocation: `kill 49766`; final checks were `lsof -nP -iTCP:18087 -sTCP:LISTEN`, `pgrep -af 'ChargingApplication|evcharging-0.0.1-SNAPSHOT|kafka-console-consumer'`, and `docker compose ps --all`.

Final observables: no listener on port `18087`; no QA-owned app or console consumer. A separate pre-existing Spring process on port `8080` was visible in the broad process probe and was preserved as unrelated shared state. Both Compose services were healthy.

## Happy path

Surface/invocation:

```text
java -jar build/libs/evcharging-0.0.1-SNAPSHOT.jar --server.port=18087
docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --group task7-final-20260814183205-49757 --from-beginning --timeout-ms 12000 --max-messages 100 --property print.key=true --property print.value=true
curl -sS -i --max-time 8 -X POST http://localhost:18087/api/v1/charging-events -H 'Content-Type: application/json' --data '{"eventId":"task7-final-20260814183205-49757-event","chargerId":"task7-final-20260814183205-49757-charger","sessionId":"task7-final-20260814183205-49757-session","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":"2026-08-14T12:00:00+09:00"}'
```

Raw response: `HTTP/1.1 202`, `Content-Length: 0`, curl exit `0`.

Matching Kafka record:

```text
task7-final-20260814183205-49757-session	{"eventId":"task7-final-20260814183205-49757-event","chargerId":"task7-final-20260814183205-49757-charger","sessionId":"task7-final-20260814183205-49757-session","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":1786676400.000000000}
```

## Kafka-down failure path

Surface/invocation:

```text
docker compose stop kafka
curl -sS -i --max-time 5 -X POST http://localhost:18087/api/v1/charging-events -H 'Content-Type: application/json' --data '{"eventId":"task7-final-20260814183205-49757-outage-event","chargerId":"task7-final-20260814183205-49757-charger","sessionId":"task7-final-20260814183205-49757-outage-session","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":"2026-08-14T12:01:00+09:00"}'
docker compose start kafka
```

Raw response: `HTTP/1.1 503`, curl exit `0`, measured elapsed time `3024 ms`. Kafka returned to healthy state.
