import java.util.Arrays;

/** Computes transition evidence after measurement, never on the measured render path. */
public final class BenchmarkTransitionAnalyzer {
    private BenchmarkTransitionAnalyzer() {
    }

    public static Statistics analyze(long[] frameNanos, int transitionFrame) {
        if (frameNanos == null || frameNanos.length < 2) {
            throw new IllegalArgumentException("At least two frame samples are required.");
        }
        if (transitionFrame <= 0 || transitionFrame >= frameNanos.length) {
            throw new IllegalArgumentException("Transition frame must separate baseline and post-transition samples.");
        }
        double[] baseline = new double[transitionFrame];
        for (int index = 0; index < transitionFrame; index++) {
            baseline[index] = milliseconds(frameNanos[index]);
        }
        Arrays.sort(baseline);
        double normalP95 = percentile(baseline, 0.95);
        double transitionMaximum = 0.0;
        int recoveryFrames = -1;
        for (int index = transitionFrame; index < frameNanos.length; index++) {
            double milliseconds = milliseconds(frameNanos[index]);
            transitionMaximum = Math.max(transitionMaximum, milliseconds);
            if (recoveryFrames < 0 && milliseconds <= normalP95) {
                recoveryFrames = index - transitionFrame;
            }
        }
        return new Statistics(
                transitionFrame,
                frameNanos.length - transitionFrame,
                normalP95,
                transitionMaximum,
                recoveryFrames);
    }

    private static double percentile(double[] sorted, double percentile) {
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[Math.min(index, sorted.length - 1)];
    }

    private static double milliseconds(long nanos) {
        if (nanos < 0L) throw new IllegalArgumentException("Frame durations cannot be negative.");
        return nanos / 1_000_000.0;
    }

    public static final class Statistics {
        public final int baselineFrames;
        public final int postFrames;
        public final double normalP95Milliseconds;
        public final double transitionMaximumMilliseconds;
        public final int recoveryFrames;

        private Statistics(
                int baselineFrames,
                int postFrames,
                double normalP95Milliseconds,
                double transitionMaximumMilliseconds,
                int recoveryFrames) {
            this.baselineFrames = baselineFrames;
            this.postFrames = postFrames;
            this.normalP95Milliseconds = normalP95Milliseconds;
            this.transitionMaximumMilliseconds = transitionMaximumMilliseconds;
            this.recoveryFrames = recoveryFrames;
        }
    }
}
