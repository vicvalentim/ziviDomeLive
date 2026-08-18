package com.victorvalentim.zividomelive.benchmark.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Produces a self-contained static report plus machine-readable and Markdown summaries. */
public final class BenchmarkReportGenerator {
    public static final int REPORT_SCHEMA_VERSION = 1;
    public static final String TITLE = "ziviDomeLive Performance Qualification";

    public Report generate(
            BenchmarkRunRepository.Result discovery,
            Path outputDirectory,
            String baselineId,
            String candidateId) throws IOException {
        Path output = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(output);

        List<String> notices = new ArrayList<>();
        for (BenchmarkRunRepository.Issue issue : discovery.issues()) {
            notices.add(issue.run() + ": " + issue.message());
        }
        Optional<BenchmarkRun> baseline = resolve(discovery, baselineId, "Baseline", notices);
        Optional<BenchmarkRun> candidate = resolve(discovery, candidateId, "Candidate", notices);
        Optional<BenchmarkComparison> comparison = baseline.isPresent() && candidate.isPresent()
                ? Optional.of(new BenchmarkComparison(baseline.get(), candidate.get()))
                : Optional.empty();
        BenchmarkRun selected = candidate.orElseGet(() -> discovery.runs().isEmpty()
                ? null : discovery.runs().get(discovery.runs().size() - 1));

        Map<String, Object> data = reportData(discovery, selected, comparison, notices);
        write(output.resolve("data.json"), SimpleJson.write(data));
        write(output.resolve("summary.md"), markdown(discovery, selected, comparison, notices));
        write(output.resolve("index.html"), html(discovery, selected, comparison, notices));
        return new Report(output.resolve("index.html"), discovery.runs().size(), notices.size(), comparison.isPresent());
    }

    private Optional<BenchmarkRun> resolve(
            BenchmarkRunRepository.Result discovery,
            String id,
            String label,
            List<String> notices) {
        if (id == null || id.isBlank()) return Optional.empty();
        Optional<BenchmarkRun> run = discovery.find(id);
        if (run.isEmpty()) notices.add(label + " run was not found: " + id);
        return run;
    }

