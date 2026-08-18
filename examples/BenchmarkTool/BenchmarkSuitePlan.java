import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Deterministic, Processing-independent execution plan for automated benchmark suites. */
public final class BenchmarkSuitePlan {
    private BenchmarkSuitePlan() {
    }

    public enum Suite {
        MODES,
        MATRIX,
        TRANSITIONS,
        ALL;

        public static Suite parse(String value) {
            if (value == null || value.trim().isEmpty()) return MODES;

            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Unknown benchmark suite '" + value
                                + "'; expected MODES, MATRIX, TRANSITIONS, or ALL.");
            }
        }
    }

    public enum Kind {
        STEADY_STATE,
        TRANSITION
    }

    public static List<Scenario> create(
            Suite suite,
            String scene,
            int selectedResolution,
            int[] resolutions,
            boolean preview,
            int transitionBaselineFrames,
            int transitionPostFrames) {

        if (suite == null) {
            throw new IllegalArgumentException("Suite is required.");
        }

        if (scene == null || scene.isBlank()) {
            throw new IllegalArgumentException("Scene is required.");
        }

        if (resolutions == null || resolutions.length == 0) {
            throw new IllegalArgumentException(
                    "At least one resolution is required.");
        }

        if (transitionBaselineFrames < 2 || transitionPostFrames < 2) {
            throw new IllegalArgumentException(
                    "Transition baseline and post intervals require at least two frames.");
        }

        int[] safeResolutions =
                Arrays.stream(resolutions)
                        .distinct()
                        .sorted()
                        .toArray();

        for (int resolution : safeResolutions) {
            if (resolution <= 0) {
                throw new IllegalArgumentException(
                        "Resolutions must be positive.");
            }
        }

        List<Scenario> scenarios = new ArrayList<>();

        if (suite == Suite.MODES) {
            addModes(
                    scenarios,
                    scene,
                    selectedResolution,
                    preview);

        } else if (suite == Suite.MATRIX) {
            addMatrix(
                    scenarios,
                    scene,
                    selectedResolution,
                    safeResolutions);

        } else if (suite == Suite.TRANSITIONS) {
            addTransitions(
                    scenarios,
                    scene,
                    selectedResolution,
                    transitionBaselineFrames,
                    transitionPostFrames);

        } else {
            addMatrix(
                    scenarios,
                    scene,
                    selectedResolution,
                    safeResolutions);

            addTransitions(
                    scenarios,
                    scene,
                    selectedResolution,
                    transitionBaselineFrames,
                    transitionPostFrames);
        }

        return Collections.unmodifiableList(scenarios);
    }

    private static void addModes(
            List<Scenario> scenarios,
            String scene,
            int resolution,
            boolean preview) {

        for (String mode : List.of(
                "STANDARD",
                "DOMEMASTER",
                "EQUIRECTANGULAR",
                "SKYBOX")) {

            scenarios.add(
                    Scenario.steady(
                            "MODE_" + mode,
                            new Endpoint(
                                    mode,
                                    resolution,
                                    scene,
                                    preview,
                                    false,
                                    false,
                                    false)));
        }
    }

    /**
     * Adds the deterministic output-resolution matrix.
     *
     * <p>STANDARD_WINDOW remains a Processing window/preview-domain baseline.
     * Spherical resolution buckets deliberately enable NDI so the ziviDomeLive
     * runtime must instantiate and render the output-resolution spherical
     * pipeline rather than silently using the preview cubemap.</p>
     *
     * <p>NDI is used because it is the cross-platform output backend shared by
     * macOS, Windows, and Linux. Preview remains disabled for spherical matrix
     * scenarios so preview-specific rendering does not add another consumer to
     * the workload.</p>
     */
    private static void addMatrix(
            List<Scenario> scenarios,
            String scene,
            int selectedResolution,
            int[] resolutions) {

        /*
         * STANDARD does not derive its raster dimensions from the spherical
         * cubemap resolution. Keep one window-domain baseline instead of
         * repeating nominal cubemap resolutions that would not change its
         * effective workload.
         */
        scenarios.add(
                Scenario.steady(
                        "STANDARD_WINDOW",
                        new Endpoint(
                                "STANDARD",
                                selectedResolution,
                                scene,
                                false,
                                false,
                                false,
                                false)));

        /*
         * A spherical MATRIX scenario must exercise OUTPUT_BASE.
         *
         * Enabling NDI creates a real cross-platform output demand, causing the
         * selected output resolution to participate in the render graph.
         */
        for (int resolution : resolutions) {
            for (String mode : List.of(
                    "DOMEMASTER",
                    "EQUIRECTANGULAR",
                    "SKYBOX")) {

                scenarios.add(
                        Scenario.steady(
                                mode + "_" + resolution,
                                new Endpoint(
                                        mode,
                                        resolution,
                                        scene,
                                        false,
                                        true,
                                        false,
                                        false)));
            }
        }
    }

    private static void addTransitions(
            List<Scenario> scenarios,
            String scene,
            int selectedResolution,
            int baselineFrames,
            int postFrames) {

        /*
         * Resolution transitions must exercise the output-resolution domain.
         * NDI remains enabled on both sides so the only intentional transition
         * here is 2048 -> 4096.
         */
        scenarios.add(
                Scenario.transition(
                        "RESOLUTION_2048_TO_4096",
                        new Endpoint(
                                "DOMEMASTER",
                                2048,
                                scene,
                                false,
                                true,
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                4096,
                                scene,
                                false,
                                true,
                                false,
                                false),
                        baselineFrames,
                        postFrames));

        scenarios.add(
                Scenario.transition(
                        "STANDARD_TO_DOMEMASTER",
                        new Endpoint(
                                "STANDARD",
                                selectedResolution,
                                scene,
                                false,
                                false,
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                scene,
                                false,
                                false,
                                false,
                                false),
                        baselineFrames,
                        postFrames));

        scenarios.add(
                Scenario.transition(
                        "PREVIEW_OFF_TO_ON",
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                scene,
                                false,
                                false,
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                scene,
                                true,
                                false,
                                false,
                                false),
                        baselineFrames,
                        postFrames));

        scenarios.add(
                Scenario.transition(
                        "SCENE_LIGHT_TO_HEAVY",
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                "LIGHT",
                                false,
                                false,
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                "HEAVY",
                                false,
                                false,
                                false,
                                false),
                        baselineFrames,
                        postFrames));

        scenarios.add(
                Scenario.transition(
                        "NDI_OFF_TO_ON",
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                scene,
                                false,
                                false,
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                scene,
                                false,
                                true,
                                false,
                                false),
                        baselineFrames,
                        postFrames));
    }

    public static final class Endpoint {
        public final String renderMode;
        public final int resolution;
        public final String scene;
        public final boolean preview;
        public final boolean ndi;
        public final boolean syphon;
        public final boolean spout;

        public Endpoint(
                String renderMode,
                int resolution,
                String scene,
                boolean preview,
                boolean ndi,
                boolean syphon,
                boolean spout) {

            if (renderMode == null || renderMode.isBlank()) {
                throw new IllegalArgumentException(
                        "Render mode is required.");
            }

            if (resolution <= 0) {
                throw new IllegalArgumentException(
                        "Resolution must be positive.");
            }

            if (scene == null || scene.isBlank()) {
                throw new IllegalArgumentException(
                        "Scene is required.");
            }

            this.renderMode = renderMode;
            this.resolution = resolution;
            this.scene = scene;
            this.preview = preview;
            this.ndi = ndi;
            this.syphon = syphon;
            this.spout = spout;
        }

        public String description() {
            return renderMode
                    + " " + resolution
                    + " " + scene
                    + " preview=" + preview
                    + " ndi=" + ndi
                    + " syphon=" + syphon
                    + " spout=" + spout;
        }
    }

    public static final class Scenario {
        public final String name;
        public final Kind kind;
        public final Endpoint initial;
        public final Endpoint target;
        public final int baselineFrames;
        public final int postFrames;

        private Scenario(
                String name,
                Kind kind,
                Endpoint initial,
                Endpoint target,
                int baselineFrames,
                int postFrames) {

            this.name = name;
            this.kind = kind;
            this.initial = initial;
            this.target = target;
            this.baselineFrames = baselineFrames;
            this.postFrames = postFrames;
        }

        public static Scenario steady(
                String name,
                Endpoint endpoint) {

            return new Scenario(
                    name,
                    Kind.STEADY_STATE,
                    endpoint,
                    endpoint,
                    0,
                    0);
        }

        public static Scenario transition(
                String name,
                Endpoint initial,
                Endpoint target,
                int baselineFrames,
                int postFrames) {

            return new Scenario(
                    name,
                    Kind.TRANSITION,
                    initial,
                    target,
                    baselineFrames,
                    postFrames);
        }
    }
}