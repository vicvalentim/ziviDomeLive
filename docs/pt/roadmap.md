# Planejamento

## Scene Services Runtime (entregue na 2.0)

A infraestrutura comprovada pelo exemplo mantido `SolarSystem` agora está disponível
por serviços da API vinculados ao lifecycle:

- `SceneServices` como ponto de acesso estável no escopo de cada ativação;
- `FrameClock`, `SimulationTimeline` e avanço fixed-step limitado;
- reload diferido e cleanup em ordem inversa;
- fila da render thread para transferência Processing/OpenGL;
- grupos de tarefas limitados e identificados por chave, apoiados pelo
  `ThreadManager` compartilhado;
- caches tipados de imagem, shader e shape com ownership explícito;
- mapeamento de input por ações, tracking de alvo do `OrbitCamera` e configuração
  Environment no escopo da cena.

O próximo trabalho de API será guiado pela adoção: refinar esses contratos somente
a partir de vários consumidores mantidos, preservar compatibilidade de fonte e
manter ownership GPU explícito. Diagnósticos/telemetria reutilizáveis e políticas
opcionais de preload são candidatos, mas não compromissos de release.

Modelos astronômicos, conversão Julian Date, propagação de Kepler e desenho orbital
permanecem fora do núcleo. Eles só deverão virar um módulo opcional de domínio depois
que mais de um consumidor mantido comprovar um contrato estável.
