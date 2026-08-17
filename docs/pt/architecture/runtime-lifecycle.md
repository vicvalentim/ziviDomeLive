---
title: "Lifecycle e Ownership de Recursos"
icon: material/source-branch
---
# Lifecycle e Ownership de Recursos

Lifecycle é parte da corretude porque recursos Processing/OpenGL pertencem a um contexto e cenas/outputs podem ser ativados, redimensionados e liberados.

Cenas podem receber services, setup, update/render e `dispose()` em troca, limpeza, substituição ou release. Uma nova ativação pode configurar novamente.

`resetGraphics(int)` solicita recriação dos targets no boundary apropriado. A biblioteca controla o draw frame do `PGraphicsOpenGL` passado a `sceneRender()`; a cena não chama `beginDraw()`/`endDraw()` e não deve reter o target como estado próprio.