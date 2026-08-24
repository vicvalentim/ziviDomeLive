# Dependencies

## Processing Libraries

| Dependency | Purpose | Platform |
|---|---|---|
| ControlP5 `2.2.6` | Built-in control panel | All |
| Syphon for Processing `4.0` | GPU texture sharing | macOS |
| Spout for Processing `2.0.8.0` | GPU texture sharing | Windows |

ControlP5 is required by every distributed example and must be installed explicitly through the
Processing Contribution Manager. `library.properties` does not claim transitive dependency
resolution. The core still degrades defensively if ControlP5 is missing by disabling only the panel.
Syphon and Spout are optional platform integrations; when absent, the matching output reports
`UNAVAILABLE`. Install those integrations through Processing's Contribution Manager only when
you need them.

## Bundled Java Dependency

The release package includes the public, runtime-separated Devolay
`2.2.0-vic.2` artifact for experimental NDI video output. Devolay is a bundled
Java/JNI dependency and is intentionally not listed as a Processing Contribution
Manager dependency. Its proprietary NDI Runtime is not bundled and must be
installed separately. Processing does not supply an official native NDI library.

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
