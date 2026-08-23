---
title: Classes Principais
icon: material/cube-outline
status: stable
tags:
  - API
---

# Classes Principais

## ziviDomeLive

Crie a facade com o `PApplet` ativo, chame `setup()` uma vez e registre cenas por ela. O construtor registra hooks do Processing; a criação dos renderers permanece lazy até existir uma superfície OpenGL válida.

```java
ziviDomeLive dome = new ziviDomeLive(this);
dome.setup();
dome.setScene(new MainScene());
```

| Tema | Controles públicos |
|---|---|
| Cenas | `setScene`, `setCurrentScene`, `registerScene`, `setSceneManager`, `getSceneManager` |
| Representação | `setRenderMode`, `getRenderMode`, `setCurrentView`, `getCurrentView` |
| Calibração | `setFov`, `setFishSize`, `setPitch`, `setYaw`, `setRoll`, `resetOrientation`, `resetControls` |
| Preview | `setShowPreview`, `setStandardOutputAspectMode` |
| Resolução/output | `resetGraphics`, `getOutputResolution`, `getOutputManager` |
| Câmera | `getSceneCamera`, `setSceneCameraInputEnabled` |
| Environment | `setEquirectangularBackground`, controles de visibilidade/intensidade/yaw, `clearEnvironmentBackground` |
| Logging | `setLogMode`, `enableDebugLogging`, `enableReleaseLogging` |
| Profiling experimental | controles de enable/disable/snapshot/capabilities |

`isInitialized()` é a consulta artist-facing de prontidão. Internals de inicialização e getters de renderer não são API pública 2.0.

## Scene

`Scene` protege o modelo de programação Processing. Apenas `sceneRender(PGraphicsOpenGL)` é abstrato; todos os métodos de lifecycle/input são defaults. Consulte o [contrato Scene completo](scene-interface.md).

## SceneManager

`SceneManager` é a autoridade da cena ativa. Registro e ativação usam **identidade do objeto**, não `equals()`.

| Método | Contrato |
|---|---|
| `registerScene(scene)` | Registra uma instância única; o primeiro registro a ativa |
| `activateScene(scene)` | Ativa uma instância registrada |
| `nextScene()` / `previousScene()` | Percorre circularmente a ordem de registro |
| `setCurrentSceneIndex(index)` | Seleciona índice zero-based válido |
| `reloadCurrentScene()` | Descarte/reativação completa com serviços novos |
| `clearScenes()` | Descarta a ativação corrente e remove todos os registros |

Prefira `setScene`/`registerScene` da facade. Um manager substituto é anexado ao lifecycle antes do primeiro setup, garantindo que `configure()` preceda `setupScene()`.

## RenderMode e ViewType

`RenderMode` é política do runtime; `ViewType` é representação final. `FULL` preserva rotas independentes. As ordens exatas dos enums estão congeladas e testadas.

## LogMode

`DEBUG` permite logging diagnóstico no console/arquivo; `RELEASE` suprime chatter de debug. Exemplos oficiais não habilitam debug para manter o console comum silencioso.

## Stable não expõe o grafo de render

Não existe `CubemapRenderer`, `CubemapTarget`, `FrameViews`, `ProcessingGlAdapter`, manager ControlP5 ou worker/executor público em 2.0. A remoção é intencional e impede que callers assumam responsabilidades de contexto gráfico e ownership.
