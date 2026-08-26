# Core architecture

```text
                 ziviDomeLive Core
                        ^
            +-----------+-----------+
            |                       |
    Processing Library         future Engine
            |                       |
      PGL / JOGL                   LWJGL
```

Dependencies point upward toward Core. Core never imports a host, facade, renderer, backend,
window, or output integration.

| Concept | Core owns? | Processing owns? | Future Engine owns? |
|---|---|---|---|
| Frame timing and fixed-step policy | State and algorithms | Tick at `pre()` | Tick at engine frame boundary |
| Quaternion and spherical orientation | Yes | Matrix/vector conversion and controls | Backend conversion and controls |
| Orbit camera | Pose, goals, guard, interpolation | Mouse events and graphics transform | GLFW input and backend transform |
| Rendering and render targets | No | PGraphics/JOGL pipeline | LWJGL backend |
| Named actions | Registry | Processing event bindings | Engine input bindings |
| Background tasks | Bounded keyed work and frame callbacks | Activation ownership | Activation ownership |
| Ports | Generic adapter lifecycle and bounded input | Optional protocol adapters | Optional protocol adapters |
| Environment | Visual scalars and orientation | PImage and renderer resource | Backend image/texture resource |
| Assets | Generic owned/borrowed cache only | Images/shaders/shapes | Backend resources |
| Scene contract | No | `Scene` and `PGraphicsOpenGL` | A future, separately designed contract |
| Output transports | No | NDI/Syphon/Spout | Host-specific transports |

The host owns frame cadence and activation ordering. The Core classes are deliberately unaware of
render calls, so update-once/render-many is preserved by construction.
