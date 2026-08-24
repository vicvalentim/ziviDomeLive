# Known Issues

## OpenGL Error 1282

Some Processing/JOGL hardware and driver combinations emit:

```text
OpenGL error 1282 at bot endDraw(): invalid operation
```

This `GL_INVALID_OPERATION` remains endemic to some framebuffer, driver, and multi-pass configurations. Version 1.4 removed a nested NDI draw lifecycle that amplified it, but the broader issue is not considered resolved. It is often non-fatal, yet production systems should treat repeated messages as a qualification failure until rendering and output stability are confirmed.

Possible mitigations:

- Keep unused external outputs disabled.
- Use a stable GPU driver and Processing build for the target machine.
- On macOS, qualify the exact Processing/Syphon/GPU combination used for deployment.
- Reduce output resolution while isolating the failing pass.

## Apple Silicon and Syphon

The upstream Syphon for Processing 4.0 package does not currently ship the
native `macos-aarch64` payload required by Processing 4. That packaging gap can
surface as a JNI/native-library load failure on Apple Silicon.

Use the ziviDomeLive community universal compatibility build:

[Syphon-for-Processing-4.0-macOS-universal-community.zip](https://github.com/vicvalentim/ziviDomeLive/releases/download/v2.0.0/Syphon-for-Processing-4.0-macOS-universal-community.zip)

The package contains `arm64` + `x86_64` native slices and is not an official
Syphon Project release. Replace the existing `libraries/Syphon/` directory
rather than merging files. See
[Dependencies](installation/dependencies.md) for checksum and upstream
provenance.

Rosetta 2 is not the normal compatibility path for this build.
## Linux External Outputs

Core rendering is supported on Linux. Syphon and Spout are not available there; NDI video output is experimental and uses Devolay with a separately installed official NDI Runtime. The exact Linux runtime, driver, network and receiver combination must still be qualified for deployment.

## Native Output Qualification

Automated tests validate routing and lifecycle without opening real GPU or receiver sessions. Syphon, Spout, NDI discovery, receiver color/orientation, resize, repeated enable/disable, pause/resume, and shutdown must be checked on target hardware.

NDI is an experimental, unofficial, video-only sender and requires the
proprietary NDI Runtime to be [installed separately](installation/ndi.md). It is
not supplied by Processing's Contribution Manager or bundled in the release.

If an NDI native send does not return during shutdown, publication stops after a bounded wait and state becomes `STOPPING`; native cleanup completes after the worker exits.
