# Task 6 Global Security Review

Reviewed SHA: `7da01ab90b48462b3283950de4c4407939109f9a` (the Task 6 sources are untracked in the shared dirty worktree, so they were inspected directly and their source hashes recorded below).

Security verdict: **FAIL**. This is a narrow HTTP boundary review; the one finding is LOW severity, but the requested PASS condition requires no security issue.

## Task

Review Task 6's `POST /api/v1/charging-events` boundary for secrets, unsafe deserialization/configuration, injection, exception exposure, and Task 6/7 scope violations.

## Deliverable

This SHA-bound report and a `review-lane` ledger record with lane `security`. No product files were modified.

## Scope

Directly reviewed the Task 6 product/test additions:

- `src/main/java/com/example/charging/controller/ChargingEventController.java`
- `src/main/java/com/example/charging/controller/dto/ChargingEventRequest.java`
- `src/main/java/com/example/charging/application/ChargingEventPublish{Command,Exception,Publisher}.java`
- `src/test/java/com/example/charging/controller/ChargingEventControllerTest.java`
- `src/test/java/com/example/charging/application/ChargingEventPublisherContractTest.java`
- Task 6 completion marker at `docs/TASK.md:127`

The dirty-worktree inventory also contains other agents' Task 6 evidence/state files and the untracked product sources. No commit-range diff can represent those untracked sources; the given SHA is the required base binding. `git diff --check` passed for tracked changes.

## Verify

Source review found no committed secret, credential, token, SQL/JPA/repository use, process execution, dynamic type loading, Kafka deserializer/configuration change, or unsafe deserialization in the Task 6 delta. The MVC binding uses the typed record at `ChargingEventRequest.java:10-18`; malformed JSON, unknown enum values, missing required fields, and offset-less timestamps are exercised by the controller boundary tests at `ChargingEventControllerTest.java:86-155`.

`ChargingEventController.java:41-53` catches only the typed publishing exception and returns an empty 503 response, so broker failure details are logged server-side rather than exposed to the client. No concrete Kafka publisher, database access, consumer, schema change, or retry loop is introduced; these remain Task 7 work.

Required skill-perspective check: **ran**. `remove-ai-slops` found no needless production extraction/parsing/normalization, deletion-only test, or broad catch. It does flag `ChargingEventPublisherContractTest.java:14-30` as tautological/lambda-mirroring test coverage (MEDIUM test-quality concern, not a security issue or Task 6 blocker because the MVC behavior is independently tested). `programming` criteria were consulted generically for boundary typing, narrow error handling, and test relevance; no untyped escape hatch or needless production abstraction violates that perspective. Its language-specific implementation references do not apply because this review changes no Python/Rust/TypeScript/Go code.

## Manual-QA

Ran the required real boundary command after forcing execution to avoid stale Gradle state:

```text
./gradlew test --no-daemon --rerun-tasks --tests com.example.charging.controller.ChargingEventControllerTest
BUILD SUCCESSFUL in 4s
4 actionable tasks: 4 executed
```

The generated JUnit XML, `build/test-results/test/TEST-com.example.charging.controller.ChargingEventControllerTest.xml:2`, reports `tests="13"`, `failures="0"`, and `errors="0"`. This covers accepted acknowledgement, publisher failure to 5xx, required-field and sequence validation, unknown event type, offset-less timestamp, and malformed JSON without a publish call.

## UltraQA

- Dirty worktree: PASS — recorded above; untracked Task 6 sources were reviewed directly rather than relying on an empty commit diff.
- Stale state / misleading success output: PASS — an earlier up-to-date task result was not accepted as execution; `--rerun-tasks` executed the test class and JUnit XML supplied independent zero-failure/error counts.
- Malformed input: PASS — direct MockMvc coverage at `ChargingEventControllerTest.java:143-155`.
- Hung/long command: PASS — bounded forced run completed in 5.2 seconds.
- Repeated interruption: N/A — no review command was interrupted or retried after interruption.
- Prompt injection: N/A — the review consumed local code and project requirements, not untrusted instructions embedded in external content.
- Cancel/resume: N/A — this review was neither cancelled nor resumed.

## Artifact/Cleanup

### CRITICAL

None.

### HIGH

None.

### MEDIUM

None for security. Test-quality note: `ChargingEventPublisherContractTest.java:14-30` only confirms the behaviour hard-coded in its own lambda; it should be replaced or removed when Task 7 introduces the concrete publisher. This does not affect the observed MVC boundary coverage.

### LOW

1. Log-forging input is possible. `ChargingEventRequest.java:11-13` accepts any non-blank `eventId`, `chargerId`, and `sessionId`, including newline/control characters. `ChargingEventController.java:45-52` writes those attacker-controlled values directly to the error log. A failed publish therefore lets a client create forged multi-line log entries. Fix by constraining identifier format at the HTTP boundary (for example, a project-approved identifier pattern that excludes control characters) or by safely encoding/sanitizing structured log fields before logging.

No reviewer-owned application, test executor, or Gradle process remains. A pre-existing Gradle daemon (PID 68010) was present before and after this review and was not created or terminated by the reviewer. No infrastructure was started.

Source SHA-256 inventory:

```text
bd77590a0e406efcee27c6951064d3730bdbdc203f2779f5c3b638778cd39584  ChargingEventPublishCommand.java
648c6d4f692b6fa4deb59b49cc329dee4f788a72b44936b2db880596e12fe9c7  ChargingEventPublishException.java
811bd49c7f7b44896433a2b00dca6427dc64179d2bab4b4ba0875b16435a8517  ChargingEventPublisher.java
6f391b73635bc521a46c2fd45ff8e3eac9b96fc924527a5827bdb9718f74468d  ChargingEventController.java
67e9c18abc86638ada75b61096ab5d4127a7f27b50665307fc6d94b363b8173f  ChargingEventRequest.java
362b5905ff1afcb07e6593fb417be8fe35806f15ce7a72a7416dcb3ec2f9c39a  ChargingEventControllerTest.java
```
