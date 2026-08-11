# Classes Principais

## zividomelive

Crie uma instância com o `PApplet` ativo e chame `setup()` uma vez:

```java
zividomelive dome = new zividomelive(this);
dome.setup();
```

O construtor registra imediatamente os hooks de lifecycle e input do
Processing. `setup()` cria os serviços de output e a cena inicial; os renderers
GPU são criados de forma tardia pelo hook `post()`, depois que o Processing
possui uma superfície OpenGL válida. `getInitState()` expõe os marcos
`NOT_INITIALIZED`, `SETUP_COMPLETE` e `MANAGERS_READY`; `READY` fica reservado.
Use `isInitialized()` para a consulta comum de render pronto. Pausa e descarte
são aspectos separados do lifecycle, não valores adicionais de `InitState`.

| Grupo | Métodos |
|---|---|
| Cena | `setScene()`, `setSceneManager()`, `getSceneManager()` |
| Renderização | `setRenderMode()`, `getRenderMode()`, `setCurrentView()` |
| Calibração | `setFov()`, `setFishSize()`, `setPitch()`, `setYaw()`, `setRoll()`, `resetOrientation()` |
| Preview | `setShowPreview()`, `setStandardOutputAspectMode()` |
| Output | `getOutputManager()`, `resetGraphics()`, `getOutputResolution()` |
| Câmera | `getSceneCamera()`, `setSceneCameraInputEnabled()` |
| Lifecycle | `pause()`, `resume()`, `dispose()` |

## SceneManager

`SceneManager` é a única autoridade da cena ativa. Ele rejeita registros nulos ou duplicados, ativa automaticamente a primeira cena e evita reinicializar uma cena já ativa.

```java
SceneManager scenes = new SceneManager();
scenes.registerScene(new SceneA());
scenes.registerScene(new SceneB());
scenes.nextScene();
```

A troca descarta a cena anterior e configura a nova. `clearScenes()` descarta a cena ativa e remove todos os registros.

| Operação | Comportamento |
|---|---|
| `registerScene(scene)` | Adiciona uma cena não nula e única; a primeira fica ativa |
| `activateScene(scene)` | Ativa uma cena registrada por identidade |
| `nextScene()` / `previousScene()` | Percorre a ordem de registro de forma circular |
| `setCurrentSceneIndex(index)` | Seleciona um índice válido baseado em zero |
| `containsScene()` / `getSceneCount()` | Consulta o estado do registro |
| `clearScenes()` | Descarta a cena ativa e limpa todos os registros |

## OutputManager

O manager separa rota configurada, disponibilidade, inicialização nativa, publicação e requisitos de renderização.

```java
OutputManager output = dome.getOutputManager();
output.setViewForOutput(
    OutputManager.OutputType.NDI,
    zividomelive.ViewType.EQUIRECTANGULAR);
output.toggleOutput("ndi");
```

Use `getOutputState()` e `getOutputFailureReason()` para diagnóstico. Use `isNdiEnabled()`, `isSyphonEnabled()` ou `isSpoutEnabled()` somente quando a pergunta for especificamente sobre publicação.

| Estado | Significado |
|---|---|
| `UNAVAILABLE` | Backend sem suporte ou cuja última inicialização falhou |
| `AVAILABLE` | Backend elegível, ainda sem recursos nativos |
| `INITIALIZED` | Recursos nativos existem; publicação desabilitada |
| `ENABLED` | Recursos nativos existem e frames são publicados |
| `STOPPING` | NDI sem publicação enquanto conclui limpeza limitada |

`setViewForOutput()` altera uma rota salva. Um `RenderMode` dedicado substitui a
rota efetiva sem apagar o valor; `FULL` o restaura. Syphon e Spout recebem
diretamente o `PGraphicsOpenGL` selecionado. NDI faz readback dos pixels na
render thread e envia por um pipeline worker limitado a três slots.

## SphericalOrientation

`SphericalOrientation` controla a atitude compartilhada por todas as projeções
esféricas. Seus setters recebem valores cíclicos, calculam o menor delta e o
compõem nos eixos locais pitch `X`, yaw `Z` ou roll `Y`. O quaternion armazenado
é normalizado depois da composição.

`getPitch()`, `getYaw()` e `getRoll()` retornam os acumuladores de controle, não
uma conversão Euler de `getQuaternion()`. A ordem dos comandos é significativa.
`reset()` restaura identidade e acumuladores zerados.

Aplicações normalmente usam esse comportamento pelos métodos de calibração da
fachada em vez de criar outra orientação.

## OrbitCamera

`OrbitCamera` é uma transformação opcional em scene space, compartilhada por
todos os targets para que Standard e vistas esféricas observem a mesma atitude.

Configure limites de distância, collapse guard, interpolação, sensibilidade do
drag e passos da roda pelos setters. `setTarget()`, `setDistance()`,
`setOrientation()`, `snapTo()` e `reset()` alteram o estado desejado. Em geral,
obtenha a instância compartilhada com `getSceneCamera()` e deixe a fachada
encaminhar o mouse apenas enquanto `setSceneCameraInputEnabled(true)` estiver
ativo.

## Renderers

As classes públicas de renderer da geração 1.x continuam disponíveis para compatibilidade: `StandardRenderer`, `CubemapRenderer`, `EquirectangularRenderer`, `FisheyeDomemaster` e `CubemapViewRenderer`.

Aplicações devem preferir a fachada e `RenderMode`. Ownership direto dos renderers é integração avançada da 1.x e pode não migrar sem mudanças para a 2.0.

Não retenha um target de renderer depois de `resetGraphics()`: a mudança de
resolução é adiada para o render loop e pode substituir as instâncias de alta
resolução. Consulte a fachada novamente depois que o reset for aplicado.
