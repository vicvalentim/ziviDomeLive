package com.victorvalentim.zividomelive.core.camera;

import com.victorvalentim.zividomelive.core.math.Quaternion;
import com.victorvalentim.zividomelive.core.math.Vec3;

import java.util.Objects;

/**
 * Host-independent quaternion orbit-camera state and controller.
 *
 * <p>Programmatic goals interpolate when {@link #update()} is invoked. Direct manipulation uses
 * immediate methods that synchronize current and goal state. A host owns the update cadence and
 * must call it exactly once per frame, never once per rendered face.</p>
 *
 * <p>Distance is signed. Negative values are valid and preserve the ziviDomeLive convention in
 * which the camera may occupy the {@code -Z} side looking toward {@code +Z}.</p>
 */
public final class OrbitCamera {

    public static final float DEFAULT_DISTANCE = 1500.0f;

    private float targetX;
    private float targetY;
    private float targetZ;
    private float goalTargetX;
    private float goalTargetY;
    private float goalTargetZ;
    private float distance;
    private float goalDistance;
    private Quaternion orientation = Quaternion.identity();
    private Quaternion goalOrientation = orientation;
    private float lerpFactor = 0.15f;
    private float minDistance = 1.0f;
    private float maxDistance = 100000.0f;
    private float collapseGuard;

    /** Creates the qualified default camera at the origin target. */
    public OrbitCamera() {
        this(DEFAULT_DISTANCE);
    }

    /** Creates an origin-target camera with an explicit finite signed distance. */
    public OrbitCamera(float initialDistance) {
        requireFinite(initialDistance, "Initial distance");
        distance = initialDistance;
        goalDistance = initialDistance;
    }

    /** Creates a camera with an explicit immutable target and finite signed distance. */
    public OrbitCamera(Vec3 initialTarget, float initialDistance) {
        this(initialDistance);
        Vec3 target = Objects.requireNonNull(initialTarget, "initialTarget");
        targetX = goalTargetX = target.x();
        targetY = goalTargetY = target.y();
        targetZ = goalTargetZ = target.z();
    }

    /** Advances current orientation, target, and distance one interpolation step. */
    public void update() {
        if (orientation != goalOrientation) {
            Quaternion updatedOrientation = orientation.slerp(goalOrientation, lerpFactor);
            if (updatedOrientation == orientation) {
                goalOrientation = orientation;
            } else {
                orientation = updatedOrientation;
            }
        }
        targetX += (goalTargetX - targetX) * lerpFactor;
        targetY += (goalTargetY - targetY) * lerpFactor;
        targetZ += (goalTargetZ - targetZ) * lerpFactor;
        distance += (goalDistance - distance) * lerpFactor;
    }

    /** Adds a smoothly interpolated world-space orbit rotation. */
    public void rotateAround(float axisX, float axisY, float axisZ, float angle) {
        Quaternion delta = Quaternion.fromAxisAngle(axisX, axisY, axisZ, angle);
        goalOrientation = delta.multiply(goalOrientation).normalized();
    }

    /** Adds a smoothly interpolated world-space orbit rotation. */
    public void rotateAround(Vec3 axis, float angle) {
        Vec3 value = Objects.requireNonNull(axis, "axis");
        rotateAround(value.x(), value.y(), value.z(), angle);
    }

    /** Applies a world-space orbit rotation immediately and synchronizes its goal. */
    public void rotateAroundImmediate(float axisX, float axisY, float axisZ, float angle) {
        Quaternion delta = Quaternion.fromAxisAngle(axisX, axisY, axisZ, angle);
        orientation = delta.multiply(orientation).normalized();
        goalOrientation = orientation;
    }

    /** Applies a world-space orbit rotation immediately and synchronizes its goal. */
    public void rotateAroundImmediate(Vec3 axis, float angle) {
        Vec3 value = Objects.requireNonNull(axis, "axis");
        rotateAroundImmediate(value.x(), value.y(), value.z(), angle);
    }

    /** Changes the distance goal by a signed finite amount. */
    public void zoom(float amount) {
        requireFinite(amount, "Zoom amount");
        goalDistance = guardDistance(goalDistance + amount, goalDistance);
    }

    /** Changes current distance immediately and synchronizes its goal. */
    public void zoomImmediate(float amount) {
        requireFinite(amount, "Zoom amount");
        setDistanceImmediate(distance + amount);
    }

    /** Sets a smoothly interpolated finite target. */
    public void setTarget(float x, float y, float z) {
        requireFinite(x, "Target x");
        requireFinite(y, "Target y");
        requireFinite(z, "Target z");
        goalTargetX = x;
        goalTargetY = y;
        goalTargetZ = z;
    }

    /** Sets a smoothly interpolated immutable target. */
    public void setTarget(Vec3 target) {
        Vec3 value = Objects.requireNonNull(target, "target");
        setTarget(value.x(), value.y(), value.z());
    }

    /** Sets the smoothly interpolated distance goal. */
    public void setDistance(float distance) {
        requireFinite(distance, "Distance");
        goalDistance = guardDistance(distance, distance);
    }

    /** Sets a smoothly interpolated normalized orientation goal. */
    public void setOrientation(Quaternion orientation) {
        goalOrientation = normalizedCopyOf(orientation);
    }

    /** Changes all pose goals as one smoothly interpolated operation. */
    public void goTo(Vec3 target, Quaternion orientation, float distance) {
        Vec3 value = Objects.requireNonNull(target, "target");
        requireFinite(distance, "Distance");
        goalTargetX = value.x();
        goalTargetY = value.y();
        goalTargetZ = value.z();
        goalOrientation = normalizedCopyOf(orientation);
        goalDistance = guardDistance(distance, distance);
    }

