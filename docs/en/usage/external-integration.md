# External Integration

External outputs are disabled by default and route independently in `RenderMode.FULL`.

NDI is an experimental, unofficial video-only integration and requires a
separately installed system runtime. Complete the [NDI Runtime](../installation/ndi.md)
setup before enabling it.

## Configure Routes

```java
OutputManager output = dome.getOutputManager();
output.setNdiView(ViewType.EQUIRECTANGULAR);
output.setSyphonView(ViewType.DOMEMASTER);
output.setSpoutView(ViewType.STANDARD);
```

Only the platform-valid local texture backend is available: Syphon on macOS or Spout on Windows.

## Toggle Publication

```java
output.toggleOutput("ndi");
output.toggleOutput("syphon");
output.toggleOutput("spout");
```

Unsupported local backends ignore the request. A repeated toggle disables an enabled backend. After an initialization failure, another explicit enable toggle retries initialization.

## Inspect Lifecycle

```java
OutputManager.OutputState state =
    output.getOutputState(OutputManager.OutputType.NDI);
String reason = output.getOutputFailureReason(OutputManager.OutputType.NDI);
```

Availability, initialization, publication, and render demand are separate. An initialized backend does not require rendering until publication is enabled.

## NDI Pipeline

NDI is the GPU-to-CPU boundary:

1. The Processing draw thread calls `loadPixels()` after target rendering completes.
2. ARGB pixels are copied into one of three reusable slots.
3. A bounded latest-frame-wins queue limits latency.
4. A dedicated worker converts to packed RGBA and sends a progressive frame.
5. The worker performs no OpenGL calls.

The default frame-rate metadata follows `dome.getTargetFrameRate()`. Use `setNdiFrameRate()` for fractional rates such as `60000/1001`.

## Telemetry

```java
output.getNdiCapturedFrames();
output.getNdiSentFrames();
output.getNdiDroppedFrames();
output.getNdiFailedFrames();
```

Dropped frames are intentional backpressure events. Failed frames identify capture or sender errors.

## Shutdown and Resume

`pause()` records enabled publications, stops outputs, and `resume()` attempts to restore them. Terminal `dispose()` releases resources and unregisters callbacks. NDI worker waiting is bounded; a blocked native send moves the backend to `STOPPING` and defers cleanup until the worker exits.

Native receiver interoperability remains a hardware qualification item.
