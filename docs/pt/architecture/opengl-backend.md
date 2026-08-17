---
title: "Backend OpenGL"
icon: material/source-branch
---
# Backend OpenGL

O renderer esférico 2.0 usa cubemap OpenGL e shaders de projeção baseados em `samplerCube`.

`ProcessingGlAdapter` concentra a fronteira Processing/OpenGL e `CubemapTarget` encapsula o papel de cubemap/FBO/depth. São tipos engine-facing, não pré-requisitos de artistas.

Trabalho OpenGL deve respeitar o contexto gráfico ativo e o ownership definido pela implementação. PBO, HDR/IBL e outras possibilidades futuras não são capacidades 2.0 enquanto não implementadas e qualificadas.