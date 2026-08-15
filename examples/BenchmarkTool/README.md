# BenchmarkTool — guia completo de uso

O **BenchmarkTool** mede o desempenho real da ziviDomeLive dentro do Processing. Ele ajuda a
responder perguntas como:

- Qual modo de renderização é mais pesado nesta máquina?
- A aplicação mantém 60 FPS com uma cena mais complexa?
- A mudança de resolução causou uma pausa grande?
- Uma alteração no código melhorou ou piorou o tempo de frame?
- Quanto do frame foi gasto no cubemap, na projeção, no preview ou no output?

Não é necessário conhecer OpenGL para começar. O caminho recomendado é executar primeiro o teste
rápido e, quando tudo estiver funcionando, iniciar a qualificação completa.

> O benchmark usa uma janela gráfica e uma GPU reais. Ele não funciona como um teste headless em
> um terminal sem sessão gráfica.

## Comece em 5 minutos

Abra um terminal na raiz do repositório.

### 1. Verifique se o Processing foi encontrado

```bash
./gradlew benchmarkDoctor
```

Um resultado saudável se parece com isto:

```text
Processing CLI: /Applications/Processing.app/Contents/MacOS/Processing (Processing cli)
```

O projeto reconhece tanto o comando antigo `processing-java` quanto o launcher moderno
`Processing cli`. A busca é automática no `PATH` e nos locais usuais do macOS, Windows e Linux.

### 2. Execute um teste rápido

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

Esse comando abre o Processing, testa os quatro modos de renderização, exporta os resultados,
fecha o sketch e cria o relatório automaticamente. Os intervalos são propositalmente curtos: esse
teste confirma que a ferramenta funciona, mas ainda não serve para julgar estabilidade ou comparar
performance.

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

Se o navegador não abrir automaticamente, acesse:

```text
build/reports/benchmark/index.html
```

## Qualificação completa

Depois que o teste rápido passar, use a configuração de qualificação:

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

Essa execução demora muito mais porque testa várias resoluções, modos e transições. Durante o
benchmark:

- não redimensione a janela;
- não altere controles;
- evite abrir programas pesados;
- mantenha a máquina conectada à energia;
- aguarde o Processing fechar sozinho.

Ao terminar, arquive a evidência:

```bash
./gradlew benchmarkArchive
```

O ZIP será criado em `build/benchmark-archives/`. Como `./gradlew clean` apaga o diretório `build`,
copie o arquivo para outro local se precisar guardá-lo por longo prazo.

## O que cada suíte executa

| Suíte | O que mede | Quando usar |
| --- | --- | --- |
| `MODES` | Standard, Domemaster, Equirectangular e Skybox na resolução escolhida | Primeiro teste e comparação rápida |
| `MATRIX` | Standard e os modos esféricos em 1024, 2048, 3072 e 4096 | Analisar escalabilidade por resolução |
| `TRANSITIONS` | Mudanças de resolução, modo, preview, cena e NDI | Encontrar travamentos e tempo de recuperação |
| `ALL` | Matriz completa seguida das transições | Qualificação final; é o padrão da automação |

Um output indisponível não é contado como se tivesse custo zero. O cenário recebe o estado
`UNSUPPORTED` e a razão fica registrada no manifesto da suíte.

## Escolhendo a cena

As cenas são sintéticas e determinísticas: a mesma configuração repete a mesma carga geométrica.

| Cena | Carga | Uso recomendado |
| --- | ---: | --- |
| `EMPTY` | Sem geometria | Medir o custo-base da biblioteca |
| `LIGHT` | 24 caixas | Projeto leve |
| `MEDIUM` | 180 caixas | Comparação geral e padrão da automação |
| `HEAVY` | 720 caixas | Estresse de geometria |
| `SPHERICAL_STRESS` | 640 caixas distribuídas ao redor da câmera | Exercitar todas as faces do cubemap |

## Entendendo os modos de renderização

| Modo | O que representa |
| --- | --- |
| `STANDARD` | Visualização retangular convencional |
| `DOMEMASTER` | Projeção fisheye para fulldome |
| `EQUIRECTANGULAR` | Projeção panorâmica 360° |
| `SKYBOX` | Visualização das faces do cubemap |

