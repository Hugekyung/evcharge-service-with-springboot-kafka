# F3 Manual QA — task-04-domain-implementation

Date: 2026-08-13
Worktree: `feature/task-04-domain`
Scope: real runtime verification of Compose, Spring Boot startup, Flyway V1, Hibernate `ddl-auto=validate`, and Gradle tests. No product edits.

## Result

PASS. PostgreSQL and Kafka were healthy; bounded `./gradlew bootRun --args='--server.port=0'` reached `Started ChargingApplication`, Flyway reported schema version 1 current, Hibernate initialized successfully under `ddl-auto: validate`, and the direct PostgreSQL catalog showed both domain tables and their mapped columns/constraints. `./gradlew test --no-daemon` exited successfully, with `NO-SOURCE` (compile/build verification only; no behavioral test sources). Boot processes were stopped, no `ChargingApplication` process remained, and Compose services/volumes were preserved.

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| F3-S1 | Task 4 completion: Entity and DB Schema Mapping normal | Docker Compose | `docker compose up -d && docker compose ps --format 'table ...'` | PASS | A1 |
| F3-S2 | Task 4 completion: Flyway V1 + Hibernate mapping startup | Spring Boot runtime | `./gradlew bootRun --args='--server.port=0'` (bounded 60s startup wait, then SIGINT/SIGTERM cleanup) | PASS | A2 |
| F3-S3 | Task 4 completion: both entities mapped to live schema | PostgreSQL catalog | `docker exec evcharging-postgres psql -U evcharging -d evcharging ...` querying Flyway, information_schema, constraints, indexes | PASS | A3 |
| F3-S4 | Required verification: `./gradlew test` | Gradle CLI | `./gradlew test --no-daemon` | PASS (NO-SOURCE) | A4 |
| F3-S5 | Cleanup requirement | OS process table | `ps -axo pid=,command= \\| awk '/com\\.example\\.charging\\.ChargingApplication/ {print}'` after bounded shutdown | PASS | A5 |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| F3-A1 | Runtime isolation | stale_state | Detect pre-existing project app JVMs rather than claiming a clean baseline; terminate only exact project app processes and recheck. | PASS | A5 |
| F3-A2 | Runtime boundedness | hung_or_long | Startup must either reach `Started` or terminate within the hard bound; no indefinite process remains. | PASS | A2, A5 |
| F3-A3 | Evidence integrity | misleading_success_output | Treat Gradle `NO-SOURCE` as compile-only and independently verify Flyway/schema/Hibernate evidence from logs and live PostgreSQL catalogs. | PASS | A2, A3, A4 |
| F3-A4 | Shutdown robustness | repeated_interruptions | SIGINT followed by bounded SIGTERM fallback must leave no `ChargingApplication` process. | PASS | A2, A5 |
| F3-A5 | Worktree safety | dirty_worktree | Record existing tracked/untracked changes and keep QA artifacts under `.omo/evidence`; do not alter product files. | PASS | A6 |

## artifactRefs

| id | kind | description | path |
|---|---|---|---|
| A1 | runtime transcript | Compose PostgreSQL/Kafka healthy status; named services left running. | `.omo/evidence/task-04-domain-implementation/f3-compose-status.txt` |
| A2 | runtime transcript | Bounded bootRun output: random port, Flyway version 1 current/up-to-date, Hibernate initialization, Started line, shutdown. | `.omo/evidence/task-04-domain-implementation/f3-bootrun-2.log` |
| A3 | database query output | Live PostgreSQL Flyway history, both tables, 21 mapped columns, unique constraints, and indexes. | `.omo/evidence/task-04-domain-implementation/f3-postgres-schema.txt` |
| A4 | CLI transcript | Full Gradle test result, `BUILD SUCCESSFUL`, explicitly `NO-SOURCE`. | `.omo/evidence/task-04-domain-implementation/f3-tests.log` |
| A5 | process transcript | Final project application-process cleanup check (empty output); exact stale JVM cleanup was performed before final check. | `.omo/evidence/task-04-domain-implementation/f3-cleanup-process-check.txt` |
| A6 | worktree inventory | Read-only worktree status snapshot; product changes were preserved. | `.omo/evidence/task-04-domain-implementation/f3-worktree-inventory.txt` |

Cleanup: boot process stopped; exact stale `ChargingApplication` JVM residue removed; no project app process remains; PostgreSQL and Kafka remain healthy; Compose volumes were not removed.
