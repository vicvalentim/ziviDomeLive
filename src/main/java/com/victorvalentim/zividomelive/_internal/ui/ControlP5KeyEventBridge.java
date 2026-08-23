package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/ui.

import controlP5.ControlP5;
import controlP5.ControlWindow;
import processing.event.KeyEvent;

import java.util.Objects;

/**
 * Internal compatibility guard for ControlP5 2.2.6 key codes.
 *
 * <p>ControlP5's {@link ControlWindow#keyEvent(KeyEvent)} indexes a fixed
 * {@code boolean[1024]} with the platform key code before controller dispatch.
 * Linux dead keys/AltGr and some platform modifier events can report negative
 * or greater-than-1023 values and therefore crash that method.</p>
 *
 * <p>This object and its package-private owner are deliberately not registered with Processing.
 * The public {@link ziviDomeLive} callback delegates here through the owner.</p>
 */
final class ControlP5KeyEventBridge {

    /** ControlP5 2.2.6 hard-codes numKeys = 1024 in ControlWindow. */
    static final int CONTROL_P5_KEY_CAPACITY = 1024;

    private final ControlWindow controlWindow;

    ControlP5KeyEventBridge(ControlP5 controlP5) {
        Objects.requireNonNull(controlP5, "controlP5 cannot be null");
        this.controlWindow = Objects.requireNonNull(
                controlP5.getWindow(),
                "ControlP5 window cannot be null");
    }

    /**
     * Dispatches a Processing key event through the safe ControlP5 path.
     *
     * @param event Processing key event
     */
    void dispatch(KeyEvent event) {
        if (event == null) {
            return;
        }

        if (isIndexableControlP5KeyCode(event.getKeyCode())) {
            controlWindow.keyEvent(event);
            return;
        }

        /*
         * Preserve delivery to controllers while bypassing only ControlP5's
         * unsafe keys[keyCode] bookkeeping and shortcut map.
         */
        controlWindow.handleKeyEvent(event);
    }

    /**
     * Returns whether {@code keyCode} can safely index ControlP5 2.2.6's
     * internal key-state array.
     */
    static boolean isIndexableControlP5KeyCode(int keyCode) {
        return keyCode >= 0 && keyCode < CONTROL_P5_KEY_CAPACITY;
    }
}
