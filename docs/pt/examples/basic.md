# Exemplos de Aprendizagem — Fundamentos

Estes exemplos formam o primeiro percurso de aprendizagem da biblioteca. Ferramentas de qualificação são documentadas separadamente porque sua finalidade é testar uma release ou configuração de hardware, e não ensinar o modelo básico de projeto.

!!! note "Política de evidência visual"
    Screenshots usadas como evidência de release devem vir do pacote instalado e qualificado. Esta página de aprendizagem usa o comportamento do código-fonte como autoridade e não substitui captura real por mockup editorial.

## EmptyProject

Template inicial mínimo em múltiplos arquivos com uma `Scene` pertencente à facade, hooks automáticos do Processing, `sceneRender()` intencionalmente vazio e `draw()` também vazio.

Comece aqui ao criar um projeto do zero.

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

## NamedActions

Demonstra actions de key-code e mouse pertencentes à ativação, registro nomeado e trigger
programático. As actions movem uma esfera iluminada numa composição 3D. A chamada explícita
`applyWithViewLighting(...)` prende um rig de luz ambiente/spot à câmera da cena e o aponta para o
target corrente. A distância orbital inicial negativa posiciona a composição no hemisfério frontal
do Domemaster sem alterar a calibração global de Pitch/Yaw/Roll. Arraste para orbitar, use a roda
do mouse ou trackpad para zoom e pressione `R` para restaurar a câmera centralizada; o clique move
a esfera sem disputar o gesto de arrasto da navegação.

## PortLoopback

Demonstra o SPI bounded de adapters `ScenePorts` sem adicionar dependência de transporte.
Mensagens inteiras controlam um anel 3D de sinais, e um adapter de output não bloqueante relata o
nível aplicado. Sua câmera adota a mesma convenção de distância negativa centralizada no
Domemaster: arraste para orbitar, use a roda do mouse ou trackpad para zoom e pressione `R` para
restaurar a vista inicial.

Todos os exemplos de aprendizagem preservam `sceneRender(PGraphicsOpenGL)` e nunca chamam `ziviDome.draw()` manualmente.

<div class="zd-actions" markdown>
[Exemplos Avançados de Aprendizagem](advanced.md){ .md-button .md-button--primary }
[BenchmarkTool](../qualification/benchmark-guide.md){ .md-button }
</div>
