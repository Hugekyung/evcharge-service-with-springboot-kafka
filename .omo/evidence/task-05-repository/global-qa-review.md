# Task 5 global hands-on QA review

## SHA-bound verdict

- Reviewed `HEAD`: `d34226f87b36490f85b514208aa13e05a05b87b1`
- Verdict: **PASS**
- Product edits by this QA: none
- Runtime cleanup: complete; no `ChargingApplication` process remains

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| GQA-S1 | Task 5 repository query derivation | Spring Boot runtime | `./gradlew bootRun --args='--server.port=0'`; stop after `Started ChargingApplication` | PASS | R1 |
| GQA-S2 | Task 5 required verification | Gradle CLI | `./gradlew test` | PASS | R1 |
| GQA-S3 | runtime prerequisite and cleanup | Docker Compose/process table | `docker compose ps ...`; `ps -axo pid=,command= | rg '[c]om.example.charging.ChargingApplication' || true` | PASS | R1 |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| GQA-A1 | bounded startup/teardown | hung_or_long | Startup reaches its marker within the bounded interactive run and teardown leaves no app process. | PASS | R1 |
| GQA-A2 | Spring Data derivation | misleading_success_output | Independent raw output contains repository scan, Flyway, Hibernate/EntityManagerFactory, and Started markers, with no query/bean creation exception. | PASS | R1 |
| GQA-A3 | repeatable command result | flaky_tests | `./gradlew test` exits successfully and reports `BUILD SUCCESSFUL`; `NO-SOURCE` is recorded honestly. | PASS | R1 |
| GQA-A4 | environment isolation | stale_state | No app process exists before or after; Compose dependencies are healthy. | PASS | R1 |
| GQA-A5 | worktree/infrastructure safety | dirty_worktree | Existing worktree state and named Compose services remain intact; QA writes evidence only. | PASS | R1 |
| GQA-A6 | repository-only change | malformed_input | No external input parser or request boundary is introduced by repository interfaces. | NOT_APPLICABLE | R1 |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| R1 | terminal transcript and runtime receipt | Fresh bounded bootRun, repository-discovery markers, Gradle result, pre/post process checks, and Compose health | `.omo/evidence/task-05-repository/global-qa-runtime-evidence.md` |

The PASS is bound to the source SHA above and independently reproduced from the live runtime; no prior executor claim was accepted without re-running the commands.
