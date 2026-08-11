# Dependencies

## Processing Libraries

| Dependency | Purpose | Platform |
|---|---|---|
| ControlP5 `2.2.6` | Built-in control panel | All |
| Syphon for Processing `4.0` | GPU texture sharing | macOS |
| Spout for Processing `2.0.8.0` | GPU texture sharing | Windows |

Install dependencies through Processing's Contribution Manager where available. Use only the local output library that matches the operating system, but keep all declared library dependencies available when the Processing package manager requests them.

## Bundled Java Dependency

The release package includes Devolay `2.2.0-vic.1` for NDI integration. NDI still depends on compatible native libraries and a receiver environment at runtime.

## Source Build Bootstrap

`compileJava` runs `downloadDependencies` when a required local JAR is missing. The bootstrap downloads immutable assets and verifies both archive and JAR SHA-256 checksums before installation.

```bash
./gradlew downloadDependencies
./gradlew build
```

Do not replace the pinned URLs with mutable `latest` assets. Update version and checksums together after independent verification.
