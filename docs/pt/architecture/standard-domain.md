---
title: "Standard Domain"
icon: material/source-branch
---
# Standard Domain

O Standard Domain renderiza perspectiva comum independentemente da captura esférica.

- origem: `Scene` ativa;
- resultado: view final Standard;
- câmera: transformações da câmera de cena se aplicam aqui;
- dependência: trabalho Standard-only não exige cubemap.

A biblioteca limpa o target final Standard para RGBA transparente `(0, 0, 0, 0)` em cada frame.
Nenhum céu escuro é inserido automaticamente. Uma Scene pode desenhar deliberadamente um
background Processing ou geometria fullscreen, e um Environment configurado e visível é conteúdo
explícito. O caminho interno de compatibilidade de cor do céu só fica opaco após pedido explícito.

Quando o mesmo frame exige Standard e uma representação esférica, os dois domínios podem renderizar no frame. Isso é diferente de duplicar acidentalmente a captura esférica para consumidores distintos.
