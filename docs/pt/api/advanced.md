---
title: API Advanced Stable
icon: material/layers-triple-outline
status: advanced
tags:
  - API
---

# API Advanced Stable

Advanced Stable é superfície pública suportada com pré-requisitos mais rigorosos de lifecycle ou conceito. Não é convite para contornar a facade.

## Serviços de ativação

| Grupo | Tipos | Garantia principal |
|---|---|---|
| Tempo | `FrameClock`, `SimulationTimeline` | Tempo `double`, deltas limitados e telemetria de catch-up fixed-step |
| Trabalho | `SceneTaskGroup` | Trabalho em background bounded/nomeado em executor compartilhado |
| Assets | `SceneAssets` | Criação de assets Processing na render thread e limpeza da ativação |
| Input | `SceneActionMap` | Bindings nomeados compatíveis com callbacks raw da Scene |
| Câmera | `SceneCameraService` | Uma orbit camera scene-space, tracking de target e rig de luz de vista opt-in |
| Environment | `SceneEnvironmentService` | Overrides da ativação com restauração segura |
| Integração | `ScenePorts`, `SceneInputPort`, `SceneOutputPort` | SPI protocol-agnostic bounded para adapters |

Todos são acessados por `SceneServices`; nenhum serviço concreto possui construtor público ou `close()` scene-facing.

## Controle de output

`OutputManager` é uma interface pública retornada pela facade. Sua superfície tipada `OutputType`/`OutputState` controla intenção e diagnóstico, enquanto operações de produtor continuam internas. Consulte [API de Outputs](outputs.md).

## Matemática e navegação

- `Quaternion` é imutável e expõe composição normalizada, interpolação esférica e publicação em matriz;
- `SphericalOrientation` mantém valores cíclicos de pitch/yaw/roll e uma atitude normalizada;
- `OrbitCamera` realiza manipulação direta imediata e movimento programático opcionalmente suave.

`SceneCameraService.applyWithViewLighting(...)` aplica a transformação orbital e um spotlight na
posição da câmera, apontado ao target corrente. A chamada é explícita e substitui as luzes
fixed-function correntes; iluminação de shader customizado continua pertencendo à cena.

Movimento de câmera scene-space e calibração do domo são independentes: alterar target/distance da órbita não redefine pitch/yaw/roll nem FOV do domemaster.

## Promessa de compatibilidade

Tipos Advanced Stable integram o snapshot exato da API 2.0. Restrições de lifecycle, membros dos enums e ausência de campos públicos mutáveis são testados. Tipos internos do engine não serão promovidos para este nível apenas porque um caller solicita acesso raw.
