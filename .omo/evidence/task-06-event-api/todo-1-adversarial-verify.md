# Task 6 Todo 1 — adversarial verification

Verdict: NEEDS-FIX

## Original intent

Define one immutable eight-field HTTP request DTO and reject malformed required input at the real Spring MVC boundary before the publishing port is called. Keep HTTP models separate from JPA entities and avoid undocumented battery/energy ranges.

## Observed result

- `ChargingEventRequest` is a Java record with exactly eight components: `eventId`, `chargerId`, `sessionId`, `eventType`, `sequence`, `batteryLevel`, `chargedKwh`, `occurredAt`.
- Required annotations are present: `@NotBlank` on the three IDs, `@NotNull` on `eventType` and `occurredAt`, and `@Positive` on primitive `long sequence`.
- `batteryLevel` is `Integer` and `chargedKwh` is `BigDecimal`; neither has a validation annotation. Direct scans found no `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`, `@PositiveOrZero`, or `@Negative` rule.
- `occurredAt` is `Instant`. The real MVC slice accepts `2026-08-12T12:00:00+09:00` and the publisher observes `2026-08-12T03:00:00Z`.
- `@Valid @RequestBody` applies Bean Validation before the controller invokes the publisher.
- Direct import scans found no `ChargingEvent`/`ChargingSession` entity, JPA, or repository import in the DTO/controller. `ChargingEventType` is the domain enum, not an entity.

## Automated verification

Fresh command:

`./gradlew test --tests '*ChargingEventControllerTest' --tests '*ChargingEventPublisherContractTest'`

Result: exit 0, `BUILD SUCCESSFUL`. Fresh XML proves real execution rather than `NO-SOURCE`:

- `build/test-results/test/TEST-com.example.charging.controller.ChargingEventControllerTest.xml`: 12 tests, 0 failures, 0 errors, 0 skipped.
- `build/test-results/test/TEST-com.example.charging.application.ChargingEventPublisherContractTest.xml`: 2 tests, 0 failures, 0 errors, 0 skipped.
- Captured output: `.omo/evidence/task-06-event-api/todo-1-adversarial-focused-final.txt`.

Flake rerun:

`./gradlew test --tests '*ChargingEventControllerTest'`

Result: exit 0, `BUILD SUCCESSFUL` in 1s; elapsed 1.68s. Capture: `.omo/evidence/task-06-event-api/todo-1-adversarial-controller-rerun.txt`.

## Manual QA matrix — MockMvc boundary

| Scenario | Executed observation | Verdict |
| --- | --- | --- |
| Valid eight-field PRD JSON with offset | HTTP 202; exactly one publisher command; UTC `Instant` asserted | PASS |
| Blank eventId/chargerId/sessionId | HTTP 400; publisher command list empty | PASS |
| Missing and blank eventType | HTTP 400; publisher command list empty | PASS |
| sequence = 0 | HTTP 400; publisher command list empty | PASS |
| sequence < 0 | No executable MockMvc case present | NEEDS-FIX |
| Missing occurredAt | HTTP 400; publisher command list empty | PASS |
| Offset-less occurredAt | HTTP 400; publisher command list empty | PASS |
| Unknown enum | HTTP 400; publisher command list empty | PASS |
| Malformed JSON | HTTP 400; publisher command list empty | PASS |

## Exact gap

The source annotation `@Positive` makes negative sequences invalid in production, so no production defect was observed. However, the stated acceptance/manual-QA criterion requires executable proof for zero/non-positive sequence values. `invalidBoundaryRequests()` contains only `sequence=0`; it never submits a negative sequence. Add one MockMvc invalid request such as `sequence=-1`, assert HTTP 400, and assert no publisher command. Then rerun the focused suite and inspect XML again.

## Adversarial / ULTRAQA

- malformed_input: NEEDS-FIX only for the missing negative-sequence executable case; all other named malformed classes passed.
- stale_state: PASS — source and fresh XML were read after this verifier's commands.
- dirty_worktree: PASS — initial/final inventory recorded; existing modified/untracked files were preserved. This review added evidence files only.
- flaky_tests: PASS — controller suite passed on the required rerun.
- misleading_success_output: PASS — XML counts inspected directly; `processTestResources NO-SOURCE` did not conceal test execution.
- repeated_interruptions: PASS — post-check found no Gradle Test worker or `ChargingApplication` process.
- hung/long commands: N/A — every Gradle command completed in about 2 seconds, under 60 seconds.
- prompt injection: N/A — reviewed local source/tests/evidence only; no untrusted instruction-bearing external content.
- cancel/resume: N/A — no cancellation or resume occurred.

Cleanup receipt command:

`ps -Ao pid=,command= | rg '[g]radle.*Test worker|[C]hargingApplication' || true`

Observed output: empty. No shared Gradle daemon was stopped.

## Slop / programming pass

- DTO/controller production code is small, typed, boundary-focused, and free of entity leakage, duplicated validation, speculative normalization, parsing helpers, or undocumented rules.
- Controller tests assert HTTP status and publisher side effects at the real MVC boundary; they do not mirror DTO implementation.
- NOTE, non-blocking for Todo 1: `ChargingEventPublisherContractTest` constructs lambda behavior and asserts that same lambda behavior, so it provides little independent confidence. It does not invalidate the DTO boundary tests or violate Todo 1's criterion.

## Recommendation

NEEDS-FIX until one negative-sequence MockMvc case proves HTTP 400 and zero publisher calls. The production DTO itself otherwise matches Todo 1.
