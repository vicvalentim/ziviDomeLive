# Problemas Conhecidos

## OpenGL Error 1282

Algumas combinações de Processing/JOGL, hardware e driver emitem:

```text
OpenGL error 1282 at bot endDraw(): invalid operation
```

Esse `GL_INVALID_OPERATION` continua endêmico em algumas configurações de framebuffer, driver e múltiplos passes. A versão 1.4 removeu um lifecycle NDI aninhado que amplificava o erro, mas o problema mais amplo não é considerado resolvido. Ele costuma ser não fatal, porém sistemas de produção devem tratar mensagens repetidas como falha de qualificação até confirmar estabilidade de renderização e output.

Possíveis medidas:

- Mantenha outputs externos desabilitados quando não estiverem em uso.
- Use uma combinação estável de driver e Processing na máquina de destino.
- No macOS, qualifique a combinação exata de Processing/Syphon/GPU usada na implantação.
- Reduza a resolução de output enquanto isola o pass que falha.

## Apple Silicon e Syphon

O pacote upstream do Syphon for Processing 4.0 não fornece atualmente o
payload nativo `macos-aarch64` necessário ao Processing 4. Essa lacuna pode
aparecer como falha de carregamento JNI/biblioteca nativa em Apple Silicon.

Use o build comunitário universal de compatibilidade do ziviDomeLive:

[Syphon-for-Processing-4.0-macOS-universal-community.zip](https://github.com/vicvalentim/ziviDomeLive/releases/download/v2.0.0/Syphon-for-Processing-4.0-macOS-universal-community.zip)

O pacote contém slices nativos `arm64` + `x86_64` e não é uma release oficial
do Syphon Project. Substitua `libraries/Syphon/` em vez de mesclar arquivos.
Veja [Dependências](installation/dependencies.md) para checksum e proveniência
upstream.

Rosetta 2 não é o caminho normal de compatibilidade desse build.
## Outputs Externos no Linux

A renderização principal é suportada no Linux. Syphon e Spout não estão disponíveis nessa plataforma; o output de vídeo NDI é experimental e usa Devolay com um NDI Runtime oficial instalado separadamente. A combinação exata de runtime, driver, rede e receiver Linux ainda deve ser qualificada para implantação.

## Qualificação de Outputs Nativos

Testes automatizados validam routing e lifecycle sem abrir sessões reais de GPU ou receiver. Syphon, Spout, descoberta NDI, cor/orientação no receiver, resize, ciclos de enable/disable, pause/resume e shutdown devem ser verificados no hardware de destino.

NDI é um sender experimental, não oficial e somente de vídeo. Ele exige que o
NDI Runtime proprietário seja [instalado separadamente](installation/ndi.md),
pois não é fornecido pelo Gerenciador de Contribuições do Processing nem
incluído no pacote da biblioteca.

Se um envio nativo NDI não retornar durante o shutdown, a publicação para após uma espera limitada e o estado muda para `STOPPING`; a limpeza nativa termina depois da saída do worker.
