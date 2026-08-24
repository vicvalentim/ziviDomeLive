package com.victorvalentim.zividomelive.benchmark.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkReportTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void discoversAndValidatesSchemaV1Run() throws Exception {
        Path root = temporaryDirectory.resolve("results");
        writeRun(root, "baseline", 60.0, 16.0, 18.0, new double[]{15.0, 17.0});

        BenchmarkRunRepository.Result result = new BenchmarkRunRepository().discover(root);

        assertEquals(1, result.runs().size());
        assertTrue(result.issues().isEmpty());
        assertEquals("baseline", result.runs().get(0).id());
        assertEquals(60.0, result.runs().get(0).metric("fpsAverage"));
        assertEquals(2, result.runs().get(0).frameTimes().length);
    }

    @Test
    void computesDirectionalBaselineDeltasWithoutThresholds() throws Exception {
        Path root = temporaryDirectory.resolve("results");
        writeRun(root, "baseline", 50.0, 20.0, 24.0, new double[]{19.0, 21.0});
        writeRun(root, "candidate", 55.0, 18.0, 22.0, new double[]{17.0, 19.0});
        BenchmarkRunRepository.Result result = new BenchmarkRunRepository().discover(root);

        BenchmarkComparison comparison = new BenchmarkComparison(
                result.find("baseline").orElseThrow(), result.find("candidate").orElseThrow());
        BenchmarkComparison.Delta fps = delta(comparison, "fpsAverage");
        BenchmarkComparison.Delta p95 = delta(comparison, "frameMsP95");

        assertEquals(5.0, fps.absolute());
        assertEquals(10.0, fps.percentage());
        assertFalse(fps.regression());
        assertEquals(-2.0, p95.absolute());
        assertFalse(p95.regression());
        assertEquals(BenchmarkComparison.Direction.LOWER_IS_BETTER, p95.direction());
    }

    @Test
    void generatesSelfContainedHtmlJsonAndMarkdown() throws Exception {
        Path root = temporaryDirectory.resolve("results");
        writeRun(root, "baseline", 50.0, 20.0, 24.0, new double[]{19.0, 21.0});
        writeRun(root, "candidate", 55.0, 18.0, 22.0, new double[]{17.0, 19.0});
        Path output = temporaryDirectory.resolve("report");

        BenchmarkReportGenerator.Report report = new BenchmarkReportGenerator().generate(
                new BenchmarkRunRepository().discover(root), output, "baseline", "candidate");

        assertTrue(Files.isRegularFile(report.index()));
        assertTrue(Files.isRegularFile(output.resolve("data.json")));
        assertTrue(Files.isRegularFile(output.resolve("summary.md")));
        String html = Files.readString(report.index());
        assertTrue(html.contains(BenchmarkReportGenerator.TITLE));
        assertTrue(html.contains("Baseline comparison"));
        assertTrue(html.contains("Frame time timeline"));
        assertTrue(html.contains("Frame time distribution"));
        assertTrue(html.contains("Test matrix"));
        assertFalse(html.contains("<script src="));
        assertFalse(html.contains("http://"));
        assertFalse(html.contains("https://"));

        Object parsed = SimpleJson.parse(Files.readString(output.resolve("data.json")));
        Map<String, Object> data = BenchmarkRun.map(parsed);
        assertEquals(1L, data.get("schemaVersion"));
        assertNotNull(data.get("comparison"));
        assertEquals(2, ((List<?>) data.get("runs")).size());
    }

    @Test
    void ignoresCorruptRunAndReportsWhy() throws Exception {
        Path root = temporaryDirectory.resolve("results");
        writeRun(root, "valid", 60.0, 16.0, 18.0, new double[]{15.0, 17.0});
        Path corrupt = writeRun(root, "corrupt", 60.0, 16.0, 18.0, new double[]{15.0, 17.0});
        Files.writeString(corrupt.resolve("frames.csv"), "wrong,header\n", StandardCharsets.UTF_8);

        BenchmarkRunRepository.Result result = new BenchmarkRunRepository().discover(root);

        assertEquals(1, result.runs().size());
        assertEquals("valid", result.runs().get(0).id());
        assertEquals(1, result.issues().size());
        assertTrue(result.issues().get(0).message().contains("unsupported header"));
    }

    @Test
    void signalsUnsupportedSchemaAndStillBuildsEmptyReport() throws Exception {
        Path root = temporaryDirectory.resolve("results");
        Path run = writeRun(root, "future", 60.0, 16.0, 18.0, new double[]{15.0, 17.0});
        String summary = Files.readString(run.resolve("summary.json")).replace(
                "\"schemaVersion\": 1", "\"schemaVersion\": 3");
        Files.writeString(run.resolve("summary.json"), summary, StandardCharsets.UTF_8);
        BenchmarkRunRepository.Result result = new BenchmarkRunRepository().discover(root);

        BenchmarkReportGenerator.Report report = new BenchmarkReportGenerator().generate(
                result, temporaryDirectory.resolve("report"), null, null);

        assertTrue(result.runs().isEmpty());
        assertEquals(1, result.issues().size());
        assertTrue(Files.readString(report.index()).contains("No valid benchmark runs"));
        assertTrue(Files.readString(report.index()).contains("not supported"));
    }

    @Test
    void validatesAndRendersTransitionEvidence() throws Exception {
        Path root = temporaryDirectory.resolve("results");
        Path run = writeRun(root, "transition", 50.0, 20.0, 80.0, new double[]{18.0, 20.0});
        String summary = Files.readString(run.resolve("summary.json"));
        summary = summary.replace(
                "  \"metrics\": {",
                """
                  "transition": {
                    "from": "STANDARD 2048 MEDIUM", "to": "DOMEMASTER 2048 MEDIUM",
                    "transitionFrame": 1, "baselineFrames": 1, "postFrames": 1,
                    "normalP95Ms": 18.0, "transitionMaxMs": 80.0, "recoveryFrames": -1
                  },
                  "metrics": {""");
        Files.writeString(run.resolve("summary.json"), summary, StandardCharsets.UTF_8);
        BenchmarkRunRepository.Result discovery = new BenchmarkRunRepository().discover(root);

        BenchmarkReportGenerator.Report report = new BenchmarkReportGenerator().generate(
                discovery, temporaryDirectory.resolve("report"), null, null);
        String html = Files.readString(report.index());

        assertEquals(1, discovery.runs().size());
        assertTrue(discovery.issues().isEmpty());
        assertTrue(html.contains("<h3>Transition</h3>"));
        assertTrue(html.contains("transitionMaxMs"));
        assertTrue(html.contains("80.0"));
        assertTrue(html.contains("recoveryFrames"));
    }

    @Test
    void discoversSuiteManifestAndRendersUnsupportedScenario() throws Exception {
        Path root = Files.createDirectories(temporaryDirectory.resolve("results"));
        Files.writeString(root.resolve("suite-test.json"), """
                {
                  "schemaVersion": 1,
                  "library": "ziviDomeLive",
                  "suite": "TRANSITIONS",
                  "revision": "abc123",
                  "startedAt": "2026-08-14T10:00:00Z",
                  "completedAt": "2026-08-14T10:05:00Z",
                  "scenarios": [
                    {"name": "NDI_OFF_TO_ON", "testType": "TRANSITION",
                     "status": "UNSUPPORTED", "reason": "NDI is unavailable", "resultDirectory": ""}
                  ]
                }
                """, StandardCharsets.UTF_8);
        BenchmarkRunRepository.Result discovery = new BenchmarkRunRepository().discover(root);

        BenchmarkReportGenerator.Report report = new BenchmarkReportGenerator().generate(
                discovery, temporaryDirectory.resolve("report"), null, null);
        String html = Files.readString(report.index());
        Map<String, Object> data = BenchmarkRun.map(
                SimpleJson.parse(Files.readString(temporaryDirectory.resolve("report/data.json"))));

        assertEquals(1, discovery.suites().size());
        assertTrue(discovery.issues().isEmpty());
        assertTrue(html.contains("Automated suites"));
        assertTrue(html.contains("NDI_OFF_TO_ON"));
        assertTrue(html.contains("NDI is unavailable"));
        assertEquals(1, ((List<?>) data.get("suites")).size());
    }

    private BenchmarkComparison.Delta delta(BenchmarkComparison comparison, String key) {
        return comparison.deltas().stream().filter(delta -> delta.key().equals(key)).findFirst().orElseThrow();
    }

    private Path writeRun(
            Path root,
            String id,
            double fps,
            double averageMs,
            double p95Ms,
            double[] frames) throws IOException {
        Path run = Files.createDirectories(root.resolve(id));
        String summary = """
                {
                  "schemaVersion": 1,
                  "library": "ziviDomeLive",
                  "version": "2.0.0",
                  "revision": "test-revision",
                  "timestamp": "2026-08-14T12:00:00Z",
                  "status": "SUPPORTED",
                  "environment": {
                    "os": "Test OS", "osVersion": "1", "architecture": "test",
                    "java": "17", "processing": "4.5.6", "glVendor": "Test",
                    "glRenderer": "Renderer", "glVersion": "4.1", "glslVersion": "4.10",
                    "windowWidth": 1280, "windowHeight": 720, "pixelDensity": 1,
                    "ndiState": "NOT_TESTED", "syphonState": "NOT_TESTED", "spoutState": "NOT_TESTED"
                  },
                  "scenario": {
                    "renderMode": "FISHEYE", "view": "DOME", "resolution": 1024,
                    "resolutionDomain": "face", "scene": "MEDIUM", "preview": true,
                    "ndi": false, "syphon": false, "spout": false,
                    "ndiStatus": "NOT_TESTED", "syphonStatus": "NOT_TESTED", "spoutStatus": "NOT_TESTED",
                    "warmupFrames": 120, "measurementFramesRequested": 2
                  },
                  "metrics": {
                    "frames": %d, "framesCompleted": %d, "framesOverwritten": 0,
                    "fpsAverage": %s, "onePercentLowFps": %s,
                    "frameMsAverage": %s, "frameMsP50": %s, "frameMsP95": %s,
                    "frameMsP99": %s, "frameMsMax": %s,
                    "framesOver16Point67Ms": 1, "framesOver33Point33Ms": 0, "framesOver50Ms": 0,
                    "invariantViolations": 0, "cubemapCaptureViolations": 0, "unexpectedPassViolations": 0
                  },
                  "ndi": {"captured": 0, "sent": 0, "dropped": 0, "failed": 0},
                  "pipeline": {
                    "FRAME_TOTAL": {"averageMs": %s, "p95Ms": %s, "p99Ms": %s,
                      "maxMs": %s, "callsPerFrame": 1.0, "totalCalls": %d}
                  },
                  "diagnostics": []
                }
                """.formatted(
                frames.length, frames.length, fps, fps * 0.8, averageMs, averageMs,
                p95Ms, p95Ms + 1.0, p95Ms + 2.0, averageMs, p95Ms, p95Ms + 1.0,
                p95Ms + 2.0, frames.length);
        Files.writeString(run.resolve("summary.json"), summary, StandardCharsets.UTF_8);
        Files.writeString(run.resolve("environment.json"), """
                {"schemaVersion": 1, "library": "ziviDomeLive", "version": "2.0.0",
                 "revision": "test-revision", "timestamp": "2026-08-14T12:00:00Z",
                 "environment": {"os": "Test OS"}}
                """, StandardCharsets.UTF_8);
        StringBuilder csv = new StringBuilder(BenchmarkRunRepository.FRAMES_HEADER).append('\n');
        for (int index = 0; index < frames.length; index++) {
            csv.append(index).append(',').append(frames[index])
                    .append(",1,2,3,4,5,6,7,1,1,1,1,1\n");
        }
        Files.writeString(run.resolve("frames.csv"), csv, StandardCharsets.UTF_8);
        return run;
    }
}
