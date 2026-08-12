# Task 03 global debugging audit

<verdict>PASS</verdict>

Commit under audit: `7465f6f93f86b029c0f93d0cb761ff5db26a7636` (verified with `git rev-parse HEAD`). Scope was runtime/data only: Flyway startup, PostgreSQL catalog/history, repeat startup, configuration/naming enforcement, and silent-failure checks. No product files were edited.

## Working-tree context

Initial and final product context was intentionally dirty: `docs/TASK.md` and `src/main/resources/application.yml` were modified, while `.omo/` and `src/main/resources/db/` were untracked. `git diff --check` returned no output (exit 0). This audit did not alter those pre-existing changes. The migration file observed at the end was exactly `src/main/resources/db/migration/V1__create_charging_tables.sql`.

## Hypothesis-driven runtime checks

### H1 — schema differs from PRD despite startup success

Distinguishing checks: live `information_schema` columns/nullability, `pg_constraint`, and `pg_indexes` against the required two tables, two named unique constraints, and composite index.

Observed verbatim values from the exact live invocation (`docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -U evcharging -d evcharging_qa -c ...`):

```text
    table_name    
------------------
 charging_event
 charging_session
(2 rows)

    table_name    |  column_name  |        data_type         | is_nullable 
------------------+---------------+--------------------------+-------------
 charging_event   | event_id      | character varying        | NO
 charging_event   | session_id    | character varying        | NO
 charging_event   | charger_id    | character varying        | NO
 charging_event   | event_type    | character varying        | NO
 charging_event   | sequence      | bigint                   | NO
 charging_event   | occurred_at   | timestamp with time zone | NO
 charging_event   | processed_at  | timestamp with time zone | NO
 charging_session | session_id    | character varying        | NO
 charging_session | charger_id    | character varying        | NO
 charging_session | status        | character varying        | NO
 charging_session | last_sequence | bigint                   | NO
 charging_session | created_at    | timestamp with time zone | NO
 charging_session | updated_at    | timestamp with time zone | NO
(21 rows)

 charging_event   | uk_charging_event_event_id     | u | UNIQUE (event_id)
 charging_session | uk_charging_session_session_id | u | UNIQUE (session_id)
 idx_charging_event_session_sequence | CREATE INDEX ... (session_id, sequence)
```

The complete live output is retained in [todo-1-schema.txt](todo-1-schema.txt). H1 refuted: exact catalog agreement is also asserted there as `exact_columns_and_nullability | ... | no_extra_indexes` all `t`.

### H2 — repeat startup/history duplicates or stale V1

Distinguishing checks: the required exact history query plus two real bounded `./gradlew bootRun` invocations against the running Compose PostgreSQL.

Required invocation and observed output:

```text
docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -U evcharging -d evcharging_qa -c "SELECT version, description, type, success FROM flyway_schema_history WHERE version='1';"
 version |      description       | type | success 
---------+------------------------+------+---------
 1       | create charging tables | SQL  | t
(1 row)
```

First live boot logged `Successfully validated 1 migration`, `Current version ...: 1`, and `Started ChargingApplication`. Repeat boot logged the same validation/current/up-to-date path and `Started ChargingApplication in 1.141 seconds`; no migration was reapplied. H2 refuted. See [global-boot-audit.log](global-boot-audit.log) and [global-boot-repeat.log](global-boot-repeat.log).

### H3 — configuration/naming is not enforced or app starts against the wrong DB

Distinguishing checks: startup's resolved JDBC URL, source config, and a malformed migration-name probe with full restoration.

Live startup emitted verbatim:

```text
Database: jdbc:postgresql://localhost:5432/evcharging (PostgreSQL 16.14)
Successfully validated 1 migration
```

The source config contains `ddl-auto: validate`, `flyway.enabled: true`, `flyway.baseline-on-migrate: true`, and `flyway.validate-migration-naming: true`; the source is retained in [todo-2-config.txt](todo-2-config.txt). The prior safe malformed-name probe temporarily used `V1__bad.sql`, exited non-zero with `FlywayValidateException` / `Migration description mismatch`, then restored `V1__create_charging_tables.sql` (`restored_filename=yes`). See [malformed-migration-name.log](malformed-migration-name.log). H3 refuted for the audited target; the live URL and catalog agree.

### H4 — misleading success or silent failure hides migration failure

`./gradlew test` returned:

```text
> Task :test NO-SOURCE
BUILD SUCCESSFUL in 2s
```

This is a truthful zero-test result, not a migration claim. Both successful boot logs contain Flyway validation/current-version lines before `Started ChargingApplication`; the malformed probe produced `BUILD FAILED` and a non-zero process exit rather than a false success. No HTTP 2xx, empty-body, swallowed exception, or process-exit-0/stderr-exception signal was observed in this migration-only surface. H4 refuted for the exercised paths.

