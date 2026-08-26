# ADR 0004 — Core versus host boundary

## Context

Current classes mix pure state with Processing vectors, matrices, events, images, and graphics.

## Decision

Extract state and algorithms; retain event binding, matrix application, images, shaders, scene
callbacks, renderers, and outputs in hosts. Do not create placeholder graphics abstractions.

## Consequences

Processing adapters can retain the frozen artist API while a future host supplies different
integration code.
