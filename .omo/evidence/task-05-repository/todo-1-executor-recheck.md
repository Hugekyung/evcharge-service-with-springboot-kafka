# Task 5 Todo 1 — repository implementation recheck

Date: 2026-08-13 (Asia/Seoul)

## DoneClaim

```json
{
  "todo": "1",
  "claim": "Two exact Spring Data JPA repository interfaces are present and Task 5 is marked [완료].",
  "status": "verified",
  "product_files_changed_by_this_recheck": false,
  "tests": "./gradlew test exited 0; Gradle reported :test NO-SOURCE (no test sources), not a test-case pass",
  "manual_startup": "passed required repository/Flyway/Hibernate/Application markers",
  "cleanup": "spawned bootRun process group gone; Compose services preserved",
  "artifact": ".omo/evidence/task-05-repository/todo-1-executor-recheck.md"
}
```

## Baseline and dirty-worktree probe

Invocation:

```text
git status --short && git branch --show-current && git log -1 --oneline && git merge-base HEAD main && git status --porcelain=v1 --untracked-files=all
```

Binary observable: exit 0. Branch was `feature/task-05-repository`; `HEAD` and `main` merge-base were both `d34226f87b36490f85b514208aa13e05a05b87b1`. Pre-existing dirty paths were `.omo/boulder.json`, `docs/TASK.md`, `.omo/drafts/task-05-repository.md`, `.omo/evidence/task-05-repository/`, `.omo/plans/task-05-repository.md`, and the two repository files. No dirty path was reverted or otherwise changed by this recheck.

Captured artifact: this file.

## Failing-first source-contract probe

This probe was run before any attempted product change. It exits nonzero if a required exact signature is absent, a custom/native query is present, or the declared method count is wrong.

Exact invocation:

```sh
set -u
session='src/main/java/com/example/charging/repository/ChargingSessionRepository.java'
event='src/main/java/com/example/charging/repository/ChargingEventRepository.java'
violations=0
check_count() {
  pattern="$1"; file="$2"; expected="$3"; label="$4"
  count=$(rg -c "$pattern" "$file" 2>/dev/null || true)
  if [ "${count:-0}" -ne "$expected" ]; then echo "VIOLATION: $label (count=${count:-0}, expected=$expected)"; violations=$((violations+1)); else echo "OK: $label (count=$count)"; fi
}
check_count '^public interface ChargingSessionRepository extends JpaRepository<ChargingSession, Long> \{$' "$session" 1 'session repository base contract'
check_count '^    Optional<ChargingSession> findBySessionId\(String sessionId\);$' "$session" 1 'session lookup exact signature'
check_count '^public interface ChargingEventRepository extends JpaRepository<ChargingEvent, Long> \{$' "$event" 1 'event repository base contract'
check_count '^    boolean existsByEventId\(String eventId\);$' "$event" 1 'event idempotency exact signature'
check_count '^    List<ChargingEvent> findBySessionIdOrderBySequenceAsc\(String sessionId\);$' "$event" 1 'event history exact signature'
if rg -n '@Query|nativeQuery|createNativeQuery' "$session" "$event" >/dev/null 2>&1; then echo 'VIOLATION: custom query annotations or native SQL present'; violations=$((violations+1)); else echo 'OK: custom query annotations/native SQL absent'; fi
session_methods=$(awk '/public interface ChargingSessionRepository/{inside=1; next} inside && /^[[:space:]]*}[[:space:]]*$/{exit} inside && /;[[:space:]]*$/{count++} END {print count+0}' "$session")
event_methods=$(awk '/public interface ChargingEventRepository/{inside=1; next} inside && /^[[:space:]]*}[[:space:]]*$/{exit} inside && /;[[:space:]]*$/{count++} END {print count+0}' "$event")
check_count '^[[:space:]]*(Optional<ChargingSession>|boolean|List<ChargingEvent>).*;[[:space:]]*$' "$session" 1 'session declared method count'
check_count '^[[:space:]]*(Optional<ChargingSession>|boolean|List<ChargingEvent>).*;[[:space:]]*$' "$event" 2 'event declared method count'
printf 'AWK_METHOD_COUNTS session=%s event=%s\n' "$session_methods" "$event_methods"
if [ "$violations" -eq 0 ]; then echo 'NEGATIVE_PROBE=PASS (violations=0; any missing/extra contract would exit nonzero)'; exit 0; else echo "NEGATIVE_PROBE=FAIL (violations=$violations)"; exit 1; fi
```

Binary observable: exit 0; all five exact-signature checks, custom-query absence, and method-count checks printed `OK`; `AWK_METHOD_COUNTS session=1 event=2`; `NEGATIVE_PROBE=PASS`.

Captured artifact: this file.

## Product-state contract

Read-only inspected files: `ChargingSessionRepository.java`, `ChargingEventRepository.java`, `ChargingSession.java`, `ChargingEvent.java`, `docs/TASK.md`, `docker-compose.yml`, and `src/main/resources/application.yml`.

Binary observables:

