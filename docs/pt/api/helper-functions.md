# Funções Operacionais

## Logging

```java
zividomelive.enableDebugLogging();
zividomelive.enableReleaseLogging();
zividomelive.setLogMode(LogManager.Mode.DEBUG);
```

Configure o logging antes de construir a fachada quando precisar de diagnóstico de startup.

## Frame Rate

```java
dome.setTargetFrameRate(60);
```

O valor deve ser positivo. Mudanças após `setup()` são aplicadas ao Processing e atualizam a metadata padrão de frame rate do NDI. Taxas fracionárias estão disponíveis por `OutputManager.setNdiFrameRate(numerator, denominator)`.

## Câmera em Scene Space

```java
OrbitCamera camera = dome.getSceneCamera();
dome.setSceneCameraInputEnabled(true);
camera.setDistanceLimits(100, 5000);
camera.setCollapseGuard(20);
```

A câmera da cena transforma o scene space e é distinta de pitch/yaw/roll esféricos, do `CameraManager` das seis faces e da câmera perspectiva Standard.

## Diagnóstico de Output

```java
OutputManager outputs = dome.getOutputManager();
OutputManager.OutputState state =
    outputs.getOutputState(OutputManager.OutputType.NDI);
String reason = outputs.getOutputFailureReason(OutputManager.OutputType.NDI);
```

A telemetria NDI informa frames captured, sent, dropped e failed. Drops representam backpressure latest-frame; falhas representam erros de captura ou envio.

## Reset de Calibração

`resetControls()` restaura os padrões esféricos de pitch, yaw, roll, FOV e Size%, sincronizando os valores do ControlP5.
