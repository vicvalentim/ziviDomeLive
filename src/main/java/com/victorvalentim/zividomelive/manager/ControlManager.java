package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.zividomelive;
import controlP5.*;
import processing.core.*;
import processing.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.function.Consumer;

/**
 * The ControlManager class manages the user interface controls for the application.
 * It uses ControlP5 for creating and handling UI elements such as sliders, buttons, and dropdown lists.
 * This class also manages the toggling and view selection for output methods (NDI, Spout, Syphon).
 */
public class ControlManager {

    private final ControlP5 cp5;
    private final ControlListener parentControlListener;
    private boolean numberboxActive = false;
    private final zividomelive parent;
    private Toggle previewToggle;
    private Toggle ndiToggle;
    private Toggle spoutToggle;
    private Toggle syphonToggle;
    private DropdownList resolutionDropdown;
    private DropdownList viewModeDropdown;
    private DropdownList ndiViewDropdown;
    private DropdownList spoutViewDropdown;
    private DropdownList syphonViewDropdown;
    private Textlabel fpsLabel;
    private final PApplet p;
    private final List<NumberboxInput> numberboxInputs = new ArrayList<>();

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
        cp5 = new ControlP5(p);

        addGlobalControls();
        addSphericalControls();
        addViewControls();
        addOutputControls(baseResolution);

        // Reset controls to default state
        resetControls();

