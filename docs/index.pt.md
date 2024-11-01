
# ziviDomeLive

**ziviDomeLive** é uma biblioteca versátil para Processing, projetada para criar experiências visuais imersivas em projeções de cúpula, ambientes de RV monoscópica e instalações interativas. Ela fornece uma estrutura flexível para gerenciar cenas, realizar renderização 3D e integrar controladores externos ou tecnologias de projeção como **Syphon** e **Spout**. Com capacidades de renderização em tempo real, **ziviDomeLive** é ideal para exibições de planetário, performances audiovisuais ao vivo e instalações interativas. Esta documentação guiará você pela instalação, principais recursos, referências de API, exemplos de uso e configurações avançadas para aproveitar ao máximo a biblioteca.

---

## Recursos Principais

- **Múltiplos Modos de Projeção**:  
  Suporta uma ampla gama de formatos de projeção, incluindo **fisheye domemaster**, **equiretangular**, **cubemap** e mais. Esses modos de projeção permitem criar visuais que engajam o espectador, adaptando-se dinamicamente a exibições imersivas, sejam em configurações de RV ou cúpulas.

- **Troca de Resolução para Domemaster**:  
  Permite alternância suave entre resoluções **1k, 2k, 3k** e **4k** no modo de projeção domemaster, garantindo visuais nítidos em vários tamanhos de cúpula ou sistemas de exibição. Esse recurso permite otimizar o desempenho com base no seu hardware e nos requisitos do projeto.

- **Gerenciamento de Cenas**:  
  Organize e alterne dinamicamente entre diferentes cenas visuais com a interface **Scene**, permitindo composições modulares. Cada cena pode ter sua própria configuração, lógica de renderização e interações, tornando-o versátil para instalações interativas e apresentações ao vivo.

- **Renderização em Tempo Real**:  
  Projetado para visuais ao vivo, **ziviDomeLive** é otimizado para uma renderização suave quadro a quadro, mesmo com cenas 3D complexas e efeitos de shader. Esse recurso faz dele uma excelente escolha para VJs, live coding e instalações de arte interativa.

- **Integração com Aplicações Externas**:  
  Integra-se perfeitamente com outras aplicações via **Syphon** (macOS) e **Spout** (Windows), permitindo o compartilhamento de quadros renderizados em tempo real do Processing. Esse recurso é especialmente valioso para performances multimídia, permitindo que os visuais sejam processados ou projetados com outras ferramentas.

- **Interface de Usuário Interativa**:  
  Integra-se ao **ControlP5** para criar controles interativos diretamente no Processing, como sliders, botões e interruptores, permitindo a manipulação em tempo real de parâmetros visuais.

- **Compatibilidade Multiplataforma**:  
  Funciona em **macOS, Windows** e **Linux**, garantindo acessibilidade e versatilidade entre sistemas operacionais. Assim, suas criações visuais podem ser facilmente implementadas em várias plataformas sem problemas de compatibilidade.

- **Pipelines de Renderização Personalizáveis**:  
  Personalize pipelines de renderização de acordo com as necessidades do seu projeto, seja em projeções de cúpula ou ambientes interativos. A biblioteca permite ajustes na resolução de renderização, modos de projeção e outros parâmetros para otimizar o desempenho e a qualidade visual.

---

## Começando com o ziviDomeLive

Para começar a explorar as capacidades do **ziviDomeLive**, consulte o **[Guia Rápido](getting-started/quickstart.md)**, que apresenta as etapas de configuração, instalação e funcionalidade essencial para criar visuais dinâmicos e interativos no Processing. Para detalhes mais específicos de configuração e uma lista de dependências, consulte o **[Guia de Instalação](installation/installation-steps.md)**.

**ziviDomeLive** é um conjunto de ferramentas flexível que incentiva a experimentação, fornecendo tudo o que você precisa para transformar seus sketches no Processing em experiências interativas e imersivas. Mergulhe, explore seus recursos e crie displays visuais em tempo real que cativam e envolvem seu público de maneiras inovadoras.
