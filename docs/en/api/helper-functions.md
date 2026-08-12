# Operational Helpers

## Logging

```java
ziviDomeLive.enableDebugLogging();
ziviDomeLive.enableReleaseLogging();
ziviDomeLive.setLogMode(LogManager.Mode.DEBUG);
```

Configure logging before constructing the facade when startup diagnostics are needed.

## Frame Rate

```java
dome.setTargetFrameRate(60);
```

The value must be positive. Changes after setup are applied to Processing and update the default NDI frame-rate metadata. Fractional NDI metadata is available through `OutputManager.setNdiFrameRate(numerator, denominator)`.

Set the target before `setup()` when possible. Changing Processing's frame rate
from a scene's initialization path can force JOGL to restart its animator on the
animator thread; use elapsed time or frame quantization for scene-local playback
profiles instead.

## Scene-Space Camera

```java
OrbitCamera camera = dome.getSceneCamera();
dome.setSceneCameraInputEnabled(true);
camera.setDistanceLimits(100, 5000);
camera.setCollapseGuard(20);
```

The scene camera transforms scene space and is distinct from spherical pitch/yaw/roll, the canonical six-face `CubemapFace` table, and the Standard perspective camera.

Disable camera input when the owning scene is disposed so later scenes do not
inherit drag or wheel interaction unintentionally.

## Environment Background

```java
PImage stars = loadImage("textures/8k_stars_milky_way.jpg");
dome.setEquirectangularBackground(stars);
dome.setEnvironmentBackgroundIntensity(1.0f);
dome.setEnvironmentBackgroundYawOffset(0.0f);
```

The environment background is an LDR `PImage` service owned by the library. It is drawn behind each native cubemap face before `sceneRender()`, so domemaster, equirectangular, and skybox projections receive the same background without requiring a scene-owned sky sphere. Use `clearEnvironmentBackground()` when the owning scene is disposed.

HDR loading, IBL maps, and ambient occlusion are not enabled by this helper yet.

## Output Diagnostics

```java
OutputManager outputs = dome.getOutputManager();
OutputManager.OutputState state =
    outputs.getOutputState(OutputManager.OutputType.NDI);
String reason = outputs.getOutputFailureReason(OutputManager.OutputType.NDI);
```

NDI telemetry methods report captured, sent, dropped, and failed frames. Dropped frames represent bounded latest-frame backpressure; failed frames represent capture or sender errors.

```java
long captured = outputs.getNdiCapturedFrames();
long sent = outputs.getNdiSentFrames();
long dropped = outputs.getNdiDroppedFrames();
long failed = outputs.getNdiFailedFrames();
```

`getLocalTextureBackendName()`, `isLocalTextureAvailable()`, and
`isLocalTextureInitialized()` describe the one platform-local texture backend.
Do not infer availability from the operating-system name alone.

## Calibration Reset

`resetControls()` restores spherical pitch, yaw, roll, FOV, and Size% defaults and synchronizes the ControlP5 values.

`resetOrientation()` resets only pitch, yaw, roll, and the shared quaternion.
Use it before replaying an ordered calibration sequence. Programmatic FOV and
Size% callers should remain within the supported panel ranges `0..360` and
`0..100`.

## Resolution And Dimensions

```java
dome.resetGraphics(2048);
int outputSize = dome.getOutputResolution();
int previewWidth = dome.getWidth();
int previewHeight = dome.getHeight();
```

`resetGraphics()` queues a high-resolution renderer reset; allocation occurs in
the draw loop. `getWidth()` and `getHeight()` report the Processing window, not
the spherical output size. Standard output aspect policy is configured through
`setStandardOutputAspectMode()`.

## OpenGL Diagnostics

`printOpenGLInfo(PApplet)` logs vendor, renderer, version, and GLSL information
from a valid OpenGL context. The packaged projection shaders target GLSL 4.10,
so the production context must support OpenGL 4.1.
