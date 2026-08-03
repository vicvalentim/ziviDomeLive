# GitHub Copilot – Instruções Persistentes para ziviDomeLive

Este arquivo fornece contexto arquitetural permanente para agentes Copilot que trabalham neste repositório.
Leia-o **antes** de qualquer edição de código.

---

## 1. Visão Geral da Arquitetura

**ziviDomeLive** é uma biblioteca Processing 4 (Java 17) para renderização fulldome e VR em tempo real.

### Pipeline de renderização por frame

```
pre()  → currentScene.update()
draw() → renderContent()
          ├─ handleGraphicsReset()          (resolução pendente)
          ├─ ensurePreviewRenderers()
          ├─ [se output ativo] captureCubemap() → updateRenderViews() → outputManager.sendOutput()
          │  └─ updatePreviewRenderViews(cubemapRenderer.getCubemapFaces())
          ├─ [se output inativo] capturePreviewCubemap() → updatePreviewRenderViews(previewCubemapFaces)
          │  └─ [se STANDARD] standardRenderer.render()
          ├─ displayPreviewCurrentView()
          ├─ [se showPreview] drawFloatingPreview()
          └─ drawControlPanel()
```

### Invariantes críticos

- **`sceneRender(PGraphicsOpenGL pg)`** é chamado com o contexto de draw JÁ ABERTO.
  A cena **nunca** deve chamar `pg.beginDraw()` / `pg.endDraw()`.
- **`ViewType` enum** — a ordem dos valores é mapeada por índice nos dropdowns do ControlP5.
  **Não reordenar**: `FISHEYE_DOMEMASTER(0), EQUIRECTANGULAR(1), CUBEMAP(2), STANDARD(3)`.
- **Dois pipelines paralelos**: `outputResolution` (alta resolução, para NDI/Syphon/Spout) e
  `previewResolution` (janela, para exibição em tela). Quando nenhum output está ativo,
  apenas o pipeline de preview é executado.
- **`LogManager.getLogger()`** — use sempre este método; não crie `Logger` ad-hoc.
- **`ThreadManager.getExecutor()`** — único pool de threads da biblioteca.

---

## 2. Bug OpenGL Error 1282 — Diagnóstico e Solução

### O que é o erro 1282

`GL_INVALID_OPERATION` (código 1282) é emitido pelo driver OpenGL quando uma operação
é invocada em estado inválido para o contexto corrente. No contexto Processing/JOGL ele
tipicamente indica:

- **`beginDraw()` chamado em `PGraphicsOpenGL` que já está em modo de draw** (aninhamento proibido).
- **`glReadPixels` ou `glBindTexture` executado fora de um framebuffer válido** ou antes de
  `endDraw()` completar o flush do framebuffer.
- **Operação de shader invocada fora de um draw context ativo.**

### Localização do bug principal: `OutputManager.createNDIFrame()`

```java
// OutputManager.java — linhas 311-353 (PROBLEMÁTICO)
pg.beginDraw();          // ← NESTED beginDraw: pg já foi desenhado e endDraw() foi chamado
PGL pgl = pg.beginPGL(); // ← Abre contexto PGL DENTRO de beginDraw que é inválido aqui
// ...
pgl.bindTexture(PGL.TEXTURE_2D, textureID);  // ← bindTexture + readPixels não é leitura de framebuffer
pgl.readPixels(0, 0, width, height, PGL.RGBA, PGL.UNSIGNED_BYTE, 0); // ← 1282 aqui
```

**Causa raiz**: O método chama `pg.beginDraw()` sobre um buffer que **acabou de ser fechado**
por `endDraw()` em `FisheyeDomemaster.applyShader()` ou `EquirectangularRenderer.render()`.
Além disso, `glReadPixels` com PBO ativo lê do framebuffer atual — não de uma texture binding —
portanto o `glBindTexture` antes do `readPixels` é semanticamente incorreto.

