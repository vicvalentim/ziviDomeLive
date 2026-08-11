# Rendering Pipeline

ziviDomeLive 1.5 keeps two rendering domains. `RenderMode` selects behavior but does not collapse them into one backend.

## Standard Domain

```text
Scene
  -> StandardRenderer
  -> perspective PGraphicsOpenGL target
  -> window preview or enabled output
```

Standard renders the scene directly through its perspective camera. It does not capture cubemap faces and is not derived from equirectangular or fisheye content. Preview and output Standard renderers share camera state so framing remains consistent across their different target sizes.

## Spherical Domain

```text
Scene
  -> six 90-degree cubemap faces
     -> CubemapViewRenderer -> skybox layout
     -> EquirectangularRenderer -> 2:1 map
        -> FisheyeDomemaster -> square domemaster + Size% scaling
```

The six faces use the stable `CameraManager` orientation table. One shared `SphericalOrientation` quaternion is applied to every face for preview and output.

This topology describes the 1.x implementation, not a permanent backend contract. A future major version may change textures or projection internals while preserving qualified visual behavior.

## Requirement Closure

`RenderRequirementsPolicy` expands requested views into only the passes needed for that frame:

| Requested representation | Standard | Cubemap source | Equirectangular | Fisheye | Cubemap layout |
|---|---:|---:|---:|---:|---:|
| Standard | Yes | No | No | No | No |
| Skybox | No | Yes | No | No | Yes |
| Equirectangular | No | Yes | Yes | No | No |
| Domemaster | No | Yes | Yes | Yes | No |

The preview request, floating domemaster request, and all enabled output requests are resolved independently and then shared where possible.

## One Frame

The automatic Processing hooks execute this order:

1. `pre()` calls `Scene.update()` once.
2. `draw()` clears the window and exits early until managers are ready.
3. A pending output-resolution change recreates output render targets.
4. Window-dependent preview renderers are checked and recreated when their automatic resolution changes.
5. Preview and output requirements are resolved.
6. At most one master cubemap is captured when spherical content is required.
7. Enabled output projection passes run and completed targets are submitted to their backends.
8. Preview passes run, reusing completed output projections where available.
9. The effective preview is composited into the Processing window.
10. The optional floating domemaster and ControlP5 panel are drawn.

`Scene.sceneRender()` can therefore execute more than once in one Processing frame: once for Standard and once for each required cubemap face. Animation and simulation changes belong in `update()`.

## Master Cubemap Reuse

When an enabled output requires spherical data, its output-resolution cubemap becomes the master source for both output and preview projections. Matching completed output projections may be down-copied into preview targets. Without spherical output demand, the library captures only the automatic preview-resolution cubemap.

This avoids duplicate scene capture while keeping the Processing window and external outputs in separate target domains.

## Resolution Ownership

| Target | Dimension policy | Recreated when |
|---|---|---|
| Standard preview | Current window width and height | Window changes |
| Spherical preview | `min(1024, max(256, min(width, height)))` square | Bucket changes |
| Output targets | Selected `1024`, `2048`, `3072`, or `4096` base | Deferred `resetGraphics()` is applied |

Output resolution does not redefine preview resolution. Reallocation happens on the Processing draw thread, never directly inside the UI callback.

## Output Boundary

- Syphon and Spout publish completed `PGraphicsOpenGL` textures on the Processing/GPU path.
- NDI calls `loadPixels()` on the Processing thread, copies into one of three CPU slots, and sends packed progressive RGBA from a dedicated worker.
- No OpenGL call is made by the NDI worker.
- Publication state is distinct from backend initialization and from render requirements.

## Stable and Internal Contracts

Stable for 1.5:

- Standard/spherical behavioral separation
- cubemap face orientation and skybox layout
- rendered scene content
- quaternion pitch/yaw/roll behavior
- domemaster FOV and Size%
- automatic preview and independent output resolution policies

Internal implementation details:

- `PGraphicsOpenGL[]` as cubemap storage
- domemaster currently consuming equirectangular output
- exact renderer allocation/copy strategy

See [Runtime Lifecycle](runtime-lifecycle.md) and [Release Readiness](../qualification/1.5-release-readiness.md).
