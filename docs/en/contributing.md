# Contributing

!!! tip "Contributing development"
    We welcome contributions to the development of ziviDomeLive. Bug fixes, well-scoped features, tests, examples, accessibility improvements, translations and documentation all matter and are greatly appreciated.

Participation is governed by the [Code of Conduct](https://github.com/vicvalentim/ziviDomeLive/blob/main/CODE_OF_CONDUCT.md) and the project's [research-integrity and human-review declaration](research-integrity.md). AI-assisted work must identify the tool and purpose; contributors must understand, test, review and accept responsibility for every submitted change.

## Steps to contribute

1. **Fork the repository.** Open the [ziviDomeLive repository](https://github.com/vicvalentim/ziviDomeLive) and select **Fork** to create a copy under your GitHub account.
2. **Clone your fork.** Replace `YOUR-USERNAME` with your GitHub account:

    ```bash
    git clone https://github.com/YOUR-USERNAME/ziviDomeLive.git
    cd ziviDomeLive
    ```

3. **Create a focused branch.** Use a short name that identifies the work:

    ```bash
    git checkout -b your-branch-name
    ```

4. **Make and test the change.** Keep the scope coherent, follow the contracts below and add or update tests and bilingual documentation when public behavior changes.
5. **Commit and push to your fork.** Write a clear commit message, then publish the branch:

    ```bash
    git add <changed-files>
    git commit -m "Describe the contribution"
    git push origin your-branch-name
    ```

6. **Open a pull request.** From your fork, open a PR against the original repository. Explain the problem, the chosen solution, intentional API or behavior changes, validation performed and any remaining hardware or visual checks. Link the related issue when one exists.

Thank you for helping ziviDomeLive remain useful, teachable and sustainable.

## Local Checks

Use Java 17 and run:

```bash
./gradlew clean qualificationTests
./gradlew build -x test
./gradlew buildReleaseArtifacts
python3 -m mkdocs build --strict
./gradlew attachJavadocsToSite --console=plain
python3 tools/validate_documentation.py --root . --site-dir site
```

Preview the manual with `python3 -m mkdocs serve`. This deliberately avoids legacy system-level MkDocs executables that may belong to Python 2.

## Project Contracts

- Keep `ViewType` order unchanged.
- Keep English and Portuguese pages paired and update `mkdocs.yml` navigation together.
- Do not call `beginDraw()` or `endDraw()` inside a `Scene`.
- Preserve deferred output-resolution reset.
- Use `LogManager` for library logging.
- Use activation-scoped `SceneServices.tasks()` for scene background work; do not expose or create another executor.
- Keep Syphon/Spout on the `PGraphicsOpenGL` path.
- Do not reintroduce the removed `PGraphicsOpenGL[]` spherical capture path.

GPU or output changes require the [CalibrationTool](qualification/calibration-tool.md) visual protocol and target-platform hardware evidence in addition to unit tests.

`qualificationTests` is the canonical automated test run. Its summary, HTML,
and JUnit XML evidence is written under `build/reports/qualification/` and
`build/test-results/qualification/`. You can diagnose one class with
`./gradlew qualificationTests --tests '*OrbitCameraTest'`, but release
acceptance requires the complete unfiltered suite. Test sources stay in Git and
are excluded from Processing packages and sketchbook deployment.

GitHub also runs this task in the independent `Automated Qualification`
workflow for every push, pull requests targeting `main`, and manual executions.
Its job summary shows the totals and its downloadable artifact retains the
detailed evidence for 30 days.

## Change Scope

Public behavior changes require Javadocs, focused unit tests, bilingual user
documentation, and a changelog entry. Keep pure routing, orientation, sizing,
and lifecycle policy isolated from OpenGL where possible so it can be tested in
the headless qualification fork.

Research, documentation and code contributions must cite their sources and
credit collaborators according to actual contribution. Do not submit private,
confidential or unpublished third-party material to generative-AI services.

Do not commit generated `build/`, `site/`, or `release/` contents. Release
artifacts are produced by Gradle and published from version tags.
