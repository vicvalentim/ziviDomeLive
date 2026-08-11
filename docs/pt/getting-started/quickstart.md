# Guia Rápido

## Crie o Sketch

```java
import com.victorvalentim.zividomelive.*;
import processing.opengl.PGraphicsOpenGL;

zividomelive ziviDome;

void settings() {
  size(1280, 720, P3D);
  pixelDensity(1);
}

void setup() {
  ziviDome = new zividomelive(this);
  ziviDome.setup();
  ziviDome.setScene(new MainScene());
}

void draw() {
  // ziviDomeLive renderiza automaticamente pelo hook draw do Processing.
}
```

Chame `setup()` uma vez após a construção. Não chame `ziviDome.draw()` no sketch: o construtor já registrou o hook de desenho da biblioteca.

## Implemente uma Cena

```java
class MainScene implements Scene {
  float angle;

  public void setupScene() {
    angle = 0;
  }

  public void update() {
    angle += 0.01f;
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    pg.background(8, 12, 24);
    pg.lights();
    pg.rotateY(angle);
    pg.box(180);
    // A biblioteca controla beginDraw() e endDraw().
  }

  public String getName() {
    return "Main";
  }
}
```

`sceneRender()` é invocado para cada target necessário no frame. Mantenha alterações de estado em `update()` para que a animação avance uma vez por frame, não uma vez por face do cubemap.

## Selecione um RenderMode

`FULL` é o padrão. Um sketch que nunca chama `setRenderMode()` mantém o comportamento de roteamento da 1.4.

```java
ziviDome.setRenderMode(RenderMode.FULL);
ziviDome.setRenderMode(RenderMode.STANDARD);
ziviDome.setRenderMode(RenderMode.DOMEMASTER);
ziviDome.setRenderMode(RenderMode.EQUIRECTANGULAR);
ziviDome.setRenderMode(RenderMode.SKYBOX);
```

Use `setCurrentView()` para a rota de preview no modo `FULL`:

```java
ziviDome.setCurrentView(zividomelive.ViewType.FISHEYE_DOMEMASTER);
```

## Receba Eventos

A biblioteca registra hooks de teclado e mouse do Processing e encaminha cada evento uma vez para a cena ativa. O listener ControlP5 interno encaminha eventos do painel pelo mesmo contrato.

```java
public void keyEvent(processing.event.KeyEvent event) {
  if (event.getAction() == processing.event.KeyEvent.PRESS) {
    println(event.getKey());
  }
}

public void mouseEvent(processing.event.MouseEvent event) {
  // Trate a entrada da cena.
}

public void controlEvent(controlP5.ControlEvent event) {
  // Trate eventos relevantes do painel interno.
}
```

Não encaminhe esses eventos novamente pelo sketch principal.

## Adicione Outras Cenas

```java
SceneManager scenes = new SceneManager();
scenes.registerScene(new IntroScene());
scenes.registerScene(new MainScene());
ziviDome.setSceneManager(scenes);
```

O primeiro registro ativa a cena. As setas Esquerda e Direita alternam cenas pelos atalhos globais da biblioteca.

## Roteie um Output

Outputs começam desabilitados:

```java
OutputManager outputs = ziviDome.getOutputManager();
outputs.setNdiView(zividomelive.ViewType.EQUIRECTANGULAR);
outputs.toggleOutput("ndi");
```

Consulte estado e diagnóstico sem confundir disponibilidade com demanda de renderização:

```java
println(outputs.getOutputState(OutputManager.OutputType.NDI));
println(outputs.getOutputFailureReason(OutputManager.OutputType.NDI));
```

A interoperabilidade nativa ainda exige qualificação de hardware por plataforma.
