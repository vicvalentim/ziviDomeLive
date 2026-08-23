package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.support.LogManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Activation-scoped boundary for optional MIDI, OSC, or device adapters.
 *
 * <p>The core library knows only application-defined message values. Input is bounded and
 * delivered on the Processing thread; output transport and backpressure remain adapter-owned.</p>
 */
public final class ScenePorts {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int DEFAULT_INPUT_CAPACITY = 256;
    private static final int DEFAULT_MAX_EVENTS_PER_FRAME = 32;

    private final RenderThreadQueue renderQueue;
    private final int inputCapacity;
    private final int maxEventsPerFrame;
    private final Deque<Runnable> pendingInput = new ArrayDeque<>();
    private final List<AutoCloseable> adapters = new ArrayList<>();
    private final Map<Object, Boolean> connected = new IdentityHashMap<>();
    private long droppedInputCount;
    private boolean accepting = true;
    private boolean paused;
    private boolean closed;

    ScenePorts(RenderThreadQueue renderQueue) {
        this(renderQueue, DEFAULT_INPUT_CAPACITY, DEFAULT_MAX_EVENTS_PER_FRAME);
    }

    ScenePorts(RenderThreadQueue renderQueue, int inputCapacity) {
        this(renderQueue, inputCapacity, DEFAULT_MAX_EVENTS_PER_FRAME);
    }

    ScenePorts(RenderThreadQueue renderQueue, int inputCapacity, int maxEventsPerFrame) {
        this.renderQueue = Objects.requireNonNull(renderQueue, "renderQueue");
        if (inputCapacity < 1) {
            throw new IllegalArgumentException("Input capacity must be positive.");
        }
        if (maxEventsPerFrame < 1) {
            throw new IllegalArgumentException("Per-frame input budget must be positive.");
        }
        this.inputCapacity = inputCapacity;
        this.maxEventsPerFrame = maxEventsPerFrame;
    }

    /**
     * Connects one external source to a handler owned by this scene activation.
     *
     * <p>When the bounded queue is full, the oldest pending message is discarded so the
     * activation can continue receiving current device state.</p>
     *
     * @param port external input adapter owned by this activation after connection
     * @param handler scene handler invoked at a Processing frame boundary
     * @param <T> message type defined by the adapter
     */
    public <T> void connectInput(
            SceneInputPort<T> port,
            Consumer<? super T> handler) {
        Objects.requireNonNull(port, "port");
        Objects.requireNonNull(handler, "handler");
        synchronized (this) {
            ensureAccepting();
            register(port);
        }
        try {
            port.start(value -> enqueue(handler, value));
        } catch (RuntimeException | Error error) {
            unregisterFailedConnection(port);
            throw error;
        }
    }

    /**
     * Connects a non-blocking output adapter and returns an activation-guarded view of it.
     *
     * @param port external output adapter owned by this activation after connection
     * @param <T> message type defined by the adapter
     * @return activation-guarded output port
     */
    public <T> SceneOutputPort<T> connectOutput(SceneOutputPort<T> port) {
        Objects.requireNonNull(port, "port");
        synchronized (this) {
            ensureAccepting();
            register(port);
        }
        return new ManagedOutputPort<>(port);
    }

    /** @return number of oldest pending input messages discarded due to queue pressure */
    public synchronized long getDroppedInputCount() {
        return droppedInputCount;
    }

    /** @return number of input messages waiting for a future frame boundary */
    public synchronized int getPendingInputCount() {
        return pendingInput.size();
    }

    int drain() {
        renderQueue.requireRenderThread();
        int limit;
        synchronized (this) {
            if (!accepting || paused) {
                return 0;
            }
            limit = Math.min(pendingInput.size(), maxEventsPerFrame);
        }
        int handled = 0;
        while (handled < limit) {
            Runnable event;
            synchronized (this) {
                if (!accepting || paused) {
                    break;
                }
                event = pendingInput.pollFirst();
            }
            if (event == null) {
                break;
            }
            try {
                event.run();
            } catch (RuntimeException error) {
                LOGGER.log(Level.WARNING, "Scene input port handler failed", error);
            }
            handled++;
        }
        return handled;
    }

    synchronized void stopAccepting() {
        if (!accepting) {
            return;
        }
        accepting = false;
        pendingInput.clear();
    }

    synchronized void pause() {
        ensureAccepting();
        paused = true;
        pendingInput.clear();
    }

    synchronized void resume() {
        ensureAccepting();
        paused = false;
    }

    void close() {
        List<AutoCloseable> toClose;
        synchronized (this) {
            if (closed) {
                return;
            }
            stopAccepting();
            closed = true;
            toClose = new ArrayList<>(adapters);
            adapters.clear();
            connected.clear();
        }
        for (int index = toClose.size() - 1; index >= 0; index--) {
            try {
                toClose.get(index).close();
            } catch (Exception | LinkageError error) {
                LOGGER.log(Level.WARNING, "Scene port cleanup failed", error);
            }
        }
    }

    private <T> void enqueue(Consumer<? super T> handler, T value) {
        synchronized (this) {
            if (!accepting || paused) {
                return;
            }
            if (pendingInput.size() == inputCapacity) {
                pendingInput.removeFirst();
                droppedInputCount++;
            }
            pendingInput.addLast(() -> handler.accept(value));
        }
    }

    private void register(AutoCloseable adapter) {
        if (connected.containsKey(adapter)) {
            throw new IllegalArgumentException("Scene port is already connected.");
        }
        connected.put(adapter, Boolean.TRUE);
        adapters.add(adapter);
    }

    private void unregisterFailedConnection(AutoCloseable adapter) {
        synchronized (this) {
            connected.remove(adapter);
            for (int index = 0; index < adapters.size(); index++) {
                if (adapters.get(index) == adapter) {
                    adapters.remove(index);
                    break;
                }
            }
        }
        try {
            adapter.close();
        } catch (Exception | LinkageError closeError) {
            LOGGER.log(Level.WARNING, "Failed scene port could not be closed", closeError);
        }
    }

    private synchronized boolean canOffer() {
        return accepting && !paused && !closed;
    }

    private void ensureAccepting() {
        if (!accepting || closed) {
            throw new IllegalStateException("Scene ports are closed for this activation.");
        }
    }

    private final class ManagedOutputPort<T> implements SceneOutputPort<T> {
        private final SceneOutputPort<T> delegate;

        private ManagedOutputPort(SceneOutputPort<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean offer(T value) {
            return canOffer() && delegate.offer(value);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
