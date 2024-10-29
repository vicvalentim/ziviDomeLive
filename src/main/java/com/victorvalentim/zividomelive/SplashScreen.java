package com.victorvalentim.zividomelive;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;

/**
 * The SplashScreen class is responsible for rendering an animated splash screen with
 * a central rotating sphere, orbiting cubes, and a library title.
 */
public class SplashScreen {
    int opacity = 255;
    float orbitRadius = 160;
    int numCubes = 13;
    float[] speeds;
    boolean showSplash = true;
    boolean fading = false;
    long startTime;
    int displayDuration = 7000;
    PApplet p;
    PGraphics backgroundLayer; // Camada para o fundo gradiente
    PGraphics animationLayer;  // Camada para os elementos animados

    /**
     * Constructs a SplashScreen instance.
     *
     * @param p the PApplet instance used for rendering
     */
    public SplashScreen(PApplet p) {
        this.p = p;
        speeds = new float[numCubes];
        for (int i = 0; i < numCubes; i++) {
            speeds[i] = 0.0008f + p.random(-0.0002f, 0.0002f);
        }
        backgroundLayer = p.createGraphics(p.width, p.height, PConstants.P3D);
        animationLayer = p.createGraphics(p.width, p.height, PConstants.P3D);
    }

    /**
     * Starts the splash screen animation.
     */
    public void start() {
        showSplash = true;
        fading = false;
        opacity = 255;
        startTime = p.millis();
    }

    /**
     * Updates the splash screen state. Starts fade-out after a set duration or on user interaction.
     */
    public void update() {
        if (!fading && p.millis() - startTime > displayDuration) {
            fading = true;
        }
        if (fading) {
            opacity -= 1;
            if (opacity <= 0) {
                opacity = 0;
                showSplash = false;
            }
        }
    }

    /**
     * Renders the background gradient layer with current opacity.
     */
    private void renderBackground() {
        backgroundLayer.beginDraw();
        backgroundLayer.clear();
        for (int i = 0; i < p.height; i++) {
            float inter = PApplet.map(i, 0, p.height, 0, 1);
            int gradColor = p.lerpColor(p.color(0, 0, 20, opacity), p.color(0, 0, 60, opacity), inter);
            backgroundLayer.stroke(gradColor);
            backgroundLayer.line(0, i, p.width, i);
        }
        backgroundLayer.endDraw();
    }

    /**
     * Renders the animated elements layer, including the rotating sphere and orbiting cubes.
     */
    private void renderAnimations() {
        animationLayer.beginDraw();
        animationLayer.clear();
        animationLayer.noFill();
        animationLayer.strokeWeight(1);

        float rotationAngleX = PApplet.radians(p.millis() * 0.02f);
        float rotationAngleY = PApplet.radians(p.millis() * 0.015f);
        float rotationAngleZ = PApplet.radians(p.millis() * 0.01f);

        // Renderiza a esfera central
        animationLayer.pushMatrix();
        animationLayer.translate(p.width / 2f, p.height / 2f, 0);
        animationLayer.rotateX(rotationAngleX);
        animationLayer.rotateY(rotationAngleY);
        animationLayer.rotateZ(rotationAngleZ);

        int rings = 16;
        int segments = 32;
        float radius = 120;

        for (int i = 0; i < rings; i++) {
            float theta = PApplet.map(i, 0, rings - 1, -PConstants.PI / 2, PConstants.PI / 2);
            float ringRadius = PApplet.cos(theta) * radius;
            float y = PApplet.sin(theta) * radius;

            int lineColor = p.lerpColor(p.color(100, 200, 255, 50), p.color(0, 120, 255, 150), PApplet.abs(PApplet.sin(p.millis() * 0.0005f + i)));
            animationLayer.stroke(lineColor, opacity);

            animationLayer.beginShape();
            for (int j = 0; j <= segments; j++) {
                float phi = PApplet.map(j, 0, segments, 0, PConstants.TWO_PI);
                float x = PApplet.cos(phi) * ringRadius;
                float z = PApplet.sin(phi) * ringRadius;
                animationLayer.vertex(x, y, z);
            }
            animationLayer.endShape();
        }
        animationLayer.popMatrix();

        // Renderiza os cubos orbitando ao redor da esfera
        for (int i = 0; i < numCubes; i++) {
            float rotationOffset = PConstants.TWO_PI / numCubes * i;
            float initialAngleOffset = PConstants.TWO_PI / numCubes * i;
            float zOffset = (i % 2 == 0 ? 1 : -1) * (orbitRadius * 0.2f);

            float angle = p.millis() * speeds[i] + initialAngleOffset;

            float x = p.width / 2f;
            float y = p.height / 2f + orbitRadius * PApplet.cos(angle);
            float z = orbitRadius * PApplet.sin(angle) + zOffset;

            animationLayer.pushMatrix();
            animationLayer.translate(p.width / 2f, p.height / 2f, 0);
            animationLayer.rotateZ(rotationOffset);
            animationLayer.translate(0, y - p.height / 2f, z);

            animationLayer.stroke(0, 200, 255, opacity);
            animationLayer.strokeWeight(1.5f);
            animationLayer.box(8);
            animationLayer.popMatrix();
        }

        // Renderiza o texto "ziviDomeLive" abaixo da esfera
        animationLayer.pushMatrix();
        animationLayer.translate(p.width / 2f, p.height / 2f + radius + 40);
        animationLayer.textAlign(PConstants.CENTER, PConstants.CENTER);
        animationLayer.textSize(32);
        animationLayer.fill(255, opacity);
        animationLayer.text("ziviDomeLive", 0, 0);
        animationLayer.popMatrix();

        animationLayer.endDraw();
    }

    /**
     * Renders the combined splash screen by overlaying the animation layer on the background layer.
     */
    public void render() {
        if (!showSplash) return;

        renderBackground(); // Atualiza o fundo com opacidade
        renderAnimations();
        p.image(backgroundLayer, 0, 0); // Desenha o fundo gradiente
        p.image(animationLayer, 0, 0); // Desenha os elementos animados sobre o fundo
    }

    /**
     * Initiates fade-out when the screen is clicked.
     */
    public void mousePressed() {
        if (!fading) {
            fading = true;
        }
    }
}
