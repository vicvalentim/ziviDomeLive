# Benchmarks Automatizados

O `BenchmarkTool` pode executar suítes determinísticas em um processo Java Mode real do Processing
4. Isso é integração com GPU/hardware, não um teste unitário headless: a máquina precisa oferecer
uma sessão gráfica OpenGL funcional e os backends nativos solicitados pelo cenário.

## CLI do Processing

Configure o executável do Processing por propriedade Gradle ou variável de ambiente:

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/caminho/para/processing-java

export PROCESSING_EXECUTABLE=/caminho/para/processing-java
./gradlew benchmarkSuite
```

Sem esses valores, o Gradle procura `processing-java` no `PATH`. Uma CLI ausente ou sem permissão de
execução encerra imediatamente com uma mensagem de configuração. Nenhum local de aplicativo é
fixado no código. `runBenchmark` usa a mesma descoberta e abre o sketch interativo:

```bash
./gradlew runBenchmark -PprocessingExecutable=/caminho/para/processing-java
```

As duas tarefas executam primeiro `deployBenchmarkLibrary`, que atualiza apenas o JAR usado pelo
benchmark, sua dependência de runtime e os metadados no sketchbook detectado. Diferente do deploy de
release completo, essa tarefa não chama `clean`, preservando as capturas de baseline.

## Suítes

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/caminho/para/processing-java \
  -PbenchmarkSuite=ALL
```

Os planos disponíveis são:

- `MODES`: Standard, Domemaster, Equirectangular e Skybox na resolução selecionada;
- `MATRIX`: um Standard retangular mais Domemaster, Equirectangular e Skybox em 1024, 2048, 3072
  e 4096;
- `TRANSITIONS`: 2048→4096, Standard→Domemaster, Preview desligado→ligado, Light→Heavy e NDI
  desligado→ligado;
- `ALL`: matriz seguida das transições; é o padrão de `benchmarkSuite`.

Standard não é repetido para resoluções cubemap quando o output externo está desligado: seu domínio
ativo é a janela retangular do Processing. Cenários com output nativo nunca são tratados como custo
zero quando indisponíveis; eles são registrados como `UNSUPPORTED` no manifesto da suíte.

O runner exporta cada run concluído, grava `suite-<timestamp>.json`, encerra o Processing e então
gera `build/reports/benchmark/index.html`.

## Propriedades de Duração e Cenário

Os padrões representam intervalos de qualificação; valores menores servem apenas para smoke tests:

```bash
./gradlew benchmarkSuite \
  -PprocessingExecutable=/caminho/para/processing-java \
  -PbenchmarkSuite=MODES \
  -PbenchmarkScene=MEDIUM \
  -PbenchmarkResolution=2048 \
  -PbenchmarkPreview=false \
  -PbenchmarkWarmupFrames=600 \
  -PbenchmarkMeasurementFrames=1800 \
  -PbenchmarkTransitionBaselineFrames=120 \
  -PbenchmarkTransitionPostFrames=240
```

O warm-up é descartado. Uma transição registra primeiro um intervalo estável, aplica exatamente uma
mudança na thread Processing/render e retém o intervalo posterior configurado. `normalP95Ms` vem do
intervalo inicial; `transitionMaxMs` é a maior amostra após a mudança; `recoveryFrames` é o primeiro
offset pós-mudança igual ou inferior ao P95 normal, ou `-1` quando não há recuperação observada.
São medidas descritivas, sem limiar oculto de aprovação.

A CLI informa ao sketch o diretório de saída e a revisão Git atual. Compare máquinas e configurações
equivalentes. Um smoke test bem-sucedido com intervalos muito curtos comprova a orquestração, não a
estabilidade da performance.
