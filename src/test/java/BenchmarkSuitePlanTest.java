import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BenchmarkSuitePlanTest {
    private static final int[] RESOLUTIONS = {4096, 1024, 2048, 3072, 2048};

    @Test
    void modesSuitePreservesFourRenderDomainsWithoutNdiByDefault() {
        List<BenchmarkSuitePlan.Scenario> scenarios = create(BenchmarkSuitePlan.Suite.MODES, false);

        assertEquals(4, scenarios.size());
        assertEquals("STANDARD", scenarios.get(0).initial.renderMode);
        assertEquals("DOMEMASTER", scenarios.get(1).initial.renderMode);
        assertEquals("EQUIRECTANGULAR", scenarios.get(2).initial.renderMode);
        assertEquals("SKYBOX", scenarios.get(3).initial.renderMode);
        assertFalse(scenarios.get(0).initial.outputDemand);
        assertTrue(scenarios.get(1).initial.outputDemand);
        assertTrue(scenarios.get(2).initial.outputDemand);
        assertTrue(scenarios.get(3).initial.outputDemand);
        assertTrue(scenarios.stream().noneMatch(scenario -> scenario.initial.ndi));
        assertTrue(scenarios.stream().allMatch(
                scenario -> scenario.kind == BenchmarkSuitePlan.Kind.STEADY_STATE));
    }

    @Test
    void matrixUsesInternalOutputDemandWithoutNdiByDefault() {
        List<BenchmarkSuitePlan.Scenario> scenarios = create(BenchmarkSuitePlan.Suite.MATRIX, false);

        assertEquals(13, scenarios.size());
        assertEquals(1, scenarios.stream().filter(
                scenario -> scenario.initial.renderMode.equals("STANDARD")).count());
        assertEquals(4, scenarios.stream().filter(
                scenario -> scenario.initial.renderMode.equals("DOMEMASTER")).count());
        assertEquals("DOMEMASTER_1024", scenarios.get(1).name);
        assertEquals("SKYBOX_4096", scenarios.get(12).name);
        assertFalse(scenarios.get(0).initial.outputDemand);
        assertTrue(scenarios.stream().skip(1).allMatch(scenario -> scenario.initial.outputDemand));
        assertTrue(scenarios.stream().noneMatch(scenario -> scenario.initial.ndi));
    }

    @Test
    void transitionPlanKeepsResolutionOutputDemandAndExcludesNdiByDefault() {
        List<BenchmarkSuitePlan.Scenario> scenarios = create(BenchmarkSuitePlan.Suite.TRANSITIONS, false);

        assertEquals(4, scenarios.size());
        assertEquals("RESOLUTION_2048_TO_4096", scenarios.get(0).name);
        assertEquals(120, scenarios.get(0).baselineFrames);
        assertEquals(240, scenarios.get(0).postFrames);
        assertTrue(scenarios.get(0).initial.outputDemand);
        assertTrue(scenarios.get(0).target.outputDemand);
        assertTrue(scenarios.stream().noneMatch(scenario -> scenario.initial.ndi || scenario.target.ndi));
        assertTrue(scenarios.stream().allMatch(
                scenario -> scenario.kind == BenchmarkSuitePlan.Kind.TRANSITION));
    }

    @Test
    void ndiOptInLayersTransportOnSteadyStateAndAddsIsolatedTransition() {
        List<BenchmarkSuitePlan.Scenario> matrix = create(BenchmarkSuitePlan.Suite.MATRIX, true);
        assertTrue(matrix.stream().allMatch(scenario -> scenario.initial.ndi));
        assertTrue(matrix.stream().allMatch(scenario -> scenario.initial.outputDemand));

        List<BenchmarkSuitePlan.Scenario> transitions =
                create(BenchmarkSuitePlan.Suite.TRANSITIONS, true);
        assertEquals(5, transitions.size());
        BenchmarkSuitePlan.Scenario ndi = transitions.get(4);
        assertEquals("NDI_OFF_TO_ON", ndi.name);
        assertTrue(ndi.initial.outputDemand);
        assertTrue(ndi.target.outputDemand);
        assertFalse(ndi.initial.ndi);
        assertTrue(ndi.target.ndi);
    }

    @Test
    void allSuiteIsDeterministicAndRejectsUnknownNames() {
        assertEquals(17, create(BenchmarkSuitePlan.Suite.ALL, false).size());
        assertEquals(18, create(BenchmarkSuitePlan.Suite.ALL, true).size());
        assertEquals(BenchmarkSuitePlan.Suite.ALL, BenchmarkSuitePlan.Suite.parse(" all "));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkSuitePlan.Suite.parse("gpu"));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkSuitePlan.create(
                BenchmarkSuitePlan.Suite.ALL, "MEDIUM", 2048, new int[0], true, 120, 240, false));
    }

    private List<BenchmarkSuitePlan.Scenario> create(
            BenchmarkSuitePlan.Suite suite,
            boolean benchmarkNdi) {
        return BenchmarkSuitePlan.create(
                suite, "MEDIUM", 2048, RESOLUTIONS, true, 120, 240, benchmarkNdi);
    }
}
