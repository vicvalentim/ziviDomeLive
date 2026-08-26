# ziviDomeLive Core implementation report

## 1. Golden baseline

- Repository branch: `architecture/zividomelive-core`
- Required and observed starting SHA: `0d2f03af8ff2dd4d077a50656018e61de08d653c`
- Local `origin/architecture/zividomelive-core` ref at start: same SHA
- Starting worktree: clean (Git emitted a non-fatal local fsmonitor IPC warning)
- Golden source: Processing Library 2.0.0 production classes and executable tests

No branch switch, merge, push, remote publication, or Processing sketchbook deployment occurred.

## 2. Environment

- Java: Eclipse Temurin OpenJDK 17.0.18+8, 64-bit Server VM
- Gradle: 8.5 (Kotlin 1.9.20, Groovy 3.0.17, Ant 1.10.13)
- OS reported by Gradle: macOS 26.5.2, Apple Silicon/aarch64
- Processing CLI: Processing 4.5.6 at `/usr/local/bin/processing-java`

The first sandboxed Gradle invocation could not open the existing user-cache lock. The approved
Gradle wrapper access was then used for every build; this was an environment permission issue, not
a repository failure.

## 3. Baseline test results

Before changes:

| Command | Result |
|---|---|
| `./gradlew clean test` | PASS, 387 tests |
| `./gradlew build` | PASS |
| `./gradlew clean qualificationTests` | PASS, 387 passed, 0 failed, 0 skipped |
| `./gradlew buildReleaseArtifacts` | PASS; package verification passed |

## 4. Architecture before

Host-neutral state lived inside the single Processing library source set. Some candidates were
already pure Java (`SimulationTimeline`, queue, task and port algorithms), while others mixed pure
state with Processing values/events/resources (`Quaternion`, spherical orientation, orbit camera,
actions, and environment).

## 5. Architecture after

The repository is a minimal multi-project build with the unchanged Processing root plus `:core`:

```text
                 ziviDomeLive Core
                        ^
            +-----------+-----------+
            |                       |
    Processing Library         future Engine
            |                       |
      PGL / JOGL                   LWJGL
```

The root has a test-only dependency on `:core` for golden fixtures. Root production has no Core
dependency yet, and Core has no dependency on root or Processing.

## 6. Core packages

`action`, `camera`, `environment`, `lifecycle`, `math`, `ports`, `projection`, `task`, and `time`.

## 7. Core public types

- Time: `FrameClock`, `SimulationTimeline`
- Math: `Quaternion`, `Vec3`
- Camera: `CameraPose`, `OrbitCamera`
- Projection: `SphericalOrientation`, `ProjectionType`, `DomemasterSettings`
- Actions: `ActionMap`
- Tasks: `FrameThreadQueue`, `TaskGroup`
- Ports: `InputPort`, `OutputPort`, `Ports`
- Environment: `EnvironmentState`
- Lifecycle/resources: `ActivationState`, `ScopedValue`, `ResourceCache`

`CoreTaskExecutor` is package-private implementation state.

## 8. Extracted semantics

- Monotonic clamped frame time and bounded fixed-step simulation.
- Immutable float quaternion axis-angle, multiplication, normalization, and shortest-path SLERP.
- Event-order local-X pitch, local-Z yaw, and local-Y roll with shortest cyclic deltas.
- Signed-distance orbit camera current/goal pose, world-space multiplication order, direct snap,
  immediate manipulation, interpolation, distance limits, and collapse guard.
- Synchronous named actions with replacement and terminal close.
- Bound/rebindable finite-snapshot frame-thread delivery.
- Bounded keyed background work, duplicate/capacity rejection, no public Future, callback delivery,
  cancellation, and stale callback suppression.
- Generic bounded port input with drop-oldest telemetry, finite frame budgets, pause behavior,
  identity registration, guarded output, and reverse adapter close.
- Visual environment visibility/intensity/yaw/source orientation without image/texture ownership.
- Four projection identities plus qualified domemaster FOV/size state.
- Coalesced activation reload state, ownership-safe scoped restoration, and generic borrowed/owned
  resource caching.

## 9. Processing-specific semantics intentionally excluded

`Scene`, `SceneServices`, `SceneManager`, `PApplet`, `PVector`, `PMatrix3D`, `PImage`, `PShape`,
`PShader`, `PGraphicsOpenGL`, Processing key/mouse events, camera graphical application,
`applyWithViewLighting`, target suppliers returning `PVector`, ControlP5 bindings, renderers,
shaders, render targets, outputs, and the facade remain in the Processing host.

## 10. Deferred future services

No Lighting/Material/PostFX/Physics service, PBR/HDR, RenderGraph, graphics backend/context/target,
texture/shader/mesh abstraction, GLFW/LWJGL/bgfx/Vulkan/Metal integration, standalone window,
TouchDesigner integration, `CoreScene`, or `RenderContext` was introduced.

