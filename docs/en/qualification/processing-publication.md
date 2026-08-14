# Processing Publication Checklist

Use this checklist before submitting ziviDomeLive to the Processing Contribution Manager or publishing an update. It follows the Processing library guidelines for metadata, documentation, examples, release artifacts, source availability, and licensing.

## Required release contents

| Requirement | ziviDomeLive location | Verification |
|---|---|---|
| Processing metadata | `library.properties`, `release/ziviDomeLive.txt` | `name`, `authors`, `url`, `categories`, `sentence`, and integer `version` are present |
| Installable archive | `release/ziviDomeLive.zip` | Contains `ziviDomeLive/library/ziviDomeLive.jar` |
| PDE installer artifact | `release/ziviDomeLive.pdex` | Byte-identical to the ZIP, with a `.pdex` extension |
| Examples | `release/ziviDomeLive/examples/` | Examples open from Processing's **File > Examples** menu |
| Reference documentation | `release/ziviDomeLive/reference/index.html` | Generated from Javadocs and updated for the release |
| Source code | GitHub repository | Public source remains available for review and long-term maintenance |
| License files | `LICENSE`, `THIRD_PARTY.md`, `licenses/` | Included in the release archive |

## Metadata rules

The Contribution Manager reads `library.properties`. Keep these fields aligned with the release:

- `name`: `ziviDomeLive`
- `authors`: author list with links where applicable
- `url`: stable documentation home page, not a direct download URL
- `categories`: Processing categories only; this release uses `3D, Video & Vision`
- `sentence`: one capitalized sentence ending with a period, without repeating the library name
- `paragraph`: second and following sentences for the Processing website; mention platform-specific limitations here
- `version`: integer release counter used for update checks
- `prettyVersion`: human-readable version without spaces
- `minRevision` and `maxRevision`: Processing revision bounds; leave `maxRevision=0` unless a future incompatible Processing revision is known

## Documentation home page

The public documentation site should remain available at a stable URL and include:

- a short abstract describing what the library does;
- installation instructions for Contribution Manager and manual installation;
- examples that demonstrate basic and advanced usage;
- tested operating systems and Processing version;
- dependency and runtime notes, including NDI Runtime separation;
- keywords and latest version information from `library.properties`;
- links to the ZIP/PDEX release artifacts when they are published;
- generated Javadocs under `/reference/`.

## Manual validation before submission

1. Run `./gradlew clean test build --console=plain`.
2. Run `./gradlew buildReleaseArtifacts --console=plain`.
3. Confirm `release/ziviDomeLive.zip`, `release/ziviDomeLive.pdex`, and `release/ziviDomeLive.txt` share the same base name and directory.
4. Install the package through `./gradlew deployToProcessingSketchbook --console=plain` or by manual extraction.
5. Restart Processing and open **File > Examples > Contributed Libraries > ziviDomeLive**.
6. Open `EmptyProject`, `Basic`, and `CalibrationTool`.
7. Record tested Processing version, OS, CPU architecture, GPU, driver, output backend, and any OpenGL warning that appears.
8. Complete the [2.0 Release Readiness](2.0-release-readiness.md) checklist for GPU and native-output evidence.

## Submission note

When the release artifacts and documentation are final, submit the library through the Processing contributions repository issue form. Processing maintainers may request metadata, packaging, or documentation changes before indexing the release.

## Official references

- [Processing Library Guidelines](https://github.com/processing/processing4/wiki/Library-Guidelines)
- [Processing Library Basics](https://github.com/processing/processing4/wiki/Library-Basics)
- [Processing Library Overview](https://github.com/processing/processing4/wiki/Library-Overview)
- [Processing Library Template: Release](https://processing.github.io/processing-library-template/release.html)
