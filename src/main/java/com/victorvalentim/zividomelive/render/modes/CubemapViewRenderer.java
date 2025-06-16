package com.victorvalentim.zividomelive.render.modes;

import processing.core.*;
import processing.opengl.PGraphicsOpenGL;

/**
 * The CubemapViewRenderer class handles the creation and rendering of cubemap views.
 */
public class CubemapViewRenderer {
    private int resolution;
    private PGraphicsOpenGL cubemap;
    private final int[] faceRotations = {2, 2, 2, 2, 2, 2};
    private final boolean[] faceInversions = {true, true, true, true, true, true};
    private final PApplet parent;

    /**
     * Constructs a CubemapViewRenderer with the specified parent PApplet and resolution.
     *
     * @param parent the parent PApplet instance
     * @param resolution the resolution of the cubemap
     */
	public CubemapViewRenderer(PApplet parent, int resolution) {
        this.parent = parent;
        this.resolution = resolution;
    }

    /**
     * Initializes or reinitializes the PGraphics object for the cubemap.
     */
    private void initializeCubemap() {
        if (cubemap != null) {
            cubemap.dispose();
        }
        cubemap = (PGraphicsOpenGL) parent.createGraphics(resolution * 2, resolution * 3 / 2, PApplet.P2D);
    }

    /**
     * Updates the resolution of the cubemap.
     *
     * @param newResolution the new resolution to be set
     */
    void updateResolution(int newResolution) {
        if (this.resolution != newResolution) {
            this.resolution = newResolution;
            initializeCubemap();
        }
    }

    /**
     * Returns the PGraphics object for the cubemap.
     *
     * @return the PGraphics object representing the cubemap
     */
    public PGraphicsOpenGL getCubemap() {
        if (cubemap == null) {
            initializeCubemap();
        }
        return (PGraphicsOpenGL) cubemap;
    }

    /**
     * Draws the cubemap texture onto the 2D graphics.
     *
     * @param cubemapTexture the OpenGL texture ID of the cubemap
     */
    public void drawCubemapToGraphics(int cubemapTexture) {
        if (cubemap == null) {
            initializeCubemap();
        }

        cubemap.beginDraw();
        cubemap.background(0, 0);
        // TODO: sample each cubemap face from cubemapTexture
        cubemap.endDraw();
    }

    /**
     * Applies transformations of rotation and inversion to the cubemap faces.
     *
     * @param target the target PGraphics object
     * @param face the PGraphics object representing the cubemap face
     * @param x the x-coordinate for the transformation
     * @param y the y-coordinate for the transformation
     * @param w the width of the face
     * @param h the height of the face
     * @param rotation the number of 90-degree rotations to apply
     * @param invert whether to apply horizontal inversion
     */
    void applyTransformations(PGraphicsOpenGL target, PGraphicsOpenGL face, float x, float y, float w, float h, int rotation, boolean invert) {
        target.pushMatrix();
        target.translate(x + w / 2, y + h / 2);

        for (int i = 0; i < rotation; i++) {
            target.rotate(PApplet.HALF_PI);
        }

        if (invert) {
            target.scale(-1, 1);
        }

        target.imageMode(PApplet.CENTER);
        target.image(face, 0, 0, w, h);
        target.popMatrix();
    }

    /**
     * Releases the graphical resources used by the cubemap.
     */
    public void dispose() {
        if (cubemap != null) {
            cubemap.dispose();
            cubemap = null;
        }
    }
}