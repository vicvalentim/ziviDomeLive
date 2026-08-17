---
title: "Threading"
icon: material/source-branch
---
# Threading

Drawing Processing e acesso OpenGL são restritos pelo contexto gráfico ativo do renderer.

Drawing de cena e operações GL controladas pelo renderer ocorrem na render thread. Trabalho em background não deve realizar chamadas GL arbitrárias.

Scene Services pode fornecer mecanismos de background e retorno de trabalho ao contexto correto; siga a API/Javadocs correntes. No NDI, envio em rede pode ocorrer em worker depois da fronteira GPU→CPU, mas captura GL continua context-bound.