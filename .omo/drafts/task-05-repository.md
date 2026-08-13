---
slug: task-05-repository
status: awaiting-approval
intent: clear
review_required: false
pending-action: write .omo/plans/task-05-repository.md
approach: Add one Spring Data JPA repository interface for ChargingSession and one for ChargingEvent under the existing repository package. Expose only the required query methods, use entity IDs/fields as the query contract, order event history by sequence ascending, and verify repository startup/query derivation with the smallest practical Gradle test gate.
---

# Draft: task-05-repository

## Components (topology ledger)
<!-- Lock the SHAPE before depth. One row per top-level component that can succeed or fail independently. -->
<!-- id | outcome (one line) | status: active|deferred | evidence path -->
| session-repository | ChargingSession persistence interface exposes `findBySessionId` | active | `docs/TASK.md:113-123`, `docs/PRD.md:229-247`, `src/main/java/com/example/charging/domain/ChargingSession.java` |
| event-repository | ChargingEvent persistence interface exposes `existsByEventId` and ordered session history | active | `docs/TASK.md:113-123`, `docs/PRD.md:250-254`, `src/main/resources/db/migration/V1__create_charging_tables.sql:17-31` |
| repository-verification | Spring Data derives the three queries and the application context starts against V1 | active | `src/main/resources/application.yml:11-19`, `AGENTS.md:570-590` |

## Open assumptions (announced defaults)
<!-- Record any default you adopt instead of asking, so the user can veto it at the gate. -->
<!-- assumption | adopted default | rationale | reversible? -->
| session lookup return | `Optional<ChargingSession> findBySessionId(String sessionId)` | absent sessions are a valid business case handled by the service; Optional avoids null ambiguity | yes |
| duplicate check return | `boolean existsByEventId(String eventId)` | exact TASK requirement and efficient idempotency existence query | yes |
| history return/order | `List<ChargingEvent> findBySessionIdOrderBySequenceAsc(String sessionId)` | PRD history endpoint and existing `(session_id, sequence)` index support deterministic ascending order | yes |
| repository style | interfaces extending `JpaRepository<Entity, Long>` with no custom implementation, query annotation, or extra methods | Spring Data JPA already provides CRUD and query derivation; repository task should remain small | yes |
| test intensity | minimal: run existing full `./gradlew test` and one application-context/query-derivation smoke; do not add a broad repository test suite or Testcontainers | user’s standing direction and dedicated later test phase | yes |

## Findings (cited - path:lines)

- `docs/TASK.md:113-123` requires `ChargingSessionRepository`, `ChargingEventRepository`, `findBySessionId`, `existsByEventId`, session event history, and repository test or application verification.
- `docs/PRD.md:229-254` defines the two read use cases; history is per session and the database index is `(session_id, sequence)` (`docs/PRD.md:291-293`).
- `src/main/java/com/example/charging/domain/` contains the two JPA entities with `Long` generated IDs and scalar `sessionId`/`eventId` fields; no repository convention exists yet.
- `build.gradle:20-33` already includes Spring Data JPA and test dependencies; no dependency addition is required.
- `src/main/resources/application.yml:11-19` enables Hibernate validation and PostgreSQL-backed startup.
- Repository responsibility is persistence/query access only; business rules and transaction boundaries remain in Application Service per `AGENTS.md:187-209` and `AGENTS.md:352-368`.
- Query execution round-trip is intentionally deferred: current domain entities expose no public construction/update API, and the user requested lightweight stage testing. This task's executable proof is repository bean/query derivation during bounded application startup plus `./gradlew test`.

## Decisions (with rationale)

- Add only `ChargingSessionRepository.java` and `ChargingEventRepository.java` under `com.example.charging.repository`.
- Use `JpaRepository<ChargingSession, Long>` and `JpaRepository<ChargingEvent, Long>` to retain standard persistence operations for later services without custom adapters.
- Use derived queries with exact entity property names; do not expose SQL or JPA implementation details outside the repository package.
- Return event history ordered by `sequence ASC`; do not add pagination, occurredAt ordering, native SQL, or speculative methods.
- Do not add repository tests unless the minimal application verification reveals a derivation issue; the later test phase owns exhaustive repository/business tests.

## Scope IN

- Add the two repository interfaces and the four required query capabilities.
- Verify Spring Data repository bean creation, query derivation, and application startup against the existing PostgreSQL schema.
- Run minimal `./gradlew test`; update `docs/TASK.md` Task 5 only after verification passes.
- Work on `feature/task-05-repository` created from current `main`.

## Scope OUT (Must NOT have)

- No entity changes, migration/config/dependency changes, service/controller/API/Kafka implementation, custom repository classes, native SQL, pagination, specifications, projections, or extra query methods.
- No broad tests, Testcontainers, fixtures, seed data, or business logic in repositories.

## Open questions

None. The repository method signatures, ordering, and minimal verification strategy are determined by `docs/TASK.md`, PRD query paths, existing schema index, and the project’s standing lightweight-testing direction.

## Approval gate
status: awaiting-approval
approach: Add two small Spring Data JPA interfaces with Optional session lookup, boolean event-id existence, and ascending sequence history query; verify derived queries through minimal Gradle/application checks and then mark Task 5 complete.
next-action: After explicit approval, create `.omo/plans/task-05-repository.md`, run mandatory Metis plan-gap review, and append sequential executable todos. Execution starts separately with `$start-work`.
<!-- When exploration is exhausted and unknowns are answered, set status: awaiting-approval. -->
<!-- That durable record is the loop guard: on a later turn read it and resume at the gate instead of re-running exploration. -->
