# External Maven consumer smoke

Run the reproducible qualification task:

```text
./gradlew :core:externalConsumerSmoke --warning-mode all
```

The task first publishes Core to the build-local Maven repository:

```text
./gradlew :core:publishCorePublicationToCoreTestRepository
```

An independent Java 17 Gradle project is then generated under
`core/build/external-consumer/`. Its only repository is
`core/build/maven-test-repository`, and its only dependency is:

```text
com.victorvalentim.zividomelive:zividomelive-core:0.1.0-SNAPSHOT
```

It must not use `project(":core")`, add repository source directories, or add Processing. The
smoke main imports `FrameClock`, `Quaternion`, `SphericalOrientation`, `OrbitCamera`, and
`ActionMap`, verifies representative behavior, and prints `CORE_EXTERNAL_CONSUMER_OK`. The task
captures the nested build result and fails if compilation/execution fails or the token is absent.

The dedicated Core CI runs this task offline on Ubuntu, macOS, and Windows with Java 17. The
consumer directory is generated evidence under `core/build/` and is intentionally not committed.
