package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.ziviDomeLive;

import java.util.List;
import java.util.Locale;

/** Stable layout and option model for the built-in ControlP5 panel. */
final class ControlPanelLayout {

    static final int CONTROL_X = 10;
    static final int CONTROL_HEIGHT = 20;
    static final int CONTROL_SPACING = 35;
    static final int INITIAL_Y = 20;
    static final int PANEL_WIDTH = 200;

    private static final List<ControlSlot> CONTROL_SLOTS = List.of(
            new ControlSlot("fpsLabel", ControlScope.GLOBAL, 0),
            new ControlSlot("pitchValue", ControlScope.SPHERICAL, 1),
            new ControlSlot("pitch", ControlScope.SPHERICAL, 1),
            new ControlSlot("yawValue", ControlScope.SPHERICAL, 2),
            new ControlSlot("yaw", ControlScope.SPHERICAL, 2),
            new ControlSlot("rollValue", ControlScope.SPHERICAL, 3),
            new ControlSlot("roll", ControlScope.SPHERICAL, 3),
            new ControlSlot("fovValue", ControlScope.SPHERICAL, 4),
            new ControlSlot("fov", ControlScope.SPHERICAL, 4),
            new ControlSlot("sizeValue", ControlScope.SPHERICAL, 5),
            new ControlSlot("size", ControlScope.SPHERICAL, 5),
            new ControlSlot("resetControls", ControlScope.SPHERICAL, 6),
            new ControlSlot("previewToggle", ControlScope.VIEW, 7),
            new ControlSlot("View Mode", ControlScope.VIEW, 8),
            new ControlSlot("Output Resolution", ControlScope.OUTPUTS, 9),
            new ControlSlot("ndiToggle", ControlScope.OUTPUTS, 10),
            new ControlSlot("spoutToggle", ControlScope.OUTPUTS, 11),
            new ControlSlot("syphonToggle", ControlScope.OUTPUTS, 11),
            new ControlSlot("NDI View", ControlScope.OUTPUTS, 12),
            new ControlSlot("Spout View", ControlScope.OUTPUTS, 13),
            new ControlSlot("Syphon View", ControlScope.OUTPUTS, 13)
    );

    private static final List<SphericalControlSpec> SPHERICAL_CONTROLS = List.of(
            new SphericalControlSpec("pitch", -Math.PI, Math.PI, 0.0f),
            new SphericalControlSpec("yaw", -Math.PI, Math.PI, 0.0f),
            new SphericalControlSpec("roll", -Math.PI, Math.PI, 0.0f),
            new SphericalControlSpec("fov", 0.0, 360.0, 210.0f),
            new SphericalControlSpec("size", 0.0, 100.0, 100.0f)
    );

    private static final List<String> VIEW_LABELS = List.of(
            "Fisheye Domemaster",
            "Equirectangular",
            "Cubemap Skybox",
            "Standard"
    );

    private static final List<Integer> OUTPUT_RESOLUTIONS = List.of(1024, 2048, 3072, 4096);

    private ControlPanelLayout() {
    }

    static int yFor(String controlName) {
        return INITIAL_Y + slotFor(controlName).row() * CONTROL_SPACING;
    }

    static int yFor(ControlScope scope, String controlName) {
        ControlSlot slot = slotFor(controlName);
        if (slot.scope() != scope) {
            throw new IllegalArgumentException(
                    "Control " + controlName + " belongs to " + slot.scope() + ", not " + scope);
        }
        return INITIAL_Y + slot.row() * CONTROL_SPACING;
    }

    static List<ControlSlot> slotsFor(ControlScope scope) {
        return CONTROL_SLOTS.stream()
                .filter(slot -> slot.scope() == scope)
                .toList();
    }

    static List<SphericalControlSpec> sphericalControls() {
        return SPHERICAL_CONTROLS;
    }

    static boolean isCyclicAngle(String controlName) {
        return "pitch".equals(controlName)
                || "yaw".equals(controlName)
                || "roll".equals(controlName);
    }

    static float wrapCyclic(float value, float minimum, float maximum) {
        float span = maximum - minimum;
        if (!Float.isFinite(value) || !Float.isFinite(span) || span <= 0.0f) {
            return value;
        }
        float wrapped = (value - minimum) % span;
        if (wrapped < 0.0f) {
            wrapped += span;
        }
        return minimum + wrapped;
    }

    static ControlVisibility visibilityFor(RenderMode renderMode, boolean floatingDomemaster) {
        RenderMode effectiveMode = renderMode == null ? RenderMode.FULL : renderMode;
        return switch (effectiveMode) {
            case FULL -> new ControlVisibility(true, true, true, true, true, true);
            case STANDARD -> new ControlVisibility(
                    floatingDomemaster,
                    floatingDomemaster,
                    floatingDomemaster,
                    true,
                    false,
                    false);
            case DOMEMASTER -> new ControlVisibility(true, true, true, false, false, false);
            case EQUIRECTANGULAR, SKYBOX ->
                    new ControlVisibility(true, false, true, false, false, false);
        };
    }

    static List<String> viewLabels() {
        return VIEW_LABELS;
    }

    static ziviDomeLive.ViewType viewForIndex(int index) {
        return ziviDomeLive.ViewType.values()[index];
    }

    static int indexForView(ziviDomeLive.ViewType view) {
        return view == null ? 0 : view.ordinal();
    }

    static List<Integer> outputResolutions() {
        return OUTPUT_RESOLUTIONS;
    }

    static int outputResolutionForIndex(int index) {
        return OUTPUT_RESOLUTIONS.get(index);
    }

    static int indexForOutputResolution(int resolution) {
        return OUTPUT_RESOLUTIONS.indexOf(resolution);
    }

    static LocalOutput localOutputFor(String osName) {
        String normalizedName = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (normalizedName.contains("win")) {
            return LocalOutput.SPOUT;
        }
        if (normalizedName.contains("mac")) {
            return LocalOutput.SYPHON;
        }
        return LocalOutput.NONE;
    }

    private static ControlSlot slotFor(String controlName) {
        return CONTROL_SLOTS.stream()
                .filter(slot -> slot.name().equals(controlName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown control: " + controlName));
    }

    record ControlSlot(String name, ControlScope scope, int row) {
    }

    record SphericalControlSpec(String name, double minimum, double maximum, float defaultValue) {
    }

    record ControlVisibility(
            boolean sphericalOrientation,
            boolean domemasterCalibration,
            boolean resetControls,
            boolean floatingDomemasterPreview,
            boolean previewViewSelection,
            boolean outputViewSelection) {

        boolean sphericalControlVisible(String controlName) {
            if (isCyclicAngle(controlName)) {
                return sphericalOrientation;
            }
            return switch (controlName) {
                case "fov", "size" -> domemasterCalibration;
                default -> throw new IllegalArgumentException(
                        "Unknown spherical control: " + controlName);
            };
        }

        boolean outputViewVisible(boolean outputEnabled) {
            return outputViewSelection && outputEnabled;
        }
    }

    enum LocalOutput {
        NONE,
        SPOUT,
        SYPHON
    }
}
