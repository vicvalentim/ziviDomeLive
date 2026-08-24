---
title: "Backends de Output"
icon: material/source-branch
---
# Backends de Output

A API artist-facing trata cada output como destino que solicita um `ViewType`; internamente o transporte varia.

**NDI:** fronteira CPU/rede na arquitetura corrente. A view final é capturada para dados acessíveis à CPU e enviada pelo caminho NDI/Devolay. Qualificação exige receiver real.

**Syphon:** compartilhamento local de textura GPU no macOS; exige receiver real na configuração declarada.

**Spout:** compartilhamento local de textura GPU no Windows; exige receiver real na configuração declarada.

Backends consomem views finais; não devem possuir lógica de renderização da cena.

O target final transporta RGBA. NDI converte pixels ARGB do Processing para RGBA packed sem
substituir alpha, enquanto Syphon e Spout recebem diretamente a textura Processing concluída. O
suporte do receiver precisa de qualificação separada: um receiver ou compositor de janela que
descarte alpha não redefine o contrato de framebuffer transparente da biblioteca.
