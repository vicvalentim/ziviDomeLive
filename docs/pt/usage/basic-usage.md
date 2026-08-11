# Uso Básico

## Modo FULL

`FULL` é o modo padrão de compatibilidade:

```java
dome.setRenderMode(RenderMode.FULL);
dome.setCurrentView(zividomelive.ViewType.STANDARD);
dome.getOutputManager().setNdiView(
    zividomelive.ViewType.EQUIRECTANGULAR);
```

A janela pode mostrar Standard enquanto NDI publica equiretangular e o backend local publica domemaster.

## Modo Dedicado

```java
dome.setRenderMode(RenderMode.DOMEMASTER);
```

Um modo dedicado substitui a representação efetiva do preview e dos outputs. As rotas `ViewType` continuam armazenadas e retornam quando `FULL` é selecionado.

## Domemaster Flutuante

```java
dome.setRenderMode(RenderMode.STANDARD);
dome.setShowPreview(true);
```

Essa combinação renderiza intencionalmente Standard e a cadeia esférica exigida pelo preview fisheye auxiliar.

## Resolução de Output

```java
dome.resetGraphics(2048);
```

Os presets válidos na interface são 1024, 2048, 3072 e 4096. A realocação é adiada para o draw loop e afeta apenas targets de output.

## Parâmetros Esféricos

```java
dome.setFov(210);
dome.setFishSize(100);
dome.setPitch(0);
dome.setYaw(0);
dome.setRoll(0);
```

Esses parâmetros são compartilhados por domemaster, equiretangular e cubemap. Não os use como substitutos de uma câmera em scene space.
