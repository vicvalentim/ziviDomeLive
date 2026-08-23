# Contribuindo

!!! tip "Contribuindo com o desenvolvimento"
    Contribuições para o desenvolvimento da ziviDomeLive são bem-vindas. Correções de bugs, features bem delimitadas, testes, exemplos, melhorias de acessibilidade, traduções e documentação são importantes e muito apreciadas.

A participação segue o [Código de Conduta](https://github.com/vicvalentim/ziviDomeLive/blob/main/CODE_OF_CONDUCT.md) e a [declaração de integridade científica e revisão humana](research-integrity.md) do projeto. Trabalho assistido por IA deve identificar ferramenta e finalidade; a pessoa que contribui precisa compreender, testar, revisar e assumir responsabilidade por cada mudança submetida.

## Etapas para contribuir

1. **Faça um fork do repositório.** Abra o [repositório da ziviDomeLive](https://github.com/vicvalentim/ziviDomeLive) e selecione **Fork** para criar uma cópia em sua conta GitHub.
2. **Clone seu fork.** Substitua `SEU-USUARIO` pela sua conta GitHub:

    ```bash
    git clone https://github.com/SEU-USUARIO/ziviDomeLive.git
    cd ziviDomeLive
    ```

3. **Crie uma branch focada.** Use um nome curto que identifique o trabalho:

    ```bash
    git checkout -b nome-da-sua-branch
    ```

4. **Implemente e teste a mudança.** Mantenha o escopo coerente, siga os contratos abaixo e adicione ou atualize testes e documentação bilíngue quando houver mudança de comportamento público.
5. **Faça commit e envie a branch ao seu fork.** Escreva uma mensagem de commit clara e publique a branch:

    ```bash
    git add <arquivos-alterados>
    git commit -m "Descreva a contribuição"
    git push origin nome-da-sua-branch
    ```

6. **Abra um pull request.** No seu fork, abra um PR destinado ao repositório original. Explique o problema, a solução escolhida, mudanças intencionais de API ou comportamento, validações executadas e eventuais verificações de hardware ou visuais ainda pendentes. Vincule a issue relacionada quando existir.

Obrigado por ajudar a manter a ziviDomeLive útil, ensinável e sustentável.

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

Contribuições de pesquisa, documentação e código devem citar suas fontes e
creditar colaboradores conforme a contribuição efetiva. Não submeta material
privado, confidencial ou inédito de terceiros a serviços de IA generativa.

Não faça commit de `build/`, `site/` ou `release/`. Os artefatos são produzidos
pelo Gradle e publicados a partir das tags de versão.
