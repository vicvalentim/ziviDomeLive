package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/render/modes.


import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;
import java.util.logging.Logger;

/**
 * Renders fisheye domemaster projections directly from a native samplerCube cubemap.
 */
class FisheyeDomemaster {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CUBEMAP_TEXTURE_UNIT = 1;
    private PGraphics domemaster;
    private final PShader samplerCubeShader;
    private final int resolution;
    private float sizePercentage;
    private final PApplet parent;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();
    private final ProcessingGlAdapter.CubemapBindingState cubemapBindingState =
            new ProcessingGlAdapter.CubemapBindingState();
    private boolean unavailableWarningLogged;
    private boolean renderFailureWarningLogged;

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
        clearDomemaster();
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
            clearDomemasterAfterFailure(null);
            if (!unavailableWarningLogged) {
                LOGGER.warning("Native cubemap or fisheye samplerCube shader unavailable; skipping shader pass.");
                unavailableWarningLogged = true;
            }
            return;
        }
        try {
            applySamplerCubeShader(nativeCubemap, fov);
            unavailableWarningLogged = false;
            renderFailureWarningLogged = false;
        } catch (RuntimeException error) {
            clearDomemasterAfterFailure(error);
            if (!renderFailureWarningLogged) {
                LOGGER.warning("Native fisheye samplerCube render failed: "
                        + error.getMessage());
                renderFailureWarningLogged = true;
            }
        }
    }

    private void applySamplerCubeShader(CubemapTarget nativeCubemap, float fov) {
        if (domemaster == null) {
            initializeDomemaster();
        }

        PGraphicsOpenGL target = (PGraphicsOpenGL) domemaster;
        target.beginDraw();
        boolean cubemapBound = false;
        boolean shaderTouched = false;
        try {
            target.clear();
            target.noStroke();
            target.blendMode(PApplet.REPLACE);
            samplerCubeShader.set("fov", fov);
            samplerCubeShader.set("sizePercentage", sizePercentage);
            samplerCubeShader.set("resolution", target.width, target.height);
            samplerCubeShader.set("cubemap", CUBEMAP_TEXTURE_UNIT);
            shaderTouched = true;
            target.shader(samplerCubeShader);
            glAdapter.bindCubemapTextureScoped(
                    target, nativeCubemap, CUBEMAP_TEXTURE_UNIT, cubemapBindingState);
            cubemapBound = true;
            target.rect(0, 0, target.width, target.height);
        } finally {
            try {
                if (cubemapBound) {
                    glAdapter.restoreCubemapTexture(target, cubemapBindingState);
                }
            } finally {
                try {
                    if (shaderTouched) {
                        target.resetShader();
                    }
                } finally {
                    target.endDraw();
                }
            }
        }
    }

    private void clearDomemaster() {
        if (domemaster == null) {
            return;
        }
        domemaster.beginDraw();
        try {
            domemaster.clear();
        } finally {
            domemaster.endDraw();
        }
    }

    private void clearDomemasterAfterFailure(RuntimeException failure) {
        try {
            clearDomemaster();
        } catch (RuntimeException clearError) {
            if (failure != null) {
                failure.addSuppressed(clearError);
            }
        }
    }

    /**
     * Returns the PGraphics object for the domemaster projection.
     *
     * @return the PGraphics object representing the domemaster projection
     */
    public PGraphicsOpenGL getDomemasterGraphics() {
        if (domemaster == null) {
            initializeDomemaster();
        }
        return (PGraphicsOpenGL) domemaster;
    }

    /**
     * Releases the graphical resources used by the domemaster projection.
     */
    public void dispose() {
        if (domemaster != null) {
            glAdapter.dispose(domemaster);
            domemaster = null;
        }
    }
}
