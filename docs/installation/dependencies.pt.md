# Dependências do ziviDomeLive

A biblioteca **ziviDomeLive** é projetada para suportar aplicações visuais interativas e de alto desempenho em ambientes imersivos. Para utilizar plenamente as capacidades da biblioteca, **ziviDomeLive** requer algumas bibliotecas adicionais no Processing. Essas dependências garantem compatibilidade e estabilidade no macOS, Windows e Linux, embora algumas integrações externas, como **Syphon** e **Spout**, funcionem apenas em sistemas operacionais específicos.

Recomenda-se a instalação de todas as dependências em todos os sistemas, pois **ziviDomeLive** é arquitetado com verificações de compatibilidade e mecanismos de fallback que dependem da presença dessas bibliotecas.

---

## Lista de Dependências Necessárias

### 1. ControlP5

- **Propósito**: A biblioteca **ControlP5** é essencial para criar interfaces gráficas de usuário (GUI) personalizadas nos sketches do Processing. Ela fornece uma variedade de componentes interativos, como sliders, botões, toggles e knobs, que permitem aos usuários ajustar parâmetros visuais dinamicamente. Essa biblioteca aprimora o **ziviDomeLive** ao permitir o controle em tempo real de aspectos do ambiente imersivo diretamente no sketch do Processing.

- **Instruções de Instalação**:
    - **Opção 1**: Instale através do Gerenciador de Contribuições do Processing:
        1. Abra o Processing.
        2. Vá para **Sketch > Import Library > Add Library...**
        3. No Gerenciador de Contribuições, pesquise por **ControlP5** e clique em **Install** para adicioná-lo ao seu ambiente Processing.

    - **Opção 2**: Baixe diretamente do [repositório ControlP5 no GitHub](https://github.com/sojamo/controlp5){:target="_blank"}:
        1. Visite a página do [ControlP5 no GitHub](https://github.com/sojamo/controlp5){:target="_blank"}.
        2. Clique em **Code** e selecione **Download ZIP**.
        3. Extraia o arquivo baixado e coloque a pasta `ControlP5` no diretório `libraries` do Processing (normalmente localizado em `Documentos/Processing/libraries/`).

- **Uso**: Após a instalação, o ControlP5 pode ser importado nos sketches do Processing para adicionar controles de interface. Ele desempenha um papel fundamental ao facilitar ajustes de parâmetros em tempo real em projetos com **ziviDomeLive**.

---

### 2. Syphon (Apenas para macOS)

- **Propósito**: **Syphon** é uma biblioteca exclusiva para macOS que permite o compartilhamento de quadros em tempo real entre o Processing e outros aplicativos. Esse recurso é particularmente útil para artistas e desenvolvedores multimídia que trabalham em ambientes onde os visuais precisam ser direcionados para vários softwares para manipulação ou exibição adicional. Embora Syphon seja específico do macOS, sua presença assegura que o **ziviDomeLive** funcione corretamente no macOS, mesmo que nem toda a funcionalidade de Syphon seja usada.

- **Instruções de Instalação**:
    1. Visite o [repositório Syphon for Processing no GitHub](https://github.com/Syphon/Processing){:target="_blank"}.
    2. Clique em **Code** e selecione **Download ZIP**.
    3. Extraia o arquivo ZIP e mova a pasta `Syphon` para o diretório `libraries` do Processing (`Documentos/Processing/libraries/`).

- **Nota**: Syphon é compatível apenas com macOS. No entanto, recomenda-se incluir essa biblioteca na configuração do Processing para fins de compatibilidade, mesmo que o seu projeto atual não utilize os recursos de compartilhamento de quadros do Syphon.

---

### 3. Spout (Apenas para Windows)

- **Propósito**: **Spout** fornece uma funcionalidade de compartilhamento de quadros semelhante ao Syphon, mas é projetado especificamente para Windows. O Spout permite aos usuários compartilhar quadros em tempo real entre o Processing e outros aplicativos compatíveis com Spout no Windows. Esta biblioteca é amplamente utilizada em arte multimídia e performance para transferir visuais de forma contínua entre diferentes softwares. Ter o Spout instalado é benéfico mesmo que o seu projeto atual não utilize seus recursos, pois ajuda a manter a compatibilidade e flexibilidade na arquitetura do **ziviDomeLive**.

- **Instruções de Instalação**:
    1. Acesse o [repositório Spout for Processing no GitHub](https://github.com/leadedge/SpoutProcessing){:target="_blank"}.
    2. Clique em **Code** e selecione **Download ZIP**.
    3. Extraia o arquivo e coloque a pasta `Spout` no diretório `libraries` do Processing (`Documentos/Processing/libraries/`).

- **Nota**: Spout está disponível apenas para Windows. Incluir esta biblioteca na sua configuração do Processing ainda pode ser útil para compatibilidade e garante que o ambiente esteja totalmente preparado para qualquer projeto **ziviDomeLive** que possa incorporar o compartilhamento de quadros no futuro.

---

## Verificando a Instalação das Dependências

Após instalar essas dependências, você pode verificar a instalação abrindo o Processing e navegando até **Sketch > Import Library**. Verifique se **ControlP5**, **Syphon** (para macOS) e **Spout** (para Windows) aparecem na lista. Esta confirmação garante que as bibliotecas estão disponíveis e prontas para uso com o **ziviDomeLive**.

A instalação de todas as dependências, mesmo aquelas específicas para determinados sistemas operacionais, maximiza a compatibilidade e prepara o ambiente Processing para um desempenho contínuo com o **ziviDomeLive**.
