package com.victorvalentim.zividomelive.render.camera;

import com.victorvalentim.zividomelive.render.Quaternion;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.core.PVector;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.RIGHT;

/**
 * Scene-space quaternion orbit camera provided as a native ziviDomeLive service.
 *
 * <p>The camera transforms the scene modelview directly inside
 * {@link com.victorvalentim.zividomelive.Scene#sceneRender(PGraphicsOpenGL)}. Because it operates
 * in scene space, the same pose works across Standard, domemaster, equirectangular, and skybox
 * views without changing spherical calibration controls.</p>
 *
 * <p>Typical usage inside a {@code Scene}:</p>
 * <pre>
 * public void sceneRender(PGraphicsOpenGL pg) {
 *     pg.pushMatrix();
 *     services.camera().apply(pg);
 *     // ... draw scene content ...
 *     pg.popMatrix();
 * }
 * </pre>
 *
 * <p>Rotations use unit quaternions (gimbal-lock free). Programmatic pose changes
 * are smoothly interpolated (SLERP/LERP), while direct mouse manipulation is
 * applied immediately so drag and wheel gestures remain attached to the pointer.</p>
 *
 * <p><strong>API stability:</strong> Advanced Stable.</p>
 *
 * @since 2.0.0
 */
public final class OrbitCamera {

    /** Point the camera looks at (current, interpolated). */
    private final PVector target = new PVector(0, 0, 0);
    /** Target the camera is easing toward. */
    private final PVector goalTarget = new PVector(0, 0, 0);

    /** Current orbit distance from the target. */
    private float distance;
    /** Goal orbit distance the camera is easing toward. */
    private float goalDistance;

    /** Current orientation (unit quaternion). */
    private Quaternion orientation = new Quaternion(0, 0, 0, 1);
    /** Goal orientation the camera is easing toward. */
    private Quaternion goalOrientation = orientation;
    /** Reused transform matrix; one camera may be applied to several cubemap faces per frame. */
    private final PMatrix3D orientationMatrix = new PMatrix3D();
    private boolean orientationMatrixDirty = true;

    /** Interpolation amount per frame (0..1); higher is snappier. */
    private float lerpFactor = 0.15f;

    /** Minimum allowed orbit distance. */
    private float minDistance = 1f;
    /** Maximum allowed orbit distance. */
    private float maxDistance = 100000f;
    /**
     * Collapse-guard dead zone around distance 0. When positive, the orbit
     * distance is never allowed inside (-collapseGuard, +collapseGuard) and can
     * never flip sign through zero, preventing the view from collapsing when the
     * allowed range spans negative and positive distances.
     */
    private float collapseGuard = 0f;

    /** Drag sensitivity in radians per pixel. */
    private float dragSensitivity = 0.01f;
    /** Distance change per standard wheel notch. */
    private float wheelStep = 80f;
    /** Distance change per fractional (trackpad) wheel notch. */
    private float wheelPadStep = 0.001f;

    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean dragging = false;

    /**
     * Creates an orbit camera looking at the origin from the given distance.
     *
     * @param initialDistance initial distance from the target
     */
    public OrbitCamera(float initialDistance) {
        this.distance = initialDistance;
        this.goalDistance = initialDistance;
    }

    /**
     * Creates an orbit camera looking at an initial target from the given distance.
     *
     * @param initialTarget initial look-at target; must not be {@code null}
     * @param initialDistance initial distance from the target
     */
    public OrbitCamera(PVector initialTarget, float initialDistance) {
        this(initialDistance);
        requireVector(initialTarget, "Initial target");
        target.set(initialTarget);
        goalTarget.set(initialTarget);
    }

    /**
     * Creates an orbit camera with a default distance of 1500 units.
     */
    public OrbitCamera() {
        this(1500f);
    }

