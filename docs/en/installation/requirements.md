# System Requirements

## Runtime

- Processing 4 (revision `1285` or newer). Source compatibility can be audited against the official Processing 4.0 distribution with `./gradlew compileProcessing4Baseline -Pprocessing4BaselineLibrary=/path/to/processing-4.0/core/library`. Routine automated tests use Processing 4.5.6; that tested version does not replace the Processing 4.0 compatibility baseline, and a static compile is not a substitute for physical runtime qualification.
- `P3D` renderer
- `pixelDensity(1)` recommended for stable cross-display behavior
- A GPU and driver exposing an OpenGL 4.1 context; packaged shaders use GLSL 4.10

Java 17 is required when building the library from source. Processing supplies its own Java runtime for installed sketches.

## Hardware

The practical requirement depends on scene complexity, output resolution, and simultaneous outputs. A dedicated GPU is recommended for 3K/4K spherical rendering and shader-heavy scenes. Integrated or legacy GPUs that cannot create an OpenGL 4.1 core context cannot compile the packaged projection shaders. Qualify the exact GPU, driver, projector, lens, and receiver chain before production use.

## Platform Capabilities

| Capability | macOS | Windows | Linux |
|---|---|---|---|
| Standard and spherical rendering | Supported | Supported | Supported |
| NDI video sender | Experimental; separate NDI Runtime and receiver qualification required | Experimental; separate NDI Runtime and receiver qualification required | Experimental; separate NDI Runtime and receiver qualification required |
| Syphon | Platform backend | Not available | Not available |
| Spout | Not available | Platform backend | Not available |

"Supported" for core rendering describes the intended platform boundary, not a claim that every GPU/driver combination has passed the manual visual protocol.

## Apple Silicon

Native Apple Silicon and Intel Processing configurations are valid ziviDomeLive targets.

For Syphon specifically, the upstream Syphon for Processing 4.0 distribution does not currently include the native `macos-aarch64` payload needed by Processing 4. Apple Silicon users can install the ziviDomeLive community universal compatibility asset (`arm64` + `x86_64`):

[Syphon-for-Processing-4.0-macOS-universal-community.zip](https://github.com/vicvalentim/ziviDomeLive/releases/download/v2.0.0/Syphon-for-Processing-4.0-macOS-universal-community.zip)

This package is external to the ziviDomeLive Processing library and is not an official Syphon Project release. Rosetta 2 is not a general ziviDomeLive requirement.

See [Dependencies](dependencies.md) for installation and checksum details.

Before deployment, review [Known Issues](../known-issues.md), [NDI Runtime](ndi.md), the [Calibration Tool Protocol](../qualification/calibration-tool.md), and [Release Readiness](../qualification/2.0-release-readiness.md).
