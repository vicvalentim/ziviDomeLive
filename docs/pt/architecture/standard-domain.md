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

Quando o mesmo frame exige Standard e uma representação esférica, os dois domínios podem renderizar no frame. Isso é diferente de duplicar acidentalmente a captura esférica para consumidores distintos.