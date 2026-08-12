package com.victorvalentim.zividomelive.render.modes;


import com.victorvalentim.zividomelive.render.gl.CubemapTarget;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;
import java.util.logging.Logger;

/**
 * Renders fisheye domemaster projections directly from a native samplerCube cubemap.
 */
public class FisheyeDomemaster {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CUBEMAP_TEXTURE_UNIT = 1;
    private PGraphics domemaster;
    private PGraphics domemasterSize;
    private final PShader samplerCubeShader;
    private final int resolution;
    private float sizePercentage;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();

    /**
     * Constructs a FisheyeDomemaster with the specified resolution, samplerCube shader files, and parent PApplet.
     *
     * @param resolution the resolution of the domemaster projection
     * @param fragmentShaderPath the samplerCube fragment shader file (.frag)
     * @param vertexShaderPath the samplerCube vertex shader file (.vert)
     * @param parent the parent PApplet instance
     */
    public FisheyeDomemaster(int resolution,String fragmentShaderPath, String vertexShaderPath, PApplet parent) {
        this.resolution = resolution;
        this.sizePercentage = 100.0f;
        this.parent = parent;
        this.samplerCubeShader = fragmentShaderPath != null && vertexShaderPath != null
                ? parent.loadShader(fragmentShaderPath, vertexShaderPath)
                : null;
    }

    /**
     * Initializes or reinitializes the PGraphics object for the domemaster projection.
     */
    private void initializeDomemaster() {
        if (domemaster != null) {
            glAdapter.dispose(domemaster);
        }
        domemaster = glAdapter.createGraphics(parent, resolution, resolution, PApplet.P2D);
    }

    /**
     * Initializes or reinitializes the PGraphics object for the domemaster size.
     */
    private void initializeDomemasterSize() {
        if (domemasterSize != null) {
            glAdapter.dispose(domemasterSize);
        }
        domemasterSize = glAdapter.createGraphics(parent, resolution, resolution, PApplet.P2D);
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
     * Returns the current size percentage of the domemaster projection.
     *
     * @return the size percentage, between 0 and 100
     */
    public float getSizePercentage() {
        return sizePercentage;
    }

    /**
     * Applies the domemaster shader directly to a native cubemap.
     *
     * @param nativeCubemap native cubemap populated by {@code CubemapRenderer}
     * @param fov the field of view to use for the shader
     */
    public void applyShader(CubemapTarget nativeCubemap, float fov) {
        if (nativeCubemap == null || !nativeCubemap.isAllocated() || samplerCubeShader == null) {
            LOGGER.warning("Native cubemap or fisheye samplerCube shader unavailable; skipping shader pass.");
            return;
        }
        try {
            applySamplerCubeShader(nativeCubemap, fov);
        } catch (RuntimeException error) {
            LOGGER.warning("Native fisheye samplerCube render failed: "
                    + error.getMessage());
        }
    }

    private void applySamplerCubeShader(CubemapTarget nativeCubemap, float fov) {
        if (domemaster == null) {
            initializeDomemaster();
        }
        if (domemasterSize == null) {
            initializeDomemasterSize();
        }

        PGraphicsOpenGL target = (PGraphicsOpenGL) domemaster;
        target.beginDraw();
        boolean cubemapBound = false;
        try {
            target.background(0, 0); // Set transparent background
            samplerCubeShader.set("fov", fov);
            samplerCubeShader.set("resolution", new float[]{target.width, target.height});
            samplerCubeShader.set("cubemap", CUBEMAP_TEXTURE_UNIT);
            target.shader(samplerCubeShader);
            glAdapter.bindCubemapTexture(target, nativeCubemap, CUBEMAP_TEXTURE_UNIT);
            cubemapBound = true;
            target.rect(0, 0, target.width, target.height);
        } finally {
            try {
                if (cubemapBound) {
                    glAdapter.unbindCubemapTexture(target, CUBEMAP_TEXTURE_UNIT);
                }
            } finally {
                target.endDraw();
            }
        }

        applySizePass();
    }

    private void applySizePass() {
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
    public PGraphicsOpenGL getDomemasterGraphics() {
        if (domemasterSize == null) {
            initializeDomemasterSize();
        }
        return (PGraphicsOpenGL) domemasterSize;
    }

    /**
     * Releases the graphical resources used by the domemaster projection.
     */
    public void dispose() {
        if (domemaster != null) {
            glAdapter.dispose(domemaster);
            domemaster = null;
        }
        if (domemasterSize != null) {
            glAdapter.dispose(domemasterSize);
            domemasterSize = null;
        }
    }
}
