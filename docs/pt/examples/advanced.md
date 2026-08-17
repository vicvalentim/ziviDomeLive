# Exemplos Avançados de Aprendizagem

Estes exemplos apresentam padrões de projetos criativos maiores, preservando os mesmos contratos de Scene e render thread introduzidos pelos exemplos fundamentais.

<div class="zd-image-placeholder" markdown>
**PLACEHOLDER DE IMAGEM — Exemplos avançados de aprendizagem**
Captura final: montagem de `InfiniteBackground`, `FulldomePBR` e `SolarSystem` a partir da biblioteca instalada.
Asset final sugerido: `docs/img/learning-examples-advanced.png`
</div>

## InfiniteBackground

Qualifica o Environment LDR compartilhado com fontes equiretangulares real e sintética. O exemplo exercita visibilidade, intensidade, offset de longitude, oclusão em far depth e orientação Standard/esférica sem geometria sky pertencente à cena.

## FulldomePBR

Demonstra geometria `PShape` retida, shader GLSL 4.10 metallic-roughness, fallback fixed-function e `OrbitCamera` compartilhado em scene space. A câmera transforma o conteúdo sem alterar pitch/yaw/roll esféricos, e a cena libera o input de câmera ao ser descartada.

## SolarSystem

Aplicação em múltiplos arquivos e consumidor de referência de `SceneServices`. Ela usa relógio da biblioteca, timeline fixed-step limitada, assets tipados, ações nomeadas, reload diferido, cleanup, tracking do `OrbitCamera` e Environment no escopo da cena. A câmera oferece reset, drag, zoom pela roda e orientação quaternion; a mesma orientação controla o fundo infinito sem sky sphere pertencente à cena. Julian Date e propagação orbital continuam como adaptadores do domínio da aplicação. Pressione `n` para alternar o diagnóstico UTC, desabilitado por padrão.

Os exemplos avançados de aprendizagem seguem a mesma regra de ownership: `sceneRender(PGraphicsOpenGL)` desenha em um target já aberto e não chama `beginDraw()` nem `endDraw()`.

<div class="zd-actions" markdown>
[Exemplos Fundamentais](basic.md){ .md-button }
[CalibrationTool](../qualification/calibration-tool.md){ .md-button }
[BenchmarkTool](../qualification/benchmark-guide.md){ .md-button }
</div>
