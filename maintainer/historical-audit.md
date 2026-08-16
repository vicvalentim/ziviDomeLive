# Historical Documentation Audit — 2.0.0

Baseline branch: `release/2.0.0`  
Baseline audited HEAD: `bc560dbb8dab1d39b55824ae71ae70179512ec6d`

This audit records knowledge that must survive the 2.0 documentation freeze. It is a preservation ledger, not release marketing.

## Classification ledger

| Concept / historical statement | Classification | 2.0 treatment |
|---|---|---|
| Standard and spherical rendering are separate domains | PRESERVE | Keep in user concepts and Developer Guide |
| `RenderMode.FULL` preserves independent preview/output `ViewType` routes | PRESERVE | Keep prominently in User Guide and API docs |
| Dedicated render modes do not erase stored routes | PRESERVE | Keep in RenderMode documentation |
| `update()` advances mutable scene state once per Processing frame | PRESERVE + EXPAND | Make explicit in Quickstart, Scene docs and Javadoc |
| `sceneRender()` is drawing only | PRESERVE + EXPAND | Explain possible multiple calls during spherical capture |
| Library owns `beginDraw()` / `endDraw()` | PRESERVE | Keep in Quickstart, Scene docs and Javadoc |
| Pitch/Yaw/Roll are shared spherical-orientation controls | PRESERVE | Keep under Spherical Calibration |
| Domemaster FOV | PRESERVE | Keep under Spherical Calibration |
| Domemaster Size% is physical projection/lens fit, not scene zoom | PRESERVE + CLARIFY | Keep under Spherical Calibration |
| Standard preview follows the Processing window | PRESERVE | Keep in Preview and Output |
| Spherical preview sizing is automatic/dynamic | PRESERVE | Keep in Preview and Output |
| Output resolution is independent from preview resolution | PRESERVE | Keep in Preview and Output |
| Output resolution change is deferred to the draw/render boundary | PRESERVE/UPDATE | Document only with current method name and implementation semantics |
| Size% survives render-target recreation | PRESERVE | Mention in calibration/resize behavior |
| Six independent Processing cubemap face targets | HISTORICAL | Migration/release history only |
| Legacy six-texture spherical shaders | HISTORICAL/REMOVE FROM CURRENT GUIDE | Developer history only |
| Equirectangular intermediate feeding Domemaster | HISTORICAL | Release notes/migration only |
| Native cubemap / `samplerCube` pipeline | PRESERVE, MOVE | Developer Guide and optional “under the hood”, not entry-point copy |
| NDI worker/buffer/latest-frame-wins internals | MOVE | Developer Guide → Output Backends |
| Syphon/Spout GPU-native path | PRESERVE, MOVE | User page: usage only; Developer Guide: implementation |
| Qualification/BenchmarkTool as beginner tutorial | MOVE | Qualification/Maintainer surface |
| CalibrationTool as beginner tutorial | MOVE | Qualification Tools |
| `SceneServices` required for ordinary scenes | INVALID | Explicitly state that simple scenes do not need it |
| Generic “VR framework / mono VR engine” identity | INVALID / NOT CONTRACTED | Remove from current public identity unless code/API proves a runtime contract |
| Spherical Mirror as 2.0 capability | REMOVE FROM CURRENT / ROADMAP ONLY | Do not expose enum/API/tutorial |
| HDR/IBL/AO/PBO as current capability | REMOVE FROM CURRENT / ROADMAP ONLY | Keep future-only |

## Historical knowledge recovered from 1.x

The 1.x documentation already established several user-facing invariants that remain valuable in 2.0:

1. users interact with final visual representations rather than cubemap implementation topology;
2. `FULL` is the compatibility/default working mode for independently routed preview and outputs;
3. dedicated modes temporarily change effective routing rather than destroying stored configuration;
4. camera/navigation and spherical orientation are separate conceptual layers;
5. preview sizing and output sizing are independent concerns;
6. Size% is a projection calibration control and survives output-target recreation;
7. output backends must expose state/failure information rather than being described as unconditionally available;
8. automated tests do not replace GPU/native receiver qualification.

## Historical material intentionally retained only as history

The following remains useful for release notes, migration notes or architectural history but should not appear as the current recommended renderer model:

- six independent Processing face targets;
- six `sampler2D` spherical shader inputs;
- equirectangular-to-domemaster projection chaining;
- internal fallback renderer scaffolding removed during 2.0 work.

## Current 2.0 implementation boundary

The 2.0 release documentation should describe these as current only where implementation/tests confirm them:

- Standard rendering independent from spherical capture;
- native cubemap as spherical source;
- sibling Domemaster/Equirectangular/Skybox projections from the same spherical source;
- optional Scene Services;
- visual LDR equirectangular Environment background;
- NDI as GPU-to-CPU/network boundary;
- Syphon/Spout as platform-local sharing outputs;
- performance instrumentation as advanced/qualification material.

## Required follow-up during freeze

Before tag creation, update this ledger if any implementation fact changes. Any documentation deletion of historically relevant material must point to a classification in this file.
