---
title: "Publicação como Processing Contributed Library"
icon: material/check-decagram-outline
status: qualification
---
# Publicação como Processing Contributed Library

Este checklist pertence à superfície de **manutenção/publicação**, não ao percurso de aprendizagem do artista.

## Artefato de publicação

O pacote Processing deve ser autocontido para instalação, exemplos, source, referência de API, licenças e citação. MkDocs é o manual técnico; o futuro GitBook não é dependência do pacote.

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
- [ ] `examples/` com seis learning examples + `CalibrationTool` + `BenchmarkTool`
- [ ] `src/` sem `src/test/`
- [ ] `library.properties`
- [ ] licença e notices de terceiros
- [ ] metadata de citação conforme a task de packaging corrente

Não deve conter relatórios locais de benchmark, evidência maintainer-only, `.DS_Store`, testes ou helper JARs locais excluídos pelo contrato de release.

Abra/execute os oito exemplos **a partir do pacote instalado**.

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
