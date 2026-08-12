package com.victorvalentim.zividomelive.render.modes;

import com.victorvalentim.zividomelive.render.gl.CubemapTarget;
import com.victorvalentim.zividomelive.render.gl.ProcessingGlAdapter;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.*;
import processing.opengl.*;

import java.util.logging.Logger;

/**
 * The EquirectangularRenderer class handles the rendering of equirectangular projections from cubemap faces.
 */
public class EquirectangularRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CUBEMAP_TEXTURE_UNIT = 1;
    private PGraphics equirectangular;
    private final PShader equirectangularShader;
    private final PShader samplerCubeShader;
    private final PApplet parent;
    private final int resolution;
    private final ProcessingGlAdapter glAdapter = ProcessingGlAdapter.getDefault();

    /**
     * Constructs an EquirectangularRenderer with the specified resolution, shader files, and parent PApplet.
     *
     * @param resolution the resolution of the equirectangular projection
     * @param vertexShaderPath the path to the vertex shader file (.vert)
     * @param fragmentShaderPath the path to the fragment shader file (.frag)
     * @param parent the parent PApplet instance
     */
    public EquirectangularRenderer(int resolution, String fragmentShaderPath, String vertexShaderPath, PApplet parent) {
        this(resolution, fragmentShaderPath, vertexShaderPath, null, null, parent);
    }

    /**
     * Constructs an EquirectangularRenderer with legacy and native samplerCube shader files.
     *
     * @param resolution the resolution of the equirectangular projection
     * @param fragmentShaderPath the legacy fragment shader file (.frag)
     * @param vertexShaderPath the legacy vertex shader file (.vert)
     * @param samplerCubeFragmentShaderPath the samplerCube fragment shader file (.frag)
     * @param samplerCubeVertexShaderPath the samplerCube vertex shader file (.vert)
     * @param parent the parent PApplet instance
     */
    public EquirectangularRenderer(
            int resolution,
            String fragmentShaderPath,
            String vertexShaderPath,
            String samplerCubeFragmentShaderPath,
            String samplerCubeVertexShaderPath,
            PApplet parent) {
        this.resolution = resolution;
        this.equirectangularShader = parent.loadShader(fragmentShaderPath, vertexShaderPath);
        this.samplerCubeShader = samplerCubeFragmentShaderPath != null && samplerCubeVertexShaderPath != null
                ? parent.loadShader(samplerCubeFragmentShaderPath, samplerCubeVertexShaderPath)
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
     * Renders the equirectangular projection from the given cubemap faces.
     *
     * @param faces an array of PGraphics objects representing the cubemap faces
     */
    public void render(PGraphicsOpenGL[] faces) {
        if (equirectangularShader == null) {
            LOGGER.warning("Equirectangular shader not initialized; skipping render.");
            return;
        }
        if (!hasValidFaces(faces)) {
            LOGGER.warning("Cubemap faces unavailable; skipping equirectangular render.");
            return;
        }
        if (equirectangular == null) {
            initializeEquirectangular();
        }

        equirectangular.beginDraw();
        equirectangular.background(0, 0);
        equirectangularShader.set("posX", faces[0]);
        equirectangularShader.set("negX", faces[1]);
        equirectangularShader.set("posY", faces[2]);
        equirectangularShader.set("negY", faces[3]);
        equirectangularShader.set("posZ", faces[4]);
        equirectangularShader.set("negZ", faces[5]);
        equirectangularShader.set("resolution", equirectangular.width, equirectangular.height);
        equirectangular.shader(equirectangularShader);
        equirectangular.rect(0, 0, equirectangular.width, equirectangular.height);
        equirectangular.endDraw();
    }

    /**
     * Renders the equirectangular projection from a native cubemap when available.
     *
     * <p>The Processing face array remains the fallback source until all spherical
     * projection renderers have migrated to samplerCube.</p>
     *
     * @param nativeCubemap native cubemap populated by {@code CubemapRenderer}
     * @param fallbackFaces legacy Processing face targets used when native sampling is unavailable
     */
    public void render(CubemapTarget nativeCubemap, PGraphicsOpenGL[] fallbackFaces) {
        if (nativeCubemap == null || !nativeCubemap.isAllocated() || samplerCubeShader == null) {
            render(fallbackFaces);
            return;
        }
        try {
            renderSamplerCube(nativeCubemap);
        } catch (RuntimeException error) {
            LOGGER.warning("Native equirectangular samplerCube render failed; falling back to Processing faces: "
                    + error.getMessage());
            render(fallbackFaces);
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

    private boolean hasValidFaces(PGraphicsOpenGL[] faces) {
        if (faces == null || faces.length < 6) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (!glAdapter.hasTexture(faces[i])) {
                return false;
            }
        }
        return true;
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
