package com.victorvalentim.zividomelive;

import controlP5.*;
import processing.core.*;
import processing.event.KeyEvent;
import java.util.regex.Pattern;

public class ControlManager {

    ControlP5 cp5;
    boolean numberboxActive = false;
    int baseResolution;
    zividomelive parent;
    CheckBox previewCheckbox;
    CheckBox outputCheckbox;
    DropdownList resolutionDropdown;
    DropdownList viewModeDropdown;
    Textlabel fpsLabel;
    PApplet p;  // Referência para PApplet

    // Construtor que recebe o PApplet e o objeto zividomelive
    ControlManager(PApplet p, zividomelive parent, int baseResolution) {
        this.p = p;  // Passando a instância de PApplet diretamente
        this.parent = parent;
        this.baseResolution = baseResolution;
        cp5 = new ControlP5(p);  // Inicializando ControlP5 com PApplet

        // Inicializa todos os elementos de controle
        initializeControls();
        addNumberboxesAndSliders();
        addButtonsAndCheckboxes();
        addDropdownLists();

        // Reseta os controles ao iniciar
        resetControls();
    }

    // Inicializa os controles básicos como o rótulo de FPS
    private void initializeControls() {
        int yOffset = 10;
        // Adiciona o rótulo de FPS no topo
        fpsLabel = cp5.addTextlabel("fpsLabel")
                .setPosition(10, yOffset)
                .setSize(200, 20)
                .setText("FPS: 0");
    }

    // Adiciona caixas de número e sliders para controlar parâmetros como pitch, yaw, roll, etc.
    private void addNumberboxesAndSliders() {
        int yOffset = 40;
        int controlSpacing = 30;

        // Adiciona controle de pitch
        addNumberbox("pitch", 10, yOffset, -PApplet.PI, PApplet.PI, parent.getPitch());
        addSlider("pitch", 10 + 60, yOffset, -PApplet.PI, PApplet.PI, parent.getPitch());

        // Adiciona controle de yaw
        addNumberbox("yaw", 10, yOffset + controlSpacing, -PApplet.PI, PApplet.PI, parent.getYaw());
        addSlider("yaw", 10 + 60, yOffset + controlSpacing, -PApplet.PI, PApplet.PI, parent.getYaw());

        // Adiciona controle de roll
        addNumberbox("roll", 10, yOffset + 2 * controlSpacing, -PApplet.PI, PApplet.PI, parent.getRoll());
        addSlider("roll", 10 + 60, yOffset + 2 * controlSpacing, -PApplet.PI, PApplet.PI, parent.getRoll());

        // Adiciona controle de field of view (fov)
        addNumberbox("fov", 10, yOffset + 3 * controlSpacing, 0, 360, parent.getFov());
        addSlider("fov", 10 + 60, yOffset + 3 * controlSpacing, 0, 360, parent.getFov());

        // Adiciona controle de tamanho do fisheye
        addNumberbox("size", 10, yOffset + 4 * controlSpacing, 0, 100, parent.getFishSize());
        addSlider("size", 10 + 60, yOffset + 4 * controlSpacing, 0, 100, parent.getFishSize());
    }

    // Adiciona botões e checkboxes para resetar os controles e controlar preview/output
    private void addButtonsAndCheckboxes() {
        int yOffset = 190;
        int controlSpacing = 30;

        // Adiciona botão para resetar os controles
        cp5.addButton("resetControls")
                .setPosition(10, yOffset)
                .setSize(200, 20)
                .setLabel("Resetar Controles")
                .onClick(event -> parent.resetControls());

        // Adiciona checkbox para o preview
        previewCheckbox = cp5.addCheckBox("previewCheckbox")
                .setPosition(10, yOffset + controlSpacing)
                .setSize(20, 20)
                .addItem("Visualizar Domemaster", 0)
                .setValue(parent.isShowPreview() ? 1 : 0);

        // Adiciona checkbox para habilitar o output
        outputCheckbox = cp5.addCheckBox("outputCheckbox")
                .setPosition(10, yOffset + 2 * controlSpacing)
                .setSize(20, 20)
                .addItem("Habilitar Saída", 0)
                .setValue(parent.isEnableOutput() ? 1 : 0);
    }

