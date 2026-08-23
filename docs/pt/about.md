---
title: Sobre o ziviDomeLive
description: Identidade, propósito, história, contexto de pesquisa, autoria, citação e licenciamento do projeto
icon: material/information-outline
tags:
  - Projeto
  - Creative coding
  - Software de pesquisa
---

# Sobre o ziviDomeLive

ziviDomeLive é uma biblioteca Java open source para **Processing 4** que transforma uma única cena de creative coding em representações Standard, Domemaster, Equirectangular e Skybox em tempo real. Ela atende artistas, creative coders, profissionais de planetário, pesquisadores, educadores, estudantes, VJs, developers e equipes de instalação que trabalham com imagens fulldome, esféricas e imersivas.

## Visão geral

| | Identidade do projeto |
|---|---|
| **Plataforma** | Processing 4 e Java 17 |
| **Domínio principal** | Renderização fulldome e esférica monoscópica em tempo real |
| **Representações** | Standard, Domemaster, Equirectangular e Skybox |
| **Licença** | GPL-2.0-only |
| **DOI do software** | [10.5281/zenodo.15671506](https://doi.org/10.5281/zenodo.15671506) |
| **Código-fonte** | [github.com/vicvalentim/ziviDomeLive](https://github.com/vicvalentim/ziviDomeLive) |

## Por que o projeto existe

Processing torna gráficos em tempo real acessíveis, mas um fluxo fulldome de produção precisa coordenar captura esférica, múltiplas projeções, calibração, lifecycle de cenas, ownership de recursos e publicação não bloqueante de outputs. ziviDomeLive oferece uma única fronteira orientada ao Processing para essas responsabilidades, mantendo o código da cena próximo de um sketch comum.

O projeto se concentra em **geração e roteamento de imagem**. Ele não é um runtime VR estereoscópico, engine para headsets, suíte de projection mapping ou framework genérico de aplicações.

## História e responsabilidade

O repositório preserva o percurso arquitetural desde o renderer 1.x original, passando pela consolidação 1.5, até o reset deliberado da API pública 2.0. Os tutoriais atuais descrevem somente comportamento chamável na 2.0; nomes históricos permanecem no [registro de migração 1.x](api/deprecated.md), nas [notas de versão](release-notes/2.0.0.md) e no changelog do projeto.

Claims técnicos deste site são governados por fontes, testes e exemplos executáveis. Evidências de qualificação e publicação ficam separadas da orientação para artistas, evitando confundir capacidade suportada, cobertura automatizada e plataformas verificadas fisicamente.

## Informações do projeto

<div class="grid cards" markdown>

-   :material-microscope: **Software de pesquisa**

    Declaração de necessidade, níveis de evidência, reprodutibilidade, prontidão JOSS e limites dos claims atuais de impacto em pesquisa.

    [Pesquisa e prontidão JOSS →](research-software.md)

-   :material-format-quote-close: **Citação**

    Cite a release do software com sua metadata legível por máquina e mantenha distintos os identificadores de software, documentação e futuro paper.

    [Orientação para citação →](citation.md)

-   :material-account: **Autor**

    Autoria do projeto, afiliações acadêmicas, website e informação ORCID.

    [Sobre o autor →](author.md)

-   :material-scale-balance: **Licença**

    Licença do projeto, avisos de dependências, assets incluídos e localização dos textos legais oficiais.

    [Licença e avisos →](license.md)

</div>

## Participe

Use o [guia de contribuição](contributing.md) para mudanças de código ou documentação, confira os [problemas conhecidos](known-issues.md) antes de relatar um bug e consulte o [roadmap](roadmap.md) para trabalhos deliberadamente fora do contrato 2.0 congelado.
