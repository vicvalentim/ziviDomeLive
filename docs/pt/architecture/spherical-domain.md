---
title: "Spherical Domain"
icon: material/source-branch
---
# Spherical Domain

O Spherical Domain captura a cena em um cubemap nativo e deriva dele as representações esféricas suportadas.

A captura requer as seis direções do espaço; por isso `sceneRender()` pode ser chamado várias vezes no mesmo Processing frame. Estado mutável deve avançar em `update()` quando todas as faces precisam observar o mesmo estado.

Domemaster, Equirectangular e Skybox são projeções irmãs derivadas do cubemap, não uma cadeia serial obrigatória. Pitch/Yaw/Roll pertence à orientação esférica compartilhada; Domemaster acrescenta FOV e Size%.