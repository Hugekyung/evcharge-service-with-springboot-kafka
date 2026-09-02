# EV Charging Event Platform

Node.js 실무에서 경험했던 이벤트 기반 처리를 Java/Spring/Kafka 환경에서 다시 설계하면서 메시지 순서·중복·실패·멱등성을 비교 검증하기 위한 전기차 충전 비동기 처리 PoC 프로젝트입니다.

HTTP로 받은 충전 이벤트를 Kafka에 발행하고, Consumer가 이벤트를 읽어 Session 상태와 이벤트 이력을 PostgreSQL에 저장합니다. 중복 전달, 순서 역전, 일시적 장애와 실패 이벤트 처리까지 포함해 이벤트 기반 백엔드의 핵심 흐름을 연습하는 것이 목적입니다.

## 핵심 흐름

```
Virtual Charger
      │ HTTP
      ▼
ChargingEventController
      │ validation + publish acknowledgement
      ▼
ChargingEventProducer
      │ sessionId를 Kafka key로 사용
      ▼
charging-events topic
      │
      ▼
ChargingEventConsumer
      │
      ▼
ChargingSessionService
      │ idempotency → ordering → state transition
      ▼
PostgreSQL
  charging_session / charging_event
```

HTTP 요청은 DB를 직접 변경하지 않습니다. 요청 형식을 검증하고 Kafka Broker 발행 성공을 확인한 뒤 `202 Accepted`를 반환하며, 실제 Session 변경은 Consumer의 트랜잭션에서 수행합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5
- Spring Boot Actuator
- Spring Web MVC, Validation
- Spring Data JPA
- Spring Kafka
- PostgreSQL 16
- Flyway
- Apache Kafka 3.9 (KRaft, 단일 Broker 로컬 구성)
- Gradle
- JUnit 5, Spring Boot Test
- Testcontainers (PostgreSQL + Kafka 통합 테스트)

## 프로젝트 구조

```
src/main/java/com/example/charging
├── controller       HTTP 요청 검증과 응답 변환
├── application      비즈니스 규칙과 트랜잭션 경계
├── domain           ChargingSession, ChargingEvent와 상태 전이
├── kafka            Producer, Consumer, Kafka 메시지
├── repository       Spring Data JPA 저장소
└── config           Topic, Listener, 초기화와 오류 처리 설정
```

## 로컬 실행

필요 환경:

- Java 21
- Docker와 Docker Compose

```bash
docker compose up -d
./gradlew bootRun
```

PostgreSQL은 `localhost:5432`, Kafka는 `localhost:9092`에서 실행됩니다. Flyway가 `V1__create_charging_tables.sql`을 적용하고, `KafkaTopicInitializer`가 Kafka Broker 연결과 Topic 생성을 확인한 뒤 Listener를 시작합니다.

Kafka 초기화는 최초 시도를 포함해 최대 5회 수행합니다. 1초부터 지연 시간이 2배씩 증가하며, 연결 timeout이나 Broker의 일시적 오류만 재시도합니다. 인증·권한·설정 오류 같은 영구 오류는 즉시 시작 실패로 처리합니다.

Actuator Health Check는 다음 주소에서 확인할 수 있습니다.

```bash
curl http://localhost:8080/actuator/health
```

Health 응답에는 애플리케이션, PostgreSQL, Kafka 연결 상태가 포함됩니다. 로컬 PoC 편의를 위해 `health`, `info` endpoint만 외부에 노출합니다.

종료할 때는 다음을 사용합니다.

```bash
docker compose down
```

개발 데이터까지 삭제하려면 `docker compose down -v`가 필요하지만, named volume의 PostgreSQL/Kafka 데이터를 삭제하므로 신중하게 사용해야 합니다.

## API

### 충전 이벤트 발행

`POST /api/v1/charging-events`

```bash
curl -i -X POST http://localhost:8080/api/v1/charging-events \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "evt-100001",
    "chargerId": "charger-001",
    "sessionId": "session-001",
    "eventType": "CHARGING_STARTED",
    "sequence": 1,
    "batteryLevel": 35,
    "chargedKwh": 0,
    "occurredAt": "2026-08-12T12:00:00+09:00"
  }'
```

- 성공: `202 Accepted`
- 구조적으로 잘못된 요청: `400 Bad Request`
- Kafka 발행 실패 또는 timeout: `503 Service Unavailable`

