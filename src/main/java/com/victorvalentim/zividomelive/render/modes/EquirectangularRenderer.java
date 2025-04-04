package com.victorvalentim.zividomelive.render.modes;

import processing.core.*;
import processing.opengl.*;

/**
 * The EquirectangularRenderer class handles the rendering of equirectangular projections from cubemap faces.
 */
public class EquirectangularRenderer {
    private PGraphics equirectangular;
    private final PShader equirectangularShader;
    private final PApplet parent;
    private final int resolution;

    /**
     * Constructs an EquirectangularRenderer with the specified resolution, shader files, and parent PApplet.
     *
     * @param resolution the resolution of the equirectangular projection
     * @param vertexShaderPath the path to the vertex shader file (.vert)
     * @param fragmentShaderPath the path to the fragment shader file (.frag)
     * @param parent the parent PApplet instance
     */
    public EquirectangularRenderer(int resolution, String fragmentShaderPath, String vertexShaderPath, PApplet parent) {
        this.resolution = resolution;
        this.equirectangularShader = parent.loadShader(fragmentShaderPath, vertexShaderPath);
        this.parent = parent;
    }

    /**
     * Initializes or reinitializes the PGraphics object for the equirectangular projection.
     */
    private void initializeEquirectangular() {
        if (equirectangular != null) {
            equirectangular.dispose();
        }
        equirectangular = (PGraphicsOpenGL) parent.createGraphics(resolution * 2, resolution, PApplet.P2D);
    }

    /**
     * Renders the equirectangular projection from the given cubemap faces.
     *
     * @param faces an array of PGraphics objects representing the cubemap faces
     */
    public void render(PGraphicsOpenGL[] faces) {
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
        equirectangularShader.set("resolution", new float[]{equirectangular.width, equirectangular.height});
        equirectangular.shader(equirectangularShader);
        equirectangular.rect(0, 0, equirectangular.width, equirectangular.height);
        equirectangular.endDraw();
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
            equirectangular.dispose();
            equirectangular = null;
        }
    }
}