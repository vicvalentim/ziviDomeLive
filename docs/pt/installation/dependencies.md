# Dependências

## Bibliotecas Processing

| Dependência | Finalidade | Plataforma |
|---|---|---|
| ControlP5 `2.2.6` | Painel de controle interno | Todas |
| Syphon for Processing `4.0` | Compartilhamento GPU de textura | macOS |
| Spout for Processing `2.0.8.0` | Compartilhamento GPU de textura | Windows |

Instale dependências pelo Gerenciador de Contribuições quando disponíveis. Use somente a biblioteca de output local compatível com o sistema, mas mantenha todas as dependências declaradas quando o gerenciador de pacotes do Processing solicitá-las.

## Dependência Java Incluída

O pacote de release inclui Devolay `2.2.0-vic.1` para integração NDI. NDI ainda depende de bibliotecas nativas compatíveis e de um ambiente receiver em runtime.

## Bootstrap do Código-Fonte

`compileJava` executa `downloadDependencies` quando algum JAR local está ausente. O bootstrap baixa assets imutáveis e verifica os checksums SHA-256 do arquivo e do JAR antes da instalação.

```bash
./gradlew downloadDependencies
./gradlew build
```

Não substitua URLs fixas por assets mutáveis `latest`. Atualize versão e checksums em conjunto depois de verificação independente.
