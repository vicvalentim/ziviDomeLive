# v2.0.0 Tag Gate

The tag is a publication event, not an experiment.

## Before creating the tag

1. complete `maintainer/release-evidence.md`;
2. review the final hero/Mermaid diagrams and attach only real installed-package captures as visual evidence;
3. run the final automated pre-release workflow or the equivalent commands locally;
4. inspect generated ZIP/TXT/PDEX;
5. install the generated Processing package and run the eight examples/tools;
6. confirm only actually qualified platform/backend combinations are claimed;
7. verify citation/Zenodo metadata and the software DOI;
8. confirm the working tree is clean and record the final HEAD.

## Final local commands

```bash
./gradlew clean test build --console=plain
./gradlew qualificationTests --console=plain
python3 tools/validate_documentation.py --root .
python3 -m mkdocs build --strict
./gradlew buildReleaseArtifacts --console=plain
python3 tools/validate_documentation.py \
  --root . \
  --release-dir release \
  --package release/ziviDomeLive.zip \
  --release-evidence
```

A failure is a release blocker until explained and resolved. Do not create `v2.0.0` first and use the tag workflow to discover whether the release was ready.
