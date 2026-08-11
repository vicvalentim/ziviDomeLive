# Requisitos do Sistema

## Runtime

- Processing 4; a versão 1.5.0 é compilada e testada contra Processing core `4.5.6`
- Renderizador `P3D`
- `pixelDensity(1)` recomendado para estabilidade entre displays
- Contexto OpenGL Processing/JOGL funcional

Java 17 é necessário para compilar a biblioteca a partir do código-fonte. O Processing fornece seu próprio runtime Java para sketches instalados.

## Hardware

O requisito prático depende da complexidade da cena, resolução de output e quantidade de outputs simultâneos. Uma GPU dedicada é recomendada para renderização esférica 3K/4K e cenas com shaders intensivos. Qualifique a combinação exata de GPU, driver, projetor, lente e receiver antes do uso em produção.

## Capacidades por Plataforma

| Capacidade | macOS | Windows | Linux |
|---|---|---|---|
| Renderização Standard e esférica | Suportada | Suportada | Suportada |
| NDI | Exige qualificação nativa/receiver | Exige qualificação nativa/receiver | Reduzido e não qualificado |
| Syphon | Backend da plataforma | Indisponível | Indisponível |
| Spout | Indisponível | Backend da plataforma | Indisponível |

"Suportada" para renderização principal descreve o limite pretendido da plataforma, não afirma que toda combinação de GPU e driver passou pelo protocolo visual manual.

## Apple Silicon

A stack Processing/Syphon usada pelo projeto pode exigir a versão Intel do Processing sob Rosetta 2 para interoperabilidade completa. Renderização ARM nativa sem Syphon continua possível, mas deve ser qualificada com o sketch e driver de destino.

Consulte [Problemas Conhecidos](../known-issues.md) e o protocolo de qualificação antes da implantação.
