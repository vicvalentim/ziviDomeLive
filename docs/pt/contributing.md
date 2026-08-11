# Contribuindo

## Verificações Locais

Use Java 17 e execute:

```bash
./gradlew clean test build
./gradlew buildReleaseArtifacts
mkdocs build --strict
```

## Contratos do Projeto

- Mantenha a ordem de `ViewType` inalterada.
- Não chame `beginDraw()` nem `endDraw()` dentro de uma `Scene`.
- Preserve o reset adiado da resolução de output.
- Use `LogManager` para logging da biblioteca.
- Use `ThreadManager` para tarefas compartilhadas em background.
- Mantenha Syphon/Spout no caminho `PGraphicsOpenGL`.
- Não adicione backends experimentais da 2.0 à linha 1.x.

Mudanças de GPU ou output exigem o protocolo visual CalibrationTool e evidência no hardware da plataforma, além dos testes unitários.
