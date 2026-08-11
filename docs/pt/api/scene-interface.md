# Interface Scene

Somente `sceneRender(PGraphicsOpenGL)` é abstrato. Todos os métodos de lifecycle e eventos possuem implementação padrão, preservando compatibilidade para cenas mínimas.

```java
class ExampleScene implements Scene {
  public void setupScene() {
    // Aloque ou reinicie estado pertencente à cena.
  }

  public void update() {
    // Avance o estado uma vez por frame do Processing.
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    // Apenas desenhe. Não chame beginDraw() nem endDraw().
  }

  public void keyEvent(processing.event.KeyEvent event) {}
  public void mouseEvent(processing.event.MouseEvent event) {}
  public void controlEvent(controlP5.ControlEvent event) {}

  public void dispose() {
    // Libere recursos criados em setupScene().
  }

  public String getName() {
    return "Example";
  }
}
```

## Regras de Ownership

- A biblioteca controla o lifecycle de desenho de cada target.
- `update()` concentra mutação que deve ocorrer uma vez por frame.
- `sceneRender()` pode executar para Standard e várias faces cubemap no mesmo frame.
- `setupScene()` pode executar novamente após uma cena ser desativada e reativada.
- `dispose()` deve liberar recursos que `setupScene()` recriará.
- Callbacks de entrada são encaminhados automaticamente; o sketch principal não deve repeti-los.
