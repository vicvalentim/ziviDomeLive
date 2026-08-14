# Javadocs Gerados

A referência Java gerada é publicada junto com o site MkDocs.

<div class="zd-grid" markdown>
<div class="zd-card" markdown>
### Referência pública da API

<a class="zd-button" href="../../reference/">Abrir Javadocs gerados</a>

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

O workflow do GitHub Pages roda `./gradlew javadoc`, gera o site MkDocs e copia `build/docs/javadoc` para `site/reference`. A mesma referência gerada também é copiada para `site/pt/reference`, para que páginas localizadas apontem para os Javadocs sem sair da árvore publicada do site. Depois do deploy, a URL acima aponta para os Javadocs exatos daquele commit.

Para uma prévia local incluindo Javadocs:

```bash
./gradlew javadoc --console=plain
.venv-docs/bin/mkdocs build --strict
cp -R build/docs/javadoc site/reference
mkdir -p site/pt
cp -R build/docs/javadoc site/pt/reference
python3 -m http.server 8000 --directory site
```
