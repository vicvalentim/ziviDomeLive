# ziviDomeLive 2.0.0 Release Evidence

Release target: `v2.0.0`  
Status: **VERIFIED — PRE-TAG GATES COMPLETE**

This file is the completed pre-tag evidence ledger for ziviDomeLive 2.0.0. All applicable release gates below have concrete results and evidence references.

## Repository state

| Item | Status | Evidence |
|---|---|---|
| branch is `release/2.0.0` | PASS | `git branch --show-current` → `release/2.0.0` |
| qualified source HEAD recorded | PASS | automated qualification executed at `16796f2005cf74f7148e677c9345156d5d03e4eb` on 2026-08-23; the subsequent evidence-only ledger commit must be requalified before tagging |
| working tree clean | PASS | `git status --porcelain` returned empty |

## AUTOMATED

| Check | Status | Evidence |
|---|---|---|
| `./gradlew clean test build --console=plain` | PASS | BUILD SUCCESSFUL at HEAD `16796f2005cf74f7148e677c9345156d5d03e4eb` |
| `./gradlew qualificationTests --console=plain` | PASS | 354 total; 354 passed; 0 failed; 0 skipped at HEAD `16796f2005cf74f7148e677c9345156d5d03e4eb` |
| `python3 tools/validate_documentation.py --root .` | PASS | 0 errors; 0 warnings |
| `python3 -m mkdocs build --strict` | PASS | EN/PT documentation built successfully |
| `./gradlew buildReleaseArtifacts --console=plain` | PASS | BUILD SUCCESSFUL; Processing package verified |
| package/sibling validator | PASS | 0 errors; 0 warnings; ZIP/PDEX byte-identical |

## Physical qualification matrix

Maintainer attestation recorded on 2026-08-23 for the ziviDomeLive 2.0 release candidate.

| Physical platform | Status | Scope |
|---|---|---|
| macOS Apple Silicon `arm64` | PASS | Core rendering, examples/tools and applicable native outputs |
| macOS Intel `x86_64` | PASS | Core rendering, examples/tools and applicable native outputs |
| Windows `x86_64` | PASS | Core rendering, examples/tools and applicable native outputs |
| Linux `x86_64` | PASS | Core rendering, examples/tools and applicable native outputs |

The maintainer physically exercised the applicable Standard and spherical
rendering paths, calibration workflow, environment rendering, graphics resize,
BenchmarkTool, Processing-package installation, all eight distributed
examples/tools and native-output paths.

Native-output applicability remains platform-specific:

- NDI: physically qualified on macOS, Windows and Linux;
- Syphon: physically qualified on macOS;
- Spout: physically qualified on Windows.

A backend is not implicitly claimed on an operating system where that backend
does not apply.

## GPU VISUAL

| Check | Status | Environment / evidence |
|---|---|---|
| Standard | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| Domemaster | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| Equirectangular | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| Skybox | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| CalibrationTool | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| Environment LDR equirectangular background | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| representative `resetGraphics(int)` resize | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |

## BENCHMARK

| Check | Status | Evidence |
|---|---|---|
| BenchmarkTool smoke | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| CPU baseline | PASS | Maintainer physical BenchmarkTool qualification on macOS arm64/x86_64, Windows x86_64 and Linux x86_64 |
| CPU/GPU mode supported by current tool | PASS | Maintainer physical BenchmarkTool qualification on macOS arm64/x86_64, Windows x86_64 and Linux x86_64 |
| environment metadata recorded | PASS | Platform/environment inspected during physical qualification on macOS arm64/x86_64, Windows x86_64 and Linux x86_64 |

## NATIVE OUTPUT

Record only platforms actually tested for this release.

| Backend | Platform/configuration | Status | Receiver evidence |
|---|---|---|---|
| NDI | macOS arm64/x86_64; Windows x86_64; Linux x86_64 | PASS | Maintainer physically verified live sender/receiver operation |
| Syphon | macOS arm64/x86_64 | PASS | Maintainer physically verified GPU texture publication/receiver operation |
| Spout | Windows x86_64 | PASS | Maintainer physically verified GPU texture publication/receiver operation |

If a backend/platform is not claimed for 2.0.0, replace its row status with `NOT CLAIMED`, not `PASS`.

