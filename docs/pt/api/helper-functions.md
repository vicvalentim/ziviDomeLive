# Funções Operacionais

## Logging

```java
ziviDomeLive.enableDebugLogging();
ziviDomeLive.enableReleaseLogging();
ziviDomeLive.setLogMode(LogManager.Mode.DEBUG);
```

Configure o logging antes de construir a fachada quando precisar de diagnóstico de startup.

## Frame Rate

```java
dome.setTargetFrameRate(60);
```

O valor deve ser positivo. Mudanças após `setup()` são aplicadas ao Processing e atualizam a metadata padrão de frame rate do NDI. Taxas fracionárias estão disponíveis por `OutputManager.setNdiFrameRate(numerator, denominator)`.

Defina o alvo antes de `setup()` quando possível. Alterar a taxa do Processing
durante a inicialização de uma cena pode fazer o JOGL reiniciar o animator na
própria thread; para perfis locais de playback, use tempo decorrido ou
quantização de frames.

## Câmera em Scene Space

```java
OrbitCamera camera = dome.getSceneCamera();
dome.setSceneCameraInputEnabled(true);
camera.setDistanceLimits(100, 5000);
camera.setCollapseGuard(20);
```

A câmera da cena transforma o scene space e é distinta de pitch/yaw/roll esféricos, da tabela canônica `CubemapFace` de seis faces e da câmera perspectiva Standard.

Desabilite o input da câmera no descarte da cena proprietária para que cenas
seguintes não herdem drag ou roda do mouse sem intenção.

## Diagnóstico de Output

```java
OutputManager outputs = dome.getOutputManager();
OutputManager.OutputState state =
    outputs.getOutputState(OutputManager.OutputType.NDI);
String reason = outputs.getOutputFailureReason(OutputManager.OutputType.NDI);
```

A telemetria NDI informa frames captured, sent, dropped e failed. Drops representam backpressure latest-frame; falhas representam erros de captura ou envio.

```java
long captured = outputs.getNdiCapturedFrames();
long sent = outputs.getNdiSentFrames();
long dropped = outputs.getNdiDroppedFrames();
long failed = outputs.getNdiFailedFrames();
```

`getLocalTextureBackendName()`, `isLocalTextureAvailable()` e
`isLocalTextureInitialized()` descrevem o único backend local de textura da
plataforma. Não deduza disponibilidade apenas pelo nome do sistema operacional.

## Reset de Calibração

`resetControls()` restaura os padrões esféricos de pitch, yaw, roll, FOV e Size%, sincronizando os valores do ControlP5.

`resetOrientation()` restaura apenas pitch, yaw, roll e o quaternion
compartilhado. Use-o antes de repetir uma sequência ordenada de calibração.
Chamadores programáticos de FOV e Size% devem permanecer nas faixas suportadas
do painel, `0..360` e `0..100`.

## Resolução E Dimensões

```java
dome.resetGraphics(2048);
int outputSize = dome.getOutputResolution();
int previewWidth = dome.getWidth();
int previewHeight = dome.getHeight();
```

`resetGraphics()` agenda o reset dos renderers de alta resolução; a alocação
acontece no draw loop. `getWidth()` e `getHeight()` informam a janela Processing,
não o tamanho do output esférico. Configure o aspect ratio Standard com
`setStandardOutputAspectMode()`.

## Diagnóstico OpenGL

`printOpenGLInfo(PApplet)` registra vendor, renderer, versão e GLSL a partir de
um contexto OpenGL válido. Os shaders de projeção empacotados usam GLSL 4.10,
portanto o contexto de produção deve suportar OpenGL 4.1.
