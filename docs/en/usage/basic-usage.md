# Basic Usage

## FULL Mode

`FULL` is the compatibility mode and default:

```java
dome.setRenderMode(RenderMode.FULL);
dome.setCurrentView(zividomelive.ViewType.STANDARD);
dome.getOutputManager().setNdiView(
    zividomelive.ViewType.EQUIRECTANGULAR);
```

The window may show Standard while NDI publishes equirectangular and the local texture backend publishes domemaster.

## Dedicated Mode

```java
dome.setRenderMode(RenderMode.DOMEMASTER);
```

A dedicated mode overrides the effective preview and output representation. Configured `ViewType` routes remain stored and return when `FULL` is selected again.

## Floating Domemaster

```java
dome.setRenderMode(RenderMode.STANDARD);
dome.setShowPreview(true);
```

This intentionally renders Standard plus the spherical chain needed by the auxiliary fisheye preview.

## Output Resolution

```java
dome.resetGraphics(2048);
```

Valid UI presets are 1024, 2048, 3072, and 4096. Reallocation is deferred to the draw loop and affects output targets only.

## Spherical Parameters

```java
dome.setFov(210);
dome.setFishSize(100);
dome.setPitch(0);
dome.setYaw(0);
dome.setRoll(0);
```

These parameters are shared by domemaster, equirectangular, and cubemap rendering. Do not use them as a substitute for a scene-space camera.
