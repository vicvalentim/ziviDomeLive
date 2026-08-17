---
title: "API Pública Avançada"
icon: material/api
status: advanced
---
# API Pública Avançada

A API pública avançada é **superfície Java pública chamável**, mas não é necessária para uma primeira cena ziviDomeLive.

## Scene services

`SceneServices` fornece serviços associados ao lifecycle para cenas que optam por recebê-los em `Scene.configure(SceneServices)`. Cenas simples podem ignorá-lo completamente.

Recursos públicos relacionados incluem serviços de frame/tempo como `FrameClock` e `SimulationTimeline`, além dos serviços de projeto descritos no guia Scene Services.

## Auxiliares de câmera e orientação

`OrbitCamera` é um helper opcional de câmera em espaço de cena. `SphericalOrientation` representa estado de orientação/calibração esférica. Mantenha os conceitos separados: mover a câmera da cena não equivale a rotacionar a representação esférica.

## Implementações públicas de renderer

Renderers Standard, cubemap e de projeção são expostos para compatibilidade/integração avançada. O uso direto transfere ao chamador preocupações de lifecycle e ownership de targets gráficos. Prefira a fachada `ziviDomeLive` salvo necessidade real de controle direto.
