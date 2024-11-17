package com.victorvalentim.zividomelive.render.camera;

import processing.core.*;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

/**
 * The MouseControlledCamera class provides a camera that can be controlled using the mouse.
 * It allows for rotation around a center point and zooming in and out.
 */
public class MouseControlledCamera implements PConstants {
    private final PVector position;
    private final PVector center;
    private final PVector up;

    private float distance = 1500;
    private float angleX = PI / 2;
    private float angleY = 0;

    private int lastMouseX = -1; // Tracks the previous mouse X position
    private int lastMouseY = -1; // Tracks the previous mouse Y position
    private boolean dragging = false;

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
    public void apply(PGraphicsOpenGL pg) {
        pg.camera(position.x, position.y, position.z, center.x, center.y, center.z, up.x, up.y, up.z);
    }

    /**
     * Updates the camera's position based on the current angles and distance.
     *
     * @param parent the PApplet instance used for calculations
     */
    public void update(PApplet parent) {
        float cosAngleY = PApplet.cos(angleY);
        position.x = center.x + distance * PApplet.cos(angleX) * cosAngleY;
        position.y = center.y + distance * PApplet.sin(angleY);
        position.z = center.z + distance * PApplet.sin(angleX) * cosAngleY;
    }

    /**
     * Handles mouse events including zoom, drag for rotation, and release for resetting drag state.
     *
     * @param event the MouseEvent object containing details of the mouse event
     */
    public void mouseEvent(MouseEvent event) {
        switch (event.getAction()) {
            case MouseEvent.WHEEL:
                zoom(event.getCount());
                break;

            case MouseEvent.DRAG:
                handleDrag(event);
                break;

            case MouseEvent.RELEASE:
                dragging = false;
                lastMouseX = -1; // Reset last mouse positions
                lastMouseY = -1;
                break;

            default:
                break;
        }
    }

    /**
     * Handles dragging to rotate the camera.
     *
     * @param event the MouseEvent object containing drag details
     */
    private void handleDrag(MouseEvent event) {
        if (isLeftMousePressed(event)) {
            if (!dragging) {
                dragging = true;
                lastMouseX = event.getX();
                lastMouseY = event.getY();
                return;
            }

            float dx = event.getX() - lastMouseX;
            float dy = event.getY() - lastMouseY;
			// Controls the camera rotation speed
			float sensitivity = 0.005f;
			angleX += dx * sensitivity;
            angleY += dy * sensitivity;

            // Clamp the vertical angle to avoid flipping the camera
            angleY = PApplet.constrain(angleY, -HALF_PI, HALF_PI);

            lastMouseX = event.getX();
            lastMouseY = event.getY();
        }
    }

    /**
     * Adjusts the distance for zooming, constrained between min and max distances.
     *
     * @param delta the amount to zoom
     */
    private void zoom(float delta) {
		// Controls the zoom speed
		float zoomSensitivity = 10f;
		distance -= delta * zoomSensitivity;
		// Minimum zoom distance
		float minDistance = 100f;
		// Maximum zoom distance
		float maxDistance = 5000f;
		distance = PApplet.constrain(distance, minDistance, maxDistance);
    }

    /**
     * Checks if the left mouse button is pressed during a mouse event.
     *
     * @param event the MouseEvent to check
     * @return true if the left mouse button is pressed, false otherwise
     */
    private boolean isLeftMousePressed(MouseEvent event) {
        return event.getButton() == LEFT;
    }
}
