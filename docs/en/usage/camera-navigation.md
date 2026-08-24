---
title: "Camera and Navigation"
icon: material/palette-outline
---
# Camera and Navigation

Scene Camera and spherical orientation are separate layers.

```text
Scene Camera              Pitch / Yaw / Roll
moves/transforms scene  ≠  orients spherical representation
```

Use camera/navigation when the creative intention is to move the observer, target or scene-space framing. Use Pitch/Yaw/Roll when the installation needs the spherical representation reoriented relative to the dome/output.

The public camera service is intended for scene-space navigation. A scene may use `getSceneCamera()`/camera services according to the current API and example patterns.

Environment backgrounds remain observer-centred/translation-invariant: camera translation should not make an infinite background appear to move closer or farther. Orientation is composed according to the implemented camera/spherical pipeline rather than by treating the Environment as ordinary scene geometry.
