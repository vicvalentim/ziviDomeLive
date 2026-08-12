# Modos de Renderização

`RenderMode` controla a representação efetiva usada pela janela Processing e por cada output externo habilitado. Ele não seleciona um backend de output e não substitui a API de routing por `ViewType`.

## Modo de Compatibilidade FULL

`FULL` é o padrão. Sketches existentes que nunca chamam `setRenderMode()` mantêm rotas independentes para preview e outputs:

```java
dome.setRenderMode(RenderMode.FULL);
dome.setCurrentView(ViewType.STANDARD);

OutputManager outputs = dome.getOutputManager();
outputs.setNdiView(ViewType.EQUIRECTANGULAR);
outputs.setSyphonView(ViewType.DOMEMASTER);
outputs.setSpoutView(ViewType.SKYBOX);
```

Somente outputs habilitados solicitam frames. Configurar uma rota ou preparar Syphon/Spout não ativa publicação nem adiciona requisito de renderização.

## Modos Dedicados

Modos dedicados forçam uma representação efetiva para o preview principal e todos os outputs habilitados:

| `RenderMode` | `ViewType` efetivo | Pipeline principal |
|---|---|---|
| `STANDARD` | `STANDARD` | Renderer Standard perspectiva direto |
| `DOMEMASTER` | `DOMEMASTER` | Cubemap, fisheye samplerCube |
| `EQUIRECTANGULAR` | `EQUIRECTANGULAR` | Cubemap, equiretangular |
| `SKYBOX` | `SKYBOX` | Cubemap, layout skybox |

```java
dome.setRenderMode(RenderMode.DOMEMASTER);
```

Os valores `ViewType` configurados para preview e outputs permanecem armazenados durante um modo dedicado. Voltar para `FULL` restaura essas rotas independentes:

```java
dome.setRenderMode(RenderMode.FULL);
```

## Domemaster Flutuante

A miniatura fisheye flutuante é um serviço auxiliar de preview:

```java
dome.setRenderMode(RenderMode.STANDARD);
dome.setShowPreview(true);
```

Essa combinação renderiza intencionalmente o caminho Standard e os passes esféricos exigidos pela miniatura. Nos demais modos dedicados o serviço ainda pode ser habilitado por API, mas o painel interno oculta seu toggle redundante.

## Requisitos de Renderização

A biblioteca calcula o fechamento de dependências a cada frame:

```text
Standard                 -> somente Standard
Layout cubemap           -> captura cubemap + layout
Equirectangular          -> captura cubemap + equiretangular
Domemaster fisheye       -> captura cubemap + fisheye samplerCube
```

Quando outputs habilitados solicitam views diferentes em `FULL`, seus requisitos são combinados. No máximo um cubemap mestre é capturado por frame. Consulte [Pipeline de Renderização](../architecture/rendering-pipeline.md) para a ordem completa.

## Domínios de Resolução

O preview Standard usa as dimensões atuais da janela Processing. Targets esféricos de preview usam:

```text
min(1024, max(256, min(windowWidth, windowHeight)))
```

Targets de output externo usam a resolução independente de output:

```java
dome.resetGraphics(2048);
```

Os presets do painel são `1024`, `2048`, `3072` e `4096`. A realocação é adiada para o draw loop, afeta somente targets de output e preserva o Size% do domemaster.

## Guias Relacionados

- [Painel de Controle](control-panel.md)
- [Calibração Esférica](spherical-calibration.md)
- [Integração Externa](external-integration.md)
- [Lifecycle de Runtime](../architecture/runtime-lifecycle.md)
