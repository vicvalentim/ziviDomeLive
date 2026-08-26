package com.victorvalentim.zividomelive.core.time;

import java.util.Objects;
import java.util.function.DoubleConsumer;

/**
 * Bounded fixed-step simulation timeline with caller-defined units.
 *
 * <p>A host advances the timeline during its once-per-frame update boundary. Excess whole steps
 * after a stall are discarded and recorded instead of creating an unbounded catch-up spiral.</p>
 */
public final class SimulationTimeline {

    private double position;
    private double rate = 1.0;
    private double fixedStep = 1.0 / 60.0;
    private double accumulator;
    private double droppedUnits;
    private int maxSubSteps = 8;
    private boolean paused;

    /** Creates a timeline at position zero with 1x rate and a 1/60 fixed step. */
    public SimulationTimeline() {
    }

    /**
     * Converts real seconds to bounded fixed simulation steps.
     *
     * @param realDeltaSeconds finite non-negative real frame time
     * @param stepper synchronous callback for each executed fixed step
     * @return number of steps executed
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
        for (int index = 0; index < executed; index++) {
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

    /** Clears only partial-step state and dropped-unit telemetry. */
    public synchronized void resetAccumulator() {
        accumulator = 0.0;
        droppedUnits = 0.0;
    }

    /** Resets position, accumulator, telemetry, and pause state without changing configuration. */
    public synchronized void reset() {
        position = 0.0;
        accumulator = 0.0;
        droppedUnits = 0.0;
        paused = false;
    }

    /** Sets a finite logical position without executing callbacks and clears accumulator state. */
    public synchronized void setPosition(double position) {
        if (!Double.isFinite(position)) {
            throw new IllegalArgumentException("Timeline position must be finite.");
        }
        this.position = position;
        resetAccumulator();
    }

    /** Adds finite units to the logical position without executing callbacks. */
    public synchronized void jump(double units) {
        if (!Double.isFinite(units)) {
            throw new IllegalArgumentException("Timeline jump must be finite.");
        }
        position += units;
    }

    /** Sets finite non-negative simulation units advanced per real second. */
    public synchronized void setRate(double rate) {
        requireFiniteNonNegative(rate, "Timeline rate");
        this.rate = rate;
    }

    /** Sets a finite positive fixed simulation step and bounds the retained remainder. */
    public synchronized void setFixedStep(double fixedStep) {
        if (!Double.isFinite(fixedStep) || fixedStep <= 0.0) {
            throw new IllegalArgumentException("Fixed step must be finite and positive.");
        }
        this.fixedStep = fixedStep;
        accumulator = Math.min(accumulator, Math.nextDown(fixedStep));
    }

    /** Sets the positive per-frame fixed-step callback budget. */
    public synchronized void setMaxSubSteps(int maxSubSteps) {
        if (maxSubSteps < 1) {
            throw new IllegalArgumentException("Maximum substeps must be at least one.");
        }
        this.maxSubSteps = maxSubSteps;
    }

    /** Pauses execution without changing position or accumulated remainder. */
    public synchronized void pause() {
        paused = true;
    }

    /** Resumes fixed-step execution. */
    public synchronized void resume() {
        paused = false;
    }

    public synchronized boolean isPaused() {
        return paused;
    }

    public synchronized double getPosition() {
        return position;
    }

    public synchronized double getRate() {
        return rate;
    }

    public synchronized double getFixedStep() {
        return fixedStep;
    }

    public synchronized int getMaxSubSteps() {
        return maxSubSteps;
    }

    public synchronized double getAccumulator() {
        return accumulator;
    }

    public synchronized double getDroppedUnits() {
        return droppedUnits;
    }

    private static void requireFiniteNonNegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(label + " must be finite and non-negative.");
        }
    }
}
