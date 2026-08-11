# Gerenciamento de Cenas

## Registre Cenas

```java
SceneManager manager = new SceneManager();
manager.registerScene(new OpeningScene());
manager.registerScene(new PerformanceScene());
dome.setSceneManager(manager);
```

A primeira cena torna-se ativa durante o registro. As demais permanecem registradas e inativas até serem selecionadas.

## Troque de Cena

```java
manager.nextScene();
manager.previousScene();
manager.setCurrentSceneIndex(1);
manager.activateScene(sceneInstance);
```

A mudança segue esta ordem:

1. Atualiza o ownership ativo no `SceneManager`.
2. Chama `dispose()` na cena que sai.
3. Chama `setupScene()` na cena que entra.
4. Sincroniza os renderers Standard com a nova cena ativa.

Selecionar a cena já ativa não produz reinicialização.

## Substitua o Manager

`dome.setSceneManager(newManager)` transfere a autoridade da fachada. A cena ativa anterior é descartada, exceto quando a mesma instância está sendo transferida para o novo manager.

## Libere Recursos

Use `dome.dispose()` apenas no encerramento terminal. Ele libera outputs, controles, cenas, renderers, callbacks, splash e o thread manager compartilhado. Chamadas repetidas são seguras.
