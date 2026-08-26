# ADR 0001 — Core independence

## Context

The same computational semantics must serve Processing and a future standalone host.

## Decision

Core production uses Java 17 `java.base` only and has no host, graphics, facade, or output imports.
Automated source and JAR checks enforce the boundary.

## Consequences

Hosts provide input, rendering, resources, and window/output integration. Core artifacts remain
small, portable, and independently publishable.
