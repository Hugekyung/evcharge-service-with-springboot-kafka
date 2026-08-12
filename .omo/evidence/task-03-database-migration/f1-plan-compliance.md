# F1 Plan Compliance Audit

Verdict: PASS

Tree under audit: `2822d88` (working-tree content; migration is currently untracked and therefore inspected directly).

## Must-have checks

| requirement | evidence | result |
|---|---|---|
| Exact V1 migration path and PRD tables/columns/types/nullability | `src/main/resources/db/migration/V1__create_charging_tables.sql`, PRD 7.1/7.2, `todo-3-startup/catalog-assertions.txt` | PASS |
| Named session/event unique constraints | Migration DDL and catalog output show `uk_charging_session_session_id`, `uk_charging_event_event_id` | PASS |
| Named composite history index with no redundant application index | Migration DDL and catalog output show `idx_charging_event_session_sequence`; assertion reports `extra_app_indexes=0` | PASS |
| Flyway naming validation and JPA validation ownership | `application.yml` diff and `todo-2-config.txt` | PASS |
| Fresh/repeat startup and V1 exactly once | `boot-first.log`, `boot-second.log`, catalog history output, `reverify-v1-count.txt` | PASS |
| Task 3 completion marker only after evidence | `docs/TASK.md`, `todo-4-task-status.txt`, prior Todo 3/4 ledger entries | PASS |

## Must-NOT-have checks

PASS: migration contains no foreign key, CHECK constraint, trigger, procedure, seed insert, repeatable migration, or speculative index. No dependency or Docker Compose changes are present. Existing Java source contains only pre-existing package markers; no domain/entity/repository/service/controller implementation was added by this task. The product diff is limited to `application.yml`, `docs/TASK.md`, and the migration file; `.omo/` contains evidence/state only.

## Adversarial verification

- stale state: repeat startup and live V1 count both prove one applied migration.
- dirty worktree: final status was inspected; unrelated existing files were preserved.
- misleading success output: PostgreSQL catalog assertions independently verify schema/history.
- hung/long commands: startup and failure probes were bounded and cleaned up.
- repeated interruptions: both successful boot processes were stopped and no app process remains.

Cleanup receipt: no QA app process remains; PostgreSQL and Kafka are healthy; no named volume was deleted; temporary QA migration filename was restored.
