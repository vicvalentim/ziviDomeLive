# Problemas Conhecidos

## Apple Silicon e Syphon

No macOS com Apple Silicon, a interoperabilidade completa com Syphon pode exigir a versão Intel do Processing rodando via Rosetta 2. A stack ARM nativa do Processing não oferece o mesmo nível de suporte ao Syphon.

## Outputs externos no Linux

Builds Linux têm suporte reduzido para saídas de vídeo externas em comparação com macOS e Windows, pois as dependências do ecossistema Processing usadas por esta biblioteca não fornecem as mesmas integrações nativas para NDI, Syphon e Spout.

## OpenGL error 1282

Algumas configurações emitem:

```text
OpenGL error 1282 at bot endDraw(): invalid operation
```

Esse é um `GL_INVALID_OPERATION` emitido pelo driver OpenGL do JOGL/Processing, geralmente acionado por estado inválido do framebuffer durante renderização ou captura de output. O erro é endêmico em certas combinações de hardware e driver e **não foi completamente eliminado**. Em geral é não-fatal — a renderização continua — mas pode indicar instabilidade em configurações específicas (especialmente Apple Silicon, certos drivers de GPU, ou renderização multi-pass complexa). A investigação da causa raiz está em andamento.

**Medidas que podem reduzir a frequência:**
- Rode o Processing na versão Intel (Rosetta 2) em Apple Silicon.
- Mantenha os outputs externos (NDI, Syphon, Spout) desabilitados quando não estiverem em uso.
- Use os drivers de GPU mais recentes para sua plataforma.