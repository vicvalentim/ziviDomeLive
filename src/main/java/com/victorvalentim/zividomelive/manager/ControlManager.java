package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.zividomelive;
import controlP5.*;
import processing.core.*;
import processing.event.KeyEvent;
import java.util.regex.Pattern;
import java.util.function.Consumer;

/**
 * The ControlManager class manages the user interface controls for the application.
 * It uses ControlP5 for creating and handling UI elements such as sliders, buttons, and dropdown lists.
 * This class also manages the toggling and view selection for output methods (NDI, Spout, Syphon).
 */
public class ControlManager {

    ControlP5 cp5;
    boolean numberboxActive = false;
    int baseResolution;
    zividomelive parent;
    Toggle previewToggle;
    Toggle ndiToggle;
    Toggle spoutToggle;
    Toggle syphonToggle;
    DropdownList resolutionDropdown;
    DropdownList viewModeDropdown;
    DropdownList ndiViewDropdown;
    DropdownList spoutViewDropdown;
    DropdownList syphonViewDropdown;
    Textlabel fpsLabel;
    PApplet p;

    // Layout configuration
    // Layout configuration
    private final int controlSpacing = 35;
    private final int controlHeight = 20;
    private final int initialYOffset = 20;
    private int currentYOffset;

    /**
     * Constructs a ControlManager with the specified PApplet, parent object, and base resolution.
     *
     * @param p the PApplet instance
     * @param parent the parent zividomelive instance
     * @param baseResolution the base resolution for the application
     */
    public ControlManager(PApplet p, zividomelive parent, int baseResolution) {
        this.p = p;
        this.parent = parent;
        this.baseResolution = baseResolution;
        cp5 = new ControlP5(p);
        this.currentYOffset = initialYOffset;

        // Initialize and increment Y offset
        initializeControls(currentYOffset);
        currentYOffset += controlSpacing;

        // Add number boxes and sliders
        addNumberboxesAndSliders(currentYOffset);
        currentYOffset += 5 * controlSpacing;

        // Add buttons
        addButtons(currentYOffset);
        currentYOffset += 2 * controlSpacing;

        // Add dropdown lists
        addDropdownLists(currentYOffset);
        currentYOffset += 2 * controlSpacing;

        // Add toggles and output view dropdowns
        addOutputToggles(currentYOffset);
        currentYOffset += 2 * controlSpacing;
        addOutputViewDropdowns(currentYOffset);

        // Reset controls to default state
        resetControls();
    }

    /**
     * Initializes the FPS label control.
     * @param yOffset the initial vertical offset for placing the FPS label.
     */
    private void initializeControls(int yOffset) {
        fpsLabel = cp5.addTextlabel("fpsLabel")
                .setPosition(10, yOffset)
                .setSize(200, controlHeight)
                .setText("FPS: 0");
    }

    /**
     * Adds number boxes and sliders for controlling parameters like pitch, yaw, roll, fov, and size.
     * @param yOffset the initial vertical offset for placing controls.
     */
    private void addNumberboxesAndSliders(int yOffset) {
        addNumberbox("pitch", yOffset, -PApplet.PI, PApplet.PI, parent.getPitch());
        addSlider("pitch", yOffset, -PApplet.PI, PApplet.PI, parent.getPitch());

        yOffset += controlSpacing;
        addNumberbox("yaw", yOffset, -PApplet.PI, PApplet.PI, parent.getYaw());
        addSlider("yaw", yOffset, -PApplet.PI, PApplet.PI, parent.getYaw());

        yOffset += controlSpacing;
        addNumberbox("roll", yOffset, -PApplet.PI, PApplet.PI, parent.getRoll());
        addSlider("roll", yOffset, -PApplet.PI, PApplet.PI, parent.getRoll());

        yOffset += controlSpacing;
        addNumberbox("fov", yOffset, 0, 360, parent.getFov());
        addSlider("fov", yOffset, 0, 360, parent.getFov());

        yOffset += controlSpacing;
        addNumberbox("size", yOffset, 0, 100, parent.getFishSize());
        addSlider("size", yOffset, 0, 100, parent.getFishSize());
    }

