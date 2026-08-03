# Known Issues

## Apple Silicon and Syphon

On macOS with Apple Silicon, full Syphon interoperability may still require the Intel build of Processing running under Rosetta 2. The native ARM Processing stack does not currently provide the same Syphon support level.

## Linux external outputs

Linux builds have reduced support for external video outputs compared with macOS and Windows because the Processing ecosystem dependencies used by this library do not provide the same native integrations for NDI, Syphon, and Spout.

## OpenGL error 1282

Older builds could emit:

```text
OpenGL error 1282 at bot endDraw(): invalid operation
```

This was caused by invalid OpenGL state during output frame capture. The fix shipped in `1.4.0`, and current builds avoid reopening the draw context while preparing NDI frames.