package com.victorvalentim.zividomelive.render.camera;

/**
 * The CameraOrientation class represents the orientation of a camera in 3D space.
 * It includes the position of the camera, the point it is looking at, and the up direction.
 */
public class CameraOrientation {
    float eyeX, eyeY, eyeZ;
    public float centerX;
	public float centerY;
	public float centerZ;
    public float upX;
	public float upY;
	public float upZ;

    /**
     * Constructs a CameraOrientation with the specified parameters.
     *
     * @param eyeX    the x-coordinate of the camera eye position
     * @param eyeY    the y-coordinate of the camera eye position
     * @param eyeZ    the z-coordinate of the camera eye position
     * @param centerX the x-coordinate of the point the camera is looking at
     * @param centerY the y-coordinate of the point the camera is looking at
     * @param centerZ the z-coordinate of the point the camera is looking at
     * @param upX     the x-component of the camera's up direction
     * @param upY     the y-component of the camera's up direction
     * @param upZ     the z-component of the camera's up direction
     */
    CameraOrientation(float eyeX, float eyeY, float eyeZ,
                      float centerX, float centerY, float centerZ,
                      float upX, float upY, float upZ) {
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