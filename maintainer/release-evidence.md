# ziviDomeLive 2.0.0 Release Evidence

Release target: `v2.0.0`  
Status: **VERIFIED — PRE-TAG GATES COMPLETE**

Qualified source revision: `88f3849b9188ff4e20a7d6be1649063d5501fb4e`

This ledger binds the final pre-tag publication-contract source revision above to the
physical and automated qualification record for ziviDomeLive 2.0.0. Runtime qualification
is carried forward from the physically qualified 2.0 candidate
`5069e00b567bd7ee5f9725f1f2f7a7fdae983348` because the final documentation/metadata publication work introduces
no runtime-sensitive source, shader, native/data or executable-example changes. The final
package comparison also confirmed that all 128 entries of `ziviDomeLive.jar` have identical
content to the previously qualified package and that the bundled
`devolay-2.2.0-vic.2.jar` is byte-identical.

The commit immediately following the qualified source may update only this evidence ledger.
The pre-tag validator permits exactly that one evidence-only commit and rejects any other
source, build, example, workflow or documentation change.

## Repository state

| Item | Status | Evidence |
|---|---|---|
| branch is `release/2.0.0` | PASS | `git branch --show-current` → `release/2.0.0` |
| final qualified source recorded | PASS | `88f3849b9188ff4e20a7d6be1649063d5501fb4e` |
| runtime carry-forward base recorded | PASS | `5069e00b567bd7ee5f9725f1f2f7a7fdae983348` |
| working tree clean before evidence commit | PASS | `git status --porcelain --untracked-files=all` returned empty |
| runtime-sensitive diff from qualified runtime candidate | PASS | none in `src/main/java`, shaders, `native/`, `data/` or executable example code |

## AUTOMATED

| Check | Status | Evidence |
|---|---|---|
| `./gradlew clean test build --console=plain` | PASS | BUILD SUCCESSFUL at `88f3849b9188ff4e20a7d6be1649063d5501fb4e` |
| `./gradlew qualificationTests --console=plain` | PASS | **387 total; 387 passed; 0 failed; 0 skipped** at `88f3849b9188ff4e20a7d6be1649063d5501fb4e` |
| `python3 tools/validate_documentation.py --root .` | PASS | 0 errors; 0 warnings |
| `python3 -m mkdocs build --strict` | PASS | EN/PT documentation built successfully |
| `./gradlew attachJavadocsToSite --console=plain` | PASS | BUILD SUCCESSFUL |
| exported-site validator | PASS | 15,410 local references resolved; 0 errors; 0 warnings |
| `./gradlew buildReleaseArtifacts --console=plain` | PASS | BUILD SUCCESSFUL; Processing package verified |
| package/sibling validator | PASS | 0 errors; 0 warnings; ZIP/PDEX byte-identical |
| CodeMeta publication contract | PASS | `codemeta.json` required by repository validator, package verifier and release package |

## Physical qualification matrix

Maintainer attestation covers the ziviDomeLive 2.0 release candidate and the four target
desktop configurations. The final publication-contract source carries this qualification
forward because its runtime-bearing payload is unchanged from the qualified candidate.

| Physical platform | Status | Scope |
|---|---|---|
| macOS Apple Silicon `arm64` | PASS | Core rendering, ten distributed examples/tools and applicable native outputs |
| macOS Intel `x86_64` | PASS | Core rendering, ten distributed examples/tools and applicable native outputs |
| Windows `x86_64` | PASS | Core rendering, ten distributed examples/tools and applicable native outputs |
| Linux `x86_64` | PASS | Core rendering, ten distributed examples/tools and applicable native outputs |

The physical matrix covers Standard, Domemaster, Equirectangular and Skybox rendering,
spherical calibration, Environment rendering, representative resize behavior, the complete
set of ten distributed examples/tools, Processing-package use and the platform-applicable
external-output paths.

Native-output applicability remains platform-specific:

- NDI: physically qualified on macOS, Windows and Linux;
- Syphon: physically qualified on macOS;
- Spout: physically qualified on Windows.

A backend is not implicitly claimed on an operating system where that backend does not apply.

## GPU VISUAL

| Check | Status | Environment / evidence |
|---|---|---|
| Standard | PASS | Maintainer physical qualification on macOS arm64/x86_64, Windows x86_64 and Linux x86_64 |
| Domemaster | PASS | Maintainer physical qualification on macOS arm64/x86_64, Windows x86_64 and Linux x86_64 |
| Equirectangular | PASS | Maintainer physical qualification on macOS arm64/x86_64, Windows x86_64 and Linux x86_64 |
| Skybox | PASS | Maintainer physical qualification on macOS arm64/x86_64, Windows x86_64 and Linux x86_64 |
| CalibrationTool | PASS | Maintainer physical qualification on the four target desktop configurations |
| Environment LDR equirectangular background | PASS | Maintainer physical qualification on the four target desktop configurations |
| representative `resetGraphics(int)` resize | PASS | Maintainer physical qualification on the four target desktop configurations |

## BENCHMARK

| Check | Status | Evidence |
|---|---|---|
| BenchmarkTool smoke | PASS | Maintainer physical qualification on macOS arm64/x86_64, Windows x86_64 and Linux x86_64 |
| CPU baseline | PASS | Maintainer physical BenchmarkTool qualification on the four target desktop configurations |
| CPU/GPU mode supported by current tool | PASS | Maintainer physical BenchmarkTool qualification on the four target desktop configurations |
| environment metadata recorded | PASS | Platform/environment inspected during physical qualification |

## NATIVE OUTPUT

