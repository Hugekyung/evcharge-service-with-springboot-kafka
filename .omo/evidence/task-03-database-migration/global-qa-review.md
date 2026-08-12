# Global QA Review — Task 03 database migration

<verdict>PASS</verdict>

Confidence: high. Reviewed commit `7465f6f93f86b029c0f93d0cb761ff5db26a763a` (HEAD prefix `7465f6f`). Scope was limited to V1 migration, Flyway/JPA configuration, startup repeatability, catalog/history, and Gradle smoke. No product files were edited during this QA run.

## Working-tree context

Before QA, `git status --short` showed pre-existing changes: `docs/TASK.md`, `src/main/resources/application.yml`, untracked `.omo/`, and untracked `src/main/resources/db/`. `git diff --check` returned exit 0. These changes were preserved.

## Scenario matrix

| ID | Criterion | Surface / exact invocation | Verdict | Evidence |
|---|---|---|---|---|
| GQ-01 | Compose services healthy | `docker compose up -d`; `docker compose ps` | PASS | [report](global-qa-review.md) — postgres and kafka `Up ... (healthy)` |
| GQ-02 | V1 migration history | `docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -U evcharging -d evcharging_qa -c "SELECT version, description, type, success FROM flyway_schema_history WHERE version='1';"` | PASS | [report](global-qa-review.md) — exactly `(1, create charging tables, SQL, t)` |
| GQ-03 | Catalog shape | `docker compose exec -T postgres psql ... -c "WITH cols ... SELECT ..."` | PASS | [report](global-qa-review.md) — `columns=21, constraints=4, indexes=5, v1_history_rows=1` |
| GQ-04 | Fresh application startup | `./gradlew bootRun --args='--spring.datasource.url=jdbc:postgresql://localhost:5432/evcharging_qa ... --server.port=18080'` (bounded 15s, terminated after Started) | PASS | [report](global-qa-review.md) — Flyway validated 1 migration; schema up to date; app started in 1.149s |
| GQ-05 | Repeat application startup | Same invocation with `--server.port=18081` (bounded 10s, terminated after Started) | PASS | [report](global-qa-review.md) — validated 1 migration; “Schema public is up to date”; app started in 1.137s |
| GQ-06 | Gradle smoke | `./gradlew test` | PASS | [report](global-qa-review.md) — `BUILD SUCCESSFUL`, 585ms |
| GQ-07 | Diff whitespace | `git diff --check` | PASS | [report](global-qa-review.md) — exit 0 |

## Manual QA matrix

### Adversarial cases

| Scenario | Criterion | Adversarial class | Expected behavior | Verdict | Evidence |
|---|---|---|---|---|---|
| ADV-01 | Startup repeatability | stale_state | Existing migration state must validate and remain at V1 without rerunning or changing catalog. | PASS | [report](global-qa-review.md), GQ-02/GQ-05 |
| ADV-02 | QA hygiene | dirty_worktree | Pre-existing edits must be detected and preserved; QA must not silently claim a clean tree. | PASS | [report](global-qa-review.md), working-tree context |
| ADV-03 | Bounded execution | hung_or_long_commands | Long-running boot must be bounded and terminated; no orphan application process remains. | PASS | [report](global-qa-review.md), GQ-04/GQ-05 |
| ADV-04 | Evidence integrity | misleading_success_output | Logs must be cross-checked against PostgreSQL catalog/history, not accepted alone. | PASS | [report](global-qa-review.md), GQ-02/GQ-03 |
| ADV-05 | Interrupt handling | repeated_interruptions | Repeated startup/termination must leave database and pre-existing services usable. | PASS | [report](global-qa-review.md), GQ-05 and final `docker compose ps` |
| ADV-06 | Input shape | malformed_input | Not applicable: Task 03 migration has no external input parser or request surface. | N/A | Reason recorded here |
| ADV-07 | Prompt injection | prompt_injection | Not applicable: no prompt or user-content processing is in scope. | N/A | Reason recorded here |
| ADV-08 | Cancellation | cancel_resume | Not applicable: no resumable user workflow is in scope. | N/A | Reason recorded here |

## Exact observed results

Manual history query returned exactly:

```text
 version |      description       | type | success
---------+------------------------+------+---------
 1       | create charging tables | SQL  | t
(1 row)
```

Catalog aggregate query returned:

```text
 columns | constraints | indexes | v1_history_rows
---------+-------------+---------+-----------------
      21 |           4 |       5 |               1
(1 row)
```

Index inspection showed five indexes: two primary-key indexes, two required unique indexes (`session_id`, `event_id`), and `idx_charging_event_session_sequence`. Constraint inspection showed four constraints: two primary keys and the two required unique constraints.

## Blockers and flaky classification

No blockers. `./gradlew test` reported `NO-SOURCE` for tests but completed successfully; this is a test-coverage limitation, not a flaky failure. Compose services were already running; `docker compose up -d` reused them.

## Cleanup receipt

The application boot PIDs were terminated with `kill -TERM` after successful startup on ports 18080 and 18081. A final process check found no surviving `ChargingApplication`/Gradle boot process. Existing `evcharging-postgres` and `evcharging-kafka` containers remained running and healthy. The pre-existing named volumes were not removed or recreated. The pre-existing disposable database `evcharging_qa` was left intact to avoid altering shared QA state; no new database, volume, or product asset was created. Temporary boot logs/PID files were under `/tmp` only.

## Artifact references

| id | kind | description | path |
|---|---|---|---|
| ART-GQA-01 | markdown | This complete manual QA report and embedded command results | `.omo/evidence/task-03-database-migration/global-qa-review.md` |