- `ChargingSessionRepository extends JpaRepository<ChargingSession, Long>` and exactly `Optional<ChargingSession> findBySessionId(String sessionId)`.
- `ChargingEventRepository extends JpaRepository<ChargingEvent, Long>` and exactly `boolean existsByEventId(String eventId)` plus `List<ChargingEvent> findBySessionIdOrderBySequenceAsc(String sessionId)`.
- No `@Query`, native SQL, extra declared methods, implementations, pagination, projections, specs, tests, or fixtures in the repository interfaces.
- `docs/TASK.md` contains `### 5. Repository 구현 [완료]`.

Captured artifact: this file.

## Gradle and diff checks

Invocation: `./gradlew test`

Binary observable: exit 0; output included `:test NO-SOURCE` and `BUILD SUCCESSFUL`. `NO-SOURCE` is explicitly recorded as no test sources, not as evidence that test cases executed.

Invocation: `git diff --check`

Binary observable: exit 0 with no output.

Captured artifact: this file.

## Manual startup QA (terminal process surface)

Preparation invocation: `docker compose up -d`

Binary observable: exit 0; both `evcharging-postgres` and `evcharging-kafka` remained running. Final `docker compose ps` also showed both healthy. Volumes and services were preserved; no compose-down or volume operation was run.

Bounded boot invocation (the Python process creates a new process group, enforces a 60-second watchdog, terminates only that group, waits, and verifies group absence):

```sh
python3 -u -c 'import os,signal,subprocess,sys,time
p=subprocess.Popen(["./gradlew","bootRun","--args=--server.port=0"],start_new_session=True)
print(f"BOOT_PID={p.pid}",flush=True)
deadline=time.monotonic()+60
while p.poll() is None and time.monotonic()<deadline:
    time.sleep(1)
if p.poll() is None:
    print("WATCHDOG=TIMEOUT_60S",flush=True)
    try: os.killpg(p.pid,signal.SIGTERM)
    except ProcessLookupError: pass
    try: p.wait(timeout=10)
    except subprocess.TimeoutExpired:
        print("CLEANUP=SIGKILL",flush=True)
        try: os.killpg(p.pid,signal.SIGKILL)
        except ProcessLookupError: pass
        p.wait(timeout=10)
    code=124
else:
    code=p.returncode
    print(f"BOOT_EXIT_BEFORE_WATCHDOG={code}",flush=True)
try:
    os.killpg(p.pid,0)
except ProcessLookupError:
    print("CLEANUP_RECEIPT=process_group_gone",flush=True)
else:
    print("CLEANUP_RECEIPT=process_group_still_exists",flush=True)
    code=125
sys.exit(code)'
```

Binary startup observables from the captured log:

- `Finished Spring Data repository scanning ... Found 2 JPA repository interfaces.`
- `Database: jdbc:postgresql://localhost:5432/evcharging (PostgreSQL 16.14)`.
- Flyway: `Successfully validated 1 migration` and schema `1` up to date.
- Hibernate ORM initialized and `Initialized JPA EntityManagerFactory for persistence unit 'default'`.
- `Started ChargingApplication in 1.444 seconds`.
- Independent error scan of the captured startup output found no `QueryCreationException` or repository `BeanCreationException`.

Binary cleanup observables: the watchdog printed `WATCHDOG=TIMEOUT_60S`, `CLEANUP_RECEIPT=process_group_gone`; the wrapper invocation exited 124 by design after the bounded watchdog. A follow-up process probe printed `BOOT_PROCESS_ABSENT` (exit 0). Final `docker compose ps` printed healthy `evcharging-kafka` and `evcharging-postgres`.

Captured artifact: this file.

## Adversarial coverage

- `dirty_worktree`: PASS — baseline and final `git status --short` captured above; unrelated paths preserved.
- `stale_state`: PASS — main ancestry checked; live startup reached repository scanning, Flyway, Hibernate, and application-start markers.
- `misleading_success_output`: PASS — required positive markers were checked independently and negative scans found no query/repository bean errors; Gradle `NO-SOURCE` was labeled accurately.
- `hung_or_long_commands`: PASS — boot process had an explicit 60-second watchdog.
- `repeated_interruptions`: PASS — cleanup receipt was `process_group_gone`, and follow-up probe was `BOOT_PROCESS_ABSENT`.
- `malformed_input`: N/A — no malformed external input is in Todo 1’s repository contract.
- `prompt_injection`: N/A — no untrusted prompt-like input is part of this local source/startup verification.
- `cancel_resume`: N/A — no cancellation/resume event occurred during this attempt.
- `flaky_tests`: N/A — Gradle reported `NO-SOURCE`; no test cases were available to classify as flaky.

## Cleanup receipt

Only the spawned bootRun process group (`BOOT_PID=89859`) was targeted. It was terminated by the 60-second watchdog, waited for, and verified absent with `CLEANUP_RECEIPT=process_group_gone`; follow-up probe: `BOOT_PROCESS_ABSENT` (exit 0). Compose services and volumes were left running.
