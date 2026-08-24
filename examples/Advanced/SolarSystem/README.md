# SolarSystem

**Category:** Advanced
**Required dependency:** ControlP5 2.2.6

A lifecycle and numerical regression scene built on the final 2.0 services: `FrameClock`, bounded
`SimulationTimeline`, `SceneAssets`, named actions, reload requests, and the scene-space camera.
Orbital time, anomaly, solvers, perturbations, and compensated elapsed-time accumulation remain in
`double`; conversion to Processing's float-based vectors happens only when publishing render state.

## Main controls

- Space: reset the camera;
- `1`–`9`: track the Sun or a planet;
- `+` / `-`: change simulated time rate;
- `W`, `S`, `T`: wireframe, solid, or textured rendering;
- `O`, `L`, `P`: toggle planet orbits, labels, or moon orbits;
- `G`/`g`, `A`/`a`, `B`/`b`: adjust global, orbital, or body scale;
- `R`: request a safe scene reload;
- `D`: set a UTC date;
- `N`: toggle clock diagnostics.

Scientific data and media retain their own terms. See [THIRD_PARTY.md](THIRD_PARTY.md) and
[ASSET_PROVENANCE.json](ASSET_PROVENANCE.json). Missing optional textures degrade to the example's
fallback rendering and do not change the core library contract.