`202`는 단순히 요청을 받았다는 뜻이 아니라 Kafka Broker 발행 성공이 확인된 경우에만 반환됩니다. DB 반영 완료까지 기다리지는 않습니다.

`eventType`은 `CHARGING_STARTED`, `CHARGING_PROGRESS`, `CHARGING_COMPLETED`, `CHARGING_FAILED` 중 하나여야 합니다. `occurredAt`은 시간대가 포함된 ISO-8601 형식이어야 합니다.

### Session 조회

`GET /api/v1/charging-sessions/{sessionId}`

```bash
curl http://localhost:8080/api/v1/charging-sessions/session-001
```

```json
{
  "sessionId": "session-001",
  "chargerId": "charger-001",
  "status": "CHARGING",
  "batteryLevel": 35,
  "chargedKwh": 0.000,
  "lastSequence": 1,
  "startedAt": "2026-08-12T03:00:00Z",
  "completedAt": null
}
```

존재하지 않는 Session은 `404 Not Found`입니다.

### Event History 조회

`GET /api/v1/charging-sessions/{sessionId}/events?page=0&size=20`

```bash
curl 'http://localhost:8080/api/v1/charging-sessions/session-001/events?page=0&size=20'
```

응답은 sequence 오름차순으로 정렬됩니다. `size`는 1 이상 100 이하만 허용됩니다.

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "hasNext": false
}
```

## 도메인 규칙

### 상태 전이

```
CHARGING_STARTED   → CHARGING
CHARGING_PROGRESS  → CHARGING
CHARGING_COMPLETED → COMPLETED
CHARGING_FAILED    → FAILED
```

`COMPLETED`와 `FAILED`는 종료 상태입니다. 종료된 Session을 다시 `CHARGING`으로 되돌리지 않습니다. Session은 `CHARGING_STARTED` 이벤트로만 생성하며, Session이 없거나 이벤트의 `chargerId`가 다르면 비즈니스 오류로 처리합니다.

### 트랜잭션

Consumer는 `ChargingSessionService.process()`를 호출하고, Service가 하나의 DB 트랜잭션을 엽니다.

```
eventId 중복 확인
→ Session 조회
→ sequence 확인
→ Session 상태 변경
→ Event History 저장
→ commit
```

상태 변경과 Event History 저장이 같은 트랜잭션에 속하므로 한쪽만 반영되는 상황을 줄입니다.

## Database

스키마 변경은 Flyway가 관리하며 Hibernate는 `ddl-auto: validate`로 매핑과 실제 스키마가 일치하는지만 확인합니다.

### `charging_session`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `BIGSERIAL` | PK |
| `session_id` | `VARCHAR` | Session 식별자, UNIQUE |
| `charger_id` | `VARCHAR` | 충전기 식별자 |
| `status` | `VARCHAR` | `CHARGING`, `COMPLETED`, `FAILED` |
| `battery_level` | `INTEGER` | 배터리 잔량 |
| `charged_kwh` | `NUMERIC(12,3)` | 충전량 |
| `last_sequence` | `BIGINT` | 마지막으로 반영한 sequence |
| `started_at` | `TIMESTAMPTZ` | 시작 시각 |
| `completed_at` | `TIMESTAMPTZ` | 완료 또는 실패 시각 |
| `created_at` | `TIMESTAMPTZ` | 생성 시각 |
| `updated_at` | `TIMESTAMPTZ` | 수정 시각 |

### `charging_event`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `BIGSERIAL` | PK |
| `event_id` | `VARCHAR` | 멱등 키, UNIQUE |
| `session_id` | `VARCHAR` | Session 식별자 |
| `charger_id` | `VARCHAR` | 충전기 식별자 |
| `event_type` | `VARCHAR` | 이벤트 종류 |
| `sequence` | `BIGINT` | 이벤트 순서 |
| `battery_level` | `INTEGER` | 배터리 잔량 |
| `charged_kwh` | `NUMERIC(12,3)` | 충전량 |
| `occurred_at` | `TIMESTAMPTZ` | 충전기 이벤트 발생 시각 |
| `processed_at` | `TIMESTAMPTZ` | 서버 처리 시각 |

Event History 조회를 위해 `(session_id, sequence)` 조합 인덱스를 사용합니다. UNIQUE 제약조건이 이미 만드는 인덱스를 중복해서 추가하지 않았습니다.

## Kafka

| Topic | Partition | Replica | 역할 |
| --- | ---: | ---: | --- |
| `charging-events` | 3 | 1 | 정상 이벤트 |
| `charging-events-dlt` | 3 | 1 | 최종 처리 실패 이벤트 |

Topic은 `KafkaTopicConfig`의 `NewTopic` Bean으로 선언하고, `KafkaTopicInitializer`가 Broker 연결 후 생성합니다. 로컬 Kafka의 자동 Topic 생성은 꺼져 있습니다.

Producer는 `sessionId`를 Message Key로 사용합니다. 같은 Session의 이벤트가 같은 Partition으로 갈 가능성을 높여 Partition 내부 순서 보장을 활용하기 위해서입니다. 다만 Kafka 순서만 믿지 않고 Application의 `lastSequence`도 함께 확인합니다.

Listener 동시성은 정상 Topic 3개 Partition에 맞춰 3으로 설정했습니다. Consumer 수가 Partition 수보다 많아도 처리량이 늘지 않고 일부 Consumer가 놀게 되므로, Partition 수와 처리 지연을 기준으로 조정합니다.

## 멱등성과 Event Ordering

### 멱등성

`eventId`를 멱등 키로 사용합니다.

```
eventId 존재 여부 확인
├─ 존재함    → 즉시 종료, 상태 변경·Event 저장 안 함
└─ 없음      → 상태 처리 후 Event 저장
```

Application의 선행 확인과 DB의 `UNIQUE(event_id)` 제약조건을 함께 사용합니다. Kafka가 같은 메시지를 다시 전달해도 Session 상태가 중복 변경되지 않도록 하는 구조입니다.

### 순서 역전

Session의 `lastSequence`보다 작거나 같은 이벤트는 오래된 이벤트로 판단합니다.

```
sequence=1 → sequence=3 → sequence=2

