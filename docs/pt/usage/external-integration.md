# Integração Externa

Outputs externos começam desabilitados e possuem rotas independentes em `RenderMode.FULL`.

NDI é uma integração experimental, não oficial e somente de vídeo, que exige um
runtime instalado separadamente no sistema. Conclua a configuração do
[Runtime NDI](../installation/ndi.md) antes de habilitá-lo.

## Configure Rotas

```java
OutputManager output = dome.getOutputManager();
output.setNdiView(ViewType.EQUIRECTANGULAR);
output.setSyphonView(ViewType.DOMEMASTER);
output.setSpoutView(ViewType.STANDARD);
```

Somente o backend local válido para a plataforma fica disponível: Syphon no macOS ou Spout no Windows.

## Alterne a Publicação

```java
output.toggleOutput("ndi");
output.toggleOutput("syphon");
output.toggleOutput("spout");
```

Backends locais não suportados ignoram a solicitação. Um novo toggle desabilita um backend ativo. Depois de falha na inicialização, outra tentativa explícita de ativação executa recuperação.

## Consulte o Lifecycle

```java
OutputManager.OutputState state =
    output.getOutputState(OutputManager.OutputType.NDI);
String reason = output.getOutputFailureReason(OutputManager.OutputType.NDI);
```

Disponibilidade, inicialização, publicação e demanda de renderização são estados separados. Um backend inicializado não exige renderização até a publicação ser habilitada.

## Pipeline NDI

NDI é a fronteira GPU para CPU:

1. A draw thread chama `loadPixels()` depois que o target foi concluído.
2. Pixels ARGB são copiados para um dos três slots reutilizáveis.
3. Uma fila limitada latest-frame-wins controla latência.
4. Um worker dedicado converte para RGBA empacotado e envia frame progressivo.
5. O worker não executa chamadas OpenGL.

A metadata de frame rate segue `dome.getTargetFrameRate()`. Use `setNdiFrameRate()` para taxas fracionárias como `60000/1001`.

## Telemetria

```java
output.getNdiCapturedFrames();
output.getNdiSentFrames();
output.getNdiDroppedFrames();
output.getNdiFailedFrames();
```

Drops são eventos intencionais de backpressure. Failed frames identificam erros de captura ou envio.

## Shutdown e Resume

`pause()` registra publicações ativas, encerra outputs e `resume()` tenta restaurá-las. `dispose()` terminal libera recursos e remove callbacks. A espera pelo worker NDI possui limite; um envio nativo bloqueado leva o backend a `STOPPING` e adia a limpeza até a saída do worker.

A interoperabilidade com receivers nativos continua sendo item de qualificação em hardware.
