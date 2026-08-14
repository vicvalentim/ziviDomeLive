# Lifecycle de Runtime

A fachada `ziviDomeLive` controla registro de hooks Processing, inicialização de managers, update de cenas, recursos dos renderers, encaminhamento de entrada, pause/resume e descarte terminal. Seu hook `draw()` delega a ordem de cada frame ao `RenderPipeline` interno, que usa o backend existente sem mudar ownership dos targets.

## Estados de Inicialização

| `InitState` | Significado |
|---|---|
| `NOT_INITIALIZED` | A instância existe; `setup()` ainda não terminou |
| `SETUP_COMPLETE` | Serviços básicos existem; managers aguardam contexto OpenGL válido após setup |
| `MANAGERS_READY` | Câmera, renderers, backend local e controles estão prontos |
| `READY` | Valor do enum 1.x reservado para expansão futura de lifecycle |

Sequência típica:

```text
construtor
  -> registra hooks pre/draw/post/input/dispose
setup()
  -> frame rate, diagnóstico OpenGL, hints, OutputManager, splash, cena bootstrap
primeiro post()
  -> CameraManager, renderers de output/preview, preparação Syphon/Spout, ControlManager
  -> MANAGERS_READY
```

`initializeManagers()` é público por compatibilidade 1.x, mas sketches comuns dependem do hook `post()` registrado. Chamadas duplicadas de `setup()` são ignoradas.

## Hooks Processing

| Hook | Responsabilidade |
|---|---|
| `pre()` | Chamar `Scene.update()`, avançar o `OrbitCamera` compartilhado e sincronizar sua orientação no Environment |
| `draw()` | Delegar a ordem do frame ao `RenderPipeline` e depois tratar o splash |
| `post()` | Inicializar managers uma vez depois do setup Processing |
| `keyEvent()` | Atalhos globais e encaminhamento à cena ativa |
| `mouseEvent()` | Encaminhar à cena ativa e rotear navegação para a câmera da cena ou para a câmera Standard |
| `controlEvent()` | Tratamento do painel e depois cena ativa |
| `dispose()` / `stop()` | Limpeza terminal |

Não chame nem encaminhe esses hooks manualmente pelo sketch.

## Ownership de Cena

`SceneManager` é a autoridade da cena ativa:

- a primeira cena registrada é ativada e recebe `setupScene()`;
- ativar outra cena descarta a anterior antes de configurar a nova;
- selecionar novamente a cena ativa é no-op;
- cenas inativas nunca ativadas não entraram no ownership setup/dispose;
- `clearScenes()` descarta a ativa e limpa registros;
- trocar o manager preserva uma instância ativa transferida e, nos demais casos, encerra o ownership antigo.

Instâncias de `StandardRenderer` são sincronizadas com a cena atual depois de mudanças de ownership.

## Pause e Resume

`pause()` registra quais outputs publicavam, encerra serviços de output e bloqueia update/render. `resume()` reinicializa managers quando necessário e tenta restaurar as publicações antes habilitadas.

A restauração de backend pode falhar independentemente. Consulte `OutputState` e `getOutputFailureReason()` em vez de presumir restart nativo bem-sucedido.

## Descarte Terminal

`dispose()` é idempotente e terminal. Ele:

1. marca a fachada como descartada;
2. libera splash e recursos ControlP5;
3. encerra NDI, Syphon e Spout;
4. descarta targets de preview e output;
5. limpa ownership de cenas;
6. descarta estado de câmera;
7. encerra o `ThreadManager` compartilhado;
8. remove callbacks Processing.

Depois do descarte, setup, troca de cena, renderização e inicialização de managers são ignorados.

## Fronteiras de Thread

- Trabalho Processing e OpenGL permanece na thread Processing.
- `Scene.sceneRender()` não deve criar outro draw lifecycle ao redor do target recebido.
- Conversão CPU e envio NDI usam worker dedicado com shutdown limitado.
- Tarefas internas devem usar `ThreadManager`; exemplos podem controlar executors somente quando também controlam e liberam seu lifecycle.

## Recuperação de Erros

Inicialização parcial de managers desfaz recursos alocados e retorna a `SETUP_COMPLETE` em vez de avançar para estado pronto. Uma tentativa válida posterior pode repetir.

Erros de publicação Syphon/Spout desabilitam a publicação sem destruir imediatamente o backend preparado. Falha de inicialização NDI marca o backend indisponível; outro pedido explícito de enable tenta novamente.

Consulte [Gerenciamento de Cenas](../usage/scene-management.md), [Manipulação de Eventos](../usage/event-handling.md) e [Integração Externa](../usage/external-integration.md).