결과:
lastSequence=3
sequence=2는 Session 상태를 덮어쓰지 않음
sequence=2는 새로운 eventId라면 Event History에는 저장
```

## Retry / DLT

Consumer는 `DefaultErrorHandler` 기반 Blocking Retry를 사용합니다.

```
최초 처리
→ 1초 후 재시도
→ 2초 후 재시도
→ 최종 실패 시 charging-events-dlt
```

- 총 시도: 3회, 최초 1회 + 재시도 2회
- 재시도 대상: DB 연결 실패 등 일시적 인프라 오류, Kafka `RetriableException`
- 재시도 제외: 비즈니스 오류, 일반 예외
- 최대 backoff: 16초

Blocking Retry를 선택한 이유는 Retry Topic 방식에서 실패한 이벤트가 별도 Retry Topic으로 이동하는 동안 같은 Partition의 뒤 이벤트가 먼저 처리될 수 있기 때문입니다. Blocking 방식은 현재 Partition의 이벤트가 성공하거나 DLT로 이동할 때까지 다음 이벤트로 넘어가지 않아 순서 보장이 더 명확합니다.

DLT에는 별도 `dltKafkaListenerContainerFactory`를 연결합니다. DLT Handler 자체가 실패해도 `charging-events-dlt-dlt`로 재전송하지 않고, 재시도 없이 오류를 로그로 남깁니다. 역직렬화 실패는 `ErrorHandlingDeserializer`와 DLT Factory의 ErrorHandler가 처리합니다.

## 테스트 전략

Service 단위 테스트를 먼저 작성하고, 마지막에 Testcontainers 통합 테스트 하나로 실제 연결을 확인합니다.

```
Service 단위 테스트
  ├─ 상태 전이
  ├─ 멱등성
  ├─ Event Ordering
  ├─ 종료 상태 보호
  └─ 잘못된 이벤트 입력

Kafka 설정 단위 테스트
  ├─ 일시적 연결 실패 후 재시도 성공
  └─ 최대 재시도 초과 시 Listener 미시작

Testcontainers 통합 테스트 1개
  PostgreSQL + Kafka
  Producer → Consumer → Service → DB
