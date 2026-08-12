# EV Charging Event Platform - PRD

## 1. 프로젝트 개요

### 1.1 프로젝트명

ev-charging-event-platform

### 1.2 목적

전기차 충전 플랫폼의 실시간 이벤트 처리 구조를 학습하고 구현하기 위한 `Spring Boot 기반 PoC` 프로젝트다.

가상의 충전기가 발생시키는 충전 상태 이벤트를 HTTP API에서 수신하고 Kafka를 통해 비동기로 처리한다. Kafka Consumer는 이벤트를 기반으로 충전 Session 상태를 관리하고 PostgreSQL에 이벤트 이력과 현재 Session 상태를 저장한다.

단순 CRUD 구현보다 다음 백엔드 설계 문제를 직접 다루는 것을 핵심 목표로 한다.

- Spring Boot 기반 API 서버 구성
- Kafka Producer / Consumer 기반 비동기 이벤트 처리
- PostgreSQL + Spring Data JPA 기반 영속성 관리
- Transaction 경계 관리
- 중복 이벤트에 대한 멱등성 보장
- 이벤트 순서 역전에 대한 방어
- Consumer 처리 실패에 대한 Retry / DLT
- Docker Compose 기반 로컬 실행 환경 구성
- 핵심 비즈니스 규칙 테스트

⸻

## 2. 핵심 아키텍처

```
Virtual Charger
      |
      | HTTP
      v
ChargingEventController
      |
      v
ChargingEventProducer
      |
      | Kafka
      v
+-------------------------+
| charging-events Topic   |
+-------------------------+
      |
      v
ChargingEventConsumer
      |
      v
ChargingSessionService
      |
      +---- Idempotency Check
      |
      +---- Sequence Check
      |
      +---- Session State Transition
      |
      v
PostgreSQL
      |
      +---- charging_session
      |
      +---- charging_event
```

HTTP 요청 처리와 실제 충전 상태 변경을 분리한다.

`HTTP Request`
→ Validation
→ Kafka Publish
→ 202 Accepted

`Kafka Consumer`
→ Event Validation
→ Idempotency Check
→ Sequence Check
→ Session State Update
→ Event History Save
→ Transaction Commit

⸻

## 3. 기술 스택

Backend

- Java 21
- Spring Boot 3.x
- Gradle

Spring

- Spring Web MVC
- Spring Validation
- Spring Data JPA
- Spring Kafka

Database

- PostgreSQL
- Flyway

Messaging

- Apache Kafka

Test

- JUnit 5
- Spring Boot Test
- 필요 시 spring-kafka-test / Embedded Kafka 또는 Testcontainers

Local Infrastructure

- Docker Compose
- PostgreSQL
- Kafka

⸻

## 4. 핵심 도메인

### 4.1 Charging Session

하나의 충전 시작부터 종료까지의 `상태를 관리`한다.

주요 속성:

sessionId
chargerId
status
batteryLevel
chargedKwh
lastSequence
startedAt
completedAt
createdAt
updatedAt

### 4.2 Charging Event

충전기가 발생시킨 `개별 이벤트`를 의미한다.

주요 속성:

eventId
sessionId
chargerId
eventType
sequence
batteryLevel
chargedKwh
occurredAt
processedAt

### 4.3 Charging Event Type

CHARGING_STARTED
CHARGING_PROGRESS
CHARGING_COMPLETED
CHARGING_FAILED

### 4.4 Charging Session Status

CHARGING
COMPLETED
FAILED

⸻

## 5. 상태 전이

기본적인 상태 흐름은 다음과 같다.

CHARGING_STARTED
→ CHARGING
CHARGING_PROGRESS
→ CHARGING
CHARGING_COMPLETED
→ COMPLETED
CHARGING_FAILED
→ FAILED

종료 상태인 COMPLETED, FAILED 이후 발생하는 이벤트에 대해서는 임의로 상태를 되돌리지 않는다.

PoC에서는 복잡한 복구 정책보다 명확한 상태 전이 규칙을 우선한다.

