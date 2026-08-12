# Guia de Capturas Visuais

Esta página define os espaços de captura usados pelo manual. Faça as capturas manualmente, salve com os nomes sugeridos e depois substitua cada bloco por uma imagem Markdown normal.

!!! tip "Ritmo de captura recomendado"
    Capture o mesmo sketch em `STANDARD`, `DOMEMASTER`, `EQUIRECTANGULAR` e `SKYBOX` para que a pessoa leitora compare os modos sem se perguntar se a cena mudou.

## Checklist de captura

<div class="zd-checklist" markdown>
<div class="zd-check" markdown>
**1. Use o CalibrationTool**

<span>Comece por uma cena com orientação de faces, profundidade e linha de horizonte bem visíveis.</span>
</div>
<div class="zd-check" markdown>
**2. Espere o splash**

<span>Deixe o splash screen terminar antes das capturas finais.</span>
</div>
<div class="zd-check" markdown>
**3. Ignore o ruído GL 1282**

<span>O aviso conhecido do teardown do Processing não invalida a captura.</span>
</div>
<div class="zd-check" markdown>
**4. Mantenha nomes estáveis**

<span>Use os arquivos abaixo para que futuras trocas na documentação sejam mecânicas.</span>
</div>
</div>

## Capturas principais

<div class="zd-gallery" markdown>
<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Hero / SKYBOX**

Salve como `docs/assets/images/screenshots/hero-skybox.png`. Use essa imagem na home.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Preview Standard**

Salve como `docs/assets/images/screenshots/standard-preview.png`.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Output Domemaster**

Salve como `docs/assets/images/screenshots/domemaster-output.png`.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Output Equiretangular**

Salve como `docs/assets/images/screenshots/equirectangular-output.png`.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Layout Skybox**

Salve como `docs/assets/images/screenshots/skybox-layout.png`.
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Painel de controle**

Salve como `docs/assets/images/screenshots/control-panel.png`.
</div>
</div>
</div>

## Padrão de substituição

Quando a captura existir, substitua o placeholder por:

```markdown
![Output Domemaster](../../assets/images/screenshots/domemaster-output.png){ .img-center }
```

Mantenha as imagens em `docs/assets/images/screenshots/` e prefira PNG para UI ou calibração, onde texto nítido e bordas duras importam.

## Enquadramento sugerido

| Captura | Proporção sugerida | O que ela deve provar |
|---|---:|---|
| Hero / SKYBOX | 16:9 | Layout das faces, continuidade de horizonte e identidade visual |
| Preview Standard | 16:9 | O preview Standard da janela Processing permanece independente |
| Domemaster | 1:1 | Forma fisheye, FOV, Size% e enquadramento de dome |
| Equiretangular | 2:1 | Qualidade de costura e continuidade horizontal |
| Painel de controle | 16:9 ou crop | Routing de modos, controles de calibração e estado de output |

Depois de substituir os slots, rode:

```bash
.venv-docs/bin/mkdocs build --strict
```

O workflow do GitHub Pages também anexa os Javadocs gerados em `/reference/`, então os links da API podem apontar para lá quando o site estiver publicado.
