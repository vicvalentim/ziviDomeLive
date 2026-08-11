# Exemplos Básicos

## Basic

Demonstra duas cenas gerenciadas e a API `RenderMode`. Em `Scene1`, pressione:

- `1`: `FULL`
- `2`: `STANDARD`
- `3`: `DOMEMASTER`
- `4`: `EQUIRECTANGULAR`
- `5`: `SKYBOX`
- `+` / `-`: ajusta a velocidade da animação
- `r`: restaura a velocidade da animação
- Roda do mouse: altera o raio da órbita dos pilares

Use as setas Esquerda/Direita para alternar entre os pilares rotativos e a grade estática de alinhamento com seis faces.

## EmptyProject

Template inicial mínimo em múltiplos arquivos com uma `Scene`, um `SceneManager`, hooks automáticos do Processing, `sceneRender()` intencionalmente vazio e `draw()` também vazio.

## SphereParticle

Exemplo de cena maior que utiliza um executor para a simulação de partículas e mantém as chamadas gráficas na render thread. Clique ou arraste para adicionar partículas.

Todos os exemplos preservam `sceneRender(PGraphicsOpenGL)` e nunca chamam `ziviDome.draw()` manualmente.

Comece em `EmptyProject`, use `Basic` para aprender troca de cenas e modos de
renderização e então consulte `SphereParticle` para a fronteira entre simulação
em background e desenho na render thread.
