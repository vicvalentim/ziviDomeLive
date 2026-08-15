package com.victorvalentim.zividomelive.benchmark.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Discovers and validates versioned BenchmarkTool runs without following symbolic links. */
public final class BenchmarkRunRepository {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;
    public static final String FRAMES_HEADER = "frame,totalMs,sceneMs,standardMs,cubemapMs,projectionMs,"
            + "previewMs,outputMs,ndiMs,standardCalls,cubemapCalls,domemasterCalls,"
            + "equirectangularCalls,skyboxCalls";

    public Result discover(Path resultsRoot) {
        Path root = resultsRoot.toAbsolutePath().normalize();
        List<BenchmarkRun> runs = new ArrayList<>();
        List<SuiteManifest> suites = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            issues.add(new Issue(root.toString(), "Results directory does not exist"));
            return new Result(root, runs, suites, issues);
        }
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            issues.add(new Issue(root.toString(), "Results path is not a directory"));
            return new Result(root, runs, suites, issues);
        }

        List<Path> candidates;
        try (Stream<Path> children = Files.list(root)) {
            candidates = children.sorted().toList();
        } catch (IOException exception) {
            issues.add(new Issue(root.toString(), "Cannot list results: " + exception.getMessage()));
            return new Result(root, runs, suites, issues);
        }
        for (Path candidate : candidates) {
            if (Files.isSymbolicLink(candidate)) {
                issues.add(new Issue(candidate.getFileName().toString(), "Symbolic-link run ignored"));
            } else if (Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    runs.add(load(candidate));
                } catch (IOException | IllegalArgumentException exception) {
                    issues.add(new Issue(candidate.getFileName().toString(), exception.getMessage()));
                }
            } else if (Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)
                    && candidate.getFileName().toString().startsWith("suite-")
                    && candidate.getFileName().toString().endsWith(".json")) {
                try {
                    suites.add(loadSuite(candidate));
                } catch (IOException | IllegalArgumentException exception) {
                    issues.add(new Issue(candidate.getFileName().toString(), exception.getMessage()));
                }
            }
        }
        runs.sort(Comparator.comparing((BenchmarkRun run) -> run.text("timestamp")).thenComparing(BenchmarkRun::id));
        suites.sort(Comparator.comparing(SuiteManifest::startedAt).thenComparing(SuiteManifest::id));
        return new Result(root, runs, suites, issues);
    }

    private SuiteManifest loadSuite(Path path) throws IOException {
        String name = path.getFileName().toString();
        Map<String, Object> document = document(path, name);
        requireSchema(document, name);
        requireText(document, "library");
        String suite = requireText(document, "suite");
        String revision = requireText(document, "revision");
        String startedAt = requireInstant(document, "startedAt", name);
        String completedAt = requireInstant(document, "completedAt", name);
        Object scenariosValue = document.get("scenarios");
        if (!(scenariosValue instanceof List)) {
            throw new IllegalArgumentException(name + " is missing scenarios array");
        }
        List<SuiteEntry> entries = new ArrayList<>();
        for (Object value : (List<?>) scenariosValue) {
            Map<String, Object> entry = requireMapValue(value, "suite scenario");
            String status = requireText(entry, "status");
            if (!List.of("SUPPORTED", "UNSUPPORTED", "FAILED", "NOT_TESTED", "STOPPED").contains(status)) {
                throw new IllegalArgumentException(name + " has invalid scenario status " + status);
            }
            entries.add(new SuiteEntry(
                    requireText(entry, "name"),
                    requireText(entry, "testType"),
                    status,
                    optionalText(entry, "reason"),
                    optionalText(entry, "resultDirectory")));
        }
        return new SuiteManifest(name, suite, revision, startedAt, completedAt, List.copyOf(entries));
    }

    private BenchmarkRun load(Path directory) throws IOException {
        Map<String, Object> summary = document(directory.resolve("summary.json"), "summary.json");
        Map<String, Object> environment = document(directory.resolve("environment.json"), "environment.json");
        requireSchema(summary, "summary.json");
        requireSchema(environment, "environment.json");
        String library = requireText(summary, "library");
        String version = requireText(summary, "version");
        String revision = requireText(summary, "revision");
        String timestamp = requireText(summary, "timestamp");
        try {
            Instant.parse(timestamp);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("summary.json has invalid timestamp");
        }
        requireMap(environment, "environment");
        requireEqual(library, requireText(environment, "library"), "library");
        requireEqual(version, requireText(environment, "version"), "version");
        requireEqual(revision, requireText(environment, "revision"), "revision");
        requireEqual(timestamp, requireText(environment, "timestamp"), "timestamp");
        Map<String, Object> scenario = requireMap(summary, "scenario");
        Map<String, Object> metrics = requireMap(summary, "metrics");
        Map<String, Object> pipeline = requireMap(summary, "pipeline");
        requireMap(summary, "environment");
        requireText(scenario, "renderMode");
        requireText(scenario, "scene");
        requireInteger(scenario, "resolution");
        int frames = requireInteger(metrics, "frames");
        for (String key : List.of(
                "fpsAverage", "onePercentLowFps", "frameMsAverage", "frameMsP50",
                "frameMsP95", "frameMsP99", "frameMsMax")) {
            requireNumber(metrics, key, false);
        }
        for (Map.Entry<String, Object> entry : pipeline.entrySet()) {
            Map<String, Object> metric = requireMapValue(entry.getValue(), "pipeline." + entry.getKey());
            for (String key : List.of("averageMs", "p95Ms", "p99Ms", "callsPerFrame", "totalCalls")) {
                requireNumber(metric, key, false);
            }
        }
        if (summary.containsKey("transition")) {
            Map<String, Object> transition = requireMap(summary, "transition");
            requireText(transition, "from");
            requireText(transition, "to");
            requireNumber(transition, "transitionFrame", false);
            requireNumber(transition, "baselineFrames", false);
            requireNumber(transition, "postFrames", false);
            requireNumber(transition, "normalP95Ms", false);
            requireNumber(transition, "transitionMaxMs", false);
            requireNumber(transition, "recoveryFrames", true);
        }
        double[] frameTimes = frames(directory.resolve("frames.csv"), frames);
        return new BenchmarkRun(directory, summary, environment, frameTimes);
    }

    private Map<String, Object> document(Path path, String name) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("Missing regular " + name);
        }
        try {
            return requireMapValue(SimpleJson.parse(Files.readString(path, StandardCharsets.UTF_8)), name);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + name + ": " + exception.getMessage());
        }
    }

    private double[] frames(Path path, int expectedFrames) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw new IllegalArgumentException("Missing regular frames.csv");
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !FRAMES_HEADER.equals(lines.get(0))) {
            throw new IllegalArgumentException("frames.csv has unsupported header");
        }
        List<Double> totals = new ArrayList<>();
        for (int line = 1; line < lines.size(); line++) {
            if (lines.get(line).isBlank()) continue;
            String[] columns = lines.get(line).split(",", -1);
            if (columns.length != 14) {
                throw new IllegalArgumentException("frames.csv row " + line + " has " + columns.length + " columns");
            }
            try {
                int index = Integer.parseInt(columns[0]);
                if (index != totals.size()) throw new NumberFormatException("non-sequential frame");
                for (int column = 1; column < columns.length; column++) {
                    double value = Double.parseDouble(columns[column]);
                    if (!Double.isFinite(value) || value < 0.0) throw new NumberFormatException("invalid value");
                }
                totals.add(Double.parseDouble(columns[1]));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("frames.csv row " + line + " is invalid");
            }
        }
        if (totals.size() != expectedFrames) {
            throw new IllegalArgumentException(
                    "frames.csv contains " + totals.size() + " frames; summary declares " + expectedFrames);
        }
        double[] values = new double[totals.size()];
        for (int index = 0; index < values.length; index++) values[index] = totals.get(index);
        return values;
    }

    private static void requireSchema(Map<String, Object> document, String name) {
        double schema = requireNumber(document, "schemaVersion", false);
        if (schema != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException(name + " schema " + schema + " is not supported");
        }
    }

    private static String requireText(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String) || ((String) value).isBlank()) {
            throw new IllegalArgumentException("Missing string " + key);
        }
        return (String) value;
    }

    private static String optionalText(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String)) throw new IllegalArgumentException("Missing string " + key);
        return (String) value;
    }

    private static String requireInstant(Map<String, Object> map, String key, String name) {
        String value = requireText(map, key);
        try {
            Instant.parse(value);
            return value;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(name + " has invalid " + key);
        }
    }

    private static Map<String, Object> requireMap(Map<String, Object> map, String key) {
        return requireMapValue(map.get(key), key);
    }

    private static Map<String, Object> requireMapValue(Object value, String name) {
        if (!(value instanceof Map)) throw new IllegalArgumentException("Missing object " + name);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) value;
        return result;
    }

    private static double requireNumber(Map<String, Object> map, String key, boolean negativeAllowed) {
        Object value = map.get(key);
        if (!(value instanceof Number)) throw new IllegalArgumentException("Missing number " + key);
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number) || (!negativeAllowed && number < 0.0)) {
            throw new IllegalArgumentException("Invalid number " + key);
        }
        return number;
    }

    private static int requireInteger(Map<String, Object> map, String key) {
        double number = requireNumber(map, key, false);
        if (number != Math.rint(number) || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid integer " + key);
        }
        return (int) number;
    }

    private static void requireEqual(String summary, String environment, String field) {
        if (!summary.equals(environment)) {
            throw new IllegalArgumentException("environment.json " + field + " does not match summary.json");
        }
    }

    public static final class Result {
        private final Path root;
        private final List<BenchmarkRun> runs;
        private final List<SuiteManifest> suites;
        private final List<Issue> issues;

        Result(Path root, List<BenchmarkRun> runs, List<SuiteManifest> suites, List<Issue> issues) {
            this.root = root;
            this.runs = List.copyOf(runs);
            this.suites = List.copyOf(suites);
            this.issues = List.copyOf(issues);
        }

        public Path root() { return root; }
        public List<BenchmarkRun> runs() { return runs; }
        public List<SuiteManifest> suites() { return suites; }
        public List<Issue> issues() { return issues; }

        public Optional<BenchmarkRun> find(String idOrPath) {
            if (idOrPath == null || idOrPath.isBlank()) return Optional.empty();
            Path supplied = Path.of(idOrPath);
            String id = supplied.getFileName() == null ? idOrPath : supplied.getFileName().toString();
            return runs.stream().filter(run -> run.id().equals(id)).findFirst();
        }
    }

    public record Issue(String run, String message) {
    }

    public record SuiteManifest(
            String id,
            String suite,
            String revision,
            String startedAt,
            String completedAt,
            List<SuiteEntry> scenarios) {
    }

    public record SuiteEntry(
            String name,
            String testType,
            String status,
            String reason,
            String resultDirectory) {
    }
}
