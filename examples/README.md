# ziviDomeLive Examples

The examples are part of the Processing-library learning and qualification surface. They use APIs recommended for ziviDomeLive 2.0.0.

## Learning examples

Use these in this order when learning the library:

1. **EmptyProject** — minimal project structure and Scene contract.
2. **Basic** — basic scene drawing and runtime use.
3. **SphereParticle** — animated scene state separated from rendering.
4. **InfiniteBackground** — Environment/background workflow.
5. **FulldomePBR** — more advanced fulldome scene content.
6. **SolarSystem** — larger multi-object/scene example.

These examples are teaching material. Keep them focused on the public API recommended to artists.

## Qualification tools

### CalibrationTool

Visual/GPU qualification tool for checking spherical orientation, domemaster calibration and representative rendered output. It is **not** an introductory tutorial.

### BenchmarkTool

Performance/qualification tool for benchmark smoke and the measurement modes implemented by the project. It is **not** required for ordinary sketches.

## Release rule

Before a release tag, open/run all eight items from the **installed generated Processing package**, not only from the repository checkout. Record environment/platform evidence separately; the existence of a platform backend does not prove that platform was tested for the release.
