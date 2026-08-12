# task-03-database-migration - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** A versioned Flyway migration that creates the two PRD-defined charging tables with their required uniqueness rules and history index, plus startup verification against PostgreSQL.

**Why this approach:** Flyway remains the only schema owner and the DDL is copied exactly from the PRD, while catalog assertions prove the database rather than trusting startup logs alone.

**What it will NOT do:** It will not add entities, repositories, Kafka business flow, seed data, foreign keys, or speculative constraints/indexes.

**Effort:** Short
**Risk:** Medium - startup verification depends on both Compose services and must isolate PostgreSQL state without deleting the user’s existing volume.
**Decisions to sanity-check:** exact PRD schema, keeping `baseline-on-migrate` unchanged, and using `ddl-auto: validate` before entities exist.

Your next move: Run `$start-work task-03-database-migration` on the task branch.

---

> TL;DR (machine): Short migration/config task; adds one immutable V1 SQL, verifies fresh and repeat startup, and updates Task 3 only after evidence passes.

## Scope
### Must have
 - `src/main/resources/db/migration/V1__create_charging_tables.sql` with both tables and exact PRD columns/types/nullability.
 - Named UNIQUE constraints for `charging_session.session_id` and `charging_event.event_id`.
 - Named composite index on `charging_event(session_id, sequence)` and no redundant standalone indexes.
 - Flyway naming validation enabled; JPA schema ownership aligned with `ddl-auto: validate`; existing `baseline-on-migrate` behavior retained.
 - Agent-executed fresh-database startup and second-startup verification with PostgreSQL catalog assertions and `flyway_schema_history` evidence.
 - `docs/TASK.md` Task 3 marked complete only after all verification passes.
### Must NOT have (guardrails, anti-slop, scope boundaries)
 - No entities, enums, repositories, services, controllers, Kafka producer/consumer, or later-task business behavior.
 - No new dependency, Docker Compose redesign, Testcontainers, seed data, repeatable migration, trigger, stored procedure, foreign key, CHECK constraint, extra index, or speculative column.
 - No destructive deletion of the user’s existing Docker volumes or unrelated file edits.

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: tests-after; Gradle smoke plus PostgreSQL catalog assertions executed by the worker.
- Evidence: `.omo/evidence/task-03-database-migration/` with startup logs, catalog query output, and verification summaries.

## Execution strategy
### Parallel execution waves
> Target 5-8 todos per wave. Fewer than 3 (except the final) means you under-split.
Wave 1 is sequential because configuration and DDL are one schema contract. Wave 2 verifies the resulting database. Wave 3 updates tracking after verification.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | — | 2 | — |
| 2 | 1 | 3 | — |
| 3 | 2 | 4 | — |
| 4 | 3 | Final wave | — |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [x] 1. Add the immutable Flyway V1 schema migration
  What to do / Must NOT do: Create `src/main/resources/db/migration/V1__create_charging_tables.sql` with `charging_session` first and `charging_event` second. Use `BIGSERIAL` primary keys; exact PRD `VARCHAR`, `INTEGER`, `BIGINT`, `NUMERIC(12,3)`, and `TIMESTAMPTZ` columns/nullability; named unique constraints `uk_charging_session_session_id` and `uk_charging_event_event_id`; and named index `idx_charging_event_session_sequence`. Must not add defaults, foreign keys, checks, triggers, seed rows, or extra indexes.
  Parallelization: Wave 1 | Blocked by: — | Blocks: 2
  References (executor has NO interview context - be exhaustive): `docs/PRD.md:258-293`; `docs/TASK.md:58-69`; `AGENTS.md:342-368`; Spring Boot migration location/naming guidance at https://docs.spring.io/spring-boot/how-to/data-initialization.html
  Acceptance criteria (agent-executable): File exists at the exact path; SQL parses; `psql` applied to a clean PostgreSQL database creates both tables, the two named unique constraints, the named composite index, and no other application index; `flyway_schema_history` records version `1`.
  QA scenarios (name the exact tool + invocation): happy: `docker compose exec -T postgres psql -U evcharging -d evcharging -c "select ... from information_schema...;"` after a clean isolated database; failure: run the same catalog assertions against a database before migration and confirm the required tables/constraints/index are absent, then apply V1 and re-run for pass. Evidence `.omo/evidence/task-03-database-migration/todo-1-schema.txt`.
  Commit: Y | `feat(database): add initial charging schema migration`

