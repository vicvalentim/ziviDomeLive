# Operational Helpers

## Logging

```java
zividomelive.enableDebugLogging();
zividomelive.enableReleaseLogging();
zividomelive.setLogMode(LogManager.Mode.DEBUG);
```

Configure logging before constructing the facade when startup diagnostics are needed.

## Frame Rate

```java
dome.setTargetFrameRate(60);
```

The value must be positive. Changes after setup are applied to Processing and update the default NDI frame-rate metadata. Fractional NDI metadata is available through `OutputManager.setNdiFrameRate(numerator, denominator)`.

## Scene-Space Camera

```java
OrbitCamera camera = dome.getSceneCamera();
dome.setSceneCameraInputEnabled(true);
camera.setDistanceLimits(100, 5000);
camera.setCollapseGuard(20);
```

The scene camera transforms scene space and is distinct from spherical pitch/yaw/roll, the six-face `CameraManager`, and the Standard perspective camera.

## Output Diagnostics

```java
OutputManager outputs = dome.getOutputManager();
OutputManager.OutputState state =
    outputs.getOutputState(OutputManager.OutputType.NDI);
String reason = outputs.getOutputFailureReason(OutputManager.OutputType.NDI);
```

NDI telemetry methods report captured, sent, dropped, and failed frames. Dropped frames represent bounded latest-frame backpressure; failed frames represent capture or sender errors.

## Calibration Reset

`resetControls()` restores spherical pitch, yaw, roll, FOV, and Size% defaults and synchronizes the ControlP5 values.
