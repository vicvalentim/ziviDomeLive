package com.victorvalentim.zividomelive;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility facade for cubemap camera orientations.
 *
 * <p>The canonical six-face contract lives in {@link CubemapFace}. This class
 * keeps the 1.x public API available for direct renderer integrations.</p>
 */
class CameraManager {
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
        orientations = new ArrayList<>(CubemapFace.count());
        for (int i = 0; i < CubemapFace.count(); i++) {
            orientations.add(CubemapFace.at(i).createCameraOrientation());
        }
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

	/**
     * Disposes of the CameraManager by clearing the list of camera orientations.
     */
    public void dispose() {
        orientations.clear();
    }
}
