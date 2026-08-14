# Planejamento

## Scene Services Runtime (pós-2.0)

A versão 2.0 permanece concentrada nos contratos qualificados de cubemap nativo,
projeções, Environment, lifecycle de cenas, câmera e outputs. Uma versão posterior
poderá transformar infraestrutura comprovada pelos exemplos maiores em serviços da
API vinculados ao lifecycle:

- `SceneServices` ou `SceneContext` como ponto de acesso estável;
- `FrameClock`, `SimulationTimeline` e avanço fixed-step limitado;
- reload diferido de cenas e cleanup no escopo da cena;
- fila da render thread para criação de recursos Processing/OpenGL;
- grupos de tarefas da cena apoiados pelo `ThreadManager` compartilhado;
- caches tipados de imagens, shaders e primitivas com ownership explícito;
- mapeamento de input por ações e tracking opcional de alvo no `OrbitCamera`.

Modelos astronômicos, conversão Julian Date, propagação de Kepler e desenho orbital
permanecem fora do núcleo. Eles só deverão virar um módulo opcional de domínio depois
que mais de um consumidor mantido comprovar um contrato estável.

Esses serviços não fazem parte da 2.0.0 e não bloqueiam sua publicação.
