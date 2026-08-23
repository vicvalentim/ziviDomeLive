package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/render/camera.

/**
 * Canonical order and camera vectors for the six cubemap faces.
 *
 * <p>The order is part of the qualified skybox layout contract:
 * {@code +X, -X, +Y, -Y, +Z, -Z}. Projection and calibration code relies on
 * these indices staying stable.</p>
 */
enum CubemapFace {
	/** Cubemap face looking along the positive X axis. */
	POSITIVE_X(0, 1f, 0f, 0f, 0f, -1f, 0f),
	/** Cubemap face looking along the negative X axis. */
	NEGATIVE_X(1, -1f, 0f, 0f, 0f, -1f, 0f),
	/** Cubemap face looking along the positive Y axis. */
	POSITIVE_Y(2, 0f, 1f, 0f, 0f, 0f, 1f),
	/** Cubemap face looking along the negative Y axis. */
	NEGATIVE_Y(3, 0f, -1f, 0f, 0f, 0f, -1f),
	/** Cubemap face looking along the positive Z axis. */
	POSITIVE_Z(4, 0f, 0f, 1f, 0f, -1f, 0f),
	/** Cubemap face looking along the negative Z axis. */
	NEGATIVE_Z(5, 0f, 0f, -1f, 0f, -1f, 0f);

	private static final CubemapFace[] ORDERED = values();

	private final int index;
	private final float centerX;
	private final float centerY;
	private final float centerZ;
	private final float upX;
	private final float upY;
	private final float upZ;

	CubemapFace(
			int index,
			float centerX,
			float centerY,
			float centerZ,
			float upX,
			float upY,
			float upZ) {
		this.index = index;
		this.centerX = centerX;
		this.centerY = centerY;
		this.centerZ = centerZ;
		this.upX = upX;
		this.upY = upY;
		this.upZ = upZ;
	}

	/**
	 * Returns the stable cubemap-face index.
	 *
	 * @return index in {@code +X, -X, +Y, -Y, +Z, -Z} order
	 */
	public int index() {
		return index;
	}

	/**
	 * Returns the number of cubemap faces.
	 *
	 * @return always {@code 6}
	 */
	public static int count() {
		return ORDERED.length;
	}

	/**
	 * Resolves a cubemap face by stable index without allocating.
	 *
	 * @param index face index in {@code +X, -X, +Y, -Y, +Z, -Z} order
	 * @return cubemap face at the given index
	 */
	public static CubemapFace at(int index) {
		return ORDERED[index];
	}

	/**
	 * Returns the X component of the face center vector.
	 *
	 * @return center X component
	 */
	public float centerX() {
		return centerX;
	}

	/**
	 * Returns the Y component of the face center vector.
	 *
	 * @return center Y component
	 */
	public float centerY() {
		return centerY;
	}

	/**
	 * Returns the Z component of the face center vector.
	 *
	 * @return center Z component
	 */
	public float centerZ() {
		return centerZ;
	}

	/**
	 * Returns the X component of the face up vector.
	 *
	 * @return up X component
	 */
	public float upX() {
		return upX;
	}

	/**
	 * Returns the Y component of the face up vector.
	 *
	 * @return up Y component
	 */
	public float upY() {
		return upY;
	}

	/**
	 * Returns the Z component of the face up vector.
	 *
	 * @return up Z component
	 */
	public float upZ() {
		return upZ;
	}

	CameraOrientation createCameraOrientation() {
		return new CameraOrientation(0f, 0f, 0f, centerX, centerY, centerZ, upX, upY, upZ);
	}
}
