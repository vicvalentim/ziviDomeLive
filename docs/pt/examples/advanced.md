# Exemplos Avançados

## CalibrationTool

Ferramenta de qualificação com duas cenas. A Cena 1 mapeia seis padrões GLSL 4.10 nas coordenadas locais explícitas `0..1` das faces de um cubo fechado; assim, grade 24 x 24, referências geométricas, linhas de foco, pontos, estrelas, degradês, amostras, escala de cinza, clipping e anotações acompanham como uma única superfície as transformações esféricas de pitch/yaw/roll. A Cena 2 seleciona um dos quatro padrões equiretangulares v14 originais e não modificados de Paul Bourke conforme o bucket de output 1k/2k/3k/4k ativo, ou o bucket mais próximo da janela quando os outputs estão desabilitados, e o mapeia numa esfera completa de 1800 unidades centrada em `(0, 0, 0)`, cujo polo norte é `+Z`. `Espaço` alterna a rotação recomendada de 60 segundos; `T` seleciona um perfil de rotação quantizado no tempo de 30 fps/1800 frames ou 60 fps/3600 frames sem alterar a taxa global do Processing; `,`/`.` avança um grau; e `C` restaura a orientação. Use as setas Esquerda/Direita para trocar de cena.

Siga o [Protocolo do Calibration Tool](../qualification/1.5-calibration-tool.md)
ao usar este exemplo como evidência de release.

## FulldomePBR

Demonstra geometria `PShape` retida, shader GLSL 4.10 metallic-roughness, fallback fixed-function e `OrbitCamera` compartilhado em scene space. A câmera transforma o conteúdo sem alterar pitch/yaw/roll esféricos, e a cena libera o input de câmera ao ser descartada.

## SolarSystem

Aplicação em múltiplos arquivos com modelos de domínio, tempo de simulação, shaders, texturas e integração com `Scene`. Ela usa o `OrbitCamera` da biblioteca para acompanhar alvos, reset, drag, zoom pela roda e orientação quaternion; a mesma orientação controla o Environment infinito sem sky sphere pertencente à cena.

Exemplos avançados seguem a mesma regra de ownership: `sceneRender(PGraphicsOpenGL)` desenha em um target já aberto e não chama `beginDraw()` nem `endDraw()`.
