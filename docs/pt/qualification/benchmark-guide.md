# Guia do Benchmark

O **BenchmarkTool** mede o desempenho real da ziviDomeLive dentro do Processing. Ele ajuda a
descobrir se a aplicação mantém a taxa de quadros desejada, onde o pipeline gasta mais tempo e se
uma alteração de código melhorou ou piorou a performance.

Não é preciso conhecer OpenGL para começar. Primeiro execute um teste rápido; depois, quando tudo
estiver funcionando, rode a qualificação completa.

!!! note "Requer uma sessão gráfica"
    O benchmark abre uma janela do Processing e usa a GPU real da máquina. Ele não é um teste
    unitário headless e precisa de uma sessão gráfica OpenGL funcional.

## Comece em 5 minutos

Abra um terminal na raiz do repositório.

### 1. Verifique o Processing

```bash
./gradlew benchmarkDoctor
```

Um resultado saudável se parece com:

```text
Processing CLI: /Applications/Processing.app/Contents/MacOS/Processing (Processing cli)
```

A descoberta reconhece o antigo `processing-java` e o launcher moderno `Processing cli`. Ela
pesquisa o `PATH` e os locais usuais do macOS, Windows e Linux.

### 2. Execute um smoke test

```bash
./gradlew benchmarkSuite \
  -PbenchmarkSuite=MODES \
  -PbenchmarkScene=EMPTY \
  -PbenchmarkResolution=1024 \
  -PbenchmarkPreview=false \
  -PbenchmarkGpu=true \
  -PbenchmarkGpuTimerPolicy=AUTO \
  -PbenchmarkWarmupFrames=8 \
  -PbenchmarkMeasurementFrames=16
```

O Processing testará os quatro modos, exportará os resultados, fechará o sketch e gerará o
relatório. Os intervalos são curtos de propósito: esse teste comprova a orquestração, não a
estabilidade da performance.

O final esperado é semelhante a:

```text
Benchmark report: .../build/reports/benchmark/index.html
(4 valid run(s), 0 notice(s), comparison=false)
BUILD SUCCESSFUL
```

### 3. Abra o relatório

```bash
./gradlew benchmarkOpen
```

Sem integração com navegador, abra manualmente `build/reports/benchmark/index.html`.

## Qualificação completa

Depois que o teste rápido passar, use intervalos longos:

```bash
./gradlew benchmarkSuite \
  -PbenchmarkSuite=ALL \
  -PbenchmarkScene=MEDIUM \
  -PbenchmarkResolution=2048 \
  -PbenchmarkPreview=false \
  -PbenchmarkGpu=true \
  -PbenchmarkGpuTimerPolicy=AUTO \
  -PbenchmarkWarmupFrames=600 \
  -PbenchmarkMeasurementFrames=1800 \
  -PbenchmarkTransitionBaselineFrames=120 \
  -PbenchmarkTransitionPostFrames=240
```

Essa execução demora porque combina modos, resoluções e transições. Enquanto ela estiver ativa:

- não redimensione nem feche a janela;
- não altere os controles;
- evite programas pesados em segundo plano;
- mantenha notebooks conectados à energia;
- aguarde o Processing encerrar sozinho.

Ao terminar, preserve a evidência:

```bash
./gradlew benchmarkArchive
```

O ZIP fica em `build/benchmark-archives/`. Um `./gradlew clean` apaga o diretório `build`; copie o
arquivo para outro local quando precisar guardá-lo por longo prazo.

## Suítes disponíveis

| Suíte | O que mede | Quando usar |
| --- | --- | --- |
| `MODES` | Standard, Domemaster, Equirectangular e Skybox na resolução escolhida | Primeiro teste e comparação rápida |
| `MATRIX` | Standard e modos esféricos em 1024, 2048, 3072 e 4096 | Escalabilidade por resolução |
| `TRANSITIONS` | Mudanças de resolução, modo, preview, cena e NDI | Travadas e tempo de recuperação |
| `ALL` | Matriz completa seguida das transições | Qualificação final; é o padrão automatizado |

Um output indisponível nunca é tratado como custo zero. O cenário recebe o estado `UNSUPPORTED` e
o manifesto registra a razão.

## Cenas de teste

As cenas são sintéticas e determinísticas: a mesma configuração repete a mesma carga.

| Cena | Carga | Uso recomendado |
| --- | ---: | --- |
| `EMPTY` | Sem geometria | Custo-base da biblioteca |
| `LIGHT` | 24 caixas | Projeto leve |
| `MEDIUM` | 180 caixas | Comparação geral; padrão automatizado |
| `HEAVY` | 720 caixas | Estresse de geometria |
| `SPHERICAL_STRESS` | 640 caixas ao redor da câmera | Exercitar todas as faces do cubemap |

## Modos de renderização

