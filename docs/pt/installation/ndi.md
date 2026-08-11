# Runtime NDI

!!! warning "Integração experimental e não oficial"
    O output NDI do ziviDomeLive 1.5.0 é um sender de vídeo experimental mantido
    pela comunidade. Ele não é uma integração oficial do Processing ou do NDI e
    não possui afiliação nem endosso da Vizrt NDI AB. O Processing não fornece
    uma biblioteca NDI oficial: portanto, o suporte NDI não é instalado pelo
    Gerenciador de Contribuições do Processing.

    A versão 1.5.0 envia somente vídeo. Ela não oferece áudio NDI, recepção,
    tally, PTZ ou interface de descoberta. Qualifique o sender, receiver, rede,
    sistema operacional e formato de frame exatos antes do uso em produção.

NDI® é uma marca registrada da Vizrt NDI AB.

## Como a Integração é Empacotada

O ziviDomeLive utiliza o fork comunitário do Devolay como binding Java/JNI:

```text
ziviDomeLive
    -> API Java do Devolay
    -> biblioteca JNI Devolay empacotada
    -> NDI Runtime instalado separadamente
    -> rede NDI
```

O artefato público `io.github.vicvalentim:devolay:2.2.0-vic.1` é um
**separated build**. Ele inclui as classes Devolay e binários JNI para desktop,
mas não inclui `Processing.NDI.Lib.x64.dll`, `libndi.dylib` ou `libndi.so.6`, que
são proprietários. O ziviDomeLive intencionalmente não usa o modo integrado do
Devolay. Um NDI Runtime deve ser instalado de forma independente em toda máquina
que enviar vídeo com ziviDomeLive.

Consulte o [modelo de runtime do Devolay](https://github.com/vicvalentim/devolay#ndi-runtime),
as [ferramentas NDI oficiais](https://ndi.video/tools/) e o
[NDI SDK oficial](https://ndi.video/for-developers/ndi-sdk/). Use sempre o
download oficial atual e leia a licença vigente; não obtenha os binários do
runtime a partir do JAR do ziviDomeLive ou do Devolay.

## Windows

O Processing 4 normalmente executa como aplicação 64-bit; use um NDI 6 Runtime
64-bit atual.

1. Feche o Processing e qualquer aplicação que esteja usando NDI.
2. Baixe e instale o [NDI Tools para Windows](https://ndi.video/tools/) atual,
   ou o NDI Runtime standalone atual fornecido pela distribuição oficial NDI.
3. Reinicie o Processing. Reinicie o Windows caso o instalador solicite ou um
   processo antigo do Processing ainda possua o ambiente anterior.
4. Ative NDI no ziviDomeLive e confira o output no NDI Studio Monitor ou em
   outro receiver compatível.

O Devolay carrega `Processing.NDI.Lib.x64.dll` a partir do diretório indicado
por `NDI_RUNTIME_DIR_V6`. O instalador oficial normalmente configura essa
variável. Para uma instalação customizada do SDK/runtime, defina a variável no
ambiente do usuário ou sistema **antes** de abrir o Processing; ela deve apontar
para o diretório que contém diretamente a DLL.

## macOS

O fork mantido do Devolay contém JNI para Intel x86-64 e Apple Silicon aarch64,
mas o NDI Runtime continua sendo uma instalação separada.

1. Feche o Processing e as aplicações NDI.
2. Instale o [NDI Tools para macOS](https://ndi.video/tools/) atual, ou o NDI
   Runtime standalone atual da distribuição oficial.
3. Reinicie o Processing e permita acesso à rede local caso o macOS solicite.
4. Confira o sender com o NDI Video Monitor ou outro receiver compatível.

O Devolay procura o runtime no caminho comum `/usr/local/lib/libndi.dylib`. Em
uma instalação completa do NDI SDK, ele também procura:

```text
/Library/NDI SDK for Apple/lib/macOS/libndi.dylib
```

Para uma instalação fora desses caminhos, defina `NDI_RUNTIME_DIR_V6` antes de
iniciar o Processing. O valor deve ser o diretório que contém `libndi.dylib`,
não o próprio arquivo.

## Linux

NDI no Linux é experimental e não integra a matriz de outputs qualificados da
1.5.0. O pacote desktop público NDI Tools é oferecido para Windows e macOS;
usuários Linux devem obter o SDK/runtime atual na
[página oficial do NDI SDK](https://ndi.video/for-developers/ndi-sdk/) e aceitar
sua licença durante a instalação.

1. Baixe o NDI SDK atual para Linux pela fonte oficial.
2. Execute o instalador incluído no arquivo e localize o diretório da arquitetura
   que contém `libndi.so.6`.
3. Antes de abrir o Processing pelo mesmo terminal, indique o runtime ao Devolay:

```bash
export NDI_RUNTIME_DIR_V6="/caminho/absoluto/para/o/diretorio-com-libndi.so.6"
```

4. Inicie o Processing nesse ambiente e confira o sender com um receiver na rede.

O Devolay também procura `/usr/local/lib/libndi.so.6` e
`/usr/lib/libndi.so.6`. Ao instalar o runtime no sistema, siga as instruções e a
licença do NDI SDK e atualize o cache do dynamic linker quando a distribuição
exigir. Não copie binários do NDI Runtime para este repositório ou para o pacote
ziviDomeLive.

## Verificação pelo Sketch

A inicialização NDI é tardia e começa somente quando a publicação é habilitada:

```java
OutputManager outputs = ziviDome.getOutputManager();
outputs.toggleOutput("ndi");

println(outputs.getOutputState(OutputManager.OutputType.NDI));
println(outputs.getOutputFailureReason(OutputManager.OutputType.NDI));
```

O estado esperado depois da inicialização é `ENABLED`. Falhas comuns:

| Diagnóstico | Significado | Ação |
|---|---|---|
| `NDI Runtime libraries were not found` | Nenhum runtime foi encontrado em `NDI_RUNTIME_DIR_V6` ou nos caminhos suportados | Instale o runtime ou corrija o diretório antes de abrir o Processing |
| `NDI Runtime libraries failed to load` | O arquivo foi encontrado, mas não pôde ser carregado | Reinstale um runtime 64-bit atual compatível e confira arquitetura/permissões |
| Sender habilitado, mas invisível | O runtime abriu; discovery, firewall, subnet ou receiver podem impedir a visibilidade | Teste em uma única subnet, libere Processing/Java no firewall e use o monitor NDI oficial |

A suíte automatizada verifica roteamento, conversão RGBA, metadata progressiva,
backpressure e shutdown sem abrir uma sessão NDI real. O uso em produção ainda
exige o [protocolo de qualificação de hardware](../qualification/1.5-release-readiness.md).

## Limite de Licenciamento

Devolay é Apache-2.0; ziviDomeLive é GPL-2.0-only. O NDI Runtime proprietário é
coberto separadamente pela licença e pelos termos de distribuição atuais do NDI
SDK. Instalar ou redistribuir o runtime não o transforma em parte de nenhuma das
licenças open source. Quem distribuir um produto com binários do NDI Runtime é
responsável por revisar os requisitos atuais de licença, distribuição,
identificação e marca registrada.
