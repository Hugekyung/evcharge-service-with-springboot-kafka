---
slug: task-06-event-api
status: plan-created
intent: clear
review_required: false
pending-action: write .omo/plans/task-06-event-api.md
approach: Thin HTTP API boundary for POST /api/v1/charging-events, with request validation and a producer/application handoff; Task 6 owns the handoff contract and Task 7 owns the concrete Kafka producer.
---

# Draft: task-06-event-api

## Components (topology ledger)
<!-- Lock the SHAPE before depth. One row per top-level component that can succeed or fail independently. -->
<!-- id | outcome (one line) | status: active|deferred | evidence path -->

| controller-boundary | Validated HTTP request is accepted by a thin controller without DB access | active | docs/TASK.md:127-143; AGENTS.md:132-164 |
| request-contract | Separate request DTO validates required event fields before publish | active | docs/PRD.md:198-225; AGENTS.md:390-420 |
| publish-handoff | Successful broker acknowledgement gates 202; failures become 5xx | active | docs/PRD.md:219-225; docs/TASK.md:136-139 |

## Open assumptions (announced defaults)
<!-- Record any default you adopt instead of asking, so the user can veto it at the gate. -->
<!-- assumption | adopted default | rationale | reversible? -->

| DTO representation | Java record under `com.example.charging.controller.dto` | No DTO convention exists; immutable boundary model matches AGENTS.md:390-401 | yes |
| external time type | `Instant` with ISO-8601 offset input | Existing domain uses `Instant`; PRD permits Instant and requires timezone-bearing JSON | yes |
| validation baseline | nonblank IDs/type, positive sequence, non-null occurredAt; reject before publish | Exact limits are unspecified; this is the minimum documented contract | yes |
| publish failure mapping | generic 5xx through centralized/small exception mapping, without inventing a response schema | Docs require 5xx but no subtype/body contract | yes |

## Findings (cited - path:lines)

- `docs/PRD.md:198-225`: POST contract, eight request fields, 202 only after broker acknowledgement, no DB wait/direct mutation.
- `docs/TASK.md:127-143`: Task 6 owns request DTO, validation, controller, 202/5xx behavior, and controller DB prohibition.
- `docs/TASK.md:147-161`: Task 7 separately owns `ChargingEventMessage`, `ChargingEventProducer`, KafkaTemplate, topic/key, and controller-to-producer wiring.
- `AGENTS.md:370-420`: required API, DTO separation, validation boundary, and no direct DB access.
- `src/main/java/com/example/charging/config/KafkaTopicConfig.java:11-33`: `charging-events` topic already exists; `application.yml:21-34` configures JSON Kafka serialization.
- Repository inspection: controller/kafka/application packages contain only `package-info.java`; no existing controller, producer, message, service, error handler, or tests.

## Decisions (with rationale)

- Keep the controller thin: bind/validate/convert and delegate; no repository/entity access or consumer/business logic.
- Do not add GET endpoints, persistence writes, authentication, or speculative validation limits.
- Verification stays lightweight: focused controller validation/status checks plus `./gradlew test`; full Kafka delivery belongs to Task 7 and later integration testing.

## Scope IN

- `ChargingEventRequest` and HTTP binding/validation.
- `ChargingEventController` for `POST /api/v1/charging-events`.
- The smallest boundary abstraction needed to express the documented acknowledgement-gated handoff, if the owner chooses to include that handoff in Task 6.
- Task 6 checklist update only after verification.

## Scope OUT (Must NOT have)

- Kafka topic/producer implementation, message DTO, or producer wiring if deferred to Task 7.
- Any JPA repository/entity access or DB mutation.
- Consumer, application service state transitions, GET APIs, retry/DLT, authentication, new dependencies, broad integration tests.

## Open questions

1. **Task boundary for the publish handoff (recommended: option A):** Should Task 6 include the minimal producer/application handoff needed to make the API return 202 only after Kafka acknowledgement, or should Task 6 implement only the controller/DTO/validation and defer all producer wiring to Task 7?
   - A (recommended): include only the thin handoff contract and its controller behavior; Task 7 supplies the concrete Kafka producer.
   - B: defer the entire publish path to Task 7; Task 6 verifies only controller binding/validation.
   Why this matters: the documents split these responsibilities, but the 202-after-ack requirement cannot be behaviorally complete without a publish collaborator.

Resolved: user selected option A. The plan includes only the handoff contract; concrete `KafkaTemplate` producer/topic wiring remains Task 7.

## Approval gate
status: approved-and-plan-created
approach: Implement the five todos in `.omo/plans/task-06-event-api.md` on `feature/task-06-event-api`; keep the controller/DTO boundary separate from Task 7's concrete producer and run focused tests plus bounded startup verification.
next-action: execute via `$omo:start-work task-06-event-api` in a task-owned worktree/branch.
<!-- When exploration is exhausted and unknowns are answered, set status: awaiting-approval. -->
<!-- That durable record is the loop guard: on a later turn read it and resume at the gate instead of re-running exploration. -->