추가 상태 전이 규칙:

- 새 Session은 `CHARGING_STARTED`에서만 생성한다.
- Session이 없는 상태에서 `CHARGING_PROGRESS`, `CHARGING_COMPLETED`, `CHARGING_FAILED`가 오면 비즈니스 오류로 처리한다.
- 이벤트의 `chargerId`는 Session의 값과 일치해야 한다.
- `COMPLETED`, `FAILED` 이후 이벤트는 Session 상태를 변경하지 않는다.

⸻

## 6. API

### 6.1 충전 이벤트 전송

`POST /api/v1/charging-events`

Request:

```json
{
  "eventId": "evt-100001",
  "chargerId": "charger-001",
  "sessionId": "session-001",
  "eventType": "CHARGING_STARTED",
  "sequence": 1,
  "batteryLevel": 35,
  "chargedKwh": 0,
  "occurredAt": "2026-08-12T12:00:00+09:00"
}
```

Response:

202 Accepted

API 서버는 충전 Session DB를 직접 변경하지 않는다.

요청 검증 후 Kafka Broker 발행 성공이 확인되면 `202 Accepted`를 응답한다. Kafka 발행 실패 또는 timeout은 `5xx`로 처리한다. DB의 비동기 이벤트 처리 완료까지 기다리지는 않는다.

⸻

### 6.2 충전 Session 조회

`GET /api/v1/charging-sessions/{sessionId}`

Response Example:

```json
{
  "sessionId": "session-001",
  "chargerId": "charger-001",
  "status": "CHARGING",
  "batteryLevel": 55,
  "chargedKwh": 12.5,
  "lastSequence": 3,
  "startedAt": "2026-08-12T12:00:00",
  "completedAt": null
}
```

⸻

### 6.3 충전 Event History 조회

`GET /api/v1/charging-sessions/{sessionId}/events`

특정 충전 Session에서 처리된 이벤트 목록을 반환한다.

⸻

## 7. Database

### 7.1 charging_session

```sql
id BIGSERIAL PK
session_id VARCHAR UNIQUE NOT NULL
charger_id VARCHAR NOT NULL
status VARCHAR NOT NULL
battery_level INTEGER
charged_kwh NUMERIC(12,3)
last_sequence BIGINT NOT NULL
started_at TIMESTAMPTZ
completed_at TIMESTAMPTZ
created_at TIMESTAMPTZ NOT NULL
updated_at TIMESTAMPTZ NOT NULL
```

## 7.2 charging_event

```sql
id BIGSERIAL PK
event_id VARCHAR UNIQUE NOT NULL
session_id VARCHAR NOT NULL
charger_id VARCHAR NOT NULL
event_type VARCHAR NOT NULL
sequence BIGINT NOT NULL
battery_level INTEGER
charged_kwh NUMERIC(12,3)
occurred_at TIMESTAMPTZ NOT NULL
processed_at TIMESTAMPTZ NOT NULL
```

event_id에는 UNIQUE Constraint를 설정한다.

Event History 조회를 위해 `charging_event(session_id, sequence)` 조합 인덱스를 추가한다. `session_id`, `event_id`의 UNIQUE Constraint가 만드는 인덱스는 중복 생성하지 않는다. Java 시간 타입은 `Instant` 또는 `OffsetDateTime`, 외부 JSON은 시간대가 포함된 ISO-8601 형식을 사용한다.

⸻

## 8. Kafka

Topic

charging-events

Message Key

sessionId

동일 Charging Session에 속한 이벤트가 동일한 Kafka Partition으로 전달되도록 sessionId를 Message Key로 사용한다.

Message:

```json
{
  "eventId": "evt-100001",
  "chargerId": "charger-001",
  "sessionId": "session-001",
  "eventType": "CHARGING_STARTED",
  "sequence": 1,
  "batteryLevel": 35,
  "chargedKwh": 0,
  "occurredAt": "2026-08-12T12:00:00+09:00"
}
```

⸻

## 9. 멱등성

