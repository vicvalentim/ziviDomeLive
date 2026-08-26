package com.victorvalentim.zividomelive.core.ports;

import java.util.function.Consumer;

/** Generic external input adapter whose producer may run on an arbitrary thread. */
public interface InputPort<T> extends AutoCloseable {

    /** Starts promptly and publishes messages to the thread-safe activation receiver. */
    void start(Consumer<? super T> receiver);

    /** Stops promptly and makes repeated calls harmless. */
    @Override
    void close();
}
