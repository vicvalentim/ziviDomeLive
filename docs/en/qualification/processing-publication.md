---
title: "Processing Contributed Library Publication"
icon: material/check-decagram-outline
status: qualification
---
# Processing Contributed Library Publication

This checklist is a **maintainer/publication** surface. It is not part of the artist learning path.

## Publication artifact

The generated Processing library must remain self-contained for installation, examples, source inspection, API reference, licensing and citation. MkDocs is the technical manual; a future GitBook is not a dependency of the Processing package.

## AUTOMATED

- [ ] `./gradlew clean test build --console=plain`
- [ ] `./gradlew qualificationTests --console=plain`
- [ ] documentation validator passes
- [ ] `python3 -m mkdocs build --strict` passes for the configured EN/PT build
- [ ] `./gradlew buildReleaseArtifacts --console=plain`
- [ ] generated ZIP/PDEX package structure passes package validation

## GPU VISUAL

- [ ] [CalibrationTool](calibration-tool.md) inspected on each configuration used as GPU qualification evidence
- [ ] Domemaster orientation/calibration checked
- [ ] Equirectangular checked
- [ ] Skybox checked
- [ ] Standard path checked
- [ ] Environment checked against the current LDR equirectangular background contract

## BENCHMARK

- [ ] [BenchmarkTool](benchmark-guide.md) smoke completed
- [ ] CPU baseline recorded
- [ ] CPU/GPU measurement mode recorded when available in the tool
- [ ] report identifies version/commit, resolution, routes, Processing/Java, OS and hardware

## NATIVE OUTPUT

Only claim a backend/platform as **tested** after end-to-end evidence:

- [ ] NDI receiver test on every platform claimed for NDI
- [ ] Syphon receiver test on every macOS configuration claimed
- [ ] Spout receiver test on every Windows configuration claimed

Supported code paths and tested release platforms are different facts.

## PACKAGE INSTALLATION

The final package must include and expose:

- [ ] `library/`
- [ ] `reference/index.html`
- [ ] `examples/` with the six learning examples plus `CalibrationTool` and `BenchmarkTool`
- [ ] `src/` without `src/test/`
- [ ] `library.properties`
- [ ] project/source license and third-party notices
- [ ] citation metadata shipped according to the current packaging task

It must not contain local benchmark reports, maintainer-only generated evidence, `.DS_Store`, tests or local helper JARs excluded by the release packaging contract.

Open/run all eight examples **from the installed package**.

## PUBLICATION METADATA

- [ ] `name`, `authors`, `url`, `categories`, `sentence`, `paragraph`, `version`, `prettyVersion`, `minRevision`, `maxRevision` validated against the current Processing contribution parser/rules
- [ ] library keywords describe implemented capabilities (no generic VR/XR claim)
- [ ] tested-platform metadata is absent unless backed by release qualification evidence
- [ ] Processing revision/version claims correspond to the real supported/tested boundary
- [ ] software DOI consistent in `CITATION.cff`, `.zenodo.json`, README and MkDocs
- [ ] no documentation DOI or ISBN has been invented

## Stable release files

The release workflow must publish the generated siblings:

- `ziviDomeLive.zip`
- `ziviDomeLive.txt`
- `ziviDomeLive.pdex`

Validate that the Processing contribution URL points to the intended stable release artifact before submitting/updating the contribution.

## Tag rule

The tag is the release publication point, **not the first qualification run**. Automated, GPU, benchmark, native-output, package-installation and publication-metadata evidence must be complete before creating `v2.0.0`.
