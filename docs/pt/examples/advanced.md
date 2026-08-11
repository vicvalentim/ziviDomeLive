# Exemplos Avançados

## CompatibilityLock

Cena estática assimétrica que identifica as seis direções do cubemap. Use-a para qualificar identidade de faces, layout, espelhamento, rotações de 90 graus, FOV, Size%, independência Standard e domemaster flutuante. Pressione `0` para restaurar a calibração canônica; controles aceitos imprimem seus valores no console.

## FulldomePBR

Demonstra geometria `PShape` retida, shader GLSL 4.10 metallic-roughness, fallback fixed-function e `OrbitCamera` compartilhado em scene space. A câmera transforma o conteúdo sem alterar pitch/yaw/roll esféricos, e a cena libera o input de câmera ao ser descartada.

## SolarSystem

Aplicação em múltiplos arquivos com modelos de domínio, tempo de simulação, shaders, texturas, controle de câmera e integração com `Scene`.

Exemplos avançados seguem a mesma regra de ownership: `sceneRender(PGraphicsOpenGL)` desenha em um target já aberto e não chama `beginDraw()` nem `endDraw()`.
