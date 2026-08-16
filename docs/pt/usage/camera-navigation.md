# Câmera e Navegação

Scene Camera e orientação esférica são camadas distintas.

```text
Scene Camera                 Pitch / Yaw / Roll
move/transforma a cena    ≠   orienta a representação esférica
```

Use câmera/navegação quando a intenção criativa for mover o observador, alvo ou enquadramento em espaço de cena. Use Pitch/Yaw/Roll quando a instalação precisar reorientar a representação esférica em relação ao domo/output.

O serviço público de câmera é destinado à navegação em espaço de cena. Uma cena pode usar `getSceneCamera()`/serviços de câmera conforme a API e os exemplos atuais.

Fundos de Environment permanecem centrados no observador e invariantes à translação: mover a câmera não deve fazer um fundo infinito parecer mais próximo ou mais distante. A orientação é composta pelo pipeline implementado de câmera/esfera, não tratando o Environment como geometria comum da cena.
