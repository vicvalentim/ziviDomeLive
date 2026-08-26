# Future repository extraction readiness

- [x] zero Processing imports
- [x] zero backend imports
- [x] zero root-source dependencies
- [x] independent Core unit tests
- [x] self-contained `build.gradle.kts`
- [x] artifact metadata
- [x] sources JAR
- [x] Javadoc JAR
- [x] Maven POM
- [x] Maven-local external consumer smoke procedure
- [x] `jdeps` qualification procedure
- [x] standalone Core documentation
- [x] license migration identified: copy the project Apache-2.0 `LICENSE` into the extracted repo
- [x] CI migration identified: add Java 17 setup plus `clean check javadoc` and publication checks
- [x] `git-filter-repo` procedure documented

The architecture branch must first complete local unit, boundary, JAR, `jdeps`, POM, and external
consumer qualification. The future extraction operation is conceptually:

```text
git filter-repo \
  --path core/ \
  --path-rename core/:
```

Do not run that command in this repository. After extraction, add a Gradle wrapper and
`settings.gradle.kts`, copy the project license, configure CI/release credentials outside source,
and update SCM coordinates. Java sources require no refactor for separation.
