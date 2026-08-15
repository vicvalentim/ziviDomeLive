package com.victorvalentim.zividomelive.benchmark.report;

import java.nio.file.Path;

/** Command-line entrypoint used by the Gradle benchmarkReport task. */
public final class BenchmarkReportMain {
    private BenchmarkReportMain() {
    }

    public static void main(String[] arguments) throws Exception {
        Arguments options = Arguments.parse(arguments);
        BenchmarkRunRepository.Result discovery = new BenchmarkRunRepository().discover(options.results);
        BenchmarkReportGenerator.Report report = new BenchmarkReportGenerator().generate(
                discovery, options.output, options.baseline, options.candidate);
        System.out.printf(
                "Benchmark report: %s (%d valid run(s), %d notice(s), comparison=%s)%n",
                report.index(), report.validRuns(), report.notices(), report.comparison());
    }

    private static final class Arguments {
        private Path results = Path.of("build/benchmark-results");
        private Path output = Path.of("build/reports/benchmark");
        private String baseline;
        private String candidate;

        private static Arguments parse(String[] arguments) {
            Arguments result = new Arguments();
            for (int index = 0; index < arguments.length; index++) {
                String option = arguments[index];
                if (index + 1 >= arguments.length) {
                    throw new IllegalArgumentException("Missing value for " + option);
                }
                String value = arguments[++index];
                switch (option) {
                    case "--results" -> result.results = Path.of(value);
                    case "--output" -> result.output = Path.of(value);
                    case "--baseline" -> result.baseline = value;
                    case "--candidate" -> result.candidate = value;
                    default -> throw new IllegalArgumentException("Unknown option: " + option);
                }
            }
            return result;
        }
    }
}
