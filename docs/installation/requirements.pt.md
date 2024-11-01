# Requisitos do Sistema para ziviDomeLive

A biblioteca **ziviDomeLive** foi projetada para suportar ambientes visuais imersivos de alto desempenho, incluindo projeções fulldome, instalações de realidade virtual e displays de mídia interativa. Dada sua capacidade de renderização 3D em tempo real, o **ziviDomeLive** exige algumas especificações de sistema e dependências para garantir desempenho ideal. Abaixo estão os requisitos detalhados de sistema, hardware e software.

---

## Ambiente Processing

Para aproveitar ao máximo as capacidades do **ziviDomeLive**, use uma versão compatível do Processing com as configurações apropriadas de ambiente. Essas configurações permitem maximizar o desempenho gráfico e a compatibilidade com o sistema:

- **Versão do Processing**: Processing 4.x ou superior. A biblioteca é otimizada para as versões mais recentes do Processing, que incorporam atualizações avançadas de renderização 3D e melhor compatibilidade com hardware atual. Versões mais antigas podem não suportar totalmente algumas funcionalidades.

- **Renderizador Gráfico**: Defina seus sketches do Processing para usar o `P3D` para renderização 3D. O renderizador `P3D` é essencial para lidar com cenas complexas em 3D, especialmente aquelas envolvendo projeções imersivas fisheye ou equiretangulares necessárias em ambientes de cúpula. Sem o `P3D`, funcionalidades 3D podem ser limitadas ou indisponíveis.

- **Sistemas Operacionais Suportados**:
    - **macOS** (10.14 Mojave ou posterior)
    - **Windows** (Windows 10 ou posterior)
    - **Linux** (Ubuntu 18.04 LTS ou posterior, Debian-based recomendado)

  Manter o sistema operacional atualizado pode melhorar significativamente o desempenho e a compatibilidade com integrações externas, particularmente o **Syphon** no macOS e o **Spout** no Windows.

---

## Recomendação de Hardware

Para renderização em tempo real ideal, especialmente em alta resolução (até 4K) para cúpula completa ou VR, o **ziviDomeLive** exige um hardware robusto para garantir uma experiência fluida e responsiva. Abaixo estão as recomendações de hardware:

- **GPU Dedicada**: Use uma placa gráfica moderna dedicada (por exemplo, NVIDIA GeForce RTX ou AMD Radeon RX). Gráficos integrados podem ter dificuldade com projeções em alta resolução e cenas com muitos shaders.
- **Memória (RAM)**: 8GB é o mínimo, mas 16GB ou mais são recomendados para lidar com cenas maiores ou integrações simultâneas.
- **Processador Multi-core**: Um processador com múltiplos núcleos aprimora o desempenho em renderização em tempo real, especialmente ao lidar com cálculos visuais complexos e múltiplas bibliotecas.

---

## Notas para Usuários de Apple Silicon (M1, M2 e Posteriores)

Se estiver usando um sistema **macOS com Apple Silicon (série M)**, considere o seguinte:

- **Compatibilidade com Syphon**: O Syphon atualmente não suporta a versão ARM nativa do Processing no Apple Silicon. Portanto, se você precisar do Syphon, execute a **versão Intel do Processing** usando **Rosetta 2**. Isso permite reter a funcionalidade completa do Syphon.

- **Limitações da Versão ARM**: Embora a versão ARM (nativa para Apple Silicon) do Processing funcione para a maioria das funcionalidades padrão, ela pode não oferecer suporte total para certas integrações em tempo real. Se o Syphon não for essencial, a versão ARM é aceitável.

---

Seguindo esses requisitos e recomendações, você garantirá que o **ziviDomeLive** ofereça o melhor desempenho, entregando visuais de alta qualidade e interatividade contínua para seus projetos de mídia imersiva.