    // Adiciona listas dropdown para selecionar resolução e modo de visualização
    private void addDropdownLists() {
        int yOffset = 280;
        int controlSpacing = 30;

        // Adiciona dropdown para seleção de modo de visualização
        addViewModeDropdown(yOffset);
        // Adiciona dropdown para seleção de resolução
        addResolutionDropdown(yOffset + controlSpacing);
    }

    // Adiciona um Numberbox para o controle de valores
    private void addNumberbox(String name, float x, float y, float min, float max, float value) {
        Numberbox numberbox = cp5.addNumberbox(name + "Value")
                .setPosition(x, y)
                .setSize(50, 20)
                .setRange(min, max)
                .setScrollSensitivity(0.1f)
                .setValue(value)
                .onChange(event -> {
                    if (!numberboxActive) {
                        numberboxActive = true;
                        setParentValue(name, event.getController().getValue());
                        cp5.getController(name).setValue(event.getController().getValue());
                        numberboxActive = false;
                    }
                });
        numberbox.setLabelVisible(false);
        numberbox.getCaptionLabel().setVisible(false);
        makeEditable(numberbox);
    }

    // Adiciona um Slider para controle contínuo de valores
    private void addSlider(String name, float x, float y, float min, float max, float value) {
        cp5.addSlider(name)
                .setPosition(x, y)
                .setSize(140, 20)
                .setRange(min, max)
                .setValue(value)
                .onChange(event -> {
                    if (!numberboxActive) {
                        numberboxActive = true;
                        setParentValue(name, event.getController().getValue());
                        cp5.getController(name + "Value").setValue(event.getController().getValue());
                        numberboxActive = false;
                    }
                });
    }

    // Adiciona dropdown para seleção de resolução
    void addResolutionDropdown(float y) {
        resolutionDropdown = cp5.addDropdownList("Resolution")
                .setPosition(10, y)
                .setSize(200, 200)
                .setBarHeight(20)
                .setItemHeight(20)
                .close();

        // Adiciona opções de resolução ao dropdown
        String[] resolutionLabels = {"1024", "2048", "3072", "4096"};
        for (int i = 0; i < resolutionLabels.length; i++) {
            resolutionDropdown.addItem("Resolução " + (i + 1) + "k " + resolutionLabels[i], i);
        }

        // Define ação quando uma resolução for selecionada
        resolutionDropdown.onChange(event -> {
            int selectedIndex = (int) event.getController().getValue();
            int newResolution = 1024 * (selectedIndex + 1);
            parent.resetGraphics(newResolution);
        });

        resolutionDropdown.onClick(event -> resolutionDropdown.bringToFront());
    }

    // Adiciona dropdown para seleção do modo de visualização
    void addViewModeDropdown(float y) {
        viewModeDropdown = cp5.addDropdownList("View Mode")
                .setPosition(10, y)
                .setSize(200, 200)
                .setItemHeight(20)
                .setBarHeight(20)
                .close();

        // Adiciona opções de modo de visualização ao dropdown
        String[] viewModes = {"Fisheye Domemaster", "Equirectangular", "Cubemap Skybox", "Standard"};
        for (String viewMode : viewModes) {
            viewModeDropdown.addItem(viewMode, viewModeDropdown.getItems().size());
        }

        // Define ação quando um modo de visualização for selecionado
        viewModeDropdown.onChange(event -> {
            int selectedIndex = (int) event.getController().getValue();
            parent.setCurrentView(zividomelive.ViewType.values()[selectedIndex]);
        });

        viewModeDropdown.onClick(event -> viewModeDropdown.bringToFront());
    }