    /**
     * Applies the camera transform to the given scene graphics.
     * Call inside {@code sceneRender} between {@code pushMatrix}/{@code popMatrix}.
     *
     * <p>This method reads camera state but does not advance interpolation.</p>
     *
     * @param pg non-null scene graphics to transform
     */
    public void apply(PGraphicsOpenGL pg) {
        if (orientationMatrixDirty) {
            orientation.toMatrix(orientationMatrix);
            orientationMatrixDirty = false;
        }
        pg.translate(0, 0, -distance);
        pg.applyMatrix(orientationMatrix);
        pg.translate(-target.x, -target.y, -target.z);
    }

    /**
     * Advances smooth interpolation toward the current goals by one step.
     *
     * <p>The ziviDomeLive facade calls this exactly once per Processing frame for its shared
     * camera; scenes using {@link com.victorvalentim.zividomelive.SceneCameraService} must not call
     * it again. Code that constructs a standalone camera owns its update cadence.</p>
     */
    public void update() {
		if (orientation != goalOrientation) {
			Quaternion updatedOrientation = orientation.slerp(goalOrientation, lerpFactor);
			if (updatedOrientation == orientation) {
				goalOrientation = orientation;
			} else {
				orientation = updatedOrientation;
				orientationMatrixDirty = true;
			}
        }
        target.x += (goalTarget.x - target.x) * lerpFactor;
        target.y += (goalTarget.y - target.y) * lerpFactor;
        target.z += (goalTarget.z - target.z) * lerpFactor;
        distance = PApplet.lerp(distance, goalDistance, lerpFactor);
    }

    /**
     * Orbits the camera around a world-space axis (eases smoothly).
     *
     * @param ax    axis x component
     * @param ay    axis y component
     * @param az    axis z component
     * @param angle rotation angle in radians
     */
    public void rotateAround(float ax, float ay, float az, float angle) {
        Quaternion delta = Quaternion.fromAxisAngle(ax, ay, az, angle);
        goalOrientation = delta.multiply(goalOrientation).normalized();
    }

    /**
     * Orbits the camera around a world-space Processing vector (eases smoothly).
     *
     * @param axis world-space axis; must not be {@code null}
     * @param angle rotation angle in radians
     */
    public void rotateAround(PVector axis, float angle) {
        requireVector(axis, "Rotation axis");
        rotateAround(axis.x, axis.y, axis.z, angle);
    }

    /**
     * Applies an orbit rotation immediately, keeping the interpolation goal synchronized.
     *
     * @param ax axis x component
     * @param ay axis y component
     * @param az axis z component
     * @param angle rotation angle in radians
     */
    public void rotateAroundImmediate(float ax, float ay, float az, float angle) {
        Quaternion delta = Quaternion.fromAxisAngle(ax, ay, az, angle);
        orientation = delta.multiply(orientation).normalized();
        goalOrientation = orientation;
        orientationMatrixDirty = true;
    }

    /**
     * Applies an orbit rotation around a Processing vector immediately.
     *
     * @param axis world-space axis; must not be {@code null}
     * @param angle rotation angle in radians
     */
    public void rotateAroundImmediate(PVector axis, float angle) {
        requireVector(axis, "Rotation axis");
        rotateAroundImmediate(axis.x, axis.y, axis.z, angle);
    }

    /**
     * Changes the goal distance by a signed amount (clamped to limits).
     *
     * @param amount distance delta (positive flies away, negative flies in)
     */
    public void zoom(float amount) {
        goalDistance = guardDistance(goalDistance + amount, goalDistance);
    }

    /**
     * Changes the current distance immediately and keeps its interpolation goal synchronized.
     * This is intended for direct manipulation such as mouse-wheel navigation.
     *
     * @param amount distance delta (positive flies away, negative flies in)
     */
    public void zoomImmediate(float amount) {
        setDistanceImmediate(distance + amount);
    }

    /**
     * Handles mouse input: drag to orbit, wheel to fly in/out.
     *
     * @param event non-null Processing mouse event
     */
    public void mouseEvent(MouseEvent event) {
        switch (event.getAction()) {
            case MouseEvent.PRESS:
                dragging = true;
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                break;
            case MouseEvent.RELEASE:
                resetInputState();
                break;
            case MouseEvent.DRAG:
                handleDrag(event);
                break;
            case MouseEvent.WHEEL:
                float scroll = event.getCount();
                boolean isPad = Math.abs(scroll) < 1f;
                zoomImmediate(isPad ? scroll * wheelPadStep : scroll * wheelStep);
                break;
            default:
                break;
        }
    }

