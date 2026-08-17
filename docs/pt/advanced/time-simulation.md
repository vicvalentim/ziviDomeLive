---
title: "Tempo e Simulação"
icon: material/layers-triple-outline
---
# Tempo e Simulação

Use serviços explícitos de tempo/simulação apenas quando o projeto precisar de timing determinístico ou associado ao lifecycle além de estado simples em `Scene.update()`. Todo estado que deve avançar uma vez por Processing frame permanece fora de `sceneRender()`. Consulte os Javadocs para métodos exatos de `FrameClock`/`SimulationTimeline` em 2.0.