Sem output externo, a resolução do modo Standard continua ligada à janela do Processing. Nos modos
esféricos, ela controla o cubemap de preview. Com NDI, Syphon ou Spout ativo, a resolução passa a
representar a base do output de alta resolução. O campo `resolutionDomain` do `summary.json` deixa
explícito qual desses domínios foi medido.

## Usando a interface interativa

Para abrir a ferramenta e configurá-la visualmente:

```bash
./gradlew runBenchmark
```

O painel da ziviDomeLive aparece à esquerda e o painel do BenchmarkTool à direita. A instalação da
biblioteca no sketchbook é atualizada automaticamente antes de o Processing abrir.

### Controles de configuração

- **Render Mode:** escolhe Standard, Domemaster, Equirectangular ou Skybox.
- **Resolution:** escolhe 1024, 2048, 3072 ou 4096.
- **Scene:** seleciona a carga sintética.
- **Floating Preview:** mede o custo com o preview flutuante ligado ou desligado.
- **GPU timer:** solicita também a medição assíncrona da GPU.
- **NDI / Syphon / Spout:** inclui o output escolhido quando ele existe no sistema.
- **Warm-up frames:** quantidade de frames descartados antes da medição.
- **Measurement frames:** quantidade de frames guardados no resultado.

### Botões

- **START:** faz o warm-up configurado e inicia uma medição.
- **WARM UP:** executa somente um aquecimento de diagnóstico; não gera resultado exportável.
- **STOP:** interrompe. Se a medição já começou, preserva os frames concluídos com estado `STOPPED`.
- **RESET:** limpa o resultado atual e volta ao estado inicial.
- **EXPORT:** grava a última medição concluída.
- **RUN SUITE:** executa interativamente a suíte `MODES` com a cena e a resolução selecionadas.

Atalhos:

- `X`: interrompe a execução;
- `E`: exporta o último resultado;
- `H`: mostra ou esconde o painel normal da ziviDomeLive.

Não altere os dois painéis durante o warm-up ou a medição. Para mudar uma configuração, pressione
**STOP**, ajuste o cenário e comece uma nova execução.

## Opções da linha de comando

| Propriedade Gradle | Padrão | Finalidade |
| --- | --- | --- |
| `benchmarkSuite` | `ALL` | `MODES`, `MATRIX`, `TRANSITIONS` ou `ALL` |
| `benchmarkScene` | `MEDIUM` | Cena sintética usada na suíte |
| `benchmarkResolution` | `2048` | Resolução selecionada |
| `benchmarkPreview` | `false` | Liga o floating preview |
| `benchmarkGpu` | `false` | Solicita medição GPU além da CPU |
| `benchmarkGpuTimerPolicy` | `AUTO` | Política de seleção do timer GPU |
| `benchmarkWarmupFrames` | `600` | Frames descartados antes de cada medição |
| `benchmarkMeasurementFrames` | `1800` | Frames retidos em testes estáveis |
| `benchmarkTransitionBaselineFrames` | `120` | Frames estáveis antes de uma transição |
| `benchmarkTransitionPostFrames` | `240` | Frames observados depois da transição |
| `processingExecutable` | descoberta automática | Caminho de uma instalação personalizada do Processing |

Exemplo de instalação personalizada:

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/caminho/para/processing-java \
  -PbenchmarkSuite=MODES
