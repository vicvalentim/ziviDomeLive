# Dependências

## Bibliotecas Processing

| Dependência | Finalidade | Plataforma |
|---|---|---|
| ControlP5 `2.2.6` | Painel de controle interno | Todas |
| Syphon for Processing `4.0` | Compartilhamento GPU de textura | macOS |
| Spout for Processing `2.0.8.0` | Compartilhamento GPU de textura | Windows |

ControlP5 é obrigatório em todos os exemplos distribuídos e deve ser instalado explicitamente
pelo Gerenciador de Contribuições do Processing. `library.properties` não promete resolução
transitiva. O core ainda degrada defensivamente quando ControlP5 está ausente, desativando o painel.
Syphon e Spout são integrações opcionais de plataforma; quando ausentes, o output correspondente
fica `UNAVAILABLE`. Instale essas integrações pelo Gerenciador de Contribuições somente quando
precisar delas.

## Dependência Java Incluída

O pacote de release inclui o artefato público do Devolay, separado do runtime,
`2.2.0-vic.2` para output de vídeo NDI experimental. Devolay é uma dependência
Java/JNI embutida e, intencionalmente, não aparece como dependência do
Gerenciador de Contribuições do Processing. Seu NDI Runtime proprietário não é
incluído e deve ser instalado separadamente. O Processing não fornece uma
biblioteca NDI nativa oficial.

Siga as instruções de [Runtime NDI](ndi.md) específicas para o sistema antes de
habilitar esse output.

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
