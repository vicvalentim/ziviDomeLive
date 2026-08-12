package com.victorvalentim.zividomelive.render.modes;

import com.victorvalentim.zividomelive.render.gl.CubemapTarget;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.*;
import processing.opengl.*;

import java.util.logging.Logger;

/**
 * Renders an equirectangular projection directly from a native samplerCube cubemap.
 */
public class EquirectangularRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CUBEMAP_TEXTURE_UNIT = 1;
    private PGraphics equirectangular;
    private final PShader samplerCubeShader;
    private final PApplet parent;
    private final int resolution;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();

    /**
     * Constructs an EquirectangularRenderer with the specified resolution, samplerCube shader files, and parent PApplet.
     *
     * @param resolution the resolution of the equirectangular projection
     * @param fragmentShaderPath the samplerCube fragment shader file (.frag)
     * @param vertexShaderPath the samplerCube vertex shader file (.vert)
     * @param parent the parent PApplet instance
     */
    public EquirectangularRenderer(int resolution, String fragmentShaderPath, String vertexShaderPath, PApplet parent) {
        this.resolution = resolution;
        this.samplerCubeShader = fragmentShaderPath != null && vertexShaderPath != null
                ? parent.loadShader(fragmentShaderPath, vertexShaderPath)
                : null;
        this.parent = parent;
    }

    /**
     * Initializes or reinitializes the PGraphics object for the equirectangular projection.
     */
    private void initializeEquirectangular() {
        if (equirectangular != null) {
            glAdapter.dispose(equirectangular);
        }
        equirectangular = glAdapter.createGraphics(parent, resolution * 2, resolution, PApplet.P2D);
    }

    /**
     * Renders the equirectangular projection from a native cubemap.
     *
     * @param nativeCubemap native cubemap populated by {@code CubemapRenderer}
     */
    public void render(CubemapTarget nativeCubemap) {
        if (nativeCubemap == null || !nativeCubemap.isAllocated() || samplerCubeShader == null) {
            LOGGER.warning("Native cubemap or equirectangular samplerCube shader unavailable; skipping render.");
            return;
        }
        try {
            renderSamplerCube(nativeCubemap);
        } catch (RuntimeException error) {
            LOGGER.warning("Native equirectangular samplerCube render failed: "
                    + error.getMessage());
        }
    }

    private void renderSamplerCube(CubemapTarget nativeCubemap) {
        if (equirectangular == null) {
            initializeEquirectangular();
        }

        PGraphicsOpenGL target = (PGraphicsOpenGL) equirectangular;
        target.beginDraw();
        boolean cubemapBound = false;
        try {
            target.background(0, 0);
            samplerCubeShader.set("resolution", target.width, target.height);
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
    }

    /**
     * Returns the PGraphics object for the equirectangular projection.
     *
     * @return the PGraphics object representing the equirectangular projection
     */
    public PGraphicsOpenGL getEquirectangular() {
        if (equirectangular == null) {
            initializeEquirectangular();
        }
        return (PGraphicsOpenGL) equirectangular;
    }

    /**
     * Releases the graphical resources used by the equirectangular projection.
     */
    public void dispose() {
        if (equirectangular != null) {
            glAdapter.dispose(equirectangular);
            equirectangular = null;
        }
    }
}
