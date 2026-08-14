package com.victorvalentim.zividomelive;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Monotonic, frame-oriented clock owned by a {@link SceneServices} activation.
 *
 * <p>The runtime ticks this clock once before {@link Scene#update()}. Large stalls are
 * clamped so a paused debugger, window move, or transient hitch cannot inject an
 * unbounded simulation delta.</p>
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

    FrameClock(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    /**
     * Advances the clock and returns the clamped frame delta in seconds.
     * The first tick establishes the baseline and returns zero.
     *
     * @return clamped seconds elapsed since the previous tick
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

    /** Resets elapsed time and makes the next tick establish a new baseline. */
    public synchronized void reset() {
        previousNanos = 0L;
        started = false;
        deltaSeconds = 0.0;
        elapsedSeconds = 0.0;
        frameIndex = 0L;
    }

    /**
     * Sets the maximum delta accepted from one frame.
     *
     * @param maxDeltaSeconds finite positive clamp in seconds
     */
    public synchronized void setMaxDeltaSeconds(double maxDeltaSeconds) {
        if (!Double.isFinite(maxDeltaSeconds) || maxDeltaSeconds <= 0.0) {
            throw new IllegalArgumentException("Maximum frame delta must be finite and positive.");
        }
        this.maxDeltaSeconds = maxDeltaSeconds;
    }

    /** @return delta from the latest tick in seconds */
    public synchronized double getDeltaSeconds() {
        return deltaSeconds;
    }

    /** @return accumulated clamped elapsed time in seconds */
    public synchronized double getElapsedSeconds() {
        return elapsedSeconds;
    }

    /** @return configured per-frame delta clamp in seconds */
    public synchronized double getMaxDeltaSeconds() {
        return maxDeltaSeconds;
    }

    /** @return number of ticks since construction or the latest reset */
    public synchronized long getFrameIndex() {
        return frameIndex;
    }
}
