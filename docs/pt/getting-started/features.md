# Funcionalidades Principais

## Modos de Renderização Explícitos

`RenderMode` seleciona o comportamento global:

- `FULL`: rotas `ViewType` independentes para preview e outputs
- `STANDARD`: representação perspectiva Standard
- `DOMEMASTER`: domemaster fisheye
- `EQUIRECTANGULAR`: projeção esférica 2:1
- `SKYBOX`: layout cubemap

Modos dedicados substituem as rotas efetivas sem apagar as seleções `ViewType` salvas. Voltar para `FULL` restaura o roteamento independente.

## Domínios de Renderização Independentes

Cenas Standard são renderizadas diretamente por `StandardRenderer`. Views esféricas compartilham a captura cubemap e os passes de projeção. Uma política central calcula apenas as views exigidas pelo preview principal, domemaster flutuante e outputs habilitados.

## Calibração de Domemaster

FOV e Size% são parâmetros operacionais de calibração:

| Parâmetro | Faixa | Padrão |
|---|---:|---:|
| FOV | `0..360` graus | `210` |
| Size | `0..100` por cento | `100` |

Pitch, yaw e roll afetam todos os modos esféricos a partir da mesma orientação.

## Lifecycle de Cenas

`SceneManager` controla a cena ativa. Ele executa `setupScene()` na ativação, `update()` antes de cada frame renderizado e `dispose()` quando uma cena deixa de ser ativa ou o manager é limpo.

## Separação entre Preview e Output

O preview Standard acompanha as dimensões da janela Processing. Targets esféricos de preview usam tamanho quadrado automático entre 256 e 1024 pixels. Outputs externos usam uma resolução global independente, selecionada entre presets 1K, 2K, 3K e 4K.

## Outputs Externos

- Syphon no macOS e Spout no Windows recebem targets GPU-native `PGraphicsOpenGL`.
- NDI copia pixels completos do Processing para um pipeline limitado a três slots e envia por um worker dedicado.
- Seleção de preview, publicação, lifecycle do backend e requisito de renderização permanecem independentes.
- Estado e diagnóstico de falha estão disponíveis em `OutputManager`.

## Painel de Controle

O painel ControlP5 agrupa status global, parâmetros esféricos, seleção de preview e controles de output. Os toggles controlam publicação, e cada output habilitado expõe seu próprio seletor de view.
