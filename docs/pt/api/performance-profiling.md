# Profiling de Performance

A API experimental de performance fornece instrumentação CPU de baixo overhead e uma medição GPU agregada opcional para ferramentas de desenvolvimento e qualificação. Ela permanece desabilitada por padrão e não substitui profilers externos de GPU.

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

Para solicitar tempo GPU, use `PerformanceMode.CPU_GPU` e consulte o modo efetivo e o canal GPU
separado:

```java
PerformanceSnapshot snapshot = dome.getPerformanceSnapshot();
if (snapshot.getEffectiveMode() == PerformanceMode.CPU_GPU && snapshot.hasGpuTimings()) {
  PerformanceSnapshot.MetricStatistics gpu =
      snapshot.getGpuStatistics(PerformanceMetric.RENDER_PIPELINE);
  println(gpu.getAverageMilliseconds());
}
```

A criação do snapshot copia e ordena as amostras retidas. Solicite snapshots somente fora do intervalo medido. A primeira fronteira `pre()` estabelece a linha de base; a primeira amostra completa de `FRAME_TOTAL` surge na fronteira seguinte.

## Modos e Overhead

- `OFF`: sem `System.nanoTime()`, escrita de samples ou atomics adicionais no caminho de renderização.
- `CPU`: wall time observado pela CPU com `System.nanoTime()`.
- `CPU_GPU`: instrumentação CPU mais um intervalo assíncrono de timestamps GPU, condicionado a capability, em torno de `RENDER_PIPELINE`. Contextos sem suporte ou com falha informam `CPU` como modo efetivo e acrescentam um diagnóstico.

O caminho OFF executa apenas verificações previsíveis de monitor inativo nas fronteiras instrumentadas. Os arrays de samples e acumuladores do worker são alocados somente ao habilitar profiling. Objetos de query GPU são criados de forma tardia somente após solicitar `CPU_GPU` na thread de renderização do Processing. O contexto desktop ativo precisa informar counter bits não nulos para `GL_TIMESTAMP`; drivers que expõem elapsed query mas não timestamps fazem fallback para CPU. Um pool de oito pares lê apenas resultados indicados por `GL_QUERY_RESULT_AVAILABLE`; saturação, resultados atrasados, disable ou perda de contexto descartam samples em vez de esperar. Não há `glFinish()`.

## Interpretação

`FRAME_TOTAL` é o intervalo entre fronteiras `pre()` consecutivas do Processing. Ele é a fonte primária para FPS médio, P50, P95, P99, pior frame, 1% low e contagens acima de 16,67/33,33/50 ms.

Durações CPU ao redor de operações OpenGL medem submissão mais qualquer espera de driver observada pelo chamador. O valor GPU separado mede somente comandos entre os timestamps do pipeline completo; não inclui `Scene.update()`, pacing, trabalho CPU fora desse intervalo, apresentação no receptor ou latência de rede. Ele não é dividido por passe porque fronteiras `beginPGL()` adicionais fariam flush e perturbariam a carga. Pares de timestamp não ocupam o target global `GL_TIME_ELAPSED`, portanto queries elapsed da cena não ficam aninhadas dentro de uma query da biblioteca. O tempo NDI continua sendo a duração da chamada do sender nativo; Syphon e Spout não medem apresentação no receptor.

Cada métrica também informa o total de chamadas e a média de chamadas por frame retido. Isso torna invariantes de passes observáveis independentemente dos tempos. O monitor registra violações quando a captura cubemap excede ou diverge da contagem requerida, ou quando Standard, projeções e cópias de preview divergem do fechamento de requisitos atual.

## Armazenamento

As amostras usam ring buffer prealocado de tipos primitivos. Ao exceder a capacidade, os frames mais antigos são sobrescritos e `getOverwrittenFrames()` informa a quantidade. Samples CPU brutos usam `getDurationNanos()` / `getCalls()`; resultados GPU alinhados usam `getGpuDurationNanos()` / `getGpuCalls()`. Contagem GPU zero significa ausência de resultado completo naquele frame, não custo GPU zero.

`disablePerformanceProfiling()` interrompe a coleta sem descartar frames completos. `resetPerformanceStatistics()` limpa tempos e contadores de invariantes.
