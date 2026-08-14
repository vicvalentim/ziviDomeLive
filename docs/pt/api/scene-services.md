# Serviços de Cena

`SceneServices` é um contexto vinculado ao lifecycle criado a cada ativação de uma
`Scene`. Ele retira infraestrutura comum de sketches grandes, mantendo astronomia,
física, regras da aplicação e decisões de desenho no código consumidor.

Receba o contexto pelo callback opcional `configure()` e registre a cena pela
fachada:

```java
class ExampleScene implements Scene {
  private SceneServices services;

  public void configure(SceneServices services) {
    this.services = services;
  }

  public void setupScene() {
    services.timeline().setFixedStep(1.0 / 120.0);
    services.timeline().setMaxSubSteps(8);
    services.actions().bindKeyPressed(
        "reload", 'R', services::requestReload);
    services.camera().setInputEnabled(true);
  }

  public void update() {
    services.timeline().advance(
        services.frameClock().getDeltaSeconds(),
        this::simulate);
  }

  public void sceneRender(PGraphicsOpenGL pg) {
    services.camera().apply(pg);
    // Apenas desenhe; a biblioteca controla beginDraw()/endDraw().
  }

  private void simulate(double step) {
    // Avance o domínio por um passo fixo de simulação.
  }
}

dome.setScene(new ExampleScene());
```

`setScene()` e `registerScene()` na fachada são os caminhos recomendados para
registro com serviços. Um `SceneManager` separado pode ativar sua primeira cena
antes de ser anexado à fachada; portanto, código que precisa dos serviços no
primeiro `setupScene()` deve registrar pela fachada.

## Serviços Disponíveis

| Acesso | Responsabilidade |
|---|---|
| `frameClock()` | Delta monotônico, tempo decorrido, índice do frame e limite configurável para hitches |
| `timeline()` | Simulação fixed-step pausável, com taxa e catch-up limitado com telemetria |
| `renderQueue()` | Transferência entre threads para a thread Processing/OpenGL no próximo frame |
| `tasks()` | Tarefas limitadas e identificadas por chave no pool compartilhado da biblioteca |
| `assets()` | Caches de imagem, shader e shape retido restritos à render thread |
| `actions()` | Ações nomeadas de teclado/mouse, preservando callbacks crus de `Scene` |
| `camera()` | `OrbitCamera` compartilhado, ownership de input e tracking dinâmico opcional |
| `environment()` | Configuração Environment LDR que restaura o estado substituído pela cena |
| `onDispose()` | Cleanup adicional em ordem inversa para recursos da aplicação |

`parent()`, `applet()` e `scene()` expõem a fachada, o applet Processing e a
identidade da cena quando o acesso direto é necessário.

## Ordem de Frame e Reload

Em cada frame ativo, a fachada drena a fila de render, avança o `FrameClock`, chama
`Scene.update()`, atualiza o alvo acompanhado, avança a câmera compartilhada,
sincroniza seu quaternion com a orientação do Environment e então renderiza.
`requestReload()` não altera a cena dentro de um callback de input: ele adia um
ciclo completo de dispose/setup para o próximo limite de frame e fornece um novo
contexto `SceneServices`.

A timeline fixed-step executa no máximo `maxSubSteps` callbacks por frame. Se um
hitch produzir mais passos completos, o excesso aparece em `getDroppedUnits()` em
vez de gerar uma espiral de catch-up sem limite.

## Regras de Thread e Recursos

Objetos Processing e OpenGL devem ser criados ou alterados na render thread.
Trabalho em background pode usar `tasks()` e depois enfileirar a menor transferência
Processing/OpenGL possível em `renderQueue()`. `submitIfIdle()` com chave é indicado
para trabalho contínuo porque impede backlog criado a cada frame. O Processing pode
executar o setup do sketch e a animação JOGL em threads diferentes; por isso o runtime
estabelece a afinidade oficial da fila em cada limite de frame `pre()`.

`SceneAssets` trata imagens, shaders e shapes comuns do Processing como emprestados:
fechar o contexto limpa referências, mas não descarta objetos gerenciados pelo
Processing. Use `SceneResourceCache.getOrCreateOwned()` apenas para recursos cujo
disposer explícito pertence à cena. Recursos próprios são descartados na ordem
inversa de inserção.

Antes de `Scene.dispose()`, o runtime encerra ações, cancela tarefas da cena,
rejeita trabalho de render pendente, desabilita o input da câmera e remove tracking.
Depois de `dispose()`, executa cleanup adicional, restaura o estado Environment que
a ativação substituiu e fecha os caches. Fechar uma cena nunca encerra o
`ThreadManager` global da biblioteca.

O exemplo `SolarSystem` é a referência mantida para todos esses serviços. Conversão
de Julian Date, propagação orbital, modelos de corpos celestes e desenho astronômico
continuam no domínio do exemplo, fora da API central.
