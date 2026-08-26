# ADR 0003 — Update once, render many

## Context

Spherical capture can render a scene six or more times after one Processing frame update.

## Decision

Time, simulation, input delivery, task callbacks, and camera interpolation advance only when the
host invokes them at its frame boundary. Core has no render callback that can advance state.

## Consequences

Every face and projection observes one coherent state snapshot for a frame.
