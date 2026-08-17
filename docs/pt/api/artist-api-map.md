---
title: Mapa da API para Artistas
icon: material/api
status: stable
---

# Mapa da API para Artistas

Este mapa identifica a superfície pública destinada a sketches comuns do Processing. Use-o para decidir **por onde começar**; use os Javadocs gerados para assinaturas exatas, overloads e notas de lifecycle.

<div class="grid cards" markdown>

- :material-application-braces-outline: **`ziviDomeLive`**

    Facade principal do runtime associada ao sketch Processing.

- :material-palette-outline: **`Scene`**

    Define atualização de estado e desenho por `update()` e `sceneRender()`.

- :material-view-dashboard-outline: **`RenderMode` + `ViewType`**

    Separam o modo de trabalho atual da representação por destino.

- :material-export: **`OutputManager`**

    Roteia views finais para outputs externos opcionais e expõe estado/falha.

</div>

## Runtime e cenas

| Tipo | Papel |
|---|---|
| `ziviDomeLive` | Facade principal do runtime usada por um sketch Processing. |
| `Scene` | Contrato de lifecycle, update e desenho da Scene. |
| `SceneManager` | Registro e troca de cenas. |

O percurso normal é `ziviDomeLive` → `Scene`, com `SceneManager` quando o sketch contém mais de uma cena.

## Controles comuns da facade

<div class="grid cards" markdown>

- **Scene** — `setScene(...)`, `registerScene(...)`, `setSceneManager(...)`, `getSceneManager()`
- **Modo de renderização** — `setRenderMode(...)`, `getRenderMode()`, `setCurrentView(...)`
- **Calibração esférica** — `setFov(...)`, `setFishSize(...)`, `setPitch(...)`, `setYaw(...)`, `setRoll(...)`, `resetOrientation()`
- **Preview/output** — `setShowPreview(...)`, `setStandardOutputAspectMode(...)`, `getOutputManager()`, `resetGraphics(int)`, `getOutputResolution()`
- **Câmera da Scene** — `getSceneCamera()`, `setSceneCameraInputEnabled(...)`
- **Lifecycle** — `pause()`, `resume()`, `dispose()`

</div>

!!! info "Java public ≠ artist-facing"
    `SceneServices`, serviços de tempo/assets/tasks, implementações públicas de renderer, snapshots de performance, `FrameViews` e tipos OpenGL de bridge/target são documentados separadamente como API Advanced, Experimental ou Engine-facing.

<div class="zd-actions" markdown>
[Visão Geral da API](overview.md){ .md-button }
[Javadocs Gerados](javadocs.md){ .md-button .md-button--primary }
</div>