## 11. Files created

- `settings.gradle.kts`, the required audit, this report, and one root golden fixture.
- The complete `core/` module build, README, architecture/extraction/consumer documents, six ADRs,
  20 production Java files (19 public types plus one package-private executor), and 14 Core test
  classes.

The authoritative exact list is `git diff --name-status <baseline>..HEAD` after the final commit.

## 12. Existing files modified

Only root `build.gradle.kts`: it adds the test-only `project(":core")` dependency used by golden
equivalence fixtures. Root version, production dependencies, public API, source, examples,
renderers, outputs, UI, shaders, and packaging behavior were not changed.

## 13. Production dependency graph

Both `:core:compileClasspath` and `:core:runtimeClasspath` report `No dependencies`. JUnit 5 is
test-only. Root production dependencies are unchanged.

## 14. Threading model

The host binds `FrameThreadQueue` at its authoritative frame boundary. Calls from its owner may run
immediately; worker calls enqueue. Drain runs a finite snapshot, so recursively or concurrently
published work waits for the next frame. `TaskGroup` uses a process-wide bounded daemon executor
with one worker per JVM-reported processor and queue capacity 256. Results/errors return through
the frame queue. `Ports` accepts arbitrary producer threads and invokes handlers only during a
bound frame-thread drain.

## 15. Lifecycle model

One host activation owns actions, task groups, queues, ports, overrides, and caches. Reload requests
coalesce. Pause drops stale input. Stopping clears reload/admission before domain disposal. Closing
tasks suppresses old callbacks; closing ports uses reverse adapter order; scoped values restore
only state still owned by the closing scope. Core does not invoke scene callbacks or advance state
during rendering.

## 16. Spatial convention

`+Z` is dome front and `-Z` rear. Distance remains signed; negative values are valid. Smooth zoom
with collapse guard retains its side of zero and stops at the qualified boundary. Scene-camera
orientation remains distinct from spherical pitch/yaw/roll and fixed environment-source alignment.

## 17. Golden equivalence results

PASS: 9/9 root integration fixtures, 0 failed/skipped. They compare deterministic values,
exceptions, transition ordering, queue snapshots, drop behavior, projection mapping, and camera
math for 2.0 versus Core. Core tests never depend on Processing.

## 18. Core unit tests

PASS: 95/95 tests, 0 failed, 0 errors, 0 skipped across all nine Core packages. Coverage includes
the requested time/timeline, quaternion/spherical, camera, action, queue/task, ports, environment,
projection, activation, scoped restoration, and resource ownership cases.

## 19. Root regression tests

PASS: post-implementation `qualificationTests` reported 396 total, 396 passed, 0 failed, 0 skipped
(the 387-test baseline plus 9 golden fixtures). `clean test` and `build` also passed as a
multi-project build.

## 20. JAR inspection

The main JAR contains only `META-INF` and
`com/victorvalentim/zividomelive/core/**`. Automated source and class-symbol boundary tasks reject
Processing, JOGL, LWJGL, ControlP5, Syphon, Devolay/NDI, Spout, AWT/Swing graphics, facade, root
Scene, and Processing render references.

## 21. jdeps result

`jdeps --multi-release 17 --print-module-deps` returned exactly `java.base`; summary returned
`zividomelive-core-0.1.0-SNAPSHOT.jar -> java.base`.

## 22. Maven POM result

PASS. Coordinates are
`com.victorvalentim.zividomelive:zividomelive-core:0.1.0-SNAPSHOT`. The POM includes name,
description, Apache-2.0 license, developer and SCM metadata, and declares no dependencies.

## 23. External Maven consumer smoke result

PASS. An independent Gradle Java 17 project under `build/core-external-consumer/` used only the
build-local Maven repository and Core coordinates—no `project(":core")` and no root source. Offline
`clean run` printed:

```text
CORE_EXTERNAL_CONSUMER_OK frame=2 distance=-300.0
```

## 24. Javadoc result

PASS. HTML Javadocs and Javadoc JAR were generated. Non-missing doclint remains enabled; repetitive
missing-tag warnings are disabled because public methods already carry focused narrative API
documentation. The final warning-mode build emitted no Core Javadoc warnings.

## 25. Qualification tasks

Executed successfully:

- Baseline and final `clean test`, `build`, and `clean qualificationTests`
- Baseline and final `buildReleaseArtifacts` plus `verifyProcessingPackage`
- `:core:clean`, `:core:test`, `:core:check`, `:core:jar`, `:core:sourcesJar`, `:core:javadoc`,
  `:core:javadocJar`, POM generation, and publication to the build-local test Maven repository
