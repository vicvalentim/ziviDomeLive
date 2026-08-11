# Visão Geral da API

## Tipos Principais

| Tipo | Responsabilidade |
|---|---|
| `zividomelive` | Integração Processing, lifecycle, renderização, calibração e acesso aos serviços |
| `RenderMode` | Comportamento global de renderização |
| `Scene` | Contrato de desenho e eventos do usuário |
| `SceneManager` | Registro, ownership ativo, troca e descarte de cenas |
| `OutputManager` | Roteamento e lifecycle de NDI, Syphon e Spout |
| `OrbitCamera` | Câmera opcional em scene space compartilhada pelos targets |

## Enums Públicos

```java
RenderMode.FULL
RenderMode.STANDARD
RenderMode.DOMEMASTER
RenderMode.EQUIRECTANGULAR
RenderMode.SKYBOX
```

`zividomelive.ViewType` continua disponível para rotas de preview e output. Sua ordem é sensível à compatibilidade e não deve ser alterada.

`OutputManager.OutputState` distingue:

- `UNAVAILABLE`: não suportado ou última inicialização falhou
- `AVAILABLE`: elegível para inicialização, sem recursos nativos
- `INITIALIZED`: recursos nativos existem, publicação desabilitada
- `ENABLED`: publicação habilitada
- `STOPPING`: NDI sem publicação enquanto conclui limpeza limitada

## Compatibilidade

- O nome público em minúsculas `zividomelive` permanece inalterado.
- `RenderMode.FULL` preserva o modelo legado de roteamento.
- `renderFisheyeDomemaster()`, `renderEquirectangular()`, `renderCubemap()` e `renderStandard()` continuam como shims de compatibilidade depreciados.
- Getters de renderers permanecem públicos na 1.x, mas a topologia interna não é um contrato permanente.

Os Javadocs gerados no pacote de release são a referência de assinaturas.
