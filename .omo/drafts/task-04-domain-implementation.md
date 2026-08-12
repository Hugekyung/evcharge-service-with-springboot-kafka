---
slug: task-04-domain-implementation
status: awaiting-approval
intent: clear
review_required: false
pending-action: write .omo/plans/task-04-domain-implementation.md
approach: Implement ChargingSession first, then ChargingEvent on the single `feature/task-04-domain` branch. Map both entities exactly to the existing Flyway V1 schema, persist enums as strings, use generated identity IDs, and verify the mappings against PostgreSQL before marking Task 4 complete.
---

# Draft: task-04-domain-implementation

## Components (topology ledger)
<!-- Lock the SHAPE before depth. One row per top-level component that can succeed or fail independently. -->
<!-- id | outcome (one line) | status: active|deferred | evidence path -->
| charging-session | Entity and status enum map to `charging_session` and preserve persistence-only state | active | `docs/TASK.md:73-87`, `docs/PRD.md:122-139`, `src/main/resources/db/migration/V1__create_charging_tables.sql:1-15` |
| charging-event | Entity and event enum map to `charging_event` and preserve event history fields | active | `docs/TASK.md:89-100`, `docs/PRD.md:141-162`, `src/main/resources/db/migration/V1__create_charging_tables.sql:17-31` |
| mapping-verification | Hibernate validation and focused persistence tests prove both mappings | active | `docs/TASK.md:107-109`, `src/main/resources/application.yml:11-19`, `AGENTS.md:570-590` |

## Open assumptions (announced defaults)
<!-- Record any default you adopt instead of asking, so the user can veto it at the gate. -->
<!-- assumption | adopted default | rationale | reversible? -->
| implementation order | ChargingSession + status first, then ChargingEvent + event type | user explicitly requested sequential implementation on one branch; event/session schema and future service depend on both | yes |
| enum persistence | `@Enumerated(EnumType.STRING)` with PostgreSQL VARCHAR columns | stable values and exact V1 schema; ordinal values are unsafe | yes before persisted data |
| generated IDs | `Long` + `@GeneratedValue(strategy = GenerationType.IDENTITY)` | matches PostgreSQL BIGSERIAL and avoids a second sequence strategy | no once persisted |
| decimal mapping | `BigDecimal` with precision 12, scale 3 | matches `NUMERIC(12,3)` without floating-point rounding | no for schema contract |
| entity responsibility | persistence mapping, invariants needed for valid construction, and state access only; no transition workflow/idempotency/ordering logic | application service owns business rules and transaction boundaries per AGENTS.md | yes |
| timestamp mapping | `Instant` for every `TIMESTAMPTZ` field (`startedAt`, `completedAt`, `createdAt`, `updatedAt`, `occurredAt`, `processedAt`) | one UTC-normalized Java type across both entities and Kafka-facing future models; user approved | yes before persisted data |

## Findings (cited - path:lines)

- `docs/TASK.md:73-109` requires exactly two entities, two enums, and successful entity-to-schema mapping; repositories and services are later tasks.
- `docs/PRD.md:122-168` defines all fields and enum values; `docs/PRD.md:172-195` reserves transition, terminal-state, missing-session, and charger matching rules for business behavior.
- `docs/PRD.md:258-293` and `src/main/resources/db/migration/V1__create_charging_tables.sql:1-31` are the exact schema contract: BIGSERIAL IDs, VARCHAR enum/text columns, NUMERIC(12,3), TIMESTAMPTZ, nullability, uniqueness, and the existing history index.
- `src/main/resources/application.yml:11-19` sets `ddl-auto: validate`, so entity column names/types/nullability must match V1 without Hibernate schema changes.
- `build.gradle:20-33` already provides JPA, PostgreSQL, Flyway, and test dependencies; no dependency addition is needed for Task 4.
- The domain package contains only `package-info.java`; no entity, enum, repository, or test convention exists to preserve.
- Official Hibernate guidance supports `java.time` mappings, string enum persistence, identity ID generation, and `BigDecimal` for exact numeric values. Sources: https://docs.hibernate.org/orm/6.5/userguide/html_single/ and https://docs.hibernate.org/orm/6.5/introduction/html_single/.

## Decisions (with rationale)

- Create only `ChargingSession`, `ChargingEvent`, `ChargingSessionStatus`, and `ChargingEventType` under `src/main/java/com/example/charging/domain/`.
- Map table/column names explicitly with `@Table` and `@Column`; use `@Id` + IDENTITY for `id`, `@Enumerated(EnumType.STRING)` for enum columns, and `BigDecimal` for energy values.
- Keep `sessionId` and `eventId` unique constraints owned by Flyway; do not add relationships or foreign keys because V1 intentionally has none and later repository/service work can use IDs.
- Keep business transition, terminal-state, idempotency, ordering, charger matching, and transaction logic out of entities; those belong to later Application Service work.
- Keep stage-level testing minimal per user direction: compile and run the existing full `./gradlew test` after each sequential implementation; defer broad mapping/integration coverage to the dedicated test phase. Add no speculative test suite or Testcontainers dependency in Task 4.

## Scope IN

- Add the two entities and two enums in the existing domain package.
- Map every PRD field, exact table/column names, nullability, ID generation, enum representation, decimal precision/scale, and timestamp type.
- Add focused tests that persist and reload each entity against PostgreSQL and exercise valid construction plus nullable fields.
- Run Hibernate schema validation and `./gradlew test`; update `docs/TASK.md` Task 4 only after both entity mappings pass.
- Work only on `feature/task-04-domain`, with ChargingSession completed before ChargingEvent.

## Scope OUT (Must NOT have)

- No Repository, Application Service, Kafka message, Controller, DTO, API, state-transition workflow, idempotency, ordering, retry/DLT, or database migration changes.
- No new dependency, PostgreSQL native enum, foreign key, relationship mapping, speculative index, auditing framework, or schema alteration.
- No additional domain model, status, event type, or state beyond the PRD.

## Open questions

None. The user selected `Instant`, and the sequential branch/order and minimal stage-level testing policy are fixed.

## Approval gate
status: awaiting-approval
approach: Implement ChargingSession and ChargingSessionStatus first, then ChargingEvent and ChargingEventType on `feature/task-04-domain`; use Instant, string enums, identity IDs, BigDecimal, explicit V1 column mappings, minimal per-stage full-test checks, and one final mapping verification before updating Task 4.
next-action: After explicit approval, create `.omo/plans/task-04-domain-implementation.md`, run the mandatory plan-gap review, and write the sequential implementation todos. Execution remains a separate `$start-work` session.
<!-- When exploration is exhausted and unknowns are answered, set status: awaiting-approval. -->
<!-- That durable record is the loop guard: on a later turn read it and resume at the gate instead of re-running exploration. -->
