package com.victorvalentim.zividomelive.core.projection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectionStateTest {

    @Test
    void projectionIdentityContainsOnlyFinalViewsInQualifiedOrder() {
        assertArrayEquals(new ProjectionType[]{
                ProjectionType.STANDARD,
                ProjectionType.DOMEMASTER,
                ProjectionType.EQUIRECTANGULAR,
                ProjectionType.SKYBOX
        }, ProjectionType.values());
    }

    @Test
    void domemasterDefaultsAndResetMatchFacadeCalibration() {
        DomemasterSettings settings = new DomemasterSettings();
        assertEquals(210.0f, settings.getFieldOfViewDegrees());
        assertEquals(100.0f, settings.getSizePercent());
        settings.setFieldOfViewDegrees(90.0f);
        settings.setSizePercent(50.0f);
        settings.reset();
        assertEquals(210.0f, settings.getFieldOfViewDegrees());
        assertEquals(100.0f, settings.getSizePercent());
    }

    @Test
    void domemasterCalibrationClampsFiniteValues() {
        DomemasterSettings settings = new DomemasterSettings();
        settings.setFieldOfViewDegrees(-1.0f);
        settings.setSizePercent(-1.0f);
        assertEquals(0.0f, settings.getFieldOfViewDegrees());
        assertEquals(0.0f, settings.getSizePercent());
        settings.setFieldOfViewDegrees(361.0f);
        settings.setSizePercent(101.0f);
        assertEquals(360.0f, settings.getFieldOfViewDegrees());
        assertEquals(100.0f, settings.getSizePercent());
    }

    @Test
    void domemasterCalibrationPreservesLastFiniteValue() {
        DomemasterSettings settings = new DomemasterSettings();
        settings.setFieldOfViewDegrees(225.0f);
        settings.setSizePercent(42.5f);
        settings.setFieldOfViewDegrees(Float.NaN);
        settings.setSizePercent(Float.POSITIVE_INFINITY);
        assertEquals(225.0f, settings.getFieldOfViewDegrees());
        assertEquals(42.5f, settings.getSizePercent());
    }
}
