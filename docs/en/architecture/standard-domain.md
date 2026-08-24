---
title: "Standard Domain"
icon: material/source-branch
---
# Standard Domain


The Standard Domain renders ordinary perspective scene output independently from spherical capture.

## Contract

- source: the active `Scene`;
- renderer: the Standard rendering path;
- result: a Standard final view/target;
- camera: scene-space camera behavior applies here;
- dependency: no spherical cubemap is required for Standard-only work.

The library clears the Standard final target to transparent RGBA `(0, 0, 0, 0)` on every frame.
No dark sky is inserted automatically. A Scene can deliberately draw a Processing background or
fullscreen geometry, and a configured visible Environment is explicit background content. The
retained internal sky-colour compatibility path becomes opaque only after an explicit request.

When a frame needs both Standard and a spherical representation, both domains may render during that Processing frame. This is expected and is different from accidentally capturing the spherical domain multiple times for multiple consumers.
