---
title: "Deprecated API"
icon: material/api
status: deprecated
---
# Deprecated API

Deprecated API is documented for migration, not recommended for new examples.

Confirmed compatibility cases in the 2.0 public surface include:

- `OutputManager.setView(ViewType)` — use a destination-specific selector such as `setNdiView(...)`, `setSyphonView(...)`, `setSpoutView(...)` or `setLocalTextureView(...)` instead;
- facade convenience render methods `renderFisheyeDomemaster()`, `renderEquirectangular()`, `renderCubemap()` and `renderStandard()` — use the current render-mode/routing model instead.

The generated Javadocs are the authority for the exact `@Deprecated` annotations and replacement guidance. Do not remove a deprecated symbol from documentation while the implementation still exposes it.
