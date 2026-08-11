# Instalação

## Gerenciador de Contribuições

Depois da publicação do pacote 1.5.0:

1. Abra o Processing.
2. Selecione **Sketch > Import Library > Add Library...**.
3. Pesquise por **ziviDomeLive**.
4. Instale a biblioteca e suas dependências declaradas.
5. Reinicie o Processing.

## Artefato de Release

Para instalação manual, use o artefato empacotado do release correspondente, não o ZIP do código-fonte:

1. Baixe `ziviDomeLive.zip` ou `ziviDomeLive.pdex`.
2. Extraia a pasta superior `ziviDomeLive`.
3. Mova-a para o diretório `libraries` do sketchbook indicado nas preferências do Processing.
4. Instale ControlP5 e a dependência de output da plataforma.
5. Reinicie o Processing.

A estrutura instalada deve conter:

```text
libraries/ziviDomeLive/
  library.properties
  library/
  examples/
  reference/
```

## Checkout do Código-Fonte

Para desenvolvimento:

```bash
git clone https://github.com/vicvalentim/ziviDomeLive.git
cd ziviDomeLive
./gradlew clean test build
./gradlew buildReleaseArtifacts
```

O pacote instalável é gerado em `release/`.
