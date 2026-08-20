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

        return create(
                suite,
                scene,
                selectedResolution,
                resolutions,
                preview,
                transitionBaselineFrames,
                transitionPostFrames,
                false);
    }

    public static List<Scenario> create(
            Suite suite,
            String scene,
            int selectedResolution,
            int[] resolutions,
            boolean preview,
            int transitionBaselineFrames,
            int transitionPostFrames,
            boolean benchmarkNdi) {

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
                    preview,
                    benchmarkNdi);

        } else if (suite == Suite.MATRIX) {
            addMatrix(
                    scenarios,
                    scene,
                    selectedResolution,
                    safeResolutions,
                    benchmarkNdi);

        } else if (suite == Suite.TRANSITIONS) {
            addTransitions(
                    scenarios,
                    scene,
                    selectedResolution,
                    transitionBaselineFrames,
                    transitionPostFrames,
                    benchmarkNdi);

        } else {
            addMatrix(
                    scenarios,
                    scene,
                    selectedResolution,
                    safeResolutions,
                    benchmarkNdi);

            addTransitions(
                    scenarios,
                    scene,
                    selectedResolution,
                    transitionBaselineFrames,
                    transitionPostFrames,
                    benchmarkNdi);
        }

        return Collections.unmodifiableList(scenarios);
    }

    private static void addModes(
            List<Scenario> scenarios,
            String scene,
            int resolution,
            boolean preview,
            boolean benchmarkNdi) {

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
                                    !mode.equals("STANDARD") || benchmarkNdi,
                                    benchmarkNdi,
                                    false,
                                    false)));
        }
    }

    /**
     * Adds the deterministic output-resolution matrix.
     *
     * <p>STANDARD_WINDOW remains a Processing window/preview-domain baseline unless NDI is
     * explicitly requested. Spherical resolution buckets always carry an internal output demand
     * so the selected resolution participates in the render graph without requiring an external
     * transport backend.</p>
     *
     * <p>NDI is opt-in. When enabled, it is layered on top of the same output-resolution workload
     * instead of being used as the mechanism that creates that workload.</p>
     */
    private static void addMatrix(
            List<Scenario> scenarios,
            String scene,
            int selectedResolution,
            int[] resolutions,
            boolean benchmarkNdi) {

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
                                benchmarkNdi,
                                benchmarkNdi,
                                false,
                                false)));

        /*
         * Spherical MATRIX scenarios always exercise OUTPUT_BASE through the
         * internal benchmark demand. NDI remains an independent opt-in cost.
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
                                        benchmarkNdi,
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
            int postFrames,
            boolean benchmarkNdi) {

        /*
         * Resolution transitions keep the output-resolution render graph active on both sides,
         * isolating the requested 2048 -> 4096 resource transition from external transport cost.
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
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                4096,
                                scene,
                                false,
                                true,
                                false,
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
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                scene,
                                false,
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
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                scene,
                                true,
                                false,
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
                                false,
                                false),
                        new Endpoint(
                                "DOMEMASTER",
                                selectedResolution,
                                "HEAVY",
                                false,
                                false,
                                false,
                                false,
                                false),
                        baselineFrames,
                        postFrames));

        if (benchmarkNdi) {
            /*
             * Keep OUTPUT_BASE active before and after the transition so NDI_OFF_TO_ON isolates
             * transport activation instead of also introducing the high-resolution render graph.
             */
            scenarios.add(
                    Scenario.transition(
                            "NDI_OFF_TO_ON",
                            new Endpoint(
                                    "DOMEMASTER",
                                    selectedResolution,
                                    scene,
                                    false,
                                    true,
                                    false,
                                    false,
                                    false),
                            new Endpoint(
                                    "DOMEMASTER",
                                    selectedResolution,
                                    scene,
                                    false,
                                    true,
                                    true,
                                    false,
                                    false),
                            baselineFrames,
                            postFrames));
        }
    }

    public static final class Endpoint {
        public final String renderMode;
        public final int resolution;
        public final String scene;
        public final boolean preview;
        public final boolean outputDemand;
        public final boolean ndi;
        public final boolean syphon;
        public final boolean spout;

        public Endpoint(
                String renderMode,
                int resolution,
                String scene,
                boolean preview,
                boolean outputDemand,
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
            this.outputDemand = outputDemand;
            this.ndi = ndi;
            this.syphon = syphon;
            this.spout = spout;
        }

        public String description() {
            return renderMode
                    + " " + resolution
                    + " " + scene
                    + " preview=" + preview
                    + " outputDemand=" + outputDemand
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