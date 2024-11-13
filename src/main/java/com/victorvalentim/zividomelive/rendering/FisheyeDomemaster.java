package com.victorvalentim.zividomelive.rendering;


import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PShader;

/**
 * The FisheyeDomemaster class handles the rendering of fisheye domemaster projections from equirectangular maps.
 */
public class FisheyeDomemaster {
    private PGraphics domemaster;
    private PGraphics domemasterSize;
    private final PShader domemasterShader;
    private final int resolution;
    private float sizePercentage;
    private final PApplet parent;

    /**
     * Constructs a FisheyeDomemaster with the specified resolution, shader files, and parent PApplet.
     *
     * @param resolution the resolution of the domemaster projection
     * @param fragmentShaderPath the path to the fragment shader file (.frag)
     * @param vertexShaderPath the path to the vertex shader file (.vert)
     * @param parent the parent PApplet instance
     */
    public FisheyeDomemaster(int resolution,String fragmentShaderPath, String vertexShaderPath, PApplet parent) {
        this.resolution = resolution;
        this.sizePercentage = 100.0f;
        this.parent = parent;
        this.domemasterShader = parent.loadShader(fragmentShaderPath, vertexShaderPath);
    }

    /**
     * Initializes or reinitializes the PGraphics object for the domemaster projection.
     */
    private void initializeDomemaster() {
        if (domemaster != null) {
            domemaster.dispose();
        }
        domemaster = parent.createGraphics(resolution, resolution, PApplet.P2D);
    }

    /**
     * Initializes or reinitializes the PGraphics object for the domemaster size.
     */
    private void initializeDomemasterSize() {
        if (domemasterSize != null) {
            domemasterSize.dispose();
        }
        domemasterSize = parent.createGraphics(resolution, resolution, PApplet.P2D);
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
        assert domemasterShader != null;
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
	public void applyShader(PGraphics equirectangular, float fov) {
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
        domemaster.background(0, 0); // Set transparent background
        domemasterShader.set("equirectangularMap", equirectangular);
        domemasterShader.set("resolution", new float[]{domemaster.width, domemaster.height});
        domemaster.shader(domemasterShader);
        domemaster.rect(0, 0, domemaster.width, domemaster.height);
        domemaster.endDraw();

        float adjustedSize = resolution * (sizePercentage / 100.0f);
        domemasterSize.beginDraw();
        domemasterSize.background(0, 0); // Set transparent background
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