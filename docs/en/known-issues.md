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
- On Apple Silicon, compare native ARM and Intel/Rosetta Processing when Syphon is required.
- Reduce output resolution while isolating the failing pass.

## Apple Silicon and Syphon

Complete Syphon interoperability may require Intel Processing under Rosetta 2. Native ARM rendering and Syphon are separate qualification questions.

## Linux External Outputs

Core rendering is intended to work on Linux, but the current Processing integrations do not provide Syphon or Spout there, and NDI native support remains reduced/unqualified.

## Native Output Qualification

Automated tests validate routing and lifecycle without opening real GPU or receiver sessions. Syphon, Spout, NDI discovery, receiver color/orientation, resize, repeated enable/disable, pause/resume, and shutdown must be checked on target hardware.

NDI is an experimental, unofficial, video-only sender and requires the
proprietary NDI Runtime to be [installed separately](installation/ndi.md). It is
not supplied by Processing's Contribution Manager or bundled in the release.

If an NDI native send does not return during shutdown, publication stops after a bounded wait and state becomes `STOPPING`; native cleanup completes after the worker exits.
