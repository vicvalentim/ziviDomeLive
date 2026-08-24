# ziviDomeLive Examples

The examples are part of the Processing-library learning and qualification surface. They use APIs recommended for ziviDomeLive 2.0.0.
All ten sketches import the required external ControlP5 2.2.6 Processing library; install it explicitly before compiling or running them.

## GettingStarted

Use these in this order when learning the library:

1. **EmptyProject** — minimal project structure and Scene contract.
2. **Basic** — scene drawing, facade-owned registration and render modes.
3. **NamedActions** — named key-code/mouse bindings and programmatic actions.
4. **PortLoopback** — bounded scene input and non-blocking output port adapters.

## Advanced

1. **SphereParticle** — activation-scoped background simulation and frame-boundary publication.
2. **InfiniteBackground** — Environment/background workflow.
3. **FulldomePBR** — retained geometry, shaders and the scene camera service.
4. **SolarSystem** — timeline, assets, actions, camera tracking and double-precision simulation.

All eight learning examples are teaching material and use the final public API recommended to
artists. Each sketch directory contains its own README.

## Tools

### CalibrationTool

Visual/GPU qualification tool for checking spherical orientation, domemaster calibration and representative rendered output. It is **not** an introductory tutorial.

### BenchmarkTool

Performance/qualification tool for benchmark smoke and the measurement modes implemented by the project. It is **not** required for ordinary sketches.

## Release rule

Before a release tag, open/run all ten items from the **installed generated Processing package**,
not only from the repository checkout. Record environment/platform evidence separately; the
existence of a platform backend does not prove that platform was tested for the release.
