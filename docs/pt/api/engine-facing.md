---
title: Fronteira Internal
icon: material/shield-lock-outline
status: internal
tags:
  - Arquitetura
  - Internal
---

# Fronteira Internal

A antiga categoria “API pública engine-facing” foi eliminada no freeze final 2.0. A implementação de renderer/output pode ser documentada para manutenção sem ser chamável por sketches.

## Taxonomia física de fontes

| Pasta | Responsabilidade |
|---|---|
| `_internal/render/core` | Resolução de requisitos do frame, composição de environment e orquestração do pipeline |
| `_internal/render/camera` | Faces/orientação do cubemap e roteamento interno de câmera |
| `_internal/render/gl` | Adapter Processing/JOGL, recursos cubemap/FBO e seam de medição GPU |
| `_internal/render/modes` | Implementações Standard, domemaster, equirectangular e skybox |
| `_internal/output` | Containers finais de frame, implementação do output manager e producers de backend |
| `_internal/performance` | Monitoring mutável, estatísticas e timers GPU |
| `_internal/runtime` | Fila da render thread e executor compartilhado |
| `_internal/scene` | Cena default e implementação de resource cache |
| `_internal/ui` | Painel ControlP5, bridge de input, layout e splash |
| `_internal/support` | Implementação de logging e metadata da biblioteca |

As pastas físicas melhoram manutenção. Declarações de package mantêm intencionalmente a fronteira de colaboração necessária às implementações package-private.

## Nomes históricos

`FrameViews`, `CubemapTarget`, `ProcessingGlAdapter`, renderers concretos, `ThreadManager`, `ControlManager` e nomes semelhantes podem aparecer em arquitetura/história. Não são API pública 2.0 e nunca devem aparecer em código de sketch atual.

## Regra de promoção

Um tipo internal só pode se tornar público por proposta futura explícita de API com audiência, ownership, lifecycle, Javadocs, testes, exemplos e política de compatibilidade. Conveniência, sozinha, é insuficiente.
