# Instalação

## Gerenciador de Contribuições

Depois da publicação do pacote 2.0.0:

1. Abra o Processing.
2. Selecione **Sketch > Import Library > Add Library...**.
3. Pesquise por **ziviDomeLive**.
4. Instale a biblioteca e suas dependências declaradas.
5. Reinicie o Processing.

Abra **File > Examples > Contributed Libraries > ziviDomeLive > EmptyProject**.
A cena preta vazia deve iniciar sem erros de shader ou dependência; pressione
`h` para confirmar que o painel de controle pode ser exibido e ocultado.

NDI é opcional e não pode ser instalado pelo Gerenciador de Contribuições do
Processing. Instale separadamente o [Runtime NDI](ndi.md) do sistema antes de
habilitar o output de vídeo NDI experimental.

## Artefato de Release

Para instalação manual, use o artefato empacotado do release correspondente, não o ZIP do código-fonte:

1. Baixe `ziviDomeLive.zip` ou `ziviDomeLive.pdex`.
2. Extraia a pasta superior `ziviDomeLive`.
3. Mova-a para o diretório `libraries` do sketchbook indicado nas preferências do Processing.
4. Instale ControlP5 e a dependência de output da plataforma.
5. Reinicie o Processing.

O pacote inclui Devolay, mas não o NDI Runtime proprietário. Usuários NDI devem
concluir a instalação separada do [Runtime NDI](ndi.md) correspondente ao
sistema operacional.

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
./gradlew buildReleaseArtifacts
./gradlew qualificationTests
```

O pacote instalável é gerado em `release/`.

Para um deploy local no sketchbook em vez de gerar o pacote:

```bash
./gradlew deployToProcessingSketchbook
```

Essa tarefa instala a biblioteca e os exemplos, mas exclui intencionalmente
`src/test` e não executa a qualificação. Rode `qualificationTests` separadamente
antes de considerar o checkout pronto para release.