| Backend | Platform/configuration | Status | Receiver evidence |
|---|---|---|---|
| NDI | macOS arm64/x86_64; Windows x86_64; Linux x86_64 | PASS | Maintainer physically verified live sender/receiver operation |
| Syphon | macOS arm64/x86_64 | PASS | Maintainer physically verified GPU texture publication/receiver operation |
| Spout | Windows x86_64 | PASS | Maintainer physically verified GPU texture publication/receiver operation |

## PACKAGE INSTALLATION AND CONTENT

The physically exercised package predates the final documentation/CodeMeta-only package
revision. Qualification is carried forward because the final package preserves identical
runtime payload content and changes only documentation/metadata publication surfaces.

| Check | Status | Evidence |
|---|---|---|
| Processing package installation on qualified candidate | PASS | Maintainer physical qualification on target platforms |
| final package structure | PASS | `verifyProcessingPackage` + package validator |
| final `ziviDomeLive.jar` runtime payload | PASS | 128 JAR entries content-identical to the previously qualified package |
| final bundled Devolay | PASS | `devolay-2.2.0-vic.2.jar` byte-identical to the previously qualified package |
| `reference/index.html` generated | PASS | Javadocs attached and exported-site validator passed |
| `src/` present; `src/test/` absent | PASS | generated package inspection |
| EmptyProject | PASS | physical qualification carried forward |
| Basic | PASS | physical qualification carried forward |
| NamedActions | PASS | physical qualification carried forward |
| PortLoopback | PASS | physical qualification carried forward |
| SphereParticle | PASS | physical qualification carried forward |
| InfiniteBackground | PASS | physical qualification carried forward |
| FulldomePBR | PASS | physical qualification carried forward |
| SolarSystem | PASS | physical qualification carried forward |
| CalibrationTool | PASS | physical qualification carried forward |
| BenchmarkTool | PASS | physical qualification carried forward |
| development-only artifacts absent | PASS | package verifier + package validator |

## PUBLICATION METADATA

| Check | Status | Evidence |
|---|---|---|
| `library.properties` validated | PASS | documentation/package validator: 0 errors; 0 warnings |
| `CITATION.cff` validated | PASS | version, DOI and Apache-2.0 contract |
| `codemeta.json` validated | PASS | Processing Contributed Library identity, version, DOI, license and scope checks |
| `.zenodo.json` validated | PASS | version and Apache-2.0 contract |
| README / MkDocs / metadata consistency | PASS | documentation validator: 0 errors; 0 warnings |
| Processing minimum revision confirmed | PASS | `minRevision=1285` |
| tested-platform scalar fields | PASS | remain intentionally blank; physical matrix is recorded in this ledger |
| generic VR/XR metadata | PASS | absent from current product metadata contract |
| GitBook DOI | PASS | absent/null until a real registration exists |
| GitBook ISBN | PASS | absent/null until a real registration exists |

### Pre-tag local artifact hashes

These hashes identify the release artifacts generated locally from qualified source
`88f3849b9188ff4e20a7d6be1649063d5501fb4e` after the CodeMeta publication contract was integrated. A later
release-workflow rebuild may have different archive bytes if ZIP timestamps differ; the
release workflow must still pass the same package/content contracts.

- `ziviDomeLive.zip`: `55fc3ade4bdcefbcc04408d82a52fd1b8ebe788c4915e14668fd8b8abd3241ba`
- `ziviDomeLive.pdex`: `55fc3ade4bdcefbcc04408d82a52fd1b8ebe788c4915e14668fd8b8abd3241ba`
- `ziviDomeLive.txt`: `f112898b3ef90ea7b5d38f56849e53c6432cf7a0ccc6247f737dfdd2167d8d74`
- ZIP/PDEX byte identity: PASS
- `codemeta.json` present in ZIP/PDEX: PASS

## DOCUMENTATION FREEZE

| Check | Status | Evidence |
|---|---|---|
| EN/PT documentation build | PASS | `python3 -m mkdocs build --strict` |
| exported documentation routes | PASS | 15,410 local references resolved; 0 errors; 0 warnings |
| Javadocs match current public signatures | PASS | generated successfully; qualification suite 387/387 PASS |
| no roadmap feature presented as current | PASS | documentation validator 0 errors/0 warnings |
| no generic VR/headset-runtime product claim | PASS | current metadata and documentation contract |
| Processing Contributed Library identity | PASS | README + CodeMeta + Processing package metadata |
| no runtime change during final documentation/metadata integration | PASS | runtime-sensitive tree diff from `5069e00b567bd7ee5f9725f1f2f7a7fdae983348` is empty |
| CHANGELOG/release documentation synchronized | PASS | current documentation validator passed |

## Runtime qualification carry-forward

Physical qualification was not repeated merely for documentation/metadata packaging changes.
The carry-forward is bounded by the following evidence:

1. qualified runtime candidate: `5069e00b567bd7ee5f9725f1f2f7a7fdae983348`;
2. final publication-contract source: `88f3849b9188ff4e20a7d6be1649063d5501fb4e`;
3. no changes between those revisions in `src/main/java`, shaders, native/data payloads or
   executable `.pde`/`.java` example code;
4. `ziviDomeLive.jar`: 128 entries with content identical to the previously qualified package;
5. `devolay-2.2.0-vic.2.jar`: byte-identical to the previously qualified package;
6. all 387 automated qualification tests pass at the final publication-contract source;
7. documentation, exported-site and Processing-package validators pass with 0 errors and
   0 warnings.

This carry-forward applies only to the bounded documentation/metadata/build-test delta above.
Any later runtime-sensitive change invalidates it and requires renewed qualification.

## Tag authorization

Final maintainer decision: **PASS — PRE-TAG EVIDENCE COMPLETE**

Tag `v2.0.0` only on the evidence-only commit that immediately follows qualified source
`88f3849b9188ff4e20a7d6be1649063d5501fb4e`, after that committed ledger state passes the final
`--release-evidence` gate successfully.

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
