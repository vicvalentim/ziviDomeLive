---
title: "API Deprecated"
icon: material/api
status: deprecated
---
# API Deprecated

API deprecated é documentada para migração, não recomendada em exemplos novos.

Casos de compatibilidade confirmados na superfície pública 2.0 incluem:

- `OutputManager.setView(ViewType)` — use um seletor específico de destino, como `setNdiView(...)`, `setSyphonView(...)`, `setSpoutView(...)` ou `setLocalTextureView(...)`;
- convenience methods da fachada `renderFisheyeDomemaster()`, `renderEquirectangular()`, `renderCubemap()` e `renderStandard()` — use o modelo corrente de render mode/roteamento.

Os Javadocs gerados são a autoridade para as anotações `@Deprecated` e orientações exatas de substituição. Não remova um símbolo deprecated da documentação enquanto a implementação ainda o expuser.
