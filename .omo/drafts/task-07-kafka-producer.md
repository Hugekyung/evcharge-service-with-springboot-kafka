---
slug: task-07-kafka-producer
status: approved-and-plan-created
intent: clear
review_required: false
pending-action: execute-plan
approach: Implement a concrete Spring Kafka adapter for the existing ChargingEventPublisher port, using a separate ChargingEventMessage DTO, sessionId as the Kafka key, and a bounded synchronous acknowledgement wait.
---

# Draft: task-07-kafka-producer

## Components (topology ledger)
<!-- Lock the SHAPE before depth. One row per top-level component that can succeed or fail independently. -->
<!-- id | outcome (one line) | status: active|deferred | evidence path -->

| message-model | Separate Kafka message DTO mirrors the documented event payload | active | docs/PRD.md:297-322; AGENTS.md:390-401 |
| producer-adapter | Spring bean implements ChargingEventPublisher and publishes to charging-events | active | docs/TASK.md:147-161; src/main/java/com/example/charging/application/ChargingEventPublisher.java |
| acknowledgement | API publisher returns only after broker acknowledgement or throws typed failure | active | docs/PRD.md:219-225; AGENTS.md:380-386 |
| broker-flow | Compose Kafka receives the message with sessionId key and expected JSON | active | docker-compose.yml:19-43; docs/TASK.md:156-161 |

## Open assumptions (announced defaults)
<!-- Record any default you adopt instead of asking, so the user can veto it at the gate. -->
<!-- assumption | adopted default | rationale | reversible? -->

| Kafka DTO | Add `com.example.charging.kafka.ChargingEventMessage` rather than serializing the application command directly | AGENTS.md requires separate HTTP/Kafka models and Task 7 explicitly names the message DTO | yes |
| acknowledgement wait | Call `KafkaTemplate.send(...).get(Duration.ofSeconds(3))` and translate timeout/execution/interruption failures to `ChargingEventPublishException` | Existing Kafka admin operation/request timeout is 3 seconds; preserves API's acknowledgement-gated 202 contract | yes |
| producer bean | `ChargingEventProducer` is the single `@Component` implementation of `ChargingEventPublisher` | Resolves Task 6 missing production bean and keeps Controller unchanged | yes |
| test strategy | Focused Mockito/Spring Kafka producer tests plus one Compose broker smoke path; no broad consumer/integration work | User's lightweight testing preference and Task 7 scope | yes |

## Findings (cited - path:lines)

- `docs/TASK.md:147-161`: Task 7 owns `ChargingEventMessage`, `ChargingEventProducer`, `KafkaTemplate`, topic, key, Controller connection, and publication confirmation.
- `docs/PRD.md:297-322`: topic `charging-events`, key `sessionId`, and eight-field JSON message shape.
- `AGENTS.md:213-229`: same-session events must use `sessionId` as message key; random keys are forbidden.
- `AGENTS.md:390-401`: HTTP/Kafka/JPA models must remain separate.
- `src/main/java/com/example/charging/application/ChargingEventPublisher.java:3-8`: existing synchronous handoff contract returns only after acknowledgement or throws.
- `src/main/java/com/example/charging/application/ChargingEventPublishCommand.java:7-15`: complete source data for mapping to Kafka DTO.
- `src/main/java/com/example/charging/config/KafkaTopicConfig.java:14-33`: existing KafkaAdmin and `charging-events` topic with 3 partitions; admin timeouts are 3 seconds.
- `src/main/java/com/example/charging/config/KafkaTopicInitializer.java:13-68`: topic initialization and listener startup are already implemented; do not redesign them in Task 7.
- `src/main/resources/application.yml:21-34`: localhost Kafka and String/JSON serializers are already configured.
- Current gap: no production `ChargingEventPublisher` bean, no Kafka message DTO, no KafkaTemplate producer, and no consumer yet.

## Decisions (with rationale)

- Use a separate `ChargingEventMessage` record in `com.example.charging.kafka`; map every field explicitly from `ChargingEventPublishCommand`.
- Use `KafkaTemplate<String, ChargingEventMessage>` with `send("charging-events", command.sessionId(), message)`.
- Block only on the send future acknowledgement with a 3-second bound; preserve the interrupted thread flag before throwing `ChargingEventPublishException`.
- Producer only publishes; it must not access repositories, mutate entities, or implement consumer retry/DLT.

## Scope IN

- Kafka message record, producer adapter bean, typed publish failure mapping, and Controller-to-producer wiring through the existing interface.
- Focused producer tests for topic/key/payload/ack success/failure/timeout/interruption.
- One local Docker Compose broker smoke verification and Task 7 completion marker after evidence.

## Scope OUT (Must NOT have)

- Kafka consumer, application service, DB writes, idempotency/order/state logic, retry/DLT, GET APIs, new dependencies, topic redesign, or Testcontainers full-flow test.
- Changes to Task 6 Controller contract unless a compile/runtime wiring issue directly requires it.
- Random Kafka keys, producer-side retry loops, `while`, `Thread.sleep`, or swallowing publish failures.

## Open questions

None. The separate DTO and 3-second acknowledgement bound are reversible implementation defaults grounded in existing requirements/configuration.

## Approval gate
status: approved-and-plan-created
approach: Add `ChargingEventMessage` and `ChargingEventProducer` as the concrete Spring Kafka implementation of the existing publisher port. Verify exact topic/key/payload and acknowledgement/error behavior with focused tests and one Compose smoke flow, then mark Task 7 complete.
next-action: Execute `.omo/plans/task-07-kafka-producer.md` via `$omo:start-work task-07-kafka-producer` on `feature/task-07-kafka-producer`.
<!-- When exploration is exhausted and unknowns are answered, set status: awaiting-approval. -->
<!-- That durable record is the loop guard: on a later turn read it and resume at the gate instead of re-running exploration. -->
