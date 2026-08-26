# External Maven consumer smoke

The separation check publishes Core to the build-local Maven repository:

```text
./gradlew :core:publishCorePublicationToCoreTestRepository
```

An independent Java 17 Gradle project is then created under
`build/core-external-consumer/`. Its only repository is
`core/build/maven-test-repository`, and its only dependency is:

```text
com.victorvalentim.zividomelive:zividomelive-core:0.1.0-SNAPSHOT
```

It must not use `project(":core")`, add repository source directories, or add Processing. The
smoke main imports `FrameClock`, `Quaternion`, `SphericalOrientation`, `OrbitCamera`, and
`ActionMap`, verifies representative behavior, and prints `CORE_EXTERNAL_CONSUMER_OK`.

This architecture branch qualified the consumer offline with Gradle 8.5 and Java 17.0.18. The
consumer directory is generated evidence under `build/` and is intentionally not committed.
