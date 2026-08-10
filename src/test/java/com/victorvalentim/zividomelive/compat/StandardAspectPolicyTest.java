package com.victorvalentim.zividomelive.compat;

import com.victorvalentim.zividomelive.zividomelive;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class StandardAspectPolicyTest {

	@Test
	void autoLandscapeAspectSnapsToKnownFamilies() throws Exception {
		assertArrayEquals(new int[]{1024, 576}, dimensions(1920, 1080, 1024,
				zividomelive.StandardOutputAspectMode.AUTO));
		assertArrayEquals(new int[]{2048, 1280}, dimensions(1440, 900, 2048,
				zividomelive.StandardOutputAspectMode.AUTO));
		assertArrayEquals(new int[]{3072, 2304}, dimensions(1024, 768, 3072,
				zividomelive.StandardOutputAspectMode.AUTO));
	}

	@Test
	void portraitOrientationSwapsSelectedFamilyDimensions() throws Exception {
		assertArrayEquals(new int[]{576, 1024}, dimensions(1080, 1920, 1024,
				zividomelive.StandardOutputAspectMode.AUTO));
		assertArrayEquals(new int[]{1536, 2048}, dimensions(768, 1024, 2048,
				zividomelive.StandardOutputAspectMode.AUTO));
	}

	@Test
	void explicitAspectModeOverridesWindowAspect() throws Exception {
		assertArrayEquals(new int[]{4096, 3072}, dimensions(1920, 1080, 4096,
				zividomelive.StandardOutputAspectMode.ASPECT_4_3));
		assertArrayEquals(new int[]{1024, 1024}, dimensions(1920, 1080, 1024,
				zividomelive.StandardOutputAspectMode.ASPECT_1_1));
	}

	private static int[] dimensions(
			int width,
			int height,
			int outputResolution,
			zividomelive.StandardOutputAspectMode mode) throws Exception {
		PApplet applet = new PApplet();
		applet.width = width;
		applet.height = height;

		zividomelive dome = new zividomelive(applet);

		Field resolution = zividomelive.class.getDeclaredField("outputResolution");
		resolution.setAccessible(true);
		resolution.setInt(dome, outputResolution);
		dome.setStandardOutputAspectMode(mode);

		Method method = zividomelive.class.getDeclaredMethod("computeStandardOutputDimensions");
		method.setAccessible(true);
		return (int[]) method.invoke(dome);
	}
}
