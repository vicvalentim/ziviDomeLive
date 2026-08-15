import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BenchmarkTransitionAnalyzerTest {
    @Test
    void reportsNormalP95SpikeAndRecoveryFromRawFrameTimes() {
        long[] frames = milliseconds(10, 11, 12, 50, 20, 11);

        BenchmarkTransitionAnalyzer.Statistics statistics =
                BenchmarkTransitionAnalyzer.analyze(frames, 3);

        assertEquals(3, statistics.baselineFrames);
        assertEquals(3, statistics.postFrames);
        assertEquals(12.0, statistics.normalP95Milliseconds);
        assertEquals(50.0, statistics.transitionMaximumMilliseconds);
        assertEquals(2, statistics.recoveryFrames);
    }

    @Test
    void reportsMinusOneWhenPostTransitionFramesDoNotRecover() {
        BenchmarkTransitionAnalyzer.Statistics statistics =
                BenchmarkTransitionAnalyzer.analyze(milliseconds(10, 11, 30, 25), 2);

        assertEquals(-1, statistics.recoveryFrames);
        assertThrows(IllegalArgumentException.class,
                () -> BenchmarkTransitionAnalyzer.analyze(milliseconds(10, 11), 0));
    }

    private long[] milliseconds(long... values) {
        long[] nanos = new long[values.length];
        for (int index = 0; index < values.length; index++) nanos[index] = values[index] * 1_000_000L;
        return nanos;
    }
}
