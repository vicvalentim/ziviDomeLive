package com.victorvalentim.zividomelive;

import processing.core.*;

/**
 * The CubemapRenderer class handles the creation and rendering of cubemap faces with dynamic frustum adjustments.
 */
public class CubemapRenderer {
    private PGraphics[] cubemapFaces;
    private int resolution;
    private final PApplet parent;

    // Valores padrão para os planos do frustum (podem ser ajustados conforme necessário)
    final float defaultNearPlane = 0.01f;
    final float defaultFarPlane = 12000.0f;

    /**
     * Constructs a CubemapRenderer with the specified initial resolution and parent PApplet.
     *
     * @param initialResolution the initial resolution of the cubemap faces
     * @param parent the parent PApplet instance
     */
    CubemapRenderer(int initialResolution, PApplet parent) {
        this.parent = parent;
        this.resolution = initialResolution;
    }

    /**
     * Initializes or reinitializes the cubemap faces with the current resolution.
     */
    private void initializeCubemapFaces() {
        if (cubemapFaces == null) {
            cubemapFaces = new PGraphics[6];
        }
        for (int i = 0; i < 6; i++) {
            if (cubemapFaces[i] != null) {
                cubemapFaces[i].dispose();
            }
            cubemapFaces[i] = parent.createGraphics(resolution, resolution, PApplet.P3D);
        }
    }

    void updateResolution(int newResolution) {
        if (this.resolution != newResolution) {
            this.resolution = newResolution;
            initializeCubemapFaces();
        }
    }

    private void configureCameraForFace(PGraphics pg, CameraOrientation orientation, float pitch, float yaw, float roll) {
        PVector eye = new PVector(0, 0, 0);

        float dynamicNearPlane = calculateNearPlaneForFace(orientation);
        float dynamicFarPlane = calculateFarPlaneForFace(orientation);
        float fieldOfView = calculateFieldOfViewForFace(orientation);

        pg.camera(eye.x, eye.y, eye.z, orientation.centerX, orientation.centerY, orientation.centerZ, orientation.upX, orientation.upY, orientation.upZ);
        pg.perspective(fieldOfView, 1, dynamicNearPlane, dynamicFarPlane);

        pg.translate((float) pg.width / 2, (float) pg.height / 2, 0);
        pg.rotateX(pitch);
        pg.rotateY(roll);
        pg.rotateZ(yaw);
        pg.translate((float) -pg.width / 2, (float) -pg.height / 2, 0);
    }

    private float calculateNearPlaneForFace(CameraOrientation orientation) {
        return defaultNearPlane;
    }

    private float calculateFarPlaneForFace(CameraOrientation orientation) {
        return defaultFarPlane;
    }

    private float calculateFieldOfViewForFace(CameraOrientation orientation) {
        return PApplet.PI / 2;
    }

    void captureCubemap(float pitch, float yaw, float roll, CameraManager cameraManager, Scene currentScene) {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        for (int i = 0; i < 6; i++) {
            cubemapFaces[i].beginDraw();
            cubemapFaces[i].background(0,0);
            configureCameraForFace(cubemapFaces[i], cameraManager.getOrientation(i), pitch, yaw, roll);
            if (currentScene != null) {
                currentScene.sceneRender(cubemapFaces[i]);
            }
            cubemapFaces[i].endDraw();
        }
    }

    PGraphics[] getCubemapFaces() {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        return cubemapFaces;
    }

   /**
     * Disposes of the cubemap faces, releasing any resources associated with them.
     * This method should be called when the cubemap faces are no longer needed to free up memory.
     */
    public void dispose() {
        if (cubemapFaces != null) {
            for (PGraphics face : cubemapFaces) {
                if (face != null) {
                    face.dispose();
                }
            }
            cubemapFaces = null;
        }
    }
}
