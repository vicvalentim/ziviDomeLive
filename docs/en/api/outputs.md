---
title: "Outputs API"
icon: material/api
status: stable
---
# Outputs API

Use `OutputManager` through `ziviDomeLive.getOutputManager()` for external-output routing.

## Per-destination ViewType

The current routing surface includes destination-specific selectors such as:

- `setNdiView(ViewType)`
- `setSyphonView(ViewType)`
- `setSpoutView(ViewType)`
- `setLocalTextureView(ViewType)`

This is the preferred model in `RenderMode.FULL`: each destination can request its own final representation.

## Enable and inspect

`toggleOutput(String)` accepts the documented backend names `"ndi"`, `"syphon"` and `"spout"`.

Use output state/failure accessors such as `getOutputState(...)` and `getOutputFailureReason(...)` instead of assuming that a UI toggle proves successful backend initialization.

## Deprecated generic routing

The generic `setView(ViewType)` compatibility method is deprecated. New code should select a view for the intended destination explicitly.

Backend worker/buffer/GL details are Developer Guide material. The artist-facing contract is destination → `ViewType` → enable → inspect state → test with a real receiver where applicable.
