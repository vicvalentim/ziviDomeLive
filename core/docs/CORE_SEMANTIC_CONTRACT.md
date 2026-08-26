# Core 0.1 semantic contract

## Status and interpretation

This document qualifies `zividomelive-core:0.1.0-SNAPSHOT` against the frozen ziviDomeLive
Processing 2.0 implementation at baseline `0d2f03af8ff2dd4d077a50656018e61de08d653c`.
It does not freeze a Core 1.0 API and does not authorize the Processing library to consume Core.

The comparison uses three labels:

- **EXACT GOLDEN EQUIVALENCE**: observable host-neutral behavior is the same, allowing only
  host-neutral terminology in exception/log messages.
- **CORE CONTRACT HARDENING**: Core deliberately rejects invalid state that Processing 2.0
  tolerated or propagated. The future 2.1 adapter must decide how the frozen Processing API
  preserves compatibility before delegating.
- **DERIVED CORE CONTRACT**: Core generalizes behavior proven by 2.0 lifecycle tests; there is no
  false claim that the two classes are structurally equivalent.

Rejected Core calls are transactional unless explicitly stated: validation completes before
observable state changes. `OrbitCamera.goTo` and `snapTo` have a regression fixture for this rule.

## Qualified comparison

| Core type | Operation | 2.0 behavior | Core behavior | Same? | Intentional hardening? | Reason | 2.1 adapter consequence |
|---|---|---|---|---|---|---|---|
| `OrbitCamera` | constructor distance | Accepts every `float`, including NaN and infinities | Requires finite signed distance; negative and zero remain valid | No | Yes — **CORE CONTRACT HARDENING** | Invalid initial state must not enter a new host-neutral controller | Preserve the 2.0 constructor contract at the Processing boundary or publish an intentional 2.1 compatibility change |
| `OrbitCamera` | `setDistance` / `setDistanceImmediate` | Finite values clamp; infinities clamp through `PApplet.constrain`; NaN may propagate | Rejects every non-finite value before mutation; finite signed values clamp | No | Yes — **CORE CONTRACT HARDENING** | Prevent permanently non-finite camera state | Adapter must pre-handle non-finite values according to the frozen facade policy |
| `OrbitCamera` | `zoom` / `zoomImmediate` | Does not validate amount; overflow/non-finite arithmetic is passed to the guard | Requires finite amount and rejects before current/goal mutation | No | Yes — **CORE CONTRACT HARDENING** | A delta API must not corrupt pose state | Adapter owns legacy tolerance for non-finite mouse/programmatic deltas |
| `OrbitCamera` | `setDistanceLimits` | Stores limits without finite/order validation, even when `minimum > maximum` | Both limits must be finite and ordered; rejection preserves old limits and pose | No | Yes — **CORE CONTRACT HARDENING** | An inverted range has no stable cross-host clamp meaning | Validate or emulate 2.0 behavior in the adapter; never pass invalid ranges into Core |
| `OrbitCamera` | negative signed distance | Valid and used by the established `-Z` camera convention | Valid, including wholly negative and zero-crossing ranges | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Spatial compatibility is contractual | Convert values without taking absolute value |
| `OrbitCamera` | range crossing zero | Allowed; without guard a distance may change sign | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Signed orbit sides are intentional | Preserve signed values |
| `OrbitCamera` | `setCollapseGuard` negative/zero | Negative clamps to zero; zero disables the dead zone | Same for finite values | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Established camera behavior | Direct mapping |
| `OrbitCamera` | `setCollapseGuard` NaN/infinity | 2.0 accepts them; NaN can poison guard state | Rejects all non-finite values before mutation | No | Yes — **CORE CONTRACT HARDENING** | Guard configuration must remain meaningful | Adapter must retain legacy tolerance or reject as an intentional Processing API change |
| `OrbitCamera` | guard larger than allowed range | Final distance remains clamped to the configured range, so the guard may be unsatisfiable | Same for finite configuration | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Range remains the final authority | No special conversion; document incompatible configuration |
| `OrbitCamera` | `setLerpFactor` finite values | Clamps to `[0.001, 1]` | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Preserves interpolation cadence | Direct mapping |
| `OrbitCamera` | `setLerpFactor` non-finite | Infinities clamp; NaN may be stored | Rejects non-finite values without mutation | No | Yes — **CORE CONTRACT HARDENING** | Interpolation cannot safely advance with NaN | Adapter must decide legacy normalization before delegation |
| `OrbitCamera` | scalar `setTarget` / scalar `snapTo` | Stores target components without finite validation | Requires every component finite | No | Yes — **CORE CONTRACT HARDENING** | Core pose values are always finite | Processing `PVector` conversion must validate or preserve legacy behavior outside Core |
| `OrbitCamera` | `Vec3` target overloads | `PVector` overload rejects null with `IllegalArgumentException`, but accepts non-finite components | Null target throws `NullPointerException`; `Vec3` construction rejects non-finite components | No | Yes — **CORE CONTRACT HARDENING** | Java value-object null conventions and finite invariant | Adapter translates exception policy if exact public compatibility is required |
| `OrbitCamera` | null orientation | Rejects with `IllegalArgumentException` | Same class and equivalent message | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Orientation is required | Direct mapping |
| `OrbitCamera` | zero quaternion orientation | Normalization rejects with `IllegalStateException` | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Zero quaternion is not a rotation | Direct mapping |
| `OrbitCamera` | `goTo` / `snapTo` rejected orientation | 2.0 may write target before orientation normalization rejects | Validates the complete pose first; rejected calls publish no partial target/goal | No | Yes — **CORE CONTRACT HARDENING** | Multi-field pose operations are atomic in Core | Adapter must validate first or deliberately emulate the old partial-write edge case outside Core |
| `OrbitCamera` | rotation axis/angle | Delegates to qualified quaternion rules | Same: finite values required; zero angle with a finite zero axis is identity; non-zero angle with zero axis rejects | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Quaternion math is shared semantics | Convert `PVector` to `Vec3`; keep axis order and world-space left multiplication |
| `OrbitCamera` | very large finite pose values | Accepted where arithmetic does not overflow; distance is later clamped by limits | Same finite distance behavior | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Finite is the Core scalar boundary | Do not narrow or absolutize values |
| `Quaternion` | construction | Rejects NaN and both infinities for every component | Same exception class/message | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Already hardened in 2.0 | Direct component conversion |
| `Quaternion` | axis-angle | Normalizes finite axis; zero angle is identity; non-zero angle with zero magnitude rejects | Same float math and exceptions | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Rotation construction is golden math | `PVector` overload remains adapter-only |
| `Quaternion` | multiply / null | Ordered non-commutative `this * other`; null rejects with `IllegalArgumentException` | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Multiplication order affects every camera/projection | Never reverse operands in adapter |
| `Quaternion` | normalize zero | Throws `IllegalStateException` | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Zero has no rotational meaning | Direct mapping |
| `Quaternion` | SLERP | Normalizes endpoints, clamps finite factor, shortest path, rejects non-finite/null | Same float thresholds and ordering | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Numerical compatibility | Direct mapping |
| `Vec3` | construction | Closest host value is mutable `PVector`, which accepts non-finite components | Immutable record; every component must be finite | No | Yes — **CORE CONTRACT HARDENING** | A Core pose cannot acquire invalid values after construction | Copy `PVector` values and decide legacy failure behavior at the adapter boundary |
| `SphericalOrientation` | pitch/yaw/roll | Ignores NaN/infinities; preserves last accepted accumulator | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Facade controls already use ignore policy | Direct scalar delegation |
| `SphericalOrientation` | cyclic delta/order | Shortest wrapped delta; local X pitch, local Z yaw, local Y roll; event-order composition | Same float constants and quaternion order | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Avoids orientation drift/regression | Never reconstruct from Euler angles |
| `SphericalOrientation` | reset/long sequence | Identity/reset and normalized long-running composition | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Stable projection control | Direct mapping |
| `DomemasterSettings` | FOV/size finite values | Facade clamps FOV `[0,360]`, size `[0,100]` | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Existing calibration semantics | Adapter updates Core and retains renderer/UI publication host-side |
| `DomemasterSettings` | non-finite values/reset | Facade ignores non-finite setter values; defaults are FOV 210 and size 100 | Same; Core additionally groups reset | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Existing calibration policy | Direct scalar mapping; host performs side effects |
| `FrameClock` | clock source/null | Package-owned source requires non-null | Public Core constructor requires non-null | Yes semantically | No — **EXACT GOLDEN EQUIVALENCE** | Deterministic host testing | Host supplies monotonic source or uses default |
| `FrameClock` | first/backward/large tick | First delta zero; backward anomaly contributes zero and rebases; large delta clamps | Same double arithmetic | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Frame timing is golden | Tick once per host frame |
| `FrameClock` | max delta invalid | Zero, negative, NaN and infinities reject without changing clamp | Same exception class/message | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Clamp must be finite positive | Direct mapping |
| `SimulationTimeline` | advance invalid/null | Null stepper rejects; negative/non-finite real delta rejects | Same and no state mutation before validation | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Fixed-step safety | Direct mapping |
| `SimulationTimeline` | fixed-step/drop ordering | Bounded callbacks, position update before callback, whole-step drop telemetry, retained remainder | Same double arithmetic/order | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Simulation results depend on order | Advance once per frame, never per render face |
| `SimulationTimeline` | invalid configuration | Non-finite/negative rate, non-positive fixed step and substep budget reject | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Stable accumulator invariants | Direct mapping |
| `SimulationTimeline` | pause/rate zero/reset | No elapsed replay; resets retain configuration as documented | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Scene owns timeline policy | Direct mapping |
| `ActionMap` | register duplicate key | Replaces the named runnable deterministically | Same Core registry behavior | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Named action replacement is golden | Processing key/mouse bindings remain adapter maps |
| `ActionMap` | blank/null | Blank/null name rejects with `IllegalArgumentException`; null action rejects with `NullPointerException` | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Registry invariants | Direct mapping |
| `ActionMap` | trigger/order | Synchronous caller-thread invocation; missing name returns false | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | No hidden event bus | Processing adapter invokes from its frame/input thread |
| `ActionMap` | close | Clears once; all registry operations reject afterward | Same, with host-neutral message and public `AutoCloseable` | Yes semantically | No — **EXACT GOLDEN EQUIVALENCE** | Activation ownership | Host closes it; artist API must not expose ownership controls |
| `FrameThreadQueue` | ownership/thread misuse | Explicit owner/rebind; drain from wrong thread rejects | Same, replacing Processing terminology with frame-thread terminology | Yes semantically | No — **EXACT GOLDEN EQUIVALENCE** | Host-neutral boundary | Bind during Processing `pre()` |
| `FrameThreadQueue` | drain ordering | Runs a finite snapshot; recursively/concurrently enqueued work waits | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Prevents unbounded frame work | Drain once at authoritative frame boundary |
| `FrameThreadQueue` | close | Repeated close is harmless, pending work drops, new work/drain rejects | Same | Yes semantically | No — **EXACT GOLDEN EQUIVALENCE** | Stale activation isolation | Close before old activation can publish |
| `TaskGroup` | submit/duplicate/capacity | Non-blank keyed work; duplicate or full group returns false | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Bounded non-blocking admission | Direct delegation |
| `TaskGroup` | result/error callback | Completion removes task key, then queues callback for frame drain | Same; deterministic unit fixture proves removal-before-callback | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Allows key reuse without running callback off-thread | Drain callback queue at `pre()` |
| `TaskGroup` | executor rejection | Bounded executor rejection returns false and releases key; unexpected runtime failure propagates after key release | Same algorithm; Core uses package-private deterministic executor injection in tests | Yes semantically | No — **EXACT GOLDEN EQUIVALENCE** | Prevent leaked busy keys | Processing host may retain its existing executor until adapter integration |
| `TaskGroup` | cancellation/close | Cancels in-flight futures, clears busy keys, rejects new work, suppresses stale callbacks; close repeats safely | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Activation isolation | Close before releasing activation queue/resources |
| `TaskGroup` | null/blank/callbacks | Blank key rejects; null task/result/error callback rejects | Same exception classes | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Explicit callback contract | Direct mapping |
| `Ports` | capacity/budget | Positive bounds required; overflow drops oldest and increments telemetry | Same defaults (256/32) and behavior | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Fresh device state wins | Direct mapping |
| `Ports` | identity/duplicates | Adapter registration uses instance identity; duplicate identity rejects | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Lifecycle ownership is identity-based | Preserve one Core connection per adapter instance |
| `Ports` | input/order/thread | Arbitrary producer accepted; handlers run in order only during owner-thread drain; handler failure is isolated | Same, with host-neutral logs | Yes | No — **EXACT GOLDEN EQUIVALENCE** | External I/O never enters render thread directly | Adapter forwards values only; host drains |
| `Ports` | pause/resume/overflow | Pause clears pending input and rejects new input/output until resume | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | No stale replay | Bind facade pause/resume to Core ports |
| `Ports` | close | Stops admission, clears pending, closes adapters reverse-order, isolates cleanup errors, repeats safely | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Deterministic activation cleanup | Host owns close; managed output fails closed |
| `ResourceCache` | get/contains/create | Key validation, null factory/resource rejection, one creation per key | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Generic extraction of the scene cache | Processing decides borrowed/owned disposer |
| `ResourceCache` | replacement/same identity | Different owned value is disposed; same instance replacement is not disposed early and adopts the new entry/disposer | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Identity prevents disposing a still-retained object | Direct mapping |
| `ResourceCache` | remove/prefix/clear | Prefix removal follows insertion order; clear/close dispose owned entries in reverse insertion order; borrowed entries only drop references | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Deterministic native cleanup | Direct mapping |
| `ResourceCache` | disposal failure | Runtime disposal exception is logged and remaining cleanup continues | Same, with host-neutral log terminology | Yes semantically | No — **EXACT GOLDEN EQUIVALENCE** | One broken disposer must not leak later entries | Direct mapping |
| `ResourceCache` | closed state | Reads and idempotent cleanup remain safe; new create/put admission rejects; repeated close is harmless | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Safe observation after shutdown | Host must not treat `remove`/`clear` no-op cleanup as new admission |
| `ScopedValue` | first/multiple set | Scene environment service captures previous state once and tracks the latest applied state | Generic Core scope does the same | Derived | No — **DERIVED CORE CONTRACT** | Extracts the ownership algorithm, not the Processing service | Create one scoped value per host property |
| `ScopedValue` | close while owned | Processing service restores A after applying B when B is still current | Same | Derived | No — **DERIVED CORE CONTRACT** | Activation owns only the value it applied | Use property-appropriate equality |
| `ScopedValue` | later owner C | Processing service does not overwrite C | Same | Derived | No — **DERIVED CORE CONTRACT** | Later ownership wins | Never restore unconditionally |
| `ScopedValue` | null/custom equality | PImage scope permits null and float/quaternion properties use identity/bitwise component comparison | Generic Core scope permits null when reader/writer do; caller supplies equality | Derived | No — **DERIVED CORE CONTRACT** | Hosts define representation equality | PImage remains host-side; floats/quaternions use qualified equality |
| `ScopedValue` | close/access | Untouched close writes nothing; repeated close is harmless; get/set after close reject | Same lifecycle pattern | Derived | No — **DERIVED CORE CONTRACT** | Terminal scope semantics | Host closes after scene dispose ordering requires restoration |
| `ActivationState` | reload coalescing | `SceneServices` coalesces requests with an atomic flag until consumed | Same | Derived | No — **DERIVED CORE CONTRACT** | Generalized admission state, not class equivalence | Facade remains authoritative consumer at frame boundary |
| `ActivationState` | pause/resume | Facade/SceneServices pause activation input and resume it without consuming reload | Core records pause independently without invoking services | Derived | No — **DERIVED CORE CONTRACT** | Core cannot own Processing callbacks/ports | Adapter coordinates Core state with Processing services |
| `ActivationState` | stopping/closed admission | `prepareForDispose` clears reload and rejects service work; final close is idempotent | `beginStopping` clears reload/admission; `close` marks terminal; requests after either reject | Derived | No — **DERIVED CORE CONTRACT** | Explicit two-phase host-neutral state | Map prepare/dispose phases, do not replace Scene lifecycle ordering |
| `EnvironmentState` | visible/intensity/yaw | Visible boolean; negative intensity clamps to zero; non-finite intensity/yaw ignored | Same | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Eligible scalar state | Direct scalar mapping |
| `EnvironmentState` | source orientation | Null means identity; non-null is normalized; invalid zero quaternion rejects without replacing current value | Same components/behavior | Yes | No — **EXACT GOLDEN EQUIVALENCE** | Fixed source alignment is shared state | Convert Processing/Core quaternion at boundary |
| `EnvironmentState` | reset | No grouped reset method on internal 2.0 state; defaults are individually established | Restores qualified visible/unit/zero/identity defaults | Derived convenience | No | Host-neutral reset groups existing defaults | Adapter may use it only when activation policy calls for a full reset |
| `EnvironmentState` | image and scene-camera orientation | Stores borrowed `PImage` and separate renderer-facing scene-camera orientation | Deliberately absent | No by design | No | These are host/render responsibilities | Keep image ownership and scene-camera publication in Processing |

## Qualification evidence

- Core unit tests contain no Processing dependency.
- Root `CoreGoldenEquivalenceTest` holds side-by-side Processing/Core fixtures.
- Concurrency fixtures use latches or a manual executor; no fixed sleeps are used.
- The Core source and JAR boundary checks remain part of `:core:check`.
- The dedicated workflow publishes only to `core/build/maven-test-repository` and validates an
  independent Maven-coordinate consumer.

The qualified label is **Core 0.1 Qualified**, not Core 1.0 Stable.
