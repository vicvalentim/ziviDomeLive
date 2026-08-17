---
title: "Tasks em Background"
icon: material/layers-triple-outline
---
# Tasks em Background

Trabalho em background serve a tarefas CPU/rede/arquivo que não devem bloquear rendering, mas não possui o contexto OpenGL do Processing. Encaminhe trabalho gráfico pelo mecanismo corrente de render thread de Scene Services e libere/cancele trabalho da cena em `dispose()` quando exigido pela API.