    // Reseta todos os controles para seus valores padrões
    void resetControls() {
        String[] controlNames = {"pitch", "yaw", "roll", "fov", "size"};
        float[] defaultValues = {0.0f, 0.0f, 0.0f, 210.0f, 100.0f};

        for (int i = 0; i < controlNames.length; i++) {
            cp5.getController(controlNames[i]).setValue(defaultValues[i]);
        }
    }

    // Exibe o painel de controle
    void show() {
        cp5.show();
    }

    // Esconde o painel de controle
    void hide() {
        cp5.hide();
    }

    // Torna um Numberbox editável ao lidar com entrada de teclas
    void makeEditable(Numberbox n) {
        final NumberboxInput nin = new NumberboxInput(n, p);  // Passando o PApplet diretamente
        n.onClick(theEvent -> {
            nin.setActive(true);
            numberboxActive = true;
        }).onLeave(theEvent -> {
            nin.setActive(false);
            numberboxActive = false;
            nin.submit();
        });
    }

    // Atualiza o valor do objeto pai baseado no nome do controle
    private void setParentValue(String name, float value) {
        switch (name) {
            case "pitch":
                parent.setPitch(value);
                break;
            case "yaw":
                parent.setYaw(value);
                break;
            case "roll":
                parent.setRoll(value);
                break;
            case "fov":
                parent.setFov(value);
                break;
            case "size":
                parent.setFishSize(value);
                parent.getFisheyeDomemaster().setSizePercentage(value);
                break;
        }
    }

    // Classe interna para tornar o Numberbox editável
    public class NumberboxInput {
        String text = "";
        Numberbox n;
        boolean active;
        PApplet p;  // Referência ao PApplet

        NumberboxInput(Numberbox theNumberbox, PApplet p) {
            n = theNumberbox;
            this.p = p;  // Passando o PApplet diretamente
            p.registerMethod("keyEvent", this);  // Usando PApplet para registrar eventos
        }

        public void keyEvent(KeyEvent k) {
            if (k.getAction() == KeyEvent.PRESS && active) {
                if (k.getKey() == '\n') {
                    submit();
                } else if (k.getKeyCode() == KeyEvent.ALT) {
                    text = text.isEmpty() ? "" : text.substring(0, text.length() - 1);
                } else if (k.getKey() == '-' && text.isEmpty()) {
                    text += k.getKey();
                } else if (k.getKey() < 255) {
                    final String regex = "-?\\d*(\\.\\d{0,2})?";
                    String s = text + k.getKey();
                    if (Pattern.matches(regex, s)) {
                        text += k.getKey();
                    }
                }
                n.getValueLabel().setText(this.text);
            }
        }

        public void setActive(boolean b) {
            active = b;
            if (active) {
                n.getValueLabel().setText("");
                text = "";
            }
        }

        public void submit() {
            if (!text.isEmpty()) {
                n.setValue(Float.parseFloat(text));
                setParentValue(n.getName().replace("Value", ""), n.getValue());
                cp5.getController(n.getName().replace("Value", "")).setValue(n.getValue());
                text = "";
            } else {
                n.getValueLabel().setText("" + n.getValue());
            }
        }
    }

    // Lida com eventos de controle
    public void handleEvent(ControlEvent theEvent) {
        if (theEvent.isFrom(previewCheckbox)) {
            parent.setShowPreview(!parent.isShowPreview());
        } else if (theEvent.isFrom(outputCheckbox)) {
            parent.setEnableOutput(!parent.isEnableOutput());
        }
    }

    // Retorna se a caixa de número está ativa
    public boolean isNumberboxActive() {
        return numberboxActive;
    }

    // Método para atualizar o rótulo de FPS
    public void updateFpsLabel(float frameRate) {
        fpsLabel.setText("FPS: " + PApplet.nf(frameRate, 0, 1));  // Atualiza o rótulo de FPS
    }
}