    private void handleDrag(MouseEvent event) {
        if (!dragging || lastMouseX < 0 || lastMouseY < 0) {
            dragging = true;
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            return;
        }
        float dx = (event.getX() - lastMouseX) * dragSensitivity;
        float dy = (event.getY() - lastMouseY) * dragSensitivity;
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        if (dx == 0 && dy == 0) return;
        rotateAroundImmediate(0f, 1f, 0f, dx); // yaw around world up
        rotateAroundImmediate(1f, 0f, 0f, dy); // pitch around world right
    }

    /**
     * Clears transient pointer state without changing the camera pose.
     * The next drag starts from a fresh anchor instead of reusing stale coordinates.
     */
    public void resetInputState() {
        dragging = false;
        lastMouseX = -1;
        lastMouseY = -1;
    }

    // -------------------------------------------------------------------------
    // Goal setters (interpolated) and immediate snap
    // -------------------------------------------------------------------------

    /**
     * Sets the goal look-at target (eased smoothly).
     *
     * @param x target x
     * @param y target y
     * @param z target z
     */
    public void setTarget(float x, float y, float z) {
        goalTarget.set(x, y, z);
    }

    /**
     * Sets the goal look-at target from a Processing vector (eased smoothly).
     *
     * @param target desired target; must not be {@code null}
     */
    public void setTarget(PVector target) {
        requireVector(target, "Target");
        setTarget(target.x, target.y, target.z);
    }

    /**
     * Sets the goal orbit distance (clamped, eased smoothly).
     *
     * @param d desired distance
     */
    public void setDistance(float d) {
        goalDistance = guardDistance(d, d);
    }

    /**
     * Sets the goal orientation (eased smoothly).
     *
     * @param q desired orientation quaternion
     */
    public void setOrientation(Quaternion q) {
        goalOrientation = normalizedCopyOf(q);
    }

    /**
     * Changes target, orientation, and distance as one smoothly interpolated pose.
     *
     * @param target desired look-at target; must not be {@code null}
     * @param orientation desired orientation; must not be {@code null}
     * @param distance desired orbit distance
     */
    public void goTo(PVector target, Quaternion orientation, float distance) {
        requireVector(target, "Target");
        goalTarget.set(target);
        goalOrientation = normalizedCopyOf(orientation);
        goalDistance = guardDistance(distance, distance);
    }

    /**
     * Immediately snaps the camera to the given pose with no interpolation.
     *
     * @param tx target x
     * @param ty target y
     * @param tz target z
     * @param q  orientation quaternion
     * @param d  distance
     */
    public void snapTo(float tx, float ty, float tz, Quaternion q, float d) {
        target.set(tx, ty, tz);
        goalTarget.set(tx, ty, tz);
        orientation = normalizedCopyOf(q);
        goalOrientation = orientation;
        orientationMatrixDirty = true;
        distance = guardDistance(d, d);
        goalDistance = distance;
    }

    /**
     * Immediately snaps the camera to a pose described with a Processing target vector.
     *
     * @param target look-at target; must not be {@code null}
     * @param orientation orientation quaternion; must not be {@code null}
     * @param distance orbit distance
     */
    public void snapTo(PVector target, Quaternion orientation, float distance) {
        requireVector(target, "Target");
        snapTo(target.x, target.y, target.z, orientation, distance);
    }

    /**
     * Immediately changes only the look-at target and synchronizes its interpolation goal.
     *
     * @param target desired target; must not be {@code null}
     */
    public void setTargetImmediate(PVector target) {
        requireVector(target, "Target");
        this.target.set(target);
        goalTarget.set(target);
    }

    /**
     * Immediately changes only the orientation and synchronizes its interpolation goal.
     *
     * @param orientation desired orientation; must not be {@code null}
     */
    public void setOrientationImmediate(Quaternion orientation) {
        this.orientation = normalizedCopyOf(orientation);
        goalOrientation = this.orientation;
        orientationMatrixDirty = true;
    }

