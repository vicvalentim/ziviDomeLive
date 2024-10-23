package com.victorvalentim.zividomelive;

import processing.core.*;
import processing.opengl.*;

public class EquirectangularRenderer {
	 private PGraphics equirectangular;
	    private PShader equirectangularShader;
	    EquirectangularRenderer(int resolution, String shaderPath, PApplet parent) {
	        equirectangular = parent.createGraphics(resolution * 2, resolution, PApplet.P2D);
	        equirectangular.smooth(4);
	        equirectangularShader = parent.loadShader(shaderPath);
	    }

	    void render(PGraphics[] faces) {
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

	    public PGraphics getEquirectangular() {
	        return equirectangular;
	    }
}
