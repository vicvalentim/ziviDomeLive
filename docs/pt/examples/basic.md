# Exemplos Básicos

## Basic

Demonstra duas cenas gerenciadas e a API `RenderMode`. Em `Scene1`, pressione:

- `1`: `FULL`
- `2`: `STANDARD`
- `3`: `DOMEMASTER`
- `4`: `EQUIRECTANGULAR`
- `5`: `SKYBOX`
- `+` / `-`: ajusta a velocidade da animação
- Roda do mouse: altera o raio da órbita dos pilares

Use as setas Esquerda/Direita para alternar entre os pilares orbitais e o cubo de orientação identificado. Pressione `R` em qualquer cena para restaurar os padrões da animação.

## EmptyProject

Template mínimo em múltiplos arquivos com uma cena registrada por `setScene`, hooks automáticos do Processing, um cubo de referência visível e `draw()` intencionalmente vazio. Pressione `R` para restaurar a rotação do cubo.

## SphereParticle

Campo de partículas limitado que avança a simulação uma vez em `update()` e executa apenas operações gráficas em `sceneRender()`. Clique ou arraste para adicionar rajadas, pressione `Espaço` para uma rajada central, `C` para limpar e `R` para restaurar o campo inicial.

Todos os exemplos preservam `sceneRender(PGraphicsOpenGL)` e nunca chamam `ziviDome.draw()` manualmente.
