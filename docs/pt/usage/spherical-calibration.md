# Calibração Esférica

Os controles esféricos transformam a captura cubemap compartilhada por domemaster, equiretangular e skybox. Eles não movem o `OrbitCamera` em scene space e não alteram a câmera perspectiva Standard independente.

## Parâmetros

| Parâmetro | Faixa do painel | Padrão | Afeta |
|---|---:|---:|---|
| Pitch | `-PI..PI` cíclico | `0` | Toda representação esférica |
| Yaw | `-PI..PI` cíclico | `0` | Toda representação esférica |
| Roll | `-PI..PI` cíclico | `0` | Toda representação esférica |
| FOV | `0..360` graus | `210` | Domemaster fisheye |
| Size% | `0..100` por cento | `100` | Escala do domemaster fisheye |

Chamadas programáticas devem permanecer no domínio suportado de FOV e Size%. `FisheyeDomemaster` limita Size% internamente, enquanto a fachada retém o valor fornecido pelo chamador por compatibilidade.

## Orientação por Quaternion

A fonte de orientação da 1.5 é um único quaternion normalizado:

- Pitch aplica o menor delta angular ao redor do eixo local `X`.
- Yaw aplica o menor delta angular ao redor do eixo local `Z`.
- Roll aplica o menor delta angular ao redor do eixo local `Y`.
- Os deltas são compostos na ordem em que chegam eventos dos setters ou do painel.
- Valores angulares não finitos são ignorados.

Os três getters preservam seus acumuladores de controle correspondentes. Eles não são uma decomposição Euler da atitude final. Por isso, duas sequências com a mesma tripla exibida ao final podem representar atitudes diferentes quando a ordem dos eventos muda. Isso remove a singularidade Euler e mantém Yaw e Roll independentes em Pitch de 90 graus.

Para calibração determinística, comece na identidade e aplique uma sequência conhecida:

```java
dome.resetOrientation();
dome.setPitch(HALF_PI);
dome.setYaw(0);
dome.setRoll(0);
```

O overload antigo `CubemapRenderer.captureCubemap(pitch, yaw, roll, ...)` permanece disponível e agora mantém seu próprio estado quaternion incremental. Código novo deve usar a fachada em vez de controlar renderers diretamente.

## FOV

FOV controla a extensão angular do shader fisheye. O padrão estabelecido é `210` graus. Valores representativos de qualificação devem incluir a configuração da lente de destino, `180`, `210` e o comportamento de borda quando aplicável.

FOV não muda a geometria equiretangular ou skybox; por isso o painel o oculta nesses modos dedicados.

## Size%

Size% escala o domemaster concluído ao redor do centro do target quadrado. Ele existe para alinhamento de projetor/lente e ajuste do círculo de imagem, não para zoom da cena. A recriação de targets restaura o valor configurado.

## Resolução

Resolução é independente de FOV e Size%:

- O preview Standard acompanha a janela.
- O preview esférico usa a política quadrada automática `256..1024`.
- Outputs externos usam o target selecionado `1024`, `2048`, `3072` ou `4096`.

Resoluções maiores aumentam o custo de cubemap e projeções. Qualifique foco e receiver no bucket exato de produção.

## Operações de Reset

```java
dome.resetOrientation(); // Somente quaternion e acumuladores Pitch/Yaw/Roll.
dome.resetControls();    // Orientação mais FOV=210 e Size%=100.
```

`resetControls()` também sincroniza os widgets internos e fica disponível depois que os managers chegam a `MANAGERS_READY`.

## Qualificação Visual

Testes automatizados de matemática verificam normalização, composição em ordem de evento, continuidade na borda cíclica, compatibilidade com acumuladores de múltiplas voltas e independência dos eixos em Pitch de 90 graus. Eles não verificam sampling de GPU, mirroring, seams, foco do projetor ou cor no receiver.

Use o [protocolo do CalibrationTool](../qualification/1.5-calibration-tool.md) para essas verificações e registre evidências com o [checklist de prontidão](../qualification/1.5-release-readiness.md).
