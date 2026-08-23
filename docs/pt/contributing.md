# Contribuindo

## Verificações Locais

Use Java 17 e execute:

```bash
./gradlew clean qualificationTests
./gradlew build -x test
./gradlew buildReleaseArtifacts
python3 -m mkdocs build --strict
./gradlew attachJavadocsToSite --console=plain
python3 tools/validate_documentation.py --root . --site-dir site
```

Visualize o manual com `python3 -m mkdocs serve`. Isso evita executáveis MkDocs antigos do sistema que podem pertencer ao Python 2.

## Contratos do Projeto

- Mantenha a ordem de `ViewType` inalterada.
- Mantenha páginas em inglês e português pareadas e atualize a navegação de `mkdocs.yml` em conjunto.
- Não chame `beginDraw()` nem `endDraw()` dentro de uma `Scene`.
- Preserve o reset adiado da resolução de output.
- Use `LogManager` para logging da biblioteca.
- Use `SceneServices.tasks()` pertencente à ativação para trabalho da cena em background; não exponha nem crie outro executor.
- Mantenha Syphon/Spout no caminho `PGraphicsOpenGL`.
- Não reintroduza o caminho removido de captura esférica `PGraphicsOpenGL[]`.

Mudanças de GPU ou output exigem o protocolo visual [CalibrationTool](qualification/calibration-tool.md) e evidência no hardware da plataforma, além dos testes unitários.

`qualificationTests` é a execução automatizada canônica. O resumo, o relatório
HTML e os resultados JUnit XML ficam em `build/reports/qualification/` e
`build/test-results/qualification/`. Para investigar uma classe, use
`./gradlew qualificationTests --tests '*OrbitCameraTest'`, mas a aceitação de
release exige a suíte completa sem filtros. Os fontes de teste permanecem no
Git e são excluídos dos pacotes Processing e do deploy no sketchbook.

O GitHub também executa essa tarefa no workflow independente
`Automated Qualification` em todos os pushes, pull requests destinados à
`main` e execuções manuais. O resumo do job mostra os totais e o artefato
disponível para download preserva as evidências detalhadas por 30 dias.

## Escopo Das Mudanças

Alterações de comportamento público exigem Javadocs, testes unitários focados,
documentação de usuário bilíngue e uma entrada no changelog. Mantenha políticas
puras de rota, orientação, dimensão e lifecycle isoladas do OpenGL sempre que
possível para testá-las no fork headless de qualificação.

Não faça commit de `build/`, `site/` ou `release/`. Os artefatos são produzidos
pelo Gradle e publicados a partir das tags de versão.
