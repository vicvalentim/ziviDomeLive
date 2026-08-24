package com.victorvalentim.zividomelive.benchmark.report;

import java.util.ArrayList;
import java.util.List;

/** Exact baseline/candidate deltas. No project-specific pass/fail threshold is applied. */
public final class BenchmarkComparison {
    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("fpsAverage", "Average FPS", Direction.HIGHER_IS_BETTER),
            new Definition("onePercentLowFps", "1% low FPS", Direction.HIGHER_IS_BETTER),
            new Definition("frameMsAverage", "Average frame time", Direction.LOWER_IS_BETTER),
            new Definition("frameMsP50", "P50 frame time", Direction.LOWER_IS_BETTER),
            new Definition("frameMsP95", "P95 frame time", Direction.LOWER_IS_BETTER),
            new Definition("frameMsP99", "P99 frame time", Direction.LOWER_IS_BETTER),
            new Definition("frameMsMax", "Maximum frame time", Direction.LOWER_IS_BETTER));

    private final BenchmarkRun baseline;
    private final BenchmarkRun candidate;
    private final List<Delta> deltas;

    public BenchmarkComparison(BenchmarkRun baseline, BenchmarkRun candidate) {
        if (baseline == null || candidate == null) throw new IllegalArgumentException("Both runs are required");
        this.baseline = baseline;
        this.candidate = candidate;
        List<Delta> values = new ArrayList<>();
        for (Definition definition : DEFINITIONS) {
            double before = baseline.metric(definition.key());
            double after = candidate.metric(definition.key());
            double absolute = after - before;
            Double percentage = before == 0.0 ? null : absolute / Math.abs(before) * 100.0;
            boolean regression = definition.direction() == Direction.LOWER_IS_BETTER
                    ? absolute > 0.0 : absolute < 0.0;
            values.add(new Delta(
                    definition.key(), definition.label(), definition.direction(),
                    before, after, absolute, percentage, regression));
        }
        this.deltas = List.copyOf(values);
    }

    public BenchmarkRun baseline() { return baseline; }
    public BenchmarkRun candidate() { return candidate; }
    public List<Delta> deltas() { return deltas; }

    public enum Direction {
        LOWER_IS_BETTER,
        HIGHER_IS_BETTER
    }

    private record Definition(String key, String label, Direction direction) {
    }

    public record Delta(
            String key,
            String label,
            Direction direction,
            double baseline,
            double candidate,
            double absolute,
            Double percentage,
            boolean regression) {
    }
}
