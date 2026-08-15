# Profiling de Performance

A API experimental de performance fornece instrumentação CPU de baixo overhead para ferramentas de desenvolvimento e qualificação. Ela permanece desabilitada por padrão e não substitui profilers externos de GPU.

## Habilitar e Ler

```java
dome.enablePerformanceProfiling(PerformanceMode.CPU, 4096);

// Execute o warm-up e então resete antes do intervalo medido.
dome.resetPerformanceStatistics();

PerformanceSnapshot snapshot = dome.getPerformanceSnapshot();
PerformanceSnapshot.MetricStatistics frame =
    snapshot.getStatistics(PerformanceMetric.FRAME_TOTAL);

println(frame.getAverageMilliseconds());
println(frame.getP95Milliseconds());
println(frame.getOnePercentLowFps());
```

A criação do snapshot copia e ordena as amostras retidas. Solicite snapshots somente fora do intervalo medido. A primeira fronteira `pre()` estabelece a linha de base; a primeira amostra completa de `FRAME_TOTAL` surge na fronteira seguinte.

## Modos e Overhead

- `OFF`: sem `System.nanoTime()`, escrita de samples ou atomics adicionais no caminho de renderização.
- `CPU`: wall time observado pela CPU com `System.nanoTime()`.
- `CPU_GPU`: reservado para timer queries OpenGL assíncronas e condicionadas a capability. A versão 2.0 informa CPU como modo efetivo e acrescenta um diagnóstico, sem sincronizar a GPU.

O caminho OFF executa apenas verificações previsíveis de monitor inativo nas fronteiras instrumentadas. Os arrays de samples e acumuladores do worker são alocados somente ao habilitar profiling.

## Interpretação

`FRAME_TOTAL` é o intervalo entre fronteiras `pre()` consecutivas do Processing. Ele é a fonte primária para FPS médio, P50, P95, P99, pior frame, 1% low e contagens acima de 16,67/33,33/50 ms.

Durações CPU ao redor de operações OpenGL medem submissão mais qualquer espera de driver observada pelo chamador. Elas não representam tempo GPU isolado. Da mesma forma, o tempo de envio NDI é a duração da chamada do sender nativo, não latência de rede ponta a ponta; Syphon e Spout não medem a apresentação no receptor.

Cada métrica também informa o total de chamadas e a média de chamadas por frame retido. Isso torna invariantes de passes observáveis independentemente dos tempos. O monitor registra violações quando a captura cubemap excede ou diverge da contagem requerida, ou quando Standard, projeções e cópias de preview divergem do fechamento de requisitos atual.

## Armazenamento

As amostras usam ring buffer prealocado de tipos primitivos. Ao exceder a capacidade, os frames mais antigos são sobrescritos e `getOverwrittenFrames()` informa a quantidade. Samples brutos continuam disponíveis em ordem cronológica por `getDurationNanos()` e `getCalls()` para futura exportação JSON/CSV.

`disablePerformanceProfiling()` interrompe a coleta sem descartar frames completos. `resetPerformanceStatistics()` limpa tempos e contadores de invariantes.
