package com.victorvalentim.zividomelive.core.time;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrameClockTest {

    @Test
    void firstTickIsZeroAndNormalTicksAccumulate() {
        AtomicLong nanos = new AtomicLong(1_000_000_000L);
        FrameClock clock = new FrameClock(nanos::get);

        assertEquals(0.0, clock.tick());
        nanos.addAndGet(25_000_000L);
        assertEquals(0.025, clock.tick(), 1.0e-12);
        assertEquals(0.025, clock.getDeltaSeconds(), 1.0e-12);
        assertEquals(0.025, clock.getElapsedSeconds(), 1.0e-12);
        assertEquals(2L, clock.getFrameIndex());
    }

    @Test
    void largeDeltaIsClamped() {
        AtomicLong nanos = new AtomicLong();
        FrameClock clock = new FrameClock(nanos::get);
        clock.setMaxDeltaSeconds(0.1);
        clock.tick();
        nanos.addAndGet(500_000_000L);

        assertEquals(0.1, clock.tick(), 1.0e-12);
        assertEquals(0.1, clock.getElapsedSeconds(), 1.0e-12);
    }

    @Test
    void backwardClockAnomalyContributesZeroAndRebases() {
        AtomicLong nanos = new AtomicLong(100L);
        FrameClock clock = new FrameClock(nanos::get);
        clock.setMaxDeltaSeconds(2.0);
        clock.tick();
        nanos.set(50L);
        assertEquals(0.0, clock.tick());
        nanos.set(1_000_000_050L);
        assertEquals(1.0, clock.tick(), 1.0e-12);
    }

    @Test
    void resetMakesTheNextTickANewBaseline() {
        AtomicLong nanos = new AtomicLong();
        FrameClock clock = new FrameClock(nanos::get);
        clock.tick();
        nanos.addAndGet(20_000_000L);
        clock.tick();

        clock.reset();

        assertEquals(0.0, clock.getDeltaSeconds());
        assertEquals(0.0, clock.getElapsedSeconds());
        assertEquals(0L, clock.getFrameIndex());
        assertEquals(0.0, clock.tick());
        assertEquals(1L, clock.getFrameIndex());
    }

    @Test
    void maximumDeltaMustBeFiniteAndPositive() {
        FrameClock clock = new FrameClock();
        assertThrows(IllegalArgumentException.class, () -> clock.setMaxDeltaSeconds(0.0));
        assertThrows(IllegalArgumentException.class, () -> clock.setMaxDeltaSeconds(-1.0));
        assertThrows(IllegalArgumentException.class, () -> clock.setMaxDeltaSeconds(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> clock.setMaxDeltaSeconds(Double.POSITIVE_INFINITY));
    }
}