| Modo | O que representa |
| --- | --- |
| `STANDARD` | Visualização retangular convencional |
| `DOMEMASTER` | Projeção fisheye para fulldome |
| `EQUIRECTANGULAR` | Projeção panorâmica 360° |
| `SKYBOX` | Visualização das faces do cubemap |

Sem output externo, a resolução Standard está ligada à janela do Processing. Nos modos esféricos,
ela controla o cubemap de preview. Com NDI, Syphon ou Spout ativo, a resolução representa a base do
output de alta resolução. Consulte `resolutionDomain` no `summary.json` para saber o domínio medido.

## Interface interativa

Abra a ferramenta visual com:

```bash
./gradlew runBenchmark
```

O painel normal da ziviDomeLive aparece à esquerda e o BenchmarkTool à direita. O Gradle atualiza a
biblioteca no sketchbook antes de abrir o Processing.

### Configuração

- **Render Mode:** Standard, Domemaster, Equirectangular ou Skybox.
- **Resolution:** 1024, 2048, 3072 ou 4096.
- **Scene:** carga sintética usada no teste.
- **Floating Preview:** inclui ou remove o preview flutuante.
- **GPU timer:** solicita uma medição assíncrona da GPU além da CPU.
- **NDI / Syphon / Spout:** inclui um output disponível no sistema.
- **Warm-up frames:** frames descartados antes da medição.
- **Measurement frames:** frames preservados no resultado.

### Botões e atalhos

| Controle | Ação |
| --- | --- |
| **START** | Executa o warm-up configurado e inicia a medição |
| **WARM UP** | Faz somente um aquecimento de diagnóstico; não gera resultado exportável |
| **STOP** ou `X` | Interrompe; preserva frames se a medição já começou |
| **RESET** | Limpa o resultado atual |
| **EXPORT** ou `E` | Grava a última medição concluída |
| **RUN SUITE** | Executa `MODES` com a cena e resolução selecionadas |
| `H` | Mostra ou esconde o painel normal da ziviDomeLive |

Não altere os painéis durante o warm-up ou a medição. Pare a execução, mude a configuração e comece
um novo teste.

## Opções automatizadas

| Propriedade Gradle | Padrão | Finalidade |
| --- | --- | --- |
| `benchmarkSuite` | `ALL` | `MODES`, `MATRIX`, `TRANSITIONS` ou `ALL` |
| `benchmarkScene` | `MEDIUM` | Cena usada pela suíte |
| `benchmarkResolution` | `2048` | Resolução selecionada |
| `benchmarkPreview` | `false` | Estado do floating preview |
| `benchmarkGpu` | `false` | Solicita timer GPU |
| `benchmarkGpuTimerPolicy` | `AUTO` | Política de seleção do timer GPU |
| `benchmarkWarmupFrames` | `600` | Frames descartados antes de cada medição |
| `benchmarkMeasurementFrames` | `1800` | Frames retidos em cenários estáveis |
| `benchmarkTransitionBaselineFrames` | `120` | Intervalo anterior à transição |
| `benchmarkTransitionPostFrames` | `240` | Intervalo posterior à transição |
| `processingExecutable` | automático | Caminho de uma instalação personalizada |

Exemplo de instalação personalizada:

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/caminho/para/processing-java \
  -PbenchmarkSuite=MODES
