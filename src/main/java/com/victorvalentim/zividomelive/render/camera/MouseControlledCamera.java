package com.victorvalentim.zividomelive.render.camera;

import processing.core.*;
import processing.event.MouseEvent;

/**
 * The MouseControlledCamera class provides a camera that can be controlled using the mouse.
 * It allows for rotation around a center point and zooming in and out.
 */
public class MouseControlledCamera {
    PVector position;
    PVector center;
    PVector up;
    float sensitivity = 0.005f;
    float zoomSensitivity = 10;
    float distance = 1500;
    float angleX = PConstants.PI / 2;
    float angleY = 0;
    boolean dragging = false;
    float minDistance = 100;
    float maxDistance = 5000;

    /**
     * Constructs a MouseControlledCamera with default settings.
     */
	public MouseControlledCamera() {
        position = new PVector(0, 0, distance);
        center = new PVector(0, 0, 0);
        up = new PVector(0, 1, 0);
    }

    /**
     * Applies the camera view to the given PGraphics object.
     *
     * @param pg the PGraphics object to apply the camera view to
     */
	public void apply(PGraphics pg) {
        pg.camera(position.x, position.y, position.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    /**
     * Updates the camera's position based on mouse input.
     *
     * @param p the PApplet instance to get mouse input from
     */
	public void update(PApplet p) {
        if (isLeftMousePressed(p)) {
            if (!dragging) {
                dragging = true;
                return;
            }
            float dx = (p.mouseX - p.pmouseX) * sensitivity;
            float dy = (p.mouseY - p.pmouseY) * sensitivity;
            angleX += dx;
            angleY += dy;
        } else {
            dragging = false;
        }

        float cosAngleY = PApplet.cos(angleY);
        position.x = center.x + distance * PApplet.cos(angleX) * cosAngleY;
        position.y = center.y + distance * PApplet.sin(angleY);
        position.z = center.z + distance * PApplet.sin(angleX) * cosAngleY;
    }

    /**
     * Adjusts the distance for zooming, constrained between min and max distances.
     *
     * @param delta the amount to zoom
     */
    void zoom(float delta) {
        distance -= delta * zoomSensitivity;
        distance = PApplet.constrain(distance, minDistance, maxDistance);
    }

    /**
     * Handles zooming using the mouse wheel.
     *
     * @param event the MouseEvent containing the wheel movement
     */
	public void mouseWheel(MouseEvent event) {
        float e = event.getCount();
        zoom(e);
    }

    /**
     * Updates the camera angles when the mouse is dragged.
     *
     * @param p the PApplet instance to get mouse input from
     */
    void mouseDragged(PApplet p) {
        if (isLeftMousePressed(p)) {
            float dx = (p.mouseX - p.pmouseX) * sensitivity;
            float dy = (p.mouseY - p.pmouseY) * sensitivity;
            angleX += dx;
            angleY += dy;
        }
    }

    /**
     * Resets the dragging flag when the mouse is released.
     */
    void mouseReleased() {
        dragging = false;
    }

    /**
     * Checks if the left mouse button is pressed.
     *
     * @param p the PApplet instance to get mouse input from
     * @return true if the left mouse button is pressed, false otherwise
     */
    private boolean isLeftMousePressed(PApplet p) {
        return p.mousePressed && p.mouseButton == PConstants.LEFT;
    }
}