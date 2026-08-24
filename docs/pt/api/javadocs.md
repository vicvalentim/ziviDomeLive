# Javadocs Gerados

A referência Java gerada é publicada junto com o site MkDocs.

<div class="zd-grid" markdown>
<div class="zd-card" markdown>
### Referência pública da API

<a class="zd-button" href="../../../reference/">Abrir Javadocs gerados</a>

Use esta página para assinaturas de classes, métodos, enums, herança e detalhes Java por pacote.
</div>

<div class="zd-card" markdown>
### Caminho do artefato local

Ao rodar `./gradlew buildReleaseArtifacts`, a mesma referência é empacotada em:

```text
release/ziviDomeLive/reference/index.html
```
</div>
</div>

## Modelo de publicação

O workflow do GitHub Pages gera os Javadocs, monta o site MkDocs bilíngue e executa `./gradlew attachJavadocsToSite` para publicar uma única referência Java, independente de idioma, em `site/reference`. Os dois idiomas usam caminhos relativos para essa árvore canônica, preservando as rotas tanto na URL do projeto quanto nos subdiretórios dos previews de pull request. Depois do deploy, o botão acima aponta para os Javadocs exatos daquele commit.

Para uma prévia local incluindo Javadocs:

```bash
./gradlew javadoc --console=plain
python3 -m mkdocs build --strict
./gradlew attachJavadocsToSite --console=plain
python3 tools/validate_documentation.py --root . --site-dir site
python3 -m http.server 8000 --directory site
```
