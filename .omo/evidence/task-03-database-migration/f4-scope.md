# F4 Scope Fidelity Audit — Independent Recheck

## Verdict

**PASS**

- recommendation: `APPROVE`
- AdversarialVerify: `confirmed`
- blockers: none

## Original intent

Complete Task 3 only: add the PRD-defined Flyway V1 migration, align Flyway/JPA schema settings, verify migration behavior, and mark Task 3 complete only after verification.

## Desired outcome

Exactly three product files may differ from `main`: the V1 migration, `application.yml`, and the Task 3 checklist. `.omo/**` files are workflow artifacts. No later-task Java/Kafka/business flow, tests, dependency, Docker/volume, seed-data, or extra-schema work is allowed.

## Stale-state and dirty-worktree result

- Branch: `feature/task-03-database-migration`
- `HEAD`: `7465f6f93f86b029c0f93d0cb761ff5db26a7636`
- `git merge-base main HEAD`: `7465f6f93f86b029c0f93d0cb761ff5db26a7636`
- Therefore `git diff --name-status main...HEAD` is empty: Task 3 is wholly uncommitted.
- Scope approval is based on the complete working tracked diff plus all untracked files, not branch-success prose.

## Exact changed-file inventory

### Product files

| State | Path |
|---|---|
| modified | `docs/TASK.md` |
| modified | `src/main/resources/application.yml` |
| untracked | `src/main/resources/db/migration/V1__create_charging_tables.sql` |

### Workflow files (`.omo/**`, non-product)

`git ls-files --others --exclude-standard` reported only these workflow paths in addition to the exact V1 path:

```text
.omo/boulder.json
.omo/drafts/task-03-database-migration.md
.omo/evidence/task-03-database-migration/f1-plan-compliance.md
.omo/evidence/task-03-database-migration/f2-code-quality.md
.omo/evidence/task-03-database-migration/f3/boot-first-result.txt
.omo/evidence/task-03-database-migration/f3/boot-first.log
.omo/evidence/task-03-database-migration/f3/boot-second-result.txt
.omo/evidence/task-03-database-migration/f3/boot-second.log
.omo/evidence/task-03-database-migration/f3/catalog-result.txt
.omo/evidence/task-03-database-migration/f3/catalog.txt
.omo/evidence/task-03-database-migration/f3/manual-qa/boot-first-result.txt
.omo/evidence/task-03-database-migration/f3/manual-qa/boot-first.log
.omo/evidence/task-03-database-migration/f3/manual-qa/boot-first.pid
.omo/evidence/task-03-database-migration/f3/manual-qa/compose-health.txt
.omo/evidence/task-03-database-migration/f3/manual-qa/compose-up.log
.omo/evidence/task-03-database-migration/f3/manual-qa/create-db.log
.omo/evidence/task-03-database-migration/f3/manual-qa/db-name.txt
.omo/evidence/task-03-database-migration/f4-scope.md
.omo/evidence/task-03-database-migration/malformed-migration-name.log
.omo/evidence/task-03-database-migration/task-03-database-migration-manual-qa.md
.omo/evidence/task-03-database-migration/todo-1-adversarial-verify.txt
.omo/evidence/task-03-database-migration/todo-1-schema.txt
.omo/evidence/task-03-database-migration/todo-2-config.txt
.omo/evidence/task-03-database-migration/todo-3-startup/boot-first-result.txt
.omo/evidence/task-03-database-migration/todo-3-startup/boot-first.log
.omo/evidence/task-03-database-migration/todo-3-startup/boot-first.pid
.omo/evidence/task-03-database-migration/todo-3-startup/boot-postgres-unavailable-result.txt
.omo/evidence/task-03-database-migration/todo-3-startup/boot-postgres-unavailable.log
.omo/evidence/task-03-database-migration/todo-3-startup/boot-second-result.txt
.omo/evidence/task-03-database-migration/todo-3-startup/boot-second.log
.omo/evidence/task-03-database-migration/todo-3-startup/boot-second.pid
.omo/evidence/task-03-database-migration/todo-3-startup/catalog-assertions.txt
.omo/evidence/task-03-database-migration/todo-3-startup/catalog-verification.sql
.omo/evidence/task-03-database-migration/todo-3-startup/catalog-verification.txt
.omo/evidence/task-03-database-migration/todo-3-startup/catalog.sql
.omo/evidence/task-03-database-migration/todo-3-startup/compose-restored.txt
.omo/evidence/task-03-database-migration/todo-3-startup/compose-up.txt
.omo/evidence/task-03-database-migration/todo-3-startup/create-db.txt
.omo/evidence/task-03-database-migration/todo-3-startup/gradlew-test-result.txt
.omo/evidence/task-03-database-migration/todo-3-startup/gradlew-test.log
.omo/evidence/task-03-database-migration/todo-3-startup/postgres-restore.txt
.omo/evidence/task-03-database-migration/todo-3-startup/postgres-stop.txt
.omo/evidence/task-03-database-migration/todo-3-startup/reset-qa-db.txt
.omo/evidence/task-03-database-migration/todo-3-startup/reverify-v1-count.txt
.omo/evidence/task-03-database-migration/todo-4-task-status.txt
.omo/plans/task-03-database-migration.md
.omo/start-work/ledger.jsonl
```

