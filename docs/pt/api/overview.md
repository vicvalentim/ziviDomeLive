# Visão Geral da API

## Tipos Principais

| Tipo | Responsabilidade |
|---|---|
| `ziviDomeLive` | Integração Processing, lifecycle, renderização, calibração e acesso aos serviços |
| `RenderMode` | Comportamento global de renderização |
| `ViewType` | Seleção de rotas para preview e outputs independentes no modo `FULL` |
| `FrameViews` | Targets finais completos expostos por view lógica sem acoplamento a renderers |
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

`ViewType` é um enum público top-level para rotas de preview e output. Sua ordem na 2.0 faz parte do contrato público.

| Índice de `ViewType` | Valor | Representação |
|---:|---|---|
| 0 | `STANDARD` | Renderização perspectiva da cena |
| 1 | `DOMEMASTER` | Projeção circular fulldome |
| 2 | `EQUIRECTANGULAR` | Projeção esférica 2:1 |
| 3 | `SKYBOX` | Layout de inspeção das seis faces |

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
- `ViewType` é top-level na 2.0; o enum aninhado da 1.x e seus nomes antigos não são mantidos.
- `RenderMode.FULL` preserva o modelo de roteamento por compatibilidade.
- `renderFisheyeDomemaster()`, `renderEquirectangular()`, `renderCubemap()` e `renderStandard()` continuam como shims de compatibilidade depreciados.
- Getters de renderers permanecem públicos por compatibilidade, mas a topologia interna não é um contrato permanente.

Os Javadocs gerados no pacote de release e no GitHub Pages são a referência de assinaturas.
Consulte [Javadocs Gerados](javadocs.md) para assinaturas diretas,
[Classes Principais](core-classes.md) para ownership e estados,
[Funções Operacionais](helper-functions.md) para controles de runtime e
[Interface Scene](scene-interface.md) para o contrato de desenho.
