# Runtime debugging audit — Task 3

Reviewed full commit: `7465f6f93f86b029c0f93d0cb761ff5db26a7636` (tree `2822d88d949a38ab783db14c68560b189910149b`).

| hypothesis | distinguishing check | observable | verdict |
|---|---|---|---|
| H1: Flyway cannot discover or apply V1 on a clean database | Fresh `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/evcharging_qa_review ./gradlew bootRun`, bounded poll for `Started` | Fresh startup marker was found; process cleaned up; direct catalog shows both tables and one successful V1 row | refuted |
| H2: Repeat startup re-applies V1 or duplicates schema objects | Second identical bounded `bootRun`; query `flyway_schema_history`, constraints, and indexes | Repeat startup marker found; catalog assertion reports `flyway_v1=1`, named uniques=2, composite index=1 | refuted |
| H3: Hibernate schema validation rejects the migrated schema | Same real application startup with `spring.jpa.hibernate.ddl-auto=validate` and migrated PostgreSQL catalog | Both fresh and repeat application starts complete successfully; no validation failure appears in boot logs | refuted |

Audit verdict: PASS. Evidence: `todo-3-review-20260813/boot-fresh-result.txt`, `boot-repeat-result.txt`, and `catalog-verification.txt`. Cleanup: disposable database dropped; no application process remains; Compose services remain healthy.
