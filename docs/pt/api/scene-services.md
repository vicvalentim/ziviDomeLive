---
title: "Scene Services"
icon: material/api
status: advanced
---
# Scene Services

## Eu realmente preciso disso?

Para uma cena simples, **não**. Implemente `Scene`, mantenha estado em `update()`, desenhe em `sceneRender()` e use a fachada principal `ziviDomeLive`.

Use `SceneServices` quando o projeto precisar de recursos associados ao lifecycle que devam ser criados/configurados com uma ativação de cena e liberados com ela.

## Adoção progressiva

Uma cena que usa services pode receber os serviços da ativação corrente por:

```java
public void configure(SceneServices services) {
  // Retenha apenas as referências de serviço realmente necessárias.
}
```

Os acessores exatos pertencem aos Javadocs gerados. Não torne Scene Services dependência de exemplos que não precisam deles.

## Necessidades típicas

### Frame/tempo

Use clock/timeline quando simulação ou agendamento exigir fonte temporal explícita em lugar de contadores ad hoc.

### Assets

Use recursos de assets associados ao lifecycle quando ownership/carregamento deve acompanhar a ativação da cena, e não estado global do sketch.

### Trabalho em background e render thread

Tasks em background não podem assumir ownership do contexto OpenGL do Processing. Trabalho que toca estado gráfico controlado pelo renderer deve retornar pelo mecanismo de render thread documentado pela API corrente.

### Actions e coordenação

Use actions/queues quando o projeto se beneficiar de coordenação explícita em lugar de mutação direta entre threads.

### Câmera e Environment

Camera tracking e Environment podem acompanhar o lifecycle da cena quando necessário. Os conceitos continuam distintos: câmera transforma o espaço da cena; orientação esférica calibra a representação esférica.

## Cleanup

Uma ativação pode terminar por troca, limpeza, substituição ou liberação da fachada. Libere recursos da cena em `dispose()` e não retenha targets do renderer além do lifetime documentado.
