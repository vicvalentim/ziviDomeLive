import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BenchmarkSuitePlanTest {
    private static final int[] RESOLUTIONS = {4096, 1024, 2048, 3072, 2048};

    @Test
    void modesSuitePreservesTheFourRenderDomains() {
        List<BenchmarkSuitePlan.Scenario> scenarios = create(BenchmarkSuitePlan.Suite.MODES);

        assertEquals(4, scenarios.size());
        assertEquals("STANDARD", scenarios.get(0).initial.renderMode);
        assertEquals("DOMEMASTER", scenarios.get(1).initial.renderMode);
        assertEquals("EQUIRECTANGULAR", scenarios.get(2).initial.renderMode);
        assertEquals("SKYBOX", scenarios.get(3).initial.renderMode);
        assertTrue(scenarios.stream().allMatch(
                scenario -> scenario.kind == BenchmarkSuitePlan.Kind.STEADY_STATE));
    }

    @Test
    void matrixAvoidsMeaninglessStandardCubemapResolutionDuplicates() {
        List<BenchmarkSuitePlan.Scenario> scenarios = create(BenchmarkSuitePlan.Suite.MATRIX);

        assertEquals(13, scenarios.size());
        assertEquals(1, scenarios.stream().filter(
                scenario -> scenario.initial.renderMode.equals("STANDARD")).count());
        assertEquals(4, scenarios.stream().filter(
                scenario -> scenario.initial.renderMode.equals("DOMEMASTER")).count());
        assertEquals("DOMEMASTER_1024", scenarios.get(1).name);
        assertEquals("SKYBOX_4096", scenarios.get(12).name);
    }

    @Test
    void transitionPlanIncludesResourceModePreviewSceneAndNdiChanges() {
        List<BenchmarkSuitePlan.Scenario> scenarios = create(BenchmarkSuitePlan.Suite.TRANSITIONS);

        assertEquals(5, scenarios.size());
        assertEquals("RESOLUTION_2048_TO_4096", scenarios.get(0).name);
        assertEquals(120, scenarios.get(0).baselineFrames);
        assertEquals(240, scenarios.get(0).postFrames);
        assertFalse(scenarios.get(4).initial.ndi);
        assertTrue(scenarios.get(4).target.ndi);
        assertTrue(scenarios.stream().allMatch(
                scenario -> scenario.kind == BenchmarkSuitePlan.Kind.TRANSITION));
    }

    @Test
    void allSuiteIsDeterministicAndRejectsUnknownNames() {
        assertEquals(18, create(BenchmarkSuitePlan.Suite.ALL).size());
        assertEquals(BenchmarkSuitePlan.Suite.ALL, BenchmarkSuitePlan.Suite.parse(" all "));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkSuitePlan.Suite.parse("gpu"));
        assertThrows(IllegalArgumentException.class, () -> BenchmarkSuitePlan.create(
                BenchmarkSuitePlan.Suite.ALL, "MEDIUM", 2048, new int[0], true, 120, 240));
    }

    private List<BenchmarkSuitePlan.Scenario> create(BenchmarkSuitePlan.Suite suite) {
        return BenchmarkSuitePlan.create(suite, "MEDIUM", 2048, RESOLUTIONS, true, 120, 240);
    }
}
