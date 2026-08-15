import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import com.victorvalentim.zividomelive.performance.PerformanceSnapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Writes one completed BenchmarkTool run without depending on Processing or a JSON library. */
public final class BenchmarkResultWriter {
    public static final int SCHEMA_VERSION = 2;

    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    private BenchmarkResultWriter() {
    }

    /** Mutable run metadata assembled outside the measured interval by the Processing sketch. */
    public static final class Run {
        public Instant timestamp = Instant.now();
        public String libraryVersion = "unknown";
        public String revision = "unknown";
        public String status = "SUPPORTED";
        public String testType = "STEADY_STATE";
        public String scenarioName = "MANUAL";
        public String renderMode = "unknown";
        public String view = "unknown";
        public int resolution;
        public String resolutionDomain = "unknown";
        public String scene = "unknown";
        public boolean preview;
        public boolean ndiRequested;
        public boolean syphonRequested;
        public boolean spoutRequested;
        public String ndiStatus = "NOT_TESTED";
        public String syphonStatus = "NOT_TESTED";
        public String spoutStatus = "NOT_TESTED";
        public int warmupFrames;
        public int requestedMeasurementFrames;
        public long ndiCaptured;
        public long ndiSent;
        public long ndiDropped;
        public long ndiFailed;
        public Transition transition;
        public final Environment environment = new Environment();
    }

    /** Optional evidence attached to a transition run after its samples are complete. */
    public static final class Transition {
        public String from = "unknown";
        public String to = "unknown";
        public int transitionFrame;
        public int baselineFrames;
        public int postFrames;
        public double normalP95Milliseconds;
        public double transitionMaximumMilliseconds;
        public int recoveryFrames = -1;
    }

    /** Environment values captured by the sketch after its OpenGL context is initialized. */
    public static final class Environment {
        public String os = "unknown";
        public String osVersion = "unknown";
        public String architecture = "unknown";
        public String javaVersion = "unknown";
        public String processingVersion = "unknown";
        public String glVendor = "unknown";
        public String glRenderer = "unknown";
        public String glVersion = "unknown";
        public String glslVersion = "unknown";
        public int windowWidth;
        public int windowHeight;
        public int pixelDensity;
        public String ndiState = "NOT_TESTED";
        public String syphonState = "NOT_TESTED";
        public String spoutState = "NOT_TESTED";
    }

    /**
     * Writes summary.json, frames.csv, and environment.json into a unique run directory.
     *
     * @param outputRoot configured benchmark-results directory
     * @param run metadata captured outside the measurement interval
     * @param snapshot immutable completed performance snapshot
     * @return created run directory
     * @throws IOException when results cannot be written
     */
    public static Path export(Path outputRoot, Run run, PerformanceSnapshot snapshot)
            throws IOException {
        if (outputRoot == null || run == null || snapshot == null) {
            throw new IllegalArgumentException("Output root, run, and snapshot are required.");
        }
        if (run.timestamp == null) {
            throw new IllegalArgumentException("Run timestamp is required.");
        }

        Path root = outputRoot.toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path runDirectory = createUniqueRunDirectory(root, run);
        write(runDirectory.resolve("summary.json"), summaryJson(run, snapshot));
        write(runDirectory.resolve("frames.csv"), framesCsv(snapshot));
        write(runDirectory.resolve("environment.json"), environmentJson(run));
        return runDirectory;
    }

