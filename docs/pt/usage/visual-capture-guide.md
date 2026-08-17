---
title: Preview e Output
icon: material/monitor-share
---

# Preview e Output

Preview e output são **consumidores independentes das views renderizadas**. Tamanho da janela, política de preview e resolução do output externo resolvem problemas distintos.

<figure markdown="span">
  ![Preview and output routing](../../img/preview-output-routing.png)
  <figcaption>Em FULL, preview e destinos externos podem solicitar ViewTypes diferentes do mesmo frame.</figcaption>
</figure>

<div class="grid cards" markdown>

- :material-monitor: **Preview**

    Inspeção interativa na janela atual do Processing. Sua resolução segue a política de preview, não o target do output externo.

- :material-export: **Output**

    Representação final entregue a um destino habilitado na resolução/aspecto configurados para output.

</div>

## Preview

- O preview Standard acompanha as dimensões atuais da janela do Processing.
- O preview esférico usa a política automática de preview quadrado da biblioteca, em vez da resolução de output.
- Redimensionar a janela, portanto, não redefine a resolução do output externo.

## Resolução de output

O ponto público de redimensionamento é `resetGraphics(int)`. Documentação e exemplos devem usar esse nome de API implementado.

!!! info "Mudanças gráficas são deferred"
    Mudanças de target de output são aplicadas na fronteira render/draw para recriar recursos gráficos no contexto Processing/OpenGL correto.

Alterar a resolução de output não deve redefinir silenciosamente a resolução de preview. O Size% do Domemaster é estado de calibração e deve persistir durante a recriação de targets.

## Roteamento independente em FULL

| Preview | Output | Por que é útil |
|---|---|---|
| Standard | Domemaster NDI | Trabalhar convencionalmente enquanto alimenta o pipeline do domo |
| Standard | Equirectangular | Manter uma view de operação enquanto publica representação 360° |
| Domemaster | Outro ViewType habilitado | Inspecionar calibração enquanto outra rota permanece ativa |

Um `RenderMode` dedicado substitui temporariamente a representação efetiva sem apagar as seleções armazenadas de `ViewType`.

!!! warning "Não iguale tamanho da janela e tamanho do output"
    A janela do Processing é uma superfície de interação/preview. A resolução do output externo constitui outro contrato.
