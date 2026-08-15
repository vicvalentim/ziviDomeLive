import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Writes one suite-level manifest after all scenarios finish or are skipped. */
public final class BenchmarkSuiteResultWriter {
    public static final int SCHEMA_VERSION = 1;

    private BenchmarkSuiteResultWriter() {
    }

    public static final class Session {
        public Instant startedAt = Instant.now();
        public Instant completedAt = Instant.now();
        public String suite = "unknown";
        public String revision = "unknown";
        public final List<Entry> scenarios = new ArrayList<>();
    }

    public static final class Entry {
        public String name = "unknown";
        public String testType = "unknown";
        public String status = "unknown";
        public String reason = "";
        public String resultDirectory = "";
    }

    public static Path export(Path outputRoot, Session session) throws IOException {
        if (outputRoot == null || session == null || session.startedAt == null || session.completedAt == null) {
            throw new IllegalArgumentException("Output root and complete suite session are required.");
        }
        Path root = outputRoot.toAbsolutePath().normalize();
        Files.createDirectories(root);
        String timestamp = session.startedAt.toString().replace(':', '-').replace('.', '-');
        Path destination = root.resolve("suite-" + timestamp + ".json");
        int suffix = 2;
        while (Files.exists(destination)) {
            destination = root.resolve("suite-" + timestamp + "-" + suffix++ + ".json");
        }
        Files.writeString(
                destination,
                json(session),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        return destination;
    }

    private static String json(Session session) {
        StringBuilder output = new StringBuilder(4096);
        output.append("{\n")
                .append("  \"schemaVersion\": ").append(SCHEMA_VERSION).append(",\n")
                .append("  \"library\": \"ziviDomeLive\",\n")
                .append("  \"suite\": \"").append(escape(session.suite)).append("\",\n")
                .append("  \"revision\": \"").append(escape(session.revision)).append("\",\n")
                .append("  \"startedAt\": \"").append(session.startedAt).append("\",\n")
                .append("  \"completedAt\": \"").append(session.completedAt).append("\",\n")
                .append("  \"scenarios\": [\n");
        for (int index = 0; index < session.scenarios.size(); index++) {
            Entry entry = session.scenarios.get(index);
            output.append("    {\"name\": \"").append(escape(entry.name))
                    .append("\", \"testType\": \"").append(escape(entry.testType))
                    .append("\", \"status\": \"").append(escape(entry.status))
                    .append("\", \"reason\": \"").append(escape(entry.reason))
                    .append("\", \"resultDirectory\": \"").append(escape(entry.resultDirectory))
                    .append("\"}")
                    .append(index + 1 < session.scenarios.size() ? ",\n" : "\n");
        }
        return output.append("  ]\n}\n").toString();
    }

    private static String escape(String value) {
        String text = value == null ? "unknown" : value;
        StringBuilder escaped = new StringBuilder(text.length() + 16);
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '"': escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default: escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
