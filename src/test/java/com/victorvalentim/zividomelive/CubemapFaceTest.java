package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CubemapFaceTest {

	@Test
	void valuesRemainInQualifiedCubemapLayoutOrder() {
		assertArrayEquals(new CubemapFace[]{
				CubemapFace.POSITIVE_X,
				CubemapFace.NEGATIVE_X,
				CubemapFace.POSITIVE_Y,
				CubemapFace.NEGATIVE_Y,
				CubemapFace.POSITIVE_Z,
				CubemapFace.NEGATIVE_Z
		}, CubemapFace.values());
	}

	@Test
	void countMatchesCubemapFaceCount() {
		assertEquals(6, CubemapFace.count());
	}

	@Test
	void indexedLookupMatchesEnumOrder() {
		for (CubemapFace face : CubemapFace.values()) {
			assertEquals(face, CubemapFace.at(face.index()));
		}
	}

	@Test
	void canonicalFaceVectorsRemainExact() {
		assertFace(CubemapFace.POSITIVE_X, 1f, 0f, 0f, 0f, -1f, 0f);
		assertFace(CubemapFace.NEGATIVE_X, -1f, 0f, 0f, 0f, -1f, 0f);
		assertFace(CubemapFace.POSITIVE_Y, 0f, 1f, 0f, 0f, 0f, 1f);
		assertFace(CubemapFace.NEGATIVE_Y, 0f, -1f, 0f, 0f, 0f, -1f);
		assertFace(CubemapFace.POSITIVE_Z, 0f, 0f, 1f, 0f, -1f, 0f);
		assertFace(CubemapFace.NEGATIVE_Z, 0f, 0f, -1f, 0f, -1f, 0f);
	}

	private static void assertFace(
			CubemapFace face,
			float centerX,
			float centerY,
			float centerZ,
			float upX,
			float upY,
			float upZ) {
		assertEquals(centerX, face.centerX(), 0f, face + " centerX");
		assertEquals(centerY, face.centerY(), 0f, face + " centerY");
		assertEquals(centerZ, face.centerZ(), 0f, face + " centerZ");
		assertEquals(upX, face.upX(), 0f, face + " upX");
		assertEquals(upY, face.upY(), 0f, face + " upY");
		assertEquals(upZ, face.upZ(), 0f, face + " upZ");
	}
}
