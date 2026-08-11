# Exemplos Avançados

## CompatibilityLock

Chart de calibração estático renderizado nas seis direções do cubemap. Grades 12 x 12, molduras de safe area, círculos, miras, marcadores assimétricos de orientação, barras de cor em GLSL 4.10 e escalas de cinza com nove níveis permitem verificar identidade das faces, alinhamento, espelhamento, cor, luminância, rotações de 90 graus, FOV, Size% e roteamento de saída. Pressione `0` para restaurar o estado canônico; os controles aceitos imprimem seus valores no console.

## FulldomePBR

Demonstra geometria `PShape` retida, shader GLSL 4.10 metallic-roughness, fallback fixed-function e `OrbitCamera` compartilhado em scene space. A câmera transforma o conteúdo sem alterar pitch/yaw/roll esféricos, e a cena libera o input de câmera ao ser descartada.

## SolarSystem

Aplicação em múltiplos arquivos com modelos de domínio, tempo de simulação, shaders, texturas, controle de câmera e integração com `Scene`.

Exemplos avançados seguem a mesma regra de ownership: `sceneRender(PGraphicsOpenGL)` desenha em um target já aberto e não chama `beginDraw()` nem `endDraw()`.
