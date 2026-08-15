# Relatórios de Benchmark

A ferramenta de relatório, exclusiva para desenvolvimento, valida e agrega execuções schema v1 do
BenchmarkTool. Ela usa apenas o JDK, grava um dashboard estático offline e permanece isolada do JAR
da biblioteca Processing e do pacote de release.

## Captura e Relatório

Configure o BenchmarkTool para exportar no repositório antes de iniciar o sketch Processing:

```bash
export ZIVIDOME_BENCHMARK_OUTPUT="$PWD/build/benchmark-results"
```

Após exportar uma ou mais execuções, gere o relatório:

```bash
./gradlew benchmarkReport
```

A tarefa lê os diretórios de execução imediatamente abaixo de `build/benchmark-results/`, valida
`summary.json`, `environment.json` e cada linha de `frames.csv`, e então grava:

- `build/reports/benchmark/index.html`: dashboard autocontido em HTML/CSS/SVG;
- `build/reports/benchmark/data.json`: schema v1 do relatório para automação;
- `build/reports/benchmark/summary.md`: resumo auditável compacto.

Execuções inválidas, incompatíveis, incompletas ou representadas por links simbólicos são excluídas
e identificadas no relatório. Um diretório ausente ou vazio produz um relatório vazio válido, sem
reaproveitar dados antigos.

## Baseline e Candidate

Informe nomes dos diretórios de execução, ou caminhos cujo último componente seja esse nome:

```bash
./gradlew benchmarkReport \
  -PbenchmarkBaseline=<run-baseline> \
  -PbenchmarkCandidate=<run-candidate>
```

A comparação mostra ambos os valores, delta absoluto, delta percentual, direção esperada e o
resultado direcional. Tempos de frame menores são melhores (média, P50, P95, P99 e máximo); FPS
médio e FPS 1% low maiores são melhores. A ferramenta classifica melhoria, regressão ou igualdade
somente a partir dessa direção e deliberadamente não aplica limiares arbitrários de aceitação.

Compare cenários e ambientes equivalentes. Tempos OpenGL observados pela CPU podem incluir esperas
do driver, e medições de outputs externos não comprovam apresentação no receptor nem latência de rede.

## Tarefas de Apoio

```bash
./gradlew benchmarkOpen
./gradlew benchmarkArchive
./gradlew benchmarkClean
```

`benchmarkOpen` regenera e abre o `index.html` quando há integração com desktop; caso contrário,
exibe o caminho absoluto. `benchmarkArchive` regenera o relatório e cria em
`build/benchmark-archives/` um ZIP com timestamp contendo entradas e saídas. `benchmarkClean` remove
explicitamente as execuções capturadas e os relatórios; use-o somente após arquivar as evidências que
precisam ser preservadas.
