# Exemplos Avançados

## CalibrationTool

Ferramenta de qualificação com duas cenas. A Cena 1 mapeia seis padrões GLSL 4.10 nas coordenadas locais explícitas `0..1` das faces de um cubo fechado; assim, grade 24 x 24, referências geométricas, linhas de foco, pontos, estrelas, degradês, amostras, escala de cinza, clipping e anotações acompanham como uma única superfície as transformações esféricas de pitch/yaw/roll. A Cena 2 mapeia o padrão equiretangular v14 original e não modificado de Paul Bourke, com 8192 x 4096 pixels, numa esfera completa de 1800 unidades centrada em `(0, 0, 0)`, cujo polo norte é `+Z`; `Espaço` alterna a rotação recomendada de 60 segundos, `,`/`.` avança um grau e `C` restaura a orientação. Use as setas Esquerda/Direita para trocar de cena.

## FulldomePBR

Demonstra geometria `PShape` retida, shader GLSL 4.10 metallic-roughness, fallback fixed-function e `OrbitCamera` compartilhado em scene space. A câmera transforma o conteúdo sem alterar pitch/yaw/roll esféricos, e a cena libera o input de câmera ao ser descartada.

## SolarSystem

Aplicação em múltiplos arquivos com modelos de domínio, tempo de simulação, shaders, texturas, controle de câmera e integração com `Scene`.

Exemplos avançados seguem a mesma regra de ownership: `sceneRender(PGraphicsOpenGL)` desenha em um target já aberto e não chama `beginDraw()` nem `endDraw()`.
