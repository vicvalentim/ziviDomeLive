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
- No Apple Silicon, compare Processing ARM nativo e Intel/Rosetta quando Syphon for necessário.
- Reduza a resolução de output enquanto isola o pass que falha.

## Apple Silicon e Syphon

A interoperabilidade completa com Syphon pode exigir Processing Intel sob Rosetta 2. Renderização ARM nativa e Syphon são questões separadas de qualificação.

## Outputs Externos no Linux

A renderização principal deve funcionar no Linux, mas as integrações atuais do Processing não oferecem Syphon ou Spout nessa plataforma, e o suporte nativo a NDI permanece reduzido/não qualificado.

## Qualificação de Outputs Nativos

Testes automatizados validam routing e lifecycle sem abrir sessões reais de GPU ou receiver. Syphon, Spout, descoberta NDI, cor/orientação no receiver, resize, ciclos de enable/disable, pause/resume e shutdown devem ser verificados no hardware de destino.

Se um envio nativo NDI não retornar durante o shutdown, a publicação para após uma espera limitada e o estado muda para `STOPPING`; a limpeza nativa termina depois da saída do worker.
