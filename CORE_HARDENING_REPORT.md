# ziviDomeLive Core 0.1 hardening report

## Qualification decision

**LOCAL STATUS: CORE 0.1 LOCALLY QUALIFIED**

**INTEGRATION STATUS: NOT READY FOR 2.1 INTEGRATION**

Every mandatory local gate passed. The sole remaining blocker is execution of the new dedicated
GitHub Actions workflow after an authorized push. This branch was not pushed because the request
authorizes local commits but explicitly prohibits automatic push without separate authorization.
The project must not claim `READY FOR 2.1 INTEGRATION` until both remote jobs pass.

Core remains `0.1.0-SNAPSHOT`. This report does not call it Core 1.0 Stable and does not begin the
Processing 2.1 integration.

## 1. HEAD initial

`c60afb1718f541c7eab58ea081ca6d7e58015d82`

The initial worktree was clean. Git emitted its known non-fatal fsmonitor IPC warning while
reporting status, but listed no changed/untracked paths.

## 2. HEAD final

Qualified implementation/documentation HEAD before the evidence-only report commit:
`da8eb1fb8701cd1ab6f74167f52052ae0bf00d77`.

The final branch HEAD containing this report is recorded by `git rev-parse HEAD` in the final
handoff because a commit cannot embed its own content-derived SHA.

## 3. Branch

`architecture/zividomelive-core`

No branch switch, merge, rebase, tag, release, or push occurred.

## 4. Baseline

Historical Processing 2.0 golden baseline:
`0d2f03af8ff2dd4d077a50656018e61de08d653c`.

Pre-change hardening baseline:

| Command | Result |
|---|---|
| `./gradlew :core:clean :core:check` | PASS; source and JAR boundaries executed |
| `./gradlew qualificationTests` | PASS; 396/396 |
| `./gradlew build` | PASS |

Environment: Eclipse Temurin Java 17.0.18, Gradle 8.5, macOS 26.5.2 aarch64.

## 5. Semantic edge differences discovered

The clinical operation-by-operation matrix is in
[`core/docs/CORE_SEMANTIC_CONTRACT.md`](core/docs/CORE_SEMANTIC_CONTRACT.md).

The material differences are concentrated in `core.camera.OrbitCamera` and `core.math.Vec3`:

- Processing 2.0 accepts or propagates some NaN/infinite distances, deltas, lerp values, limits,
  collapse guards, and target components. Core requires finite values.
- Processing 2.0 stores inverted/non-finite distance limits. Core rejects them transactionally.
- A Processing `PVector` is mutable and permits non-finite components. Core `Vec3` is immutable and
  finite.
- Processing 2.0 `goTo`/`snapTo` may publish target fields before an invalid orientation fails.
  Core now validates the complete pose before publishing any field.
- Target-vector null exception policy differs (`IllegalArgumentException` in the Processing helper,
  Java `NullPointerException` at the Core value-object boundary).

Other audited types preserve their eligible 2.0 behavior or explicitly generalize it without
claiming false class equivalence.

## 6. Exact golden-equivalent behaviors

- FrameClock first-zero delta, clamp, backward anomaly, elapsed time, reset, and exception policy.
- SimulationTimeline fixed-step order, bounded catch-up, dropped units, rate, pause, position, and
  accumulator policy.
- Quaternion finite validation, axis normalization, multiply order, unit normalization, and
  shortest-path SLERP.
- SphericalOrientation cyclic deltas, local X/Z/Y axes, event order, non-finite ignore policy, and
  long-sequence normalization.
- Valid OrbitCamera signed-distance math, guard/clamp behavior, left-multiplied rotations, smooth
  updates, and immediate goal synchronization.
- Named action replacement, synchronous trigger, unregister, and terminal close.
- Frame queue ownership, finite-snapshot drain, ordering, and closed rejection.
- TaskGroup keyed admission, duplicate/capacity rejection, callback frame delivery, error delivery,
  key removal ordering, cancellation, stale suppression, and idempotent close.
- Ports identity ownership, drop-oldest overflow, budgets, pause, managed output, and reverse close.
- ResourceCache borrowed/owned creation, identity replacement, disposal, prefix removal, reverse
  clear/close, post-close reads, and failure isolation.
- Environment visible/intensity/yaw/source-orientation state.
- Explicit `ViewType` to `ProjectionType` mapping and domemaster calibration.

## 7. Intentional Core hardenings

The following are classified **CORE CONTRACT HARDENING**, not regressions:

- finite OrbitCamera constructor/distance/zoom/target/configuration inputs;
- finite immutable `Vec3` components;
- ordered finite distance ranges;
- finite collapse guard and interpolation factor;
- all-or-nothing `goTo` and `snapTo` pose publication.

Unit tests prove rejection class, preserved state, signed/zero/large finite values, guard/range edge
cases, zero rotations, zero axes, null values, and invalid quaternion normalization. A golden
fixture explicitly proves the stricter Core result beside the tolerated Processing 2.0 behavior.

## 8. Future 2.1 adapter compatibility obligations

The complete boundary is documented in
[`core/docs/PROCESSING_2_1_ADAPTER_CONTRACT.md`](core/docs/PROCESSING_2_1_ADAPTER_CONTRACT.md).

The future adapter must:

- convert Processing quaternions/PVectors without changing multiplication order or signed distance;
- retain Processing mouse routing, matrix application, UI ownership, images, scene-camera
  environment orientation, renderers, and outputs host-side;
- apply explicit `ViewType`/`ProjectionType` mapping and keep `RenderMode.FULL` host-side;
- preserve facade activation/frame authority and fresh activation resources on reload;
- decide at each stricter Core edge whether to sanitize/emulate the frozen 2.0 tolerance at the
  facade or publish an intentional 2.1 behavior change;
- never weaken Core automatically to inherit Processing-specific invalid state.

## 9. New golden fixtures

Eight golden tests were added to the prior nine:

1. explicit OrbitCamera invalid-input/transactional hardening difference;
2. TaskGroup admission, duplicate/capacity, result/error callbacks, and removal ordering;
3. TaskGroup cancellation, repeated close, and stale callback suppression;
4. ResourceCache ownership/replacement/removal/prefix/reverse-close semantics;
5. ResourceCache disposal exception isolation;
6. ScopedValue behavior derived from Processing environment ownership-safe restoration;
7. Core-eligible EnvironmentState equivalence;
8. ActivationState contract derived from actual SceneServices lifecycle behavior.

Task concurrency uses latches and state boundaries. Core executor rejection/removal/cancellation
uses a manual deterministic executor. No fixed sleeps were added.

## 10. Core unit test count

PASS: **119 total, 119 passed, 0 failed, 0 skipped**.

This is 24 more than the 95-test extraction baseline. The count comes from the final Core JUnit XML
after `:core:clean :core:check` / `clean test`.

## 11. Golden test count

PASS: **17 total, 17 passed, 0 failed, 0 skipped** when filtered to
`CoreGoldenEquivalenceTest`.

## 12. Root qualification count

PASS: **404 total, 404 passed, 0 failed, 0 skipped**.

The categories are not conflated:

- Core unit tests: 119 (separate `:core:test` suite);
- Core golden equivalence tests in the root suite: 17;
- Processing/root non-golden tests: 387;
- authoritative root qualification total: 404.

## 13. Dependency audit

PASS:

- `:core:compileClasspath`: `No dependencies`;
- `:core:runtimeClasspath`: `No dependencies`.

JUnit remains test-only.

## 14. Source boundary result

PASS: `:core:verifyCoreSourceBoundary` executed through `:core:check` and found no Processing,
JOGL, LWJGL, ControlP5, output backend, AWT/Swing, facade, Scene, or Processing-render reference.

## 15. JAR boundary result

PASS: `:core:verifyCoreJarBoundary` executed through `:core:check`. Direct `jar tf` inspection
found only `META-INF` and directory/class entries below
`com/victorvalentim/zividomelive/core` (plus required parent directory entries).

No `processing`, JOGL, LWJGL, facade, or Scene entry exists in the main JAR.

## 16. jdeps result

PASS:

```text
java.base
zividomelive-core-0.1.0-SNAPSHOT.jar -> java.base
```

## 17. POM result

PASS. The generated POM has coordinates
`com.victorvalentim.zividomelive:zividomelive-core:0.1.0-SNAPSHOT`, Apache-2.0 metadata,
developer/SCM metadata, and no `<dependencies>` section.

## 18. Maven local publication result

PASS. `:core:publishCorePublicationToCoreTestRepository` produced main, sources, and Javadoc JARs,
Gradle metadata, and POM only below `core/build/maven-test-repository`. No remote publication ran.

## 19. External consumer result

PASS. `:core:externalConsumerSmoke` generated a separate Gradle Java 17 project under
`core/build/external-consumer`, resolved only the local Maven coordinates, compiled, ran, and
printed:

```text
CORE_EXTERNAL_CONSUMER_OK frame=2 distance=-300.0
```

It imports and exercises `FrameClock`, `Quaternion`, `SphericalOrientation`, `OrbitCamera`, and
`ActionMap`. It does not use `project(":core")` or a source directory.

## 20. CI jobs created

Workflow: `.github/workflows/core-qualification.yml`, named **ziviDomeLive Core Qualification**.

- **Core Qualification (`ubuntu-latest`, `macos-latest`, `windows-latest`; Java 17):** wrapper
  validation, Core clean/check/Javadocs/artifacts, dependency audit, JAR inspection, jdeps, local
  Maven publication/POM inspection, external consumer, separate unit count, evidence upload.
- **Core Golden Compatibility (`ubuntu-latest`; Java 17):** Core units, golden-only fixtures,
  authoritative root qualification, and separate Core/golden/Processing counts.

Push trigger is restricted to `architecture/zividomelive-core`. Pull requests trigger only for
Core/build/golden/workflow paths. The workflow uses checkout/setup-java major v5, Gradle actions v4,
and upload-artifact v4 without broad edits to unrelated workflows.

## 21. CI results

**NOT RUN remotely.** No push was authorized. Local execution of every substantive workflow
command passed, and the YAML parsed successfully, but local results are not reported as GitHub
Actions matrix results.

After push authorization, both must pass:

- `ziviDomeLive Core Qualification` (all three hosts);
- `Automated Qualification`.

## 22. Warnings

- Git intermittently reports `fsmonitor_ipc__send_query: unspecified error` for the local daemon;
  disabling fsmonitor for read-only status produces normal results. No repository change is
  associated with the warning.
- The sandbox initially denied the Gradle wrapper access to the existing `~/.gradle` cache. The
  approved wrapper permission resolved it; this was not a project regression.
- The independent consumer uses `--no-daemon`, so Gradle prints its informational single-use
  daemon message.

No Java compiler, Javadoc, test, Core boundary, dependency, POM, JAR, or jdeps warning blocks the
qualification.

## 23. NOT RUN checks

- Remote GitHub Actions jobs: NOT RUN because push was not explicitly authorized.
- `git push`: NOT RUN by instruction.
- Remote Maven publication, release, tag, merge, and release creation: NOT RUN by instruction.
- Processing 4.0 revision-1285 baseline compile: NOT RUN; that exact installation remains
  unavailable and no Processing production code changed.
- Processing renderer benchmarks and interactive GPU examples: NOT RUN because rendering was
  explicitly out of scope and no renderer source changed.

Commands to run after explicit push authorization:

```text
git push origin architecture/zividomelive-core
gh run list --workflow core-qualification.yml --branch architecture/zividomelive-core --limit 1
gh run list --workflow automated-qualification.yml --branch architecture/zividomelive-core --limit 1
gh run watch <run-id> --exit-status
```

Use the returned run ID for each workflow. Do not merge, tag, release, or publish Maven remotely.

## 24. Known limitations

- Processing production code deliberately does not consume Core on this branch.
- Core 0.1 remains a development API, not a 1.0 compatibility freeze.
- Environment image/texture and scene-camera renderer orientation remain host-side.
- Camera input and matrix application remain host-side.
- The shared executor remains process-wide daemon state.
- The project uses the classpath and has no Java module descriptor.
- Cross-host evidence exists only after the GitHub matrix actually runs.

## 25. Recommendation

Checklist at report generation:

- [x] `:core:check` PASS
- [x] all Core tests PASS
- [x] golden fixtures PASS
- [x] root qualification PASS
- [x] zero production dependencies
- [x] source boundary PASS
- [x] bytecode/JAR boundary PASS
- [x] `jdeps = java.base`
- [x] Maven POM clean
- [x] external consumer PASS
- [x] semantic hardenings documented
- [x] Processing 2.1 adapter obligations documented
- [ ] dedicated Core CI PASS — remote run not authorized/not run
- [x] no Processing renderer changes
- [x] no public 2.0 API changes (`PublicApiCompatibilityTest` passed and root production source is unchanged)

Recommendation: **NOT READY FOR 2.1 INTEGRATION** until the dedicated remote Core CI and existing
Automated Qualification workflows pass. Once they pass with no new failure, the recommendation may
be updated to **READY FOR 2.1 INTEGRATION** and the artifact may be called **Core 0.1 Qualified**.