```

실행:

```bash
./gradlew test
```

통합 테스트는 Docker가 필요합니다. Docker가 실행되지 않으면 통합 테스트가 Skip되지 않고 실패하도록 구성했습니다.

## 주요 ADR

### ADR-001. HTTP에서 DB를 직접 변경하지 않고 Kafka를 거친다

HTTP 요청과 DB 상태 변경을 분리해 Producer와 Consumer를 독립적으로 확장하고, Kafka Retry/DLT와 장애 격리를 적용하기로 했습니다. 따라서 API는 발행 성공까지만 책임지고, Session 변경은 Consumer의 Service가 담당합니다.

### ADR-002. Kafka Message Key는 `sessionId`로 한다

같은 Session의 이벤트를 같은 Partition으로 보내 Partition 내부 순서 보장을 활용하기 위해 `sessionId`를 Key로 선택했습니다. 그래도 순서 역전 가능성은 남으므로 `lastSequence` 검사를 함께 둡니다.

### ADR-003. 멱등 키는 `eventId`로 한다

Kafka의 중복 전달을 전제로 `eventId` 존재 여부를 먼저 확인하고, DB에도 `UNIQUE(event_id)`를 둡니다. Application 확인과 DB 제약조건의 두 계층으로 중복 상태 변경을 방어합니다.

### ADR-004. Retry는 Blocking 방식으로 한다

초기에는 Spring Kafka Retry Topic 방식을 검토했지만, Retry Topic으로 이동한 이벤트가 원래 Partition의 다음 이벤트보다 늦게 처리되면 같은 Session의 순서가 흔들릴 수 있었습니다. 이 PoC에서는 Partition 순서를 우선해 `DefaultErrorHandler` Blocking Retry를 사용합니다.

### ADR-005. Retry와 DLT의 책임을 분리한다

일시적인 인프라 오류만 재시도하고, 잘못된 상태 전이 같은 비즈니스 오류는 즉시 DLT로 보냅니다. DLT Handler에는 전용 Factory와 `FixedBackOff(0, 0)`을 적용해 DLT 처리 실패가 다시 DLT Topic을 만드는 순환을 막습니다.

### ADR-006. Topic은 애플리케이션 코드로 선언한다

개인 PoC에서는 `NewTopic` Bean으로 Topic 이름·Partition·Replica 구성을 코드에서 함께 관리합니다. 운영 환경에서는 Topic 보존 정책, 복제 수, 권한을 애플리케이션과 분리해 Terraform, Helm, Strimzi 같은 인프라 코드로 관리하는 편이 적합합니다.

### ADR-007. 시간 타입은 Java `Instant`를 사용한다

서버와 지역에 따라 시간이 달라지는 문제를 줄이기 위해 내부 시간 타입은 UTC 기준 `Instant`로 통일했습니다. HTTP 입력은 offset이 포함된 ISO-8601을 받고, Kafka JSON의 `occurredAt`도 ISO-8601 문자열로 직렬화합니다.

### ADR-008. 통합 테스트는 Testcontainers로 검증한다

Mock만으로는 Kafka Broker, Flyway, PostgreSQL, Listener 연결 문제를 확인할 수 없습니다. 따라서 Service 단위 테스트를 먼저 작성하고, 최종적으로 실제 PostgreSQL과 Kafka를 띄우는 Testcontainers 통합 테스트 한 개로 핵심 흐름을 검증합니다. Docker가 없을 때 테스트를 조용히 Skip하지 않도록 했습니다.

### ADR-009. 운영성 개선은 Actuator Health Check만 적용한다

이 프로젝트는 작은 PoC이므로 Metrics 서버, Structured Logging 파이프라인, 커스텀 Consumer 상태 API까지 확장하지 않습니다. 대신 Actuator의 `health`, `info` endpoint만 노출해 애플리케이션·PostgreSQL·Kafka의 기본 연결 상태를 확인할 수 있게 했습니다. 나머지 운영·개발환경 확장은 현재 범위에서 제외합니다.

## 범위에서 제외한 기능

- 인증·인가
- Redis, Elasticsearch
- WebSocket
- OCPP 연동
- 결제와 복잡한 요금 계산
- Kubernetes, AWS, Microservice 분리
- 관리자 기능과 수동 DLT 재처리 UI

## 현재 완료 기준

- HTTP → Kafka Producer → Kafka Consumer → Service → PostgreSQL 흐름
- Session 조회 및 Event History 조회
- 멱등성 및 Event Ordering
- Kafka Retry / DLT
- Service 단위 테스트와 Testcontainers 통합 테스트
- Actuator Health Check
- `./gradlew test` 통과
