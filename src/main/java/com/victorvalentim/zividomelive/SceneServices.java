package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lifecycle-aware service context for one activation of one {@link Scene}.
 *
 * <p>The facade creates this context before {@link Scene#setupScene()}, advances it around
 * {@link Scene#update()}, and closes it after {@link Scene#dispose()}. Existing scenes remain
 * source compatible; service-aware scenes receive it through {@link Scene#configure(SceneServices)}.</p>
 */
public final class SceneServices implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ziviDomeLive parent;
    private final Scene scene;
    private final FrameClock frameClock = new FrameClock();
    private final SimulationTimeline timeline = new SimulationTimeline();
    private final RenderThreadQueue renderQueue;
    private final SceneTaskGroup tasks = new SceneTaskGroup();
    private final SceneAssets assets;
    private final SceneActionMap actions = new SceneActionMap();
    private final SceneCameraService camera;
    private final SceneEnvironmentService environment;
    private final Deque<Runnable> cleanup = new ArrayDeque<>();
    private final AtomicBoolean reloadRequested = new AtomicBoolean();
    private volatile boolean preparedForDispose;
    private volatile boolean closed;

    SceneServices(ziviDomeLive parent, Scene scene, Thread renderThread) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.scene = Objects.requireNonNull(scene, "scene");
        this.renderQueue = new RenderThreadQueue(Objects.requireNonNull(renderThread, "renderThread"));
        this.assets = new SceneAssets(parent.getPApplet(), renderQueue);
        this.camera = new SceneCameraService(parent);
        this.environment = new SceneEnvironmentService(parent);
    }

    /** @return facade that owns this activation */
    public ziviDomeLive parent() {
        return parent;
    }

    /** @return Processing applet that owns the facade */
    public PApplet applet() {
        return parent.getPApplet();
    }

    /** @return scene that owns this activation */
    public Scene scene() {
        return scene;
    }

    /** @return monotonic activation-scoped frame clock */
    public FrameClock frameClock() {
        ensureOpen();
        return frameClock;
    }

    /** @return activation-scoped fixed-step timeline */
    public SimulationTimeline timeline() {
        ensureOpen();
        return timeline;
    }

    /** @return activation-scoped Processing/OpenGL thread queue */
    public RenderThreadQueue renderQueue() {
        ensureOpen();
        return renderQueue;
    }

    /** @return bounded activation-scoped task group */
    public SceneTaskGroup tasks() {
        ensureOpen();
        return tasks;
    }

    /** @return typed activation-scoped Processing asset caches */
    public SceneAssets assets() {
        ensureOpen();
        return assets;
    }

    /** @return activation-scoped named input map */
    public SceneActionMap actions() {
        ensureOpen();
        return actions;
    }

    /** @return activation-scoped wrapper around the shared scene camera */
    public SceneCameraService camera() {
        ensureOpen();
        return camera;
    }

    /** @return activation-scoped Environment configuration */
    public SceneEnvironmentService environment() {
        ensureOpen();
        return environment;
    }

    /** Defers a full dispose/setup cycle of the active scene to the next frame boundary. */
    public void requestReload() {
        ensureOpen();
        reloadRequested.set(true);
    }

    /**
     * Registers additional cleanup in last-in/first-out order.
     *
     * @param cleanupAction cleanup callback
     */
    public synchronized void onDispose(Runnable cleanupAction) {
        ensureOpen();
        cleanup.push(Objects.requireNonNull(cleanupAction, "cleanupAction"));
    }

    /** @return whether this activation context has completed cleanup */
    public boolean isClosed() {
        return closed;
    }

    void beginFrame() {
        ensureOpen();
        renderQueue.bindToCurrentThread();
        renderQueue.drain();
        frameClock.tick();
    }

    void endFrame() {
        ensureOpen();
        camera.updateTarget();
    }

    boolean consumeReloadRequest() {
        return reloadRequested.getAndSet(false);
    }

    void prepareForDispose() {
        if (closed || preparedForDispose) {
            return;
        }
        preparedForDispose = true;
        reloadRequested.set(false);
        actions.close();
        tasks.close();
        renderQueue.close();
        camera.close();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        prepareForDispose();

        while (!cleanup.isEmpty()) {
            try {
                cleanup.pop().run();
            } catch (RuntimeException error) {
                LOGGER.log(Level.WARNING, "Scene cleanup action failed", error);
            }
        }
        environment.close();
        assets.close();
        timeline.reset();
        frameClock.reset();
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scene services are closed for " + scene.getName() + ".");
        }
    }
}
