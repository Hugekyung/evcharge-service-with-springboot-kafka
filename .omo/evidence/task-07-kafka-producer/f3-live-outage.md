# F3 live outage evidence

Date: 2026-08-14 Asia/Seoul

Surface/invocation:

```text
docker compose stop kafka
curl -i --max-time 5 -X POST http://localhost:18087/api/v1/charging-events -H 'Content-Type: application/json' --data '{"eventId":"f3-manual-outage-20260814-1822","chargerId":"charger-f3-outage","sessionId":"session-f3-outage-20260814-1822","eventType":"CHARGING_STARTED","sequence":1,"batteryLevel":35,"chargedKwh":0,"occurredAt":"2026-08-14T12:01:00+09:00"}'
docker compose start kafka
```

Observed:

```text
HTTP/1.1 503
Content-Length: 0
real 3.02
CURL_EXIT=0
evcharging-kafka restored Up (healthy)
evcharging-postgres Up (healthy)
```

The failed publish returned a 5xx in about 3.02 seconds, below the 3.5-second bound. Kafka was restored without deleting volumes.
