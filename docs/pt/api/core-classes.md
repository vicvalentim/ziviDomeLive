# Classes Principais

## zividomelive

Crie uma instância com o `PApplet` ativo e chame `setup()` uma vez:

```java
zividomelive dome = new zividomelive(this);
dome.setup();
```

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

## Renderers

As classes públicas de renderer da geração 1.x continuam disponíveis para compatibilidade: `StandardRenderer`, `CubemapRenderer`, `EquirectangularRenderer`, `FisheyeDomemaster` e `CubemapViewRenderer`.

Aplicações devem preferir a fachada e `RenderMode`. Ownership direto dos renderers é integração avançada da 1.x e pode não migrar sem mudanças para a 2.0.
