package com.victorvalentim.zividomelive.scene;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.opengl.PGraphicsOpenGL;
import processing.event.MouseEvent;

import java.util.logging.Logger;

/**
 * The DefaultScene provides a visually appealing experience for ziviDomeLive,
 * featuring dynamic animations and interactivity.
 */
public class DefaultScene implements Scene {

	private final Logger logger = LogManager.getLogger();
	private final PApplet p;

	private final int numCubes = 13;
	private final float[] speeds;
	private final float orbitRadius = 160;
	private final int opacity = 255;

	/**
	 * Constructs the DefaultScene instance.
	 *
	 * @param p the PApplet instance used for rendering
	 */
	public DefaultScene(PApplet p) {
		this.p = p;
		speeds = new float[numCubes];
		for (int i = 0; i < numCubes; i++) {
			speeds[i] = 0.0008f + p.random(-0.0002f, 0.0002f);
		}
	}
	
	public void setupScene() {
		logger.info("Default scene setup completed.");
	}

	/**
	 * Renders the main scene with lighting to highlight the objects.
	 *
	 * @param pg the PGraphicsOpenGL object used for rendering
	 */
	public void sceneRender(PGraphicsOpenGL pg) {
		pg.beginDraw();

		// Posiciona o sistema de coordenadas para (0, 0, 300)
		pg.translate(0, 250, 250);
		pg.rotateX(-PConstants.HALF_PI);

		// Renderiza a esfera central e objetos orbitais
		renderRotatingSphere(pg);
		renderOrbitingCubes(pg);

		// Renderiza texto
		renderText(pg);

		pg.endDraw();
	}

	public void update() {
		// Atualizações contínuas podem ser aplicadas aqui, como animações
	}


	public void mouseEvent(MouseEvent event) {
		if (event.getAction() == MouseEvent.CLICK) {
			logger.info("Mouse clicked in default scene.");
		}
	}


	public void dispose() {
		logger.info("Default scene resources released.");
	}


	public String getName() {
		return "Default Scene";
	}

	/**
	 * Renders the central rotating sphere.
	 *
	 * @param pg the PGraphicsOpenGL object used for rendering
	 */
	private void renderRotatingSphere(PGraphicsOpenGL pg) {
		pg.pushMatrix();
		pg.clear();
		pg.noFill();
		pg.strokeWeight(2.0f);

		float rotationAngleX = PApplet.radians(p.millis() * 0.02f);
		float rotationAngleY = PApplet.radians(p.millis() * 0.015f);
		float rotationAngleZ = PApplet.radians(p.millis() * 0.01f);

		pg.rotateX(rotationAngleX);
		pg.rotateY(rotationAngleY);
		pg.rotateZ(rotationAngleZ);

		int rings = 16;
		int segments = 32;
		float radius = 120;

		for (int i = 0; i < rings; i++) {
			float theta = PApplet.map(i, 0, rings - 1, -PConstants.PI / 2, PConstants.PI / 2);
			float ringRadius = PApplet.cos(theta) * radius;
			float y = PApplet.sin(theta) * radius;

			int lineColor = p.lerpColor(p.color(100, 200, 255, 50), p.color(0, 120, 255, 150), PApplet.abs(PApplet.sin(p.millis() * 0.0005f + i)));
			pg.stroke(lineColor, opacity);
			pg.noFill();
			pg.beginShape();
			for (int j = 0; j <= segments; j++) {
				float phi = PApplet.map(j, 0, segments, 0, PConstants.TWO_PI);
				float x = PApplet.cos(phi) * ringRadius;
				float z = PApplet.sin(phi) * ringRadius;
				pg.vertex(x, y, z);
			}
			pg.endShape();
		}

		pg.popMatrix();
	}

	/**
	 * Renders cubes orbiting around the central sphere.
	 *
	 * @param pg the PGraphicsOpenGL object used for rendering
	 */
	private void renderOrbitingCubes(PGraphicsOpenGL pg) {
		for (int i = 0; i < numCubes; i++) {
			float rotationOffset = PConstants.TWO_PI / numCubes * i;
			float initialAngleOffset = PConstants.TWO_PI / numCubes * i;
			float zOffset = (i % 2 == 0 ? 1 : -1) * (orbitRadius * 0.2f);

			float angle = p.millis() * speeds[i] + initialAngleOffset;

			float y = orbitRadius * PApplet.cos(angle);
			float z = orbitRadius * PApplet.sin(angle) + zOffset;

			pg.pushMatrix();
			pg.rotateZ(rotationOffset);
			pg.translate(0, y, z);
			pg.stroke(0, 200, 255, opacity);
			pg.strokeWeight(1.5f);
			pg.noFill();
			pg.box(8);

			pg.popMatrix();
		}
	}

	/**
	 * Renders the text "ziviDomeLive" below the central sphere.
	 *
	 * @param pg the PGraphicsOpenGL object used for rendering
	 */
	private void renderText(PGraphicsOpenGL pg) {
		pg.pushMatrix();
		pg.translate(0, 200, 0);
		pg.textAlign(PConstants.CENTER, PConstants.CENTER);
		pg.fill(255, opacity);
		pg.textSize(32);
		pg.text("ziviDomeLive", 0, 0);
		pg.popMatrix();
	}
}
