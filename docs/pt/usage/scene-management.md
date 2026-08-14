# Gerenciamento de Cenas

## Registre Cenas

```java
dome.setScene(new OpeningScene());
dome.registerScene(new PerformanceScene());
SceneManager manager = dome.getSceneManager();
```

A primeira cena torna-se ativa durante o registro. As demais permanecem registradas e inativas até serem selecionadas. O registro pela fachada garante que `configure(SceneServices)` execute antes do primeiro `setupScene()`.

## Troque de Cena

```java
manager.nextScene();
manager.previousScene();
manager.setCurrentSceneIndex(1);
manager.activateScene(sceneInstance);
```

A mudança segue esta ordem:

1. Encerra input, tarefas, trabalho de render pendente e tracking da câmera no contexto que sai.
2. Chama `dispose()` na cena anterior e então fecha os serviços restantes.
3. Cria um contexto novo e chama `configure()` e depois `setupScene()` na cena que entra.
4. Sincroniza os renderers Standard com a nova cena ativa.

Selecionar a cena já ativa não produz reinicialização.

## Substitua o Manager

`dome.setSceneManager(newManager)` transfere a autoridade da fachada. A cena ativa anterior é descartada, exceto quando a mesma instância está sendo transferida para o novo manager. Como um manager separado ativa imediatamente seu primeiro registro, prefira o registro pela fachada para cenas que exigem serviços no primeiro setup.

## Libere Recursos

Use `dome.dispose()` apenas no encerramento terminal. Ele libera outputs, controles, cenas, renderers, callbacks e splash. Grupos de tarefas das cenas são cancelados; o pool global compartilhado permanece disponível para outras instâncias. Chamadas repetidas são seguras.
