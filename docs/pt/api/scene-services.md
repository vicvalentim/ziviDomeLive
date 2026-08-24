---
title: Scene Services
icon: material/layers-triple-outline
status: advanced
tags:
  - API
  - Lifecycle
  - Concorrência
---

# Scene Services

`SceneServices` é um conjunto de capacidades **Advanced Stable** pertencente a uma ativação. `ziviDomeLive` o cria, avança e fecha, fornecendo-o antes de cada `setupScene()`.

```java
class ServiceScene implements Scene {
  SceneServices services;

  public void configure(SceneServices services) {
    this.services = services;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    services.camera().apply(pg);
    // desenho
  }
}
```

## Mapa de serviços

| Accessor | Foco | A cena controla | O runtime controla |
|---|---|---|---|
| `applet()` | Host Processing | Uso comum do applet na thread correta | Lifetime do applet |
| `frameClock()` | Tempo de frame | Delta máximo aceito | Tick e índice monotônico |
| `timeline()` | Simulação | Rate, position, fixed step, catch-up, pause | Nenhuma política de step da cena |
| `tasks()` | CPU/I/O em background | Submissão nomeada limitada, callbacks de resultado/erro | Executor compartilhado, cancelamento, publicação na fronteira do frame |
| `assets()` | Imagens/shaders/shapes | Requests e shapes retidos | Criação na render thread e shutdown do cache |
| `actions()` | Input nomeado | Bind, trigger, unregister | Ordem de dispatch e limpeza da ativação |
| `camera()` | Navegação scene-space e iluminação de vista opt-in | Pose, input, tracking, chamada do rig | Update uma vez por frame e reset de âncoras |
| `environment()` | Overrides de background | Imagem, visibilidade, intensidade, yaw de longitude, orientação fixa da fonte | Restauração condicional ao desativar |
| `ports()` | Adapters opcionais | Conectar ports bounded de input/output | Limite de drain, telemetria e fechamento |
| `requestReload()` | Lifecycle | Solicitar reload | Executar em fronteira segura de frame |

## Restrições de ownership

Cenas não podem construir nem fechar serviços fornecidos pelo runtime. `parent`, `scene`, filas raw de render, dispose hooks arbitrários e métodos `close()` dos serviços estão ausentes intencionalmente.

Somente `SceneInputPort` e `SceneOutputPort` estendem `AutoCloseable`; isso é SPI para providers de adapter. `ScenePorts` ainda retém o ownership da ativação e fecha adapters conectados.

## Contrato de task em background

`SceneTaskGroup.submitIfIdle(key, ...)` é limitado e baseado em callbacks. Não retorna `Future`, não expõe executor e não aceita trabalho ilimitado.

```java
services.tasks().submitIfIdle(
    "mesh",
    () -> buildCpuOnlyMeshData(),
    data -> publishOnFrameBoundary(data),
    error -> report(error));
```

- callable/runnable não pode chamar Processing/OpenGL;
- consumers de resultado e erro rodam somente para a ativação que submeteu;
- trabalho antigo de ativação descartada não publica numa ativação posterior da mesma instância `Scene`;
- `getInFlightCount()` e `getMaxInFlight()` expõem telemetria bounded.

## Contrato de ports

`connectInput(port, consumer)` recebe dados de thread externa numa fila bounded da ativação. O runtime entrega quantidade limitada numa fronteira de frame. `getPendingInputCount()` e `getDroppedInputCount()` tornam backpressure observável.

`connectOutput(port)` retém o provider e seu contrato não bloqueante `offer(value)`. Adapters MIDI, OSC ou device reais continuam opcionais e fora do core.

## Restauração de Environment

O serviço restaura somente valores que alterou e apenas enquanto o estado da facade ainda corresponde ao valor aplicado. Limpeza antiga nunca sobrescreve um owner posterior.

## Iluminação sincronizada à câmera

`camera().applyWithViewLighting(pg)` é uma alternativa explícita a `camera().apply(pg)`. Ela aplica
a transformação orbital corrente, limpa as luzes do target e instala uma luz ambiente neutra mais
um spotlight na posição da câmera de cena, apontado para seu target corrente. Luzes adicionais
podem ser aplicadas depois.

A chamada apenas lê a pose já atualizada da câmera. Ela não avança interpolação, portanto todas as
faces do cubemap no mesmo frame do Processing observam posição e direção idênticas. A iluminação
nunca é habilitada automaticamente; use `apply(pg)` quando a cena possui outro modelo de luz ou
pipeline de shader.

Cenas comuns podem permanecer apenas com o wildcard do pacote raiz: `setDistanceLimits`,
`setCollapseGuard`, `setLerpFactor`, `setDragSensitivity` e `snapToAxisAngle` encaminham as
operações orbitais usuais. O acesso avançado `orbit()` continua disponível quando o código usa
intencionalmente `render.camera.OrbitCamera` ou `render.Quaternion` de forma direta.

## Reload

`requestReload()` adia o pedido. A próxima fronteira segura executa stop-work → `dispose()` → liberação dos serviços → serviços novos → `configure()` → `setupScene()`.
