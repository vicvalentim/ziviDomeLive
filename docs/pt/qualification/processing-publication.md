---
title: "Template do Procedimento de Publicação como Processing Contributed Library"
icon: material/check-decagram-outline
status: qualification
---
# Template do Procedimento de Publicação como Processing Contributed Library

Este checklist é um **template reutilizável do procedimento de manutenção/publicação**, não um
ledger de status nem parte do percurso de aprendizagem do artista. Registre o estado autoritativo
de cada execução em
[`maintainer/release-evidence.md`](https://github.com/vicvalentim/ziviDomeLive/blob/release/2.0.0/maintainer/release-evidence.md)
e no artefato de CI vinculado ao SHA de código-fonte qualificado. Nunca copie o estado de conclusão para
este template nem o deduza de uma qualificação antiga.

## Artefato de publicação

O pacote Processing deve ser autocontido para instalação, exemplos, source, referência de API, licenças e citação. MkDocs é o manual técnico; o futuro GitBook não é dependência do pacote.

## Mapeamento das diretrizes oficiais

### Homepage do projeto

- [ ] URL pública estável apresenta abstract conciso e audiência pretendida
- [ ] instalação separa Contribution Manager, artefato de release e build do source
- [ ] os dez exemplos/tools executáveis estão listados, com tutoriais/manual linkados quando úteis
- [ ] baseline Processing/Java, renderer, `pixelDensity(1)`, dependências e limitações estão declarados
- [ ] sistemas/versões Processing testados têm evidência ou aparecem explicitamente sem qualificação
- [ ] `library.keywords` e data de atualização documental estão visíveis
- [ ] latest stable e o estado 2.0 sem tag não são confundidos
- [ ] links da release estável usam o basename comum `ziviDomeLive` para ZIP/TXT/PDEX

### Pacote e referência

- [ ] Javadocs são gerados em `reference/index.html`
- [ ] source, examples, licença/notices e `library.properties` acompanham a library runtime
- [ ] nome da pasta raiz e basenames dos artefatos permanecem `ziviDomeLive`
- [ ] exemplos compilam contra o pacote instalado, não apenas o build do repositório
- [ ] a referência pública contém somente os tipos Stable, Advanced Stable e Experimental pretendidos

## AUTOMATED

- [ ] `./gradlew clean test build --console=plain`
- [ ] `./gradlew qualificationTests --console=plain`
- [ ] validator documental passa
- [ ] `python3 -m mkdocs build --strict` passa para EN/PT configurados
- [ ] `./gradlew buildReleaseArtifacts --console=plain`
- [ ] estrutura ZIP/PDEX passa na validação do pacote

## GPU VISUAL

- [ ] [CalibrationTool](calibration-tool.md) inspecionado em cada configuração usada como evidência GPU
- [ ] Domemaster, Equirectangular, Skybox e Standard conferidos
- [ ] Environment conferido conforme o contrato visual LDR equirectangular corrente

## BENCHMARK

- [ ] smoke do [BenchmarkTool](benchmark-guide.md) concluído
- [ ] baseline CPU registrado
- [ ] modo CPU/GPU registrado quando implementado pela ferramenta
- [ ] relatório identifica versão/commit, resolução, rotas, Processing/Java, OS e hardware

## NATIVE OUTPUT

Só declare backend/plataforma como **testado** após evidência end-to-end:

- [ ] receiver NDI em toda plataforma declarada para NDI
- [ ] receiver Syphon em cada configuração macOS declarada
- [ ] receiver Spout em cada configuração Windows declarada

Caminho de código suportado e plataforma testada na release são fatos diferentes.

## PACKAGE INSTALLATION

O pacote final deve conter:

- [ ] `library/`
- [ ] `reference/index.html`
- [ ] `examples/` com `GettingStarted/`, `Advanced/` e `Tools/`, contendo os oito learning examples + `CalibrationTool` + `BenchmarkTool`
- [ ] `src/` sem `src/test/`
- [ ] `library.properties`
- [ ] licença e notices de terceiros
- [ ] metadata de citação conforme a task de packaging corrente
- [ ] README, CHANGELOG, `CITATION.cff` e `THIRD_PARTY.md`

Não deve conter relatórios locais de benchmark, evidência maintainer-only, `.DS_Store`, testes ou helper JARs locais excluídos pelo contrato de release.

Abra/execute os dez exemplos/tools **a partir do pacote instalado**.

## PUBLICATION METADATA

- [ ] campos centrais de `library.properties` validados pelas regras/parser atuais do Processing
- [ ] keywords descrevem capacidades implementadas (sem claim VR/XR genérico)
- [ ] metadata de plataforma testada ausente sem evidência de qualificação
- [ ] boundary de Processing suportado/testado corresponde à realidade
- [ ] DOI do software consistente em CFF, Zenodo, README e MkDocs
- [ ] nenhum DOI/ISBN documental inventado

## Arquivos estáveis de release

O workflow publica os siblings gerados:

- `ziviDomeLive.zip`
- `ziviDomeLive.txt`
- `ziviDomeLive.pdex`

Valide o URL estável pretendido antes de submeter/atualizar a contribuição.

## Regra da tag

A tag é o ponto de publicação da release, **não a primeira execução de qualificação**. Evidências automated, GPU, benchmark, native output, package installation e publication metadata precisam estar concluídas antes de `v2.0.0`.