### Solução correta para leitura PBO

A abordagem certa é ler do **framebuffer** associado ao `PGraphicsOpenGL` **sem reabrir** o
contexto de draw. O fluxo deve ser:

```
1. O buffer PG já está fechado (endDraw() ocorreu) → textura está pronta na GPU.
2. Abrir apenas beginPGL() (sem beginDraw()) para acessar o contexto GL baixo-nível.
3. Fazer bind do FBO do PG (não da textura) e ler pixels com glReadPixels do framebuffer.
4. OU: usar pg.loadPixels() + pg.pixels[] (mais simples, sem PBO, aceitável para NDI).
5. Fechar endPGL() sem endDraw().
```

**Implementação recomendada (opção simples, sem PBO):**

```java
private synchronized DevolayVideoFrame createNDIFrame(PGraphicsOpenGL pg) {
    if (pg == null || reusableFrame == null) return null;
    int width = pg.width;
    int height = pg.height;
    if (width <= 0 || height <= 0) return null;

    // Não chamar beginDraw() — pg já foi renderizado.
    pg.loadPixels();
    int[] pixels = pg.pixels;
    if (pixels == null || pixels.length != width * height) return null;

    int byteCount = width * height * 4;
    if (ndiBuffer == null || ndiBuffer.capacity() != byteCount) {
        ndiBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN);
    }
    ndiBuffer.clear();
    for (int px : pixels) {
        ndiBuffer.put((byte) ((px >> 16) & 0xFF)); // R
        ndiBuffer.put((byte) ((px >> 8)  & 0xFF)); // G
        ndiBuffer.put((byte) ( px        & 0xFF)); // B
        ndiBuffer.put((byte) ((px >> 24) & 0xFF)); // A
    }
    ndiBuffer.flip();

    reusableFrame.setResolution(width, height);
    reusableFrame.setData(ndiBuffer);
    reusableFrame.setFourCCType(DevolayFrameFourCCType.RGBA);
    reusableFrame.setLineStride(width * 4);
    reusableFrame.setFormatType(DevolayFrameFormatType.INTERLEAVED);
    reusableFrame.setFrameRate(150, 1);
    return reusableFrame;
}
```

**Implementação recomendada (opção PBO correta, sem beginDraw):**

```java
// Usar o FBO do PGraphicsOpenGL, não bindTexture
pg.beginPGL();  // SEM pg.beginDraw()
pgl.bindFramebuffer(PGL.FRAMEBUFFER, pg.getFboID());
pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, pboId);
pgl.readPixels(0, 0, width, height, PGL.RGBA, PGL.UNSIGNED_BYTE, 0);
// ... map, copy, unmap ...
pgl.bindBuffer(PGL.PIXEL_PACK_BUFFER, 0);
pgl.bindFramebuffer(PGL.FRAMEBUFFER, 0);
pg.endPGL();
```

### Outros pontos que geram 1282

| Local | Causa | Correção |
|---|---|---|
| `CubemapRenderer.captureCubemap()` | `configureCameraForFace()` chama `pg.applyMatrix()` que pode coincidir com operações de textura em andamento de outro face | Garantir que cada `beginDraw/endDraw` de face cubemap seja atômico (já é o caso, verificar em plataformas ARM) |
| `EquirectangularRenderer.render()` | `equirectangularShader.set("posX", faces[0])` pode falhar se `faces[0]` ainda não tiver textura inicializada | Adicionar null-check na textura de cada face antes de passar ao shader |
| `drawControlPanel()` no `zividomelive` | `p.hint(DISABLE_DEPTH_TEST)` / `p.hint(ENABLE_DEPTH_TEST)` invocados fora de um contexto de draw na janela principal | Verificar que esses hints só são chamados dentro do fluxo `draw()` do PApplet |

---

## 3. Melhorias Pendentes para v1.5

### 3.1 `Scene.dispose()` nunca é chamado (bug de vazamento de recursos)

