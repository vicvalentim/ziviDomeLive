package com.victorvalentim.zividomelive;

import processing.core.*;
import processing.event.MouseEvent;

public class MouseControlledCamera {
    PVector position; // Position of the camera
    PVector center; // The point the camera is looking at
    PVector up; // The up direction of the camera
    float sensitivity = 0.005f; // Sensitivity for mouse movements
    float zoomSensitivity = 10; // Sensitivity for zoom actions
    float distance = 1500; // Initial distance from the center point
    float angleX = PConstants.PI / 2; // Horizontal rotation angle
    float angleY = 0; // Vertical rotation angle
    boolean dragging = false; // Flag to indicate if the mouse is being dragged
    float minDistance = 100; // Minimum zoom distance
    float maxDistance = 5000; // Maximum zoom distance

    MouseControlledCamera() {
        // Initialize the position, center, and up vector
        position = new PVector(0, 0, distance);
        center = new PVector(0, 0, 0);
        up = new PVector(0, 1, 0);
    }

    void apply(PGraphics pg) {
        // Set the camera view for the given PGraphics object
        pg.camera(position.x, position.y, position.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    void update(PApplet p) {
        // Update the camera's position based on mouse input
        if (isLeftMousePressed(p)) {
            if (!dragging) {
                dragging = true;
                return;
            }
            // Calculate the change in angles based on mouse movement
            float dx = (p.mouseX - p.pmouseX) * sensitivity;
            float dy = (p.mouseY - p.pmouseY) * sensitivity;
            angleX += dx;
            angleY += dy;
        } else {
            dragging = false;
        }

        // Update the camera's position based on the angles
        float cosAngleY = PApplet.cos(angleY);
        position.x = center.x + distance * PApplet.cos(angleX) * cosAngleY;
        position.y = center.y + distance * PApplet.sin(angleY);
        position.z = center.z + distance * PApplet.sin(angleX) * cosAngleY;
    }

    void zoom(float delta) {
        // Adjust the distance for zooming, constrained between min and max distances
        distance -= delta * zoomSensitivity;
        distance = PApplet.constrain(distance, minDistance, maxDistance);
    }

    void mouseWheel(MouseEvent event) {
        // Handle zooming using the mouse wheel
        float e = event.getCount();
        zoom(e);
    }

    void mouseDragged(PApplet p) {
        // Update the camera angles when the mouse is dragged
        if (isLeftMousePressed(p)) {
            float dx = (p.mouseX - p.pmouseX) * sensitivity;
            float dy = (p.mouseY - p.pmouseY) * sensitivity;
            angleX += dx;
            angleY += dy;
        }
    }

    void mouseReleased() {
        // Reset dragging flag when the mouse is released
        dragging = false;
    }

    private boolean isLeftMousePressed(PApplet p) {
        // Check if the left mouse button is pressed
        return p.mousePressed && p.mouseButton == PConstants.LEFT;
    }
}
