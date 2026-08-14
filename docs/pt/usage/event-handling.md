# Manipulação de Eventos

O construtor de `ziviDomeLive` registra hooks `keyEvent` e `mouseEvent` no Processing. O painel ControlP5 interno registra um listener que encaminha eventos relevantes à cena ativa.

Implemente os callbacks na cena:

```java
public void keyEvent(processing.event.KeyEvent event) {}
public void mouseEvent(processing.event.MouseEvent event) {}
public void controlEvent(controlP5.ControlEvent event) {}
```

Não adicione encaminhamento no sketch principal, como `ziviDome.keyEvent(event)`. Isso entrega o mesmo evento duas vezes.

Cenas com serviços podem mapear nomes estáveis de ações em vez de ramificar no
callback cru:

```java
services.actions().bindKeyPressed("reload", 'R', services::requestReload);
services.actions().register("reset-camera", () -> services.camera().orbit().reset());
services.actions().trigger("reset-camera");
```

O runtime despacha as ações antes do callback cru da cena. O callback ainda executa
por compatibilidade; evite realizar a mesma operação nos dois caminhos. Os bindings
são limpos automaticamente quando a cena perde ownership.

## Atalhos Globais

- `h`: mostra ou oculta o painel interno
- `m`: alterna a view legada configurada para preview
- Setas Esquerda/Direita: cena anterior/próxima

Atalhos globais executam antes de o evento chegar à cena.

## Entrada de Câmera

Com `setSceneCameraInputEnabled(true)` ou `services.camera().setInputEnabled(true)`, gestos de navegação chegam ao `OrbitCamera` em scene space em vez da câmera perspectiva Standard independente. Isso impede que um único drag ou evento da roda mova duas câmeras ao mesmo tempo.

Todos os callbacks registrados são removidos no descarte terminal.
