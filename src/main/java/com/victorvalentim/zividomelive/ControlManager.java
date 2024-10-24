package com.victorvalentim.zividomelive;

import controlP5.*;
import processing.core.*;
import processing.event.KeyEvent;
import java.util.regex.Pattern;

/**
 * The ControlManager class manages the user interface controls for the application.
 * It uses ControlP5 for creating and handling UI elements such as sliders, buttons, and dropdown lists.
 */
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
    PApplet p;

    /**
     * Constructs a ControlManager with the specified PApplet, parent object, and base resolution.
     *
     * @param p the PApplet instance
     * @param parent the parent zividomelive instance
     * @param baseResolution the base resolution for the application
     */
    ControlManager(PApplet p, zividomelive parent, int baseResolution) {
        this.p = p;
        this.parent = parent;
        this.baseResolution = baseResolution;
        cp5 = new ControlP5(p);
        initializeControls();
        addNumberboxesAndSliders();
        addButtonsAndCheckboxes();
        addDropdownLists();
        resetControls();
    }

    /**
     * Initializes the basic controls such as the FPS label.
     */
    private void initializeControls() {
        int yOffset = 10;
        fpsLabel = cp5.addTextlabel("fpsLabel")
                .setPosition(10, yOffset)
                .setSize(200, 20)
                .setText("FPS: 0");
    }

    /**
     * Adds number boxes and sliders for controlling parameters like pitch, yaw, roll, etc.
     */
    private void addNumberboxesAndSliders() {
        int yOffset = 40;
        int controlSpacing = 30;
        addNumberbox("pitch", 10, yOffset, -PApplet.PI, PApplet.PI, parent.getPitch());
        addSlider("pitch", 10 + 60, yOffset, -PApplet.PI, PApplet.PI, parent.getPitch());
        addNumberbox("yaw", 10, yOffset + controlSpacing, -PApplet.PI, PApplet.PI, parent.getYaw());
        addSlider("yaw", 10 + 60, yOffset + controlSpacing, -PApplet.PI, PApplet.PI, parent.getYaw());
        addNumberbox("roll", 10, yOffset + 2 * controlSpacing, -PApplet.PI, PApplet.PI, parent.getRoll());
        addSlider("roll", 10 + 60, yOffset + 2 * controlSpacing, -PApplet.PI, PApplet.PI, parent.getRoll());
        addNumberbox("fov", 10, yOffset + 3 * controlSpacing, 0, 360, parent.getFov());
        addSlider("fov", 10 + 60, yOffset + 3 * controlSpacing, 0, 360, parent.getFov());
        addNumberbox("size", 10, yOffset + 4 * controlSpacing, 0, 100, parent.getFishSize());
        addSlider("size", 10 + 60, yOffset + 4 * controlSpacing, 0, 100, parent.getFishSize());
    }

    /**
     * Adds buttons and checkboxes for resetting controls and controlling preview/output.
     */
    private void addButtonsAndCheckboxes() {
        int yOffset = 190;
        int controlSpacing = 30;
        cp5.addButton("resetControls")
                .setPosition(10, yOffset)
                .setSize(200, 20)
                .setLabel("Reset Controls")
                .onClick(event -> parent.resetControls());
        previewCheckbox = cp5.addCheckBox("previewCheckbox")
                .setPosition(10, yOffset + controlSpacing)
                .setSize(20, 20)
                .addItem("Preview Domemaster", 0)
                .setValue(parent.isShowPreview() ? 1 : 0);
        outputCheckbox = cp5.addCheckBox("outputCheckbox")
                .setPosition(10, yOffset + 2 * controlSpacing)
                .setSize(20, 20)
                .addItem("Enable Output", 0)
                .setValue(parent.isEnableOutput() ? 1 : 0);
    }

    /**
     * Adds dropdown lists for selecting resolution and view mode.
     */
    private void addDropdownLists() {
        int yOffset = 280;
        int controlSpacing = 30;
        addViewModeDropdown(yOffset);
        addResolutionDropdown(yOffset + controlSpacing);
    }

    /**
     * Adds a number box for controlling values.
     *
     * @param name the name of the number box
     * @param x the x position of the number box
     * @param y the y position of the number box
     * @param min the minimum value of the number box
     * @param max the maximum value of the number box
     * @param value the initial value of the number box
     */
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

    /**
     * Adds a slider for continuous value control.
     *
     * @param name the name of the slider
     * @param x the x position of the slider
     * @param y the y position of the slider
     * @param min the minimum value of the slider
     * @param max the maximum value of the slider
     * @param value the initial value of the slider
     */
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

    /**
     * Adds a dropdown list for selecting resolution.
     *
     * @param y the y position of the dropdown list
     */
    void addResolutionDropdown(float y) {
        resolutionDropdown = cp5.addDropdownList("Resolution")
                .setPosition(10, y)
                .setSize(200, 200)
                .setBarHeight(20)
                .setItemHeight(20)
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

    /**
     * Adds a dropdown list for selecting view mode.
     *
     * @param y the y position of the dropdown list
     */
    void addViewModeDropdown(float y) {
        viewModeDropdown = cp5.addDropdownList("View Mode")
                .setPosition(10, y)
                .setSize(200, 200)
                .setItemHeight(20)
                .setBarHeight(20)
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
     * Resets all controls to their default values.
     */
    void resetControls() {
        String[] controlNames = {"pitch", "yaw", "roll", "fov", "size"};
        float[] defaultValues = {0.0f, 0.0f, 0.0f, 210.0f, 100.0f};
        for (int i = 0; i < controlNames.length; i++) {
            cp5.getController(controlNames[i]).setValue(defaultValues[i]);
        }
    }

    /**
     * Shows the control panel.
     */
    void show() {
        cp5.show();
    }

    /**
     * Hides the control panel.
     */
    void hide() {
        cp5.hide();
    }

    /**
     * Makes a Numberbox editable by handling key input.
     *
     * @param n the Numberbox to make editable
     */
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

    /**
     * Updates the parent object's value based on the control name.
     *
     * @param name the name of the control
     * @param value the value to set
     */
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
     * Inner class to make a Numberbox editable by handling key input.
     */
    public class NumberboxInput {
        String text = "";
        Numberbox n;
        boolean active;
        PApplet p;

        /**
         * Constructs a NumberboxInput with the specified Numberbox and PApplet.
         *
         * @param theNumberbox the Numberbox to make editable
         * @param p the PApplet instance
         */
        NumberboxInput(Numberbox theNumberbox, PApplet p) {
            n = theNumberbox;
            this.p = p;
            p.registerMethod("keyEvent", this);
        }

        /**
         * Handles key events for the Numberbox.
         *
         * @param k the KeyEvent to handle
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
         * Sets the active state of the NumberboxInput.
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
         * Submits the current text as the Numberbox value.
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
     * Handles control events.
     *
     * @param theEvent the ControlEvent to handle
     */
    public void handleEvent(ControlEvent theEvent) {
        if (theEvent.isFrom(previewCheckbox)) {
            parent.setShowPreview(!parent.isShowPreview());
        } else if (theEvent.isFrom(outputCheckbox)) {
            parent.setEnableOutput(!parent.isEnableOutput());
        }
    }

    /**
     * Returns whether the number box is active.
     *
     * @return true if the number box is active, false otherwise
     */
    public boolean isNumberboxActive() {
        return numberboxActive;
    }

    /**
     * Updates the FPS label with the current frame rate.
     *
     * @param frameRate the current frame rate
     */
    public void updateFpsLabel(float frameRate) {
        fpsLabel.setText("FPS: " + PApplet.nf(frameRate, 0, 1));
    }
}