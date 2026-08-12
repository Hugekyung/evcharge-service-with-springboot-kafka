# Global hands-on QA review — Task 4 domain implementation

## Verdict

PASS for the requested live domain-mapping/runtime criteria. Flyway V1 and Hibernate `ddl-auto: validate` both succeeded against the existing Compose PostgreSQL database, and the Gradle test task exited successfully. Test coverage caveat: Gradle reported `NO-SOURCE`, so no behavioral tests ran.

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| GQA-01 | Task 4 completion; plan Todo 2 acceptance: both entities load under `ddl-auto: validate` | Compose PostgreSQL/Kafka | `docker compose up -d && docker compose ps && docker volume inspect evcharging_postgres-data evcharging_kafka-data --format '{{.Name}} {{.Mountpoint}}'` | PASS | `ART-GQA-RAW` |
| GQA-02 | Plan Todo 2 acceptance: Flyway V1 and both entities validate | Spring Boot runtime | `./gradlew bootRun --args='--server.port=0'` | PASS | `ART-GQA-RAW` |
| GQA-03 | Task 4 verification; plan final verification: `./gradlew test` | Gradle test task | `./gradlew test --no-daemon` | PASS (NO-SOURCE caveat) | `ART-GQA-RAW` |
| GQA-04 | V1 schema mapping and uniqueness requirements | PostgreSQL catalog | `docker compose exec -T postgres psql -U evcharging -d evcharging -v ON_ERROR_STOP=1 -c ...` (Flyway history, information_schema, constraints, indexes) | PASS | `ART-GQA-RAW` |
| GQA-05 | Cleanup / preserve Compose volumes | Process and Compose state | `pgrep -af '[j]ava.*ChargingApplication' || true; docker compose ps; docker volume inspect ...` | PASS | `ART-GQA-RAW` |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| ADV-GQA-01 | Runtime mapping must be checked against live V1, not stale logs | stale_state | Live startup and independent catalog query agree on Flyway V1 and entity schema | PASS | `ART-GQA-RAW` |
| ADV-GQA-02 | Preserve unrelated worktree changes | dirty_worktree | QA does not overwrite existing product or planning changes | PASS | `ART-GQA-RAW` |
| ADV-GQA-03 | Test output must not overclaim behavioral coverage | misleading_success_output | `NO-SOURCE` is reported explicitly; only build-task success is claimed | PASS | `ART-GQA-RAW` |
| ADV-GQA-04 | Runtime command must be bounded and cleaned up | hung_or_long_commands | Boot reaches startup, is stopped, and leaves no application JVM | PASS | `ART-GQA-RAW` |
| ADV-GQA-05 | Interrupt cleanup must leave infrastructure intact | repeated_interruptions | Application process is gone while healthy Compose services and named volumes remain | PASS | `ART-GQA-RAW` |
| ADV-GQA-06 | External-input malformed-input handling | not_applicable | Domain-only persistence mapping exposes no HTTP or message input boundary | not_applicable | `ART-GQA-RAW` |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| ART-GQA-RAW | runtime-log-and-catalog | Direct Compose, bootRun, Gradle test, catalog, and cleanup observations from this QA run | `.omo/evidence/task-04-domain-implementation/global-qa-runtime-evidence.md` |

## Scope and handoff

No product edits were made. Existing Compose services and named volumes were preserved. The only limitation is the absence of test sources; downstream behavioral work should add focused tests as planned.
