# Task 4 Debugging Runtime Audit

Date: 2026-08-13
Verdict: **PASS**

## Binding

- Git HEAD: `5eb94a4c4f3d740f4108f507dbe5a1dc7a8337ff`
- Task 4 domain-set SHA-256: `06b3f5f674957b2ac01a0db60740ff9d6946028db5fdd9dbdf429af3c1d8b085`
- `ChargingSession.java`: `f7e42270cb4db162f5cce95bfd6b882dd4b94bbd639be9dfb2137eeb7b85d363`
- `ChargingSessionStatus.java`: `ed342d5df00e27c5e37e4fdc6577dd7c9320b75d3e874376f5d38db08110aeba`
- `ChargingEvent.java`: `f9c3cee57a1e37e50a825d34070ba859c9d2e5b6c24759a2631bb7f56980cf74`
- `ChargingEventType.java`: `efec2ad8b402532268b8bada277a743be3087a043f8366488ecfde1ab364c717`

The domain files are untracked at this HEAD, so the domain-set SHA-256 binds this audit to their exact reviewed bytes rather than pretending the commit alone contains them.

## Runtime

- Java: Temurin 21.0.12
- Gradle: 8.14
- Spring Boot: 3.5.5
- Hibernate ORM: 6.6.26.Final
- PostgreSQL: Compose `postgres:16-alpine`, live server 16.14
- Entry: bounded `./gradlew bootRun --no-daemon --args='--spring.main.web-application-type=none --logging.level.org.hibernate.orm.boot=DEBUG --logging.level.org.hibernate.mapping=DEBUG'`
- Result: `bounded_result=started`; `Started ChargingApplication in 1.153 seconds`

## Hypothesis results

### H1 — entity scan or mapping discovery failure

**REFUTED.** Hibernate emitted mapping metadata for every column of both `charging_event` and `charging_session`, including both primary keys, then initialized the default persistence unit. Representative observed lines:

```text
Skipping column re-registration: charging_event.event_type
Skipping column re-registration: charging_event.occurred_at
Skipping column re-registration: charging_session.status
Skipping column re-registration: charging_session.updated_at
Forcing column [id] to be non-null as it is part of the primary key for table [charging_event]
Forcing column [id] to be non-null as it is part of the primary key for table [charging_session]
Initialized JPA EntityManagerFactory for persistence unit 'default'
```

### H2 — Hibernate `ddl-auto=validate` schema mismatch

**REFUTED.** The live boot used the configured PostgreSQL datasource and `ddl-auto: validate`. Flyway found schema version 1 current, Hibernate completed validation and initialized the EntityManagerFactory, and the application reached `Started`. The audit log contained none of `Schema-validation`, `Validation: missing`, `wrong column type`, `ERROR`, or `Exception`.

```text
Successfully validated 1 migration
Current version of schema "public": 1
Schema "public" is up to date. No migration necessary.
Initialized JPA EntityManagerFactory for persistence unit 'default'
Started ChargingApplication in 1.153 seconds
```

### H3 — enum, time, or numeric mapping mismatch

**REFUTED.** Hibernate discovered the relevant mapped columns and passed live validation. A direct PostgreSQL catalog query independently returned:

```text
charging_event|event_type|character varying|varchar|||NO
charging_event|charged_kwh|numeric|numeric|12|3|YES
charging_event|occurred_at|timestamp with time zone|timestamptz|||NO
charging_event|processed_at|timestamp with time zone|timestamptz|||NO
charging_session|status|character varying|varchar|||NO
charging_session|charged_kwh|numeric|numeric|12|3|YES
charging_session|started_at|timestamp with time zone|timestamptz|||YES
charging_session|completed_at|timestamp with time zone|timestamptz|||YES
charging_session|created_at|timestamp with time zone|timestamptz|||NO
charging_session|updated_at|timestamp with time zone|timestamptz|||NO
```

This matches `EnumType.STRING` to varchar, `BigDecimal(12,3)` to numeric(12,3), and `Instant` to timestamptz.

## Cleanup and scope

- No product files edited by this audit.
- The Gradle wrapper process and its child `ChargingApplication` JVM were both terminated after the bounded startup observation.
- Final exact process scan for `com.example.charging.ChargingApplication` was empty.
- Compose PostgreSQL and Kafka remained `running` and `healthy`.
- Named volumes were preserved: `evcharging_postgres-data`, `evcharging_kafka-data`.
- No database or volume was dropped or recreated.

## Conclusion

All three required runtime hypotheses are refuted by reproduced runtime evidence. Task 4's two entities are discovered, Hibernate validation agrees with live Flyway V1, and enum/time/numeric mappings agree with PostgreSQL. No defect or product fix was found.
