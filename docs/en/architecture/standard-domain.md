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

When a frame needs both Standard and a spherical representation, both domains may render during that Processing frame. This is expected and is different from accidentally capturing the spherical domain multiple times for multiple consumers.
