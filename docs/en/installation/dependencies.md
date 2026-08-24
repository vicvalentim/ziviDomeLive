# Dependencies

## Processing Libraries

| Dependency | Purpose | Platform | Repository |
|---|---|---|---|
| ControlP5 `2.2.6` | Built-in control panel | All | [sojamo/controlp5](https://github.com/sojamo/controlp5) |
| Syphon for Processing `4.0` | GPU texture sharing | macOS | [Syphon/Processing](https://github.com/Syphon/Processing) |
| Spout for Processing `2.0.8.0` | GPU texture sharing | Windows | [leadedge/SpoutProcessing](https://github.com/leadedge/SpoutProcessing) |

ControlP5 is required by every distributed example and must be installed explicitly through the
Processing Contribution Manager. `library.properties` does not claim transitive dependency
resolution. The core still degrades defensively if ControlP5 is missing by disabling only the panel.
Syphon and Spout are optional platform integrations; when absent, the matching output reports
`UNAVAILABLE`.

### ControlP5 — required

1. Open Processing.
2. Select **Sketch → Import Library… → Manage Libraries…**.
3. Search for **ControlP5**.
4. Install ControlP5 `2.2.6`, or the compatible release offered by the Processing Contribution Manager.
5. Restart Processing before opening the ziviDomeLive examples.

Every distributed ziviDomeLive sketch imports:

```java
import controlP5.*;
```

The core contains a defensive fail-soft path when ControlP5 is unavailable.
That behavior exists for resilience and does not define the supported
installation workflow.

### Syphon — optional, macOS

The Processing wrapper is maintained at [Syphon/Processing](https://github.com/Syphon/Processing), the native framework at [Syphon/Syphon-Framework](https://github.com/Syphon/Syphon-Framework), and the Java/JNI bridge at [Syphon/Java](https://github.com/Syphon/Java).

The upstream Syphon for Processing 4.0 package does not currently ship the native Apple Silicon payload required by Processing 4 on `macos-aarch64`. Apple Silicon users can install:

[Syphon-for-Processing-4.0-macOS-universal-community.zip](https://github.com/vicvalentim/ziviDomeLive/releases/download/v2.0.0/Syphon-for-Processing-4.0-macOS-universal-community.zip)

SHA-256: `59996d8e984c8662e1b964768861e28faa04ab9495daa641a0e14a5a1bf35995`

The package contains universal `arm64` + `x86_64` native binaries for `libJSyphon.jnilib`, `JSyphon.so` and `Syphon.framework`. It is not an official Syphon Project release and preserves the upstream Syphon for Processing 4.0 API/library identity.

Quit Processing, replace the existing Sketchbook `libraries/Syphon/` directory with the extracted package, and restart Processing. Do not merge it over an older Syphon directory.

A normal ziviDomeLive sketch does not need to import the Syphon package.
Add `codeanticode.syphon.*` only when sketch code directly uses the Syphon API.

### Spout — optional, Windows

Install **Spout for Processing 2.0.8.0** through the Processing Contribution
Manager only when Spout output is required.

A normal ziviDomeLive sketch does not need to import `spout.*`.
Add it only when sketch code directly uses the Spout API.
## Bundled Java Dependency

The release package includes the public, runtime-separated Devolay
`2.2.0-vic.2` artifact for experimental NDI video output. The maintained source
is [vicvalentim/devolay](https://github.com/vicvalentim/devolay), a
community-maintained fork of
[WalkerKnapp/devolay](https://github.com/WalkerKnapp/devolay).

Devolay is a bundled Java/JNI dependency and is intentionally not listed as a
Processing Contribution Manager dependency. Its proprietary NDI Runtime is not
bundled and must be installed separately. Processing does not supply an official
native NDI library.

Follow the operating-system-specific [NDI Runtime](ndi.md) instructions before
enabling this output.
## Source Build Bootstrap

`compileJava` runs the cross-platform Gradle/JVM `downloadDependencies` task. It uses a versioned
ControlP5 URL and immutable GitHub asset IDs for Syphon/Spout, then verifies both archive and JAR
SHA-256 checksums before installation. It does not require Bash, `unzip`, or `sha256sum`.

```bash
./gradlew downloadDependencies
./gradlew build
```

Do not replace the pinned URLs with mutable `latest` assets. Update version and checksums together after independent verification.

Set a non-default Processing sketchbook only for deployment tasks with
`-PprocessingSketchbook=/path/to/sketchbook` or `PROCESSING_SKETCHBOOK`.
