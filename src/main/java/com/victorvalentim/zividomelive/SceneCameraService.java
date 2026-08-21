package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.render.camera.OrbitCamera;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;

import java.util.Objects;
import java.util.function.Supplier;

/** Scene-scoped access to the shared orbit camera and optional target tracking. */
public final class SceneCameraService {

    private final ziviDomeLive parent;
    private final OrbitCamera orbitCamera;
    private final boolean previousInputEnabled;
    private Supplier<PVector> targetSupplier;
    private boolean inputConfigured;
    private boolean closed;

    SceneCameraService(ziviDomeLive parent) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.orbitCamera = parent.getSceneCamera();
        this.previousInputEnabled = parent.isSceneCameraInputEnabled();
    }

    /** @return shared scene-space orbit camera */
    public OrbitCamera orbit() {
        ensureOpen();
        return orbitCamera;
    }

    /**
     * Applies the shared scene-camera transform to an open render target.
     *
     * @param graphics active scene render target
     */
    public void apply(PGraphicsOpenGL graphics) {
        orbit().apply(graphics);
    }

    /**
     * Selects whether navigation input is routed to the scene camera.
     *
     * @param enabled true to enable scene-camera mouse input
     */
    public void setInputEnabled(boolean enabled) {
        ensureOpen();
        inputConfigured = true;
        parent.setSceneCameraInputEnabled(enabled);
    }

    /**
     * Tracks a dynamic world-space target once after each scene update.
     *
     * @param targetSupplier supplier of the latest target, or null results to skip a frame
     */
    public void trackTarget(Supplier<PVector> targetSupplier) {
        ensureOpen();
        this.targetSupplier = Objects.requireNonNull(targetSupplier, "targetSupplier");
    }

    /** Stops automatic target tracking without changing the current camera target. */
    public void clearTargetTracking() {
        ensureOpen();
        targetSupplier = null;
    }

    /** @return whether a dynamic target supplier is configured */
    public boolean isTrackingTarget() {
        ensureOpen();
        return targetSupplier != null;
    }

    void updateTarget() {
        Supplier<PVector> supplier = targetSupplier;
        if (closed || supplier == null) {
            return;
        }
        PVector target = supplier.get();
        if (target != null) {
            orbitCamera.setTarget(target);
        }
    }

    void close() {
        if (closed) {
            return;
        }
        targetSupplier = null;
        orbitCamera.resetInputState();
        if (inputConfigured) {
            parent.setSceneCameraInputEnabled(previousInputEnabled);
        }
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scene camera service is closed.");
        }
    }
}
