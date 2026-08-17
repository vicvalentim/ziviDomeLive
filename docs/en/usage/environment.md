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

Environment is not a lighting model. Version 2.0 does **not** claim HDR loading, IBL, irradiance maps, specular prefiltering, BRDF LUT integration, ambient occlusion or a general PBR environment engine. Those belong to future roadmap work.
