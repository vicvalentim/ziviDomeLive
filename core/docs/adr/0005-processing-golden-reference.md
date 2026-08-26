# ADR 0005 — Processing 2.0 golden reference

## Context

The release/2.0.0 implementation has mature behavioral tests and a frozen public API.

## Decision

Core 0.1 copies qualified semantics without replacing Processing production classes. Root
equivalence tests compare deterministic values, transitions, exceptions, and ordering.

## Consequences

This branch proves extraction independently. Delegation belongs to the future 2.1 integration.
