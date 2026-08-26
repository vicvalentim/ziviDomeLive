# ADR 0006 — Future repository extraction

## Context

Core may move to its own repository while preserving useful directory history.

## Decision

Keep all Core Java, tests, build, and documentation below `core/`; avoid root source-set and build
helper dependencies; configure Maven publication in the module itself.

## Consequences

`git filter-repo --path core/ --path-rename core/:` leaves a buildable Java module after wrapper,
settings, license, and CI/release files are added.
