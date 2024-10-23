package com.victorvalentim.zividomelive;


import java.util.ArrayList;
import java.util.List;

public class CameraManager {
	List<CameraOrientation> orientations;

    CameraManager() {
        initializeOrientations();
    }

    void initializeOrientations() {
        // Initialize different camera orientations, storing them in a list
        orientations = new ArrayList<>();
        orientations.add(new CameraOrientation(0, 0, 0, 1, 0, 0, 0, -1, 0));
        orientations.add(new CameraOrientation(0, 0, 0, -1, 0, 0, 0, -1, 0));
        orientations.add(new CameraOrientation(0, 0, 0, 0, 1, 0, 0, 0, 1));
        orientations.add(new CameraOrientation(0, 0, 0, 0, -1, 0, 0, 0, -1));
        orientations.add(new CameraOrientation(0, 0, 0, 0, 0, 1, 0, -1, 0));
        orientations.add(new CameraOrientation(0, 0, 0, 0, 0, -1, 0, -1, 0));
    }

    CameraOrientation getOrientation(int index) {
        return orientations.get(index);
    }
}

