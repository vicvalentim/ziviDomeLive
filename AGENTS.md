# AGENTS Guide for ziviDomeLive

## Scope and source priority
- This repo is a Processing 4 Java library for fulldome/VR rendering (`README.md`, `src/main/java/com/victorvalentim/zividomelive/zividomelive.java`).
- Prefer source-of-truth in this order: `src/main/java` -> `build.gradle.kts` + `.github/workflows` -> `README.md` -> `examples/`.
- Canonical agent guidance is this `AGENTS.md`; no `CLAUDE.md` or Copilot-instruction files are present in the repository.

## Big-picture architecture
- Entrypoint: `zividomelive` orchestrates setup, lifecycle hooks, rendering, controls, and outputs.
- Scene boundary: `Scene` is the extension contract; `SceneManager` registers/switches scenes and calls `setupScene()` on activation.
- Rendering pipeline is layered:
  - `render/CubemapRenderer.java`: renders 6 faces from camera orientations.
  - `render/modes/EquirectangularRenderer.java`: cubemap -> equirectangular shader pass.
  - `render/modes/FisheyeDomemaster.java`: equirectangular -> domemaster shader pass.
  - Optional viewers: `StandardRenderer` and `CubemapViewRenderer`.
- Service managers:
  - `manager/ControlManager.java`: ControlP5 UI, output toggles, resolution/view selectors.
  - `manager/OutputManager.java`: NDI (Devolay), Syphon (macOS), Spout (Windows).
  - `support/ThreadManager.java` + `support/LogManager.java`: shared executor and logging.

## Runtime flow that matters
- Processing hooks are auto-registered in constructor (`pre`, `draw`, `post`, `mouseEvent`, `keyEvent`, etc.).
- Initialization is split: `setup()` creates `OutputManager` + splash/default scene; `post()` lazily initializes render managers once.
- `draw()` is gated by `initialized`; it clears to black and returns until `post()` calls `initializeManagers()`, flips `initialized`, and unregisters `post()`.
- Per-frame flow (`renderContent()`): clear -> pending graphics reset -> cubemap capture -> projection render -> output send -> optional preview -> control panel.
- Scene updates happen in `pre()` via `currentScene.update()` before drawing.

## Project-specific conventions to preserve
- Keep `zividomelive.ViewType` enum order stable; dropdowns map by index (`ControlManager` uses `ViewType.values()[selectedIndex]`).
- Resolution changes are deferred (`resetGraphics` sets `pendingReset`; actual renderer reallocation occurs inside draw loop).
- Shader paths are loaded from packaged data paths (`data/shaders/*.vert|*.frag`), and `build.gradle.kts` copies `shaders/` into the JAR at `data/shaders`.
- Use `LogManager.getLogger()` instead of ad-hoc loggers; logs also go to `/tmp/zividomelive/logs` on non-Windows.
- Threaded tasks should use `ThreadManager` (shared fixed pool), not new executors per feature.

## Build, test, docs, and release workflows
- Main CI mirrors local build: `./gradlew build` (`.github/workflows/gradle.yml`).
- Run tests with JUnit 5: `./gradlew test`.
- Non-obvious dependency bootstrap: `compileJava` depends on `downloadDependencies`; `download_dependencies.sh` fetches `Syphon.jar`, `controlP5.jar`, `spout.jar` into `src/main/libs` when empty.
- Release artifacts: `./gradlew buildReleaseArtifacts` then package emits `release/ziviDomeLive.zip`, `.pdex`, `.txt` (`release.yml` publishes these on `v*` tags).
- Processing-local install task: `./gradlew deployToProcessingSketchbook`.
- Docs site: `mkdocs build` using Material + `mkdocs-static-i18n`; bilingual files use suffix mode (`*.md` + `*.pt.md`) in `docs/`.
- Docs CI/deploy: `.github/workflows/deploy_website.yml` builds docs on `main` and deploys `site/` to `gh-pages`; `.github/workflows/pr_preview.yml` publishes PR previews under `gh-pages/pr-preview`.

## Testing patterns in this repo
- Tests are intentionally lightweight and avoid GPU contexts when possible:
  - `SceneManagerTest` uses a `FakeScene` stub and asserts lifecycle semantics.
  - `CameraManagerTest` validates exact cubemap orientation vectors.
  - `QuaternionTest` validates math-only behavior.
- When adding renderer features, prefer isolating pure math/state logic into testable units first.

## Integration and platform boundaries
- Output behavior is OS-gated in `OutputManager`: Syphon on macOS, Spout on Windows, NDI where native libs load.
- `OutputManager` constructor auto-initializes Syphon/Spout on supported OS and enables that output by default; NDI initializes only when `toggleOutput("ndi")` is called.
- Linux has reduced external-output support per `README.md` known issues.
- Keyboard conventions in runtime: `h` toggles control panel, `m` cycles view mode, Left/Right arrows switch scenes (`zividomelive.keyEvent`).
- Metadata is not fully consistent across files (example: `README.md` says GPL-2.0, `mkdocs.yml` footer says MIT); verify before changing licensing/version fields.

