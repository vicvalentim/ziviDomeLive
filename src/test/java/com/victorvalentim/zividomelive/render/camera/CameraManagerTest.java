package com.victorvalentim.zividomelive.render.camera;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CameraManager}.
 *
 * <p>The class under test has no Processing or OpenGL dependency, so these
 * tests run entirely in plain Java with no GPU context. The test is placed in
 * the same package as the production class so that package-private fields
 * ({@code orientations}) are accessible.</p>
 */
class CameraManagerTest {

    private CameraManager manager;

    @BeforeEach
    void setUp() {
        manager = new CameraManager();
    }

    // -----------------------------------------------------------------------
    // Construction / count
    // -----------------------------------------------------------------------

    @Test
    void constructor_createsSixOrientations() {
        assertEquals(6, manager.orientations.size(),
                "A cubemap has exactly 6 faces, so 6 orientations are required");
    }

    // -----------------------------------------------------------------------
    // Per-face direction vectors
    // The 6 faces correspond to the ±X, ±Y, ±Z directions of the cubemap.
    // Each orientation is: eye at origin, center towards the face axis, up as defined.
    // -----------------------------------------------------------------------

    @Test
    void orientation0_pointsToPositiveX() {
        CameraOrientation o = manager.getOrientation(0);
        assertEquals( 1f, o.centerX, 0f);
        assertEquals( 0f, o.centerY, 0f);
        assertEquals( 0f, o.centerZ, 0f);
        assertEquals( 0f, o.upX, 0f);
        assertEquals(-1f, o.upY, 0f);
        assertEquals( 0f, o.upZ, 0f);
    }

    @Test
    void orientation1_pointsToNegativeX() {
        CameraOrientation o = manager.getOrientation(1);
        assertEquals(-1f, o.centerX, 0f);
        assertEquals( 0f, o.centerY, 0f);
        assertEquals( 0f, o.centerZ, 0f);
        assertEquals( 0f, o.upX, 0f);
        assertEquals(-1f, o.upY, 0f);
        assertEquals( 0f, o.upZ, 0f);
    }

    @Test
    void orientation2_pointsToPositiveY() {
        CameraOrientation o = manager.getOrientation(2);
        assertEquals( 0f, o.centerX, 0f);
        assertEquals( 1f, o.centerY, 0f);
        assertEquals( 0f, o.centerZ, 0f);
        assertEquals( 0f, o.upX, 0f);
        assertEquals( 0f, o.upY, 0f);
        assertEquals( 1f, o.upZ, 0f);
    }

    @Test
    void orientation3_pointsToNegativeY() {
        CameraOrientation o = manager.getOrientation(3);
        assertEquals( 0f, o.centerX, 0f);
        assertEquals(-1f, o.centerY, 0f);
        assertEquals( 0f, o.centerZ, 0f);
        assertEquals( 0f, o.upX, 0f);
        assertEquals( 0f, o.upY, 0f);
        assertEquals(-1f, o.upZ, 0f);
    }

    @Test
    void orientation4_pointsToPositiveZ() {
        CameraOrientation o = manager.getOrientation(4);
        assertEquals( 0f, o.centerX, 0f);
        assertEquals( 0f, o.centerY, 0f);
        assertEquals( 1f, o.centerZ, 0f);
        assertEquals( 0f, o.upX, 0f);
        assertEquals(-1f, o.upY, 0f);
        assertEquals( 0f, o.upZ, 0f);
    }

    @Test
    void orientation5_pointsToNegativeZ() {
        CameraOrientation o = manager.getOrientation(5);
        assertEquals( 0f, o.centerX, 0f);
        assertEquals( 0f, o.centerY, 0f);
        assertEquals(-1f, o.centerZ, 0f);
        assertEquals( 0f, o.upX, 0f);
        assertEquals(-1f, o.upY, 0f);
        assertEquals( 0f, o.upZ, 0f);
    }

    // -----------------------------------------------------------------------
    // Eye position — always at origin for all faces
    // -----------------------------------------------------------------------

    @Test
    void allOrientations_eyeIsAtOrigin() {
        for (int i = 0; i < 6; i++) {
            CameraOrientation o = manager.getOrientation(i);
            assertEquals(0f, o.eyeX, 0f, "Face " + i + ": eyeX must be 0");
            assertEquals(0f, o.eyeY, 0f, "Face " + i + ": eyeY must be 0");
            assertEquals(0f, o.eyeZ, 0f, "Face " + i + ": eyeZ must be 0");
        }
    }

    // -----------------------------------------------------------------------
    // Distinctness — no two faces look in the same direction
    // -----------------------------------------------------------------------

    @Test
    void allOrientations_haveDistinctCenterVectors() {
        for (int i = 0; i < 6; i++) {
            CameraOrientation oi = manager.getOrientation(i);
            for (int j = i + 1; j < 6; j++) {
                CameraOrientation oj = manager.getOrientation(j);
                boolean sameCenterVector =
                        oi.centerX == oj.centerX &&
                        oi.centerY == oj.centerY &&
                        oi.centerZ == oj.centerZ;
                assertFalse(sameCenterVector,
                        "Orientations " + i + " and " + j + " must point in different directions");
            }
        }
    }

    // -----------------------------------------------------------------------
    // dispose
    // -----------------------------------------------------------------------

    @Test
    void dispose_clearsAllOrientations() {
        manager.dispose();
        assertTrue(manager.orientations.isEmpty(),
                "dispose() must clear all orientations");
    }

    @Test
    void dispose_doesNotThrow() {
        assertDoesNotThrow(() -> manager.dispose());
    }

    // -----------------------------------------------------------------------
    // reinitializeOrientations
    // -----------------------------------------------------------------------

    @Test
    void initializeOrientations_afterDispose_restoresSixOrientations() {
        manager.dispose();
        manager.initializeOrientations();
        assertEquals(6, manager.orientations.size(),
                "initializeOrientations() must restore the full set of 6 orientations");
    }
}
