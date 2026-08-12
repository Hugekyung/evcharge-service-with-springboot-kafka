# Global QA runtime evidence — Task 4 domain implementation

Date: 2026-08-13 (Asia/Seoul)
Branch: `feature/task-04-domain`
HEAD: `5eb94a4c4f3d740f4108f507dbe5a1dc7a8337ff`
Worktree status hash (`git status --porcelain=v1 | shasum -a 256`): `534070b31f98c201b502f2076ceac484cd02d33cf306591e02f0cbd6ba2b3c47`

## Surface 1 — Compose infrastructure

Exact invocation: `docker compose up -d && docker compose ps && docker volume inspect evcharging_postgres-data evcharging_kafka-data --format '{{.Name}} {{.Mountpoint}}'`

Observed: `evcharging-postgres` and `evcharging-kafka` were already running and healthy. The named volumes remained present at `/var/lib/docker/volumes/evcharging_postgres-data/_data` and `/var/lib/docker/volumes/evcharging_kafka-data/_data`.

## Surface 2 — Spring Boot startup and schema validation

Exact invocation: `./gradlew bootRun --args='--server.port=0'`

Observed startup evidence:

- Java 21.0.12 and Spring Boot 3.5.5 started `ChargingApplication`.
- Flyway: `Successfully validated 1 migration`; schema version `1`; `Schema "public" is up to date. No migration necessary.`
- Hikari connected to PostgreSQL 16.14.
- Hibernate initialized `EntityManagerFactory` successfully with no schema validation exception.
- Tomcat started on ephemeral port 54844; application logged `Started ChargingApplication`.
- The interactive boot process was stopped with Ctrl-C after startup; the cleanup check found no `ChargingApplication` JVM.

## Surface 3 — Gradle test task

Exact invocation: `./gradlew test --no-daemon`

Observed: exit code 0, `BUILD SUCCESSFUL in 3s`; `compileTestJava NO-SOURCE`, `test NO-SOURCE`. This proves the build/test task succeeds, but there are no behavioral test cases in the current project.

## Surface 4 — PostgreSQL catalog cross-check

Exact invocation: `docker compose exec -T postgres psql -U evcharging -d evcharging -v ON_ERROR_STOP=1 -c "SELECT version, success FROM flyway_schema_history ..." ...`

Observed: exactly one successful Flyway history row (`version=1`, `success=t`). Both `charging_session` and `charging_event` columns matched the V1 schema, including `numeric(12,3)` for `charged_kwh` and `timestamp with time zone` for all `Instant` columns. Unique constraints were present for `session_id` and `event_id`; the `(session_id, sequence)` index was present.

## Cleanup

Exact invocation: `pgrep -af '[j]ava.*ChargingApplication' || true; docker compose ps; docker volume inspect ...`

Observed: no application process; PostgreSQL and Kafka healthy; named volumes preserved. Product source files were not modified by this QA run.
