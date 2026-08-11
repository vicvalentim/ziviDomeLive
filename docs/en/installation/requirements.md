# System Requirements

## Runtime

- Processing 4; release 1.5.0 is built and tested against Processing core `4.5.6`
- `P3D` renderer
- `pixelDensity(1)` recommended for stable cross-display behavior
- A working Processing/JOGL OpenGL context

Java 17 is required when building the library from source. Processing supplies its own Java runtime for installed sketches.

## Hardware

The practical requirement depends on scene complexity, output resolution, and simultaneous outputs. A dedicated GPU is recommended for 3K/4K spherical rendering and shader-heavy scenes. Qualify the exact GPU, driver, projector, lens, and receiver chain before production use.

## Platform Capabilities

| Capability | macOS | Windows | Linux |
|---|---|---|---|
| Standard and spherical rendering | Supported | Supported | Supported |
| NDI | Native/receiver qualification required | Native/receiver qualification required | Reduced and unqualified |
| Syphon | Platform backend | Not available | Not available |
| Spout | Not available | Platform backend | Not available |

"Supported" for core rendering describes the intended platform boundary, not a claim that every GPU/driver combination has passed the manual visual protocol.

## Apple Silicon

The Processing/Syphon stack used by this project may require the Intel Processing build under Rosetta 2 for complete Syphon interoperability. Native ARM rendering without Syphon can still be used, but must be qualified with the target sketch and driver.

See [Known Issues](../known-issues.md) and the release qualification protocol before deployment.
