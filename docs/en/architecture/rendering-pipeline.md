# Rendering Pipeline

ziviDomeLive keeps two rendering domains. `RenderMode` selects behavior but does not collapse them into one backend.

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
Optional environment background
Scene.sceneRender(PGraphicsOpenGL)
  -> native GL_TEXTURE_CUBE_MAP capture
     -> CubemapViewRenderer -> skybox layout
     -> EquirectangularRenderer -> 2:1 map
     -> FisheyeDomemaster -> square domemaster + Size% scaling
```

The native cubemap capture uses the stable `CubemapFace` orientation table (`+X`, `-X`, `+Y`, `-Y`, `+Z`, `-Z`). The scene is emitted through one offscreen `PGraphicsOpenGL` command target and rendered into each face of a native cubemap framebuffer. `CameraManager` remains as the compatibility facade for direct renderer integrations, but the runtime cubemap capture uses the canonical `CubemapFace` table as its authoritative source. One shared `SphericalOrientation` quaternion is applied to every face for preview and output.

When an LDR equirectangular environment background is configured, `EnvironmentBackgroundRenderer` fills each cubemap face first with depth testing disabled, then delegates to `Scene.sceneRender(PGraphicsOpenGL)`. This keeps sky and star-field maps out of scene geometry while preserving the same cubemap source for domemaster, equirectangular, and skybox projections.

All spherical projections sample the native cubemap through `samplerCube`; domemaster/fisheye no longer depends on an intermediate equirectangular texture.

## Requirement Closure

`RenderRequirementsPolicy` expands requested views into only the passes needed for that frame:

| Requested representation | Standard | Cubemap source | Equirectangular | Fisheye | Cubemap layout |
|---|---:|---:|---:|---:|---:|
| Standard | Yes | No | No | No | No |
| Skybox | No | Yes | No | No | Yes |
| Equirectangular | No | Yes | Yes | No | No |
| Domemaster | No | Yes | No | Yes | No |

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

## Processing GL Boundary

`ProcessingGlAdapter` is the narrow boundary for current Processing/OpenGL calls:
target allocation, texture presence checks, `loadPixels()` readback for NDI,
target disposal, and capability discovery from the active PGL context. The
reported capabilities include texture, FBO, cubemap, seamless cubemap, PBO, and
sync fence support so native capture and readback paths can gate their GL usage
explicitly.

`CubemapTarget` owns native `GL_TEXTURE_CUBE_MAP` storage with conservative
texture policy, a render framebuffer, and a depth renderbuffer. Runtime capture
preserves the `Scene.sceneRender(PGraphicsOpenGL)` contract by using one
offscreen Processing graphics object as the command emitter while binding each
native cubemap face as the active framebuffer target. No legacy `PGraphicsOpenGL[]`
face array or six-texture fallback path is kept.

SamplerCube projection shader resources for cubemap, equirectangular,
domemaster/fisheye, and skybox modes are staged under
`data/shaders/samplercube/` in packaged artifacts. All spherical runtime
renderers sample the native cubemap directly.

LDR environment background shaders are staged under `data/shaders/environment/`.
They are deliberately separate from future HDR, IBL, and ambient-occlusion
passes.

## Resolution Ownership

| Target | Dimension policy | Recreated when |
|---|---|---|
| Standard preview | Current window width and height | Window changes |
| Spherical preview | `min(1024, max(256, min(width, height)))` square | Bucket changes |
| Output targets | Selected `1024`, `2048`, `3072`, or `4096` base | Deferred `resetGraphics()` is applied |

Output resolution does not redefine preview resolution. Reallocation happens on the Processing draw thread, never directly inside the UI callback.

## Output Boundary

- `RenderPipeline` supplies completed targets through the minimal `FrameViews` contract. `OutputManager` selects the logical `ViewType` to publish without inspecting the concrete renderer that produced it.
- One `FrameViews` boundary is reused for the runtime lifetime and resolves current targets lazily, so the hot path does not allocate a carrier per frame and deferred renderer resets cannot leave stale references.
- `OutputManager` coordinates routing while the concrete `NdiOutputBackend`, `SyphonOutputBackend`, and `SpoutOutputBackend` services own their native resources and lifecycle directly; there is no backend factory layer.
- Syphon and Spout publish completed `PGraphicsOpenGL` textures on the Processing/GPU path.
- NDI calls `loadPixels()` on the Processing thread, copies into one of three CPU slots, and sends packed progressive RGBA from a dedicated worker.
- No OpenGL call is made by the NDI worker.
- Publication state is distinct from backend initialization and from render requirements.

## Stable and Internal Contracts

Stable:

- Standard/spherical behavioral separation
- cubemap face orientation and skybox layout
- rendered scene content
- quaternion pitch/yaw/roll behavior
- domemaster FOV and Size%
- automatic preview and independent output resolution policies

Internal implementation details:

- one offscreen Processing graphics command target feeding native cubemap face FBOs
- `CubemapTarget` allocation and framebuffer policy
- exact renderer allocation and mipmap strategy

See [Runtime Lifecycle](runtime-lifecycle.md) and [Release Readiness](../qualification/2.0-release-readiness.md).