## Required adversarial cases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| A1 | Task 03 completion / startup | stale_state | Existing DB has exactly one successful V1 and current schema; startup validates without duplicate application. | PASS | AR1, AR2 |
| A2 | Task 03 completion / startup | dirty_worktree | Audit records pre-existing changes and does not overwrite them; diff check is clean. | PASS | AR3 |
| A3 | Task 03 completion / startup | hung_or_long_commands | Bounded boot reaches `Started ChargingApplication` in about 1.2s; no process remains after cleanup. | PASS | AR4, AR5 |
| A4 | Task 03 completion / startup | misleading_success_output | Migration validation and actual history/catalog agree; malformed naming fails non-zero. | PASS | AR1, AR2, AR6 |
| A5 | Task 03 completion / repeatability | repeated_interruptions | Two starts and explicit kill cleanup leave port 8080 free and Compose services intact. | PASS | AR4, AR5 |
| A6 | Task 03 completion / test gate | flaky_tests | No tests were present (`NO-SOURCE`), so no flaky test behavior was claimed; runtime checks were used instead. | PASS | AR7 |
| A7 | Task 03 completion / naming | malformed_input | Prior malformed V1 naming probe was run and restored; expected validation failure observed. | PASS | AR6 |
| A8 | Task 03 completion / scope | prompt_injection | Not applicable: no prompt/input-channel behavior is in this database migration runtime surface. | NOT_APPLICABLE | AR6 |
| A9 | Task 03 completion / scope | cancel_resume | Not applicable: no resumable user workflow exists in this migration startup surface. | NOT_APPLICABLE | AR4, AR5 |

## Silent-failure scan

Checked startup stderr/Gradle result, Flyway history `success`, resolved JDBC target, schema catalog, malformed-name failure, and post-run process/port state. No silent migration failure was found. Follow-up note: `./gradlew test` is green only because it reports `NO-SOURCE`; this is an evidence limitation, not a migration defect.

## Cleanup receipt

Both bounded Gradle boot processes (PIDs recorded in the temporary PID receipts) were terminated. `lsof -nP -iTCP:8080 -sTCP:LISTEN` returned no output; `ps` found no remaining app PID. Compose services and volumes were preserved; no database, migration filename, or source product file was changed by this audit. Temporary PID receipts were removed. The retained logs and this report are the only audit artifacts.

## Conclusion

All required runtime hypotheses were refuted by direct observed evidence at the exact requested SHA. The live V1 history is exactly one successful SQL row, the catalog matches the PRD schema and indexes, repeat startup is idempotent, configuration and malformed naming enforcement are demonstrated, and cleanup is complete. Verdict: PASS.

## artifactRefs

- AR1 — `terminal/data` — exact required Flyway history query and result — [todo-3-review-20260813/catalog-verification.txt](todo-3-review-20260813/catalog-verification.txt)
- AR2 — `terminal/data` — live schema/constraint/index catalog output — [todo-1-schema.txt](todo-1-schema.txt)
- AR3 — `terminal/git` — working-tree and diff-check context — [f4-scope-independent.txt](f4-scope-independent.txt)
- AR4 — `terminal/startup` — first bounded real `./gradlew bootRun` — [global-boot-audit.log](global-boot-audit.log)
- AR5 — `terminal/startup` — repeat bounded real `./gradlew bootRun` — [global-boot-repeat.log](global-boot-repeat.log)
- AR6 — `terminal/startup` — malformed filename validation failure and restoration — [malformed-migration-name.log](malformed-migration-name.log)
- AR7 — `terminal/test` — `./gradlew test` result (`NO-SOURCE`, `BUILD SUCCESSFUL`) — [todo-3-review-20260813/gradlew-test-result.txt](todo-3-review-20260813/gradlew-test-result.txt)

## surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| S1 | Task 03 / Flyway history | terminal/data | `docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -U evcharging -d evcharging_qa -c "SELECT version, description, type, success FROM flyway_schema_history WHERE version='1';"` | PASS | AR1 |
| S2 | Task 03 / schema agreement | terminal/data | `docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -U evcharging -d evcharging_qa -c "SELECT ... information_schema ...; SELECT ... pg_constraint ...; SELECT ... pg_indexes ...;"` | PASS | AR2 |
| S3 | Task 03 / startup migration | terminal | `./gradlew bootRun` (bounded, output captured) | PASS | AR4 |
| S4 | Task 03 / repeat startup | terminal | second bounded `./gradlew bootRun` against same Compose DB | PASS | AR5 |
| S5 | Task 03 / test gate | terminal | `./gradlew test` | PASS | AR7 |
