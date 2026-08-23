---
title: API 1.x Removida
icon: material/history
status: internal
tags:
  - Migração
  - História
---

# API 1.x Removida

A superfície final 2.0 contém **zero API deprecated de compatibilidade**. Esta página preserva a história de migração sem apresentar símbolos removidos como chamáveis.

| Conceito 1.x | Migração 2.0 |
|---|---|
| facade lowercase `zividomelive` | Renomeie declarações/construção para `ziviDomeLive`; o package mantém a grafia |
| valores nested/legacy de view | Importe `ViewType` top-level; use `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR`, `SKYBOX` |
| `renderStandard`, `renderFisheyeDomemaster`, `renderEquirectangular`, `renderCubemap` diretos | Selecione `RenderMode` e `ViewType` do destino; o render roda pelos hooks registrados |
| getters/setters de renderer e classes de renderer diretas | Use configuração da facade; o grafo concreto é interno |
| toggles genéricos/string e `setView` | Use `OutputType`, `setOutputEnabled`, `setViewForOutput` tipados |
| `sendOutput`/containers de frame públicos | Use controles consumer de `OutputManager`; publicação é interna |
| `ThreadManager`/executor público | Use `SceneServices.tasks().submitIfIdle(...)` |
| `Scene.controlEvent(ControlEvent)` | Use painel pertencente à facade, `SceneActionMap` ou callbacks raw de teclado/mouse |
| hooks de cleanup do runtime e `close()` de serviços | Libere recursos da cena em `dispose()`; o runtime fecha serviços da ativação |
| fila raw de render | Retorne resultados CPU por callbacks de task na fronteira de frame da ativação |
| adapters/targets GL/cubemap públicos | Sem substituição; são detalhes de implementação |

## Disciplina de migração

1. Torne mutação de estado explícita em `update()`.
2. Mantenha `sceneRender()` draw-only e remova `beginDraw()`/`endDraw()`.
3. Registre cenas pela facade.
4. Mova background work para tasks bounded da ativação.
5. Substitua strings de backend e roteamento genérico por métodos tipados.
6. Elimine dependências de renderers, `FrameViews`, adapters GL e targets de cubemap.

Snapshots históricos da API 1.4/1.5 em `docs/qualification/` continuam como evidência do que existiu; não são referência 2.0.
