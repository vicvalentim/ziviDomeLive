package com.victorvalentim.zividomelive;

public class CameraOrientation {
	   float eyeX, eyeY, eyeZ;
	    float centerX, centerY, centerZ;
	    float upX, upY, upZ;

	    /**
	     * Constructor for CameraOrientation.
	     *
	     * @param eyeX     The x-coordinate of the camera eye position.
	     * @param eyeY     The y-coordinate of the camera eye position.
	     * @param eyeZ     The z-coordinate of the camera eye position.
	     * @param centerX  The x-coordinate of the point the camera is looking at.
	     * @param centerY  The y-coordinate of the point the camera is looking at.
	     * @param centerZ  The z-coordinate of the point the camera is looking at.
	     * @param upX      The x-component of the camera's up direction.
	     * @param upY      The y-component of the camera's up direction.
	     * @param upZ      The z-component of the camera's up direction.
	     */
	    CameraOrientation(float eyeX, float eyeY, float eyeZ,
	                      float centerX, float centerY, float centerZ,
	                      float upX, float upY, float upZ) {
	        // Set the position of the camera, the point it is looking at, and the up direction
	        this.eyeX = eyeX;
	        this.eyeY = eyeY;
	        this.eyeZ = eyeZ;
	        this.centerX = centerX;
	        this.centerY = centerY;
	        this.centerZ = centerZ;
	        this.upX = upX;
	        this.upY = upY;
	        this.upZ = upZ;
	    }
}
