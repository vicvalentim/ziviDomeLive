---
title: Início
hide:
  - navigation
  - toc
---

<section class="zd-hero" markdown>
<div markdown>
<span class="zd-kicker">Processing 4 · Fulldome · Cubemap Nativo</span>

# Renderização esférica em tempo real para artistas que precisam de controle.

ziviDomeLive é uma biblioteca para Processing 4 voltada a fulldome, VR monoscópico, instalações imersivas e output esférico GPU-native. A versão 2.0.0 preserva o contrato familiar `Scene.sceneRender(PGraphicsOpenGL)` enquanto consolida captura nativa `GL_TEXTURE_CUBE_MAP` e shaders de projeção diretos com `samplerCube`.

<div class="zd-badges" markdown>
<span class="zd-badge">2.0.0</span>
<span class="zd-badge">P3D</span>
<span class="zd-badge">Domemaster</span>
<span class="zd-badge">Equiretangular</span>
<span class="zd-badge">Skybox</span>
</div>

<div class="zd-actions" markdown>
[Comece pelo guia rápido](getting-started/quickstart.md){ .zd-button }
[Veja os modos](usage/basic-usage.md){ .zd-button .zd-button--secondary }
[Planeje as capturas](usage/visual-capture-guide.md){ .zd-button .zd-button--secondary }
[Abrir Javadocs](api/javadocs.md){ .zd-button .zd-button--secondary }
</div>
</div>

<div class="zd-visual" markdown>
<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Placeholder da imagem principal**

Depois substitua por uma captura manual do `CalibrationTool` em modo `SKYBOX`, de preferência 16:9.
</div>
</div>
</div>
</section>

## O que mudou na 2.0

<div class="zd-grid" markdown>
<div class="zd-card" markdown>
### Captura esférica nativa

A renderização esférica agora escreve em um único target cubemap nativo em vez de manter vivo o antigo fallback `PGraphicsOpenGL[]`.
</div>

<div class="zd-card" markdown>
### Shaders de projeção diretos

Equiretangular, domemaster e skybox amostram o cubemap via `samplerCube`, reduzindo indireções de pipeline e preservando orientação das faces.
</div>

<div class="zd-card" markdown>
### Contrato Processing estável

As cenas continuam renderizando por `Scene.sceneRender(PGraphicsOpenGL)`, então sketches existentes podem migrar sem adotar um contexto customizado.
</div>
</div>

## Renderização de relance

<div class="zd-pipeline" markdown>
<div class="zd-step" markdown>
**Cena**

<span>Comandos de desenho Processing na `Scene` ativa.</span>
</div>
<div class="zd-step" markdown>
**Cubemap nativo**

<span>Seis orientações capturadas em `GL_TEXTURE_CUBE_MAP`.</span>
</div>
<div class="zd-step" markdown>
**samplerCube**

<span>Shaders de projeção amostram o cubemap diretamente.</span>
</div>
<div class="zd-step" markdown>
**Output**

<span>Preview, domemaster, equiretangular, skybox, NDI, Syphon ou Spout.</span>
</div>
</div>

## Espaços para capturas manuais

<div class="zd-gallery" markdown>
<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Preview Standard**

Arquivo sugerido: `docs/assets/images/screenshots/standard-preview.png`
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Domemaster**

Arquivo sugerido: `docs/assets/images/screenshots/domemaster-output.png`
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Equiretangular**

Arquivo sugerido: `docs/assets/images/screenshots/equirectangular-output.png`
</div>
</div>

<div class="zd-placeholder" markdown>
<div class="zd-placeholder__label" markdown>
**Layout Skybox**

Arquivo sugerido: `docs/assets/images/screenshots/skybox-layout.png`
</div>
</div>
</div>

Use o [Guia de Capturas Visuais](usage/visual-capture-guide.md) quando estiver pronto para substituir esses placeholders pelas imagens finais.

## Comece aqui

1. Consulte os [requisitos do sistema](installation/requirements.md) e as [dependências](installation/dependencies.md).
2. Instale o pacote seguindo os [passos de instalação](installation/installation-steps.md).
3. Crie a primeira cena com o [guia rápido](getting-started/quickstart.md).
4. Escolha entre roteamento independente e renderização dedicada em [Modos de Renderização](usage/basic-usage.md).
5. Conheça o [painel de controle](usage/control-panel.md) e a [calibração esférica](usage/spherical-calibration.md).
6. Consulte as [notas da versão 2.0.0](release-notes/2.0.0.md) antes de atualizar um sketch existente.

## Contratos estáveis da 2.0

- `Scene.sceneRender(PGraphicsOpenGL)` recebe um target já aberto; a biblioteca controla `beginDraw()` e `endDraw()`.
- `RenderMode.FULL` é o padrão e preserva rotas independentes de preview e output.
- A renderização Standard é independente da captura cubemap esférica.
- A captura esférica escreve em um `GL_TEXTURE_CUBE_MAP` nativo; equiretangular, domemaster e skybox o amostram diretamente.
- Pitch, yaw e roll esféricos compõem deltas mínimos em um único quaternion normalizado; os valores da fachada continuam como acumuladores de controle.
- O FOV do domemaster varia de `0..360`, com padrão `210`.
- O Size% do domemaster varia de `0..100`, com padrão `100`.
- Os presets de resolução de output são `1024`, `2048`, `3072` e `4096`.
- A publicação por outputs externos começa desabilitada.

## Qualificação

A suíte Java valida API, estado, lifecycle, routing, matemática, metadata e contratos de release sem exigir GPU. Comece pela [arquitetura de renderização](architecture/rendering-pipeline.md) e depois use o [protocolo do CalibrationTool](qualification/1.5-calibration-tool.md) e o [checklist de prontidão 2.0](qualification/2.0-release-readiness.md) em hardware qualificado. O repositório não fabrica imagens golden.

Consulte os [problemas conhecidos](known-issues.md) antes de uma implantação de produção.
