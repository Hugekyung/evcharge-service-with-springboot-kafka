# EV Charging Event Platform - TASK

## 0. 개발 원칙

- P0 작업 완료 전 추가 기능을 구현하지 않는다.
- 요구사항에 없는 기술을 임의로 추가하지 않는다.
- Frontend는 구현하지 않는다.
- 실제 OCPP 연동은 구현하지 않는다.
- 각 Phase 완료 후 최소한의 실행 검증을 수행한다.

⸻

## P0 - Core MVP

### 1. Spring Boot 프로젝트 초기화 [완료]

- [x] Java 21 설정
- [x] Spring Boot 3.x 프로젝트 생성
- [x] Gradle 설정
- [x] 기본 패키지 구조 생성

Dependencies:

- [x] Spring Web
- [x] Spring Validation
- [x] Spring Data JPA
- [x] Spring Kafka
- [x] PostgreSQL Driver
- [x] Flyway
- [x] Spring Boot Test

완료 조건:

- [x] ./gradlew test
- [x] ./gradlew bootRun

정상 실행

⸻

### 2. Docker Compose 인프라 구성 [완료]

- [x] docker-compose.yml 생성
- [x] PostgreSQL 구성
- [x] Kafka 구성
- [x] PostgreSQL Connection 설정
- [x] Kafka Bootstrap Server 설정
- [x] application.yml 작성

완료 조건:

docker compose up -d

실행 후 Spring Boot에서 PostgreSQL과 Kafka 연결 가능

⸻

### 3. Database Migration 구성 [완료]

- Flyway 설정
- charging_session 테이블 생성
- charging_event 테이블 생성
- session_id UNIQUE Constraint
- event_id UNIQUE Constraint
- `charging_event(session_id, sequence)` 인덱스 설정

완료 조건:

Spring Boot 실행 시 Flyway Migration 성공

⸻

### 4. Domain 구현

ChargingSession

- ChargingSession Entity 구현
- sessionId
- chargerId
- status
- batteryLevel
- chargedKwh
- lastSequence
- startedAt
- completedAt
- createdAt
- updatedAt

ChargingEvent

- ChargingEvent Entity 구현
- eventId
- sessionId
- chargerId
- eventType
- sequence
- batteryLevel
- chargedKwh
- occurredAt
- processedAt

Enum

- ChargingSessionStatus 구현
- ChargingEventType 구현

완료 조건:

Entity와 DB Schema Mapping 정상 동작

⸻

### 5. Repository 구현

- ChargingSessionRepository 구현
- ChargingEventRepository 구현
- findBySessionId 구현
- existsByEventId 구현
- Session Event History 조회 구현

완료 조건:

Repository Test 또는 Application 실행을 통해 정상 조회 확인

⸻

### 6. Charging Event API 구현

Endpoint:

POST /api/v1/charging-events

- ChargingEventRequest DTO 구현
- Validation 적용
- ChargingEventController 구현
- 정상 요청에 202 Accepted 반환
- Kafka Broker 발행 성공 확인 후 202 반환
- Kafka 발행 실패 또는 timeout은 5xx 처리
- Controller에서 DB 직접 접근 금지

완료 조건:

HTTP Request가 정상적으로 Controller까지 전달되고 Validation 동작

⸻

### 7. Kafka Producer 구현

- ChargingEventMessage 구현
- ChargingEventProducer 구현
- KafkaTemplate 사용
- Topic charging-events 사용
- Kafka Message Key로 sessionId 사용
- Controller → Producer 연결

완료 조건:

POST /api/v1/charging-events
→ Kafka charging-events Topic

메시지 발행 확인

⸻

### 8. Kafka Consumer 구현

- ChargingEventConsumer 구현
- @KafkaListener 적용
- charging-events Topic Consume
- ChargingSessionService 호출
- Consumer에 비즈니스 로직 직접 구현하지 않음

완료 조건:

Producer에서 전송한 Event를 Consumer가 정상적으로 수신

⸻

### 9. Charging Session 비즈니스 로직 구현

- ChargingSessionService 구현
- @Transactional 적용
- CHARGING_STARTED 처리
- CHARGING_PROGRESS 처리
- CHARGING_COMPLETED 처리
- CHARGING_FAILED 처리
- Session 상태 변경
- ChargingEvent History 저장

완료 조건:

POST Event
→ Kafka
→ Consumer
→ Service
→ PostgreSQL

전체 흐름 정상 동작

⸻

### 10. Session 조회 API 구현

Endpoint:

GET /api/v1/charging-sessions/{sessionId}

- ChargingSessionResponse 구현
- Session 조회 Service 구현
- Controller 구현
- 존재하지 않는 Session 처리

완료 조건:

Kafka Event 처리 후 현재 Session 상태 조회 가능

⸻

### 11. Event History API 구현

Endpoint:

GET /api/v1/charging-sessions/{sessionId}/events

- Event History 조회 구현
- sequence 또는 occurredAt 기준 정렬
- Response DTO 구현

완료 조건:

Session에 처리된 Event 목록 확인 가능

⸻

## P1 - Reliability

### 12. 멱등성 구현

- Consumer 처리 전 eventId 존재 여부 확인
- 이미 처리된 Event는 상태 변경하지 않음
- event_id UNIQUE Constraint 확인
- 중복 Event 로그 기록

