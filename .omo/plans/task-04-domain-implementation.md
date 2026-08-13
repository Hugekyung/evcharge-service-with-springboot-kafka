# task-04-domain-implementation - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** Two JPA domain entities and their enums, implemented sequentially on one Task 4 branch and mapped exactly to the existing charging schema.

**Why this approach:** ChargingSession is implemented first as the session aggregate record, then ChargingEvent; both use `Instant`, string enums, identity IDs, and no business workflow that belongs to later services.

**What it will NOT do:** It will not add repositories, services, APIs, Kafka models, migrations, or broad test suites.

**Effort:** Short
**Risk:** Medium - `ddl-auto: validate` makes exact column and Java type mapping important.
**Decisions to sanity-check:** UTC timestamps use `Instant`; enum columns use stable strings; stage testing stays minimal.

Your next move: Approve the plan, then run `$start-work task-04-domain-implementation`.

---

> TL;DR (machine): Short sequential entity-mapping task; adds four domain types, runs minimal full-test checks, and validates against V1.

## Scope
### Must have
- One branch `feature/task-04-domain` containing both domain implementations in order: ChargingSession first, ChargingEvent second.
- `ChargingSession`, `ChargingSessionStatus`, `ChargingEvent`, and `ChargingEventType` under `com.example.charging.domain`.
- Exact V1 mappings: explicit table/column names, identity-generated Long IDs, BigDecimal NUMERIC(12,3), Instant TIMESTAMPTZ, nullable fields, and string enums.
- Minimal verification after each stage and final `./gradlew test`; update `docs/TASK.md` Task 4 only after mapping verification passes.
### Must NOT have (guardrails, anti-slop, scope boundaries)
- No repositories, services, Kafka/HTTP DTOs, controllers, business transitions, idempotency/order logic, retry/DLT, migration edits, dependencies, relationships, or extra domain types.
- No broad test suite or Testcontainers addition in this task; dedicated testing work comes later.

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: minimal tests-after; run the existing full `./gradlew test` after each sequential entity stage and once at final verification. No new test suite unless a mapping failure requires a focused regression test.
- Evidence: `.omo/evidence/task-04-domain-implementation/` with test output, schema-validation output, and final scope review.

## Execution strategy
### Parallel execution waves
> Target 5-8 todos per wave. Fewer than 3 (except the final) means you under-split.
Wave 1 and Wave 2 are strictly sequential on the same branch. The final verification wave runs after both.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | — | 2 | — |
| 2 | 1 | Final wave | — |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [x] 1. Implement ChargingSession and ChargingSessionStatus
  What to do / Must NOT do: Add only `ChargingSession.java` and `ChargingSessionStatus.java` under `src/main/java/com/example/charging/domain`. Map `charging_session` exactly: identity `Long id`, unique/non-null `sessionId`, non-null `chargerId`, string `status`, nullable `batteryLevel`, nullable `BigDecimal chargedKwh` with precision 12/scale 3, non-null `long lastSequence`, nullable `Instant startedAt`/`completedAt`, non-null `Instant createdAt`/`updatedAt`. Use explicit `@Table`/`@Column`, `@Enumerated(EnumType.STRING)`, and no transition/service logic. Must not change SQL, config, repositories, or other domain types.
  Parallelization: Wave 1 | Blocked by: — | Blocks: 2
  References (executor has NO interview context - be exhaustive): `docs/TASK.md:73-87`; `docs/PRD.md:122-139,164-194,258-274`; `src/main/resources/db/migration/V1__create_charging_tables.sql:1-15`; `src/main/resources/application.yml:11-19`; `AGENTS.md:187-209,282-317`; Hibernate mapping guidance https://docs.hibernate.org/orm/6.5/userguide/html_single/.
  Acceptance criteria (agent-executable): `./gradlew test` exits 0 after the stage; application context/schema validation does not report a `charging_session` mismatch when the existing PostgreSQL migration is present; enum constants are exactly `CHARGING`, `COMPLETED`, `FAILED`; no files outside the allowed entity/enum paths are modified.
  QA scenarios (name the exact tool + invocation): happy: `./gradlew test` with Docker Compose PostgreSQL available and inspect compile output; failure: run a read-only diff/path check proving no repository/service/Kafka/migration files changed and that a deliberately absent required column would be rejected by Hibernate validation (do not modify the real schema). Evidence `.omo/evidence/task-04-domain-implementation/todo-1-session.txt`.
  Commit: Y | `feat(domain): add charging session entity`

- [x] 2. Implement ChargingEvent and ChargingEventType, then verify Task 4
  What to do / Must NOT do: After Todo 1 passes, add only `ChargingEvent.java` and `ChargingEventType.java`. Map `charging_event` exactly: identity `Long id`, unique/non-null `eventId`, non-null `sessionId`, `chargerId`, string `eventType`, non-null `long sequence`, nullable `batteryLevel`, nullable `BigDecimal chargedKwh` precision 12/scale 3, non-null `Instant occurredAt`/`processedAt`. Use explicit names and string enum persistence; do not add a JPA relationship to ChargingSession. Run `./gradlew test`, verify schema mapping, then mark only Task 4 in `docs/TASK.md` `[완료]`.
  Parallelization: Wave 2 | Blocked by: 1 | Blocks: Final wave
  References (executor has NO interview context - be exhaustive): `docs/TASK.md:89-109`; `docs/PRD.md:141-162,258-293`; `src/main/resources/db/migration/V1__create_charging_tables.sql:17-31`; `src/main/resources/application.yml:11-19`; `AGENTS.md:233-255,570-610`.
  Acceptance criteria (agent-executable): `./gradlew test` exits 0; both entities load under `ddl-auto: validate` against V1 without mapping errors; event enum constants are exactly `CHARGING_STARTED`, `CHARGING_PROGRESS`, `CHARGING_COMPLETED`, `CHARGING_FAILED`; `docs/TASK.md` Task 4 is marked `[완료]` only after verification; no unrelated files changed.
  QA scenarios (name the exact tool + invocation): happy: `docker compose up -d && ./gradlew test` and inspect startup/schema-validation output; failure: run `git diff --name-only` and `git diff --check` to reject any migration, repository, service, API, dependency, or unrelated documentation change. Evidence `.omo/evidence/task-04-domain-implementation/todo-2-event-final.txt`.
  Commit: Y | `feat(domain): add charging event entity`

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [x] F1. Plan compliance audit
- [x] F2. Code quality review
- [x] F3. Real manual QA
- [x] F4. Scope fidelity

## Commit strategy

Use the already-created `feature/task-04-domain` branch from current `main`. Keep the two sequential entity changes in separate focused commits if cleanly possible; do not push or open a PR unless requested. Preserve `.omo/` planning state and unrelated worktree changes.

## Success criteria

- Both entities and enums exist under the domain package with exact V1 mappings.
- `Instant`, `BigDecimal`, IDENTITY, and string enum mappings validate successfully.
- ChargingSession is implemented and verified before ChargingEvent begins.
- Minimal stage checks and final `./gradlew test` pass.
- Task 4 is marked complete only after the mapping verification succeeds.
