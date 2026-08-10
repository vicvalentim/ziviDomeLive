package com.victorvalentim.zividomelive.compat;

import com.victorvalentim.zividomelive.zividomelive;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreviewResolutionPolicyTest {

	@Test
	void sphericalPreviewUsesSmallerWindowDimension() throws Exception {
		assertEquals(720, computePreviewResolution(1280, 720));
		assertEquals(800, computePreviewResolution(800, 1200));
	}

	@Test
	void sphericalPreviewIsCappedAt1024() throws Exception {
		assertEquals(1024, computePreviewResolution(1920, 1080));
		assertEquals(1024, computePreviewResolution(4096, 2160));
	}

	@Test
	void sphericalPreviewIsFlooredAt256() throws Exception {
		assertEquals(256, computePreviewResolution(100, 100));
		assertEquals(256, computePreviewResolution(300, 200));
	}

	private static int computePreviewResolution(int width, int height) throws Exception {
		PApplet applet = new PApplet();
		applet.width = width;
		applet.height = height;

		zividomelive dome = new zividomelive(applet);
		Method method = zividomelive.class.getDeclaredMethod("computePreviewResolution");
		method.setAccessible(true);
		return (int) method.invoke(dome);
	}
}
