package com.victorvalentim.zividomelive.core.time;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Monotonic frame-oriented clock advanced explicitly by its host.
 *
 * <p>The first tick establishes a baseline and returns zero. Later deltas are clamped and a
 * backward clock anomaly contributes zero, so accepted elapsed time never decreases.</p>
 */
public final class FrameClock {

    private final LongSupplier nanoTime;
    private long previousNanos;
    private boolean started;
    private double deltaSeconds;
    private double elapsedSeconds;
    private double maxDeltaSeconds = 0.25;
    private long frameIndex;

    /** Creates a clock backed by {@link System#nanoTime()}. */
    public FrameClock() {
        this(System::nanoTime);
    }

    /**
     * Creates a deterministic or custom monotonic clock.
     *
     * @param nanoTime nanosecond source queried once per tick
     */
    public FrameClock(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    /**
     * Advances the clock once at a host frame boundary.
     *
     * @return accepted delta in seconds
     */
    public synchronized double tick() {
        long now = nanoTime.getAsLong();
        if (!started) {
            previousNanos = now;
            started = true;
            deltaSeconds = 0.0;
        } else {
            long nanos = Math.max(0L, now - previousNanos);
            previousNanos = now;
            deltaSeconds = Math.min(nanos / 1_000_000_000.0, maxDeltaSeconds);
            elapsedSeconds += deltaSeconds;
        }
        frameIndex++;
        return deltaSeconds;
    }

    /** Resets elapsed state and makes the next tick a zero-delta baseline. */
    public synchronized void reset() {
        previousNanos = 0L;
        started = false;
        deltaSeconds = 0.0;
        elapsedSeconds = 0.0;
        frameIndex = 0L;
    }

    /** Sets the finite positive per-frame delta clamp in seconds. */
    public synchronized void setMaxDeltaSeconds(double maxDeltaSeconds) {
        if (!Double.isFinite(maxDeltaSeconds) || maxDeltaSeconds <= 0.0) {
            throw new IllegalArgumentException("Maximum frame delta must be finite and positive.");
        }
        this.maxDeltaSeconds = maxDeltaSeconds;
    }

    /** @return delta accepted by the latest tick, in seconds */
    public synchronized double getDeltaSeconds() {
        return deltaSeconds;
    }

    /** @return sum of accepted deltas since reset, in seconds */
    public synchronized double getElapsedSeconds() {
        return elapsedSeconds;
    }

    /** @return configured per-frame delta clamp, in seconds */
    public synchronized double getMaxDeltaSeconds() {
        return maxDeltaSeconds;
    }

    /** @return one-based frame count after the first tick */
    public synchronized long getFrameIndex() {
        return frameIndex;
    }
}
