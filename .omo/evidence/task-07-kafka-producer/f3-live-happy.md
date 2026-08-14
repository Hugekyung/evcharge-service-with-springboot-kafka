# F3 live happy-path evidence

Date: 2026-08-14 Asia/Seoul

Surface/invocation:

```text
docker compose ps
java -jar build/libs/evcharging-0.0.1-SNAPSHOT.jar --server.port=18087
docker compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic charging-events --group f3-manual-20260814-1822 --from-beginning --property print.key=true --property print.value=true --timeout-ms 12000
curl -i -X POST http://localhost:18087/api/v1/charging-events -H 'Content-Type: application/json' --data '{"eventId":"f3-manual-20260814-1822-001","chargerId":"charger-f3-manual","sessionId":"session-f3-manual-20260814-1822","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":"2026-08-14T12:00:00+09:00"}'
```

Observed:

```text
evcharging-kafka Up (healthy), 0.0.0.0:9092->9092/tcp
evcharging-postgres Up (healthy), 0.0.0.0:5432->5432/tcp
HTTP/1.1 202
Content-Length: 0
Kafka consumer record:
session-f3-manual-20260814-1822	{"eventId":"f3-manual-20260814-1822-001","chargerId":"charger-f3-manual","sessionId":"session-f3-manual-20260814-1822","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":1786676400.000000000}
```

The record key equals sessionId and the payload fields equal the POST body.
