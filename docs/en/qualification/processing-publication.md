---
title: "Processing Contributed Library Publication Procedure Template"
icon: material/check-decagram-outline
status: qualification
---
# Processing Contributed Library Publication Procedure Template

This checklist is a reusable **maintainer/publication procedure template**, not a status ledger and
not part of the artist learning path. Keep every run's authoritative status in
[`maintainer/release-evidence.md`](https://github.com/vicvalentim/ziviDomeLive/blob/release/2.0.0/maintainer/release-evidence.md)
and its CI artifact bound to the qualified source SHA. Never copy completion state back into this
template or infer it from an older qualification.

## Publication artifact

The generated Processing library must remain self-contained for installation, examples, source inspection, API reference, licensing and citation. MkDocs is the technical manual; a future GitBook is not a dependency of the Processing package.

## Official-guideline mapping

### Project homepage

- [ ] stable public URL presents a concise abstract and the intended audience
- [ ] installation distinguishes Contribution Manager, release artifact and source build
- [ ] all ten executable examples/tools are listed, with tutorials/manual linked when useful
- [ ] Processing/Java baseline, renderer, `pixelDensity(1)`, dependencies and limitations are stated
- [ ] tested systems/Processing versions are either backed by evidence or explicitly left unqualified
- [ ] `library.keywords` and the documentation-update date are visible
- [ ] latest stable release and the untagged 2.0 development state are not confused
- [ ] direct stable-release links use the common `ziviDomeLive` basename for ZIP/TXT/PDEX

### Package and reference

- [ ] Javadocs are generated into `reference/index.html`
- [ ] source, examples, license/notices and `library.properties` ship beside the runtime library
- [ ] the package root/folder name and artifact basenames remain `ziviDomeLive`
- [ ] examples compile against the installed package, not only the repository build
- [ ] the public reference contains only the intended Stable, Advanced Stable and Experimental types

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
- [ ] `examples/` with `GettingStarted/`, `Advanced/`, and `Tools/`, containing all eight learning examples plus `CalibrationTool` and `BenchmarkTool`
- [ ] `src/` without `src/test/`
- [ ] `library.properties`
- [ ] project/source license and third-party notices
- [ ] citation metadata shipped according to the current packaging task
- [ ] README, CHANGELOG, `CITATION.cff` and `THIRD_PARTY.md`

It must not contain local benchmark reports, maintainer-only generated evidence, `.DS_Store`, tests or local helper JARs excluded by the release packaging contract.

Open/run all ten examples/tools **from the installed package**.

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
