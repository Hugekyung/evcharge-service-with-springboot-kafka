# Manual QA — Task 3 Database Migration

Result: PASS for Todos 3 and 4 on the isolated `evcharging_qa` database. The Compose PostgreSQL and Kafka services were available for happy-path boot; the named volumes were preserved. The task marker was changed only after the assertions passed.

## surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| startup-fresh | Todo 3 / fresh startup | terminal + PostgreSQL catalog | `docker compose up -d`; `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/evcharging_qa ./gradlew bootRun` (background, bounded by readiness poll, terminated after `Started`); `docker compose exec -T postgres psql -U evcharging -d evcharging_qa -f - < catalog-verification.sql` | PASS | `boot-first-log`, `catalog-assertions`, `catalog-verification`, `compose-up` |
| startup-repeat | Todo 3 / repeat startup | terminal + PostgreSQL catalog | `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/evcharging_qa ./gradlew bootRun` (same bounded readiness invocation); query `flyway_schema_history` with `psql` | PASS | `boot-second-log`, `catalog-assertions`, `catalog-verification` |
| gradle-suite | Todo 3 / verification | terminal | `./gradlew test` | PASS | `gradle-test`, `gradle-test-result` |
| task-marker | Todo 4 / completion record | terminal + working-tree diff | `rg -n '3\\. Database Migration 구성.*\\[완료\\]' docs/TASK.md`; `git diff --check` | PASS | `task-status` |

## catalog assertions

The captured catalog output proves all 21 PRD columns and nullability, both named unique constraints, the named `(session_id, sequence)` index, no extra application indexes beyond primary/required unique indexes, and exactly one successful V1 row. See `catalog-assertions.txt` and the executable assertion result in `catalog-verification.txt`.

## adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| unavailable-postgres | Todo 3 / failure probe | stale_state | PostgreSQL outage must fail startup instead of reporting success; restoring PostgreSQL must return it to healthy state | PASS | `boot-unavailable`, `boot-unavailable-result`, `compose-restored` |
| dirty-tree | Todo 4 / scope fidelity | dirty_worktree | Preserve unrelated existing work; only the requested task marker/config/migration and evidence are present | PASS | `task-status`, `git-status` |
| repeat-history | Todo 3 / repeat startup | stale_state | Second startup must not duplicate objects or add a second V1 history row | PASS | `boot-second-log`, `catalog-assertions` |
| catalog-over-log | Todo 3 / catalog proof | misleading_success_output | A log claiming startup is insufficient; independent catalog assertions must pass | PASS | `catalog-verification`, `catalog-assertions` |
| bounded-boot | Todo 3 / bounded execution | hung/long-command | Startup probe is bounded and leaves no app process behind | PASS | `boot-first-result`, `boot-second-result`, `task-status` |
| repeated-stop | Todo 3 / cleanup | repeated-interruption | Repeatedly stopping the two successful boot processes must leave services and database usable | PASS | `boot-first-result`, `boot-second-result`, `compose-restored`, `catalog-verification` |

## artifactRefs

| id | kind | description | path |
|---|---|---|---|
| boot-first-log | terminal-transcript | Fresh isolated startup; Flyway applies V1 and Spring reports Started | `.omo/evidence/task-03-database-migration/todo-3-startup/boot-first.log` |
| boot-second-log | terminal-transcript | Repeat startup; current schema version is 1 and Spring reports Started | `.omo/evidence/task-03-database-migration/todo-3-startup/boot-second.log` |
| catalog-assertions | db-catalog-output | Columns, nullability, constraints, indexes, Flyway history, volume inspection | `.omo/evidence/task-03-database-migration/todo-3-startup/catalog-assertions.txt` |
| catalog-verification | db-assertion-output | PostgreSQL DO-block assertion result: tables=2, columns=21, uniques=2, composite_index=1, extra_app_indexes=0, V1=1 | `.omo/evidence/task-03-database-migration/todo-3-startup/catalog-verification.txt` |
| boot-unavailable | terminal-transcript | Bounded boot with PostgreSQL stopped; Flyway connection refused and application fails | `.omo/evidence/task-03-database-migration/todo-3-startup/boot-postgres-unavailable.log` |
| boot-unavailable-result | command-result | Non-zero unavailable-Postgres exit and healthy restore receipt | `.omo/evidence/task-03-database-migration/todo-3-startup/boot-postgres-unavailable-result.txt` |
| compose-up | service-state | Compose PostgreSQL and Kafka startup/health evidence | `.omo/evidence/task-03-database-migration/todo-3-startup/compose-up.txt` |
| compose-restored | cleanup-receipt | PostgreSQL restored healthy; Kafka remained healthy | `.omo/evidence/task-03-database-migration/todo-3-startup/compose-restored.txt` |
| gradle-test | test-transcript | Full Gradle test command output | `.omo/evidence/task-03-database-migration/todo-3-startup/gradlew-test.log` |
| gradle-test-result | command-result | `./gradlew test` exit code 0 | `.omo/evidence/task-03-database-migration/todo-3-startup/gradlew-test-result.txt` |
| task-status | diff-and-marker | Task 3 marker, diff check, diff stat, service health, no leftover app process | `.omo/evidence/task-03-database-migration/todo-4-task-status.txt` |
| git-status | working-tree-state | Current branch and dirty-tree state captured during QA | `.omo/evidence/task-03-database-migration/todo-4-task-status.txt` |

Cleanup receipts: both boot processes were terminated after readiness and verified absent; the unavailable-Postgres probe exited non-zero under a 20-second alarm; PostgreSQL was restarted and reported healthy; Kafka remained healthy; no named volume was deleted.
