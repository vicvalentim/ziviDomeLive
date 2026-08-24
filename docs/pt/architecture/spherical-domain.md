---
title: "Spherical Domain"
icon: material/source-branch
---
# Spherical Domain

O Spherical Domain captura a cena em um cubemap nativo e deriva dele as representações esféricas suportadas.

A captura requer as seis direções do espaço; por isso `sceneRender()` pode ser chamado várias vezes no mesmo Processing frame. Estado mutável deve avançar em `update()` quando todas as faces precisam observar o mesmo estado.

Domemaster, Equirectangular e Skybox são projeções irmãs derivadas do cubemap, não uma cadeia serial obrigatória. Pitch/Yaw/Roll pertence à orientação esférica compartilhada; Domemaster acrescenta FOV e Size%. Skybox preserva os slots qualificados da cruz, mapeando cada face com coordenadas tangentes equiangulares (EAC) reais.

As seis faces compartilham um timestamp monotônico de captura em nanossegundos. Uma barreira de
publicação permanece fechada enquanto o target OpenGL reutilizável as renderiza sequencialmente e
só abre após todas as faces e mipmaps terminarem. É um lote lógico atômico, não um sleep nem a
afirmação de que um único contexto OpenGL rasteriza literalmente seis câmeras ao mesmo tempo.

Cada face do cubemap começa transparente, e seu alpha é amostrado sem substituição por
Equirectangular e Skybox. O Domemaster multiplica o alpha amostrado somente pela cobertura da
circunferência baseada em derivadas: pixels externos permanecem transparentes, Size% menor expõe
área transparente e `Size%=0` produz um target final totalmente transparente. Input ausente ou
falha de projeção limpa o destino em vez de conservar um frame anterior.