**Arquivo**: `SceneManager.java`

`nextScene()`, `previousScene()` e `setCurrentSceneIndex()` trocam a cena atual sem chamar
`dispose()` na cena que está sendo desativada. Recursos GPU (PShape, PImage, PGraphics) ficam
vazando.

**Correção**: antes de `newScene.setupScene()`, chamar `currentScene.dispose()` se
`previousIndex != currentSceneIndex`.

```java
// Em nextScene() / previousScene() / setCurrentSceneIndex()
Scene leaving = getCurrentScene();
currentSceneIndex = novoIndice;
if (leaving != null) {
    leaving.dispose();
}
Scene arriving = getCurrentScene();
arriving.setupScene();
```

### 3.2 Campo `enableOutput` morto

**Arquivo**: `zividomelive.java` linha 52

`private final boolean enableOutput = false;` — sempre `false`, nunca usado.
A lógica real é `isEnableOutput()` que delega ao `OutputManager`.
**Remover o campo.**

### 3.3 `resetGraphics()` submete tarefa inútil ao executor

**Arquivo**: `zividomelive.java` método `resetGraphics()`

O bloco `ThreadManager.getExecutor().submit(...)` apenas dorme 100ms e loga.
A lógica real de reset ocorre no próximo frame via `handleGraphicsReset()`.
**Remover o bloco submit; manter apenas o set de `pendingOutputReset`.**

### 3.4 Frame rate hardcoded em 70

**Arquivo**: `zividomelive.java` método `setup()`

`p.frameRate(70)` impõe limite ao usuário sem configuração.
**Adicionar `setTargetFrameRate(int fps)` (padrão 60) e usar o valor configurado.**

### 3.5 Métodos públicos `renderFisheyeDomemaster()` etc. são enganosos

**Arquivo**: `zividomelive.java` linhas 557–599

Todos chamam `renderWithCurrentView()` independentemente do nome.
- **Opção A**: remover todos os 4 métodos (API morta) e atualizar Javadoc.
- **Opção B**: corrigir cada um para renderizar efetivamente o modo correspondente,
  invocando `setCurrentView(ViewType.X)` antes de `renderWithCurrentView()`.

### 3.6 Paths de shader duplicados

**Arquivo**: `zividomelive.java` métodos `initializeRenderers()` e `initializePreviewRenderers()`

As 4 strings de shader se repetem. **Extrair para constantes estáticas:**

```java
private static final String EQUIRECT_VERT = "data/shaders/equirectangular.vert";
private static final String EQUIRECT_FRAG = "data/shaders/equirectangular.frag";
private static final String DOME_VERT     = "data/shaders/domemaster.vert";
private static final String DOME_FRAG     = "data/shaders/domemaster.frag";
```

### 3.7 Campos do `ControlManager` com visibilidade package-private

**Arquivo**: `ControlManager.java` linhas 17–31

`cp5`, `numberboxActive`, `baseResolution`, `parent` e todos os widgets devem ser `private`.

### 3.8 Alinhamento de `showControlPanel` com o estado do widget ControlP5

**Arquivo**: `zividomelive.java` método `keyEvent()`

Quando `h` alterna `showControlPanel`, o estado interno muda mas nenhum widget do ControlP5
reflete a mudança. Adicionar `controlManager.syncPanelVisibilityToggle(showControlPanel)` ou
equivalente para manter UI e estado sincronizados.

### 3.9 Pipeline de alta resolução alocado sem uso no modo sem output

**Arquivo**: `zividomelive.java` método `initializeRenderers()`

Quando nenhum output está ativo (padrão), os renderers `cubemapRenderer`, `equirectangularRenderer`,
`fisheyeDomemaster` e `cubemapViewRenderer` são alocados com `outputResolution` mas nunca renderizam.
**Adiar a alocação desses renderers para quando o primeiro output for ativado**
(lazy init acionado em `OutputManager.toggleOutput()`).

