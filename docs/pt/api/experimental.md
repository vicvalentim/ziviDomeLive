---
title: "API Pública Experimental"
icon: material/api
status: experimental
---
# API Pública Experimental

A superfície corrente de instrumentação de performance é experimental e orientada a qualificação.

## Contrato

- não trate métricas experimentais como garantias estáveis da API para usuários;
- medidas de CPU wall time não são medidas de GPU elapsed time;
- documente apenas timing GPU realmente exposto pela implementação corrente;
- use `BenchmarkTool` em qualificação, não como primeiro exemplo de aprendizagem.

`PerformanceSnapshot` e tipos relacionados de performance pertencem aqui. Consulte os Javadocs gerados para campos/métodos exatos desta release.
