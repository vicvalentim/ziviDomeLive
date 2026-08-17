---
title: "API Pública Engine-facing"
icon: material/api
status: engine
---
# API Pública Engine-facing

Algumas classes são públicas porque o engine de renderização e as integrações de output precisam de fronteiras explícitas. Elas não são pré-requisitos para sketches comuns.

Exemplos confirmados:

- `FrameViews` — views finais por frame trocadas entre pipeline de renderização e consumidores de output;
- `CubemapTarget` — target OpenGL de cubemap/FBO/depth usado pelo domínio esférico;
- `ProcessingGlAdapter` — fronteira que concentra a interação Processing/OpenGL.

Outros tipos públicos de suporte GL/cubemap de baixo nível devem receber a mesma classificação quando seus Javadocs indicarem função de engine.

## Regra de estabilidade

Não deduza estabilidade artist-facing apenas da visibilidade Java. Tipos engine-facing podem impor restrições de lifecycle, contexto gráfico ou ownership inadequadas ao User Guide.

Classes package-private de pipeline/policy permanecem internas, ainda que o Developer Guide explique sua arquitetura.