    private Map<String, Object> reportData(
            BenchmarkRunRepository.Result discovery,
            BenchmarkRun selected,
            Optional<BenchmarkComparison> comparison,
            List<String> notices) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", REPORT_SCHEMA_VERSION);
        root.put("title", TITLE);
        root.put("generatedAt", Instant.now().toString());
        root.put("resultsRoot", discovery.root().toString());
        root.put("selectedRun", selected == null ? null : selected.id());
        List<Object> runs = new ArrayList<>();
        for (BenchmarkRun run : discovery.runs()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", run.id());
            item.put("summary", run.summary());
            item.put("frameTimesMs", numbers(run.frameTimes()));
            runs.add(item);
        }
        root.put("runs", runs);
        List<Object> suites = new ArrayList<>();
        for (BenchmarkRunRepository.SuiteManifest suite : discovery.suites()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", suite.id());
            item.put("suite", suite.suite());
            item.put("revision", suite.revision());
            item.put("startedAt", suite.startedAt());
            item.put("completedAt", suite.completedAt());
            List<Object> scenarios = new ArrayList<>();
            for (BenchmarkRunRepository.SuiteEntry entry : suite.scenarios()) {
                Map<String, Object> scenario = new LinkedHashMap<>();
                scenario.put("name", entry.name());
                scenario.put("testType", entry.testType());
                scenario.put("status", entry.status());
                scenario.put("reason", entry.reason());
                scenario.put("resultDirectory", entry.resultDirectory());
                scenarios.add(scenario);
            }
            item.put("scenarios", scenarios);
            suites.add(item);
        }
        root.put("suites", suites);
        root.put("notices", notices);
        comparison.ifPresent(value -> root.put("comparison", comparisonData(value)));
        return root;
    }

    private Map<String, Object> comparisonData(BenchmarkComparison comparison) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseline", comparison.baseline().id());
        result.put("candidate", comparison.candidate().id());
        List<Object> metrics = new ArrayList<>();
        for (BenchmarkComparison.Delta delta : comparison.deltas()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", delta.key());
            item.put("label", delta.label());
            item.put("direction", delta.direction().name());
            item.put("baseline", delta.baseline());
            item.put("candidate", delta.candidate());
            item.put("absoluteDelta", delta.absolute());
            item.put("percentageDelta", delta.percentage());
            item.put("regression", delta.regression());
            metrics.add(item);
        }
        result.put("metrics", metrics);
        return result;
    }

    private String html(
            BenchmarkRunRepository.Result discovery,
            BenchmarkRun selected,
            Optional<BenchmarkComparison> comparison,
            List<String> notices) {
        StringBuilder page = new StringBuilder(64_000);
        page.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>").append(TITLE).append("</title><style>")
                .append(css()).append("</style></head><body><main>")
                .append("<header><p class=\"eyebrow\">BENCHMARK REPORT · RUN SCHEMA 1–2</p><h1>")
                .append(TITLE).append("</h1><p class=\"lede\">")
                .append(discovery.runs().size()).append(" valid run(s) discovered in <code>")
                .append(escape(discovery.root().toString())).append("</code>; ")
                .append(discovery.suites().size()).append(" suite manifest(s).</p></header>");
        if (!notices.isEmpty()) {
            page.append("<section><h2>Validation notices</h2><ul class=\"notices\">");
            notices.forEach(notice -> page.append("<li>").append(escape(notice)).append("</li>"));
            page.append("</ul></section>");
        }
        suites(page, discovery.suites());
        if (selected == null) {
            page.append("<section class=\"empty\"><h2>No valid benchmark runs</h2>")
                    .append("<p>Run BenchmarkTool and export results before regenerating this report.</p></section>");
        } else {
            overview(page, selected);
            comparison.ifPresent(value -> comparison(page, value));
            pipeline(page, selected);
            charts(page, selected);
        }
        matrix(page, discovery.runs());
        page.append("<footer>Generated offline by the ziviDomeLive benchmark report tool. ")
                .append(escape(Instant.now().toString())).append("</footer></main></body></html>\n");
        return page.toString();
    }

    private void suites(StringBuilder page, List<BenchmarkRunRepository.SuiteManifest> suites) {
        if (suites.isEmpty()) return;
        page.append("<section><h2>Automated suites</h2>");
        for (BenchmarkRunRepository.SuiteManifest suite : suites) {
            page.append("<article><h3>").append(escape(suite.suite())).append("</h3><p><code>")
                    .append(escape(suite.id())).append("</code> · ")
                    .append(escape(suite.revision())).append(" · ")
                    .append(escape(suite.startedAt())).append(" → ")
                    .append(escape(suite.completedAt())).append("</p>")
                    .append("<div class=\"table-wrap\"><table><thead><tr><th>Scenario</th><th>Type</th>")
                    .append("<th>Status</th><th>Reason</th><th>Result directory</th></tr></thead><tbody>");
            for (BenchmarkRunRepository.SuiteEntry entry : suite.scenarios()) {
                String statusClass = entry.status().equals("SUPPORTED") ? "good"
                        : entry.status().equals("UNSUPPORTED") || entry.status().equals("NOT_TESTED")
                                ? "neutral" : "bad";
                page.append("<tr><td>").append(escape(entry.name())).append("</td><td>")
                        .append(escape(entry.testType())).append("</td><td><span class=\"")
                        .append(statusClass).append("\">").append(escape(entry.status()))
                        .append("</span></td><td>").append(escape(entry.reason().isBlank() ? "—" : entry.reason()))
                        .append("</td><td><code>")
                        .append(escape(entry.resultDirectory().isBlank() ? "—" : entry.resultDirectory()))
                        .append("</code></td></tr>");
            }
            page.append("</tbody></table></div></article>");
        }
        page.append("</section>");
    }

    private void overview(StringBuilder page, BenchmarkRun run) {
        Map<String, Object> scenario = run.section("scenario");
        Map<String, Object> environment = run.section("environment");
        page.append("<section><div class=\"section-head\"><h2>Summary</h2><span class=\"run-id\">")
                .append(escape(run.id())).append("</span></div><div class=\"cards\">");
        card(page, "Average FPS", format(run.metric("fpsAverage")), "fps");
        card(page, "1% low", format(run.metric("onePercentLowFps")), "fps");
        card(page, "Average frame", format(run.metric("frameMsAverage")), "ms");
        card(page, "P95 frame", format(run.metric("frameMsP95")), "ms");
        card(page, "P99 frame", format(run.metric("frameMsP99")), "ms");
        card(page, "Maximum frame", format(run.metric("frameMsMax")), "ms");
        page.append("</div><div class=\"split\"><article><h3>Scenario</h3>");
        definition(page, scenario, List.of(
                "testType", "scenarioName", "renderMode", "view", "resolution", "resolutionDomain", "scene", "preview",
                "ndi", "ndiStatus", "syphon", "syphonStatus", "spout", "spoutStatus",
                "warmupFrames", "measurementFramesRequested"));
        page.append("</article><article><h3>Environment</h3>");
        definition(page, environment, List.of(
                "os",
                "osVersion",
                "architecture",
                "java",
                "processing",
                "glVendor",
                "glRenderer",
                "glVersion",
                "glslVersion",
                "joglProfile",
                "hardwareRasterizerKnown",
                "hardwareRasterizer",
                "windowWidth",
                "windowHeight",
                "pixelDensity",
                "ndiState",
                "syphonState",
                "spoutState"));
        page.append("</article>");
        Map<String, Object> profiling = run.section("profiling");
        if (!profiling.isEmpty()) {
            page.append("<article><h3>Profiling</h3>");
            definition(page, profiling, List.of(
                    "requestedMode", "effectiveMode", "gpuTimerPolicy", "gpuTimerBackend",
                    "gpuTimerArchitecture", "gpuTimings", "gpuMetric", "gpuSamples"));
            page.append("</article>");
        }
        Map<String, Object> transition = run.section("transition");
        if (!transition.isEmpty()) {
            page.append("<article><h3>Transition</h3>");
            definition(page, transition, List.of(
                    "from", "to", "transitionFrame", "baselineFrames", "postFrames",
                    "normalP95Ms", "transitionMaxMs", "recoveryFrames"));
            page.append("</article>");
        }
        page.append("</div></section>");
    }

    private void comparison(StringBuilder page, BenchmarkComparison comparison) {
        page.append("<section><h2>Baseline comparison</h2><p><code>")
                .append(escape(comparison.baseline().id())).append("</code> → <code>")
                .append(escape(comparison.candidate().id())).append("</code></p>")
                .append("<p class=\"hint\">Direction is descriptive only; no arbitrary pass/fail threshold is applied.</p>")
                .append("<div class=\"table-wrap\"><table><thead><tr><th>Metric</th><th>Direction</th>")
                .append("<th>Baseline</th><th>Candidate</th><th>Absolute Δ</th><th>Δ %</th><th>Result</th>")
                .append("</tr></thead><tbody>");
        for (BenchmarkComparison.Delta delta : comparison.deltas()) {
            page.append("<tr><td>").append(escape(delta.label())).append("</td><td>")
                    .append(delta.direction() == BenchmarkComparison.Direction.LOWER_IS_BETTER
                            ? "lower is better" : "higher is better")
                    .append("</td><td>").append(format(delta.baseline())).append("</td><td>")
                    .append(format(delta.candidate())).append("</td><td>")
                    .append(signed(delta.absolute())).append("</td><td>")
                    .append(delta.percentage() == null ? "n/a" : signed(delta.percentage()) + "%")
                    .append("</td><td><span class=\"")
                    .append(delta.absolute() == 0.0 ? "neutral" : delta.regression() ? "bad" : "good")
                    .append("\">")
                    .append(delta.absolute() == 0.0 ? "unchanged" : delta.regression() ? "regression" : "improvement")
                    .append("</span></td></tr>");
        }
        page.append("</tbody></table></div></section>");
    }

    private void pipeline(StringBuilder page, BenchmarkRun run) {
        double frame = run.metric("frameMsAverage");
        boolean gpuColumns = !run.section("profiling").isEmpty();
        Map<String, Object> gpuPipeline = run.section("gpuPipeline");
        String gpuMetric = value(gpuPipeline.get("metric"));
        boolean gpuMeasured = number(gpuPipeline.get("samples")) > 0.0;
        page.append("<section><h2>Pipeline</h2><div class=\"table-wrap\"><table><thead><tr>")
                .append("<th>Metric</th><th>CPU average ms</th><th>CPU P95 ms</th><th>CPU P99 ms</th>");
        if (gpuColumns) {
            page.append("<th>GPU average ms</th><th>GPU P95 ms</th><th>GPU samples</th>");
        }
        page
                .append("<th>Calls/frame</th><th>Total calls</th><th>% frame</th></tr></thead><tbody>");
        for (Map.Entry<String, Object> entry : run.section("pipeline").entrySet()) {
            Map<String, Object> values = BenchmarkRun.map(entry.getValue());
            double average = number(values.get("averageMs"));
            page.append("<tr><td><code>").append(escape(entry.getKey())).append("</code></td><td>")
                    .append(format(average)).append("</td><td>").append(format(number(values.get("p95Ms"))))
                    .append("</td><td>").append(format(number(values.get("p99Ms"))))
                    .append("</td>");
            if (gpuColumns) {
                if (gpuMeasured && entry.getKey().equals(gpuMetric)) {
                    page.append("<td>").append(format(number(gpuPipeline.get("averageMs"))))
                            .append("</td><td>").append(format(number(gpuPipeline.get("p95Ms"))))
                            .append("</td><td>").append(format(number(gpuPipeline.get("samples"))))
                            .append("</td>");
                } else {
                    page.append("<td>—</td><td>—</td><td>—</td>");
                }
            }
            page.append("<td>").append(format(number(values.get("callsPerFrame"))))
                    .append("</td><td>").append(format(number(values.get("totalCalls"))))
                    .append("</td><td>").append(frame == 0.0 ? "0.000" : format(average / frame * 100.0))
                    .append("%</td></tr>");
        }
        page.append("</tbody></table></div></section>");
    }

    private void charts(StringBuilder page, BenchmarkRun run) {
        double[] frames = run.frameTimes();
        page.append("<section><h2>Frame-time analysis</h2><div class=\"charts\"><article><h3>Timeline</h3>")
                .append(lineChart(frames)).append("</article><article><h3>Distribution</h3>")
                .append(histogram(frames)).append("</article></div></section>");
    }

    private void matrix(StringBuilder page, List<BenchmarkRun> runs) {

        page.append(
                        "<section><h2>Test matrix</h2>"
                                + "<div class=\"table-wrap\"><table><thead><tr>")
                .append("<th>Run</th>")
                .append("<th>Timestamp</th>")
                .append("<th>Type</th>")
                .append("<th>Scenario</th>")
                .append("<th>Version</th>")
                .append("<th>Mode</th>")
                .append("<th>Scene</th>")
                .append("<th>Resolution</th>")
                .append("<th>Domain</th>")
                .append("<th>Preview</th>")
                .append("<th>NDI</th>")
                .append("<th>FPS</th>")
                .append("<th>P95 ms</th>")
                .append("<th>Transition max ms</th>")
                .append("<th>Recovery frames</th>")
                .append("<th>Status</th>")
                .append("</tr></thead><tbody>");

        for (BenchmarkRun run : runs) {
            Map<String, Object> scenario =
                    run.section("scenario");

            Map<String, Object> transition =
                    run.section("transition");

            page.append("<tr><td><code>")
                    .append(escape(run.id()))
                    .append("</code></td><td>")
                    .append(escape(run.text("timestamp")))
                    .append("</td><td>")
                    .append(escape(value(scenario.get("testType"))))
                    .append("</td><td>")
                    .append(escape(value(scenario.get("scenarioName"))))
                    .append("</td><td>")
                    .append(escape(run.text("version")))
                    .append("</td><td>")
                    .append(escape(value(scenario.get("renderMode"))))
                    .append("</td><td>")
                    .append(escape(value(scenario.get("scene"))))
                    .append("</td><td>")
                    .append(escape(value(scenario.get("resolution"))))
                    .append("</td><td>")
                    .append(escape(value(scenario.get("resolutionDomain"))))
                    .append("</td><td>")
                    .append(escape(value(scenario.get("preview"))))
                    .append("</td><td>")
                    .append(escape(value(scenario.get("ndi"))))
                    .append("</td><td>")
                    .append(format(run.metric("fpsAverage")))
                    .append("</td><td>")
                    .append(format(run.metric("frameMsP95")))
                    .append("</td><td>")
                    .append(escape(value(transition.get("transitionMaxMs"))))
                    .append("</td><td>")
                    .append(escape(value(transition.get("recoveryFrames"))))
                    .append("</td><td>")
                    .append(escape(run.text("status")))
                    .append("</td></tr>");
        }

        if (runs.isEmpty()) {
            page.append(
                    "<tr><td colspan=\"16\">No valid runs.</td></tr>");
        }

        page.append(
                "</tbody></table></div></section>");
    }

    private String lineChart(double[] values) {
        if (values.length == 0) return "<p>No frame samples.</p>";
        int width = 760;
        int height = 260;
        int plotHeight = 210;
        int points = Math.min(values.length, 600);
        double maximum = maximum(values);
        StringBuilder path = new StringBuilder();
        for (int point = 0; point < points; point++) {
            int source = points == 1 ? 0 : (int) Math.round(point * (values.length - 1.0) / (points - 1.0));
            double x = points == 1 ? 0.0 : point * (width - 1.0) / (points - 1.0);
            double y = plotHeight - values[source] / maximum * (plotHeight - 8.0);
            path.append(point == 0 ? "M" : " L").append(format(x)).append(' ').append(format(y));
        }
        return "<svg role=\"img\" aria-label=\"Frame time timeline\" viewBox=\"0 0 " + width + " " + height
                + "\"><line class=\"axis\" x1=\"0\" y1=\"210\" x2=\"760\" y2=\"210\"/>"
                + "<path class=\"line\" d=\"" + path + "\"/><text x=\"0\" y=\"238\">frame 0</text>"
                + "<text text-anchor=\"end\" x=\"760\" y=\"238\">frame " + (values.length - 1) + "</text>"
                + "<text x=\"4\" y=\"16\">max " + format(maximum) + " ms</text></svg>";
    }

    private String histogram(double[] values) {
        if (values.length == 0) return "<p>No frame samples.</p>";
        int buckets = Math.min(20, Math.max(1, (int) Math.ceil(Math.sqrt(values.length))));
        double maximum = maximum(values);
        int[] counts = new int[buckets];
        for (double value : values) {
            int bucket = maximum == 0.0 ? 0 : Math.min(buckets - 1, (int) (value / maximum * buckets));
            counts[bucket]++;
        }
        int maxCount = 1;
        for (int count : counts) maxCount = Math.max(maxCount, count);
        double barWidth = 740.0 / buckets;
        StringBuilder bars = new StringBuilder();
        for (int index = 0; index < buckets; index++) {
            double height = counts[index] / (double) maxCount * 190.0;
            bars.append("<rect class=\"bar\" x=\"").append(format(index * barWidth + 10))
                    .append("\" y=\"").append(format(210 - height)).append("\" width=\"")
                    .append(format(Math.max(1.0, barWidth - 3))).append("\" height=\"")
                    .append(format(height)).append("\"><title>").append(counts[index])
                    .append(" frame(s)</title></rect>");
        }
        return "<svg role=\"img\" aria-label=\"Frame time distribution\" viewBox=\"0 0 760 260\">"
                + bars + "<line class=\"axis\" x1=\"10\" y1=\"210\" x2=\"750\" y2=\"210\"/>"
                + "<text x=\"10\" y=\"238\">0 ms</text><text text-anchor=\"end\" x=\"750\" y=\"238\">"
                + format(maximum) + " ms</text></svg>";
    }

    private String markdown(
            BenchmarkRunRepository.Result discovery,
            BenchmarkRun selected,
            Optional<BenchmarkComparison> comparison,
            List<String> notices) {
        StringBuilder text = new StringBuilder("# " + TITLE + "\n\n");
        text.append("- Valid runs: ").append(discovery.runs().size()).append('\n');
        text.append("- Suite manifests: ").append(discovery.suites().size()).append('\n');
        text.append("- Results root: `").append(discovery.root()).append("`\n");
        if (selected != null) {
            Map<String, Object> profiling = selected.section("profiling");
            text.append("- Selected run: `").append(selected.id()).append("`\n")
                    .append("- Average FPS: ").append(format(selected.metric("fpsAverage"))).append("\n")
                    .append("- 1% low FPS: ").append(format(selected.metric("onePercentLowFps"))).append("\n")
                    .append("- Average/P95/P99 frame time: ")
                    .append(format(selected.metric("frameMsAverage"))).append(" / ")
                    .append(format(selected.metric("frameMsP95"))).append(" / ")
                    .append(format(selected.metric("frameMsP99"))).append(" ms\n");
            if (!profiling.isEmpty()) {
                text.append("- GPU timer policy/backend/architecture: ")
                        .append(value(profiling.get("gpuTimerPolicy"))).append(" / ")
                        .append(value(profiling.get("gpuTimerBackend"))).append(" / ")
                        .append(value(profiling.get("gpuTimerArchitecture"))).append('\n');
            }
        }
        if (!notices.isEmpty()) {
            text.append("\n## Validation notices\n\n");
            notices.forEach(notice -> text.append("- ").append(notice).append('\n'));
        }
        comparison.ifPresent(value -> {
            text.append("\n## Baseline comparison\n\n")
                    .append("Baseline: `").append(value.baseline().id()).append("`  \n")
                    .append("Candidate: `").append(value.candidate().id()).append("`\n\n")
                    .append("| Metric | Baseline | Candidate | Absolute delta | Delta % | Directional result |\n")
                    .append("|---|---:|---:|---:|---:|---|\n");
            for (BenchmarkComparison.Delta delta : value.deltas()) {
                text.append('|').append(delta.label()).append('|').append(format(delta.baseline()))
                        .append('|').append(format(delta.candidate())).append('|').append(signed(delta.absolute()))
                        .append('|').append(delta.percentage() == null ? "n/a" : signed(delta.percentage()) + "%")
                        .append('|').append(delta.absolute() == 0.0 ? "unchanged" : delta.regression() ? "regression" : "improvement")
                        .append("|\n");
            }
            text.append("\nNo arbitrary pass/fail threshold is applied.\n");
        });
        return text.toString();
    }

    private void definition(StringBuilder page, Map<String, Object> values, List<String> keys) {
        page.append("<dl>");
        for (String key : keys) {
            page.append("<dt>").append(escape(key)).append("</dt><dd>")
                    .append(escape(value(values.get(key)))).append("</dd>");
        }
        page.append("</dl>");
    }

    private void card(StringBuilder page, String label, String value, String unit) {
        page.append("<article class=\"card\"><span>").append(escape(label)).append("</span><strong>")
                .append(value).append("</strong><small>").append(unit).append("</small></article>");
    }

    private List<Double> numbers(double[] values) {
        List<Double> result = new ArrayList<>(values.length);
        for (double value : values) result.add(value);
        return result;
    }

    private double maximum(double[] values) {
        double maximum = 0.0;
        for (double value : values) maximum = Math.max(maximum, value);
        return Math.max(maximum, 0.000_001);
    }

    private double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private String value(Object value) {
        return value == null ? "unknown" : String.valueOf(value);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private String signed(double value) {
        return String.format(Locale.ROOT, "%+.3f", value);
    }

    private String escape(String value) {
        if (value == null) return "unknown";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void write(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private String css() {
        return """
                :root{color-scheme:dark;--bg:#07110f;--panel:#0d1c19;--line:#23433c;--ink:#edf8f4;--muted:#9bb8af;--accent:#55e6b2;--warn:#ffcb6b;--bad:#ff7383}*{box-sizing:border-box}body{margin:0;background:radial-gradient(circle at 15% 0,#15372f 0,transparent 32rem),var(--bg);color:var(--ink);font:15px/1.55 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace}main{max-width:1280px;margin:auto;padding:48px 24px}header{padding:40px 0 20px}.eyebrow{color:var(--accent);letter-spacing:.15em;font-size:.78rem}h1{font:700 clamp(2.2rem,6vw,5.2rem)/.95 system-ui,sans-serif;max-width:900px;margin:.2em 0}.lede,.hint,footer{color:var(--muted)}section{background:color-mix(in srgb,var(--panel) 94%,transparent);border:1px solid var(--line);border-radius:12px;padding:24px;margin:20px 0;box-shadow:0 18px 55px #0004}h2,h3{font-family:system-ui,sans-serif;margin-top:0}.section-head{display:flex;justify-content:space-between;gap:16px;align-items:baseline}.run-id,code{color:var(--accent);overflow-wrap:anywhere}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(155px,1fr));gap:12px}.card{background:#081512;border:1px solid var(--line);padding:16px;border-radius:8px}.card span,.card small{display:block;color:var(--muted)}.card strong{font:700 2rem/1.2 system-ui,sans-serif}.split,.charts{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px;margin-top:20px}.split article,.charts article{background:#081512;border-radius:8px;padding:18px}dl{display:grid;grid-template-columns:minmax(120px,1fr) 2fr;margin:0}dt,dd{border-bottom:1px solid #19332d;padding:6px 0;margin:0}dt{color:var(--muted)}.table-wrap{overflow:auto}table{border-collapse:collapse;width:100%;font-size:.86rem}th,td{text-align:left;padding:9px 11px;border-bottom:1px solid var(--line);white-space:nowrap}th{color:var(--muted);position:sticky;top:0;background:var(--panel)}.good{color:var(--accent)}.bad{color:var(--bad)}.neutral{color:var(--muted)}.notices{color:var(--warn)}svg{width:100%;height:auto;color:var(--muted);font-size:12px}.axis{stroke:var(--line)}.line{fill:none;stroke:var(--accent);stroke-width:2}.bar{fill:#2ba77e}.empty{text-align:center;padding:60px}footer{padding:30px 0}@media(max-width:760px){main{padding:24px 12px}.split,.charts{grid-template-columns:1fr}.section-head{display:block}section{padding:16px}}
                """;
    }

    public record Report(Path index, int validRuns, int notices, boolean comparison) {
    }
}
