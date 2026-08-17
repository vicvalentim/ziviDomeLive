# ziviDomeLive 2.0.0 Release Evidence

Release target: `v2.0.0`  
Status of this template: **UNVERIFIED**  

This file is the pre-tag evidence ledger. Replace every `UNVERIFIED` with a concrete result and evidence reference. Do not create the tag while any applicable item is unverified.

## Repository state

| Item | Status | Evidence |
|---|---|---|
| branch is `release/2.0.0` | UNVERIFIED | `git branch --show-current` |
| final HEAD recorded | UNVERIFIED | `git rev-parse HEAD` |
| working tree clean | UNVERIFIED | `git status --porcelain` |

## AUTOMATED

| Check | Status | Evidence |
|---|---|---|
| `./gradlew clean test build --console=plain` | UNVERIFIED | attach/log run |
| `./gradlew qualificationTests --console=plain` | UNVERIFIED | attach/log run |
| `python3 tools/validate_documentation.py --root .` | UNVERIFIED | attach/log run |
| `python3 -m mkdocs build --strict` | UNVERIFIED | attach/log run |
| `./gradlew buildReleaseArtifacts --console=plain` | UNVERIFIED | attach/log run |
| package/sibling validator | UNVERIFIED | attach/log run |

## GPU VISUAL

| Check | Status | Environment / evidence |
|---|---|---|
| Standard | UNVERIFIED | |
| Domemaster | UNVERIFIED | |
| Equirectangular | UNVERIFIED | |
| Skybox | UNVERIFIED | |
| CalibrationTool | UNVERIFIED | |
| Environment LDR equirectangular background | UNVERIFIED | |
| representative `resetGraphics(int)` resize | UNVERIFIED | |

## BENCHMARK

| Check | Status | Evidence |
|---|---|---|
| BenchmarkTool smoke | UNVERIFIED | |
| CPU baseline | UNVERIFIED | report path/hash |
| CPU/GPU mode supported by current tool | UNVERIFIED | report path/hash |
| environment metadata recorded | UNVERIFIED | OS/GPU/Processing/Java/resolution/routes |

## NATIVE OUTPUT

Record only platforms actually tested for this release.

| Backend | Platform/configuration | Status | Receiver evidence |
|---|---|---|---|
| NDI | | UNVERIFIED | |
| Syphon | macOS configuration if claimed | UNVERIFIED | |
| Spout | Windows configuration if claimed | UNVERIFIED | |

If a backend/platform is not claimed for 2.0.0, replace its row status with `NOT CLAIMED`, not `PASS`.

## PACKAGE INSTALLATION

| Check | Status | Evidence |
|---|---|---|
| generated ZIP/PDEX installed in clean sketchbook | UNVERIFIED | |
| `reference/index.html` opens | UNVERIFIED | |
| `src/` present; `src/test/` absent | UNVERIFIED | |
| EmptyProject | UNVERIFIED | |
| Basic | UNVERIFIED | |
| SphereParticle | UNVERIFIED | |
| InfiniteBackground | UNVERIFIED | |
| FulldomePBR | UNVERIFIED | |
| SolarSystem | UNVERIFIED | |
| CalibrationTool | UNVERIFIED | |
| BenchmarkTool | UNVERIFIED | |
| no local reports/helper artifacts/`.DS_Store` | UNVERIFIED | |

## PUBLICATION METADATA

| Check | Status | Evidence |
|---|---|---|
| `library.properties` validated | UNVERIFIED | |
| ZIP/TXT/PDEX stable siblings present | UNVERIFIED | |
| software DOI externally verified | UNVERIFIED | registered record |
| CFF / Zenodo / README / MkDocs consistent | UNVERIFIED | validator |
| Processing minimum revision confirmed | UNVERIFIED | official Processing source/revision record |
| tested-platform claims match evidence | UNVERIFIED | current `library.properties` intentionally makes no `tested.*` claim |
| GitBook DOI absent/null | UNVERIFIED | metadata audit |
| GitBook ISBN absent/null | UNVERIFIED | metadata audit |

## DOCUMENTATION FREEZE

| Check | Status | Evidence |
|---|---|---|
| provisional images replaced and `docs/img/PLACEHOLDERS.txt` removed | UNVERIFIED | visual review |
| EN/PT facts synchronized | UNVERIFIED | validator + review |
| Javadocs match public signatures | UNVERIFIED | Javadoc build/review |
| no roadmap feature presented as current | UNVERIFIED | review |
| no generic VR/headset runtime claim | UNVERIFIED | validator/review |
| no documentation-only renderer/API change | UNVERIFIED | `git diff` review |
| CHANGELOG synchronized | UNVERIFIED | final diff review |

## Tag authorization

Final maintainer decision: **UNVERIFIED**

Tag `v2.0.0` only after this ledger contains no `UNVERIFIED`, `PENDING` or unchecked applicable gate.
