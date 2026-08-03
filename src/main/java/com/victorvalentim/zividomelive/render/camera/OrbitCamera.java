package com.victorvalentim.zividomelive.render.camera;

import com.victorvalentim.zividomelive.render.Quaternion;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

/**
 * Scene-space quaternion orbit camera provided as a native ziviDomeLive service.
 *
 * <p>Unlike {@link MouseControlledCamera} (which drives the Standard View through
 * {@code pg.camera(...)}), {@code OrbitCamera} transforms the scene modelview
 * directly inside {@code sceneRender}. Because it operates in scene space, the
 * same camera works identically across every projection (fisheye, equirectangular,
 * cubemap and standard) and never touches the dome parameters (yaw / pitch / roll /
 * fov) articulated by the ControlManager.</p>
 *
 * <p>Typical usage inside a {@code Scene}:</p>
 * <pre>
 * public void sceneRender(PGraphicsOpenGL pg) {
 *     pg.pushMatrix();
 *     parent.getSceneCamera().apply(pg); // move through space
 *     // ... draw scene content ...
 *     pg.popMatrix();
 * }
 * </pre>
 *
 * <p>Rotations use unit quaternions (gimbal-lock free) and every target,
 * orientation and distance change is smoothly interpolated (SLERP/LERP) for
 * fluid motion. Mouse handling is built in: drag to orbit, wheel to fly in/out.</p>
 */
public class OrbitCamera implements PConstants {

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
    private Quaternion goalOrientation = new Quaternion(0, 0, 0, 1);

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
    private float wheelStep = 120f;
    /** Distance change per fractional (trackpad) wheel notch. */
    private float wheelPadStep = 4f;

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
     * Creates an orbit camera with a default distance of 1500 units.
     */
    public OrbitCamera() {
        this(1500f);
    }

    /**
     * Applies the camera transform to the given scene graphics.
     * Call inside {@code sceneRender} between {@code pushMatrix}/{@code popMatrix}.
     *
     * @param pg the scene graphics to transform
     */
    public void apply(PGraphicsOpenGL pg) {
        pg.translate(0, 0, -distance);
        pg.applyMatrix(orientation.toMatrix());
        pg.translate(-target.x, -target.y, -target.z);
    }

    /**
     * Advances the smooth interpolation toward the current goals.
     * The library calls this once per frame; scenes normally do not need to.
     */
    public void update() {
        orientation = orientation.slerp(goalOrientation, lerpFactor);
        target.set(PVector.lerp(target, goalTarget, lerpFactor));
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
        goalOrientation = delta.multiply(goalOrientation).normalize();
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
     * Handles mouse input: drag to orbit, wheel to fly in/out.
     *
     * @param event the mouse event
     */
    public void mouseEvent(MouseEvent event) {
        switch (event.getAction()) {
            case MouseEvent.PRESS:
                dragging = true;
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                break;
            case MouseEvent.RELEASE:
                dragging = false;
                lastMouseX = -1;
                lastMouseY = -1;
                break;
            case MouseEvent.DRAG:
                handleDrag(event);
                break;
            case MouseEvent.WHEEL:
                float scroll = event.getCount();
                boolean isPad = Math.abs(scroll) < 1f;
                zoom(isPad ? scroll * wheelPadStep : scroll * wheelStep);
                break;
            default:
                break;
        }
    }

    private void handleDrag(MouseEvent event) {
        if (lastMouseX < 0 || lastMouseY < 0) {
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
        rotateAround(0f, 1f, 0f, dx); // yaw around world up
        rotateAround(1f, 0f, 0f, dy); // pitch around world right
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
        goalOrientation = q.normalize();
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
        orientation = q.normalize();
        goalOrientation = new Quaternion(orientation.x, orientation.y, orientation.z, orientation.w);
        distance = guardDistance(d, d);
        goalDistance = distance;
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
     * @param min minimum distance
     * @param max maximum distance
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
     * @param dragSensitivity sensitivity value
     */
    public void setDragSensitivity(float dragSensitivity) {
        this.dragSensitivity = dragSensitivity;
    }

    /**
     * Sets the wheel zoom step sizes.
     *
     * @param wheelStep    distance change per standard notch
     * @param wheelPadStep distance change per fractional (trackpad) notch
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
     * Returns the current (interpolated) orientation quaternion.
     *
     * @return current orientation
     */
    public Quaternion getOrientation() {
        return orientation;
    }
}

