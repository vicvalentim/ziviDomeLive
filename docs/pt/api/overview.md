---
title: "Visão Geral da API"
icon: material/api
---
# Visão Geral da API

A superfície Java pública é documentada deliberadamente por **público e papel de estabilidade**, não apenas pelo modificador `public`.

## Artist-facing stable

Comece aqui em projetos Processing comuns:

- `ziviDomeLive`
- `Scene`
- `SceneManager`
- `RenderMode`
- `ViewType`
- `OutputManager`

Esses tipos definem o fluxo normal de cena, renderização, calibração, preview e output.

## Advanced public

Recursos públicos para projetos que exigem mais controle de lifecycle, tempo, tasks, câmera ou renderers incluem `SceneServices`, `FrameClock`, `SimulationTimeline`, `OrbitCamera`, `SphericalOrientation` e implementações públicas de renderers. Permanecem API pública chamável, mas não são pré-requisitos de uma cena simples.

## Experimental public

A instrumentação de performance é experimental e orientada a qualificação. Trate os Javadocs gerados e a página Performance Profiling como contrato das métricas efetivamente implementadas. CPU wall time e GPU elapsed time são medidas diferentes.

## Engine-facing public

Alguns tipos são públicos porque componentes do renderer/output precisam de uma fronteira chamável. Exemplos: `FrameViews`, `CubemapTarget` e `ProcessingGlAdapter`. Eles são documentados para contribuidores e integrações avançadas, não como percurso inicial do artista.

## Superfície deprecated de compatibilidade

Métodos deprecated continuam documentados enquanto existirem para permitir migração de sketches. Novos exemplos não devem usar convenience methods deprecated quando houver rota corrente.

## Arquitetura interna não é API pública

Tipos package-private da política/pipeline de renderização podem ser explicados no Developer Guide sem serem apresentados como API chamável.

Para assinaturas exatas, use sempre os Javadocs gerados. Se prosa e Javadocs divergirem, implementação/Javadocs prevalecem e a prosa deve ser corrigida.