## Guardrail matrix

| Plan / project guardrail | Result | Evidence pointer |
|---|---|---|
| Exact Task 3 product inventory only | PASS | `git status --short --untracked-files=all`; product table above |
| No entities or enums | PASS | prohibited-path diff: no `src/main/java` changes |
| No repositories | PASS | prohibited-path diff: no `src/main/java` changes |
| No services/controllers/Kafka business flow | PASS | prohibited-path diff: no `src/main/java` changes |
| No tests or Testcontainers | PASS | no `src/test` or build-file changes |
| No dependency/build changes | PASS | no diff in `build.gradle`, `settings.gradle`, `gradle.properties`, or `gradle/wrapper/**` |
| No Docker/volume redesign | PASS | no diff in `docker-compose.yml`; only existing `./docker-compose.yml` found among Docker/Compose files |
| No seed data/repeatable migration | PASS | direct inspection of sole migration V1 |
| No FK, CHECK, trigger, procedure, extra index, or speculative column | PASS | V1 contains two tables, two UNIQUE constraints, and the one required composite index only |
| Config limited to schema ownership/naming | PASS | `application.yml` diff changes `ddl-auto` and adds `validate-migration-naming`; retains `baseline-on-migrate: true` |
| TASK edit limited to Task 3 status | PASS | one-line `docs/TASK.md` diff |
| No unrelated product edits | PASS | exact tracked/untracked inventory and allowlist result `UNEXPECTED_UNTRACKED=NONE` |
| No AI-slop/overfit maintenance burden | PASS | direct `remove-ai-slops` + `programming` pass: no production Java, helpers, abstractions, parsing/normalization, or tests added; SQL is a small single-purpose declarative migration |

## Automated verification

```text
git diff --check
DIFF_CHECK_EXIT=0

prohibited tracked diff over build files, docker-compose.yml, src/main/java, src/test
PROHIBITED_TRACKED_DIFF=NONE

untracked allowlist: exact V1 path plus .omo/**
UNEXPECTED_UNTRACKED=NONE
```

## Manual QA channel

Terminal/data-surface command reproduced exactly:

```sh
git status --short --untracked-files=all && git diff --name-status && git ls-files --others --exclude-standard
```

Result: PASS. Tracked product changes are only config/TASK; untracked product change is exactly V1; every other untracked file is under `.omo/**`.

## User outcome review

The product surface matches Task 3 exactly. V1 declares only the two required tables, exact PRD fields, two named uniqueness constraints, and the required `(session_id, sequence)` index. Configuration changes only hand schema ownership to Flyway/JPA validation and enable migration-name validation. The checklist changes only Task 3 to complete. No later product behavior or infrastructure redesign exists in the working tree.

## UltraQA classes

| Class | Result |
|---|---|
| `dirty_worktree` | PASS — complete tracked and untracked inventory recorded above |
| `stale_state` | PASS — HEAD equals merge base; working tree explicitly treated as authoritative |
| `misleading_success_output` | PASS — verdict derived from path allowlists and direct diffs, not reports/prose |
| Other adversarial classes | N/A — bounded, read-only scope audit |

## Cleanup receipt

- Product files edited by this audit: none
- Runtime assets/processes spawned: none
- Temporary project artifacts created: none
- Workflow artifact replaced: `.omo/evidence/task-03-database-migration/f4-scope.md`
- Cleanup required: none

## Evidence gaps

None for F4 scope fidelity. The product changes remain uncommitted, so `main...HEAD` is intentionally empty and cannot stand alone as scope evidence.
