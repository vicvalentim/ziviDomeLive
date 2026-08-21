package com.victorvalentim.zividomelive;

/**
 * Optional non-blocking sink for scene-control messages handled by an external adapter.
 *
 * @param <T> message type defined by the optional adapter
 */
public interface SceneOutputPort<T> extends AutoCloseable {

    /**
     * Offers a message without blocking the Processing/OpenGL thread.
     *
     * @param value message to offer to the external adapter
     * @return true when the adapter accepted the message
     */
    boolean offer(T value);

    /** Stops the adapter and releases its external resources. */
    @Override
    void close();
}
