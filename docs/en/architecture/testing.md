---
title: "Testing and Qualification"
icon: material/source-branch
---
# Testing and Qualification


Testing is layered because not every contract can be proved by JUnit or headless CI.

## Automated

Use unit/integration/qualification tests for routing, lifecycle, metadata and non-visual invariants that can run deterministically in CI.

## GPU visual

Use [CalibrationTool](../qualification/calibration-tool.md) and representative scenes to inspect projection orientation, seams, calibration and Environment behavior on a real OpenGL configuration.

## Benchmark

Use [BenchmarkTool](../qualification/benchmark-guide.md) for smoke, CPU baseline and CPU/GPU measurement modes supported by the tool.

## Native output

NDI, Syphon and Spout claims require end-to-end receiver tests on the exact platforms claimed as tested.

## Package installation

The final Processing package must be installed from the generated ZIP/PDEX and examples opened/run from that installed package, not only from the repository checkout.
