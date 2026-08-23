package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.render.camera.OrbitCamera;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Activation-scoped access to the shared scene-space orbit camera.
 *
 * <p>Camera input configuration and target tracking are restored or cleared automatically when
 * the activation ends. The facade advances camera interpolation exactly once per Processing
 * frame; calling {@link OrbitCamera#update()} from a scene would advance it twice.</p>
 *
 * <p><strong>API stability:</strong> Advanced Stable.</p>
 *
 * @since 2.0.0
 */
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

    /** @return shared scene-space orbit camera; never {@code null} during the activation */
    public OrbitCamera orbit() {
        ensureOpen();
        return orbitCamera;
    }

    /**
     * Applies the shared scene-camera transform to an open render target.
     * Call from {@link Scene#sceneRender(PGraphicsOpenGL)} between a matching
     * {@code pushMatrix()}/{@code popMatrix()} pair.
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
     * <p>The supplier runs on the Processing frame thread. Its vector is copied into the camera,
     * so callers may safely reuse one mutable {@link PVector} as a no-allocation result buffer.</p>
     *
     * @param targetSupplier non-null supplier of the latest target; a {@code null} result skips
     *                       that frame
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
