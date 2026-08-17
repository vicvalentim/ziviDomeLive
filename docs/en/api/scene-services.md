---
title: "Scene Services"
icon: material/api
status: advanced
---
# Scene Services

## Do I really need this?

For a simple scene, **no**. Implement `Scene`, keep state in `update()`, draw in `sceneRender()`, and use the main `ziviDomeLive` facade.

Use `SceneServices` when a project needs lifecycle-aware facilities that should be created/configured with a scene activation and released with that activation.

## Opt in progressively

A service-aware scene can receive the current activation services through:

```java
public void configure(SceneServices services) {
  // Retain only the service references your scene actually needs.
}
```

The exact service accessors are part of the generated Javadocs. Do not make Scene Services a dependency of examples that do not need them.

## Typical needs

### Frame/time

Use the frame clock/timeline facilities when simulation or scheduling needs an explicit time source rather than ad-hoc counters.

### Assets

Use lifecycle-aware asset facilities when asset ownership/loading should follow scene activation rather than global sketch state.

### Background work and render-thread work

Background tasks must not assume they own the Processing OpenGL context. Work that must touch renderer-owned graphics state has to return through the library's render-thread mechanism documented by the current API.

### Actions and coordination

Use service-backed actions/queues when a project benefits from explicit coordination rather than direct cross-thread mutation.

### Camera and Environment

Camera tracking and Environment state can be integrated with scene lifecycle when the project requires it. They remain conceptually distinct: camera transforms scene space; spherical orientation calibrates the spherical representation.

## Cleanup

A scene activation can end on switch, clear, replacement or facade release. Release scene-owned resources in `dispose()` and do not retain renderer targets beyond their documented lifetime.
