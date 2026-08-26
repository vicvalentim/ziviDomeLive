package com.victorvalentim.zividomelive.core.environment;

import com.victorvalentim.zividomelive.core.math.Quaternion;
import com.victorvalentim.zividomelive.core.projection.SphericalOrientation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentStateTest {

    private static final float EPSILON = 1.0e-5f;

    @Test
    void defaultsAndResetDescribeVisibleUnitEnvironment() {
        EnvironmentState state = new EnvironmentState();
        assertDefaults(state);
        state.setVisible(false);
        state.setIntensity(2.0f);
        state.setYawOffset(0.5f);
        state.setSourceOrientation(Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.7f));
        state.reset();
        assertDefaults(state);
    }

    @Test
    void intensityIsNonNegativeAndNonFiniteValuesAreIgnored() {
        EnvironmentState state = new EnvironmentState();
        state.setIntensity(-3.0f);
        assertEquals(0.0f, state.getIntensity());
        state.setIntensity(1.5f);
        state.setIntensity(Float.NaN);
        assertEquals(1.5f, state.getIntensity());
        state.setIntensity(Float.POSITIVE_INFINITY);
        assertEquals(1.5f, state.getIntensity());
    }

    @Test
    void yawPreservesFiniteMultiTurnValuesAndIgnoresNonFiniteValues() {
        EnvironmentState state = new EnvironmentState();
        float multiTurn = (float) Math.PI * 4.0f + 0.25f;
        state.setYawOffset(multiTurn);
        state.setYawOffset(Float.NEGATIVE_INFINITY);
        assertEquals(multiTurn, state.getYawOffset());
    }

    @Test
    void sourceOrientationIsNormalizedAndNullMeansIdentity() {
        EnvironmentState state = new EnvironmentState();
        Quaternion value = new Quaternion(0.0f, 2.0f, 0.0f, 2.0f);
        state.setSourceOrientation(value);
        Quaternion stored = state.getSourceOrientation();
        float magnitude = (float) Math.sqrt(stored.y() * stored.y() + stored.w() * stored.w());
        assertEquals(1.0f, magnitude, EPSILON);
        state.setSourceOrientation(null);
        assertSame(Quaternion.identity(), state.getSourceOrientation());
    }

    @Test
    void sourceOrientationDoesNotCoupleToSphericalControls() {
        EnvironmentState environment = new EnvironmentState();
        Quaternion source = Quaternion.fromAxisAngle(1.0f, 0.0f, 0.0f, 0.5f);
        environment.setSourceOrientation(source);
        SphericalOrientation spherical = new SphericalOrientation();
        spherical.setPitch(1.0f);
        spherical.setYaw(2.0f);
        spherical.setRoll(3.0f);

        assertSame(source, environment.getSourceOrientation());
    }

    @Test
    void allNonFiniteScalarsAreIgnoredWithoutMutation() {
        EnvironmentState state = new EnvironmentState();
        state.setIntensity(2.5f);
        state.setYawOffset(-0.75f);

        for (float invalid : new float[]{
                Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            state.setIntensity(invalid);
            state.setYawOffset(invalid);
            assertEquals(2.5f, state.getIntensity());
            assertEquals(-0.75f, state.getYawOffset());
        }
    }

    @Test
    void invalidOrientationNormalizationDoesNotReplaceTheOwnedValue() {
        EnvironmentState state = new EnvironmentState();
        Quaternion valid = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.5f);
        state.setSourceOrientation(valid);

        assertThrows(IllegalStateException.class,
                () -> state.setSourceOrientation(new Quaternion(0.0f, 0.0f, 0.0f, 0.0f)));

        assertSame(valid, state.getSourceOrientation());
    }

    private static void assertDefaults(EnvironmentState state) {
        assertTrue(state.isVisible());
        assertEquals(1.0f, state.getIntensity());
        assertEquals(0.0f, state.getYawOffset());
        assertSame(Quaternion.identity(), state.getSourceOrientation());
        state.setVisible(false);
        assertFalse(state.isVisible());
        state.setVisible(true);
    }
}