    /** Changes all pose goals from an immutable pose. */
    public void goTo(CameraPose pose) {
        CameraPose value = Objects.requireNonNull(pose, "pose");
        goTo(value.target(), value.orientation(), value.distance());
    }

    /** Immediately changes the complete pose and synchronizes every goal. */
    public void snapTo(float x, float y, float z, Quaternion orientation, float distance) {
        requireFinite(x, "Target x");
        requireFinite(y, "Target y");
        requireFinite(z, "Target z");
        requireFinite(distance, "Distance");
        targetX = goalTargetX = x;
        targetY = goalTargetY = y;
        targetZ = goalTargetZ = z;
        this.orientation = normalizedCopyOf(orientation);
        goalOrientation = this.orientation;
        this.distance = guardDistance(distance, distance);
        goalDistance = this.distance;
    }

    /** Immediately changes the complete pose and synchronizes every goal. */
    public void snapTo(Vec3 target, Quaternion orientation, float distance) {
        Vec3 value = Objects.requireNonNull(target, "target");
        snapTo(value.x(), value.y(), value.z(), orientation, distance);
    }

    /** Immediately changes the complete pose and synchronizes every goal. */
    public void snapTo(CameraPose pose) {
        CameraPose value = Objects.requireNonNull(pose, "pose");
        snapTo(value.target(), value.orientation(), value.distance());
    }

    /** Immediately changes only target and synchronizes its goal. */
    public void setTargetImmediate(Vec3 target) {
        Vec3 value = Objects.requireNonNull(target, "target");
        targetX = goalTargetX = value.x();
        targetY = goalTargetY = value.y();
        targetZ = goalTargetZ = value.z();
    }

    /** Immediately changes only orientation and synchronizes its goal. */
    public void setOrientationImmediate(Quaternion orientation) {
        this.orientation = normalizedCopyOf(orientation);
        goalOrientation = this.orientation;
    }

    /** Immediately changes only signed distance and synchronizes its goal. */
    public void setDistanceImmediate(float distance) {
        requireFinite(distance, "Distance");
        this.distance = guardDistance(distance, distance);
        goalDistance = this.distance;
    }

    /** Resets to identity orientation, the origin target, and an explicit distance. */
    public void reset(float distance) {
        snapTo(0.0f, 0.0f, 0.0f, Quaternion.identity(), distance);
    }

    /** Sets an ordered finite distance range and clamps current and goal state. */
    public void setDistanceLimits(float minimum, float maximum) {
        requireFinite(minimum, "Minimum distance");
        requireFinite(maximum, "Maximum distance");
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum distance cannot exceed maximum distance.");
        }
        minDistance = minimum;
        maxDistance = maximum;
        goalDistance = guardDistance(goalDistance, goalDistance);
        distance = guardDistance(distance, distance);
    }

    /**
     * Sets the non-negative forbidden half-width around zero. Smooth zoom stays on its current
     * sign and stops at the guard boundary when the configured range spans zero.
     */
    public void setCollapseGuard(float guard) {
        requireFinite(guard, "Collapse guard");
        collapseGuard = Math.max(0.0f, guard);
        goalDistance = guardDistance(goalDistance, goalDistance);
        distance = guardDistance(distance, distance);
    }

    /** Sets and clamps the finite per-update interpolation factor to [0.001, 1]. */
    public void setLerpFactor(float lerpFactor) {
        requireFinite(lerpFactor, "Interpolation factor");
        this.lerpFactor = constrain(lerpFactor, 0.001f, 1.0f);
    }

    public float getLerpFactor() {
        return lerpFactor;
    }

    public float getMinimumDistance() {
        return minDistance;
    }

    public float getMaximumDistance() {
        return maxDistance;
    }

    public float getCollapseGuard() {
        return collapseGuard;
    }

    public float getDistance() {
        return distance;
    }

    public float getGoalDistance() {
        return goalDistance;
    }

    public Vec3 getTarget() {
        return new Vec3(targetX, targetY, targetZ);
    }

    public Vec3 getGoalTarget() {
        return new Vec3(goalTargetX, goalTargetY, goalTargetZ);
    }

    public Quaternion getOrientation() {
        return orientation;
    }

    public Quaternion getGoalOrientation() {
        return goalOrientation;
    }

    public CameraPose getPose() {
        return new CameraPose(getTarget(), orientation, distance);
    }

    public CameraPose getGoalPose() {
        return new CameraPose(getGoalTarget(), goalOrientation, goalDistance);
    }

    private float guardDistance(float desired, float reference) {
        float guarded = constrain(desired, minDistance, maxDistance);
        if (collapseGuard <= 0.0f) {
            return guarded;
        }
        float sign = reference >= 0.0f ? 1.0f : -1.0f;
        if (guarded * sign < 0.0f) {
            guarded = sign * collapseGuard;
        } else if (Math.abs(guarded) < collapseGuard) {
            guarded = sign * collapseGuard;
        }
        return constrain(guarded, minDistance, maxDistance);
    }

    private static Quaternion normalizedCopyOf(Quaternion quaternion) {
        if (quaternion == null) {
            throw new IllegalArgumentException("Orientation cannot be null.");
        }
        return quaternion.normalized();
    }

    private static float constrain(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void requireFinite(float value, String label) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite.");
        }
    }
}
