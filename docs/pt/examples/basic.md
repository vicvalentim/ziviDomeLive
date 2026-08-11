# Exemplos Básicos

## Basic

Demonstra duas cenas gerenciadas e a API `RenderMode`. Em `Scene1`, pressione:

- `1`: `FULL`
- `2`: `STANDARD`
- `3`: `DOMEMASTER`
- `4`: `EQUIRECTANGULAR`
- `5`: `SKYBOX`
- `+` / `-`: ajusta a velocidade da animação

## EmptyProject

Template mínimo em múltiplos arquivos com uma `Scene`, um `SceneManager`, hooks automáticos do Processing e `draw()` intencionalmente vazio.

## SphereParticle

Exemplo maior que utiliza executor para simulação e mantém chamadas gráficas na render thread. Ele mostra por que mutações de estado devem permanecer fora do desenho executado para cada target.

Todos os exemplos preservam `sceneRender(PGraphicsOpenGL)` e nunca chamam `ziviDome.draw()` manualmente.
