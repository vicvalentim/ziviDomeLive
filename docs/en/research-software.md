---
title: Research Software and JOSS Readiness
icon: material/school-outline
status: qualification
tags:
  - Research software
  - JOSS
  - Reproducibility
---

# Research Software and JOSS Readiness

ziviDomeLive is developed as open-source research software and as a technical-artistic artifact for creative coding, fulldome, immersive-media, artistic-research and education workflows.

!!! important "Readiness map, not publication status"
    This page maps repository evidence to current JOSS-style review concerns. It does not claim submission, review, acceptance, endorsement or a JOSS paper DOI.

## Research provenance and institutional context

The library originated in 2024 as part of the Arts doctoral research in the Technological Poetics research line at PPGARTES/UFMG, *O domo é vivo: entre técnica, sensível e poética em imersão* ([institutional record](https://hdl.handle.net/1843/981)). This origin situates the software as a research-creation artifact concerned with the relationships among technique, sensory experience, poetics and immersion.

Its current research context is the project *Arte, Codificação e Imersão: pesquisa-criação, open-source e ecossistemas audiovisuais imersivos*, selected under [PIBITI/UFRB Call no. 05/2026](https://ufrb.edu.br/ppgci/noticias/1624-edital-05-2026-do-pibiti-26-27) for the 2026–2027 cycle. Prof. Dr. Victor Hugo Soares Valentim coordinates the project, with undergraduate scholarship students **Tiago Silva Rosa** and **David Siqueira de Araujo**, both at CECULT/UFRB, participating as collaborators. This institutional provenance describes the research setting; it does not by itself establish research impact, publication status or authorship of a particular release.

### Research record

- [Open Source Science project record](https://science.ecosyste.ms/projects/36511);
- Victor Hugo Soares Valentim, [*ziviDomeLive como artefato de pesquisa e experimentação em arte e tecnologia para criação de experiências audiovisuais ao vivo em fulldome*](https://files.cercomp.ufg.br/weby/up/777/o/ziviDomeLive_como_artefato_de_pesquisa_e_experimentac%CC%A7a%CC%83o_em_arte_e_tecnologia_para_criac%CC%A7a%CC%83o_de_experie%CC%82ncias_audiovisuais_ao_vivo_em_fulldome.pdf), *Proceedings of the XIII International Symposium on Innovation in Interactive Media — Paradigmas*, Goiânia: Media Lab / Iberoamérica, PPG ACT, 2026, pp. 615–628, ISSN 2358-0488;
- the [research-integrity, full human-review and conduct declaration](research-integrity.md), which records the normative framework, AI-use disclosure and current research-team credit.

## Statement of need

### Problem

A Processing artist who targets a dome or spherical image must coordinate multi-view capture, per-frame state consistency, projection conversion, calibration, scene/resource lifecycle and optional live publication. Implementing those boundaries independently in every sketch increases the chance of face-dependent animation, graphics-context errors, unbounded background work and output stalls.

### Audience

The primary audience is artists, creative coders, researchers, educators, students, planetarium practitioners, installation technicians, VJs and developers who want a Processing-oriented scene contract instead of a low-level rendering engine API.

### Contribution

ziviDomeLive provides:

- one scene lifecycle in which mutable state advances once and can be rendered several times;
- independent Standard and spherical rendering domains;
- final Standard, Domemaster, Equirectangular and Skybox representations;
- spherical orientation and domemaster physical calibration;
- activation-scoped time, task, asset, action, camera, environment and port services;
- typed, opt-in NDI/Syphon/Spout control with bounded runtime ownership;
- executable examples, automated qualification and target-hardware protocols.

The scholarly/technical value lies in making these constraints explicit and teachable within Processing, not in exposing an unrestricted OpenGL framework.

## State of the field

Processing supplies the creative-coding environment and OpenGL renderer; NDI, Syphon and Spout ecosystems supply transport/sharing mechanisms; fulldome production defines projection and calibration needs. ziviDomeLive composes those boundaries around a Processing `Scene` lifecycle.

A future JOSS submission still needs a peer-reviewed, cited comparison with commonly used fulldome/spherical Processing libraries and adjacent creative-coding tools, including a clear build-versus-contribute rationale. The repository does not currently present that literature comparison as complete.

## Evidence matrix

| Review concern | Repository evidence | Status for a future submission |
|---|---|---|
| Open-source license | `LICENSE`, `THIRD_PARTY.md`, packaged notices | Ready for review |
| Statement of need/audience | README and this page | Ready for review |
| Installation/dependencies | README, Installation section, Gradle/bootstrap and Processing package | Ready; installed-package evidence remains pre-tag |
| Example usage | Six learning sketches plus two qualification tools | Ready; final installed-package run remains pre-tag |
| API documentation | Public API freeze test, Javadocs, API levels, EN/PT guide | Ready for review |
| Automated tests | JUnit suites, `qualificationTests`, CI workflows | Ready; exact current totals belong to generated evidence |
| Manual scientific/visual checks | Calibration and benchmark protocols, receiver checklist | Protocol ready; hardware evidence incomplete |
| Community guidelines | Contributing guide, issue tracker, support path | Ready for review |
| Citation/authorship | `CITATION.cff`, `.zenodo.json`, DOI/ORCID metadata | External DOI record verification remains pre-tag |
| State-of-field comparison | Explicit gap above | **Not ready** |
| Research impact | Evidence policy below | **Not ready without concrete external evidence** |
| Software paper | No `paper.md` is claimed in 2.0 | **Not submitted** |

## Reproducibility levels

```mermaid
flowchart TB
  L1[Level 1<br/>source contract] --> L2[Level 2<br/>automated clean build and tests]
  L2 --> L3[Level 3<br/>Processing package install and examples]
  L3 --> L4[Level 4<br/>GPU visual and benchmark evidence]
  L4 --> L5[Level 5<br/>native receiver and installation evidence]
```

### Level 1 — Contract

Versioned source, exact public API snapshot, lifecycle tests, documentation validator and conservative metadata.

### Level 2 — Automated execution

Clean Java 17 build, JUnit/qualification suites, strict MkDocs build, Javadocs and deterministic release-package checks in CI.

### Level 3 — Processing distribution

Install generated ZIP/PDEX into a clean sketchbook, open `reference/index.html`, and compile/run all eight examples from the installed package.

### Level 4 — Visual/performance qualification

Record OpenGL environment, projection/view, resolution and calibration results; run BenchmarkTool with warm-up, duration and metric mode declared.

### Level 5 — External interoperability

Record the exact sender/receiver, OS, architecture, runtime, network and backend for every NDI/Syphon/Spout claim.

Lower-level success never implies a higher level.

## Research impact evidence policy

A DOI, affiliation or statement of potential use is not evidence of research impact. A future impact statement must cite concrete, verifiable material such as publications using the software, documented independent users, integration in research/teaching workflows, archived benchmark improvements or reproducible research artifacts. Until that evidence is assembled, the honest status is **incomplete**.

## Community, support and credit

- contribute code/tests/docs through the [Contributing Guide](contributing.md);
- report reproducible problems or seek public support through [GitHub Issues](https://github.com/vicvalentim/ziviDomeLive/issues);
- cite the software through [Citation](citation.md) and `CITATION.cff`;
- preserve contributor authorship/credit according to actual intellectual contribution.

## AI-assisted work and full human review

The 2.0 work was assisted by OpenAI Codex under the direction of Prof. Dr. Victor Hugo Soares Valentim. Assistance included codebase analysis, implementation and refactoring support, test and documentation development, and validation workflow execution. The author declares that he reviewed the resulting code, documentation, tests, technical and research claims, references and release materials in full, understands and approves the final decisions, and accepts full human responsibility for research and development in art and technology.

The complete scope, limits and normative basis of this statement are recorded in [Research Integrity, Human Review and Conduct](research-integrity.md). Any future JOSS paper must include its own complete disclosure and human verification procedure.

## Reviewer commands

```bash
./gradlew clean test build
./gradlew qualificationTests
python3 tools/validate_documentation.py --root .
python3 -m mkdocs build --strict
./gradlew buildReleaseArtifacts
python3 tools/validate_documentation.py \
  --root . \
  --package release/ziviDomeLive.zip \
  --release-dir release
```

GPU, projector, benchmark and receiver evidence cannot be replaced by a headless command.
