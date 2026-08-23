# Exemplos de Aprendizagem — Fundamentos

Estes exemplos formam o primeiro percurso de aprendizagem da biblioteca. Ferramentas de qualificação são documentadas separadamente porque sua finalidade é testar uma release ou configuração de hardware, e não ensinar o modelo básico de projeto.

!!! note "Política de evidência visual"
    Screenshots usadas como evidência de release devem vir do pacote instalado e qualificado. Esta página de aprendizagem usa o comportamento do código-fonte como autoridade e não substitui captura real por mockup editorial.

## EmptyProject

Template inicial mínimo em múltiplos arquivos com uma `Scene`, um `SceneManager`, hooks automáticos do Processing, `sceneRender()` intencionalmente vazio e `draw()` também vazio.

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

## SphereParticle

Exemplo de cena maior que executa trabalho de simulação bounded por `SceneServices.tasks()`, isola resultados na ativação atual e mantém chamadas gráficas na render thread. Clique ou arraste para adicionar partículas.

Todos os exemplos de aprendizagem preservam `sceneRender(PGraphicsOpenGL)` e nunca chamam `ziviDome.draw()` manualmente.

<div class="zd-actions" markdown>
[Exemplos Avançados de Aprendizagem](advanced.md){ .md-button .md-button--primary }
[BenchmarkTool](../qualification/benchmark-guide.md){ .md-button }
</div>
