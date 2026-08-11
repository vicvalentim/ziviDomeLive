# Visão Geral da API

## Tipos Principais

| Tipo | Responsabilidade |
|---|---|
| `ziviDomeLive` | Integração Processing, lifecycle, renderização, calibração e acesso aos serviços |
| `RenderMode` | Comportamento global de renderização |
| `Scene` | Contrato de desenho e eventos do usuário |
| `SceneManager` | Registro, ownership ativo, troca e descarte de cenas |
| `OutputManager` | Roteamento e lifecycle de NDI, Syphon e Spout |
| `OrbitCamera` | Câmera opcional em scene space compartilhada pelos targets |
| `SphericalOrientation` | Acumulação cíclica de pitch/yaw/roll em um quaternion unitário |

## Enums Públicos

```java
RenderMode.FULL
RenderMode.STANDARD
RenderMode.DOMEMASTER
RenderMode.EQUIRECTANGULAR
RenderMode.SKYBOX
```

`ziviDomeLive.ViewType` continua disponível para rotas de preview e output. Sua ordem é sensível à compatibilidade e não deve ser alterada.

| Índice de `ViewType` | Valor | Representação |
|---:|---|---|
| 0 | `FISHEYE_DOMEMASTER` | Projeção circular fulldome |
| 1 | `EQUIRECTANGULAR` | Projeção esférica 2:1 |
| 2 | `CUBEMAP` | Layout de inspeção das seis faces |
| 3 | `STANDARD` | Renderização perspectiva da cena |

`StandardOutputAspectMode` seleciona `AUTO`, `ASPECT_16_9`, `ASPECT_16_10`,
`ASPECT_4_3` ou `ASPECT_1_1` para o output Standard em alta resolução. Ele não
redimensiona a janela de preview do Processing.

`OutputManager.OutputState` distingue:

- `UNAVAILABLE`: não suportado ou última inicialização falhou
- `AVAILABLE`: elegível para inicialização, sem recursos nativos
- `INITIALIZED`: recursos nativos existem, publicação desabilitada
- `ENABLED`: publicação habilitada
- `STOPPING`: NDI sem publicação enquanto conclui limpeza limitada

## Compatibilidade

- A fachada pública é `ziviDomeLive`; a classe em minúsculas da 1.x não é mantida na 2.0.
- `RenderMode.FULL` preserva o modelo legado de roteamento.
- `renderFisheyeDomemaster()`, `renderEquirectangular()`, `renderCubemap()` e `renderStandard()` continuam como shims de compatibilidade depreciados.
- Getters de renderers permanecem públicos na 1.x, mas a topologia interna não é um contrato permanente.

Os Javadocs gerados no pacote de release são a referência de assinaturas.
Consulte [Classes Principais](core-classes.md) para ownership e estados,
[Funções Operacionais](helper-functions.md) para controles de runtime e
[Interface Scene](scene-interface.md) para o contrato de desenho.
