---
title: "Pipeline de Renderização"
icon: material/source-branch
---
# Pipeline de Renderização

O pipeline resolve as views necessárias no frame, renderiza o(s) domínio(s) exigidos e entrega views finais aos consumidores de preview/output.

A política interna decide se o frame exige Standard, cubemap esférico e quais projeções. Esses tipos internos são arquitetura, não API artist-facing.

O cubemap esférico deve ser capturado uma vez por frame e reutilizado pelas projeções/consumidores necessários. A view Standard permanece independente. `FrameViews` funciona como fronteira engine-facing das representações finais.