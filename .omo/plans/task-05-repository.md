# task-05-repository - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** Two small Spring Data JPA repositories that provide session lookup, event-id existence checks, and ascending session event history queries.

**Why this approach:** Derived query methods keep persistence code minimal and match the existing entity names/index; no custom SQL or extra abstraction is needed.

**What it will NOT do:** It will not add services, APIs, custom SQL, pagination, new dependencies, or a broad test suite.

**Effort:** Short
**Risk:** Low - query derivation is isolated to existing JPA entities and schema.
**Decisions to sanity-check:** history order is sequence ascending; equal-sequence ties remain unspecified by the current requirements.

Your next move: Run `$start-work task-05-repository` on the task branch.

---

> TL;DR (machine): Short repository-interface task; three derived queries, minimal Gradle/boot verification, and Task 5 checklist update.

## Scope
### Must have
- `ChargingSessionRepository extends JpaRepository<ChargingSession, Long>` with `Optional<ChargingSession> findBySessionId(String sessionId)`.
- `ChargingEventRepository extends JpaRepository<ChargingEvent, Long>` with `boolean existsByEventId(String eventId)` and `List<ChargingEvent> findBySessionIdOrderBySequenceAsc(String sessionId)`.
- Repository beans and all derived query names load successfully under Spring Boot/JPA against the V1 schema.
- Only Task 5 heading changes to `[완료]` after verification.
### Must NOT have (guardrails, anti-slop, scope boundaries)
- No custom implementation, native SQL, `@Query`, pagination, projections, specifications, extra methods, entity/schema/config/dependency changes, or business logic.
- No broad repository test suite, Testcontainers, fixtures, or seed migration.

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: minimal tests-after; run `./gradlew test` and one bounded real application startup with PostgreSQL and Kafka healthy. Because current entities expose no public construction API and the user requested lightweight tests, this task proves repository bean/query derivation and startup, not persistence round-trip behavior; dedicated tests handle that later.
- Evidence: `.omo/evidence/task-05-repository/` with Gradle output, startup log, bean/query signature inspection, and scope snapshot.

## Execution strategy
### Parallel execution waves
> Target 5-8 todos per wave. Fewer than 3 (except the final) means you under-split.
Use one implementation-and-verification todo; splitting two interfaces into artificial tasks would add coordination without value. Final review runs afterward.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | — | Final wave | — |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [x] 1. Add both repository interfaces and verify query derivation
  What to do / Must NOT do: Add only `src/main/java/com/example/charging/repository/ChargingSessionRepository.java` and `ChargingEventRepository.java`. Use exact entity generic types and three required derived methods. History must be ordered by `sequence ASC`; equal-sequence tie order is unspecified and must not be invented. Verify current `main` ancestry is fresh before editing, start both Compose services, run bounded `./gradlew bootRun --args='--server.port=0'`, confirm Flyway and Hibernate startup plus repository bean initialization, run `./gradlew test`, then change only `### 5. Repository 구현` to `### 5. Repository 구현 [완료]`.
  Parallelization: Wave 1 | Blocked by: — | Blocks: Final wave
  References (executor has NO interview context - be exhaustive): `docs/TASK.md:113-123`; `docs/PRD.md:229-254,291-293`; `src/main/java/com/example/charging/domain/ChargingSession.java`; `src/main/java/com/example/charging/domain/ChargingEvent.java`; `src/main/resources/application.yml:11-19`; `src/main/java/com/example/charging/config/KafkaTopicInitializer.java`; `docker-compose.yml`; `AGENTS.md:187-209,570-610,648-653`.
  Acceptance criteria (agent-executable): exactly two repository source files exist; signatures are `Optional<ChargingSession> findBySessionId(String)`, `boolean existsByEventId(String)`, and `List<ChargingEvent> findBySessionIdOrderBySequenceAsc(String)`; no custom query/implementation or extra method exists; `./gradlew test` exits 0; bounded boot reaches `Started ChargingApplication` with no repository query-derivation exception; Task 5 heading is `[완료]`; `git diff --check` exits 0.
  QA scenarios (name the exact tool + invocation): happy: `docker compose up -d && timeout 60 ./gradlew bootRun --args='--server.port=0' > .omo/evidence/task-05-repository/boot.log 2>&1`, assert log contains `Started ChargingApplication` and no `QueryCreationException`, then stop the process and run `./gradlew test`; failure: run a read-only source/signature scan rejecting missing/extra methods and inspect boot exit/log for a deliberately unavailable Kafka/PostgreSQL disposable run, restoring Compose health afterward. Evidence `.omo/evidence/task-05-repository/todo-1-repository.txt`.
  Commit: Y | `feat(repository): add charging event repositories`

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [x] F1. Plan compliance audit
- [x] F2. Code quality review
- [x] F3. Real manual QA
- [x] F4. Scope fidelity

## Commit strategy

Use `feature/task-05-repository`, created from the latest `main`; do not push or open a PR unless requested. Keep repository interfaces and the verified Task 5 marker in one focused commit. Preserve unrelated `.omo` state.

## Success criteria

- Both repository interfaces expose exactly the three required derived queries.
- Application startup proves Spring Data bean creation and query derivation against V1.
- `./gradlew test` and `git diff --check` pass.
- Task 5 is marked complete only after verification; persistence round-trip coverage remains for the dedicated test phase.
