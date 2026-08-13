# Task 5 global QA runtime evidence

SHA-bound source under review:

- `HEAD`: `d34226f87b36490f85b514208aa13e05a05b87b1`
- `ChargingSessionRepository.java` SHA-1: `97b6e5ba2ac504e5f362b6481836d60953e777b2`
- `ChargingEventRepository.java` SHA-1: `35faaf3138f19dd2e5280204cc7f15a9e0329140`

## Preflight

Exact invocation: `docker compose ps --format 'table {{.Name}}\\t{{.Service}}\\t{{.State}}\\t{{.Status}}'`; `ps -axo pid=,command= | rg '[c]om.example.charging.ChargingApplication' || true`.

Observed: `evcharging-kafka` and `evcharging-postgres` were `running` and `healthy`; no existing `ChargingApplication` process was listed.

## Bounded bootRun

Exact invocation: `./gradlew bootRun --args='--server.port=0'`.

Interactive terminal session was stopped with one controlled Ctrl-C after the startup marker. Positive markers observed in the raw terminal transcript:

```text
Finished Spring Data repository scanning ... Found 2 JPA repository interfaces.
Database: jdbc:postgresql://localhost:5432/evcharging (PostgreSQL 16.14)
Successfully validated 1 migration
Schema "public" is up to date. No migration necessary.
Initialized JPA EntityManagerFactory for persistence unit 'default'
Started ChargingApplication in 1.607 seconds
```

No `QueryCreationException`, `BeanCreationException`, or startup failure appeared. Follow-up process scan returned no `ChargingApplication` process.

## Gradle verification

Exact invocation: `./gradlew test`.

Observed result:

```text
> Task :test NO-SOURCE
BUILD SUCCESSFUL in 585ms
```

This is a build/task-success signal only; Gradle reports no test sources, so it is not behavioral test coverage.

## Postflight

Exact invocation: `ps -axo pid=,command= | rg '[c]om.example.charging.ChargingApplication' || true`; `docker compose ps --format 'table {{.Name}}\\t{{.Service}}\\t{{.State}}\\t{{.Status}}'`.

Observed: no application process; PostgreSQL and Kafka remained running and healthy. No containers, volumes, databases, or product files were removed or modified.
