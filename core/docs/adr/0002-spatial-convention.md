# ADR 0002 — Spatial convention

## Context

ziviDomeLive 2.0 qualifies `+Z` as dome front and uses meaningful negative orbit distances.

## Decision

Core preserves `+Z` front, `-Z` rear, a default camera on the `-Z` side looking toward `+Z`, signed
distance, and the collapse guard through zero.

## Consequences

Adapters must not silently convert to a conventional unsigned engine distance or reverse axes.
