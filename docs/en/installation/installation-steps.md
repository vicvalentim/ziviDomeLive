# Installation

## Processing Contribution Manager

After the 1.5.0 package is published:

1. Open Processing.
2. Select **Sketch > Import Library > Add Library...**.
3. Search for **ziviDomeLive**.
4. Install the library and its declared dependencies.
5. Restart Processing.

## Release Artifact

For manual installation, use the packaged artifact from the matching release rather than the repository source ZIP:

1. Download `ziviDomeLive.zip` or `ziviDomeLive.pdex`.
2. Extract the top-level `ziviDomeLive` folder.
3. Move it into the sketchbook `libraries` directory shown in Processing Preferences.
4. Install ControlP5 and the required platform output dependency.
5. Restart Processing.

The installed structure must contain:

```text
libraries/ziviDomeLive/
  library.properties
  library/
  examples/
  reference/
```

## Source Checkout

For development:

```bash
git clone https://github.com/vicvalentim/ziviDomeLive.git
cd ziviDomeLive
./gradlew buildReleaseArtifacts
./gradlew qualificationTests
```

The installable output is generated under `release/`.