```

Também é possível usar a variável de ambiente:

```bash
export PROCESSING_EXECUTABLE=/caminho/para/processing-java
./gradlew benchmarkSuite -PbenchmarkSuite=MODES
```

## Medição de GPU e Apple Silicon

Para a maioria das pessoas, a configuração correta é:

```text
-PbenchmarkGpu=true -PbenchmarkGpuTimerPolicy=AUTO
```

`AUTO` observa a arquitetura da máquina e os recursos expostos pelo contexto OpenGL:

- em GPUs desktop com timestamps válidos, prefere pares de timestamps;
- em Apple Silicon, usa o caminho controlado `TIME_ELAPSED_EXCLUSIVE` quando necessário;
- se o contexto não oferecer um timer seguro, continua com CPU e registra o diagnóstico.

Os campos abaixo, dentro de `summary.json`, mostram o que realmente aconteceu:

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

O campo mais importante é `effectiveMode`. Se ele mostrar `CPU`, o timer GPU solicitado não ficou
disponível e os `diagnostics` explicam o motivo.

Políticas avançadas:

- `AUTO`: escolha recomendada e sensível à arquitetura;
- `SAFE`: permite somente backends que não assumem propriedade exclusiva de elapsed query;
- `TIMESTAMP`: exige timestamps;
- `ARCHITECTURE_AWARE`: seleção explícita sensível à arquitetura;
- `ELAPSED` ou `TIME_ELAPSED_EXCLUSIVE`: diagnóstico controlado de elapsed query.

Não compare uma execução CPU-only com outra CPU+GPU. O timer GPU introduz fronteiras adicionais no
pipeline OpenGL; compare sempre medições feitas com a mesma política.

## Como ler as métricas

### Métricas principais

| Métrica | Como interpretar |
| --- | --- |
| `FPS Average` | Quanto maior, melhor |
| `Frame Average` | Tempo médio do frame; quanto menor, melhor |
| `P50` | Metade dos frames foi igual ou mais rápida que esse valor |
| `P95` | 95% dos frames ficou abaixo desse valor; mostra estabilidade melhor que a média |
| `P99` | Evidencia travadas raras |
| `Max` | Pior frame observado |
| `1% low FPS` | FPS da faixa mais lenta; quanto maior, mais estável |
| `>16.67 ms` | Frames que perderam o orçamento de 60 FPS |
| `>33.33 ms` | Frames que perderam o orçamento de 30 FPS |
| `>50 ms` | Travadas perceptíveis |

Para avaliar uma mudança, observe primeiro `P95`, `P99`, `1% low` e os frames acima de 16,67 ms.
Uma média boa pode esconder pausas ocasionais.

### Pipeline breakdown

O painel e o relatório separam o custo observado em partes como:

- render Standard;
- captura do cubemap;
- projeção Domemaster ou Equirectangular;
- Skybox;
- preview;
- captura NDI;
- pipeline GPU, quando disponível.

`calls/f` significa chamadas por frame. Os invariantes devem permanecer em zero; um valor diferente
de zero indica que o fluxo de renderização não seguiu a quantidade esperada de passes.

### Transições

Os testes de transição registram:

- `normalP95Ms`: P95 do intervalo estável anterior à mudança;
- `transitionMaxMs`: pior frame depois da mudança;
- `recoveryFrames`: quantos frames foram necessários para voltar ao P95 normal;
- `-1`: a recuperação não aconteceu dentro do intervalo observado.

Esses números são descritivos. A ferramenta não inventa um limite automático de aprovação.

## Onde os resultados são gravados

Quando a ferramenta é iniciada pelo Gradle, os dados ficam em:

```text
build/benchmark-results/
├── <data>-<modo>-<resolução>-<cena>/
│   ├── summary.json
│   ├── frames.csv
│   └── environment.json
└── suite-<data>.json
```

- `summary.json`: configuração, ambiente, agregados, profiling e diagnósticos;
- `frames.csv`: uma linha por frame retido, útil para planilhas e gráficos próprios;
- `environment.json`: sistema operacional, arquitetura, Java, Processing e OpenGL;
- `suite-*.json`: manifesto com cenários `SUPPORTED`, `UNSUPPORTED` ou `FAILED`.

O relatório offline gera:

```text
build/reports/benchmark/
├── index.html
├── data.json
└── summary.md
```

Se o sketch for aberto diretamente, sem Gradle e sem configuração de saída, ele usa
`~/ziviDomeLive-benchmark-results` e mostra o caminho no painel.

## Comparando duas execuções

Escolha os nomes dos dois diretórios existentes em `build/benchmark-results`:

```bash
./gradlew benchmarkReport \
  -PbenchmarkBaseline=<diretorio-antigo> \
  -PbenchmarkCandidate=<diretorio-novo>
