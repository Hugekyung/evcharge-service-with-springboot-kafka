# Task 3 manual QA — F3

Date: 2026-08-13 (Asia/Seoul)

Scope: database migration only. No product files were edited. Compose services were already running; `docker compose up -d` was used to health-check/reconcile them. A uniquely named disposable database, `qa_f3_20260813003836_44324`, was used. The existing named volumes and `evcharging` database were preserved.

## Surface evidence

| Scenario | Criterion | Surface and exact invocation | Verdict | Artifact refs |
|---|---|---|---|---|
| F3-S1 | Compose PostgreSQL/Kafka available | `docker compose up -d`; `docker compose ps` | PASS | A1 |
| F3-S2 | Migration creates required schema | `docker exec -i evcharging-postgres psql -U evcharging -d qa_f3_20260813003836_44324 -v ON_ERROR_STOP=1 < .omo/evidence/task-03-database-migration/f3/manual-qa/catalog.sql` | PASS | A5 |
| F3-S3 | First Spring Boot start runs Flyway and starts | `script -q .omo/evidence/task-03-database-migration/f3/manual-qa/boot-first-final.log env SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/qa_f3_20260813003836_44324 SPRING_DATASOURCE_USERNAME=evcharging SPRING_DATASOURCE_PASSWORD=evcharging SERVER_PORT=18081 ./gradlew bootRun`, bounded by polling for `Started` (60 seconds max), then terminated | PASS | A2, A3 |
| F3-S4 | Second start is repeat-safe / stale-state check | Same command with `boot-second-final.log`, bounded identically and terminated | PASS | A2, A4 |
| F3-S5 | Automated verification | `./gradlew test`; `git diff --check` | PASS | A6, A7 |
| F3-S6 | Cleanup and state restoration | `docker exec evcharging-postgres psql ... -c 'DROP DATABASE "qa_f3_20260813003836_44324"'`; `docker compose ps`; process inventory | PASS | A8, A9 |

Independent catalog output shows both tables, all migration columns, both unique constraints, the `(session_id, sequence)` index, and `flyway_v1_count = 1`. Both bounded starts contain `Started ChargingApplication`; the second start did not re-run a pending migration.

Note: an initial exploratory second start found port 8080 occupied by a pre-existing app process. That process was terminated as part of QA process cleanup, and both formal bounded runs used the explicitly recorded disposable port `18081`.

## Adversarial cases

| Scenario | Criterion | Adversarial class | Expected behavior | Verdict | Artifact refs |
|---|---|---|---|---|---|
| F3-A1 | Repeat startup | stale_state | Existing V1 state remains valid; second boot reaches `Started` and Flyway count remains one | PASS | A4, A5 |
| F3-A2 | Truthful migration verification | misleading_success_output | Direct PostgreSQL catalogs, not only application logs, prove schema/constraints/index and V1 count | PASS | A5 |
| F3-A3 | Bounded execution | hung_or_long | Each boot is bounded at 60 seconds and terminated after observable result | PASS | A3, A4 |
| F3-A4 | Process hygiene | repeated_interruptions | Terminated boot processes leave zero `ChargingApplication`/`bootRun` QA processes | PASS | A8 |
| F3-A5 | Repeatability | flaky_tests | Two independent sequential starts against the same disposable DB both pass | PASS | A3, A4 |
| F3-A6 | Migration filename/input handling | malformed_input | N/A — this task's QA target is the fixed checked-in migration; malformed filename is not a runtime scenario triggered by this change | NOT_APPLICABLE | A10 |
| F3-A7 | Prompt injection | prompt_injection | N/A — no user-controlled prompt/content surface exists in database migration startup | NOT_APPLICABLE | A5 |
| F3-A8 | Cancellation/resume | cancel_resume | N/A — migration startup has no resumable user workflow; bounded repeated startup covers the applicable recovery behavior | NOT_APPLICABLE | A3, A4 |

## Artifacts

| ID | Kind | Description | Path |
|---|---|---|---|
| A1 | terminal | Compose health and startup output | `.omo/evidence/task-03-database-migration/f3/manual-qa/compose-health.txt`, `compose-up.log` |
| A2 | command | Disposable DB name and exact boot invocations | `.omo/evidence/task-03-database-migration/f3/manual-qa/db-name.txt`, `boot-first-final-result.txt`, `boot-second-final-result.txt` |
| A3 | terminal transcript | First bounded boot, including Flyway and `Started` | `.omo/evidence/task-03-database-migration/f3/manual-qa/boot-first-final.log` |
| A4 | terminal transcript | Second bounded boot, including Flyway and `Started` | `.omo/evidence/task-03-database-migration/f3/manual-qa/boot-second-final.log` |
| A5 | SQL/catalog | Independent columns, constraints, indexes, and exact V1 count | `.omo/evidence/task-03-database-migration/f3/manual-qa/catalog.sql`, `catalog.txt` |
| A6 | test output | `./gradlew test` successful | `.omo/evidence/task-03-database-migration/f3/manual-qa/gradlew-test.log`, `gradlew-test-result.txt` |
| A7 | check output | `git diff --check` exit 0 | `.omo/evidence/task-03-database-migration/f3/manual-qa/git-diff-check.txt` |
| A8 | cleanup receipt | Disposable DB dropped; zero remaining QA boot processes | `.omo/evidence/task-03-database-migration/f3/manual-qa/drop-db.log`, `cleanup-receipt.txt`, `processes-after-cleanup.txt` |
| A9 | terminal | Compose state after cleanup; services remain healthy | `.omo/evidence/task-03-database-migration/f3/manual-qa/compose-restored.txt` |
| A10 | prior evidence | Fixed migration naming check | `.omo/evidence/task-03-database-migration/malformed-migration-name.log` |

## Final verdict

DoneClaim: PASS. The required migration behavior is observable: healthy Compose services, clean disposable PostgreSQL migration, two bounded Spring Boot starts with `Started`, direct catalog equality/constraint/index assertions, and exactly one successful V1 history row. Cleanup completed before this verdict: disposable DB dropped, no QA boot process remains, and Compose services/volumes were preserved.
