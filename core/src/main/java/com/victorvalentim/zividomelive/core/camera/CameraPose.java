package com.victorvalentim.zividomelive.core.camera;

import com.victorvalentim.zividomelive.core.math.Quaternion;
import com.victorvalentim.zividomelive.core.math.Vec3;

import java.util.Objects;

/** Immutable scene-camera pose with a signed orbit distance. */
public record CameraPose(Vec3 target, Quaternion orientation, float distance) {

    /** Validates values and normalizes the immutable orientation. */
    public CameraPose {
        target = Objects.requireNonNull(target, "target");
        orientation = Objects.requireNonNull(orientation, "orientation").normalized();
        if (!Float.isFinite(distance)) {
            throw new IllegalArgumentException("Camera distance must be finite.");
        }
    }
}
