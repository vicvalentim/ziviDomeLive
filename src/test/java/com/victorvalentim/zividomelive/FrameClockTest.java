package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrameClockTest {

    @Test
    void firstTickEstablishesBaselineAndLaterTicksAreClamped() {
        AtomicLong nanos = new AtomicLong(1_000_000_000L);
        FrameClock clock = new FrameClock(nanos::get);
        clock.setMaxDeltaSeconds(0.1);

        assertEquals(0.0, clock.tick());
        nanos.addAndGet(50_000_000L);
        assertEquals(0.05, clock.tick(), 1e-9);
        nanos.addAndGet(500_000_000L);
        assertEquals(0.1, clock.tick(), 1e-9);
        assertEquals(0.15, clock.getElapsedSeconds(), 1e-9);
        assertEquals(3, clock.getFrameIndex());
    }

    @Test
    void resetMakesNextTickAZeroDeltaBaseline() {
        AtomicLong nanos = new AtomicLong();
        FrameClock clock = new FrameClock(nanos::get);
        clock.tick();
        nanos.addAndGet(20_000_000L);
        clock.tick();

        clock.reset();

        assertEquals(0.0, clock.tick());
        assertEquals(1, clock.getFrameIndex());
        assertEquals(0.0, clock.getElapsedSeconds());
    }

    @Test
    void rejectsInvalidMaximumDelta() {
        FrameClock clock = new FrameClock();
        assertThrows(IllegalArgumentException.class, () -> clock.setMaxDeltaSeconds(0));
        assertThrows(IllegalArgumentException.class, () -> clock.setMaxDeltaSeconds(Double.NaN));
    }
}
