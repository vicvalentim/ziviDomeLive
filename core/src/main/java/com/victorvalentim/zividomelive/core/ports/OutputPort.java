package com.victorvalentim.zividomelive.core.ports;

/** Generic non-blocking output adapter with adapter-owned transport backpressure. */
public interface OutputPort<T> extends AutoCloseable {

    /** Offers a message promptly without blocking the frame thread. */
    boolean offer(T value);

    /** Stops promptly and makes repeated calls harmless. */
    @Override
    void close();
}
