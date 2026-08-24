---
title: "Backend OpenGL"
icon: material/source-branch
---
# Backend OpenGL

O renderer esférico 2.0 usa cubemap OpenGL e shaders de projeção baseados em `samplerCube`.

Um adapter Processing/OpenGL interno concentra alocação gráfica, operações de framebuffer, binding de cubemap, readback e descoberta de capabilities. Um target interno de cubemap possui recursos texture/FBO/depth. Esses tipos de implementação são descritos para manutenção e não são API pública.

Trabalho OpenGL deve respeitar o contexto gráfico ativo e o ownership definido pela implementação. PBO, HDR/IBL e outras possibilidades futuras não são capacidades 2.0 enquanto não implementadas e qualificadas.
