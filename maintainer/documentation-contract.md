# ziviDomeLive Documentation Contract

Status: documentation/release freeze for `release/2.0.0`.

Baseline audited HEAD: `bc560dbb8dab1d39b55824ae71ae70179512ec6d`.

## 1. Documentary objects

The ziviDomeLive documentation ecosystem contains three distinct objects:

1. **Processing library / software product** — installable code, examples, generated reference, metadata, legal notices and release artifacts.
2. **MkDocs** — official, versioned, operational technical documentation for the currently released software version.
3. **Future GitBook** — independent didactic/editorial publication that may contextualize the software, creative practice and immersive-media concepts in greater depth.

These surfaces must not substitute for one another.

## 2. Authority hierarchy

Documentation must follow this authority order:

```text
IMPLEMENTATION + TESTS
        ↓
PUBLIC API CONTRACT
        ↓
JAVADOC
        ↓
MKDOCS
        ↓
FUTURE GITBOOK
```

Consequences:

- implementation and tests determine what exists;
- public API/Javadoc determine what can be called and under which lifecycle constraints;
- MkDocs teaches installation, use, configuration, diagnosis and architecture of the current version;
- GitBook may interpret and contextualize but must never redefine the API.

## 3. Audience contract

### User Guide

Audience: artists, students, creative coders, teachers, installation technicians and general users.

May teach: `Scene`, `update()`, `sceneRender()`, `RenderMode`, `ViewType`, Standard, Domemaster, Equirectangular, Skybox, Preview, Output, spherical calibration, camera, Environment, scene management, interaction and optional outputs.

Must not require engine internals such as `FrameViews`, `RenderRequirementsPolicy`, `CubemapTarget`, PGL/JOGL/FBO identifiers, timer queries, Gradle internals, CI or packaging.

### API Reference

Audience: programmers calling the public library API.

Every public type must be classified as one of:

- ARTIST-FACING STABLE
- ADVANCED PUBLIC
- EXPERIMENTAL PUBLIC
- DEPRECATED
- ENGINE-FACING PUBLIC

Java `public` visibility alone does not define the intended audience.

### Developer Guide

Audience: contributors, maintainers, software researchers and developers.

May document Standard/Spherical domains, render pipeline, cubemap target ownership, OpenGL state, output backends, threading, performance internals, resource lifecycle and testing.

### Maintainer Documentation

Audience: project maintainers.

Contains publication qualification, Processing release procedure, benchmark/calibration protocols, citation metadata, GitBook publication planning, release evidence and historical audits.

## 4. Non-contamination rules

- **USER → INTERNAL:** engine internals are never a prerequisite for ordinary creative use.
- **INTERNAL → USER:** developer documentation may explain implementation but may not redefine public behavior.
- **HISTORY → CURRENT:** 1.x names/behavior appear in 2.0 tutorials only when required for migration.
- **ROADMAP → CURRENT:** future work is never described as current capability.
- **CAPABILITY → TESTED:** a technically supported path is not automatically a qualified/tested platform.
- **EXAMPLE → API:** examples must use recommended public contracts.
- **JAVADOC → CODE:** every documented callable signature must exist.
- **PT ↔ EN:** both languages share the same facts and information architecture.

## 5. Scene contract

The creative documentation must preserve this distinction prominently:

```text
update()      = mutable state / simulation / animation
sceneRender() = drawing only
```

`sceneRender(PGraphicsOpenGL)` may be called more than once during one Processing frame when spherical capture renders multiple views. Any state that should advance once per frame belongs in `update()`.

The library owns `beginDraw()` / `endDraw()` for the target passed to `sceneRender()`.

## 6. RenderMode and ViewType

`RenderMode` answers: **How do I want to work now?**

Current 2.0 values:

- `FULL`
- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

`ViewType` answers: **Which final representation should this destination receive?**

Current values:

- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

`FULL` preserves independent preview/output routes. Dedicated modes temporarily override the effective representation without erasing stored routes.

## 7. Current/future boundary

The following are explicitly **not** current 2.0 contracts unless implementation changes before release:

- Spherical Mirror;
- HDR environment loading/render targets;
- IBL, irradiance, BRDF LUT, AO/PBR engine features;
- PBO/fence NDI transfer;
- generic VR/headset runtime claims.

Roadmap material must remain labelled as future work.

## 8. Publication identifiers

- ziviDomeLive software: software DOI only, once externally verified and kept consistent across release metadata.
- MkDocs: official technical documentation of the software version; no independent DOI/ISBN is asserted here.
- Future GitBook: may later receive its own DOI/ISBN; no identifier exists unless explicitly registered.
- Future JOSS paper: independent peer-reviewed publication and independent editorial DOI.

No placeholder DOI or ISBN may appear in public release material.

## 9. Historical preservation rule

Every historically relevant statement must be classified before removal:

`PRESERVE`, `UPDATE`, `MOVE`, `MERGE`, `HISTORICAL`, `REMOVE`, `INVALID`.

Only `REMOVE` and `INVALID` authorize definitive disappearance.

## 10. Freeze rule

This documentation freeze must not introduce renderer refactors, API aliases or feature development for editorial convenience. Documentation must adapt to the implemented software, not the reverse.