```

O relatório mostra os valores, o delta absoluto, a variação percentual e a direção. Compare somente
execuções equivalentes:

- mesma máquina e monitor;
- mesma cena, modo e resolução;
- mesmo estado de preview e outputs;
- mesma configuração de GPU timer;
- mesma quantidade de frames;
- condições térmicas e programas em segundo plano semelhantes.

## Tarefas úteis

```bash
./gradlew benchmarkDoctor   # verifica o Processing
./gradlew runBenchmark      # abre a interface
./gradlew benchmarkSuite    # executa uma suíte automatizada
./gradlew benchmarkReport   # regenera o relatório
./gradlew benchmarkOpen     # gera e abre o relatório
./gradlew benchmarkArchive  # cria um ZIP com dados e relatório
./gradlew benchmarkClean    # apaga dados capturados e relatórios
```

Use `benchmarkClean` somente depois de arquivar tudo o que deseja preservar.

## Solução de problemas

### “Processing CLI was not found”

Execute:

```bash
./gradlew benchmarkDoctor
```

No macOS, confirme que `Processing.app` está em `/Applications`. Para uma instalação personalizada,
informe `-PprocessingExecutable=<caminho>` ou `PROCESSING_EXECUTABLE`.

### O relatório mostra zero execuções válidas

Verifique se existem diretórios em `build/benchmark-results`. Uma execução interrompida antes da
medição ou um `clean` não deixa dados válidos para o relatório. Rode novamente um smoke test.

### O timer GPU voltou para CPU

Veja `profiling.effectiveMode`, `gpuTimerBackend` e `diagnostics` em `summary.json`. O fallback é
intencional quando o driver não oferece um caminho seguro. Em Apple Silicon, mantenha a política
`AUTO` para permitir a seleção apropriada.

### NDI, Syphon ou Spout aparece como indisponível

Isso depende do sistema e das bibliotecas nativas:

- Syphon: macOS;
- Spout: Windows;
- NDI: somente quando as bibliotecas nativas conseguem inicializar.

Um cenário que exige um output ausente deve aparecer como `UNSUPPORTED`, não como custo zero.

### O Processing abriu, mas a suíte não terminou

Confira se existe uma sessão gráfica OpenGL funcional. Não feche a janela manualmente. Para
interromper de forma controlada no modo interativo, use `X` ou **STOP**.

### O relatório desapareceu depois de gerar a release

Tarefas que executam `clean` removem o diretório `build`. Execute novamente a suíte ou restaure um
arquivo criado por `benchmarkArchive`.

## Execução direta, sem Gradle

O Gradle é recomendado porque instala a biblioteca, escolhe o launcher correto e configura os
caminhos automaticamente. Para execução manual no Processing 4.4.3 ou mais recente no macOS:

```bash
export ZIVIDOME_BENCHMARK_OUTPUT="$PWD/build/benchmark-results"
export ZIVIDOME_BENCHMARK_REVISION="$(git rev-parse HEAD)"
/Applications/Processing.app/Contents/MacOS/Processing cli \
  --sketch="$PWD/examples/BenchmarkTool" \
  --output="$PWD/build/processing-benchmark" \
  --force --run
```

Em versões antigas, use `processing-java` e remova o subcomando `cli`. Antes da execução manual, a
biblioteca precisa estar instalada no sketchbook:

```bash
./gradlew deployToProcessingSketchbook
```

## Limitações importantes

- Tempos do pipeline, exceto a métrica GPU explícita, são observações de CPU.
- O tempo NDI mede captura/envio local, não latência de rede nem apresentação no receptor.
- Um valor zero de amostras GPU significa “nenhum resultado disponível”, não “GPU sem custo”.
- A interface ControlP5 visível faz parte da carga medida pelo exemplo.
- Agregação final e gravação em disco acontecem depois da janela medida.
- Resultados de máquinas ou configurações diferentes não devem ser tratados como comparação direta.

Para detalhes de auditoria e formato dos relatórios, consulte:

- `docs/pt/qualification/automated-benchmarks.md`;
- `docs/pt/qualification/benchmark-reporting.md`.
