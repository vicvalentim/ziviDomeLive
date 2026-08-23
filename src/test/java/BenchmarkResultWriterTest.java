import com.victorvalentim.zividomelive.performance.PerformanceMetric;
import com.victorvalentim.zividomelive.performance.PerformanceMode;
import com.victorvalentim.zividomelive.performance.PerformanceSnapshot;
import com.victorvalentim.zividomelive.PerformanceSnapshotFixture;
import com.victorvalentim.zividomelive.performance.GpuTimerArchitecture;
import com.victorvalentim.zividomelive.performance.GpuTimerBackend;
import com.victorvalentim.zividomelive.performance.GpuTimerPolicy;
import com.victorvalentim.zividomelive.benchmark.report.BenchmarkRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkResultWriterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsVersionedJsonCsvAndEnvironmentFiles() throws Exception {
        PerformanceSnapshot snapshot = snapshot();
        BenchmarkResultWriter.Run run = run();

        Path directory = BenchmarkResultWriter.export(temporaryDirectory, run, snapshot);

        String summary = Files.readString(directory.resolve("summary.json"));
        String environment = Files.readString(directory.resolve("environment.json"));
        List<String> frames = Files.readAllLines(directory.resolve("frames.csv"));
        assertTrue(summary.contains("\"schemaVersion\": 2"));
        assertTrue(summary.contains("\"revision\": \"abc123\""));
        assertTrue(summary.contains("\"frameMsP95\": 20.0"));
        assertTrue(summary.contains("\"resolutionDomain\": \"OUTPUT_BASE\""));
        assertTrue(summary.contains("\"ndiStatus\": \"NOT_TESTED\""));
        assertTrue(summary.contains("\"invariantViolations\": 0"));
        assertTrue(summary.contains("\"NDI_CAPTURE\""));
        assertTrue(summary.contains("\"testType\": \"TRANSITION\""));
        assertTrue(summary.contains("\"transitionMaxMs\": 48.0"));
        assertTrue(summary.contains("\"recoveryFrames\": 3"));
        assertTrue(summary.contains("\"effectiveMode\": \"CPU_GPU\""));
        assertTrue(summary.contains("\"gpuTimerPolicy\": \"ARCHITECTURE_AWARE\""));
        assertTrue(summary.contains("\"gpuTimerBackend\": \"TIME_ELAPSED_EXCLUSIVE\""));
        assertTrue(summary.contains("\"gpuTimerArchitecture\": \"APPLE_SILICON\""));
        assertTrue(summary.contains("\"gpuMetric\": \"RENDER_PIPELINE\""));
        assertTrue(summary.contains("\"gpuPipeline\""));
        assertTrue(summary.contains("\"averageMs\": 6.0"));
        assertTrue(environment.contains("\"glRenderer\": \"Test GPU\""));
        assertEquals(3, frames.size());
        assertTrue(frames.get(0).startsWith("frame,totalMs,sceneMs"));
        assertTrue(frames.get(1).startsWith("0,10.0,"));
        for (String row : frames) {
            assertEquals(16, row.split(",", -1).length);
        }
        assertTrue(directory.getFileName().toString().contains("domemaster-2048-medium"));

        BenchmarkRunRepository.Result discovery =
                new BenchmarkRunRepository().discover(temporaryDirectory);
        assertEquals(1, discovery.runs().size());
        assertTrue(discovery.issues().isEmpty());
    }

    @Test
    void repeatedTimestampProducesAUniqueRunDirectory() throws Exception {
        BenchmarkResultWriter.Run run = run();
        Path first = BenchmarkResultWriter.export(temporaryDirectory, run, snapshot());
        Path second = BenchmarkResultWriter.export(temporaryDirectory, run, snapshot());

        assertTrue(Files.isDirectory(first));
        assertTrue(Files.isDirectory(second));
        assertTrue(second.getFileName().toString().endsWith("-2"));
    }

    @Test
    void rejectsIncompleteExportArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> BenchmarkResultWriter.export(null, run(), snapshot()));
        BenchmarkResultWriter.Run run = run();
        run.timestamp = null;
        assertThrows(IllegalArgumentException.class,
                () -> BenchmarkResultWriter.export(temporaryDirectory, run, snapshot()));
    }

    private static BenchmarkResultWriter.Run run() {
        BenchmarkResultWriter.Run run = new BenchmarkResultWriter.Run();
        run.timestamp = Instant.parse("2026-08-14T12:34:56Z");
        run.libraryVersion = "2.0.0";
        run.revision = "abc123";
        run.renderMode = "DOMEMASTER";
        run.view = "DOMEMASTER";
        run.resolution = 2048;
        run.resolutionDomain = "OUTPUT_BASE";
        run.scene = "MEDIUM";
        run.preview = true;
        run.warmupFrames = 600;
        run.requestedMeasurementFrames = 2;
        run.testType = "TRANSITION";
        run.scenarioName = "STANDARD_TO_DOMEMASTER";
        run.transition = new BenchmarkResultWriter.Transition();
        run.transition.from = "STANDARD 2048 MEDIUM";
        run.transition.to = "DOMEMASTER 2048 MEDIUM";
        run.transition.transitionFrame = 1;
        run.transition.baselineFrames = 1;
        run.transition.postFrames = 1;
        run.transition.normalP95Milliseconds = 20.0;
        run.transition.transitionMaximumMilliseconds = 48.0;
        run.transition.recoveryFrames = 3;
        run.ndiCaptured = 2;
        run.ndiSent = 1;
        run.ndiDropped = 1;
        run.environment.os = "Test OS";
        run.environment.glRenderer = "Test GPU";
        run.environment.windowWidth = 1280;
        run.environment.windowHeight = 720;
        run.environment.pixelDensity = 1;
        return run;
    }

    private static PerformanceSnapshot snapshot() {
        int metricCount = PerformanceMetric.values().length;
        long[][] durations = new long[metricCount][2];
        int[][] calls = new int[metricCount][2];
        long[][] gpuDurations = new long[metricCount][2];
        int[][] gpuCalls = new int[metricCount][2];
        durations[PerformanceMetric.FRAME_TOTAL.ordinal()][0] = 10_000_000L;
        durations[PerformanceMetric.FRAME_TOTAL.ordinal()][1] = 20_000_000L;
        calls[PerformanceMetric.FRAME_TOTAL.ordinal()][0] = 1;
        calls[PerformanceMetric.FRAME_TOTAL.ordinal()][1] = 1;
        durations[PerformanceMetric.CUBEMAP_TOTAL.ordinal()][0] = 3_000_000L;
        calls[PerformanceMetric.CUBEMAP_TOTAL.ordinal()][0] = 1;
        gpuDurations[PerformanceMetric.RENDER_PIPELINE.ordinal()][0] = 5_000_000L;
        gpuDurations[PerformanceMetric.RENDER_PIPELINE.ordinal()][1] = 7_000_000L;
        gpuCalls[PerformanceMetric.RENDER_PIPELINE.ordinal()][0] = 1;
        gpuCalls[PerformanceMetric.RENDER_PIPELINE.ordinal()][1] = 1;
        return PerformanceSnapshotFixture.create(
                PerformanceMode.CPU_GPU,
                PerformanceMode.CPU_GPU,
                2L,
                2,
                0L,
                durations,
                calls,
                0L,
                0L,
                0L,
                List.of(),
                gpuDurations,
                gpuCalls,
                GpuTimerPolicy.ARCHITECTURE_AWARE,
                GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE,
                GpuTimerArchitecture.APPLE_SILICON);
    }
}
