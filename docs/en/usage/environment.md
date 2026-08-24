---
title: "Environment"
icon: material/palette-outline
---
# Environment

The 2.0 Environment feature is a **visual LDR equirectangular background** shared by Standard and spherical representations.

```java
PImage stars = loadImage("textures/8k_stars_milky_way.jpg");
dome.setEquirectangularBackground(stars);
dome.setEnvironmentBackgroundVisible(true);
dome.setEnvironmentBackgroundIntensity(1.0f);
dome.setEnvironmentBackgroundYawOffset(0.0f);
```

## Current contract

- source: Processing `PImage`;
- format role: visual LDR background;
- mapping: equirectangular;
- behavior: observer-centred and translation-invariant;
- shared use: Standard, Domemaster, Equirectangular and Skybox rendering paths.

Without a source, or while visibility is disabled, Environment draws nothing and the library-owned
background stays transparent. Configuring and showing a source is an explicit request to render
that image; its texture alpha is preserved by both Standard and spherical Environment shaders.

Environment is not a lighting model. Version 2.0 does **not** claim HDR loading, IBL, irradiance maps, specular prefiltering, BRDF LUT integration, ambient occlusion or a general PBR environment engine. Those belong to future roadmap work.