    private static Path createUniqueRunDirectory(Path root, Run run) throws IOException {
        String timestamp = run.timestamp.toString()
                .replace(':', '-')
                .replace('.', '-');
        String baseName = timestamp + "-" + slug(run.renderMode)
                + "-" + run.resolution + "-" + slug(run.scene);
        Path candidate = root.resolve(baseName);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = root.resolve(baseName + "-" + suffix++);
        }
        return Files.createDirectory(candidate);
    }

    private static String summaryJson(Run run, PerformanceSnapshot snapshot) {
        PerformanceSnapshot.MetricStatistics frame =
                snapshot.getStatistics(PerformanceMetric.FRAME_TOTAL);
        StringBuilder json = new StringBuilder(16_384);
        json.append("{\n");
        field(json, 1, "schemaVersion", SCHEMA_VERSION, true);
        field(json, 1, "library", "ziviDomeLive", true);
        field(json, 1, "version", run.libraryVersion, true);
        field(json, 1, "revision", run.revision, true);
        field(json, 1, "timestamp", run.timestamp.toString(), true);
        field(json, 1, "status", run.status, true);
        json.append("  \"environment\": ");
        appendEnvironment(json, run.environment, 1);
        json.append(",\n");
        json.append("  \"scenario\": {\n");
        field(json, 2, "testType", run.testType, true);
        field(json, 2, "scenarioName", run.scenarioName, true);
        field(json, 2, "renderMode", run.renderMode, true);
        field(json, 2, "view", run.view, true);
        field(json, 2, "resolution", run.resolution, true);
        field(json, 2, "resolutionDomain", run.resolutionDomain, true);
        field(json, 2, "scene", run.scene, true);
        field(json, 2, "preview", run.preview, true);
        field(json, 2, "ndi", run.ndiRequested, true);
        field(json, 2, "syphon", run.syphonRequested, true);
        field(json, 2, "spout", run.spoutRequested, true);
        field(json, 2, "ndiStatus", run.ndiStatus, true);
        field(json, 2, "syphonStatus", run.syphonStatus, true);
        field(json, 2, "spoutStatus", run.spoutStatus, true);
        field(json, 2, "warmupFrames", run.warmupFrames, true);
        field(json, 2, "measurementFramesRequested", run.requestedMeasurementFrames, false);
        json.append("  },\n");
        if (run.transition != null) {
            json.append("  \"transition\": {\n");
            field(json, 2, "from", run.transition.from, true);
            field(json, 2, "to", run.transition.to, true);
            field(json, 2, "transitionFrame", run.transition.transitionFrame, true);
            field(json, 2, "baselineFrames", run.transition.baselineFrames, true);
            field(json, 2, "postFrames", run.transition.postFrames, true);
            numberField(json, 2, "normalP95Ms", run.transition.normalP95Milliseconds, true);
            numberField(json, 2, "transitionMaxMs", run.transition.transitionMaximumMilliseconds, true);
            field(json, 2, "recoveryFrames", run.transition.recoveryFrames, false);
            json.append("  },\n");
        }
        json.append("  \"profiling\": {\n");
        field(json, 2, "requestedMode", snapshot.getRequestedMode().name(), true);
        field(json, 2, "effectiveMode", snapshot.getEffectiveMode().name(), true);
        field(json, 2, "gpuTimerPolicy", snapshot.getGpuTimerPolicy().name(), true);
        field(json, 2, "gpuTimerBackend", snapshot.getGpuTimerBackend().name(), true);
        field(json, 2, "gpuTimerArchitecture", snapshot.getGpuTimerArchitecture().name(), true);
        field(json, 2, "gpuTimings", snapshot.hasGpuTimings(), true);
        field(json, 2, "gpuMetric", snapshot.hasGpuTimings() ? "RENDER_PIPELINE" : "NONE", true);
        field(json, 2, "gpuSamples",
                snapshot.getGpuStatistics(PerformanceMetric.RENDER_PIPELINE).getSampledFrames(), false);
        json.append("  },\n");
        PerformanceSnapshot.MetricStatistics gpuPipeline =
                snapshot.getGpuStatistics(PerformanceMetric.RENDER_PIPELINE);
        json.append("  \"gpuPipeline\": {\n");
        field(json, 2, "metric", "RENDER_PIPELINE", true);
        field(json, 2, "samples", gpuPipeline.getSampledFrames(), true);
        numberField(json, 2, "averageMs", gpuPipeline.getAverageMilliseconds(), true);
        numberField(json, 2, "p95Ms", gpuPipeline.getP95Milliseconds(), true);
        numberField(json, 2, "p99Ms", gpuPipeline.getP99Milliseconds(), true);
        numberField(json, 2, "maxMs", gpuPipeline.getMaximumMilliseconds(), false);
        json.append("  },\n");
        json.append("  \"metrics\": {\n");
        field(json, 2, "frames", snapshot.getStoredFrames(), true);
        field(json, 2, "framesCompleted", snapshot.getTotalFrames(), true);
        field(json, 2, "framesOverwritten", snapshot.getOverwrittenFrames(), true);
        numberField(json, 2, "fpsAverage", frame.getAverageFps(), true);
        numberField(json, 2, "onePercentLowFps", frame.getOnePercentLowFps(), true);
        numberField(json, 2, "frameMsAverage", frame.getAverageMilliseconds(), true);
        numberField(json, 2, "frameMsP50", frame.getP50Milliseconds(), true);
        numberField(json, 2, "frameMsP95", frame.getP95Milliseconds(), true);
        numberField(json, 2, "frameMsP99", frame.getP99Milliseconds(), true);
        numberField(json, 2, "frameMsMax", frame.getMaximumMilliseconds(), true);
        field(json, 2, "framesOver16Point67Ms", frame.getFramesOver16Point67Milliseconds(), true);
        field(json, 2, "framesOver33Point33Ms", frame.getFramesOver33Point33Milliseconds(), true);
        field(json, 2, "framesOver50Ms", frame.getFramesOver50Milliseconds(), true);
        field(json, 2, "invariantViolations", snapshot.getInvariantViolations(), true);
        field(json, 2, "cubemapCaptureViolations", snapshot.getCubemapCaptureViolations(), true);
        field(json, 2, "unexpectedPassViolations", snapshot.getUnexpectedPassViolations(), false);
        json.append("  },\n");
        json.append("  \"ndi\": {\n");
        field(json, 2, "captured", run.ndiCaptured, true);
        field(json, 2, "sent", run.ndiSent, true);
        field(json, 2, "dropped", run.ndiDropped, true);
        field(json, 2, "failed", run.ndiFailed, false);
        json.append("  },\n");
        json.append("  \"pipeline\": {\n");
        PerformanceMetric[] metrics = PerformanceMetric.values();
        for (int index = 0; index < metrics.length; index++) {
            PerformanceMetric metric = metrics[index];
            PerformanceSnapshot.MetricStatistics statistics = snapshot.getStatistics(metric);
            json.append("    \"").append(metric.name()).append("\": {");
            json.append("\"averageMs\": ").append(number(statistics.getAverageMilliseconds()));
            json.append(", \"p95Ms\": ").append(number(statistics.getP95Milliseconds()));
            json.append(", \"p99Ms\": ").append(number(statistics.getP99Milliseconds()));
            json.append(", \"maxMs\": ").append(number(statistics.getMaximumMilliseconds()));
            json.append(", \"callsPerFrame\": ").append(number(statistics.getAverageCallsPerFrame()));
            json.append(", \"totalCalls\": ").append(statistics.getTotalCalls()).append('}');
            json.append(index + 1 < metrics.length ? ",\n" : "\n");
        }
        json.append("  },\n");
        json.append("  \"diagnostics\": ");
        appendStringArray(json, snapshot.getDiagnostics());
        json.append("\n}\n");
        return json.toString();
    }

    private static String environmentJson(Run run) {
        StringBuilder json = new StringBuilder(2_048);
        json.append("{\n");
        field(json, 1, "schemaVersion", SCHEMA_VERSION, true);
        field(json, 1, "library", "ziviDomeLive", true);
        field(json, 1, "version", run.libraryVersion, true);
        field(json, 1, "revision", run.revision, true);
        field(json, 1, "timestamp", run.timestamp.toString(), true);
        json.append("  \"environment\": ");
        appendEnvironment(json, run.environment, 1);
        json.append("\n}\n");
        return json.toString();
    }

    private static void appendEnvironment(
            StringBuilder json,
            Environment environment,
            int indentation) {
        json.append("{\n");
        int fieldsIndentation = indentation + 1;
        field(json, fieldsIndentation, "os", environment.os, true);
        field(json, fieldsIndentation, "osVersion", environment.osVersion, true);
        field(json, fieldsIndentation, "architecture", environment.architecture, true);
        field(json, fieldsIndentation, "java", environment.javaVersion, true);
        field(json, fieldsIndentation, "processing", environment.processingVersion, true);
        field(json, fieldsIndentation, "glVendor", environment.glVendor, true);
        field(json, fieldsIndentation, "glRenderer", environment.glRenderer, true);
        field(json, fieldsIndentation, "glVersion", environment.glVersion, true);
        field(json, fieldsIndentation, "glslVersion", environment.glslVersion, true);
        field(json, fieldsIndentation, "windowWidth", environment.windowWidth, true);
        field(json, fieldsIndentation, "windowHeight", environment.windowHeight, true);
        field(json, fieldsIndentation, "pixelDensity", environment.pixelDensity, true);
        field(json, fieldsIndentation, "ndiState", environment.ndiState, true);
        field(json, fieldsIndentation, "syphonState", environment.syphonState, true);
        field(json, fieldsIndentation, "spoutState", environment.spoutState, false);
        indent(json, indentation).append('}');
    }

    private static String framesCsv(PerformanceSnapshot snapshot) {
        StringBuilder csv = new StringBuilder(Math.max(1_024, snapshot.getStoredFrames() * 180));
        csv.append("frame,totalMs,sceneMs,standardMs,cubemapMs,projectionMs,previewMs,")
                .append("outputMs,ndiMs,standardCalls,cubemapCalls,domemasterCalls,")
                .append("equirectangularCalls,skyboxCalls,gpuPipelineMs,gpuPipelineCalls\n");
        for (int frame = 0; frame < snapshot.getStoredFrames(); frame++) {
            csv.append(frame).append(',');
            csvNumber(csv, duration(snapshot, PerformanceMetric.FRAME_TOTAL, frame));
            csvNumber(csv, duration(snapshot, PerformanceMetric.SCENE_UPDATE, frame)
                    + duration(snapshot, PerformanceMetric.SCENE_RENDER, frame));
            csvNumber(csv, duration(snapshot, PerformanceMetric.STANDARD_RENDER, frame));
            csvNumber(csv, duration(snapshot, PerformanceMetric.CUBEMAP_TOTAL, frame));
            csvNumber(csv, duration(snapshot, PerformanceMetric.DOMEMASTER, frame)
                    + duration(snapshot, PerformanceMetric.EQUIRECTANGULAR, frame)
                    + duration(snapshot, PerformanceMetric.SKYBOX, frame));
            csvNumber(csv, duration(snapshot, PerformanceMetric.PREVIEW_PIPELINE, frame));
            csvNumber(csv, duration(snapshot, PerformanceMetric.OUTPUT_PIPELINE, frame));
            csvNumber(csv, duration(snapshot, PerformanceMetric.NDI_CAPTURE, frame)
                    + duration(snapshot, PerformanceMetric.NDI_CONVERSION, frame)
                    + duration(snapshot, PerformanceMetric.NDI_QUEUE, frame)
                    + duration(snapshot, PerformanceMetric.NDI_SEND, frame));
            csv.append(snapshot.getCalls(PerformanceMetric.STANDARD_RENDER, frame)).append(',');
            csv.append(snapshot.getCalls(PerformanceMetric.CUBEMAP_TOTAL, frame)).append(',');
            csv.append(snapshot.getCalls(PerformanceMetric.DOMEMASTER, frame)).append(',');
            csv.append(snapshot.getCalls(PerformanceMetric.EQUIRECTANGULAR, frame)).append(',');
            csv.append(snapshot.getCalls(PerformanceMetric.SKYBOX, frame)).append(',');
            csvNumber(csv, gpuDuration(snapshot, PerformanceMetric.RENDER_PIPELINE, frame));
            csv.append(snapshot.getGpuCalls(PerformanceMetric.RENDER_PIPELINE, frame)).append('\n');
        }
        return csv.toString();
    }

    private static double duration(
            PerformanceSnapshot snapshot,
            PerformanceMetric metric,
            int frame) {
        return snapshot.getDurationNanos(metric, frame) / NANOS_PER_MILLISECOND;
    }

    private static double gpuDuration(
            PerformanceSnapshot snapshot,
            PerformanceMetric metric,
            int frame) {
        return snapshot.getGpuDurationNanos(metric, frame) / NANOS_PER_MILLISECOND;
    }

    private static void csvNumber(StringBuilder csv, double value) {
        csv.append(number(value)).append(',');
    }

    private static void write(Path path, String content) throws IOException {
        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }

    private static void field(
            StringBuilder json,
            int indentation,
            String name,
            String value,
            boolean comma) {
        indent(json, indentation)
                .append('"').append(escape(name)).append("\": \"")
                .append(escape(known(value))).append('"');
        json.append(comma ? ",\n" : "\n");
    }

    private static void field(
            StringBuilder json,
            int indentation,
            String name,
            long value,
            boolean comma) {
        indent(json, indentation).append('"').append(escape(name)).append("\": ").append(value);
        json.append(comma ? ",\n" : "\n");
    }

    private static void field(
            StringBuilder json,
            int indentation,
            String name,
            boolean value,
            boolean comma) {
        indent(json, indentation).append('"').append(escape(name)).append("\": ").append(value);
        json.append(comma ? ",\n" : "\n");
    }

    private static void numberField(
            StringBuilder json,
            int indentation,
            String name,
            double value,
            boolean comma) {
        indent(json, indentation).append('"').append(escape(name)).append("\": ").append(number(value));
        json.append(comma ? ",\n" : "\n");
    }

    private static StringBuilder indent(StringBuilder text, int indentation) {
        for (int index = 0; index < indentation; index++) {
            text.append("  ");
        }
        return text;
    }

    private static void appendStringArray(StringBuilder json, List<String> values) {
        json.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) json.append(", ");
            json.append('"').append(escape(values.get(index))).append('"');
        }
        json.append(']');
    }

    private static String number(double value) {
        return Double.isFinite(value) ? Double.toString(value) : "0.0";
    }

    private static String known(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value;
    }

    private static String slug(String value) {
        String normalized = known(value).toLowerCase(Locale.ROOT);
        StringBuilder slug = new StringBuilder(normalized.length());
        boolean separator = false;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            if (Character.isLetterOrDigit(character)) {
                slug.append(character);
                separator = false;
            } else if (!separator && slug.length() > 0) {
                slug.append('-');
                separator = true;
            }
        }
        while (slug.length() > 0 && slug.charAt(slug.length() - 1) == '-') {
            slug.setLength(slug.length() - 1);
        }
        return slug.length() == 0 ? "unknown" : slug.toString();
    }

    private static String escape(String value) {
        String text = value == null ? "unknown" : value;
        StringBuilder escaped = new StringBuilder(text.length() + 16);
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }
}
