# Generated Javadocs

The generated Java API reference is published beside the MkDocs site.

<div class="zd-grid" markdown>
<div class="zd-card" markdown>
### Public API reference

<a class="zd-button" href="../../reference/">Open generated Javadocs</a>

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

The GitHub Pages workflow generates Javadocs, builds the bilingual MkDocs site, and runs `./gradlew attachJavadocsToSite` to publish one language-neutral reference at `site/reference`. Both languages use relative links to that canonical tree, so the same layout works at the project URL and inside pull-request preview subdirectories. After deployment, the button above resolves to the exact Javadocs for that commit.

For a local preview that includes Javadocs:

```bash
./gradlew javadoc --console=plain
python3 -m mkdocs build --strict
./gradlew attachJavadocsToSite --console=plain
python3 tools/validate_documentation.py --root . --site-dir site
python3 -m http.server 8000 --directory site
```
