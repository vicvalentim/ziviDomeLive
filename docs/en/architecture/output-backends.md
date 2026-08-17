---
title: "Output Backends"
icon: material/source-branch
---
# Output Backends


The artist-facing API treats each external output as a destination that requests a `ViewType`. Internally, the transport path depends on the backend.

## NDI

NDI is the CPU/network boundary in the current architecture: a final rendered view is captured for CPU-accessible frame data and sent through the NDI/Devolay path. The implementation uses bounded asynchronous publishing behavior rather than letting network send work redefine the render loop.

Qualification must include a real receiver. Availability/initialization is not proof of end-to-end output.

## Syphon

Syphon is the macOS platform-local GPU texture-sharing backend. Qualification requires a real Syphon receiver on the claimed macOS configuration.

## Spout

Spout is the Windows platform-local GPU texture-sharing backend. Qualification requires a real Spout receiver on the claimed Windows configuration.

## Routing invariant

Backends consume final views; they should not own scene rendering logic. `OutputManager` selects the destination view and publishes the corresponding final target.