- [x] 2. Align Spring Boot migration and schema ownership configuration
  What to do / Must NOT do: Update only `src/main/resources/application.yml`: set `spring.jpa.hibernate.ddl-auto: validate` and `spring.flyway.validate-migration-naming: true`; retain Flyway enabled, PostgreSQL connection values, and existing `baseline-on-migrate: true`. Do not add dependencies or alter Kafka/HTTP settings.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 3
  References (executor has NO interview context - be exhaustive): `src/main/resources/application.yml:5-18`; `build.gradle:20-33`; `AGENTS.md:342-357`; Spring Boot schema initialization guidance at https://docs.spring.io/spring-boot/how-to/data-initialization.html
  Acceptance criteria (agent-executable): `./gradlew test` exits 0; application properties resolve to Flyway enabled, migration naming validation true, and JPA validate; no build dependency diff is introduced.
  QA scenarios (name the exact tool + invocation): happy: `./gradlew test` and a property inspection from the running app show the expected values; failure: temporarily use a malformed migration filename in a disposable copy and confirm naming validation rejects it, then restore the exact V1 filename. Evidence `.omo/evidence/task-03-database-migration/todo-2-config.txt`.
  Commit: Y | `chore(database): align flyway and jpa schema validation`

- [x] 3. Prove fresh and repeat Spring Boot startup migration behavior
  What to do / Must NOT do: Start both PostgreSQL and Kafka with `docker compose up -d`, use a disposable PostgreSQL database/schema or isolated Compose project without deleting existing named volumes, run `./gradlew bootRun` with a bounded timeout, capture Flyway success, and query the catalog plus `flyway_schema_history`. Stop the app and start it a second time to prove no duplicate-object failure and no second V1 application. Account for `KafkaTopicInitializer` requiring Kafka during boot.
  Parallelization: Wave 2 | Blocked by: 1, 2 | Blocks: 4
  References (executor has NO interview context - be exhaustive): `docker-compose.yml:1-50`; `src/main/java/com/example/charging/config/KafkaTopicInitializer.java:13-48`; `src/main/resources/application.yml:5-18`; `docs/TASK.md:66-69`; `AGENTS.md:570-590`
  Acceptance criteria (agent-executable): fresh run exits successfully with Flyway applying V1; catalog assertions match the PRD; second run exits successfully with V1 still recorded exactly once; Kafka topic initialization does not obscure the Flyway result; existing `postgres-data`/`kafka-data` volumes remain intact.
  QA scenarios (name the exact tool + invocation): happy: `docker compose up -d && timeout 90 ./gradlew bootRun > .omo/evidence/task-03-database-migration/boot-first.log 2>&1`, then repeat for `boot-second.log` and query `flyway_schema_history`; failure: run `./gradlew bootRun` with PostgreSQL unavailable in a disposable environment and confirm bounded non-zero startup, then restore services and re-run happy path. Evidence `.omo/evidence/task-03-database-migration/todo-3-startup/`.
  Commit: N | verification-only

- [x] 4. Record verified Task 3 completion
  What to do / Must NOT do: After todos 1-3 pass, change only the Task 3 heading/checklist in `docs/TASK.md` to `[완료]` and preserve all later tasks and wording. Do not claim completion from file presence alone.
  Parallelization: Wave 3 | Blocked by: 3 | Blocks: Final wave
  References (executor has NO interview context - be exhaustive): `docs/TASK.md:58-69`; `AGENTS.md:594-610`; evidence from `.omo/evidence/task-03-database-migration/`
  Acceptance criteria (agent-executable): `docs/TASK.md` shows Task 3 `[완료]`; `git diff --check` exits 0; `git diff --stat` contains only the planned migration/config/checklist files plus evidence/state artifacts.
  QA scenarios (name the exact tool + invocation): happy: `rg -n "3\. Database Migration 구성.*\[완료\]" docs/TASK.md && git diff --check`; failure: on a disposable copy with one required catalog assertion removed, verify the checklist remains incomplete until the assertion is restored. Evidence `.omo/evidence/task-03-database-migration/todo-4-task-status.txt`.
  Commit: Y | `docs(task): mark database migration complete`

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [x] F1. Plan compliance audit
  Verify every Must-have against the final diff and the recorded SQL/catalog/startup evidence; pass only if no Must-NOT-have artifact exists.
- [x] F2. Code quality review
  Inspect SQL naming, ordering, nullability, PostgreSQL compatibility, YAML placement, and migration immutability; pass only with no correctness or maintainability finding.
- [x] F3. Real manual QA
  Re-run `docker compose up -d`, bounded first/second `./gradlew bootRun`, and catalog queries from the evidence directory; pass only when fresh and repeat startup both succeed and the schema history count is one.
- [x] F4. Scope fidelity
  Run `git diff --name-only main...HEAD` and confirm only migration/config/TASK files are product changes; confirm no dependency, Docker volume, entity, repository, or business-flow changes.

## Commit strategy

Use the existing `feature/task-03-database-migration` branch. Keep the migration, configuration, and verified checklist changes in focused commits (or one atomic commit if the worker’s repository state makes splitting unsafe); never commit unrelated pre-existing changes. Do not push or open a PR unless explicitly requested.

## Success criteria

- Flyway applies V1 successfully on a clean PostgreSQL database at application startup.
- Both tables, all PRD columns/nullability, the two unique constraints, and the composite index are observable in PostgreSQL catalog queries.
- A second application startup is successful and does not duplicate V1.
- `./gradlew test` and `git diff --check` pass.
- Task 3 is marked `[완료]` only after the evidence exists.
