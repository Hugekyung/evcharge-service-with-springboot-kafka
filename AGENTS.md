# AGENTS.md

## 1. Project Goal

This project is a small Proof of Concept for an EV charging event processing backend.

The primary goal is to demonstrate and practice:

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Kafka
- PostgreSQL
- Kafka Producer / Consumer
- Transaction management
- Idempotent event processing
- Event ordering
- Kafka retry / DLT
- Backend testing

This is intentionally a small project designed to be completed quickly.

Do not expand the project beyond the scope defined in PRD.md.

⸻

## 2. Source of Truth

Before starting implementation, read:

- docs/PRD.md
- docs/TASK.md
- AGENTS.md

Priority when requirements conflict:

AGENTS.md
→ docs/PRD.md
→ docs/TASK.md
→ Existing Code

Do not introduce new requirements unless explicitly requested.

⸻

## 3. Scope

The required system flow is:

Virtual Charger
→ HTTP API
→ Kafka Producer
→ Kafka
→ Kafka Consumer
→ Application Service
→ PostgreSQL

The core domain consists only of:

- ChargingSession
- ChargingEvent

Do not introduce additional domain models unless required by an existing requirement.

⸻

## 4. Technology Constraints

Use:

Java 21
Spring Boot 3.x
Gradle
Spring Web MVC
Spring Validation
Spring Data JPA
Spring Kafka
PostgreSQL
Flyway
Kafka
Docker Compose
JUnit 5

Do not replace these technologies without explicit instruction.

⸻

## 5. Out of Scope

Do NOT implement the following unless explicitly requested:

Frontend
Authentication / Authorization
Redis
Elasticsearch
WebSocket
OCPP integration
Payment
AWS
Kubernetes
Charging Station management
Admin features
Complex pricing
Microservices
CQRS framework
Event Sourcing framework

Do not add infrastructure because it might be useful later.

Optimize for completing the current PoC.

⸻

## 6. Architecture Rules

Use a simple layered architecture.

Recommended structure:

```
com.example.charging
charging
├── controller
├── application
├── domain
├── kafka
├── repository
└── config
```

Responsibilities:

Controller

Responsible for:

HTTP request
Validation
DTO conversion
HTTP response

Must NOT contain:

Business logic
JPA queries
Kafka consumer logic

Kafka Producer

Responsible for:

Kafka message publishing

Must NOT update database state.

Kafka Consumer

Responsible for:

Receiving Kafka messages
Calling Application Service

Keep business logic outside the Consumer.

Application Service

Responsible for:

Business rules
Transaction boundary
Idempotency
Event ordering
Session state transitions

Repository

Responsible for:

Persistence
Database queries

Do not expose JPA implementation details outside the persistence boundary unnecessarily.

⸻

## 7. Transaction Rules

Business transactions must be controlled at the Application Service layer.

Example:

```java
@Transactional
public void process(ChargingEventMessage event) {
// business transaction
}
```

A single Charging Event processing transaction should include:

Idempotency Check
→ Session Read
→ State Transition
→ ChargingEvent Save
→ Session Save
→ Commit

Do not scatter transaction boundaries across Controller, Consumer, and Repository.

⸻

## 8. Kafka Rules

Topic:

charging-events

Use:

sessionId

as the Kafka Message Key.

Reason:

Events belonging to the same Charging Session should be routed to the same Kafka partition when possible.

Do not use random Kafka keys.

⸻

## 9. Idempotency Rules

Kafka consumers must assume that messages can be delivered more than once.

Use:

eventId

as the idempotency key.

Before processing:

Check whether eventId already exists.
Exists
→ Do not apply state transition again.
Does not exist
→ Process event.

The database must also enforce:

UNIQUE(event_id)

Do not rely only on application-level existence checks.

⸻

## 10. Event Ordering Rules

Each Charging Event contains:

sequence

ChargingSession contains:

lastSequence

Before changing Session state:

if incoming.sequence <= session.lastSequence
→ do not apply the state transition

After successful processing:

session.lastSequence = incoming.sequence

Do not assume Kafka ordering alone completely solves event ordering problems.

⸻

## 11. State Transition Rules

Supported events:

CHARGING_STARTED
CHARGING_PROGRESS
CHARGING_COMPLETED
CHARGING_FAILED

Supported Session states:

CHARGING
COMPLETED
FAILED

Expected transitions:

CHARGING_STARTED
→ CHARGING
CHARGING_PROGRESS
→ CHARGING
CHARGING_COMPLETED
→ COMPLETED
CHARGING_FAILED
→ FAILED

Do not create additional states without a requirement.

Terminal states:

COMPLETED
FAILED

Do not silently move terminal states back to CHARGING.

Create a new Session only from CHARGING_STARTED. If the Session does not exist for another event type, treat it as a business error. An event's chargerId must match the Session's chargerId.

⸻

## 12. Retry Rules

Kafka Consumer failures should use Spring Kafka retry mechanisms.

Target behavior:

Processing
→ Failure
→ Retry (2회)
→ DLT

총 처리 시도는 3회(최초 1회 + 재시도 2회)로 한다. DB 연결 실패 같은 일시적 인프라 오류만 Retry 대상으로 하며, 비즈니스 오류는 재시도 없이 DLT로 보낸다.

Use Spring Kafka functionality rather than implementing custom retry loops.

Do not use:

while (...)
Thread.sleep(...)

for retry behavior.

⸻

## 13. Database Rules

Use PostgreSQL.

Schema changes must be managed through Flyway.

Do not depend on Hibernate automatic schema creation as the final database schema management strategy.

Prefer:

ddl-auto: validate

after migrations are available.

Required constraints:

charging_session.session_id UNIQUE
charging_event.event_id UNIQUE

Add indexes only when there is a concrete query path requiring them.

