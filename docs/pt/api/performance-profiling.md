---
title: "Profiling de Performance"
icon: material/api
status: experimental
---
# Profiling de Performance

!!! warning "Avançado / qualificação"
    Instrumentação de performance não é requisito para criar/renderizar uma cena ziviDomeLive comum. Use-a para profiling, benchmark ou qualificação de release/hardware.

## CPU wall time não é GPU elapsed time

CPU wall time descreve tempo observado pela CPU ao redor de uma etapa. Trabalho OpenGL pode ser enfileirado/assíncrono, portanto esse valor não deve ser descrito como tempo de execução GPU da mesma etapa.

GPU elapsed só é válido onde a implementação corrente expõe de fato uma medida GPU. Não deduza granularidade GPU por estágio que a API não fornece.

## BenchmarkTool

`BenchmarkTool` é ferramenta de qualificação. Use-a para produzir evidência repetível de uma configuração específica de software/hardware; não a apresente como pré-requisito de sketches comuns.

## Regra de relatório

Um relatório útil identifica ao menos:

- versão/commit ziviDomeLive sob teste;
- ambiente Processing/Java realmente usado;
- resolução de output e rotas ativas;
- modo/tipo de medida de benchmark;
- origem CPU ou GPU de cada métrica;
- hardware/OS quando o resultado for usado como evidência de qualificação de plataforma.

Nunca converta “suportado pelo caminho de código” em “testado nesta plataforma” sem qualificação registrada.
