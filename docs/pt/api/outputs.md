---
title: "API de Outputs"
icon: material/api
status: stable
---
# API de Outputs

Use `OutputManager` por meio de `ziviDomeLive.getOutputManager()` para o roteamento de outputs externos.

## ViewType por destino

A superfície corrente inclui seletores específicos por destino, entre eles:

- `setNdiView(ViewType)`
- `setSyphonView(ViewType)`
- `setSpoutView(ViewType)`
- `setLocalTextureView(ViewType)`

Esse é o modelo preferencial em `RenderMode.FULL`: cada destino pode solicitar sua própria representação final.

## Habilitar e inspecionar

`toggleOutput(String)` aceita os nomes de backend documentados `"ndi"`, `"syphon"` e `"spout"`.

Use acessores de estado/falha como `getOutputState(...)` e `getOutputFailureReason(...)` em vez de assumir que um toggle da interface comprova inicialização bem-sucedida.

## Roteamento genérico deprecated

O método de compatibilidade `setView(ViewType)` é deprecated. Código novo deve selecionar explicitamente a view do destino pretendido.

Detalhes de worker/buffers/GL pertencem ao Developer Guide. O contrato para artistas é destino → `ViewType` → habilitar → inspecionar estado → testar com receiver real quando aplicável.
