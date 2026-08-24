# Transparent Background Audit — 2.0.0

This audit classifies the source sweep requested for the transparent-final-frame contract. It is
source and automated-test evidence, not a substitute for receiver/platform qualification.

## Sweep scope

The audited command covers production Java and packaged shaders:

```bash
rg -n "background\\(|\\.clear\\(|FragColor|fragColor|vec4\\(|alpha|skyColor|skyR|skyG|skyB" \
  src/main/java shaders
```

## `background(...)` occurrences

| Location | Classification | Decision |
|---|---|---|
| `ziviDomeLive.draw()` initialization/pause gate | Primary Processing surface clear request with alpha zero | Preserved as four-component RGBA; the primary window may still be RGB/OS-composited |
| `ziviDomeLive.clearBackground()` | Primary preview-composite surface clear request with alpha zero | Preserved; off-screen preview alpha remains authoritative |
| `StandardRenderer.render()` | Retained sky-colour compatibility behavior | Gated by `skyColorExplicitlySet`; never runs by default |
| `DefaultScene.sceneRender()` | Explicit Scene-authored visual background | Preserved unchanged |
| Javadocs mentioning Scene `background()` | Artist-controlled behavior description | Preserved |

Every `background(...)` in `examples/**/*.pde` is Scene-authored content. The sweep found explicit
backgrounds in Basic Scene1/Scene2, NamedActions, PortLoopback, SolarSystem, CalibrationTool,
SphereParticle, InfiniteBackground and FulldomePBR. They remain untouched because the global
contract forbids only backgrounds introduced automatically by the library.

## `clear()` occurrences

| Category | Locations | Classification |
|---|---|---|
| Final/render targets | `StandardRenderer`, `FisheyeDomemaster`, `EquirectangularRenderer`, `CubemapViewRenderer`, `CubemapRenderer`, preview-copy destination | Required transparent RGBA reset |
| Native cubemap FBO | `CubemapTarget` after `clearColor(0,0,0,0)` | Required transparent color plus depth reset |
| Splash layers | `SplashScreen` background/animation layers | Transparent UI-layer reset |
| Collections/queues | Scene manager/services/actions/ports/tasks, render queue, resource cache, camera map, output slots, ControlP5 input map | Non-graphics lifecycle cleanup |
| NIO state buffers | GL adapter/target/environment state buffers and NDI RGBA buffer | Buffer-position reset; does not write framebuffer color |

Projection targets are cleared after allocation, before every successful pass, and after an
unavailable or failed input when the target already exists. A failed projection therefore cannot
republish stale opaque pixels. Fullscreen projection rectangles disable stroke and use Processing
`REPLACE` blending so the shader writes RGBA directly instead of accumulating alpha along a
stroke/overdraw path.

## Shader color writes

| Shader | Classification |
|---|---|
| `samplercube/cubemap.frag` | Samples and returns the complete cubemap `vec4`; no forced alpha |
| `samplercube/equirectangular.frag` | Returns the complete sampled cubemap `vec4`; no opaque fallback |
| `samplercube/skybox.frag` | Writes `vec4(0.0)` outside the cross and preserves sampled alpha inside |
| `samplercube/fisheye.frag` | Writes `vec4(0.0)` for `Size%=0`; sampled alpha is multiplied only by antialiased circle coverage |
| Standard/spherical Environment fragments | Preserve source texture alpha while applying intensity only to RGB |
| Environment vertex `vec4(...)` and direction-transform `vec4(...,0.0)` | Position/direction mathematics, not framebuffer color fallbacks |

No projection shader contains an automatic black-opaque fallback. Invisible RGB is not rewritten
through a new premultiplication or unpremultiplication stage.

## Outputs

- Syphon and Spout publish the completed `PGraphicsOpenGL` texture directly.
- NDI reads Processing ARGB and writes packed RGBA, including the original alpha byte.
- No output backend inserts a background or alpha replacement.
- Receiver/compositor alpha support is external and must be recorded independently.

## Physical qualification handoff

### Local OpenGL probe executed

On 2026-08-24, a temporary Processing 4.5.6 probe ran on macOS 26.5.2 arm64 and read alpha back
from the actual preview and high-resolution `PGraphicsOpenGL` targets. All 28 combinations passed:

- Standard, Domemaster, Equirectangular and Skybox with Environment absent: every alpha byte zero;
- the same four views with an Environment source configured but disabled: every alpha byte zero;
- the same four views with a uniform source alpha of 128: covered pixels remained exactly 128;
- Domemaster Size 100 and 50: corners/outside-circle alpha zero and covered pixels alpha 128;
- Domemaster Size 0: every alpha byte zero;
- the complete matrix was repeated independently for preview and high-resolution final targets.

The probe used the experimental output-render demand and did not enable a transport. It therefore
qualifies local internal framebuffer behavior only, not NDI, Syphon or Spout receiver behavior.

The executable matrix is maintained in the bilingual 2.0 release-readiness procedure. It covers
Standard, Domemaster, Equirectangular and Skybox with a background-free Scene, Environment absent,
Environment disabled, Environment enabled, preview and final/output targets. Domemaster adds
`Size%=100`, `50` and `0`; receiver checks cover applicable NDI, Syphon and Spout configurations.

Do not mark the existing release-evidence ledger as passing this new contract until that matrix is
rerun on the claimed physical platforms and receivers against the exact candidate revision.
