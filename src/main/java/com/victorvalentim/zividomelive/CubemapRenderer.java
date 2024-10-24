package com.victorvalentim.zividomelive;

import processing.core.*;

/**
 * The CubemapRenderer class handles the creation and rendering of cubemap faces.
 */
public class CubemapRenderer {
    private PGraphics[] cubemapFaces;
    private int resolution;
    private PApplet parent;

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
            cubemapFaces[i] = parent.createGraphics(resolution / 2, resolution / 2, PApplet.P3D);
        }
    }

    /**
     * Updates the resolution of the cubemap and reinitializes the faces.
     *
     * @param newResolution the new resolution to be set
     */
    void updateResolution(int newResolution) {
        if (this.resolution != newResolution) {
            this.resolution = newResolution;
            initializeCubemapFaces();
        }
    }

    /**
     * Configures the camera for a specific face of the cubemap.
     *
     * @param pg the PGraphics object for the cubemap face
     * @param orientation the CameraOrientation for the face
     * @param pitch the pitch angle
     * @param yaw the yaw angle
     * @param roll the roll angle
     */
    private void configureCameraForFace(PGraphics pg, CameraOrientation orientation, float pitch, float yaw, float roll) {
        PVector eye = new PVector(0, 0, 0);
        pg.camera(eye.x, eye.y, eye.z, orientation.centerX, orientation.centerY, orientation.centerZ, orientation.upX, orientation.upY, orientation.upZ);
        pg.perspective(PApplet.PI / 2, 1, 0.1f, 20000);
        pg.translate(pg.width / 2, pg.height / 2, 0);
        pg.rotateX(pitch);
        pg.rotateY(roll);
        pg.rotateZ(yaw);
        pg.translate(-pg.width / 2, -pg.height / 2, 0);
    }

    /**
     * Captures the cubemap by applying camera transformations and rendering the scene.
     *
     * @param pitch the pitch angle
     * @param yaw the yaw angle
     * @param roll the roll angle
     * @param cameraManager the CameraManager instance
     * @param currentScene the current Scene instance
     */
    void captureCubemap(float pitch, float yaw, float roll, CameraManager cameraManager, Scene currentScene) {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        for (int i = 0; i < 6; i++) {
            cubemapFaces[i].beginDraw();
            configureCameraForFace(cubemapFaces[i], cameraManager.getOrientation(i), pitch, yaw, roll);
            if (currentScene != null) {
                currentScene.sceneRender(cubemapFaces[i]);
            }
            cubemapFaces[i].endDraw();
        }
    }

    /**
     * Returns the cubemap faces for other renderers.
     *
     * @return an array of PGraphics objects representing the cubemap faces
     */
    PGraphics[] getCubemapFaces() {
        if (cubemapFaces == null) {
            initializeCubemapFaces();
        }
        return cubemapFaces;
    }

    /**
     * Releases the graphical resources used by the cubemap faces.
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