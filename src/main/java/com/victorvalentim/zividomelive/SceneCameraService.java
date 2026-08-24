package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.render.camera.OrbitCamera;
import com.victorvalentim.zividomelive.render.Quaternion;
import processing.core.PConstants;
import processing.core.PMatrix3D;
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
    private final PMatrix3D lightOrientationMatrix = new PMatrix3D();
    private final PVector lightPosition = new PVector();
    private final PVector lightDirection = new PVector();
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
     * Applies the shared scene-camera transform and installs a camera-synchronized view-lighting
     * rig on an open render target.
     *
     * <p>The rig replaces the target's current lights with a neutral ambient light and a warm
     * white spotlight located at the current scene-camera position and aimed at its current
     * look-at target. It remains stable while spherical capture renders multiple cubemap faces
     * because this method only reads camera state; it never advances camera interpolation.</p>
     *
     * <p>Call from {@link Scene#sceneRender(PGraphicsOpenGL)} between a matching
     * {@code pushMatrix()}/{@code popMatrix()} pair, in place of {@link #apply(PGraphicsOpenGL)}.
     * Additional scene lights may be added after this call.</p>
     *
     * @param graphics active scene render target
     */
    public void applyWithViewLighting(PGraphicsOpenGL graphics) {
        ensureOpen();
        Objects.requireNonNull(graphics, "graphics");
        orbitCamera.apply(graphics);
        calculateViewLightPose(
                orbitCamera, lightPosition, lightDirection, lightOrientationMatrix);

        graphics.noLights();
        graphics.ambientLight(36f, 38f, 48f);
        graphics.spotLight(
                255f, 248f, 232f,
                lightPosition.x, lightPosition.y, lightPosition.z,
                lightDirection.x, lightDirection.y, lightDirection.z,
                PConstants.HALF_PI, 1f);
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
     * Sets the allowed orbit-distance interval.
     *
     * @param minimum minimum distance
     * @param maximum maximum distance
     */
    public void setDistanceLimits(float minimum, float maximum) {
        orbit().setDistanceLimits(minimum, maximum);
    }

    /**
     * Sets the dead zone that prevents an orbit distance from collapsing through zero.
     *
     * @param guard non-negative dead-zone radius
     */
    public void setCollapseGuard(float guard) {
        orbit().setCollapseGuard(guard);
    }

    /**
     * Sets the interpolation amount used by programmatic camera movement.
     *
     * @param factor interpolation factor in the camera's supported range
     */
    public void setLerpFactor(float factor) {
        orbit().setLerpFactor(factor);
    }

    /**
     * Sets direct pointer-drag sensitivity in radians per pixel.
     *
     * @param sensitivity non-negative drag sensitivity
     */
    public void setDragSensitivity(float sensitivity) {
        orbit().setDragSensitivity(sensitivity);
    }

    /**
     * Immediately applies a complete orbit pose expressed as an axis-angle rotation.
     * This root-service convenience keeps ordinary Processing scenes independent from the
     * advanced quaternion package while retaining the same qualified camera mathematics.
     *
     * @param targetX look-at target X
     * @param targetY look-at target Y
     * @param targetZ look-at target Z
     * @param axisX rotation-axis X
     * @param axisY rotation-axis Y
     * @param axisZ rotation-axis Z
     * @param angle rotation angle in radians
     * @param distance orbit distance
     */
    public void snapToAxisAngle(
            float targetX,
            float targetY,
            float targetZ,
            float axisX,
            float axisY,
            float axisZ,
            float angle,
            float distance) {
        orbit().snapTo(
                targetX,
                targetY,
                targetZ,
                Quaternion.fromAxisAngle(axisX, axisY, axisZ, angle),
                distance);
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

    static void calculateViewLightPose(
            OrbitCamera camera,
            PVector position,
            PVector direction,
            PMatrix3D orientationMatrix) {
        Objects.requireNonNull(camera, "camera");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(orientationMatrix, "orientationMatrix");

        PVector target = camera.getTarget();
        camera.getOrientation().toMatrix(orientationMatrix);
        float distance = camera.getDistance();
        float offsetX = orientationMatrix.m20 * distance;
        float offsetY = orientationMatrix.m21 * distance;
        float offsetZ = orientationMatrix.m22 * distance;
        position.set(target.x + offsetX, target.y + offsetY, target.z + offsetZ);

        float offsetMagnitude = Math.abs(distance);
        if (offsetMagnitude > 1.0e-6f) {
            direction.set(
                    -offsetX / offsetMagnitude,
                    -offsetY / offsetMagnitude,
                    -offsetZ / offsetMagnitude);
        } else {
            direction.set(
                    -orientationMatrix.m20,
                    -orientationMatrix.m21,
                    -orientationMatrix.m22);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scene camera service is closed.");
        }
    }
}
