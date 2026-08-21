package com.victorvalentim.zividomelive;

import java.util.function.Consumer;

/**
 * Optional source of scene-control messages supplied by an external adapter.
 *
 * <p>Implementations may receive data on their own threads. The receiver supplied by
 * {@link #start(Consumer)} is thread-safe and defers scene handling to a Processing frame
 * boundary. Starting and closing an adapter must return promptly.</p>
 *
 * @param <T> message type defined by the optional adapter
 */
public interface SceneInputPort<T> extends AutoCloseable {

    /**
     * Starts publishing messages to the supplied activation receiver.
     *
     * @param receiver thread-safe receiver bound to the current scene activation
     */
    void start(Consumer<? super T> receiver);

    /** Stops the adapter and releases its external resources. */
    @Override
    void close();
}
