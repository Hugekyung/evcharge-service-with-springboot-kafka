# F3 Manual QA — task-05-repository

Scope: real runtime verification of the repository interfaces on the existing healthy Compose PostgreSQL/Kafka services. No product files were edited by this QA run; Compose services and named volumes were preserved.

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| F3-S1 | Task 5 repository startup/query derivation | Spring Boot runtime | `./gradlew bootRun --args='--server.port=0'` (one bounded 60-second startup wait; stop after `Started ChargingApplication`) | PASS | A2, A3 |
| F3-S2 | Required automated verification | Gradle CLI | `./gradlew test` | PASS | A4, A5 |
| F3-S3 | Runtime prerequisite and cleanup | Docker Compose/process table | `docker compose ps --format 'table {{.Name}}\\t{{.Service}}\\t{{.State}}\\t{{.Status}}'`; final `ps -axo pid=,command= \\| grep '[c]om.example.charging.ChargingApplication'` | PASS | A1, A6 |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| F3-A1 | bounded boot and teardown | hung_or_long | Boot reaches the startup marker within 60 seconds and leaves no app process after termination. | PASS | A2, A6 |
| F3-A2 | repository query derivation | misleading_success_output | Require independent positive markers (`Found 2 JPA repository interfaces`, Flyway, Hibernate, EntityManagerFactory, `Started`) and no `QueryCreationException`. | PASS | A3 |
| F3-A3 | process cleanup | repeated_interruptions | Controlled interrupt/termination leaves no `ChargingApplication` process while Compose services remain healthy. | PASS | A6, A7 |
| F3-A4 | test repeatability signal | flaky_tests | The required Gradle test task exits 0 and reports `BUILD SUCCESSFUL`; the output is labeled `NO-SOURCE`, not behavioral coverage. | PASS | A4, A5 |
| F3-A5 | environment isolation | stale_state | Pre-run process inventory contains no existing `ChargingApplication`; final inventory is empty. | PASS | A8, A6 |
| F3-A6 | worktree/infrastructure safety | dirty_worktree | Preserve existing product/worktree changes and named Compose volumes; QA writes only evidence artifacts. | PASS | A9, A7 |
| F3-A7 | repository-interface-only scope | malformed_input | NOT_APPLICABLE — this change has no external input parser or request boundary to feed malformed input. | NOT_APPLICABLE | A10 |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| A1 | terminal transcript | Compose PostgreSQL/Kafka health before QA | `.omo/evidence/task-05-repository/f3-compose-status.txt` |
| A2 | terminal transcript | Full bounded bootRun output | `.omo/evidence/task-05-repository/f3-bootrun.log` |
| A3 | terminal transcript | Independent startup-marker and QueryCreationException checks | `.omo/evidence/task-05-repository/f3-bootrun-excerpts.txt` |
| A4 | terminal transcript | Full `./gradlew test` output | `.omo/evidence/task-05-repository/f3-gradle-test.log` |
| A5 | command result | Gradle test exit status | `.omo/evidence/task-05-repository/f3-gradle-test-result.txt` |
| A6 | process transcript | Final no-application-process cleanup check | `.omo/evidence/task-05-repository/f3-processes-final.txt` |
| A7 | terminal transcript | Compose health after QA; services remain running | `.omo/evidence/task-05-repository/f3-compose-final.txt` |
| A8 | process transcript | Pre-run application-process inventory | `.omo/evidence/task-05-repository/f3-processes-before.txt` |
| A9 | worktree inventory | Read-only status showing pre-existing changes and QA-only evidence additions | `.omo/evidence/task-05-repository/f3-worktree-inventory.txt` |
| A10 | scope note | Reason malformed-input class is not applicable | `.omo/evidence/task-05-repository/f3-manual-qa.md` |

## Verification notes

- Boot log contains `Found 2 JPA repository interfaces`, Flyway PostgreSQL connection, Hibernate initialization, `Initialized JPA EntityManagerFactory`, and `Started ChargingApplication`.
- Boot log contains no `QueryCreationException`.
- `./gradlew test` exited 0 with `BUILD SUCCESSFUL`; Gradle reports `test NO-SOURCE`.
- No `ChargingApplication` process remains. Kafka and PostgreSQL remain healthy; no Compose volume command was run.
