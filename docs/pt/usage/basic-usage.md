# RenderMode e ViewType

O ziviDomeLive separa **como a aplicação está trabalhando agora** de **qual representação um destino recebe**.

![RenderMode e ViewType](../../img/render-modes-overview.png)

## RenderMode: como quero trabalhar agora?

`RenderMode` define o modo global efetivo:

- `FULL`
- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

`FULL` é o padrão e preserva as rotas independentes de preview e outputs configuradas por `ViewType`.

Modos dedicados substituem temporariamente a representação efetiva. Eles **não** apagam as rotas armazenadas que reaparecem ao retornar a `FULL`.

## ViewType: qual representação vai para este destino?

`ViewType` identifica a representação final solicitada pelo preview ou por um output externo:

- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

A distinção é especialmente importante em `FULL`: um preview Standard pode coexistir, por exemplo, com uma saída NDI Domemaster sem transformar os dois destinos na mesma rota.

## O que RenderMode não significa

Um modo de renderização não é uma segunda classe de runtime e não substitui a instância `ziviDomeLive`. O modelo público continua sendo um único runtime com múltiplos modos de trabalho.
