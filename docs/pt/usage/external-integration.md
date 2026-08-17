---
title: Outputs Externos
icon: material/video-wireless-outline
---

# Outputs Externos

Outputs externos são opcionais. Primeiro escolha **qual representação** o destino deve receber; depois habilite apenas o backend necessário à instalação e verifique seu estado real no receiver.

<figure markdown="span">
  ![Rotas de output externo](../../img/external-outputs.png)
  <figcaption>ViewType seleciona a representação; o backend define como essa representação sai da aplicação.</figcaption>
</figure>

=== "NDI"

    **O que é?** Output de vídeo em rede.  
    **Plataforma:** a disponibilidade depende de runtime nativo Devolay/NDI compatível e qualificação com receiver.  
    **Selecione uma view:** `setNdiView(ViewType...)`.  
    **Habilite/desabilite:** `toggleOutput("ndi")`.

    ```java
    OutputManager outputs = dome.getOutputManager();
    outputs.setNdiView(ViewType.EQUIRECTANGULAR);
    outputs.toggleOutput("ndi");

    println(outputs.getOutputState(OutputManager.OutputType.NDI));
    println(outputs.getOutputFailureReason(OutputManager.OutputType.NDI));
    ```

    !!! tip "Qualificação"
        Teste com um receiver NDI real antes de marcar uma plataforma como qualificada.

=== "Syphon"

    **O que é?** Compartilhamento local de textura GPU no macOS.  
    **Selecione uma view:** `setSyphonView(ViewType...)`.  
    **Habilite/desabilite:** `toggleOutput("syphon")`.

    Disponibilidade não equivale a inicialização bem-sucedida nem a qualificação com receiver.

=== "Spout"

    **O que é?** Compartilhamento local de textura GPU no Windows.  
    **Selecione uma view:** `setSpoutView(ViewType...)`.  
    **Habilite/desabilite:** `toggleOutput("spout")`.

    Teste com um receiver real na configuração Windows que será declarada como qualificada.

## Estado e troubleshooting

Use `getOutputState(...)` e `getOutputFailureReason(...)` para distinguir estados indisponíveis, inicializados/habilitados e falhos. Não deduza a saúde do output apenas pelo toggle da UI.

??? abstract "Por dentro"
    Fronteiras GPU/CPU, filas de worker, buffers, comportamento latest-frame-wins e detalhes de compartilhamento nativo estão em [Backends de Output](../architecture/output-backends.md). Eles não são pré-requisitos para habilitar um output.
