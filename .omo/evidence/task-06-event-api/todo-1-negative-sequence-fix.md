# Task 6 Todo 1 negative-sequence validation fix

## Change

Added the `sequence=-1` request to `invalidBoundaryRequests()` in `src/test/java/com/example/charging/controller/ChargingEventControllerTest.java`. No production code changed.

## Focused verification

Scenario: MockMvc receives a structurally valid charging event with `sequence=-1`.

Invocation 1: `./gradlew test --tests '*ChargingEventControllerTest' --tests '*ChargingEventPublisherContractTest'`

Output:

```text
> Task :compileJava UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestJava
> Task :processTestResources NO-SOURCE
> Task :testClasses
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
> Task :test

BUILD SUCCESSFUL in 1s
4 actionable tasks: 2 executed, 2 up-to-date
EXIT_CODE=0
```

Invocation 2 (flake check): `./gradlew test --tests '*ChargingEventControllerTest' --tests '*ChargingEventPublisherContractTest'`

Output:

```text
> Task :compileJava UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestJava UP-TO-DATE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
> Task :test UP-TO-DATE

BUILD SUCCESSFUL in 489ms
4 actionable tasks: 4 up-to-date
EXIT_CODE=0
```

Binary observable: `build/test-results/test/TEST-com.example.charging.controller.ChargingEventControllerTest.xml` records `tests=13`, `failures=0`, `errors=0`, `skipped=0`. Its parameterized test contains invocation `[7]` with `"sequence": -1`; the test asserts HTTP 400 and an empty publisher command list for every invalid request. The contract XML records `tests=2`, `failures=0`, `errors=0`, `skipped=0`.

Artifacts:

- `build/test-results/test/TEST-com.example.charging.controller.ChargingEventControllerTest.xml`
- `build/test-results/test/TEST-com.example.charging.application.ChargingEventPublisherContractTest.xml`

## Full verification

Invocation: `./gradlew test`

Output:

```text
> Task :compileJava UP-TO-DATE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestJava UP-TO-DATE
> Task :processTestResources NO-SOURCE
> Task :testClasses UP-TO-DATE
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
> Task :test

BUILD SUCCESSFUL in 1s
4 actionable tasks: 1 executed, 3 up-to-date
EXIT_CODE=0
```

Binary observable: the same two Surefire XML files contain no failures or errors after the full suite.

## Cleanup

Post-QA invocation: `ps -Ao pid=,command= | rg '[g]radle.*Test worker|[C]hargingApplication' || true`

Output: empty; no Gradle Test worker or ChargingApplication process remained. The shared Gradle daemon was retained.

No Docker or other infrastructure was started.
