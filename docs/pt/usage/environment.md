---
title: "Environment"
icon: material/palette-outline
---
# Environment

O recurso Environment da versão 2.0 é um **background visual LDR equirectangular** compartilhado pelas representações Standard e esféricas.

```java
PImage stars = loadImage("textures/8k_stars_milky_way.jpg");
dome.setEquirectangularBackground(stars);
dome.setEnvironmentBackgroundVisible(true);
dome.setEnvironmentBackgroundIntensity(1.0f);
dome.setEnvironmentBackgroundYawOffset(0.0f);
```

## Contrato atual

- fonte: `PImage` do Processing;
- função: background visual LDR;
- mapeamento: equirectangular;
- comportamento: centrado no observador e invariante à translação;
- uso compartilhado: caminhos Standard, Domemaster, Equirectangular e Skybox.

Environment não é um modelo de iluminação. A versão 2.0 **não** declara carregamento HDR, IBL, mapas de irradiância, prefilter especular, BRDF LUT, ambient occlusion nem um engine PBR geral de environment. Esses itens pertencem ao roadmap futuro.