    /**
     * Immediately changes only the distance and synchronizes its interpolation goal.
     *
     * @param distance desired orbit distance
     */
    public void setDistanceImmediate(float distance) {
        this.distance = guardDistance(distance, distance);
        goalDistance = this.distance;
    }

    /**
     * Resets the camera to identity orientation, origin target and the given distance.
     *
     * @param d distance to reset to
     */
    public void reset(float d) {
        snapTo(0, 0, 0, new Quaternion(0, 0, 0, 1), d);
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Sets the allowed distance range for zoom.
     *
     * @param min minimum distance; callers should supply a finite value no greater than max
     * @param max maximum distance; callers should supply a finite value no less than min
     */
    public void setDistanceLimits(float min, float max) {
        this.minDistance = min;
        this.maxDistance = max;
        this.goalDistance = guardDistance(goalDistance, goalDistance);
        this.distance = guardDistance(distance, distance);
    }

    /**
     * Sets a collapse-guard dead zone around distance 0. When positive, the orbit
     * distance can never enter {@code (-guard, +guard)} nor flip sign through zero,
     * which prevents the view from collapsing when the allowed distance range spans
     * both negative and positive values. Set to 0 to disable.
     *
     * @param guard half-width of the forbidden zone around zero (>= 0)
     */
    public void setCollapseGuard(float guard) {
        this.collapseGuard = Math.max(0f, guard);
        this.goalDistance = guardDistance(goalDistance, goalDistance);
        this.distance = guardDistance(distance, distance);
    }

    /**
     * Clamps a desired distance to the allowed range and enforces the collapse
     * guard: the result stays on the same side of zero as {@code reference} and
     * never enters the forbidden dead zone.
     *
     * @param desired   requested distance
     * @param reference distance whose sign defines the allowed side
     * @return a safe distance value
     */
    private float guardDistance(float desired, float reference) {
        float d = PApplet.constrain(desired, minDistance, maxDistance);
        if (collapseGuard <= 0f) {
            return d;
        }
        float sign = reference >= 0f ? 1f : -1f;
        if (d * sign < 0f) {
            // Would cross zero: stop at the near boundary on the reference side.
            d = sign * collapseGuard;
        } else if (Math.abs(d) < collapseGuard) {
            d = sign * collapseGuard;
        }
        return PApplet.constrain(d, minDistance, maxDistance);
    }

    /**
     * Sets the interpolation amount per frame (0..1). Higher is snappier.
     *
     * @param lerpFactor easing factor
     */
    public void setLerpFactor(float lerpFactor) {
        this.lerpFactor = PApplet.constrain(lerpFactor, 0.001f, 1f);
    }

    /**
     * Sets drag sensitivity in radians per pixel.
     *
     * @param dragSensitivity finite sensitivity value
     */
    public void setDragSensitivity(float dragSensitivity) {
        this.dragSensitivity = dragSensitivity;
    }

    /**
     * Sets the wheel zoom step sizes.
     *
     * @param wheelStep    finite distance change per standard notch
     * @param wheelPadStep finite distance change per fractional (trackpad) notch
     */
    public void setWheelSteps(float wheelStep, float wheelPadStep) {
        this.wheelStep = wheelStep;
        this.wheelPadStep = wheelPadStep;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the current (interpolated) orbit distance.
     *
     * @return current distance
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Returns a copy of the current (interpolated) look-at target.
     *
     * @return current target as a new PVector
     */
    public PVector getTarget() {
        return target.copy();
    }

    /**
     * Returns the current immutable, interpolated orientation quaternion.
     *
     * @return current orientation
     */
    public Quaternion getOrientation() {
        return orientation;
    }

    private static Quaternion normalizedCopyOf(Quaternion quaternion) {
        if (quaternion == null) {
            throw new IllegalArgumentException("Orientation cannot be null.");
        }
        return quaternion.normalized();
    }

    private static void requireVector(PVector vector, String label) {
        if (vector == null) {
            throw new IllegalArgumentException(label + " cannot be null.");
        }
    }
}
