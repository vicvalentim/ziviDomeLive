package com.victorvalentim.zividomelive.manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlP5KeyEventBridgeTest {

    @Test
    void acceptsOnlyIndicesRepresentableByControlP5KeyStateArray() {
        assertTrue(ControlP5KeyEventBridge.isIndexableControlP5KeyCode(0));
        assertTrue(ControlP5KeyEventBridge.isIndexableControlP5KeyCode(1));
        assertTrue(ControlP5KeyEventBridge.isIndexableControlP5KeyCode(1023));

        assertFalse(ControlP5KeyEventBridge.isIndexableControlP5KeyCode(-1));
        assertFalse(ControlP5KeyEventBridge.isIndexableControlP5KeyCode(-431));
        assertFalse(ControlP5KeyEventBridge.isIndexableControlP5KeyCode(1024));
        assertFalse(ControlP5KeyEventBridge.isIndexableControlP5KeyCode(65406));
    }
}
