package com.victorvalentim.zividomelive.render.camera;

import com.victorvalentim.zividomelive.render.Quaternion;
import processing.core.*;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

/**
 * A mouse-controlled orbit camera for the Standard View.
 *
 * <p>Rotation is tracked as a unit quaternion to avoid gimbal lock.
 * Horizontal drag rotates around the world Y axis (yaw);
 * vertical drag rotates around the camera's current right axis (pitch).
 * The scroll wheel zooms multiplicatively within configurable limits.</p>
 */
public class MouseControlledCamera implements PConstants {

    /** Orbit distance from the look-at center. */
    private float distance = 1500f;

    /** Minimum allowed orbit distance. */
    private static final float MIN_DISTANCE = 50f;

    /** Maximum allowed orbit distance. */
    private static final float MAX_DISTANCE = 10000f;

    /** Zoom multiplier applied per scroll-wheel notch. */
    private static final float ZOOM_FACTOR = 0.92f;

    /** Drag sensitivity in radians per pixel. */
    private static final float DRAG_SENSITIVITY = 0.005f;

    /** Current camera orientation as a unit quaternion (identity = eye at +Z). */
    private Quaternion rotation = new Quaternion(0f, 0f, 0f, 1f);

    /** Cached eye position updated each frame. */
    private final PVector position = new PVector(0, 0, 0);

    /** Cached rotation matrix shared by camera application and environment ray reconstruction. */
    private final PMatrix3D rotationMatrix = new PMatrix3D();

    /** Look-at center (fixed at origin). */
    private final PVector center = new PVector(0, 0, 0);

    /** Previous mouse coordinates for delta computation during drag. */
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private boolean dragging = false;

    /**
     * Constructs a MouseControlledCamera with default orbit settings.
     * The initial eye position is at (0, 0, distance) looking at the origin.
     */
    public MouseControlledCamera() {
        // identity rotation keeps eye at (0, 0, distance)
    }

    /**
     * Recomputes the eye position from the current quaternion rotation and distance.
     * Call once per frame before {@link #apply(PGraphicsOpenGL)}.
     *
     * @param parent the PApplet instance (unused but kept for API compatibility)
     */
    public void update(PApplet parent) {
        rotation.toMatrix(rotationMatrix);
        // Camera eye is the rotated (0, 0, distance) vector offset from center
        position.x = center.x + rotationMatrix.m02 * distance;
        position.y = center.y + rotationMatrix.m12 * distance;
        position.z = center.z + rotationMatrix.m22 * distance;
    }

    /**
     * Applies the camera view to the given PGraphics object.
     *
     * @param pg the PGraphics object to apply the camera view to
     */
    public void apply(PGraphicsOpenGL pg) {
        // Up vector is the rotated (0, 1, 0) – second column of rotation matrix
        pg.camera(
            position.x, position.y, position.z,
            center.x,   center.y,   center.z,
            rotationMatrix.m01, rotationMatrix.m11, rotationMatrix.m21
        );
    }

    /**
     * Copies the camera rotation into a caller-owned matrix without including eye translation.
     *
     * @param destination destination matrix; must not be {@code null}
     */
    public void copyRotationMatrix(PMatrix3D destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Destination matrix cannot be null.");
        }
        destination.set(rotationMatrix);
    }

    /**
     * Handles mouse events: scroll to zoom, left-drag to orbit.
     *
     * @param event the MouseEvent object containing details of the mouse event
     */
    public void mouseEvent(MouseEvent event) {
        switch (event.getAction()) {
            case MouseEvent.WHEEL:
                zoom(event.getCount());
                break;
            case MouseEvent.DRAG:
                handleDrag(event);
                break;
            case MouseEvent.RELEASE:
                resetInputState();
                break;
            default:
                break;
        }
    }

    /**
     * Resets rotation to identity (eye at +Z, no tilt) while keeping current distance.
     */
    public void resetRotation() {
        rotation = new Quaternion(0f, 0f, 0f, 1f);
    }

    /**
     * Clears transient pointer state without changing the camera pose.
     */
    public void resetInputState() {
        dragging = false;
        lastMouseX = -1;
        lastMouseY = -1;
    }

    /**
     * Returns the current orbit distance.
     *
     * @return distance from center to eye
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Sets the orbit distance, clamped to [MIN_DISTANCE, MAX_DISTANCE].
     *
     * @param distance desired distance
     */
    public void setDistance(float distance) {
        this.distance = PApplet.constrain(distance, MIN_DISTANCE, MAX_DISTANCE);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Handles left-button drag to orbit the camera using quaternion increments.
     * Horizontal drag applies a world-Y yaw; vertical drag applies a camera-local-X pitch.
     *
     * @param event the drag MouseEvent
     */
    private void handleDrag(MouseEvent event) {
        if (event.getButton() != LEFT) return;

        if (!dragging) {
            dragging = true;
            lastMouseX = event.getX();
            lastMouseY = event.getY();
            return;
        }

        float dx = event.getX() - lastMouseX;
        float dy = event.getY() - lastMouseY;
        lastMouseX = event.getX();
        lastMouseY = event.getY();

        if (dx == 0 && dy == 0) return;

        // Yaw: rotate around world Y axis
        Quaternion yawDelta = Quaternion.fromAxisAngle(0f, 1f, 0f, -dx * DRAG_SENSITIVITY);

        // Pitch: rotate around camera's current right axis (first column of rotation matrix)
        rotation.toMatrix(rotationMatrix);
        Quaternion pitchDelta = Quaternion.fromAxisAngle(
                rotationMatrix.m00,
                rotationMatrix.m10,
                rotationMatrix.m20,
                -dy * DRAG_SENSITIVITY);

        // Apply yaw in world space (pre-multiply), pitch in local space (post-multiply)
        rotation = yawDelta.multiply(rotation).multiply(pitchDelta).normalized();
    }

    /**
     * Zooms multiplicatively so large distances zoom faster and small ones stay precise.
     *
     * @param delta scroll wheel notch count (positive = zoom out)
     */
    private void zoom(float delta) {
        if (delta > 0) {
            distance /= ZOOM_FACTOR;
        } else if (delta < 0) {
            distance *= ZOOM_FACTOR;
        }
        distance = PApplet.constrain(distance, MIN_DISTANCE, MAX_DISTANCE);
    }
}
