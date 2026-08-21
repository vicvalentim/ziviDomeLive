package com.victorvalentim.zividomelive;

import java.util.Objects;
import java.util.function.DoubleConsumer;

/**
 * Bounded fixed-step simulation timeline.
 *
 * <p>The timeline converts real frame seconds into arbitrary simulation units through
 * {@code rate}. It executes at most {@code maxSubSteps} callbacks per frame and drops
 * excess whole steps after a stall, preventing an unbounded catch-up spiral.</p>
 */
public final class SimulationTimeline {

    private double position;
    private double rate = 1.0;
    private double fixedStep = 1.0 / 60.0;
    private double accumulator;
    private double droppedUnits;
    private int maxSubSteps = 8;
    private boolean paused;

    SimulationTimeline() {
    }

    /**
     * Advances this timeline using a bounded number of fixed steps.
     *
     * @param realDeltaSeconds non-negative real frame time in seconds
     * @param stepper callback invoked once for each fixed simulation step
     * @return number of fixed steps executed
     */
    public synchronized int advance(double realDeltaSeconds, DoubleConsumer stepper) {
        Objects.requireNonNull(stepper, "stepper");
        requireFiniteNonNegative(realDeltaSeconds, "Real delta");
        if (paused || realDeltaSeconds == 0.0 || rate == 0.0) {
            return 0;
        }

        accumulator += realDeltaSeconds * rate;
        int available = (int) Math.floor(accumulator / fixedStep);
        int executed = Math.min(available, maxSubSteps);
        for (int i = 0; i < executed; i++) {
            accumulator -= fixedStep;
            position += fixedStep;
            stepper.accept(fixedStep);
        }

        if (available > maxSubSteps) {
            int droppedSteps = available - maxSubSteps;
            double dropped = droppedSteps * fixedStep;
            droppedUnits += dropped;
            accumulator -= dropped;
        }
        return executed;
    }

    /** Clears only the partial-step accumulator and dropped-time telemetry. */
    public synchronized void resetAccumulator() {
        accumulator = 0.0;
        droppedUnits = 0.0;
    }

    /** Resets position, accumulator, telemetry, and pause state. */
    public synchronized void reset() {
        position = 0.0;
        accumulator = 0.0;
        droppedUnits = 0.0;
        paused = false;
    }

    /**
     * Moves the logical position without executing simulation callbacks.
     *
     * @param position finite position in the caller's simulation units
     */
    public synchronized void setPosition(double position) {
        if (!Double.isFinite(position)) {
            throw new IllegalArgumentException("Timeline position must be finite.");
        }
        this.position = position;
        resetAccumulator();
    }

    /**
     * Adds simulation units without executing callbacks.
     *
     * @param units finite amount to add to the current position
     */
    public synchronized void jump(double units) {
        if (!Double.isFinite(units)) {
            throw new IllegalArgumentException("Timeline jump must be finite.");
        }
        position += units;
    }

    /**
     * Sets simulation units advanced per real second.
     *
     * @param rate finite non-negative simulation rate
     */
    public synchronized void setRate(double rate) {
        requireFiniteNonNegative(rate, "Timeline rate");
        this.rate = rate;
    }

    /**
     * Sets the fixed simulation step size.
     *
     * @param fixedStep finite positive step in simulation units
     */
    public synchronized void setFixedStep(double fixedStep) {
        if (!Double.isFinite(fixedStep) || fixedStep <= 0.0) {
            throw new IllegalArgumentException("Fixed step must be finite and positive.");
        }
        this.fixedStep = fixedStep;
        accumulator = Math.min(accumulator, Math.nextDown(fixedStep));
    }

    /**
     * Sets the maximum number of callbacks allowed during one frame.
     *
     * @param maxSubSteps positive per-frame callback budget
     */
    public synchronized void setMaxSubSteps(int maxSubSteps) {
        if (maxSubSteps < 1) {
            throw new IllegalArgumentException("Maximum substeps must be at least one.");
        }
        this.maxSubSteps = maxSubSteps;
    }

    /** Pauses fixed-step callback execution without changing position. */
    public synchronized void pause() {
        paused = true;
    }

    /** Resumes fixed-step callback execution. */
    public synchronized void resume() {
        paused = false;
    }

    /** @return whether fixed-step execution is paused */
    public synchronized boolean isPaused() {
        return paused;
    }

    /** @return current logical position in simulation units */
    public synchronized double getPosition() {
        return position;
    }

    /** @return simulation units advanced per real second */
    public synchronized double getRate() {
        return rate;
    }

    /** @return configured fixed step in simulation units */
    public synchronized double getFixedStep() {
        return fixedStep;
    }

    /** @return maximum fixed-step callbacks allowed per frame */
    public synchronized int getMaxSubSteps() {
        return maxSubSteps;
    }

    /** @return accumulated partial step in simulation units */
    public synchronized double getAccumulator() {
        return accumulator;
    }

    /** @return simulation units dropped by bounded catch-up */
    public synchronized double getDroppedUnits() {
        return droppedUnits;
    }

    private static void requireFiniteNonNegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(label + " must be finite and non-negative.");
        }
    }
}
