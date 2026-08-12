# Global Security Review — Task 03 Database Migration

<verdict>PASS</verdict>

- recommendation: `APPROVE`
- confidence: `HIGH`
- reviewed SHA: `7465f6f93f86b029c0f93d0cb761ff5db26a7636`
- branch: `feature/task-03-database-migration`
- blockers: none

## Original intent and desired outcome

Task 3 adds one immutable Flyway V1 migration for the two PRD tables, their required uniqueness constraints, and the required event-history index. It also makes JPA validate the Flyway-owned schema and marks Task 3 complete only after database verification. The desired user-visible result is a repeat-safe PostgreSQL startup migration with no later-task behavior, dependency, or infrastructure expansion.

## Working-tree context

`HEAD` is the reviewed SHA and also the merge base with `main`; Task 3 is entirely in the dirty working tree. The complete product change set is:

- modified: `docs/TASK.md`
- modified: `src/main/resources/application.yml`
- untracked: `src/main/resources/db/migration/V1__create_charging_tables.sql`

All other untracked paths are under `.omo/**` and are workflow plans, state, or evidence. There is no tracked branch diff, build/dependency diff, Docker Compose diff, Java source diff, or test diff. Review conclusions therefore use the working-tree diff plus the untracked inventory, not `main...HEAD` success prose.

## Checked artifacts

- `AGENTS.md`
- `docs/PRD.md`, especially the database contract
- `docs/TASK.md`, Task 3
- `.omo/plans/task-03-database-migration.md`
- `src/main/resources/db/migration/V1__create_charging_tables.sql`
- `src/main/resources/application.yml`
- `build.gradle`
- `docker-compose.yml`
- complete `git status --short --untracked-files=all`, tracked diff, untracked inventory, and `main...HEAD` inventory
- `.omo/evidence/task-03-database-migration-code-review.md`
- `.omo/evidence/task-03-database-migration/f1-plan-compliance.md`
- `.omo/evidence/task-03-database-migration/f2-code-quality.md`
- `.omo/evidence/task-03-database-migration/f3-manual-qa.md` and referenced terminal/catalog artifacts
- `.omo/evidence/task-03-database-migration/f4-scope.md`

The existing code-review report explicitly records direct `omo:remove-ai-slops` and `omo:programming` checks, including excessive/useless tests, deletion-only or removal-verification tests, tautological and implementation-mirroring tests, and unnecessary extraction/parsing/normalization. I independently repeated that pass: the change adds no tests or production-language code, and the 31-line declarative SQL has no abstraction, parser, normalization layer, dead code, excessive complexity, or scope drift.

## Security checklist

| Check | Result | Evidence |
|---|---|---|
| SQL injection / dynamic SQL | PASS | V1 is fixed declarative DDL only. No concatenated SQL, prepared dynamic statement, native-query code, procedure, or trigger exists in the change. |
| Privilege assumptions / unsafe grants | PASS | V1 contains no `GRANT`, `REVOKE`, role creation, ownership change, security-definer object, or superuser operation. It needs only the existing migration user's ordinary schema DDL rights. |
| Destructive DDL | PASS | Product V1 contains only `CREATE TABLE` and `CREATE INDEX`; no `DROP`, `TRUNCATE`, destructive `ALTER`, or data deletion. Destructive commands found in QA evidence target explicitly disposable QA databases and are not shipped product code. |
| Secrets exposure | PASS | No token, private key, API key, or new secret appears in the product diff. `evcharging` database credentials are pre-existing project-local Compose/application defaults and were unchanged. The mandated live query emitted no credential. Kafka logs contain only null password/token configuration fields. |
| Unsafe defaults introduced | PASS | `ddl-auto` changes from `none` to `validate`, which prevents Hibernate schema mutation. Migration-name validation is enabled. Existing `baseline-on-migrate: true` is retained, not introduced by Task 3. No network, auth, or Docker default changed. |
| Dependencies / supply chain | PASS | `build.gradle`, Gradle wrapper/settings, and Docker image pins have no diff. No dependency, repository, plugin, image, or lockfile change exists. |
| Integrity / immutability | PASS | Named UNIQUE constraints enforce `session_id` and `event_id`; the live Flyway history has exactly one successful V1 row. The canonical fixed filename plus Flyway checksum validation protects applied-version immutability on repeat startup. |
| Data exposure | PASS | Schema contains only PRD charging identifiers, state, measurements, and timestamps. No seed data, views, exports, logging changes, or public data surface was added. |
| Scope drift | PASS | Product changes are limited to the exact migration, two schema-management settings, and Task 3 status. No entity, repository, business flow, test framework, Docker redesign, foreign key, extra constraint, or speculative index was added. |
| AI-slop / overfit | PASS | No tests were added, removed, or altered; therefore no deletion-only, removal-verification, tautological, implementation-mirroring, excessive, or brittle prose test exists. Direct SQL avoids unnecessary production extraction/parsing/normalization. |

## Automated verification

- `git diff --check`: PASS, exit 0.
- Secret/private-key/token scan: PASS; only unchanged local database defaults and null Kafka diagnostic fields were found.
- Dynamic SQL / destructive product DDL / unsafe grant scan: PASS; none in product changes.
- Dependency and Docker diff scan: PASS; no changes.
- Live history inspection, read-only exact invocation:

```sh
docker compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -U evcharging -d evcharging_qa -c "SELECT version, description, type, success FROM flyway_schema_history WHERE version='1';"
```

Observed exactly one row: version `1`, description `create charging tables`, type `SQL`, success `t`. No credential was printed. PostgreSQL and Kafka remained healthy; no service, volume, or database object was changed by this review.

## UltraQA

- `stale_state`: PASS — current live history was queried independently and agrees with the current V1 filename and prior catalog evidence.
- `dirty_worktree`: PASS — full tracked and untracked inventory was inspected; the dirty state is explicitly accounted for.
- `misleading_success_output`: PASS — the verdict relies on direct source/diff inspection and a fresh live SQL query, not executor success wording. The earlier F3 wording that implied its manually preloaded database was migrated by that first boot was noted; separate startup evidence and the current live history establish the criterion.
- `prompt_injection`: N/A — no untrusted prompt or content execution path exists.
- `malformed_input`: N/A for security — this task ships a fixed migration, not a user-controlled SQL input surface; migration naming validation is enabled and separate evidence covers malformed naming.
- `cancel_resume`: N/A — no resumable user workflow.
- `repeated_interruptions`: N/A — no interactive workflow or write process in this lane.
- `flaky_tests`: N/A — no tests changed; Gradle is a `NO-SOURCE` build smoke gate, while database behavior is verified through deterministic catalog/history queries.
- `hung_commands`: PASS — review commands were bounded and completed.

## Blockers and evidence gaps

Blockers: none. No stated Task 3 security or scope criterion is violated.

Exact evidence gaps: none for this security lane. The product changes remain uncommitted, so `main...HEAD` alone is empty and must not be used as the change inventory; the complete working-tree and untracked-file inventories fill that gap.

## Cleanup receipt

Evidence-file write only. No product file, process, container, service, database object, temporary asset, or volume was created, altered, stopped, or deleted. Existing services and volumes were preserved.
