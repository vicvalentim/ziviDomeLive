# Installation

## Processing Contribution Manager

After the 2.0.0 package is published:

1. Open Processing.
2. Select **Sketch > Import Library > Add Library...**.
3. Search for **ziviDomeLive**.
4. Install ziviDomeLive.
5. Install **ControlP5** explicitly through the Contribution Manager if it is not already installed.
6. Restart Processing.

After installation, open **File > Examples > Contributed Libraries > ziviDomeLive > EmptyProject**. The empty scene should start without shader or dependency errors. Press `h` to confirm that the control panel can be shown and hidden.

NDI is optional and cannot be installed through Processing's Contribution Manager. Install the system [NDI Runtime](ndi.md) separately before enabling the experimental NDI video output.

### Apple Silicon + Syphon

The upstream Syphon for Processing 4.0 package does not currently include the native Apple Silicon payload required by Processing 4.

1. Download [Syphon-for-Processing-4.0-macOS-universal-community.zip](https://github.com/vicvalentim/ziviDomeLive/releases/download/v2.0.0/Syphon-for-Processing-4.0-macOS-universal-community.zip).
2. Quit Processing completely.
3. Back up or remove the existing Sketchbook `libraries/Syphon/` directory.
4. Extract the ZIP into `libraries/` so the installed path is `libraries/Syphon/`.
5. Restart Processing.

Do not merge it over an older Syphon installation. See [Dependencies](dependencies.md) for provenance and SHA-256.
## Release Artifact

For manual installation, use the packaged artifact from the matching release. Do not install the repository source ZIP as a Processing library:

1. Download `ziviDomeLive.zip` or `ziviDomeLive.pdex`.
2. Extract the top-level `ziviDomeLive` folder.
3. Move it into the sketchbook `libraries` directory shown in Processing Preferences.
4. Install the required external ControlP5 library explicitly; install Syphon or Spout only when the corresponding optional platform output is needed.
5. Restart Processing.

The archive includes Devolay but not the proprietary NDI Runtime. NDI users must
complete the separate [NDI Runtime](ndi.md) installation for their operating
system.

The installed structure must follow this Processing library layout:

```text
libraries/ziviDomeLive/
  library.properties
  library/
  examples/
    GettingStarted/
    Advanced/
    Tools/
  reference/
```

## Source Checkout

Use a source checkout only for development or release verification:

```bash
git clone https://github.com/vicvalentim/ziviDomeLive.git
cd ziviDomeLive
./gradlew buildReleaseArtifacts
./gradlew qualificationTests
```

The installable output is generated under `release/` as `ziviDomeLive.zip`, `ziviDomeLive.pdex`, and `ziviDomeLive.txt`.

For a local sketchbook deployment instead of a release package:

```bash
./gradlew deployToProcessingSketchbook
```

This task deploys the library and examples, but deliberately excludes `src/test` and does not execute qualification. Run `qualificationTests` separately before treating the checkout as release-ready. For publication checks, see [Processing Publication](../qualification/processing-publication.md).
