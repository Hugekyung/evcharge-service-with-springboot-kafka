# Independent Manual QA — Task 3 Database Migration

## surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| review-startup-fresh | Task 3 / Flyway fresh startup | terminal + PostgreSQL | `DROP DATABASE IF EXISTS evcharging_qa_review; CREATE DATABASE evcharging_qa_review`; `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/evcharging_qa_review ./gradlew bootRun` (bounded poll for `Started`, then terminate process) | PASS | `review-db-create`, `review-boot-fresh`, `review-boot-fresh-result` |
| review-startup-repeat | Task 3 / repeat startup | terminal + PostgreSQL | same `./gradlew bootRun` invocation against `evcharging_qa_review`, bounded poll for `Started`, then terminate; direct `flyway_schema_history` query | PASS | `review-boot-repeat`, `review-boot-repeat-result`, `review-catalog` |
| review-catalog | Task 3 / schema and constraints | PostgreSQL catalog | `docker compose exec -T postgres psql -U evcharging -d evcharging_qa_review -v ON_ERROR_STOP=1` with catalog queries and a PostgreSQL `DO` assertion block | PASS | `review-catalog` |
| review-gradle | Task 3 / regression verification | terminal | `./gradlew test` | PASS | `review-gradle-test`, `review-gradle-result` |

## adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| review-repeat-history | Task 3 / repeat startup | stale_state | Repeat startup leaves one successful V1 row and does not duplicate schema objects | PASS | `review-catalog`, `review-boot-repeat-result` |
| review-catalog-over-log | Task 3 / schema proof | misleading_success_output | Independent catalog assertions, not only startup logs, must pass | PASS | `review-catalog` |
| review-bounded-cleanup | Task 3 / bounded execution | hung/long-command | Boot probe is bounded and leaves no application process | PASS | `review-boot-fresh-result`, `review-boot-repeat-result`, `review-cleanup` |
| review-disposable-cleanup | Task 3 / cleanup | dirty_worktree | Disposable QA database is removed while Compose services remain healthy | PASS | `review-cleanup` |
| review-malformed-migration | Task 3 / Flyway naming | malformed_input | Not applicable: this lane verifies the committed V1 migration and startup; malformed-name rejection was already independently captured in `malformed-migration-name.log` | not_applicable | `malformed-migration-name.log` |

## artifactRefs

| id | kind | description | path |
|---|---|---|---|
| review-db-create | db-transcript | Disposable review database creation | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/qa-db-create.txt` |
| review-boot-fresh | terminal-transcript | Fresh Spring Boot startup against empty database | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/boot-fresh.log` |
| review-boot-fresh-result | command-result | Fresh startup marker and process cleanup | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/boot-fresh-result.txt` |
| review-boot-repeat | terminal-transcript | Repeat Spring Boot startup | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/boot-repeat.log` |
| review-boot-repeat-result | command-result | Repeat startup marker and process cleanup | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/boot-repeat-result.txt` |
| review-catalog | db-assertion-output | Direct PostgreSQL catalog and Flyway assertions | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/catalog-verification.txt` |
| review-gradle-test | test-transcript | Independent Gradle test run | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/gradlew-test.log` |
| review-gradle-result | command-result | Gradle test exit code 0 | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/gradlew-test-result.txt` |
| review-cleanup | cleanup-receipt | Review DB dropped, no app process, Compose services healthy | `.omo/evidence/task-03-database-migration/todo-3-review-20260813/cleanup-receipt.txt` |

Independent verdict: PASS. Cleanup: disposable `evcharging_qa_review` dropped; no application process remains; PostgreSQL and Kafka Compose services retained healthy.
