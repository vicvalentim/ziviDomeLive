---
title: "Pipeline de Renderização"
icon: material/source-branch
---
# Pipeline de Renderização

O pipeline resolve as views necessárias no frame, renderiza o(s) domínio(s) exigidos e entrega views finais aos consumidores de preview/output.

A política interna decide se o frame exige Standard, cubemap esférico e quais projeções. Esses tipos internos são arquitetura, não API artist-facing.

O cubemap esférico deve ser capturado uma vez por frame e reutilizado pelas projeções/consumidores necessários. A view Standard permanece independente. Uma fronteira interna imutável por frame leva as representações finais aos producers; callers escolhem resultados por `ViewType`.

Toda view final offscreen começa em RGBA `(0, 0, 0, 0)`. O alpha continua presente no caminho
Scene → renderer → view final → preview/output. Backgrounds desenhados pela Scene e imagens de
Environment configuradas e visíveis são conteúdo explícito, não fallbacks da biblioteca. A janela
primária do Processing e receivers externos podem ter limitações próprias de composição/alpha;
essas limitações não mudam o contrato do framebuffer final.
