# Task 3 debugging-oriented runtime audit

- Date: 2026-08-13 (Asia/Seoul)
- Scope: Task 3 Flyway migration/configuration/startup only
- Product files modified by this audit: none
- HEAD SHA: `7465f6f93f86b029c0f93d0cb761ff5db26a7636`
- Working-tree content hash: `ca829623be05e115f75d86c42c1ead6056b225db7da13ac0e2e20f03344cdc98`
- Hash definition: SHA-256 of `git diff --binary HEAD` plus sorted SHA-256 records for untracked non-`.omo` product files; transient audit state excluded.
- Verdict: **PASS**

## Runtime ground truth

- Java: Temurin `21.0.12`; Gradle: `8.14`; Spring Boot: `3.5.5`
- PostgreSQL `postgres:16-alpine`: healthy; `pg_isready` returned `accepting connections`
- Kafka `apache/kafka:3.9.0`: healthy; broker API probe returned broker id `1`
- Disposable database: `debug_audit_task03`; audit port: `18083`

## Hypotheses and distinguishing evidence

### H1 — Flyway cannot discover or accept V1

Plausible cause: missing classpath resource, malformed name, or strict naming rejection. Check: real boot against empty PostgreSQL.

```text
Successfully validated 1 migration
Current version of schema "public": << Empty Schema >>
Migrating schema "public" to version "1 - create charging tables"
Successfully applied 1 migration to schema "public", now at version v1
```

Result: **REFUTED**. Exactly one V1 was discovered, validated, and applied.

### H2 — Flyway/JPA order or schema mismatch blocks startup

Plausible cause: Hibernate validation runs too early or DDL types/nullability disagree. Check: startup ordering plus direct catalog read-back.

```text
Successfully applied 1 migration ... version v1
Initialized JPA EntityManagerFactory for persistence unit 'default'
Tomcat started on port 18083
Started ChargingApplication in 1.182 seconds
```

Catalog read-back: `charging_event` 10 columns; `charging_session` 11; `charged_kwh numeric(12,3)`; timestamps use time zone; both named unique constraints and `idx_charging_event_session_sequence (session_id, sequence)` exist.

Result: **REFUTED**. Flyway completes before JPA initialization and the real server starts.

### H3 — Repeat startup re-applies or duplicates V1

Plausible cause: second startup reruns DDL or duplicates history. Check: boot again against the same database, then query history.

```text
Successfully validated 1 migration
Current version of schema "public": 1
Schema "public" is up to date. No migration necessary.
Started ChargingApplication in 1.041 seconds
```

History contained exactly one row: version `1`, description `create charging tables`, type `SQL`, success `true`.

Result: **REFUTED**. Repeat startup is safe and history remains singular.

### H4 — Unhealthy Compose dependencies create a false failure

Check: `docker compose ps`, PostgreSQL `pg_isready`, Kafka broker API probe. Both containers reported `healthy`; PostgreSQL accepted connections; Kafka returned broker metadata.

Result: **REFUTED**.

## Other gates

- `./gradlew test`: `BUILD SUCCESSFUL`; `test NO-SOURCE` (build/resource smoke only, not behavioral test coverage).
- `git diff --check`: exit 0.
- Catalog showed five expected physical indexes: two primary-key, two unique-constraint backing, one required composite.
- No silent-success mismatch: application success was independently confirmed by catalogs and Flyway history.

## Exact command surfaces

```text
docker compose ps
docker exec evcharging-postgres pg_isready -U evcharging -d evcharging
docker exec evcharging-kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
docker exec evcharging-postgres psql -U evcharging -d postgres -c 'CREATE DATABASE debug_audit_task03'
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/debug_audit_task03 SERVER_PORT=18083 ./gradlew bootRun --console=plain
# stopped after Started; repeated once against the same database
docker exec evcharging-postgres psql -U evcharging -d debug_audit_task03 ... catalog/history queries
./gradlew test
git diff --check
```

Local Compose credentials are intentionally redacted from the command transcription.

## Cleanup

- Both bounded `bootRun` sessions stopped after observable startup.
- Port `18083`: no listener; no remaining audit `ChargingApplication`/`bootRun` process.
- Database `debug_audit_task03`: dropped; follow-up database count `0`.
- PostgreSQL and Kafka left running and healthy, matching pre-audit state.
- No product file changed by this audit.

## Verdict

**PASS.** The current uncommitted Task 3 artifact boots on empty PostgreSQL, applies V1, exposes the required schema/constraints/index, and boots safely a second time. No runtime blocker found.
