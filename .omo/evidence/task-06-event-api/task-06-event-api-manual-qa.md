# Task6 Manual QA Matrix

Run date: 2026-08-14 Asia/Seoul
Source SHA: `7da01ab90b48462b3283950de4c4407939109f9a`
Scope: focused tests only; no product edits or broad test suite.

## surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| T6-S1 | docs/TASK.md §6: successful publish returns 202 | Spring MockMvc HTTP POST controller surface | `./gradlew test --no-daemon --tests com.example.charging.controller.ChargingEventControllerTest --tests com.example.charging.application.ChargingEventPublisherContractTest` | PASS | `A1`, `A2` |
| T6-S2 | docs/TASK.md §6: validation rejects invalid request | Spring MockMvc HTTP POST controller surface | same focused invocation; invalid-field parameterized cases and malformed/unknown/timestamp cases | PASS | `A1`, `A2` |
| T6-S3 | docs/TASK.md §6: publisher failure maps to 5xx | Spring MockMvc HTTP POST controller surface | same focused invocation; `postReturns5xxWhenPublisherFails` | PASS | `A1`, `A2` |
| T6-S4 | focused-test completion and cleanup | Gradle test runner/process surface | same focused invocation, then `ps aux | rg 'Gradle Test Executor|ChargingApplication' | rg -v rg` | PASS | `A1`, `A2` |

## adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| T6-A1 | docs/TASK.md §16–§17 | malformed input | HTTP 400 and publisher is not called | PASS | `A1`, `A2` |
| T6-A2 | docs/TASK.md §6 | dependency/publisher failure | HTTP 5xx; failure is not reported as accepted | PASS | `A1`, `A2` |
| T6-A3 | docs/TASK.md §6 | misleading success output | XML must independently show 15 tests, zero skipped/failures/errors | PASS | `A1` |
| T6-A4 | execution safety | stale state / flaky rerun | fresh focused run completes successfully with current XML | PASS | `A1` |
| T6-A5 | execution safety | hung process / cleanup | no test worker or application process remains after run | PASS | `A1` |
| T6-A6 | docs/TASK.md §6 | browser UI | Not applicable: Task6 exposes an HTTP API and has no browser UI surface | NOT_APPLICABLE | `A1` |

## artifactRefs

| id | kind | description | path |
|---|---|---|---|
| A1 | command-transcript | Fresh focused Gradle invocation, XML result counts, behavior mapping, and cleanup probe | `.omo/evidence/task-06-event-api/global-qa-focused-run.txt` |
| A2 | test-source | MockMvc scenarios proving 202/400/5xx and no-publish-on-validation behavior | `src/test/java/com/example/charging/controller/ChargingEventControllerTest.java` |

Overall verdict: PASS. Every PASS has a non-empty artifact reference. No product files were changed by this QA run.
