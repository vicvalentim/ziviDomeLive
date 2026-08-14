package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulationTimelineTest {

    @Test
    void advancesWithFixedStepsAndPreservesRemainder() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setFixedStep(0.1);
        timeline.setRate(2.0);
        AtomicInteger callbacks = new AtomicInteger();

        int steps = timeline.advance(0.26, ignored -> callbacks.incrementAndGet());

        assertEquals(5, steps);
        assertEquals(5, callbacks.get());
        assertEquals(0.5, timeline.getPosition(), 1e-9);
        assertEquals(0.02, timeline.getAccumulator(), 1e-9);
    }

    @Test
    void boundsCatchUpAndRecordsDroppedSimulationUnits() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setFixedStep(0.1);
        timeline.setMaxSubSteps(3);

        assertEquals(3, timeline.advance(1.05, ignored -> {}));
        assertEquals(0.3, timeline.getPosition(), 1e-9);
        assertEquals(0.7, timeline.getDroppedUnits(), 1e-9);
        assertEquals(0.05, timeline.getAccumulator(), 1e-9);
    }

    @Test
    void pauseAndPositionChangesDoNotExecuteCallbacks() {
        SimulationTimeline timeline = new SimulationTimeline();
        timeline.setPosition(42.0);
        timeline.pause();

        assertEquals(0, timeline.advance(1.0, ignored -> {
            throw new AssertionError("callback must not run while paused");
        }));
        assertEquals(42.0, timeline.getPosition());
        timeline.jump(3.0);
        assertEquals(45.0, timeline.getPosition());
    }

    @Test
    void rejectsInvalidConfiguration() {
        SimulationTimeline timeline = new SimulationTimeline();
        assertThrows(IllegalArgumentException.class, () -> timeline.setRate(-1));
        assertThrows(IllegalArgumentException.class, () -> timeline.setFixedStep(0));
        assertThrows(IllegalArgumentException.class, () -> timeline.setMaxSubSteps(0));
        assertThrows(IllegalArgumentException.class,
                () -> timeline.advance(Double.NaN, ignored -> {}));
    }
}
