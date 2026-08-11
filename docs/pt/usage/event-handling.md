# Manipulação de Eventos

O construtor de `ziviDomeLive` registra hooks `keyEvent` e `mouseEvent` no Processing. O painel ControlP5 interno registra um listener que encaminha eventos relevantes à cena ativa.

Implemente os callbacks na cena:

```java
public void keyEvent(processing.event.KeyEvent event) {}
public void mouseEvent(processing.event.MouseEvent event) {}
public void controlEvent(controlP5.ControlEvent event) {}
```

Não adicione encaminhamento no sketch principal, como `ziviDome.keyEvent(event)`. Isso entrega o mesmo evento duas vezes.

## Atalhos Globais

- `h`: mostra ou oculta o painel interno
- `m`: alterna a view legada configurada para preview
- Setas Esquerda/Direita: cena anterior/próxima

Atalhos globais executam antes de o evento chegar à cena.

## Entrada de Câmera

Com `setSceneCameraInputEnabled(true)`, eventos de mouse também chegam ao `OrbitCamera` em scene space. A câmera perspectiva Standard permanece um serviço separado.

Todos os callbacks registrados são removidos no descarte terminal.