- `:core:verifyCoreSourceBoundary`, `:core:verifyCoreJarBoundary`, and aggregate boundary check
- `:core:dependencies` for compile and runtime production classpaths
- `benchmarkDoctor` (Processing CLI detected; no benchmark or deployment was started)
- JAR listing, `jdeps`, POM inspection, and explicit prohibited-reference search
- Independent offline Maven consumer compile/run
- Processing 4.5.6 SolarSystem build from a temporary sketch copy whose `code/` contained the
  just-built `ziviDomeLive.jar`; result: `Finished.`
- `git diff --check`

## 26. NOT RUN tasks and exact reasons

- `compileProcessing4Baseline`: NOT RUN. Its contract requires the official Processing 4.0
  revision-1285 `core.jar`, `jogl-all.jar`, and `gluegen-rt.jar` directory via
  `-Pprocessing4BaselineLibrary`. Only Processing 4.5.6 is installed; substituting it would not
  qualify the 4.0 baseline.
- `deployToProcessingSketchbook` and `deployBenchmarkLibrary`: NOT RUN to avoid overwriting the
  user's active sketchbook, as required.
- `runBenchmark` / `benchmarkSuite`: NOT RUN. No renderer production behavior changed; these are
  interactive/GPU performance workflows rather than deterministic Core qualification.
- Remote publication/release, Git push, and `git filter-repo`: NOT RUN by explicit instruction.

## 27. Known limitations

- Processing production types do not delegate to Core on this branch; integration is deliberately
  deferred to 2.1.
- Core 0.1 is a development API, not a frozen 1.0 contract.
- Environment has no source image/texture; hosts own it.
- Camera has no host input events or render-matrix application.
- The shared task executor is process-wide daemon state and intentionally is not shut down by an
  activation.
- There is no Java module descriptor; classpath publication is intentional for this phase.

## 28. Future 2.1.0 integration plan

1. Add Processing adapters for vector/matrix conversions, camera input/application, action input
   bindings, environment image ownership, and explicit `ViewType` mapping.
2. Delegate frozen `FrameClock`, `SimulationTimeline`, quaternion/orientation, camera, task, action,
   port, and resource semantics without changing artist-facing public signatures.
3. Retain `SceneServices` construction/closure and facade frame/lifecycle authority.
4. Run the 2.0 API snapshot, all golden fixtures, full qualification, affected examples, and
   renderer/output tests before any 2.1 release change.

## 29. Future repository-extraction plan

After this branch is accepted, run in a separate migration operation:

```text
git filter-repo \
  --path core/ \
  --path-rename core/:
```

Then add wrapper/settings, copy the Apache-2.0 license, configure CI and publication/release, and
update SCM coordinates. No Java refactor or Processing removal is required after filtering.

## 30. Local commits created

- `docs(core): record extraction baseline and architecture boundary`
- `build(core): introduce standalone Java core module`
- `feat(core): add time and simulation primitives`
- `feat(core): add math and spherical orientation`
- `feat(core): add frame-thread and task services`
- `feat(core): add action and port services`
- `feat(core): add host-independent orbit camera`
- `feat(core): add environment projection and lifecycle state`
- `test(core): add golden compatibility qualification`
- `docs(core): complete qualification and extraction report` (final documentation commit)

All commits are local on `architecture/zividomelive-core`; none were pushed.

## Final `core/` source tree

```text
core/
├── ARCHITECTURE.md
├── EXTERNAL_CONSUMER_SMOKE.md
├── EXTRACTION_READINESS.md
├── README.md
├── build.gradle.kts
├── docs/adr/{0001-core-independence,0002-spatial-convention,
│   0003-update-once-render-many,0004-core-host-boundary,
│   0005-processing-golden-reference,0006-future-repository-extraction}.md
└── src/
    ├── main/java/com/victorvalentim/zividomelive/core/
    │   ├── action/ActionMap.java
    │   ├── camera/{CameraPose,OrbitCamera}.java
    │   ├── environment/EnvironmentState.java
    │   ├── lifecycle/{ActivationState,ResourceCache,ScopedValue}.java
    │   ├── math/{Quaternion,Vec3}.java
    │   ├── ports/{InputPort,OutputPort,Ports}.java
    │   ├── projection/{DomemasterSettings,ProjectionType,SphericalOrientation}.java
    │   ├── task/{CoreTaskExecutor,FrameThreadQueue,TaskGroup}.java
    │   └── time/{FrameClock,SimulationTimeline}.java
    └── test/java/com/victorvalentim/zividomelive/core/
        ├── action/ActionMapTest.java
        ├── camera/OrbitCameraTest.java
        ├── environment/EnvironmentStateTest.java
        ├── lifecycle/{ActivationState,ResourceCache,ScopedValue}Test.java
        ├── math/QuaternionTest.java
        ├── ports/PortsTest.java
        ├── projection/{ProjectionState,SphericalOrientation}Test.java
        ├── task/{FrameThreadQueue,TaskGroup}Test.java
        └── time/{FrameClock,SimulationTimeline}Test.java
```
