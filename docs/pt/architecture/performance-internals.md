---
title: "Internals de Performance"
icon: material/source-branch
---
# Internals de Performance

Diagnóstico deve separar observação CPU de execução GPU.

CPU wall time identifica custo/bloqueio do lado CPU, mas não mede por si só execução GPU assíncrona. Relate apenas GPU elapsed realmente registrado pela implementação e não prometa granularidade inexistente.

Targets grandes devem permanecer na GPU no fluxo normal; readback CPU é uma fronteira de custo distinta necessária por transportes como o caminho NDI corrente. Benchmark é evidência da configuração testada, não garantia universal.