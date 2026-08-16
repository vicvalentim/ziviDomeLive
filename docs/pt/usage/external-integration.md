# Outputs Externos

Outputs externos são opcionais. Comece escolhendo **qual representação** cada destino deve receber e habilite somente o backend necessário à instalação.

![Outputs externos](../../img/external-outputs.png)

## NDI

**O que é?** Saída de vídeo em rede.  
**Plataforma:** disponibilidade depende de runtime nativo Devolay/NDI compatível e qualificação com receiver.  
**Selecionar view:** `setNdiView(ViewType...)`.  
**Habilitar/desabilitar:** `toggleOutput("ndi")`.

```java
OutputManager outputs = dome.getOutputManager();
outputs.setNdiView(ViewType.EQUIRECTANGULAR);
outputs.toggleOutput("ndi");

println(outputs.getOutputState(OutputManager.OutputType.NDI));
println(outputs.getOutputFailureReason(OutputManager.OutputType.NDI));
```

Teste com receiver NDI real antes de declarar uma plataforma qualificada.

## Syphon

**O que é?** Compartilhamento local de textura GPU no macOS.  
**Selecionar view:** `setSyphonView(ViewType...)`.  
**Habilitar/desabilitar:** `toggleOutput("syphon")`.

Disponibilidade não equivale a inicialização bem-sucedida nem a receiver qualificado.

## Spout

**O que é?** Compartilhamento local de textura GPU no Windows.  
**Selecionar view:** `setSpoutView(ViewType...)`.  
**Habilitar/desabilitar:** `toggleOutput("spout")`.

Teste com receiver real na configuração Windows que será declarada como qualificada.

## Estado e troubleshooting

Use `getOutputState(...)` e `getOutputFailureReason(...)` para distinguir indisponibilidade, inicialização/habilitação e falhas. Não deduza a saúde do backend apenas pelo estado visual de um toggle.

Fronteiras GPU/CPU, workers, buffers e latest-frame-wins pertencem ao Guia do Desenvolvedor, não a esta página de primeiro uso.