```

Também é possível definir `PROCESSING_EXECUTABLE` no ambiente.

## GPU e Apple Silicon

Para a maioria das máquinas, use:

```text
-PbenchmarkGpu=true -PbenchmarkGpuTimerPolicy=AUTO
```

A política `AUTO` observa a arquitetura e as capabilities do OpenGL:

- prefere pares de timestamps quando eles são confiáveis;
- no Apple Silicon, seleciona `TIME_ELAPSED_EXCLUSIVE` quando necessário;
- se não houver timer seguro, continua em CPU e registra o diagnóstico.

O bloco `profiling` do `summary.json` mostra o resultado real:

```json
"profiling": {
  "requestedMode": "CPU_GPU",
  "effectiveMode": "CPU_GPU",
  "gpuTimerPolicy": "ARCHITECTURE_AWARE",
  "gpuTimerBackend": "TIME_ELAPSED_EXCLUSIVE",
  "gpuTimerArchitecture": "APPLE_SILICON",
  "gpuSamples": 1800
}
```

Use `effectiveMode` como fonte de verdade. Se ele mostrar `CPU`, consulte `diagnostics`. Não compare
uma execução CPU-only com outra CPU+GPU, pois o profiling GPU acrescenta fronteiras ao pipeline.

Políticas avançadas: `SAFE` proíbe o elapsed exclusivo; `TIMESTAMP` exige timestamps;
`ARCHITECTURE_AWARE` explicita a seleção por arquitetura; `ELAPSED` e
`TIME_ELAPSED_EXCLUSIVE` servem para diagnóstico controlado.

## Como ler as métricas

| Métrica | Interpretação |
| --- | --- |
| FPS médio | Quanto maior, melhor |
| Tempo médio do frame | Quanto menor, melhor |
| P50 | Metade dos frames foi igual ou mais rápida |
| P95 | 95% dos frames ficou abaixo do valor; bom indicador de estabilidade |
| P99 | Evidencia travadas raras |
| Máximo | Pior frame observado |
| 1% low FPS | Desempenho da faixa mais lenta; quanto maior, melhor |
| Frames acima de 16,67 ms | Frames que perderam o orçamento de 60 FPS |
| Frames acima de 33,33 ms | Frames que perderam o orçamento de 30 FPS |
| Frames acima de 50 ms | Travadas perceptíveis |

Observe primeiro P95, P99, 1% low e frames acima de 16,67 ms. Uma média boa pode esconder pausas.

O **Pipeline Breakdown** separa custos de Standard, cubemap, projeções, Skybox, preview, NDI e GPU.
`calls/f` significa chamadas por frame. Os invariantes devem permanecer em zero.

### Transições

- `normalP95Ms`: P95 do intervalo estável anterior;
- `transitionMaxMs`: pior frame depois da mudança;
- `recoveryFrames`: frames necessários para voltar ao P95 normal;
- `-1`: sem recuperação dentro do intervalo observado.

São medidas descritivas; a ferramenta não aplica um limite arbitrário de aprovação.

## Resultados e relatórios

Cada execução cria:

```text
build/benchmark-results/
├── <data>-<modo>-<resolução>-<cena>/
│   ├── summary.json
│   ├── frames.csv
│   └── environment.json
└── suite-<data>.json
```

- `summary.json`: configuração, agregados, profiling e diagnósticos;
- `frames.csv`: uma linha para cada frame preservado;
- `environment.json`: sistema, arquitetura, Java, Processing e OpenGL;
- `suite-*.json`: estados `SUPPORTED`, `UNSUPPORTED` e `FAILED`.

O relatório offline fica em `build/reports/benchmark/` e contém `index.html`, `data.json` e
`summary.md`.

### Comparar baseline e candidate

```bash
./gradlew benchmarkReport \
  -PbenchmarkBaseline=<diretorio-antigo> \
  -PbenchmarkCandidate=<diretorio-novo>
```

Compare somente execuções com a mesma máquina, cena, modo, resolução, outputs, timer GPU e número de
frames. Condições térmicas e programas em segundo plano também importam.

## Tarefas úteis

```bash
./gradlew benchmarkDoctor   # verifica o Processing
./gradlew runBenchmark      # abre a interface
./gradlew benchmarkSuite    # executa a automação
./gradlew benchmarkReport   # regenera o relatório
./gradlew benchmarkOpen     # gera e abre o relatório
./gradlew benchmarkArchive  # arquiva dados e relatório
./gradlew benchmarkClean    # apaga dados e relatório
```

## Problemas comuns

### Processing CLI não encontrado

Rode `./gradlew benchmarkDoctor`. No macOS, mantenha `Processing.app` em `/Applications`. Para uma
instalação personalizada, use `-PprocessingExecutable=<caminho>` ou `PROCESSING_EXECUTABLE`.

### Zero execuções válidas

Confirme se `build/benchmark-results` contém runs completos. Uma medição não iniciada ou um `clean`
não deixa dados válidos. Execute novamente o smoke test.

### Timer GPU voltou para CPU

Confira `effectiveMode`, `gpuTimerBackend` e `diagnostics` no `summary.json`. O fallback é esperado
quando o driver não fornece um caminho seguro. Em Apple Silicon, mantenha `AUTO`.

### NDI, Syphon ou Spout indisponível

Syphon é específico do macOS; Spout, do Windows; NDI depende das bibliotecas nativas. Um output
ausente gera `UNSUPPORTED`, nunca uma medição artificial de custo zero.

### Relatório removido depois de `clean`

Execute novamente a suíte ou restaure um `benchmarkArchive`. Preserve arquivos importantes fora de
`build`.

## Limitações

- Tempos do pipeline, exceto a métrica GPU explícita, são observações da CPU.
- NDI mede captura e envio local, não latência de rede ou apresentação no receptor.
- Zero amostras GPU significa ausência de resultado, não GPU sem custo.
- A interface ControlP5 visível faz parte da carga do exemplo.
- Agregação final e gravação em disco acontecem depois da janela medida.
- Máquinas ou configurações diferentes não formam uma comparação direta confiável.

## Referência avançada

- [Benchmarks automatizados](automated-benchmarks.md): planos, propriedades e transições em detalhe.
- [Relatórios de benchmark](benchmark-reporting.md): schemas, validação e comparação.
- [Profiling de performance](../api/performance-profiling.md): API pública de instrumentação.
