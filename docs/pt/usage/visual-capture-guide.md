# Preview e Output

Preview e output são consumidores independentes das representações renderizadas.

![Roteamento de preview e output](../../img/preview-output-routing.png)

## Preview

- O preview Standard acompanha as dimensões atuais da janela Processing.
- O preview esférico usa a política automática de resolução quadrada da biblioteca, não a resolução de output.
- Redimensionar a janela, portanto, não redefine a resolução dos outputs externos.

## Resolução de output

O ponto público de recriação dos targets é `resetGraphics(int)`. Documentação e exemplos devem usar esse nome de API implementado.

Mudanças nos targets de output são deferidas para a fronteira de renderização/draw, preservando o contexto correto Processing/OpenGL.

Alterar a resolução de output não deve redefinir silenciosamente a resolução de preview. Size% do Domemaster é estado de calibração e deve persistir durante a recriação dos targets.

## Aspecto da saída

A representação final determina a geometria/aspecto relevante. Standard, Domemaster, Equirectangular e Skybox devem ser tratados como views finais distintas, não como uma mesma imagem redimensionada.

## Roteamento independente em FULL

`RenderMode.FULL` permite combinações como:

- preview Standard + NDI Domemaster;
- preview Standard + output local Equirectangular;
- preview Domemaster + outra view em output habilitado.

A rota efetiva é resolvida por destino. Um `RenderMode` dedicado substitui temporariamente a representação efetiva sem apagar as seleções `ViewType` armazenadas.
