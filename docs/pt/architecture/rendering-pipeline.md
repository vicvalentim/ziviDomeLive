# Pipeline de Renderização

ziviDomeLive 1.5 mantém dois domínios de renderização. `RenderMode` seleciona comportamento, mas não os reduz a um único backend.

## Domínio Standard

```text
Scene
  -> StandardRenderer
  -> target PGraphicsOpenGL perspectiva
  -> preview da janela ou output habilitado
```

Standard renderiza a cena diretamente por sua câmera perspectiva. Ele não captura faces cubemap e não é derivado de conteúdo equiretangular ou fisheye. Os renderers Standard de preview e output compartilham estado de câmera para manter o enquadramento consistente entre targets de tamanhos diferentes.

## Domínio Esférico

```text
Scene
  -> seis faces cubemap de 90 graus
     -> CubemapViewRenderer -> layout skybox
     -> EquirectangularRenderer -> mapa 2:1
        -> FisheyeDomemaster -> domemaster quadrado + escala Size%
```

As seis faces usam a tabela estável de orientação `CubemapFace` (`+X`, `-X`, `+Y`, `-Y`, `+Z`, `-Z`). `CameraManager` permanece como fachada de compatibilidade da 1.x para integrações diretas com renderers. Um único quaternion `SphericalOrientation` é aplicado a todas as faces de preview e output.

Essa topologia descreve a implementação 1.x, não um contrato permanente de backend. Uma futura versão major pode trocar texturas ou projeções internas preservando o comportamento visual qualificado.

## Fechamento de Requisitos

`RenderRequirementsPolicy` expande as views solicitadas apenas para os passes necessários no frame:

| Representação solicitada | Standard | Fonte cubemap | Equirectangular | Fisheye | Layout cubemap |
|---|---:|---:|---:|---:|---:|
| Standard | Sim | Não | Não | Não | Não |
| Skybox | Não | Sim | Não | Não | Sim |
| Equirectangular | Não | Sim | Sim | Não | Não |
| Domemaster | Não | Sim | Sim | Sim | Não |

O requisito de preview, domemaster flutuante e todos os outputs habilitados é resolvido independentemente e compartilhado quando possível.

## Um Frame

Os hooks Processing automáticos executam nesta ordem:

1. `pre()` chama `Scene.update()` uma vez.
2. `draw()` limpa a janela e retorna cedo enquanto managers não estiverem prontos.
3. Uma mudança pendente de resolução recria targets de output.
4. Renderers de preview dependentes da janela são verificados e recriados quando o bucket automático muda.
5. Requisitos de preview e output são resolvidos.
6. No máximo um cubemap mestre é capturado quando há conteúdo esférico.
7. Passes de output habilitados executam e os targets completos são enviados aos backends.
8. Passes de preview executam, reutilizando projeções de output já concluídas quando possível.
9. O preview efetivo é composto na janela Processing.
10. O domemaster flutuante opcional e o painel ControlP5 são desenhados.

`Scene.sceneRender()` pode executar mais de uma vez no mesmo frame Processing: uma vez para Standard e uma vez para cada face cubemap exigida. Mudanças de animação e simulação pertencem a `update()`.

## Reuso do Cubemap Mestre

Quando um output habilitado exige dados esféricos, seu cubemap na resolução de output vira a fonte mestre de output e preview. Projeções de output concluídas e compatíveis podem ser copiadas para targets de preview. Sem demanda esférica de output, a biblioteca captura somente o cubemap na resolução automática de preview.

Isso evita captura duplicada da cena mantendo janela Processing e outputs externos em domínios de target separados.

## Fronteira Processing GL

`ProcessingGlAdapter` é a fronteira estreita para as chamadas Processing/OpenGL
atuais: alocação de targets, verificação de textura, readback `loadPixels()` do
NDI, descarte de targets e descoberta de capabilities pelo contexto PGL ativo.
As capabilities reportadas incluem suporte a textura, FBO, cubemap, seamless
cubemap, PBO e sync fence para que PRs posteriores de cubemap nativo e readback
possam condicionar seus caminhos GL explicitamente.

`CubemapTarget` controla armazenamento nativo `GL_TEXTURE_CUBE_MAP` com política
conservadora de textura e framebuffers de cópia reutilizáveis. A captura em
runtime ainda renderiza cada face da cena em targets `PGraphicsOpenGL` do
Processing para preservar o contrato `Scene.sceneRender(PGraphicsOpenGL)`, e
depois copia cada face concluída pelo caminho GPU para a face correspondente do
cubemap nativo. `EquirectangularRenderer` amostra esse cubemap nativo
diretamente quando ele está disponível, usando o shader legado de seis texturas
como fallback.

Os recursos de shader `samplerCube` para os modos cubemap, equiretangular,
domemaster/fisheye e skybox ficam preparados em `data/shaders/samplercube/` nos
artefatos empacotados. Equiretangular agora é selecionado em runtime para
amostragem do cubemap nativo; domemaster/fisheye e skybox continuam preparados
para PRs posteriores da migração.

## Ownership de Resolução

| Target | Política de dimensão | Recriado quando |
|---|---|---|
| Preview Standard | Largura e altura atuais da janela | Janela muda |
| Preview esférico | Quadrado `min(1024, max(256, min(width, height)))` | Bucket muda |
| Targets de output | Base selecionada `1024`, `2048`, `3072` ou `4096` | `resetGraphics()` adiado é aplicado |

A resolução de output não redefine a resolução de preview. A realocação ocorre na draw thread do Processing, nunca diretamente no callback da UI.

## Fronteira de Output

- `RenderPipeline` fornece targets completos pelo contrato mínimo `FrameViews`. `OutputManager` seleciona o `ViewType` lógico a publicar sem inspecionar o renderer concreto que o produziu.
- Uma única fronteira `FrameViews` é reutilizada durante o runtime e resolve os targets atuais de forma lazy; o hot path não aloca um carrier por frame e resets adiados de renderers não deixam referências obsoletas.
- `OutputManager` coordena o routing, enquanto os serviços concretos `NdiOutputBackend`, `SyphonOutputBackend` e `SpoutOutputBackend` controlam diretamente seus recursos nativos e lifecycle; não existe uma camada de factory de backends.
- Syphon e Spout publicam texturas `PGraphicsOpenGL` completas no caminho Processing/GPU.
- NDI chama `loadPixels()` na thread Processing, copia para um dos três slots CPU e envia RGBA progressivo empacotado por worker dedicado.
- O worker NDI não faz chamadas OpenGL.
- Estado de publicação é distinto de inicialização do backend e de requisito de renderização.

## Contratos Estáveis e Internos

Estáveis na 1.5:

- separação comportamental Standard/esférico
- orientação das faces cubemap e layout skybox
- conteúdo renderizado da cena
- comportamento quaternion de pitch/yaw/roll
- FOV e Size% do domemaster
- preview automático e resolução de output independente

Detalhes internos:

- `PGraphicsOpenGL[]` como contrato das faces renderizadas pela Scene
- `CubemapTarget` é preenchido a partir das faces capturadas e alimenta o output equiretangular quando disponível
- shaders `samplerCube` de domemaster/fisheye e skybox estão empacotados, mas ainda não são selecionados em runtime
- domemaster consumindo atualmente a saída equiretangular
- estratégia exata de alocação e cópia entre renderers

Consulte [Lifecycle de Runtime](runtime-lifecycle.md) e [Prontidão da Release](../qualification/1.5-release-readiness.md).
