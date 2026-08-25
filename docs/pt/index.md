---
title: ziviDomeLive
icon: material/home-outline
description: Crie fluxos visuais fulldome, esféricos e imersivos em tempo real no Processing.
---

<div class="zd-hero" markdown>
<div markdown>

<div class="zd-hero__eyebrow">Biblioteca Processing · ziviDomeLive 2.0</div>

# Crie para o domo, a esfera e a imagem ao vivo

Construa fluxos visuais **fulldome, esféricos e imersivos em tempo real** a partir de uma única Scene do Processing e escolha como cada preview ou output será representado.

<div class="zd-actions" markdown>
[Comece a criar](getting-started/quickstart.md){ .md-button .md-button--primary }
[Explore a API](api/artist-api-map.md){ .md-button }
</div>

</div>
<div class="zd-hero__image">
<div class="zd-splash-stage" data-zd-splash>
<canvas class="zd-splash-canvas" data-zd-splash-canvas width="566" height="480" role="img" aria-label="Esfera animada do splash do ziviDomeLive com cubos wireframe orbitais">Esfera animada do splash do ziviDomeLive</canvas>
</div>
</div>
</div>

## O que posso criar?

<div class="grid cards" markdown>

- :material-monitor: **Standard**

    Renderização em perspectiva para a janela do Processing e outputs visuais convencionais.

- :material-panorama-fisheye: **Domemaster**

    Representação fisheye circular para projeção fulldome e calibração do domo.

- :material-earth: **Equirectangular**

    Representação esférica 2:1 para fluxos de imagem 360°.

- :material-cube-outline: **Skybox**

    Layout de cubemap para inspeção e fluxos esféricos compatíveis.

</div>

!!! tip "Comece com uma Scene"
    Um projeto básico precisa do runtime `ziviDomeLive` e de uma `Scene`. Coloque estado/simulação em `update()` e desenho em `sceneRender()`.

## Versão e compatibilidade

ziviDomeLive é uma biblioteca para Processing 4 destinada a fluxos visuais Standard, fulldome e esféricos em tempo real, incluindo representações Domemaster, Equirectangular e Skybox, gerenciamento de cenas, calibração esférica e roteamento opcional de outputs NDI, Syphon e Spout.

| Item | Informação atual |
|---|---|
| Versão | **2.0.0** |
| Última atualização | **24 de agosto de 2026** |
| Compatibilidade com Processing | Processing 4, revisão `1285+` |
| Versão usada nos testes automatizados | **Processing 4.5.6** |
| Sistemas qualificados fisicamente | macOS Apple Silicon `arm64`; macOS Intel `x86_64`; Windows `x86_64`; Linux `x86_64` |
| Categorias | `3D`, `Video & Vision` |
| Palavras-chave | `fulldome`, `projection`, `immersive`, `spherical`, `360`, `visual`, `real-time`, `NDI`, `Syphon`, `Spout` |
| Dependência Processing obrigatória | ControlP5 `2.2.6` para os exemplos distribuídos e painel de controle |
| Dependências opcionais de output | NDI Runtime; Syphon for Processing `4.0` no macOS; Spout for Processing `2.0.8.0` no Windows |

**Exemplos e ferramentas distribuídos:** `EmptyProject`, `Basic`, `NamedActions`, `PortLoopback`, `SphereParticle`, `InfiniteBackground`, `FulldomePBR`, `SolarSystem`, `CalibrationTool` e `BenchmarkTool`.

[Baixar ziviDomeLive.zip](https://github.com/vicvalentim/ziviDomeLive/releases/latest/download/ziviDomeLive.zip){ .md-button .md-button--primary }
[Detalhes de instalação](installation/installation-steps.md){ .md-button }
[Dependências](installation/dependencies.md){ .md-button }

O registro de qualificação da release cobre as quatro configurações desktop acima, os dez exemplos e ferramentas distribuídos, renderização Standard e esférica, calibração, instalação do pacote Processing e os outputs nativos aplicáveis a cada plataforma. Para uso em produção, qualifique a combinação específica de GPU, driver, projetor, lente e receptor.

## Escolha seu percurso

<div class="grid cards" markdown>

- :material-rocket-launch-outline: **Primeiro contato com ziviDomeLive**

    Instale a biblioteca, execute o Guia Rápido e avance pelos oito exemplos de aprendizagem.

    [Abrir o Guia Rápido →](getting-started/quickstart.md)

- :material-palette-outline: **Criando uma obra ou instalação**

    Aprenda RenderMode, Preview × Output, calibração esférica, câmera/navegação e outputs externos.

    [Abrir o Guia Criativo →](usage/basic-usage.md)

- :material-api: **Programando com a biblioteca**

    Comece pelo Mapa da API para Artistas e use os Javadocs gerados para assinaturas exatas.

    [Abrir o Mapa da API →](api/artist-api-map.md)

- :material-source-branch: **Contribuindo ou pesquisando o engine**

    Estude os domínios Standard/Esférico, backend OpenGL, lifecycle, threading e fronteiras de output.

    [Abrir a arquitetura →](architecture/overview.md)

</div>

## Calibração pertence ao output, não ao zoom da cena

Pitch/Yaw/Roll orientam o domínio esférico compartilhado. O Domemaster utiliza adicionalmente FOV e Size% para adequar a representação ao sistema físico de projeção.

[Abrir Calibração Esférica](usage/spherical-calibration.md){ .md-button }

??? abstract "Por dentro"
    A versão 2.0 captura o domínio esférico em um cubemap nativo e deriva Domemaster, Equirectangular e Skybox dessa representação compartilhada. Esse detalhe de implementação pertence ao percurso de desenvolvimento; artistas podem permanecer no nível de `Scene`, `RenderMode`, `ViewType` e calibração.
