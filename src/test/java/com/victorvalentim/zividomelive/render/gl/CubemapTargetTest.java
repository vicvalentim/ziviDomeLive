package com.victorvalentim.zividomelive.render.gl;

import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.opengl.PGL;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubemapTargetTest {

	@Test
	void validatesPositiveResolution() {
		assertThrows(IllegalArgumentException.class, () -> CubemapTarget.validateResolution(0));
		assertThrows(IllegalArgumentException.class, () -> CubemapTarget.validateResolution(-1));
		assertDoesNotThrow(() -> CubemapTarget.validateResolution(1));
	}

	@Test
	void faceTargetsFollowCanonicalCubemapFaceOrder() {
		assertEquals(PGL.TEXTURE_CUBE_MAP_POSITIVE_X,
				CubemapTarget.glTargetFor(CubemapFace.POSITIVE_X));
		assertEquals(PGL.TEXTURE_CUBE_MAP_NEGATIVE_X,
				CubemapTarget.glTargetFor(CubemapFace.NEGATIVE_X));
		assertEquals(PGL.TEXTURE_CUBE_MAP_POSITIVE_Y,
				CubemapTarget.glTargetFor(CubemapFace.POSITIVE_Y));
		assertEquals(PGL.TEXTURE_CUBE_MAP_NEGATIVE_Y,
				CubemapTarget.glTargetFor(CubemapFace.NEGATIVE_Y));
		assertEquals(PGL.TEXTURE_CUBE_MAP_POSITIVE_Z,
				CubemapTarget.glTargetFor(CubemapFace.POSITIVE_Z));
		assertEquals(PGL.TEXTURE_CUBE_MAP_NEGATIVE_Z,
				CubemapTarget.glTargetFor(CubemapFace.NEGATIVE_Z));
	}

	@Test
	void nullFaceIsRejected() {
		assertThrows(NullPointerException.class, () -> CubemapTarget.glTargetFor(null));
	}

        @Test
        void failedDisposeRetainsResourceOwnershipForRetry() throws Exception {
                CubemapTarget target = targetWithResourceIds(
                                512,
                                101,
                                102,
                                103);

                assertTrue(target.isAllocated());
                assertDoesNotThrow(target::ensureAllocated);

                assertThrows(IllegalStateException.class, target::dispose);

                assertEquals(103, target.textureId());
                assertTrue(target.isAllocated());
                assertDoesNotThrow(target::ensureAllocated);
        }

        private static CubemapTarget targetWithResourceIds(
                        int resolution,
                        int framebufferId,
                        int renderbufferId,
                        int textureId) throws Exception {

                Constructor<CubemapTarget> constructor =
                                CubemapTarget.class.getDeclaredConstructor(
                                                PApplet.class,
                                                ProcessingGlAdapter.class,
                                                int.class,
                                                int.class,
                                                int.class,
                                                int.class);

                constructor.setAccessible(true);

                return constructor.newInstance(
                                new PApplet(),
                                ProcessingGlAdapter.getDefault(),
                                resolution,
                                framebufferId,
                                renderbufferId,
                                textureId);
        }

}
