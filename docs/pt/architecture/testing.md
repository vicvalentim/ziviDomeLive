---
title: "Testes e Qualificação"
icon: material/source-branch
---
# Testes e Qualificação

A qualificação é em camadas.

- **Automated:** unit/integration/qualification para invariantes determinísticos.
- **GPU visual:** `CalibrationTool` e cenas representativas em OpenGL real.
- **Benchmark:** `BenchmarkTool` nos modos suportados.
- **Native output:** receivers reais para NDI/Syphon/Spout nas plataformas declaradas.
- **Package installation:** instalar ZIP/PDEX final e abrir/executar exemplos a partir do pacote instalado.
