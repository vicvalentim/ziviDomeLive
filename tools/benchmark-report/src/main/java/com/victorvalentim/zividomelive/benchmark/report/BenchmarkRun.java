package com.victorvalentim.zividomelive.benchmark.report;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/** One validated BenchmarkTool result directory. */
public final class BenchmarkRun {
    private final Path directory;
    private final Map<String, Object> summary;
    private final Map<String, Object> environmentDocument;
    private final double[] frameTimes;

    BenchmarkRun(
            Path directory,
            Map<String, Object> summary,
            Map<String, Object> environmentDocument,
            double[] frameTimes) {
        this.directory = directory;
        this.summary = Collections.unmodifiableMap(summary);
        this.environmentDocument = Collections.unmodifiableMap(environmentDocument);
        this.frameTimes = frameTimes.clone();
    }

    public Path directory() {
        return directory;
    }

    public String id() {
        return directory.getFileName().toString();
    }

    public Map<String, Object> summary() {
        return summary;
    }

    public Map<String, Object> environmentDocument() {
        return environmentDocument;
    }

    public double[] frameTimes() {
        return frameTimes.clone();
    }

    public String text(String key) {
        Object value = summary.get(key);
        return value instanceof String ? (String) value : "unknown";
    }

    public Map<String, Object> section(String key) {
        return map(summary.get(key));
    }

    public double metric(String key) {
        Object value = section("metrics").get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    public static Map<String, Object> map(Object value) {
        if (!(value instanceof Map)) return Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) value;
        return result;
    }
}
