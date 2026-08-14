# Rendering Pipeline

ziviDomeLive keeps two rendering domains. `RenderMode` selects behavior but does not collapse them into one backend.

## Standard Domain

```text
Scene
  -> StandardRenderer
  -> optional far-depth Environment pass from perspective camera rays
  -> perspective PGraphicsOpenGL target
  -> window preview or enabled output
```

Standard renders the scene directly through its perspective camera. It does not capture cubemap faces and is not derived from equirectangular or fisheye content. Preview and output Standard renderers share camera state so framing remains consistent across their different target sizes. Its Environment pass draws a sky sphere in camera space just inside the far plane, then converts each sphere direction back to world space through the inverse camera basis before sampling the equirectangular source. The panorama therefore follows camera rotation, while the observer-centred sphere prevents camera translation and orbit distance from introducing parallax.

## Spherical Domain

```text
Scene.sceneRender(PGraphicsOpenGL)
  -> one reusable face scratch target
  -> optional far-depth environment background in the same scratch target
  -> GPU framebuffer resolve/blit
  -> native GL_TEXTURE_CUBE_MAP
     -> CubemapViewRenderer -> skybox layout
     -> EquirectangularRenderer -> 2:1 map
     -> FisheyeDomemaster -> square domemaster + Size% scaling
```

The native cubemap capture uses the stable `CubemapFace` orientation table (`+X`, `-X`, `+Y`, `-Y`, `+Z`, `-Z`). `CameraManager` exposes that table through the qualified 1.x camera contract. The scene is rendered into one reusable offscreen `PGraphicsOpenGL` scratch target, Processing's resolved color framebuffer is selected, and a vertically converted GPU framebuffer blit copies the result into the matching native cubemap face. One shared `SphericalOrientation` quaternion is applied to every face for preview and output.

When an LDR equirectangular environment background is configured, `EnvironmentBackgroundRenderer` draws it after `Scene.sceneRender(PGraphicsOpenGL)` at far-plane depth in that same scratch framebuffer, before its resolved color is copied. The Environment orientation composes the spherical Pitch/Yaw/Roll quaternion followed by the shared scene-space `OrbitCamera` quaternion. Target and distance are excluded. This makes the background behave like an infinite environment: scene-owned `background()` calls cannot erase it, foreground geometry stays in front, and domemaster, equirectangular, and skybox projections share the same cubemap source and orientation.

## Environment Boundary and Ownership

One `EnvironmentState` owned by the facade is shared by Standard output, Standard preview, spherical output, and spherical preview. It contains the borrowed LDR source plus `visible`, visual `intensity`, longitude `yawOffset`, and a defensive copy of the current scene-camera orientation; renderers do not duplicate this logical state. `PImage` remains the Processing-friendly input, while its Processing-managed `Texture` is the operational shader resource. ziviDomeLive owns its shader programs and native cubemap targets, but never owns or disposes the source image/texture.

The Environment shaders implement base-level bilinear filtering directly: longitude indices wrap and latitude indices clamp at the poles, so the borrowed Processing texture parameters are never mutated. SamplerCube consumers save, enable, and restore cross-face seamless sampling and the prior cubemap binding. Depth, blend, cull, and scissor state changed by an Environment pass is scoped and restored.

The current boundary is intentionally `LDR equirectangular source -> GPU texture -> visual background`. A future lighting processor may independently add `HDR source -> floating-point texture -> environment cubemap -> diffuse irradiance + specular prefilter + BRDF LUT -> IBL/PBR -> AO integration`. No lighting responsibility belongs to `EnvironmentBackgroundRenderer`.

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
texture policy, a reusable face framebuffer, and a depth renderbuffer. Runtime
capture preserves the `Scene.sceneRender(PGraphicsOpenGL)` contract by using one
offscreen Processing scratch target, resolving it on the GPU when MSAA is
enabled, and blitting its final color into each native cubemap face. No legacy
`PGraphicsOpenGL[]` face array or six-texture fallback path is kept.

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
