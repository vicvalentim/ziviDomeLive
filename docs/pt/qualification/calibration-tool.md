---
title: CalibrationTool
icon: material/axis-arrow
status: qualification
---
# CalibrationTool

`CalibrationTool` é o **instrumento visual corrente de qualificação** do ziviDomeLive. Não é um exemplo de aprendizagem e não substitui o protocolo histórico da 1.5. Use-o para inspecionar orientação esférica, mapeamento das projeções, foco, cor, controles de calibração e comportamento dos outputs no sistema real de destino.

!!! warning "A captura deve ser evidência"
    A imagem final desta página deve vir do pacote 2.0 instalado e qualificado, registrando cena ativa, `ViewType`, resolução e estado de calibração. Mockup editorial não vale como evidência de qualificação.

A ferramenta inicia em **Paul Bourke Environment Background**, na view `DOMEMASTER`, com FOV `210°`, Size `100%` e pitch/yaw/roll globais em `0°`. Um giro fixo de `90°` no eixo X é aplicado diretamente ao lookup da imagem do Environment, mantendo o alinhamento da imagem independente da orientação do domo e da geometria da cena. A tecla `0` restaura exatamente esse estado inicial.

## Cube Focus and Color

Seis targets GLSL 4.10 formam um cubo fechado ao redor do observador. Cada face utiliza coordenadas locais explícitas `0..1`, fazendo grades, referências geométricas, elementos de foco, rampas de cor e anotações comporem uma única superfície contínua de calibração esférica.

A cena contém:

- grade 24 × 24 com divisões em quartos e limites de face;
- safe areas, círculos concêntricos, raios e crosshairs centrais;
- pares de linhas de 1, 2, 4 e 8 pixels;
- pontos de 1, 2, 3 e 4 pixels, starbursts e campo estelar determinístico;
- referências RGB/CMY/branco/preto;
- escala de cinza contínua, níveis discretos e patches próximos de preto/branco para clipping;
- índice da face, eixo, direção, coordenadas da grade e marcadores `UP` e `R`.

Recursos dimensionados em pixels são exatos apenas quando uma face do cubemap é amostrada um para um. A degradação através de outra projeção, resolução, codec ou receiver faz parte do que a ferramenta deve revelar.

## Cena inicial — Paul Bourke como Environment Background

A cena inicial fornece um dos quatro padrões equiretangulares v14 originais e não modificados de Paul Bourke por `SceneEnvironmentService`. O passe da biblioteca em profundidade infinita é invariável à translação, compõe o alinhamento da imagem independentemente da orientação esférica compartilhada e usa a mesma fonte nas views Standard, Domemaster, Equirectangular e Skybox.

A fonte acompanha o bucket de resolução do output ativo quando há output externo habilitado. Com outputs desabilitados, o bucket mais próximo é selecionado a partir da janela do Processing.

| Bucket de render | Fonte usada pelo exemplo |
|---|---|
| 1024 (1k) | `spherical2400.png` (2400 × 1200) |
| 2048 (2k) | `spherical4096.png` (4096 × 2048) |
| 3072 (3k) | `spherical4800.png` (4800 × 2400) |
| 4096 (4k) | `spherical8192.png` (8192 × 4096) |

A cena Paul Bourke também oferece rotação lenta e quantizada no tempo para observar aliasing e descontinuidade de playback:

- `Espaço`: alterna uma revolução a cada 60 segundos;
- `T`: alterna entre 30 fps / 1800 posições e 60 fps / 3600 posições;
- `,` / `.`: avança para trás / para frente um grau e pausa;
- `C`: restaura a orientação da fonte e pausa;
- `V`: alterna a visibilidade do Environment;
- `D` / `B`: diminui / aumenta a intensidade do Environment.

O perfil de rotação não altera a taxa global de frames do Processing.

## Controles compartilhados

Use as setas Esquerda/Direita para alternar as cenas.

| Controle | Efeito |
|---|---|
| `1` | ViewType Domemaster |
| `2` | ViewType Equirectangular |
| `3` | ViewType Skybox |
| `4` | ViewType Standard |
| `[` / `]` | Diminui / aumenta Size% do Domemaster em 10 |
| `-` / `+` | Diminui / aumenta o FOV em 10° |
| `P` | Soma 90° de pitch |
| `Y` | Soma 90° de yaw |
| `R` | Soma 90° de roll |
| `F` | Alterna o preview Domemaster flutuante |
| `0` | Restaura o estado inicial de projeção |

O exemplo inicia em `RenderMode.FULL`. A seleção de view, portanto, exercita o mesmo modelo independente de roteamento Preview/Output documentado para projetos normais.

## Sequência recomendada de qualificação

1. Comece em Paul Bourke Environment Background no estado inicial/reset (`0`).
2. Passe para Cube Focus and Color e inspecione-a em Domemaster, Equirectangular, Skybox e Standard.
3. Aplique `P`, `Y` e `R` independentemente e verifique orientação contínua entre as projeções esféricas.
4. Em Domemaster, varie FOV e Size% e confirme que resolvem cobertura angular e ajuste físico da imagem, não movimento da câmera da Scene.
5. Alterne o preview flutuante e confirme que ele não redefine a resolução do output externo.
6. Retorne à cena Paul Bourke e inspecione polos, equador, continuidade longitudinal, invariância à translação, controles de visibilidade/intensidade e o perfil de rotação lenta.
7. Repita em cada combinação GPU/plataforma/output que será declarada como testada na release.
8. Registre screenshots e evidência do receiver a partir do **pacote realmente instalado ou checkout qualificado**.

!!! warning "Inspeção visual é evidência de hardware"
    Compatibilidade de código-fonte não prova que GPU, driver, projetor, lente ou cadeia de receiver esteja qualificada. Registre o ambiente exato usado em cada claim da release.

## Protocolo corrente vs. protocolo histórico

Esta página é o protocolo corrente do CalibrationTool. O [Calibration Tool e Compatibility Baseline da 1.5](1.5-calibration-tool.md) preserva o estado histórico da qualificação 1.5 e deve permanecer inalterado, salvo correções factuais explicitamente identificadas como correções históricas.

<div class="zd-actions" markdown>
[Calibração Esférica](../usage/spherical-calibration.md){ .md-button .md-button--primary }
[Prontidão para Release](2.0-release-readiness.md){ .md-button }
</div>
