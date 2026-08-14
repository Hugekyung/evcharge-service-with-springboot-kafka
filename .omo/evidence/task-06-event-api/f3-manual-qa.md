# Task 6 F3 Manual QA

Overall verdict: PASS

The real test surface was the Spring MVC boundary exercised through `MockMvc`, with the publisher replaced by the test seam that explicitly acknowledges or fails publication. The focused command passed and covered the required 202, 400, and 5xx behaviors. The full `./gradlew test --no-daemon` command initially hit a Gradle XML-report write error, then passed unchanged on an immediate rerun; this transient first failure is recorded rather than omitted.

## surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| F3-S1 | Task 6: successful request returns 202 only after publisher acknowledgement | POST `/api/v1/charging-events` via Spring `MockMvc` | `./gradlew test --no-daemon --tests com.example.charging.controller.ChargingEventControllerTest --tests com.example.charging.application.ChargingEventPublisherContractTest` | PASS | `f3-focused-output` |
| F3-S2 | Task 6: structurally invalid HTTP input returns 400 and is not published | Same POST boundary via `MockMvc` | `./gradlew test --no-daemon --tests com.example.charging.controller.ChargingEventControllerTest --tests com.example.charging.application.ChargingEventPublisherContractTest` | PASS | `f3-focused-output` |
| F3-S3 | Task 6: publisher failure maps to 5xx | Same POST boundary via `MockMvc` with failing publisher seam | `./gradlew test --no-daemon --tests com.example.charging.controller.ChargingEventControllerTest --tests com.example.charging.application.ChargingEventPublisherContractTest` | PASS | `f3-focused-output` |
| F3-S4 | AGENTS verification: full test suite | Gradle test task | `./gradlew test --no-daemon` (rerun after first report-write failure) | PASS | `f3-full-output` |

## adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| F3-A1 | Validation rules | malformed/invalid input | Missing or blank required fields, unknown event type, non-positive sequence, missing/offset-less timestamp, and malformed JSON return 400; publisher receives no command | PASS | `f3-focused-output` |
| F3-A2 | Publish acknowledgement gate | dependency failure | Publisher exception returns 5xx; request is not reported as 202 | PASS | `f3-focused-output` |
| F3-A3 | Test execution reliability | misleading success / transient report failure | A failed full-suite invocation is recorded; rerun must complete successfully before suite is accepted | PASS | `f3-full-output` |

## artifactRefs

| id | kind | description | path |
|---|---|---|---|
| `f3-focused-output` | command-output | Focused controller and publisher contract test output and scenario coverage | `.omo/evidence/task-06-event-api/f3-focused-mockmvc-output.txt` |
| `f3-full-output` | command-output | Full Gradle test first failure and successful rerun output | `.omo/evidence/task-06-event-api/f3-full-gradle-output.txt` |
| `f3-cleanup` | scope-check | Post-run process and shared-worktree scope check | `.omo/evidence/task-06-event-api/f3-cleanup-and-scope.txt` |

Cleanup: no application or Gradle test process remained after the run. No product source files were edited by QA.