Kafka 메시지는 중복 전달될 가능성이 있다는 전제로 Consumer를 구현한다.

eventId를 멱등키로 사용한다.

Kafka Event
→ eventId 존재 여부 확인

1. 이미 존재
   → 처리 종료

2. 존재하지 않음
   → Session Update
   → Event 저장

Application 레벨 검증뿐만 아니라 다음 DB Constraint를 사용한다.

`UNIQUE(event_id)`

따라서 중복 이벤트 처리를 다음 두 계층에서 방어한다.

Application Idempotency Check

- Database UNIQUE Constraint

⸻

## 10. 이벤트 순서 처리

충전기가 발생시킨 이벤트에는 증가하는 sequence가 존재한다고 가정한다.

예:

sequence=1 CHARGING_STARTED
sequence=2 CHARGING_PROGRESS
sequence=3 CHARGING_PROGRESS
sequence=4 CHARGING_COMPLETED

ChargingSession은 마지막 처리 sequence를 저장한다.

lastSequence

Consumer는 다음 조건을 검증한다.

incoming.sequence <= session.lastSequence
→ 오래된 이벤트로 판단
→ Event History에는 저장하되 Session 상태 변경하지 않음

이미 존재하는 `eventId`는 저장하지 않고 처리를 종료한다. 새로운 `eventId`지만 오래된 sequence인 이벤트는 이력에 저장한다.

실제 서비스에서는 이벤트 종류와 운영 정책에 따라 별도 저장, 재처리, reconciliation 등의 전략이 필요할 수 있다.

⸻

## 11. Transaction

Kafka Consumer에서 하나의 이벤트를 처리하는 작업을 하나의 Transaction으로 관리한다.

Event Validation
→ Session 조회
→ Session 상태 변경
→ Event History 저장
→ Commit

Application Service를 Transaction Boundary로 사용한다.

```java
@Transactional
public void process(ChargingEventMessage event) {
// ...
}
```

Session 상태 변경과 Event History 저장이 하나의 DB Transaction 안에서 처리되도록 한다.

⸻

## 12. Kafka Retry / DLT

Consumer 처리 중 일시적인 장애가 발생할 수 있다.

예:

Kafka Event
→ Consumer
→ Database Connection Failure

처리 전략:

Consumer Failure
→ Retry
→ Retry
→ DLT

Spring Kafka의 Retry Topic 기능을 활용한다.

예상 정책:

attempts: 3 (최초 1회 + 재시도 2회)
initial delay: 1 second
backoff: exponential

최종적으로 처리되지 않은 이벤트는 DLT로 전달한다.

DB 연결 실패 같은 일시적 장애만 Retry 대상으로 하고, 잘못된 상태 전이나 존재하지 않는 Session 같은 비즈니스 오류는 재시도 없이 DLT로 보낸다. Spring Kafka `@RetryableTopic`을 사용하며 재시도 루프를 직접 구현하지 않는다.

PoC에서는 DLT 이벤트를 로그로 남긴다.

실서비스에서는 다음과 같은 후속 처리를 고려할 수 있다.

- 운영 알림
- 관리자 확인
- 수동 재처리
- 자동 재처리
- 장애 이벤트 저장

⸻

## 13. Architecture Decisions

### ADR-001. HTTP 요청에서 DB를 직접 변경하지 않는 이유

충전 이벤트 수신과 상태 변경 처리를 Kafka를 통해 분리한다.

이를 통해 이벤트 발생 속도와 DB 처리 속도를 느슨하게 결합할 수 있다.

또한 Consumer 확장, Retry, 장애 격리 등의 이벤트 기반 처리 전략을 적용하기 쉬워진다.

⸻

### ADR-002. Kafka Message Key로 sessionId를 사용하는 이유

동일 Charging Session 이벤트를 동일 Partition으로 전달하기 위해 sessionId를 Kafka Message Key로 사용한다.

Kafka Partition 내부의 메시지 순서 보장 특성을 활용하기 위한 설계다.

