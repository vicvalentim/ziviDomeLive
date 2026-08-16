# Guia Rápido

Este percurso evita deliberadamente Scene Services, benchmark, internals de output, threading e arquitetura do renderer.

## 1. Imports

```java
import com.victorvalentim.zividomelive.*;
import processing.opengl.PGraphicsOpenGL;

// Dependências de runtime do pacote Processing:
import controlP5.*;
import codeanticode.syphon.*;
import spout.*;
```

Os imports de Syphon/Spout fazem parte das dependências de runtime da distribuição como biblioteca contribuída. Você não precisa configurar nem aprender esses sistemas para criar uma cena básica.

## 2. Crie o ziviDomeLive

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
  dome = new ziviDomeLive(this);
  dome.setup();
  dome.setScene(new MainScene());
}
```

O construtor registra os hooks do Processing usados pela biblioteca. Não encaminhe manualmente draw/eventos salvo quando uma API documentada solicitar isso.

## 4. Crie uma Scene

```java
class MainScene implements Scene {
  float angle;

  public void update() {
    angle += 0.01f;
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

## 5. `update()` = estado

Use `update()` para tudo que precisa avançar **uma vez por frame do Processing**:

- contadores de animação;
- física/simulação;
- timelines;
- randomização mutável;
- transições de estado.

## 6. `sceneRender()` = desenho

Use `sceneRender(PGraphicsOpenGL)` somente para desenhar o estado atual.

!!! important
    A captura esférica pode chamar `sceneRender()` mais de uma vez durante um único frame do Processing. Se a animação/estado avançar dentro de `sceneRender()`, as diferentes direções esféricas podem observar estados diferentes.

A biblioteca já controla `beginDraw()` e `endDraw()` no target recebido. Não os chame dentro de `sceneRender()`.

## 7. Mude o RenderMode

```java
dome.setRenderMode(RenderMode.STANDARD);
dome.setRenderMode(RenderMode.DOMEMASTER);
dome.setRenderMode(RenderMode.EQUIRECTANGULAR);
dome.setRenderMode(RenderMode.SKYBOX);
dome.setRenderMode(RenderMode.FULL);
```

`FULL` é o modo padrão para preview e outputs com rotas independentes.

## 8. Teste Domemaster

Comece com:

```java
dome.setRenderMode(RenderMode.DOMEMASTER);
```

Depois consulte Calibração Esférica antes de usar projetor/lente. FOV, Size% e Pitch/Yaw/Roll são controles de calibração; não substituem o movimento da câmera da cena.
