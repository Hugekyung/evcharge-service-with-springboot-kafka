# EV Charging Event Platform

Spring Boot와 Kafka를 사용해 충전 이벤트를 비동기로 처리하는 PoC 프로젝트다.

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

애플리케이션이 시작되면 Kafka Admin 설정을 통해 `charging-events` 토픽이 자동으로 생성된다. Kafka가 아직 준비되지 않은 경우 1초, 2초, 4초, 8초 간격으로 최대 5회 재시도하며, 끝까지 연결되지 않으면 애플리케이션 시작을 실패시킨다. 토픽 생성이 성공한 뒤 Kafka Listener를 시작한다.

## ADR

### ADR-001. Kafka 토픽은 애플리케이션 코드로 선언한다

#### 결정

`charging-events` 토픽을 Spring Kafka의 `NewTopic` Bean으로 선언한다. 토픽은 3개 파티션과 복제 수 1개로 생성한다.

#### 이유

이 프로젝트는 단일 Kafka 브로커를 사용하는 작은 PoC다. 토픽 생성 설정을 애플리케이션 코드에 명시하면 Producer와 Consumer가 요구하는 토픽 이름과 파티션 구성을 코드에서 바로 확인할 수 있다. Kafka의 자동 토픽 생성을 끄고 필요한 토픽만 명시적으로 만들 수 있어 오타로 인한 잘못된 토픽 생성도 줄일 수 있다.

운영 환경에서는 토픽 생성과 보존 기간, 복제 수, 접근 권한을 애플리케이션 배포와 분리해 Terraform, Helm, Strimzi 같은 인프라 코드로 관리하는 것을 고려한다.

### ADR-002. Kafka 데이터를 named volume에 저장한다

#### 결정

Kafka 로그 디렉터리를 `kafka-data` named volume에 연결한다.

#### 이유

Docker 컨테이너를 재생성해도 KRaft 메타데이터와 Kafka 메시지가 유지되도록 PostgreSQL과 동일하게 개발 환경의 데이터를 영속화한다. 단, `docker compose down -v`를 사용하면 named volume도 삭제된다.
