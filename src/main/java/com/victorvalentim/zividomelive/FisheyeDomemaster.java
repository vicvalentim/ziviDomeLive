package com.victorvalentim.zividomelive;

import processing.core.*;
import processing.opengl.*;

/**
 * The FisheyeDomemaster class handles the rendering of fisheye domemaster projections from equirectangular maps.
 */
public class FisheyeDomemaster {
    private PGraphics domemaster;
    private PGraphics domemasterSize;
    private PShader domemasterShader;
    private int resolution;
    private float sizePercentage;
    private PApplet parent;

    /**
     * Constructs a FisheyeDomemaster with the specified resolution, shader, and parent PApplet.
     *
     * @param resolution the resolution of the domemaster projection
     * @param shader the PShader object
     * @param parent the parent PApplet instance
     */
    public FisheyeDomemaster(int resolution, PShader shader, PApplet parent) {
        this.resolution = resolution;
        this.sizePercentage = 100.0f;
        this.parent = parent;
        this.domemasterShader = shader;
    }

    /**
     * Initializes or reinitializes the PGraphics object for the domemaster projection.
     */
    private void initializeDomemaster() {
        if (domemaster != null) {
            domemaster.dispose();
        }
        domemaster = parent.createGraphics(resolution, resolution, PApplet.P2D);
        domemaster.smooth(4);
    }

    /**
     * Initializes or reinitializes the PGraphics object for the domemaster size.
     */
    private void initializeDomemasterSize() {
        if (domemasterSize != null) {
            domemasterSize.dispose();
        }
        domemasterSize = parent.createGraphics(resolution, resolution, PApplet.P2D);
        domemasterSize.smooth(4);
    }

    /**
     * Sets the field of view (FOV) for the domemaster shader.
     *
     * @param fov the field of view to set
     */
    void setFOV(float fov) {
        if (domemasterShader == null) {
            initializeDomemaster();
        }
        domemasterShader.set("fov", fov);
    }

    /**
     * Sets the size percentage for the domemaster projection.
     *
     * @param percentage the size percentage to set, constrained between 0 and 100
     */
    public void setSizePercentage(float percentage) {
        sizePercentage = PApplet.constrain(percentage, 0, 100);
    }

    /**
     * Applies the shader to the equirectangular map and renders the domemaster projection.
     *
     * @param equirectangular the PGraphics object representing the equirectangular map
     * @param fov the field of view to use for the shader
     */
    void applyShader(PGraphics equirectangular, float fov) {
        if (equirectangular == null) {
            System.out.println("Equirectangular PGraphics is null.");
            return;
        }

        if (domemaster == null) {
            initializeDomemaster();
        }
        if (domemasterSize == null) {
            initializeDomemasterSize();
        }

        setFOV(fov);

        domemaster.beginDraw();
        domemaster.background(0, 0);
        domemasterShader.set("equirectangularMap", equirectangular);
        domemasterShader.set("resolution", new float[]{domemaster.width, domemaster.height});
        domemaster.shader(domemasterShader);
        domemaster.rect(0, 0, domemaster.width, domemaster.height);
        domemaster.endDraw();

        float adjustedSize = resolution * (sizePercentage / 100.0f);
        domemasterSize.beginDraw();
        domemasterSize.background(0, 0);
        domemasterSize.image(domemaster, (domemasterSize.width - adjustedSize) / 2, (domemasterSize.height - adjustedSize) / 2, adjustedSize, adjustedSize);
        domemasterSize.endDraw();
    }

    /**
     * Returns the PGraphics object for the domemaster projection.
     *
     * @return the PGraphics object representing the domemaster projection
     */
    public PGraphics getDomemasterGraphics() {
        if (domemasterSize == null) {
            initializeDomemasterSize();
        }
        return domemasterSize;
    }

    /**
     * Releases the graphical resources used by the domemaster projection.
     */
    public void dispose() {
        if (domemaster != null) {
            domemaster.dispose();
            domemaster = null;
        }
        if (domemasterSize != null) {
            domemasterSize.dispose();
            domemasterSize = null;
        }
    }
}