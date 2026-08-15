import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkSuiteResultWriterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesVersionedManifestIncludingUnsupportedScenarios() throws Exception {
        BenchmarkSuiteResultWriter.Session session = new BenchmarkSuiteResultWriter.Session();
        session.startedAt = Instant.parse("2026-08-14T10:00:00Z");
        session.completedAt = Instant.parse("2026-08-14T10:05:00Z");
        session.suite = "ALL";
        session.revision = "abc123";
        BenchmarkSuiteResultWriter.Entry supported = new BenchmarkSuiteResultWriter.Entry();
        supported.name = "DOMEMASTER_2048";
        supported.testType = "STEADY_STATE";
        supported.status = "SUPPORTED";
        supported.resultDirectory = "/results/domemaster";
        session.scenarios.add(supported);
        BenchmarkSuiteResultWriter.Entry unsupported = new BenchmarkSuiteResultWriter.Entry();
        unsupported.name = "NDI_OFF_TO_ON";
        unsupported.testType = "TRANSITION";
        unsupported.status = "UNSUPPORTED";
        unsupported.reason = "NDI is unavailable";
        session.scenarios.add(unsupported);

        Path manifest = BenchmarkSuiteResultWriter.export(temporaryDirectory, session);
        String json = Files.readString(manifest);

        assertTrue(json.contains("\"schemaVersion\": 1"));
        assertTrue(json.contains("\"suite\": \"ALL\""));
        assertTrue(json.contains("\"status\": \"UNSUPPORTED\""));
        assertTrue(json.contains("NDI is unavailable"));
        try (Stream<Path> files = Files.list(temporaryDirectory)) {
            assertEquals(1, files.count());
        }
    }
}
