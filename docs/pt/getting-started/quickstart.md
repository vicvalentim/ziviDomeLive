---
title: Guia Rápido
icon: material/rocket-launch-outline
description: Crie uma primeira Scene do ziviDomeLive e teste Domemaster sem aprender os internals do renderer.
---

# Guia Rápido

Crie uma primeira cena funcional, mantenha o estado da animação coerente e teste o Domemaster. Este percurso evita intencionalmente Scene Services, benchmark, internals de output, threading e arquitetura do renderer.

!!! info "O contrato essencial"
    `update()` avança **estado/simulação uma vez por frame do Processing**. `sceneRender()` desenha o estado atual e pode executar mais de uma vez durante a captura esférica.

## 1. Imports

```java
import com.victorvalentim.zividomelive.*;
import controlP5.*;
```

ControlP5 é uma biblioteca Processing externa obrigatória e todos os exemplos distribuídos a importam. Instale-a explicitamente pelo Gerenciador de Contribuições; ziviDomeLive não promete instalação transitiva por `library.properties`. Syphon e Spout continuam integrações opcionais de plataforma e não devem ser importadas por sketches que não usam suas APIs diretamente.

## 2. Crie o runtime

```java
ziviDomeLive dome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}
```

## 3. Setup

```java
void setup() {
  dome = new ziviDomeLive(this); // (1)!
  dome.setup();
  dome.setScene(new MainScene()); // (2)!
}
```

1. Associa o runtime ao sketch atual do Processing e aos hooks de lifecycle.
2. Define `MainScene` como a cena ativa.

## 4. Crie uma Scene

```java
class MainScene implements Scene {
  float angle;

  public void update() {
    angle += 0.01f; // (1)!
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(8, 12, 24);
    pg.lights();
    pg.translate(pg.width * 0.5f, pg.height * 0.5f);
    pg.rotateY(angle);
    pg.box(180);
  }
}
```

1. O estado avança aqui para que todos os passes esféricos observem o mesmo estado do frame.

## 5. Entenda `update()`

Use `update()` para estados que devem avançar **uma vez por frame do Processing**: contadores de animação, física/simulação, timelines, randomização mutável e transições de estado.

## 6. Entenda `sceneRender()`

Use `sceneRender(PGraphicsOpenGL)` apenas para desenhar o estado atual.

!!! warning "A captura esférica pode renderizar a Scene repetidamente"
    `sceneRender()` pode ser chamado mais de uma vez durante um frame do Processing. Avançar animação dentro dele pode fazer diferentes direções esféricas observarem estados diferentes.

A biblioteca já controla `beginDraw()` e `endDraw()` do target entregue à Scene. Não os chame dentro de `sceneRender()`.

## 7. Altere o RenderMode

```java
// Teste um por vez:
dome.setRenderMode(RenderMode.STANDARD);
dome.setRenderMode(RenderMode.DOMEMASTER);
dome.setRenderMode(RenderMode.EQUIRECTANGULAR);
dome.setRenderMode(RenderMode.SKYBOX);
dome.setRenderMode(RenderMode.FULL);
```

`FULL` é o modo de trabalho padrão para rotas independentes de preview/output.

## 8. Teste Domemaster

```java
dome.setRenderMode(RenderMode.DOMEMASTER);
```

Depois siga para [Calibração Esférica](../usage/spherical-calibration.md). FOV, Size% e Pitch/Yaw/Roll são controles de calibração; não substituem o movimento da câmera da Scene.

<div class="zd-actions" markdown>
[Modos de renderização](../usage/basic-usage.md){ .md-button .md-button--primary }
[Exemplos de aprendizagem](../examples/basic.md){ .md-button }
</div>
