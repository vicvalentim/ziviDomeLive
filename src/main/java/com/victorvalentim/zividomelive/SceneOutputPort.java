package com.victorvalentim.zividomelive;

/**
 * Optional non-blocking sink for scene-control messages handled by an external adapter.
 *
 * <p>If the transport can block, the adapter must own a bounded worker queue; {@link #offer(Object)}
 * itself must return promptly. After connection, {@link ScenePorts} owns adapter lifecycle.</p>
 *
 * <p><strong>API stability:</strong> Advanced Stable.</p>
 *
 * @param <T> message type defined by the optional adapter
 * @since 2.0.0
 */
public interface SceneOutputPort<T> extends AutoCloseable {

    /**
     * Offers a message without blocking the Processing/OpenGL thread.
     *
     * @param value message to offer to the external adapter
     * @return {@code true} when accepted; {@code false} when unavailable or under backpressure
     */
    boolean offer(T value);

    /**
     * Stops the adapter and releases its external resources promptly.
     * Implementations should make repeated calls harmless.
     */
    @Override
    void close();
}
