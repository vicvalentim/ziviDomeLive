---
title: About ziviDomeLive
description: Project identity, purpose, history, research context, authorship, citation and licensing
icon: material/information-outline
tags:
  - Project
  - Creative coding
  - Research software
---

# About ziviDomeLive

ziviDomeLive is an open-source Java library for **Processing 4** that turns one creative-coding scene into real-time Standard, Domemaster, Equirectangular and Skybox representations. It is designed for artists, creative coders, planetarium practitioners, researchers, educators, students, VJs, developers and installation teams working with fulldome, spherical and immersive images.

## At a glance

| | Project identity |
|---|---|
| **Platform** | Processing 4 and Java 17 |
| **Primary domain** | Real-time monoscopic fulldome and spherical rendering |
| **Representations** | Standard, Domemaster, Equirectangular and Skybox |
| **License** | GPL-2.0-only |
| **Software DOI** | [10.5281/zenodo.15671506](https://doi.org/10.5281/zenodo.15671506) |
| **Source** | [github.com/vicvalentim/ziviDomeLive](https://github.com/vicvalentim/ziviDomeLive) |

## Why the project exists

Processing makes real-time graphics approachable, but a production fulldome workflow must coordinate spherical capture, multiple projections, calibration, scene lifecycle, resource ownership and non-blocking output publication. ziviDomeLive provides one Processing-oriented boundary for those concerns while keeping scene code close to an ordinary sketch.

The project focuses on **image generation and routing**. It is not a stereoscopic VR runtime, headset engine, projection-mapping suite or general application framework.

## History and stewardship

The repository preserves the architectural path from the original 1.x renderer through the 1.5 consolidation and the deliberate 2.0 public-API reset. Current tutorials describe only callable 2.0 behavior; historical names remain in the [1.x migration record](api/deprecated.md), [release notes](release-notes/2.0.0.md) and project changelog.

Technical claims in this site are governed by source, tests and executable examples. Qualification and publication evidence are kept separate from artist-facing guidance so that supported capability, automated coverage and physically verified platforms are not conflated.

## Project information

<div class="grid cards" markdown>

-   :material-microscope: **Research software**

    Statement of need, evidence levels, reproducibility, JOSS readiness and the limits of current research-impact claims.

    [Research and JOSS readiness →](research-software.md)

-   :material-format-quote-close: **Citation**

    Cite the software release with its machine-readable metadata and keep software, documentation and future-paper identifiers distinct.

    [Citation guidance →](citation.md)

-   :material-account: **Author**

    Project authorship, academic affiliations, website and ORCID information.

    [About the author →](author.md)

-   :material-scale-balance: **License**

    Project license, dependency notices, bundled assets and the location of authoritative legal texts.

    [License and notices →](license.md)

</div>

## Participate

Use the [contribution guide](contributing.md) for code and documentation changes, review [known issues](known-issues.md) before reporting a problem, and consult the [roadmap](roadmap.md) for work that is intentionally outside the frozen 2.0 contract.
