package com.victorvalentim.zividomelive.core.time;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationTimelineTest {

    @Test
    void fixedStepsHonorRateAndPreserveRemainder() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setFixedStep(0.1);
        timeline.setRate(2.0);
        List<Double> steps = new ArrayList<>();

        assertEquals(5, timeline.advance(0.26, steps::add));
        assertEquals(List.of(0.1, 0.1, 0.1, 0.1, 0.1), steps);
        assertEquals(0.5, timeline.getPosition(), 1.0e-9);
        assertEquals(0.02, timeline.getAccumulator(), 1.0e-9);
    }

    @Test
    void catchUpIsBoundedAndDroppedUnitsAccumulate() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setFixedStep(0.1);
        timeline.setMaxSubSteps(3);

        assertEquals(3, timeline.advance(1.05, ignored -> { }));
        assertEquals(0.3, timeline.getPosition(), 1.0e-9);
        assertEquals(0.7, timeline.getDroppedUnits(), 1.0e-9);
        assertEquals(0.05, timeline.getAccumulator(), 1.0e-9);
    }

    @Test
    void pauseResumeAndZeroRateDoNotReplayElapsedTime() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setFixedStep(0.1);
        timeline.pause();
        assertTrue(timeline.isPaused());
        assertEquals(0, timeline.advance(10.0, ignored -> { }));
        timeline.resume();
        assertFalse(timeline.isPaused());
        timeline.setRate(0.0);
        assertEquals(0, timeline.advance(10.0, ignored -> { }));
        assertEquals(0.0, timeline.getAccumulator());
    }

    @Test
    void positionJumpAndAccumulatorResetHaveDistinctSemantics() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setFixedStep(1.0);
        timeline.advance(0.5, ignored -> { });
        timeline.setPosition(10.0);
        assertEquals(10.0, timeline.getPosition());
        assertEquals(0.0, timeline.getAccumulator());
        timeline.jump(-2.5);
        assertEquals(7.5, timeline.getPosition());
        timeline.advance(10.0, ignored -> { });
        assertTrue(timeline.getDroppedUnits() > 0.0);
        timeline.resetAccumulator();
        assertEquals(0.0, timeline.getAccumulator());
        assertEquals(0.0, timeline.getDroppedUnits());
        assertEquals(15.5, timeline.getPosition());
    }

    @Test
    void resetPreservesConfigurationButClearsRuntimeState() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setRate(3.0);
        timeline.setFixedStep(0.25);
        timeline.setMaxSubSteps(2);
        timeline.setPosition(5.0);
        timeline.pause();

        timeline.reset();

        assertEquals(0.0, timeline.getPosition());
        assertEquals(0.0, timeline.getAccumulator());
        assertEquals(0.0, timeline.getDroppedUnits());
        assertFalse(timeline.isPaused());
        assertEquals(3.0, timeline.getRate());
        assertEquals(0.25, timeline.getFixedStep());
        assertEquals(2, timeline.getMaxSubSteps());
    }

    @Test
    void changingFixedStepBoundsAnExistingRemainder() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setFixedStep(1.0);
        timeline.advance(0.75, ignored -> { });

        timeline.setFixedStep(0.5);

        assertEquals(Math.nextDown(0.5), timeline.getAccumulator());
    }

    @Test
    void invalidInputsAreRejected() {
        SimulationTimeline timeline = new SimulationTimeline();
        assertThrows(NullPointerException.class, () -> timeline.advance(0.1, null));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.advance(Double.NaN, ignored -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.advance(-0.1, ignored -> { }));
        assertThrows(IllegalArgumentException.class, () -> timeline.setRate(-1.0));
        assertThrows(IllegalArgumentException.class, () -> timeline.setRate(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> timeline.setFixedStep(0.0));
        assertThrows(IllegalArgumentException.class, () -> timeline.setFixedStep(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> timeline.setMaxSubSteps(0));
        assertThrows(IllegalArgumentException.class, () -> timeline.setPosition(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> timeline.jump(Double.NEGATIVE_INFINITY));
    }
}
