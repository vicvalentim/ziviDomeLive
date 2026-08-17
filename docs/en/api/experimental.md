---
title: "Experimental Public API"
icon: material/api
status: experimental
---
# Experimental Public API

The current performance-instrumentation surface is experimental and qualification-oriented.

## Contract

- do not treat experimental metrics as stable user-facing API guarantees;
- CPU wall-time measurements are not GPU elapsed-time measurements;
- document only GPU timing actually exposed by the current implementation;
- use `BenchmarkTool` for qualification work, not as the first learning example.

`PerformanceSnapshot` and related performance types belong here. Consult generated Javadocs for the exact fields/methods present in this release.
