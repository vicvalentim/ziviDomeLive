# Painel de Controle

O painel ControlP5 interno é uma superfície operacional para preview, calibração e publicação de outputs. Pressione `h` para mostrar ou ocultar. O painel é criado após a inicialização dos renderers e fica oculto durante o splash de startup.

## Grupos de Controles

| Grupo | Controles | Escopo |
|---|---|---|
| Global | Label de FPS | Estado do runtime |
| Esférico | Pitch, Yaw, Roll, FOV, Size%, Reset | Orientação esférica compartilhada e calibração de domemaster |
| View | Domemaster flutuante, View Mode | Preview da janela Processing |
| Outputs | Resolução, NDI, Syphon/Spout, View por output | Targets offscreen e publicação |

Controles Syphon existem somente no macOS. Controles Spout existem somente no Windows. NDI aparece em todas as plataformas, mas a disponibilidade nativa é informada separadamente por `OutputState`.

## Visibilidade por RenderMode

O painel mostra apenas controles capazes de afetar a capacidade de renderização ativa:

| Modo | Pitch / Yaw / Roll | FOV / Size | Domemaster flutuante | View Mode principal | Seletores de output |
|---|---|---|---|---|---|
| `FULL` | Visível | Visível | Visível | Visível | Visíveis para outputs habilitados |
| `STANDARD` | Visível somente com domemaster flutuante | Visível somente com domemaster flutuante | Visível | Oculto | Ocultos |
| `DOMEMASTER` | Visível | Visível | Oculto | Oculto | Ocultos |
| `EQUIRECTANGULAR` | Visível | Oculto | Oculto | Oculto | Ocultos |
| `SKYBOX` | Visível | Oculto | Oculto | Oculto | Ocultos |

A resolução e os toggles de publicação continuam visíveis em todos os modos. Ocultar um seletor não apaga seu valor configurado. Voltar para `FULL` restaura o routing independente de preview e outputs.

## Sliders Cíclicos de Orientação

Os sliders Pitch, Yaw e Roll usam o estilo de handle flexível do ControlP5 e faixa visual `-PI..PI`. O movimento da roda do mouse atravessa as duas bordas:

```text
... 3.10, 3.14, -3.10, -3.04 ...
```

A volta não salta a atitude renderizada. A biblioteca calcula o menor delta e o compõe no quaternion unitário compartilhado. FOV e Size% continuam como sliders limitados.

Cada linha de orientação também possui number box editável. Entrada numérica e chamadas diretas à fachada preservam o valor fornecido; a atitude final não é reconvertida para Euler para exibição.

## Controles de View e Output

- **Preview Domemaster** habilita a miniatura fisheye flutuante.
- **View Mode** altera a rota configurada do preview principal em `FULL`.
- **Output Resolution** agenda realocação adiada dos targets de output.
- **Enable NDI/Syphon/Spout** altera o estado de publicação.
- **NDI/Syphon/Spout View** altera somente a rota daquele output em `FULL`.

Os toggles de publicação são donos das mudanças de backend. A facade recebe o callback ControlP5 e o encaminha ao controller interno do painel; `Scene` deliberadamente não possui `controlEvent()` em 2.0. Use getters/setters da facade ou `SceneActionMap` para comportamento da aplicação.

## Atalhos de Teclado

| Tecla | Ação |
|---|---|
| `h` | Mostrar/ocultar o painel |
| `m` | Alternar o `ViewType` configurado para preview |
| Esquerda / Direita | Cena anterior/próxima |

O atalho `m` atualiza a rota armazenada mesmo enquanto um modo dedicado força outra view efetiva. A seleção volta a aparecer em `FULL`.

## Controle Programático

Setters da fachada afetam a renderização imediatamente. O painel sincroniza seus pares slider/number-box para mudanças originadas nele e para `resetControls()`. Em uma UI gerenciada pela aplicação, trate a fachada como estado autoritativo e consulte seus getters em vez de ler widgets ControlP5.

Use:

```java
dome.resetOrientation(); // Somente a atitude quaternion.
dome.resetControls();    // Orientação, FOV, Size% e valores do painel.
```

`resetControls()` exige que o control manager esteja inicializado. Em sketches comuns isso acontece automaticamente pelo hook `post()` registrado após `setup()`.

Consulte [Calibração Esférica](spherical-calibration.md) para os eixos e parâmetros.
