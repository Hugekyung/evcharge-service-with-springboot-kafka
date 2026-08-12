---
slug: task-03-database-migration
status: awaiting-approval
intent: clear
review_required: false
pending-action: write .omo/plans/task-03-database-migration.md
approach: Add one immutable Flyway V1 migration for both charging tables, enforce the PRD constraints/index, enable strict migration filename validation, and verify startup against the Docker Compose PostgreSQL instance. Keep entity validation aligned with the AGENTS.md recommendation after migrations exist.
---

# Draft: task-03-database-migration

## Components (topology ledger)
<!-- Lock the SHAPE before depth. One row per top-level component that can succeed or fail independently. -->
<!-- id | outcome (one line) | status: active|deferred | evidence path -->
| migration-script | V1 creates the two required tables, constraints, and event-history index | active | `docs/PRD.md:258-293`, `docs/TASK.md:58-69` |
| flyway-boot-config | Spring Boot discovers and validates the migration at startup | active | `src/main/resources/application.yml:5-18`, `build.gradle:20-33` |
| startup-verification | Docker Compose PostgreSQL accepts the migration and the app starts successfully | active | `docker-compose.yml:2-19`, `docs/TASK.md:66-69` |

## Open assumptions (announced defaults)
<!-- Record any default you adopt instead of asking, so the user can veto it at the gate. -->
<!-- assumption | adopted default | rationale | reversible? -->
| migration location/naming | `src/main/resources/db/migration/V1__create_charging_tables.sql` | Spring Boot/Flyway default discovery and versioned naming; makes the first schema change explicit | yes, before applying; applied scripts are immutable |
| schema source | use the exact PRD columns/types/nullability | PRD is authoritative and already resolves data-shape decisions | no, once consumers/entities depend on it |
| JPA schema mode | plan `ddl-auto: validate` once the migration exists; do not add entities in Task 3 | aligns with AGENTS.md and keeps Flyway as schema owner; Task 4 supplies mappings | yes |
| migration test strategy | tests-after: startup smoke verification plus direct PostgreSQL catalog assertions; no speculative repository/entity tests | Task 3 has no domain code yet, while the acceptance condition is migration success | yes |

## Findings (cited - path:lines)

- `docs/PRD.md:258-293` defines `charging_session`, `charging_event`, `BIGSERIAL` keys, `TIMESTAMPTZ` timestamps, `NUMERIC(12,3)`, unique `session_id`/`event_id`, and the composite index `(session_id, sequence)`.
- `docs/TASK.md:58-69` limits Task 3 to Flyway setup, the two tables, two unique constraints, and the composite index; completion is successful Spring Boot startup migration.
- `build.gradle:20-33` already contains JPA, Flyway core, PostgreSQL Flyway database support, and the PostgreSQL driver; no dependency addition is needed.
- `src/main/resources/application.yml:5-18` already points to the Docker Compose database, enables Flyway, and currently sets `ddl-auto: none` and `baseline-on-migrate: true`.
- `src/main/java/com/example/charging/` contains no entities, repositories, migrations, or tests yet; Task 3 must not implement those later-task concerns.
- Spring Boot/Flyway guidance: use `classpath:db/migration`, `V<VERSION>__<DESCRIPTION>.sql`, let Flyway own schema initialization, and validate migration naming/checksums. Sources: [Spring Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [Flyway migrations](https://documentation.red-gate.com/flyway/reference/migrations).

## Decisions (with rationale)

- One V1 migration creates both tables in dependency order (`charging_session` then `charging_event`); no repeatable migration or seed data because the PoC has no views/procedures/reference-data requirement.
- Use named unique constraints for `session_id` and `event_id`, plus a named `(session_id, sequence)` index. Do not add foreign keys or speculative indexes because the PRD does not require them and event ingestion may be modeled independently.
- Use `CREATE TABLE` statements with explicit `NOT NULL`/nullable columns and PostgreSQL types exactly as documented. Do not add defaults, triggers, update procedures, or application data.
- Enable `spring.flyway.validate-migration-naming: true` and keep Flyway as the only schema initializer. The existing `baseline-on-migrate` setting remains unchanged in this task to avoid silently changing the established local bootstrap behavior.
- Change `spring.jpa.hibernate.ddl-auto` from `none` to `validate` after the migration is present; no entity classes are added until Task 4, so this task still remains migration-only while future mappings are checked automatically.

## Scope IN

- Add `src/main/resources/db/migration/V1__create_charging_tables.sql`.
- Adjust only the Flyway/JPA properties needed for deterministic migration discovery and schema ownership in `src/main/resources/application.yml`.
- Start PostgreSQL with Docker Compose and run the application to prove Flyway startup migration.
- Inspect PostgreSQL catalog metadata to verify both tables, columns, nullability, named unique constraints, and the composite index.
- Update the Task 3 checklist to `[완료]` only after verification passes.

## Scope OUT (Must NOT have)

- No entity, enum, repository, service, controller, Kafka producer/consumer, or business logic implementation (Tasks 4+).
- No Testcontainers dependency or full integration test; that belongs to the later testing task and Task 3 can be proven against the already-configured local PostgreSQL container.
- No seed/sample data, repeatable migrations, triggers, stored procedures, foreign keys, extra indexes, pagination schema, or speculative audit columns.
- No dependency upgrades, Docker Compose redesign, or unrelated documentation changes.

## Open questions

None. The PRD and current repository configuration determine the migration shape and verification approach.

## Approval gate
status: awaiting-approval
approach: Add the exact PRD-defined two-table schema in one versioned Flyway migration, enable migration filename validation, use JPA validation as the schema ownership guard, verify against Docker Compose PostgreSQL, and then mark Task 3 complete.
next-action: After explicit approval, create `.omo/plans/task-03-database-migration.md`, run the mandatory plan-gap review, and write the decision-complete todos. Do not implement product code in this planning session.
<!-- When exploration is exhausted and unknowns are answered, set status: awaiting-approval. -->
<!-- That durable record is the loop guard: on a later turn read it and resume at the gate instead of re-running exploration. -->