## PACKAGE INSTALLATION

| Check | Status | Evidence |
|---|---|---|
| generated ZIP/PDEX installed in clean sketchbook | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| `reference/index.html` opens | PASS | full local documentation preview returned HTTP 200 for EN `/reference/index.html` and PT `/pt/reference/index.html` |
| `src/` present; `src/test/` absent | PASS | generated package inspection: `src/` present; `src/test/` absent |
| EmptyProject | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| Basic | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| SphereParticle | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| InfiniteBackground | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| FulldomePBR | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| SolarSystem | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| CalibrationTool | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| BenchmarkTool | PASS | Maintainer physical qualification attestation, 2026-08-23: macOS Apple Silicon arm64, macOS Intel x86_64, Windows x86_64 and Linux x86_64 |
| no local reports/helper artifacts/`.DS_Store` | PASS | generated package inspection found no `.DS_Store`, `__MACOSX`, build reports, test-results or benchmark-results |

## PUBLICATION METADATA

| Check | Status | Evidence |
|---|---|---|
| `library.properties` validated | PASS | documentation/package validator: 0 errors; 0 warnings |
| ZIP/TXT/PDEX stable siblings present | PASS | `buildReleaseArtifacts` + package/sibling validator |
| software DOI externally verified | PASS | DOI `10.5281/zenodo.15671506` resolves to Zenodo; DataCite HTTP 200, state `findable`, publication year 2026, title `ziviDomeLive: Processing library for immersive fulldome visuals`, creator `Valentim, Victor` |
| CFF / Zenodo / README / MkDocs consistent | PASS | documentation validator: 0 errors; 0 warnings |
| Processing minimum revision confirmed | PASS | `minRevision=1285` is the declared Processing 4 baseline; enforced by `library.properties`, `release.properties`, `ReleaseMetadataTest`, documentation validator and generated release package |
| tested-platform claims match evidence | PASS | `tested.platform` and `tested.processingVersion` remain intentionally blank; physical platform qualification is recorded in this ledger and no package-level claim is fabricated without a single canonical Processing-version record |
| GitBook DOI absent/null | PASS | `maintainer/gitbook-publication-plan.md` records `documentation_doi: null` intentionally until real registration |
| GitBook ISBN absent/null | PASS | `maintainer/gitbook-publication-plan.md` records `documentation_isbn: null` intentionally until real registration |

## DOCUMENTATION FREEZE

| Check | Status | Evidence |
|---|---|---|
| raster diagram placeholders removed; Mermaid diagrams and hero asset reviewed | PASS | maintainer visual review of generated EN/PT site; heroes and Mermaid diagrams rendered correctly with no provisional/broken visual assets; `mkdocs build --strict` PASS |
| EN/PT facts synchronized | PASS | documentation validator: 0 errors/0 warnings; critical EN/PT fact-parity review passed for About, License, 2.0 release notes and OpenGL backend |
| Javadocs match public signatures | PASS | `PublicApiCompatibilityTest` protects the frozen 2.0 public baseline; 354/354 qualification PASS; Javadocs generated successfully |
| no roadmap feature presented as current | PASS | documentation validator 0 errors/0 warnings plus release-scope review; future features remain confined to roadmap/history |
| no generic VR/headset runtime claim | PASS | documentation validator 0 errors/0 warnings; current references are explicit exclusions or historical context, not product claims |
| no documentation-only renderer/API change | PASS | `git diff --name-status 16796f2^ 16796f2 -- src/main/java` returned empty; licensing/provenance migration made no renderer/API source change |
| CHANGELOG synchronized | PASS | final licensing/provenance diff reviewed; 2.0 changelog records Apache-2.0 migration and corrected third-party provenance |

## Tag authorization

Final maintainer decision: **PASS — PRE-TAG EVIDENCE COMPLETE**

Tag `v2.0.0` only after the committed ledger state itself passes the final automated gate successfully without changing HEAD.

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
- `python3 -m mkdocs build --strict`: PASS
- `buildReleaseArtifacts`: PASS
- `verifyProcessingPackage`: PASS
- ZIP/PDEX byte identity: PASS
- packaged Devolay SHA-256: `5220b15fbba3eb655595e6fd02898748dd57da67989ae87b2cd8a727f86d6d24`