    /**
     * Adds buttons for resetting controls and controlling preview mode.
     * @param yOffset the vertical offset for placing buttons.
     */
    private void addButtons(int yOffset) {
        cp5.addButton("resetControls")
                .setPosition(10, yOffset)
                .setSize(200, controlHeight)
                .setLabel("Reset Controls")
                .getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER)
                .setPaddingX(5);
        cp5.getController("resetControls").onClick(event -> parent.resetControls());

        yOffset += controlSpacing;

        previewToggle = cp5.addToggle("previewToggle")
                .setPosition(10, yOffset)
                .setSize(20, controlHeight)
                .setValue(parent.isShowPreview());
        previewToggle.getCaptionLabel()
                .align(ControlP5.RIGHT_OUTSIDE, ControlP5.CENTER)
                .setPaddingX(5)
                .setText("Preview Domemaster");
        previewToggle.onChange(event -> parent.setShowPreview(previewToggle.getState()));
    }

    /**
     * Adds dropdown lists for selecting resolution and view mode.
     * @param yOffset the vertical offset for placing dropdowns.
     */
    private void addDropdownLists(int yOffset) {
        addViewModeDropdown(yOffset);
        addResolutionDropdown(yOffset + controlSpacing);
    }

    /**
     * Adds toggles for enabling/disabling output methods: NDI, Spout, and Syphon.
     * Each toggle controls the visibility of a corresponding view mode dropdown list.
     * @param yOffset the vertical offset for placing output toggles.
     */
    private void addOutputToggles(int yOffset) {
        // NDI Toggle
        ndiToggle = cp5.addToggle("ndiToggle")
                .setPosition(10, yOffset)
                .setSize(20, controlHeight)
                .setValue(parent.getOutputManager().isNdiEnabled());
        ndiToggle.getCaptionLabel()
                .align(ControlP5.RIGHT_OUTSIDE, ControlP5.CENTER)
                .setPaddingX(5)
                .setText("Enable NDI");
        ndiToggle.onChange(event -> {
            parent.getOutputManager().toggleOutput("ndi");
            toggleDropdownVisibility();
        });

        yOffset += controlSpacing;

        // Spout Toggle (Windows only)
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            spoutToggle = cp5.addToggle("spoutToggle")
                    .setPosition(10, yOffset)
                    .setSize(20, controlHeight)
                    .setValue(parent.getOutputManager().isSpoutEnabled());
            spoutToggle.getCaptionLabel()
                    .align(ControlP5.RIGHT_OUTSIDE, ControlP5.CENTER)
                    .setPaddingX(5)
                    .setText("Enable Spout");
            spoutToggle.onChange(event -> {
                parent.getOutputManager().toggleOutput("spout");
                toggleDropdownVisibility();
            });
            yOffset += controlSpacing;
        }

        // Syphon Toggle (macOS only)
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            syphonToggle = cp5.addToggle("syphonToggle")
                    .setPosition(10, yOffset)
                    .setSize(20, controlHeight)
                    .setValue(parent.getOutputManager().isSyphonEnabled());
            syphonToggle.getCaptionLabel()
                    .align(ControlP5.RIGHT_OUTSIDE, ControlP5.CENTER)
                    .setPaddingX(5)
                    .setText("Enable Syphon");
            syphonToggle.onChange(event -> {
                parent.getOutputManager().toggleOutput("syphon");
                toggleDropdownVisibility();
            });
        }
    }

    /**
     * Adds dropdowns for selecting the view mode to be used with each output method.
     * These are only visible when the corresponding toggle is enabled.
     * @param yOffset the vertical offset for placing view mode dropdowns.
     */
    private void addOutputViewDropdowns(int yOffset) {
        String[] viewModes = {"Fisheye Domemaster", "Equirectangular", "Cubemap Skybox", "Standard"};

        // Adiciona os dropdowns, cada um com o espaçamento padrão
        ndiViewDropdown = createViewDropdown("NDI View", yOffset, viewModes, view -> parent.getOutputManager().setNdiView(view));
        yOffset += controlSpacing;

        spoutViewDropdown = createViewDropdown("Spout View", yOffset, viewModes, view -> parent.getOutputManager().setSpoutView(view));
        syphonViewDropdown = createViewDropdown("Syphon View", yOffset, viewModes, view -> parent.getOutputManager().setSyphonView(view));

        toggleDropdownVisibility();
    }

    /**
     * Helper method to create a view mode dropdown for an output toggle.
     * @param label the label for the dropdown.
     * @param yOffset the vertical position of the dropdown.
     * @param viewModes the available view modes to select from.
     * @param setView the consumer function to set the view type in OutputManager.
     * @return the created DropdownList.
     */
    private DropdownList createViewDropdown(String label, int yOffset, String[] viewModes, Consumer<zividomelive.ViewType> setView) {
        DropdownList dropdown = cp5.addDropdownList(label)
                .setPosition(10, yOffset)
                .setSize(200, 200)
                .setBarHeight(controlHeight)
                .setItemHeight(controlHeight)
                .setVisible(false) // Inicialmente oculto
                .close();
        for (String viewMode : viewModes) {
            dropdown.addItem(viewMode, dropdown.getItems().size());
        }
        dropdown.onChange(event -> {
            int selectedIndex = (int) event.getController().getValue();
            setView.accept(zividomelive.ViewType.values()[selectedIndex]);
        });
        dropdown.onClick(event -> dropdown.bringToFront());
        return dropdown;
    }

    /**
     * Toggles the visibility of the view mode dropdown lists for each output based on the state of the toggles.
     */
    private void toggleDropdownVisibility() {
        ndiViewDropdown.setVisible(ndiToggle.getState());
        if (spoutToggle != null) spoutViewDropdown.setVisible(spoutToggle.getState());
        if (syphonToggle != null) syphonViewDropdown.setVisible(syphonToggle.getState());
    }

    private void addNumberbox(String name, float y, float min, float max, float value) {
        Numberbox numberbox = cp5.addNumberbox(name + "Value")
                .setPosition(10, y)
                .setSize(50, controlHeight)
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

    private void addSlider(String name, float y, float min, float max, float value) {
        cp5.addSlider(name)
                .setPosition(70, y)
                .setSize(140, controlHeight)
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

    void addResolutionDropdown(float y) {
        resolutionDropdown = cp5.addDropdownList("Resolution")
                .setPosition(10, y)
                .setSize(200, 200)
                .setBarHeight(controlHeight)
                .setItemHeight(controlHeight)
                .close();
        String[] resolutionLabels = {"1024", "2048", "3072", "4096"};
        for (int i = 0; i < resolutionLabels.length; i++) {
            resolutionDropdown.addItem("Resolution " + (i + 1) + "k " + resolutionLabels[i], i);
        }
        resolutionDropdown.onChange(event -> {
            int selectedIndex = (int) event.getController().getValue();
            int newResolution = 1024 * (selectedIndex + 1);
            parent.resetGraphics(newResolution);
        });
        resolutionDropdown.onClick(event -> resolutionDropdown.bringToFront());
    }

    void addViewModeDropdown(float y) {
        viewModeDropdown = cp5.addDropdownList("View Mode")
                .setPosition(10, y)
                .setSize(200, 200)
                .setItemHeight(controlHeight)
                .setBarHeight(controlHeight)
                .close();
        String[] viewModes = {"Fisheye Domemaster", "Equirectangular", "Cubemap Skybox", "Standard"};
        for (String viewMode : viewModes) {
            viewModeDropdown.addItem(viewMode, viewModeDropdown.getItems().size());
        }
        viewModeDropdown.onChange(event -> {
            int selectedIndex = (int) event.getController().getValue();
            parent.setCurrentView(zividomelive.ViewType.values()[selectedIndex]);
        });
        viewModeDropdown.onClick(event -> viewModeDropdown.bringToFront());
    }

    /**
     * Resets all the controls to their default state.
     */
    public void resetControls() {
        String[] controlNames = {"pitch", "yaw", "roll", "fov", "size"};
        float[] defaultValues = {0.0f, 0.0f, 0.0f, 210.0f, 100.0f};
        for (int i = 0; i < controlNames.length; i++) {
            cp5.getController(controlNames[i]).setValue(defaultValues[i]);
        }
    }

    /**
     * Shows the control panel.
     */
    public void show() {
        cp5.show();
    }

    /**
     * Hides the control panel.
     */
    public void hide() {
        cp5.hide();
    }

    void makeEditable(Numberbox n) {
        final NumberboxInput nin = new NumberboxInput(n, p);
        n.onClick(theEvent -> {
            nin.setActive(true);
            numberboxActive = true;
        }).onLeave(theEvent -> {
            nin.setActive(false);
            numberboxActive = false;
            nin.submit();
        });
    }

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

    /**
     * Disposes of the ControlManager by releasing all resources and clearing the ControlP5 instance.
     */
    public void dispose() {
        cp5.dispose();
    }

    /**
     * The NumberboxInput class handles text input for a Numberbox control.
     * It allows users to type in values directly and updates the Numberbox accordingly.
     */
    public class NumberboxInput {
        String text = "";
        Numberbox n;
        boolean active;
        PApplet p;

        /**
         * Constructs a NumberboxInput with the specified Numberbox and PApplet.
         *
         * @param theNumberbox the Numberbox to be managed
         * @param p the PApplet instance
         */
        public NumberboxInput(Numberbox theNumberbox, PApplet p) {
            n = theNumberbox;
            this.p = p;
            p.registerMethod("keyEvent", this);
        }

        /**
         * Handles key events for the Numberbox input.
         * Updates the text and Numberbox value based on user input.
         *
         * @param k the KeyEvent to be processed
         */
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

        /**
         * Sets the active state of the Numberbox input.
         * When active, the Numberbox input is ready to receive user input.
         *
         * @param b the active state to set
         */
        public void setActive(boolean b) {
            active = b;
            if (active) {
                n.getValueLabel().setText("");
                text = "";
            }
        }

        /**
         * Submits the current text input to the Numberbox.
         * Updates the Numberbox value and the parent value accordingly.
         */
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

   /**
     * Handles events from the ControlP5 UI elements.
     * This method is called whenever a control event is triggered.
     *
     * @param theEvent the ControlEvent that triggered this method
     */
    public void handleEvent(ControlEvent theEvent) {
        if (theEvent.isFrom(previewToggle)) {
            parent.setShowPreview(previewToggle.getState());
        } else if (theEvent.isFrom(ndiToggle)) {
            parent.getOutputManager().toggleOutput("ndi");
        } else if (theEvent.isFrom(spoutToggle) && spoutToggle != null) {
            parent.getOutputManager().toggleOutput("spout");
        } else if (theEvent.isFrom(syphonToggle) && syphonToggle != null) {
            parent.getOutputManager().toggleOutput("syphon");
        }
    }

    /**
     * Checks if the number box is currently active.
     * @return true if the number box is active, false otherwise
     */
    public boolean isNumberboxActive() {
        return numberboxActive;
    }

    /**
     * Updates the FPS label with the current frame rate.
     * @param frameRate the current frame rate to display
     */
    public void updateFpsLabel(float frameRate) {
        fpsLabel.setText("FPS: " + PApplet.nf(frameRate, 0, 1));
    }
}
