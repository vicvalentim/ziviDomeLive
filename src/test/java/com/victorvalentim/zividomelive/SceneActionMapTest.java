package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SceneActionMapTest {

    @Test
    void dispatchesNamedKeyAndKeyCodeActions() {
        SceneActionMap actions = new SceneActionMap();
        AtomicInteger calls = new AtomicInteger();
        actions.bindKeyPressed("reload", 'R', calls::incrementAndGet);
        actions.bindKeyCodePressed("left", 37, calls::incrementAndGet);

        assertTrue(actions.dispatch(keyEvent('R', 0)));
        assertTrue(actions.dispatch(keyEvent((char) 0, 37)));
        assertEquals(2, calls.get());
        assertTrue(actions.trigger("reload"));
        assertEquals(3, calls.get());
    }

    @Test
    void ignoresReleaseAndUnboundInput() {
        SceneActionMap actions = new SceneActionMap();
        actions.bindKeyPressed("reload", 'R', () -> {});
        KeyEvent release = new KeyEvent(null, 0, KeyEvent.RELEASE, 0, 'R', 0);

        assertFalse(actions.dispatch(release));
        assertFalse(actions.dispatch(keyEvent('x', 0)));
    }

    @Test
    void dispatchesMouseBindingsAndCloseClearsThem() {
        SceneActionMap actions = new SceneActionMap();
        AtomicInteger x = new AtomicInteger();
        actions.bindMouse("pick", MouseEvent.PRESS, event -> x.set(event.getX()));
        MouseEvent press = new MouseEvent(null, 0, MouseEvent.PRESS, 0, 42, 8, 1, 1);

        assertTrue(actions.dispatch(press));
        assertEquals(42, x.get());
        actions.close();
        assertFalse(actions.dispatch(press));
        assertThrows(IllegalStateException.class,
                () -> actions.register("late", () -> {}));
    }

    private static KeyEvent keyEvent(char key, int keyCode) {
        return new KeyEvent(null, 0, KeyEvent.PRESS, 0, key, keyCode);
    }
}
