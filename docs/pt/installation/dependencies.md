# Dependências

## Bibliotecas Processing

| Dependência | Finalidade | Plataforma | Repositório |
|---|---|---|---|
| ControlP5 `2.2.6` | Painel de controle interno | Todas | [sojamo/controlp5](https://github.com/sojamo/controlp5) |
| Syphon for Processing `4.0` | Compartilhamento GPU de textura | macOS | [Syphon/Processing](https://github.com/Syphon/Processing) |
| Spout for Processing `2.0.8.0` | Compartilhamento GPU de textura | Windows | [leadedge/SpoutProcessing](https://github.com/leadedge/SpoutProcessing) |

ControlP5 é obrigatório em todos os exemplos distribuídos e deve ser instalado explicitamente
pelo Gerenciador de Contribuições do Processing. `library.properties` não promete resolução
transitiva. O core ainda degrada defensivamente quando ControlP5 está ausente, desativando somente o painel.
Syphon e Spout são integrações opcionais de plataforma; quando ausentes, o output correspondente
fica `UNAVAILABLE`.

### ControlP5 — obrigatório

1. Abra o Processing.
2. Selecione **Sketch → Import Library… → Manage Libraries…**.
3. Procure por **ControlP5**.
4. Instale ControlP5 `2.2.6`, ou a versão compatível oferecida pelo Gerenciador de Contribuições.
5. Reinicie o Processing antes de abrir os exemplos do ziviDomeLive.

Todos os sketches distribuídos pelo ziviDomeLive importam:

```java
import controlP5.*;
```

O core possui um fallback defensivo quando ControlP5 está ausente.
Esse comportamento existe para resiliência e não define o fluxo oficial
de instalação.

### Syphon — opcional, macOS

O wrapper para Processing é mantido em [Syphon/Processing](https://github.com/Syphon/Processing), o framework nativo em [Syphon/Syphon-Framework](https://github.com/Syphon/Syphon-Framework) e a ponte Java/JNI em [Syphon/Java](https://github.com/Syphon/Java).

O pacote upstream do Syphon for Processing 4.0 não fornece atualmente o payload nativo Apple Silicon exigido pelo Processing 4 em `macos-aarch64`. Usuários Apple Silicon podem instalar:

[Syphon-for-Processing-4.0-macOS-universal-community.zip](https://github.com/vicvalentim/ziviDomeLive/releases/download/v2.0.0/Syphon-for-Processing-4.0-macOS-universal-community.zip)

SHA-256: `59996d8e984c8662e1b964768861e28faa04ab9495daa641a0e14a5a1bf35995`

O pacote contém binários nativos universais `arm64` + `x86_64` para `libJSyphon.jnilib`, `JSyphon.so` e `Syphon.framework`. Ele não é uma release oficial do Syphon Project e preserva a API/identidade upstream do Syphon for Processing 4.0.

Feche o Processing, substitua o diretório `libraries/Syphon/` existente no Sketchbook pelo pacote extraído e reinicie o Processing. Não mescle sobre uma pasta Syphon antiga.

Um sketch normal do ziviDomeLive não precisa importar o pacote Syphon.
Adicione `codeanticode.syphon.*` somente quando o código usar diretamente
a API do Syphon.

### Spout — opcional, Windows

Instale **Spout for Processing 2.0.8.0** pelo Gerenciador de Contribuições
somente quando precisar do output Spout.

Um sketch normal do ziviDomeLive não precisa importar `spout.*`.
Adicione esse import somente quando o código usar diretamente a API do Spout.
## Dependência Java Incluída

O pacote de release inclui o artefato público do Devolay, separado do runtime,
`2.2.0-vic.2` para output de vídeo NDI experimental. O código-fonte mantido está
em [vicvalentim/devolay](https://github.com/vicvalentim/devolay), fork
comunitário de [WalkerKnapp/devolay](https://github.com/WalkerKnapp/devolay).

Devolay é uma dependência Java/JNI embutida e, intencionalmente, não aparece como dependência do
Gerenciador de Contribuições do Processing. Seu NDI Runtime proprietário não é
incluído e deve ser instalado separadamente. O Processing não fornece uma
biblioteca NDI nativa oficial.

Siga as instruções de [Runtime NDI](ndi.md) antes de habilitar esse output.
## Bootstrap do Código-Fonte

`compileJava` executa a tarefa Gradle/JVM multiplataforma `downloadDependencies`. Ela usa uma URL
versionada do ControlP5 e IDs imutáveis de assets GitHub para Syphon/Spout, verificando os
checksums SHA-256 do archive e do JAR antes da instalação. Bash, `unzip` e `sha256sum` não são
necessários.

```bash
./gradlew downloadDependencies
./gradlew build
```

Não substitua URLs fixas por assets mutáveis `latest`. Atualize versão e checksums em conjunto depois de verificação independente.

Para tarefas de deploy, configure um sketchbook não padrão com
`-PprocessingSketchbook=/caminho/do/sketchbook` ou `PROCESSING_SKETCHBOOK`.
