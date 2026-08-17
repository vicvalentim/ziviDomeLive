---
title: "API de Renderização"
icon: material/api
status: stable
---
# API de Renderização

## RenderMode

`RenderMode` responde: **Como quero que o runtime trabalhe agora?**

- `FULL`
- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

`FULL` é o modo de trabalho com múltiplas rotas. Modos dedicados forçam temporariamente a representação efetiva e não apagam as seleções de preview/output armazenadas para `FULL`.

## ViewType

`ViewType` responde: **Qual representação final este destino deve receber?**

- `STANDARD`
- `DOMEMASTER`
- `EQUIRECTANGULAR`
- `SKYBOX`

Não documente `ViewType` como outro modo de runtime: ele representa uma rota de saída.

## Resolução e calibração

A fachada consolidada usa `resetGraphics(int)` para solicitar recriação dos targets de output. Documentação e exemplos devem preservar esse nome de API implementado.

A calibração esférica usa Pitch/Yaw/Roll; Domemaster acrescenta FOV e Size%. Movimento de câmera e orientação esférica são operações distintas.

## Acesso direto a renderers

Implementações de renderer permanecem públicas por compatibilidade/uso avançado, mas não são o ponto inicial recomendado para sketches comuns. Ownership e invalidação de targets importam nesse uso; consulte Javadocs e Developer Guide.
