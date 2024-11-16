package com.victorvalentim.zividomelive.render.camera;

import java.util.ArrayList;
import java.util.List;

/**
 * The CameraManager class manages a list of CameraOrientation objects.
 * It provides methods to initialize and retrieve different camera orientations.
 */
public class CameraManager {
    List<CameraOrientation> orientations;

    /**
     * Constructs a CameraManager and initializes the camera orientations.
     */
	public CameraManager() {
        initializeOrientations();
    }

    /**
     * Initializes the list of camera orientations with predefined values.
     */
    void initializeOrientations() {
        orientations = new ArrayList<>();
        orientations.add(new CameraOrientation(0, 0, 0, 1, 0, 0, 0, -1, 0));
        orientations.add(new CameraOrientation(0, 0, 0, -1, 0, 0, 0, -1, 0));
        orientations.add(new CameraOrientation(0, 0, 0, 0, 1, 0, 0, 0, 1));
        orientations.add(new CameraOrientation(0, 0, 0, 0, -1, 0, 0, 0, -1));
        orientations.add(new CameraOrientation(0, 0, 0, 0, 0, 1, 0, -1, 0));
        orientations.add(new CameraOrientation(0, 0, 0, 0, 0, -1, 0, -1, 0));
    }

    /**
     * Retrieves the CameraOrientation at the specified index.
     *
     * @param index the index of the desired CameraOrientation
     * @return the CameraOrientation at the specified index
     */
	public CameraOrientation getOrientation(int index) {
        return orientations.get(index);
    }
}