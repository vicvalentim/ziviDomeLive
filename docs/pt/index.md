# ziviDomeLive

**Crie visuais fulldome, esféricos e imersivos em tempo real no Processing.**

O ziviDomeLive permite apresentar uma mesma cena Processing como perspectiva Standard, Domemaster, Equirectangular ou Skybox. É possível trabalhar interativamente na janela do Processing, calibrar a saída esférica para domo, gerenciar múltiplas cenas e, opcionalmente, publicar representações selecionadas via NDI, Syphon ou Spout.

![Visão geral orientada ao artista](../img/hero-overview.png)

## Comece aqui

Se este é seu primeiro projeto, siga o [Guia Rápido](getting-started/quickstart.md). Uma cena simples precisa apenas da instância da biblioteca, `setup()`, `update()` quando o estado muda no tempo e `sceneRender()` para desenho.

## Escolha uma representação

![Visão geral dos modos](../img/render-modes-overview.png)

| Representação | Uso típico |
|---|---|
| Standard | Perspectiva na janela do Processing e saída visual convencional |
| Domemaster | Imagem fisheye para projeção fulldome |
| Equirectangular | Fluxos esféricos/360° em 2:1 |
| Skybox | Layout de cubemap e inspeção |

`RenderMode.FULL` mantém preview e outputs habilitados roteáveis independentemente por `ViewType`. Modos dedicados são modos de trabalho temporários e não apagam rotas armazenadas.

## Calibre para o domo

Use Pitch/Yaw/Roll para orientar o domínio esférico compartilhado, FOV para definir o campo angular do Domemaster e Size% para ajustar a imagem circular ao conjunto projetor/lente. Size% é calibração física da saída, não zoom da cena.

## Aprenda pelos exemplos

Use os seis exemplos de aprendizagem em ordem crescente de complexidade e depois avance para as ferramentas de qualificação quando precisar validar uma instalação ou produzir evidências de desempenho.

## API e material de desenvolvimento

A [Referência da API](api/overview.md) documenta contratos chamáveis. O Guia do Desenvolvedor explica a arquitetura Standard/Spherical, backend OpenGL, lifecycle, threading e internals de output. Esses detalhes não são pré-requisitos para criação artística.

### Sob o capô

A versão 2.0 usa um cubemap nativo como fonte compartilhada das projeções esféricas. Esse é um detalhe de implementação: os contratos para artistas são as representações finais, o roteamento, a orientação e a calibração.