완료 조건:

동일 eventId를 두 번 전송해도 Event가 한 번만 반영됨

⸻

### 13. Event Ordering 구현

- ChargingSession lastSequence 사용
- Event 처리 후 lastSequence 업데이트
- incomingSequence <= lastSequence 검증
- 오래된 Event는 Session 상태 변경하지 않음
- Out-of-order Event 로그 기록
- 새로운 eventId인 오래된 Event는 Event History에 저장
- 이미 존재하는 eventId는 저장하지 않고 처리 종료

완료 조건:

sequence=1
sequence=3
sequence=2

전송 후:

lastSequence=3

유지

⸻

### 14. Kafka Retry 구현

- Consumer 실패 Retry 설정
- 총 3회 처리 시도: 최초 1회 + 재시도 2회
- Backoff 적용
- 일시적 인프라 오류만 Retry, 비즈니스 오류는 재시도 없이 DLT
- Retry 동작 확인

완료 조건:

의도적으로 Exception 발생 시 Retry 수행 확인

⸻

### 15. Kafka DLT 구현

- Retry exhausted Event DLT 전달
- DLT Handler 구현
- 실패 Event 로그 기록

완료 조건:

Consumer Failure
→ Retry
→ Retry
→ DLT

흐름 확인

⸻

## P1 - Tests

### 16. 상태 전이 테스트

- CHARGING_STARTED 테스트
- CHARGING_PROGRESS 테스트
- CHARGING_COMPLETED 테스트
- CHARGING_FAILED 테스트

핵심 Scenario:

STARTED
→ PROGRESS
→ COMPLETED
Expected:
status = COMPLETED

⸻

### 17. 멱등성 테스트

- 동일 eventId 두 번 처리

Expected:

ChargingEvent = 1
Session State Update = 1

⸻

### 18. Event Ordering 테스트

Scenario:

sequence=1
sequence=3
sequence=2

Expected:

lastSequence=3
sequence=2 상태 변경 없음

⸻

### 19. 테스트 전략 및 Kafka 통합 테스트

- Service 단위 테스트를 먼저 촘촘하게 작성한다.
- 상태 전이, 멱등성, 순서 역전, 종료 상태 보호, 잘못된 이벤트 입력을 검증한다.
- Kafka 연결 실패 후 재시도 동작을 검증한다.
- 최초 Kafka 연결 실패 시 애플리케이션이 즉시 성공 처리되지 않고 재시도하는지 검증한다.
- 재시도 중 Kafka가 다시 연결되면 토픽 생성이 성공하고 애플리케이션이 정상 기동하는지 검증한다.
- 최대 재시도 횟수 초과 시 애플리케이션 기동이 실패하는지 검증한다.
- 재연결 성공 후 Kafka Listener가 시작되는지 검증한다.
- 최종적으로 Testcontainers 기반 PostgreSQL + Kafka 통합 테스트 1개를 필수 수행한다.
- Producer → Consumer 통합 테스트
- Kafka → DB 통합 흐름 테스트

⸻

## P1 - Documentation

### 20. README 작성

- 프로젝트 목적
- Architecture Diagram
- 기술 스택
- 실행 방법
- API 사용 예제
- DB Schema
- Kafka Topic 설명
- Kafka Message Key 설계 이유
- 멱등성 설계
- Event Ordering 설계
- Retry / DLT 설계
- Transaction Boundary 설명
- Architecture Decisions 작성
- PoC 한계 및 향후 개선사항 작성

⸻

## P2 - Optional

P0/P1이 모두 완료된 경우에만 수행한다.

### 21. 운영성 개선

- Spring Boot Actuator
- Health Check
- Kafka Consumer 상태 확인
- Structured Logging 개선

### 22. 개발환경 개선

- Kafka UI
- Application Dockerfile
- 전체 Docker Compose 실행 지원

### 23. 테스트 개선

- Testcontainers PostgreSQL
- Testcontainers Kafka
- 전체 Integration Test

⸻

제외 범위

다음 기능은 이번 PoC에서 구현하지 않는다.

- Frontend
- 인증/인가
- Redis
- Elasticsearch
- WebSocket
- 실제 OCPP
- 실제 충전기 연결
- 결제
- AWS
- Kubernetes
- 관리자 기능

⸻

Definition of Done

최소 완료 조건:

```
[HTTP]
POST Charging Event
↓
[Spring Boot]
Controller
Producer
↓
[Kafka]
charging-events
↓
[Spring Boot]
Consumer
ChargingSessionService
↓
[PostgreSQL]
charging_session
charging_event
```

아래 명령으로 로컬 환경을 실행할 수 있어야 한다.

docker compose up -d
./gradlew bootRun

다음 시나리오가 실제로 동작해야 한다.

1. CHARGING_STARTED 전송
2. CHARGING_PROGRESS 전송
3. CHARGING_COMPLETED 전송
4. Session 조회
5. COMPLETED 상태 확인
6. Event History 조회

최종 PoC 완료 조건:

- Core Event Flow 정상 동작
- PostgreSQL 저장 정상
- 멱등성 동작
- Event Ordering 처리
- Retry / DLT 동작
- 핵심 Test 통과
- README 작성 완료
- ./gradlew test 성공
