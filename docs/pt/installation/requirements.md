# Requisitos do Sistema

## Runtime

- Processing 4 (revisão `1285` ou mais recente). A compatibilidade de código-fonte pode ser auditada contra a distribuição oficial do Processing 4.0 com `./gradlew compileProcessing4Baseline -Pprocessing4BaselineLibrary=/caminho/para/processing-4.0/core/library`. Os testes automatizados regulares usam Processing 4.5.6; essa versão testada não substitui a linha-base de compatibilidade 4.0, e compilação estática não substitui qualificação física de runtime.
- Renderizador `P3D`
- `pixelDensity(1)` recomendado para estabilidade entre displays
- GPU e driver capazes de expor OpenGL 4.1; os shaders empacotados usam GLSL 4.10

Java 17 é necessário para compilar a biblioteca a partir do código-fonte. O Processing fornece seu próprio runtime Java para sketches instalados.

## Hardware

O requisito prático depende da complexidade da cena, resolução de output e quantidade de outputs simultâneos. Uma GPU dedicada é recomendada para renderização esférica 3K/4K e cenas com shaders intensivos. GPUs integradas ou antigas que não criam um contexto OpenGL 4.1 não conseguem compilar os shaders de projeção empacotados. Qualifique a combinação exata de GPU, driver, projetor, lente e receiver antes do uso em produção.

## Capacidades por Plataforma

| Capacidade | macOS | Windows | Linux |
|---|---|---|---|
| Renderização Standard e esférica | Suportada | Suportada | Suportada |
| Sender de vídeo NDI | Experimental; exige NDI Runtime separado e qualificação com receiver | Experimental; exige NDI Runtime separado e qualificação com receiver | Experimental, reduzido e não qualificado |
| Syphon | Backend da plataforma | Indisponível | Indisponível |
| Spout | Indisponível | Backend da plataforma | Indisponível |

"Suportada" para renderização principal descreve o limite pretendido da plataforma, não afirma que toda combinação de GPU e driver passou pelo protocolo visual manual.

## Apple Silicon

A stack Processing/Syphon usada pelo projeto pode exigir a versão Intel do Processing sob Rosetta 2 para interoperabilidade completa. Renderização ARM nativa sem Syphon continua possível, mas deve ser qualificada com o sketch e driver de destino.

Antes da implantação, consulte [Problemas Conhecidos](../known-issues.md),
[Runtime NDI](ndi.md), o
[Protocolo do Calibration Tool](../qualification/calibration-tool.md) e a
[Prontidão para Release](../qualification/2.0-release-readiness.md).
