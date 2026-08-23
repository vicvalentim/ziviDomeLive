# Dependências

## Bibliotecas Processing

| Dependência | Finalidade | Plataforma |
|---|---|---|
| ControlP5 `2.2.6` | Painel de controle interno | Todas |
| Syphon for Processing `4.0` | Compartilhamento GPU de textura | macOS |
| Spout for Processing `2.0.8.0` | Compartilhamento GPU de textura | Windows |

Instale dependências pelo Gerenciador de Contribuições quando disponíveis. Use somente a biblioteca de output local compatível com o sistema, mas mantenha todas as dependências declaradas quando o gerenciador de pacotes do Processing solicitá-las.

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

`compileJava` executa `downloadDependencies` quando algum JAR local está ausente. O bootstrap baixa assets imutáveis e verifica os checksums SHA-256 do arquivo e do JAR antes da instalação.

```bash
./gradlew downloadDependencies
./gradlew build
```

Não substitua URLs fixas por assets mutáveis `latest`. Atualize versão e checksums em conjunto depois de verificação independente.
