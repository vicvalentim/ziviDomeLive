# Known Issues

## Apple Silicon and Syphon

On macOS with Apple Silicon, full Syphon interoperability may still require the Intel build of Processing running under Rosetta 2. The native ARM Processing stack does not currently provide the same Syphon support level.

## Linux external outputs

Linux builds have reduced support for external video outputs compared with macOS and Windows because the Processing ecosystem dependencies used by this library do not provide the same native integrations for NDI, Syphon, and Spout.

## OpenGL error 1282

Some configurations emit:

```text
OpenGL error 1282 at bot endDraw(): invalid operation
```

This is a `GL_INVALID_OPERATION` raised by the JOGL/Processing OpenGL driver, typically triggered by invalid framebuffer state during rendering or output capture. The error is endemic to certain hardware and driver combinations and has not been fully eliminated. It is generally non-fatal — rendering continues — but may indicate instability in specific setups (particularly Apple Silicon, certain GPU drivers, or complex multi-pass rendering). We continue to investigate the root cause.

**Workarounds that may reduce frequency:**
- Run Processing using the Intel (Rosetta 2) build on Apple Silicon.
- Keep external outputs (NDI, Syphon, Spout) disabled when not in use.
- Use the latest GPU drivers for your platform.