Avoid speculative indexing.

⸻

## 14. API Rules

Required APIs:

POST /api/v1/charging-events
GET /api/v1/charging-sessions/{sessionId}
GET /api/v1/charging-sessions/{sessionId}/events

Do not add CRUD APIs that are not required by the PoC.

The Event POST API should return:

202 Accepted

단, Kafka Broker 발행 성공이 확인된 경우에만 202를 반환한다. 발행 실패 또는 timeout은 5xx로 처리한다.

when the event has been successfully accepted for asynchronous processing.

⸻

## 15. DTO Rules

Do not expose JPA Entities directly through HTTP APIs or Kafka messages.

Use separate models for:

HTTP Request
HTTP Response
Kafka Message
JPA Entity

Java record may be used for immutable DTOs where appropriate.

⸻

## 16. Validation Rules

Validate external HTTP input.

At minimum validate:

eventId
chargerId
sessionId
eventType
sequence
occurredAt

Reject structurally invalid requests before publishing them to Kafka.

Business validation belongs in the Application Service.

⸻

## 17. Error Handling

Distinguish between:

HTTP validation errors
Domain/business errors
Kafka processing errors
Persistence errors

Do not hide unexpected exceptions.

Log enough information to identify:

eventId
sessionId
chargerId
eventType
sequence

Avoid logging unnecessary sensitive information.

⸻

## 18. Testing Rules

Prioritize tests for business behavior rather than framework behavior.

Required test scenarios:

State Transition

STARTED
→ PROGRESS
→ COMPLETED
Expected:
COMPLETED

Idempotency

same eventId
→ process twice
Expected:
state transition applied once

Event Ordering

sequence=1
sequence=3
sequence=2
Expected:
lastSequence=3
sequence=2 does not overwrite current state

Write focused Application Service unit tests first. The final end-to-end flow must include one Testcontainers-based PostgreSQL and Kafka integration test.

When fixing a bug:

1. Reproduce the bug with a test when practical.
2. Implement the fix.
3. Run the relevant tests.

⸻

## 19. Implementation Priority

Always prioritize tasks in this order:

P0 Core Flow
→ P1 Reliability
→ P1 Tests
→ Documentation
→ P2 Optional

Do not work on P2 while P0 is incomplete.

P0 Core Flow:

HTTP
→ Kafka Producer
→ Kafka
→ Kafka Consumer
→ Service
→ PostgreSQL

This flow must work before additional improvements are attempted.

⸻

## 20. Change Scope Rules

Keep changes focused on the current task.

Do not perform unrelated:

Large refactoring
Package restructuring
Dependency upgrades
Naming cleanup across the entire project
Architecture migration
Formatting of unrelated files

If an unrelated issue is discovered:

Document it.
Do not automatically fix it.

unless it blocks the current task.

⸻

## 21. Dependency Rules

Before adding a dependency:

1. Check whether Spring Boot or the JDK already provides the required functionality.
2. Add the dependency only if it directly supports a project requirement.
3. Avoid adding libraries for trivial utilities.

Do not introduce a framework merely to reduce a small amount of code.

⸻

## 22. Code Quality

Prefer:

Simple code
Explicit business rules
Small methods
Clear naming
Constructor injection
Immutable DTOs

Avoid:

Premature abstraction
Generic frameworks
Deep inheritance
Unnecessary interfaces
Utility classes without clear responsibility
Over-engineering

This is a PoC, but the code should still communicate production-oriented backend design decisions.

⸻

## 23. Verification

After meaningful implementation changes, run relevant verification.

At minimum before declaring the task complete:

./gradlew test

When infrastructure is required:

docker compose up -d
./gradlew test

If tests fail:

1. Identify the root cause.
2. Fix only the relevant problem.
3. Run the failed test again.
4. Run the full test suite.

Do not claim completion while required tests are failing.

⸻

## 24. Task Progress

Use docs/TASK.md as the implementation checklist.

실제 경로는 docs/TASK.md다. 각 항목은 코드 작성이 아니라 검증까지 끝난 뒤 완료 처리한다.

After completing and verifying a task:

- [ ] Task

may be changed to:

- [x] Task

Do not mark a task complete merely because code was written.

A task is complete only after its expected behavior has been verified.

⸻

## 25. Definition of Done

The core implementation is complete when this flow works:

POST Charging Event
→ Kafka Publish
→ Kafka Consume
→ Idempotency Check
→ Sequence Check
→ ChargingSession Update
→ ChargingEvent Save

And the following can be verified:

GET ChargingSession
GET ChargingEvent History

새로운 eventId인 오래된 sequence 이벤트는 이력에 저장하되 Session 상태는 바꾸지 않는다. 이미 존재하는 eventId는 저장하지 않고 종료한다.

Final project completion additionally requires:

Idempotency
Event Ordering
Retry / DLT
Core Tests
README
./gradlew test PASS

⸻

## 26. Agent Behavior

## 26.1 Branch Workflow

Each TASK must be implemented on a feature branch created from `main`.

1. Start from the latest `main` branch.
2. Create a feature branch named for the task, such as `feature/task-06-event-api`.
3. Perform and verify the work on that feature branch.
4. Do not implement TASK changes directly on `main`.

When all requested work is complete, briefly report the core work performed and its verification result.

When implementing a task:

Read requirement
→ Inspect relevant existing code
→ Make the smallest coherent change
→ Verify
→ Update docs/TASK.md

Before modifying existing architecture, inspect the current implementation first.

Do not rewrite working code simply because another implementation is preferred.

When requirements are ambiguous:

Prefer the simplest implementation
that satisfies docs/PRD.md
without expanding project scope.

If a decision could significantly change the architecture or project scope, stop and report the decision instead of making a speculative large change.
