# ziviDomeLive 2.0.0 Release Evidence

Release target: `v2.0.0`  
Status of this template: **UNVERIFIED**  

This file is the pre-tag evidence ledger. Replace every `UNVERIFIED` with a concrete result and evidence reference. Do not create the tag while any applicable item is unverified.

## Repository state

| Item | Status | Evidence |
|---|---|---|
| branch is `release/2.0.0` | PASS | `git branch --show-current` → `release/2.0.0` |
| qualified HEAD recorded | PASS | automated qualification executed at `6089194071084ebcc240116b7501d179ce4c66d6` |
| working tree clean | PASS | `git status --porcelain` returned empty |

## AUTOMATED

| Check | Status | Evidence |
|---|---|---|
| `./gradlew clean test build --console=plain` | PASS | BUILD SUCCESSFUL at HEAD `6089194071084ebcc240116b7501d179ce4c66d6` |
| `./gradlew qualificationTests --console=plain` | PASS | 304 total; 304 passed; 0 failed; 0 skipped |
| `python3 tools/validate_documentation.py --root .` | PASS | 0 errors; 0 warnings |
| `python3 -m mkdocs build --strict` | PASS | EN/PT documentation built successfully |
| `./gradlew buildReleaseArtifacts --console=plain` | PASS | BUILD SUCCESSFUL; Processing package verified |
| package/sibling validator | PASS | 0 errors; 0 warnings; ZIP/PDEX byte-identical |

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
| `reference/index.html` opens | PASS | full local documentation preview returned HTTP 200 for EN `/reference/index.html` and PT `/pt/reference/index.html` |
| `src/` present; `src/test/` absent | PASS | generated package inspection: `src/` present; `src/test/` absent |
| EmptyProject | UNVERIFIED | |
| Basic | UNVERIFIED | |
| SphereParticle | UNVERIFIED | |
| InfiniteBackground | UNVERIFIED | |
| FulldomePBR | UNVERIFIED | |
| SolarSystem | UNVERIFIED | |
| CalibrationTool | UNVERIFIED | |
| BenchmarkTool | UNVERIFIED | |
| no local reports/helper artifacts/`.DS_Store` | PASS | generated package inspection found no `.DS_Store`, `__MACOSX`, build reports, test-results or benchmark-results |

## PUBLICATION METADATA

| Check | Status | Evidence |
|---|---|---|
| `library.properties` validated | PASS | documentation/package validator: 0 errors; 0 warnings |
| ZIP/TXT/PDEX stable siblings present | PASS | `buildReleaseArtifacts` + package/sibling validator |
| software DOI externally verified | UNVERIFIED | registered record |
| CFF / Zenodo / README / MkDocs consistent | PASS | documentation validator: 0 errors; 0 warnings |
| Processing minimum revision confirmed | PASS | `minRevision=1285` is the declared Processing 4 baseline; enforced by `library.properties`, `release.properties`, `ReleaseMetadataTest`, documentation validator and generated release package |
| tested-platform claims match evidence | PASS | no `tested.*` claim is published while manual platform qualification remains incomplete |
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

## Devolay 2.2.0-vic.2 physical qualification

The Maven Central artifact `io.github.vicvalentim:devolay:2.2.0-vic.2` was physically qualified with ziviDomeLive on both supported macOS CPU architectures.

Published artifact SHA-256:

`5220b15fbba3eb655595e6fd02898748dd57da67989ae87b2cd8a727f86d6d24`

Qualification results:

- macOS Intel `x86_64`: PASS
- macOS Apple Silicon `arm64`: PASS
- Processing integration: PASS
- ziviDomeLive initialization: PASS
- Devolay/NDI initialization: PASS
- live NDI transmission: PASS
- physical network transmission between Intel and Apple Silicon systems: PASS

The exact Maven Central JAR SHA-256 above was verified on both physical machines.

The previously qualified release candidate and the published Maven Central artifact are not byte-for-byte identical because the native binaries were rebuilt during release publication. For macOS, forensic comparison showed that the resulting differences are confined to linker-generated Mach-O metadata: `LC_UUID`, and on arm64 the corresponding ad-hoc linker signature data. No difference was observed in Java entries, native binary size, architecture, linked library set, or runtime behavior.

### Maven Central resolution and Processing package chain

The ziviDomeLive `release/2.0.0` build resolves:

`io.github.vicvalentim:devolay:2.2.0-vic.2`

through the Gradle runtime classpath.

The resolved dependency was propagated unchanged through the Processing release pipeline:

- Gradle dependency resolution: PASS
- clean project build: PASS
- release package generation: PASS
- Processing package verification: PASS
- ZIP/PDEX byte-identity verification: PASS
- packaged Devolay SHA-256: `5220b15fbba3eb655595e6fd02898748dd57da67989ae87b2cd8a727f86d6d24`
- `deployToProcessingSketchbook`: PASS
- previous Processing installation removed and rebuilt by Gradle: PASS
- deployed Devolay SHA-256: `5220b15fbba3eb655595e6fd02898748dd57da67989ae87b2cd8a727f86d6d24`
- obsolete `devolay-2.2.0-vic.1.jar` in deployed Processing library: ABSENT

Therefore the artifact resolved from Maven Central, packaged by ziviDomeLive, deployed to Processing, and physically qualified with NDI on macOS Intel and Apple Silicon is the same published `2.2.0-vic.2` JAR.

### Final local qualification after Devolay 2.2.0-vic.2 integration

Qualification run on 2026-08-17:

- `qualificationTests`: PASS — 304 total, 304 passed, 0 failed, 0 skipped
- documentation validator: PASS — 0 errors, 0 warnings
- `mkdocs build --strict`: PASS
- `buildReleaseArtifacts`: PASS
- `verifyProcessingPackage`: PASS
- ZIP/PDEX byte identity: PASS
- packaged Devolay SHA-256: `5220b15fbba3eb655595e6fd02898748dd57da67989ae87b2cd8a727f86d6d24`
