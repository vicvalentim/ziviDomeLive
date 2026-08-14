# Checklist de Publicação Processing

Use este checklist antes de submeter ziviDomeLive ao Gerenciador de Contribuições do Processing ou publicar uma atualização. Ele segue as diretrizes de bibliotecas Processing para metadados, documentação, exemplos, artefatos de release, disponibilidade do código-fonte e licenciamento.

## Conteúdo obrigatório do release

| Requisito | Local em ziviDomeLive | Verificação |
|---|---|---|
| Metadados Processing | `library.properties`, `release/ziviDomeLive.txt` | `name`, `authors`, `url`, `categories`, `sentence` e `version` inteiro estão presentes |
| Arquivo instalável | `release/ziviDomeLive.zip` | Contém `ziviDomeLive/library/ziviDomeLive.jar` |
| Artefato instalador PDE | `release/ziviDomeLive.pdex` | Idêntico ao ZIP, com extensão `.pdex` |
| Exemplos | `release/ziviDomeLive/examples/` | Exemplos abrem pelo menu **File > Examples** do Processing |
| Documentação de referência | `release/ziviDomeLive/reference/index.html` | Gerada por Javadocs e atualizada para o release |
| Código-fonte | Repositório GitHub | Código público permanece disponível para revisão e manutenção |
| Arquivos de licença | `LICENSE`, `THIRD_PARTY.md`, `licenses/` | Incluídos no arquivo de release |

## Regras de metadados

O Gerenciador de Contribuições lê `library.properties`. Mantenha estes campos alinhados ao release:

- `name`: `ziviDomeLive`
- `authors`: lista de autores com links quando aplicável
- `url`: página pública estável da documentação, não um link direto de download
- `categories`: somente categorias Processing; este release usa `3D, Video & Vision`
- `sentence`: uma frase curta, iniciada com maiúscula e terminada com ponto, sem repetir o nome da biblioteca
- `paragraph`: segunda frase em diante para o site Processing; mencione limitações específicas de plataforma aqui
- `version`: contador inteiro usado para verificação de updates
- `prettyVersion`: versão legível, sem espaços
- `minRevision` e `maxRevision`: limites de revisão do Processing; mantenha `maxRevision=0` a menos que uma revisão futura incompatível seja conhecida

## Página pública da documentação

O site público da documentação deve permanecer em uma URL estável e incluir:

- um resumo curto do propósito da biblioteca;
- instruções de instalação pelo Gerenciador de Contribuições e por instalação manual;
- exemplos de uso básico e avançado;
- sistemas operacionais e versão Processing testados;
- notas de dependências e runtime, incluindo separação do NDI Runtime;
- keywords e versão mais recente vindas de `library.properties`;
- links para os artefatos ZIP/PDEX quando publicados;
- Javadocs gerados em `/reference/`.

## Validação manual antes da submissão

1. Rode `./gradlew clean test build --console=plain`.
2. Rode `./gradlew buildReleaseArtifacts --console=plain`.
3. Confirme que `release/ziviDomeLive.zip`, `release/ziviDomeLive.pdex` e `release/ziviDomeLive.txt` usam o mesmo nome base e o mesmo diretório.
4. Instale o pacote por `./gradlew deployToProcessingSketchbook --console=plain` ou por extração manual.
5. Reinicie o Processing e abra **File > Examples > Contributed Libraries > ziviDomeLive**.
6. Abra todos os exemplos mantidos: `EmptyProject`, `Basic`, `SphereParticle`,
   `CalibrationTool`, `FulldomePBR`, `InfiniteBackground` e `SolarSystem`.
7. Registre versão do Processing, sistema operacional, arquitetura de CPU, GPU, driver, backend de output e qualquer aviso OpenGL observado.
8. Complete o checklist de [Prontidão da Release 2.0](2.0-release-readiness.md) para evidências GPU e outputs nativos.

## Nota de submissão

Quando os artefatos e a documentação estiverem finais, submeta a biblioteca pelo formulário de issue do repositório Processing Contributions. Mantenedores Processing podem solicitar ajustes de metadados, empacotamento ou documentação antes de indexar o release.

## Referências oficiais

- [Processing Library Guidelines](https://github.com/processing/processing4/wiki/Library-Guidelines)
- [Processing Library Basics](https://github.com/processing/processing4/wiki/Library-Basics)
- [Processing Library Overview](https://github.com/processing/processing4/wiki/Library-Overview)
- [Processing Library Template: Release](https://processing.github.io/processing-library-template/release.html)
