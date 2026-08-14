# Generated Javadocs

The generated Java API reference is published beside the MkDocs site.

<div class="zd-grid" markdown>
<div class="zd-card" markdown>
### Public API reference

[Open generated Javadocs](../../reference/){ .zd-button }

Use this for class signatures, methods, enums, inheritance, and package-level Java details.
</div>

<div class="zd-card" markdown>
### Local artifact path

When you run `./gradlew buildReleaseArtifacts`, the same reference is packaged at:

```text
release/ziviDomeLive/reference/index.html
```
</div>
</div>

## Publishing model

The GitHub Pages workflow runs `./gradlew javadoc`, builds the MkDocs site, and copies `build/docs/javadoc` into `site/reference`. The same generated reference is also copied into `site/pt/reference` so localized pages can link to Javadocs without leaving the deployed site tree. After the site is deployed, the URL above resolves to the exact Javadocs for that commit.

For a local preview that includes Javadocs:

```bash
./gradlew javadoc --console=plain
.venv-docs/bin/mkdocs build --strict
cp -R build/docs/javadoc site/reference
mkdir -p site/pt
cp -R build/docs/javadoc site/pt/reference
python3 -m http.server 8000 --directory site
```