단, Kafka Partition의 순서 보장만으로 모든 순서 문제를 해결할 수 있다고 가정하지 않는다.

Application에서도 sequence를 관리한다.

⸻

### ADR-003. 동일 이벤트가 두 번 들어오는 경우

Kafka Consumer는 동일 메시지를 다시 처리할 가능성이 있다.

따라서 eventId를 멱등키로 사용한다.

Application Check

- Database UNIQUE Constraint

두 계층을 통해 중복 상태 변경을 방지한다.

⸻

### ADR-004. 이벤트 순서가 뒤바뀌는 경우

각 충전 Event는 증가하는 sequence 값을 가진다.

ChargingSession에 lastSequence를 저장하고 현재 값 이하의 sequence가 들어오면 오래된 이벤트로 판단한다.

⸻

### ADR-005. Consumer 처리 실패

일시적인 장애는 Retry를 수행한다.

정해진 Retry 횟수를 초과하면 DLT로 이벤트를 전달한다.

PoC에서는 DLT 이벤트를 로그로 기록하며 실서비스에서는 별도의 재처리 정책이 필요하다.

⸻

### ADR-006. 충전기 연결이 끊어진 경우

단순 네트워크 연결 여부만으로 실제 충전 상태를 즉시 변경해서는 안 된다.

실서비스에서는 마지막 충전기 이벤트 시간, Heartbeat, Charger 상태, Timeout 정책 등을 조합해 상태를 판단해야 한다.

본 PoC에서는 연결 상태 감지 자체는 구현 범위에서 제외하고 Architecture Consideration으로만 관리한다.

⸻

### ADR-007. WebSocket 연결과 충전 상태를 분리하는 이유

WebSocket은 Client에게 실시간 상태를 전달하기 위한 Communication Channel이다.

WebSocket 연결 종료가 실제 충전기의 충전 종료를 의미하지 않는다.

따라서 실제 충전 상태의 Source of Truth는 Backend의 ChargingSession 및 충전기 Event이며 WebSocket 연결 상태와 분리되어야 한다.

본 PoC에서는 WebSocket 구현은 제외한다.

⸻

## 14. 테스트 시나리오

정상 상태 전이

```
CHARGING_STARTED
→ CHARGING_PROGRESS
→ CHARGING_COMPLETED
Expected:
ChargingSession.status = COMPLETED
```

중복 이벤트

```
eventId = evt-001
eventId = evt-001
Expected:
ChargingEvent = 1
Session update = 1
```

Out-of-order Event

```
sequence = 1
sequence = 3
sequence = 2
Expected:
sequence=2는 Session 상태를 변경하지 않음
lastSequence=3 유지
```

Consumer Failure

```
Consumer Exception
→ Retry
→ Retry
→ DLT
```

테스트는 Service 단위 테스트를 우선 촘촘하게 작성하고, 최종적으로 Testcontainers 기반 PostgreSQL + Kafka 전체 흐름 통합 테스트 1개를 수행한다.

⸻

## 15. 프로젝트 범위에서 제외하는 기능

- Frontend
- 회원 인증/인가
- 실제 OCPP 연동
- 실제 충전기 통신
- 결제
- Redis
- Elasticsearch
- WebSocket
- AWS 배포
- Kubernetes
- 관리자 페이지
- 충전소 관리
- 복잡한 요금 정책

⸻

## 16. 완료 기준

다음 흐름이 실제로 동작하면 구현 완료로 판단한다.

1. docker compose up
   → Spring Boot 실행
2. POST Charging Event
   → Kafka Publish
   → Kafka Consumer
   → ChargingSession Update
   → ChargingEvent Save
3. GET ChargingSession
   → 현재 상태 확인
4. GET ChargingEvent History
   → 처리 이벤트 확인

추가적으로 다음 항목까지 구현되면 PoC를 최종 완료한 것으로 판단한다.

- 중복 Event 방어
- Event sequence 검증
- Kafka Retry / DLT
- 핵심 Domain Test
- README Architecture 설명
