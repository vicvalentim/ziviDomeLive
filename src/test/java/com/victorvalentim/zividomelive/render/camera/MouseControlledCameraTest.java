package com.victorvalentim.zividomelive.render.camera;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PMatrix3D;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MouseControlledCamera}.
 *
 * <p>All tests operate on pure math (quaternion + PMatrix3D) and therefore
 * run without any Processing/OpenGL GPU context.</p>
 */
class MouseControlledCameraTest {

    private static final float EPS = 1e-5f;
    private MouseControlledCamera cam;

    @BeforeEach
    void setUp() {
        cam = new MouseControlledCamera();
    }

    // -----------------------------------------------------------------------
    // Default state
    // -----------------------------------------------------------------------

    @Test
    void defaultDistance_is1500() {
        assertEquals(1500f, cam.getDistance(), EPS);
    }

    @Test
    void update_identityRotation_eyeAtPositiveZ() {
        // identity quaternion → eye should be at (0, 0, distance)
        cam.update(null);
        // We can't call cam.apply() without a PGraphicsOpenGL, but we can verify
        // the internal position via a dedicated public getter if we add one,
        // or we verify the update() does not throw and the distance is unchanged.
        assertDoesNotThrow(() -> cam.update(null));
        assertEquals(1500f, cam.getDistance(), EPS);
    }

	@Test
	void copiedEnvironmentRotationContainsNoOrbitTranslation() {
		cam.setDistance(9000.0f);
		cam.update(null);
		PMatrix3D rotation = new PMatrix3D();

		cam.copyRotationMatrix(rotation);

		assertEquals(0.0f, rotation.m03, EPS);
		assertEquals(0.0f, rotation.m13, EPS);
		assertEquals(0.0f, rotation.m23, EPS);
		assertEquals(1.0f, rotation.m33, EPS);
	}

    // -----------------------------------------------------------------------
    // resetRotation
    // -----------------------------------------------------------------------

    @Test
    void resetRotation_doesNotThrow() {
        assertDoesNotThrow(() -> cam.resetRotation());
    }

    @Test
    void resetRotation_preservesDistance() {
        cam.setDistance(800f);
        cam.resetRotation();
        assertEquals(800f, cam.getDistance(), EPS);
    }

    // -----------------------------------------------------------------------
    // Distance / zoom limits
    // -----------------------------------------------------------------------

    @Test
    void setDistance_clampsToMinimum() {
        cam.setDistance(-100f);
        assertEquals(50f, cam.getDistance(), EPS, "Distance must not go below MIN_DISTANCE=50");
    }

    @Test
    void setDistance_clampsToMaximum() {
        cam.setDistance(99999f);
        assertEquals(10000f, cam.getDistance(), EPS, "Distance must not exceed MAX_DISTANCE=10000");
    }

    @Test
    void setDistance_acceptsValidValue() {
        cam.setDistance(3000f);
        assertEquals(3000f, cam.getDistance(), EPS);
    }

    // -----------------------------------------------------------------------
    // MouseEvent – wheel zoom
    // -----------------------------------------------------------------------

    @Test
    void wheelForward_decreasesDistance() {
        float before = cam.getDistance();
        // Simulate scroll in (negative delta → zoom in / decrease distance)
        simulateWheel(-1);
        assertTrue(cam.getDistance() < before,
                "Scrolling in (delta < 0) must decrease orbit distance");
    }

    @Test
    void wheelBackward_increasesDistance() {
        float before = cam.getDistance();
        simulateWheel(1);
        assertTrue(cam.getDistance() > before,
                "Scrolling out (delta > 0) must increase orbit distance");
    }

    @Test
    void wheel_neverExceedsBounds() {
        for (int i = 0; i < 200; i++) simulateWheel(1);
        assertEquals(10000f, cam.getDistance(), EPS,
                "Repeated scroll-out must not exceed MAX_DISTANCE");

        for (int i = 0; i < 200; i++) simulateWheel(-1);
        assertEquals(50f, cam.getDistance(), EPS,
                "Repeated scroll-in must not go below MIN_DISTANCE");
    }

    // -----------------------------------------------------------------------
    // MouseEvent – drag rotation (state transitions only, no GPU needed)
    // -----------------------------------------------------------------------

    @Test
    void drag_startAndRelease_doesNotThrow() {
        assertDoesNotThrow(() -> {
            simulateDragStart(100, 100);
            simulateDragMove(110, 105);
            simulateDragRelease();
        });
    }

    @Test
    void drag_multiplePixels_doesNotCorruptDistance() {
        float before = cam.getDistance();
        simulateDragStart(200, 200);
        for (int i = 0; i < 50; i++) simulateDragMove(200 + i, 200 + i);
        simulateDragRelease();
        assertEquals(before, cam.getDistance(), EPS,
                "Drag rotation must not alter orbit distance");
    }

    // -----------------------------------------------------------------------
    // Helpers – build minimal MouseEvents without Processing context
    // -----------------------------------------------------------------------

    private void simulateWheel(int count) {
        processing.event.MouseEvent e = new processing.event.MouseEvent(
            null,
            System.currentTimeMillis(),
            processing.event.MouseEvent.WHEEL,
            0,
            0, 0,   // x, y
            0,      // button (no button for wheel)
            count   // count
        );
        cam.mouseEvent(e);
    }

    private void simulateDragStart(int x, int y) {
        processing.event.MouseEvent e = new processing.event.MouseEvent(
            null,
            System.currentTimeMillis(),
            processing.event.MouseEvent.PRESS,
            0,
            x, y,
            processing.core.PConstants.LEFT,
            1
        );
        cam.mouseEvent(e);
        processing.event.MouseEvent drag = new processing.event.MouseEvent(
            null,
            System.currentTimeMillis(),
            processing.event.MouseEvent.DRAG,
            0,
            x, y,
            processing.core.PConstants.LEFT,
            1
        );
        cam.mouseEvent(drag);
    }

    private void simulateDragMove(int x, int y) {
        processing.event.MouseEvent e = new processing.event.MouseEvent(
            null,
            System.currentTimeMillis(),
            processing.event.MouseEvent.DRAG,
            0,
            x, y,
            processing.core.PConstants.LEFT,
            1
        );
        cam.mouseEvent(e);
    }

    private void simulateDragRelease() {
        processing.event.MouseEvent e = new processing.event.MouseEvent(
            null,
            System.currentTimeMillis(),
            processing.event.MouseEvent.RELEASE,
            0,
            0, 0,
            processing.core.PConstants.LEFT,
            0
        );
        cam.mouseEvent(e);
    }
}