### 3.10 Filtro de log suprime erros repetidos

**Arquivo**: `LogManager.java` linhas 92–100

O `Filter` global suprime mensagens idênticas consecutivas. Em falhas per-frame (renderer nulo),
o erro aparece apenas uma vez e some. **Substituir por throttle baseado em tempo**
(ex: permitir a mesma mensagem no máximo 1x a cada 5 segundos).

### 3.11 `activateScene()` chama `setupScene()` mesmo quando a cena já é a atual

**Arquivo**: `SceneManager.java` método `activateScene()`

Sempre invoca `setupScene()`, podendo reinicializar uma cena em uso.
**Adicionar guard:**

```java
if (currentSceneIndex == index) return; // já ativa, não reinicializar
```

### 3.12 OpenGL Error 1282 — problema endêmico, sem solução completa

**Arquivo**: `README.md` e `docs/en/known-issues.md`

O erro 1282 (`GL_INVALID_OPERATION`) é **endêmico** a certas combinações de hardware/driver e **não foi resolvido**. O CHANGELOG 1.4.0 documenta apenas a remoção de um caminho de captura NDI que amplificava o problema (nested `beginDraw`/`endDraw`), mas o erro persiste em outras situações (Apple Silicon, drivers específicos, multi-pass).

**Não** tratar nem documentar o 1282 como resolvido. O README e `known-issues.md` devem mantê-lo listado como problema em investigação com workarounds parciais.

---

## 4. Cobertura de Testes — Gaps Prioritários

Os testes existentes (`SceneManagerTest`, `QuaternionTest`, `CameraManagerTest`,
`MouseControlledCameraTest`) evitam contexto GPU com sucesso.

**Classes sem nenhum teste:**

| Classe | O que testar (sem GPU) |
|---|---|
| `OutputManager` | `toggleOutput()` habilita/desabilita flags; comportamento em plataforma não suportada |
| `OrbitCamera` | `guardDistance()`, `zoom()` com colapso, `reset()`, `snapTo()` |
| `zividomelive` (ciclo de vida) | Transições de `InitState`; `isEnableOutput()` delegando ao `OutputManager` |
| `FisheyeDomemaster` | `setSizePercentage()` restrição; `setFOV()` com shader null |
| `LogManager` | `setMode()` / `getMode()` thread-safety; supressão de duplicatas |

**Padrão para novos testes**: usar stubs sem `PApplet` quando possível; separar lógica
pura (matemática, estado) em métodos testáveis antes de implementar.

---

## 5. Convenções de Código Obrigatórias

- **Nunca** criar `ExecutorService` local — usar `ThreadManager.getExecutor()`.
- **Nunca** usar `System.out.println` — usar `LOGGER` via `LogManager.getLogger()`.
- **Nunca** chamar `beginDraw()`/`endDraw()` dentro de `sceneRender()`.
- **Nunca** reordenar valores de `ViewType` ou `InitState` (índices usados externamente).
- **Nunca** substituir ou envolver `OutputManager.sendOutput()` com chamadas diretas de
  Syphon/Spout/NDI fora da classe `OutputManager`.
- Constantes de shader devem ser `private static final String` na classe que as usa.
- Qualquer nova API pública na interface `Scene` deve ter implementação `default` vazia
  para não quebrar cenas existentes.

---

## 6. Fluxo de Build e Validação

```bash
# Baixa dependências não-Maven (ControlP5, Syphon, Spout) se necessário
bash download_dependencies.sh

# Compilar
./gradlew build

# Testes (sem GPU, roda em CI)
./gradlew test

# Release completo
./gradlew buildReleaseArtifacts
```

Os shaders em `shaders/` são copiados para `data/shaders/` dentro do JAR pelo `build.gradle.kts`.
Não mover nem renomear os arquivos `.vert`/`.frag` sem atualizar as constantes no código Java.
