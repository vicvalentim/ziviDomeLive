package com.victorvalentim.zividomelive;

import processing.core.PApplet;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lifecycle-aware service context for one activation of one {@link Scene}.
 *
 * <p>The facade creates this context before {@link Scene#setupScene()}, supplies it through
 * {@link Scene#configure(SceneServices)}, advances it around {@link Scene#update()}, and closes it
 * after {@link Scene#dispose()}. Reloading the same scene object creates a fresh context.</p>
 *
 * <p>Scenes consume these services but never construct or close them. Every accessor rejects use
 * after the activation begins disposal.</p>
 *
 * <p><strong>API stability:</strong> Advanced Stable.</p>
 *
 * @since 2.0.0
 */
public final class SceneServices {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ziviDomeLive parent;
    private final Scene scene;
    private final FrameClock frameClock = new FrameClock();
    private final SimulationTimeline timeline = new SimulationTimeline();
    private final RenderThreadQueue renderQueue;
    private final SceneTaskGroup tasks;
    private final SceneAssets assets;
    private final SceneActionMap actions = new SceneActionMap();
    private final SceneCameraService camera;
    private final SceneEnvironmentService environment;
    private final ScenePorts ports;
    private final AtomicBoolean reloadRequested = new AtomicBoolean();
    private volatile boolean preparedForDispose;
    private volatile boolean closed;

    SceneServices(ziviDomeLive parent, Scene scene, Thread renderThread) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.scene = Objects.requireNonNull(scene, "scene");
        this.renderQueue = new RenderThreadQueue(Objects.requireNonNull(renderThread, "renderThread"));
        this.tasks = new SceneTaskGroup(renderQueue);
        this.assets = new SceneAssets(parent.getPApplet(), renderQueue);
        this.camera = new SceneCameraService(parent);
        this.environment = new SceneEnvironmentService(parent);
        this.ports = new ScenePorts(renderQueue);
    }

    /** @return Processing applet that owns the facade and this activation */
    public PApplet applet() {
        ensureOpen();
        return parent.getPApplet();
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

    /** @return activation-scoped named action and input-binding map */
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

    /** @return activation-scoped boundary for optional external message adapters */
    public ScenePorts ports() {
        ensureOpen();
        return ports;
    }

    /**
     * Requests one full dispose/configure/setup cycle for the active scene.
     *
     * <p>Multiple requests before the next Processing frame boundary coalesce into one reload.</p>
     */
    public void requestReload() {
        ensureOpen();
        reloadRequested.set(true);
    }

    boolean isClosed() {
        return preparedForDispose || closed;
    }

    boolean beginFrame() {
        ensureOpen();
        renderQueue.bindToCurrentThread();
        renderQueue.drain();
        if (isClosed()) {
            return false;
        }
        ports.drain();
        if (isClosed()) {
            return false;
        }
        frameClock.tick();
        return true;
    }

    void endFrame() {
        ensureOpen();
        camera.updateTarget();
    }

    void pause() {
        ensureOpen();
        ports.pause();
    }

    void resume() {
        ensureOpen();
        ports.resume();
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
        ports.stopAccepting();
        closeService("scene actions", actions::close);
        closeService("scene tasks", tasks::close);
        closeService("render-thread queue", renderQueue::close);
        closeService("scene camera", camera::close);
    }

    synchronized void close() {
        if (closed) {
            return;
        }
        prepareForDispose();
        closeService("scene ports", ports::close);
        closeService("scene environment", environment::close);
        closeService("scene assets", assets::close);
        closeService("simulation timeline", timeline::reset);
        closeService("frame clock", frameClock::reset);
        closed = true;
    }

    private void ensureOpen() {
        if (preparedForDispose || closed) {
            throw new IllegalStateException("Scene services are closed for " + scene.getName() + ".");
        }
    }

    private static void closeService(String label, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | LinkageError error) {
            LOGGER.log(Level.WARNING, "Failed to close " + label, error);
        }
    }
}