        parentControlListener = parent::controlEvent;
        cp5.addListener(parentControlListener);
    }

    /**
     * Adds controls whose values describe the application as a whole.
     */
    private void addGlobalControls() {
        fpsLabel = cp5.addTextlabel("fpsLabel")
                .setPosition(
                        ControlPanelLayout.CONTROL_X,
                        ControlPanelLayout.yFor(ControlScope.GLOBAL, "fpsLabel"))
                .setSize(ControlPanelLayout.PANEL_WIDTH, ControlPanelLayout.CONTROL_HEIGHT)
                .setText("FPS: 0");
    }

    /**
     * Adds the shared spherical orientation and domemaster calibration controls.
     */
    private void addSphericalControls() {
        for (ControlPanelLayout.SphericalControlSpec control : ControlPanelLayout.sphericalControls()) {
            float y = ControlPanelLayout.yFor(ControlScope.SPHERICAL, control.name());
            float minimum = (float) control.minimum();
            float maximum = (float) control.maximum();
            float value = getParentValue(control.name());
            addNumberbox(control.name(), y, minimum, maximum, value);
            addSlider(control.name(), y, minimum, maximum, value);
        }

        cp5.addButton("resetControls")
                .setPosition(
                        ControlPanelLayout.CONTROL_X,
                        ControlPanelLayout.yFor(ControlScope.SPHERICAL, "resetControls"))
                .setSize(ControlPanelLayout.PANEL_WIDTH, ControlPanelLayout.CONTROL_HEIGHT)
                .setLabel("Reset Controls")
                .getCaptionLabel().align(ControlP5.CENTER, ControlP5.CENTER)
                .setPaddingX(5);
        cp5.getController("resetControls").onClick(event -> parent.resetControls());
    }

    /** Adds preview representation controls without changing their legacy positions. */
    private void addViewControls() {
        previewToggle = cp5.addToggle("previewToggle")
                .setPosition(
                        ControlPanelLayout.CONTROL_X,
                        ControlPanelLayout.yFor(ControlScope.VIEW, "previewToggle"))
                .setSize(ControlPanelLayout.CONTROL_HEIGHT, ControlPanelLayout.CONTROL_HEIGHT)
                .setValue(parent.isShowPreview());
        previewToggle.getCaptionLabel()
                .align(ControlP5.RIGHT_OUTSIDE, ControlP5.CENTER)
                .setPaddingX(5)
                .setText("Preview Domemaster");
        previewToggle.onChange(event -> parent.setShowPreview(previewToggle.getState()));

        addViewModeDropdown(ControlPanelLayout.yFor(ControlScope.VIEW, "View Mode"));
    }

    /** Adds output resolution, publication toggles, and per-output view routing. */
    private void addOutputControls(int baseResolution) {
        addResolutionDropdown(
                ControlPanelLayout.yFor(ControlScope.OUTPUTS, "Output Resolution"),
                baseResolution);
        addOutputToggles();
        addOutputViewDropdowns();
    }

    /**
     * Adds toggles for enabling/disabling output methods: NDI, Spout, and Syphon.
     * Each toggle controls the visibility of a corresponding view mode dropdown list.
     */
    private void addOutputToggles() {
        // NDI Toggle
        ndiToggle = cp5.addToggle("ndiToggle")
                .setPosition(
                        ControlPanelLayout.CONTROL_X,
                        ControlPanelLayout.yFor(ControlScope.OUTPUTS, "ndiToggle"))
                .setSize(ControlPanelLayout.CONTROL_HEIGHT, ControlPanelLayout.CONTROL_HEIGHT)
                .setValue(parent.getOutputManager().isNdiEnabled());
        ndiToggle.getCaptionLabel()
                .align(ControlP5.RIGHT_OUTSIDE, ControlP5.CENTER)
                .setPaddingX(5)
                .setText("Enable NDI");
        ndiToggle.onChange(event -> {
            parent.getOutputManager().toggleOutput("ndi");
            toggleDropdownVisibility();
        });

        ControlPanelLayout.LocalOutput localOutput =
                ControlPanelLayout.localOutputFor(System.getProperty("os.name"));
        if (localOutput == ControlPanelLayout.LocalOutput.SPOUT) {
            spoutToggle = cp5.addToggle("spoutToggle")
                    .setPosition(
                            ControlPanelLayout.CONTROL_X,
                            ControlPanelLayout.yFor(ControlScope.OUTPUTS, "spoutToggle"))
                    .setSize(ControlPanelLayout.CONTROL_HEIGHT, ControlPanelLayout.CONTROL_HEIGHT)
                    .setValue(parent.getOutputManager().isSpoutEnabled());
            spoutToggle.getCaptionLabel()
                    .align(ControlP5.RIGHT_OUTSIDE, ControlP5.CENTER)
                    .setPaddingX(5)
                    .setText("Enable Spout");
            spoutToggle.onChange(event -> {
                parent.getOutputManager().toggleOutput("spout");
                toggleDropdownVisibility();
            });
        } else if (localOutput == ControlPanelLayout.LocalOutput.SYPHON) {
            syphonToggle = cp5.addToggle("syphonToggle")
                    .setPosition(
                            ControlPanelLayout.CONTROL_X,
                            ControlPanelLayout.yFor(ControlScope.OUTPUTS, "syphonToggle"))
                    .setSize(ControlPanelLayout.CONTROL_HEIGHT, ControlPanelLayout.CONTROL_HEIGHT)
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
     */
    private void addOutputViewDropdowns() {
        ndiViewDropdown = createViewDropdown(
                "NDI View",
                parent.getOutputManager().getViewForOutput(OutputManager.OutputType.NDI),
                view -> parent.getOutputManager().setNdiView(view));

        if (spoutToggle != null) {
            spoutViewDropdown = createViewDropdown(
                    "Spout View",
                    parent.getOutputManager().getViewForOutput(OutputManager.OutputType.SPOUT),
                    view -> parent.getOutputManager().setSpoutView(view));
        }
        if (syphonToggle != null) {
            syphonViewDropdown = createViewDropdown(
                    "Syphon View",
                    parent.getOutputManager().getViewForOutput(OutputManager.OutputType.SYPHON),
                    view -> parent.getOutputManager().setSyphonView(view));
        }

        toggleDropdownVisibility();
    }

    /**
     * Helper method to create a view mode dropdown for an output toggle.
     * @param label the label for the dropdown.
     * @param initialView the view currently configured for the output.
     * @param setView the consumer function to set the view type in OutputManager.
     * @return the created DropdownList.
     */
    private DropdownList createViewDropdown(
            String label,
            zividomelive.ViewType initialView,
            Consumer<zividomelive.ViewType> setView) {
        DropdownList dropdown = cp5.addDropdownList(label)
                .setPosition(
                        ControlPanelLayout.CONTROL_X,
                        ControlPanelLayout.yFor(ControlScope.OUTPUTS, label))
                .setSize(ControlPanelLayout.PANEL_WIDTH, ControlPanelLayout.PANEL_WIDTH)
                .setBarHeight(ControlPanelLayout.CONTROL_HEIGHT)
                .setItemHeight(ControlPanelLayout.CONTROL_HEIGHT)
                .setVisible(false)
                .close();
        for (String viewMode : ControlPanelLayout.viewLabels()) {
            dropdown.addItem(viewMode, dropdown.getItems().size());
        }
        dropdown.setValue(ControlPanelLayout.indexForView(initialView));
        dropdown.onChange(event -> {
            int selectedIndex = (int) event.getController().getValue();
            setView.accept(ControlPanelLayout.viewForIndex(selectedIndex));
        });
        dropdown.onClick(event -> dropdown.bringToFront());
        return dropdown;
    }

    /**
     * Toggles the visibility of the view mode dropdown lists for each output based on the state of the toggles.
     */
    private void toggleDropdownVisibility() {
        ndiViewDropdown.setVisible(ndiToggle.getState());
        if (spoutViewDropdown != null) {
            spoutViewDropdown.setVisible(spoutToggle.getState());
        }
        if (syphonViewDropdown != null) {
            syphonViewDropdown.setVisible(syphonToggle.getState());
        }
    }

    private void addNumberbox(String name, float y, float min, float max, float value) {
        Numberbox numberbox = cp5.addNumberbox(name + "Value")
                .setPosition(ControlPanelLayout.CONTROL_X, y)
                .setSize(50, ControlPanelLayout.CONTROL_HEIGHT)
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
                .setSize(140, ControlPanelLayout.CONTROL_HEIGHT)
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

    void addResolutionDropdown(float y, int baseResolution) {
        resolutionDropdown = cp5.addDropdownList("Output Resolution")
                .setPosition(ControlPanelLayout.CONTROL_X, y)
                .setSize(ControlPanelLayout.PANEL_WIDTH, ControlPanelLayout.PANEL_WIDTH)
                .setBarHeight(ControlPanelLayout.CONTROL_HEIGHT)
                .setItemHeight(ControlPanelLayout.CONTROL_HEIGHT)
                .close();
        List<Integer> resolutions = ControlPanelLayout.outputResolutions();
        for (int i = 0; i < resolutions.size(); i++) {
            resolutionDropdown.addItem(
                    "Resolution " + (i + 1) + "k " + resolutions.get(i),
                    i);
        }
        int selectedIndex = ControlPanelLayout.indexForOutputResolution(baseResolution);
        if (selectedIndex >= 0) {
            resolutionDropdown.setValue(selectedIndex);
        }
        resolutionDropdown.onChange(event -> {
            int index = (int) event.getController().getValue();
            int newResolution = ControlPanelLayout.outputResolutionForIndex(index);
            parent.resetGraphics(newResolution);
        });
        resolutionDropdown.onClick(event -> resolutionDropdown.bringToFront());
    }

    void addViewModeDropdown(float y) {
        viewModeDropdown = cp5.addDropdownList("View Mode")
                .setPosition(ControlPanelLayout.CONTROL_X, y)
                .setSize(ControlPanelLayout.PANEL_WIDTH, ControlPanelLayout.PANEL_WIDTH)
                .setItemHeight(ControlPanelLayout.CONTROL_HEIGHT)
                .setBarHeight(ControlPanelLayout.CONTROL_HEIGHT)
                .close();
        for (String viewMode : ControlPanelLayout.viewLabels()) {
            viewModeDropdown.addItem(viewMode, viewModeDropdown.getItems().size());
        }
        viewModeDropdown.setValue(ControlPanelLayout.indexForView(parent.getCurrentView()));
        viewModeDropdown.onChange(event -> {
            int selectedIndex = (int) event.getController().getValue();
            parent.setCurrentView(ControlPanelLayout.viewForIndex(selectedIndex));
        });
        viewModeDropdown.onClick(event -> viewModeDropdown.bringToFront());
    }

    /**
     * Resets all the controls to their default state.
     */
    public void resetControls() {
        for (ControlPanelLayout.SphericalControlSpec control : ControlPanelLayout.sphericalControls()) {
            cp5.getController(control.name()).setValue(control.defaultValue());
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

    /**
     * Synchronizes the ControlP5 widgets with the externally toggled panel visibility state.
     *
     * @param visible true to show the panel widgets, false to hide them
     */
    public void syncPanelVisibility(boolean visible) {
        if (visible) {
            show();
        } else {
            hide();
        }
    }

    void makeEditable(Numberbox n) {
        final NumberboxInput nin = new NumberboxInput(n, p);
        numberboxInputs.add(nin);
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
                break;
            default:
                throw new IllegalArgumentException("Unknown spherical control: " + name);
        }
    }

    private float getParentValue(String name) {
        switch (name) {
            case "pitch":
                return parent.getPitch();
            case "yaw":
                return parent.getYaw();
            case "roll":
                return parent.getRoll();
            case "fov":
                return parent.getFov();
            case "size":
                return parent.getFishSize();
            default:
                throw new IllegalArgumentException("Unknown spherical control: " + name);
        }
    }

    /**
     * Disposes of the ControlManager by releasing all resources and clearing the ControlP5 instance.
     */
    public void dispose() {
        for (NumberboxInput input : numberboxInputs) {
            try {
                p.unregisterMethod("keyEvent", input);
            } catch (RuntimeException ignored) {
                // Continue releasing the remaining controls.
            }
        }
        numberboxInputs.clear();
        cp5.removeListener(parentControlListener);
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
        if (theEvent == null) {
            return;
        }

        if (theEvent.isFrom(previewToggle)) {
            parent.setShowPreview(previewToggle.getState());
        } else if (theEvent.isFrom(ndiToggle)
                || (spoutToggle != null && theEvent.isFrom(spoutToggle))
                || (syphonToggle != null && theEvent.isFrom(syphonToggle))) {
            // Output publication is changed exclusively by each toggle's onChange callback.
            toggleDropdownVisibility();
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
