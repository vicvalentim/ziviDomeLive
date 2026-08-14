package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.opengl.PGraphicsOpenGL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SceneManager}.
 *
 * <p>All tests use a lightweight {@link FakeScene} stub that requires no
 * OpenGL context and tracks how many times {@code setupScene()} has been
 * called. This lets us verify scene lifecycle behaviour without any
 * Processing or GPU dependency.</p>
 */
class SceneManagerTest {

    private SceneManager manager;

    @BeforeEach
    void setUp() {
        manager = new SceneManager();
    }

    // -----------------------------------------------------------------------
    // Initial state
    // -----------------------------------------------------------------------

    @Test
    void newManager_hasNoCurrentScene() {
        assertNull(manager.getCurrentScene());
    }

    @Test
    void newManager_hasZeroSceneCount() {
        assertEquals(0, manager.getSceneCount());
    }

    // -----------------------------------------------------------------------
    // registerScene
    // -----------------------------------------------------------------------

    @Test
    void registerNullScene_isIgnoredSilently() {
        manager.registerScene(null);
        assertEquals(0, manager.getSceneCount());
        assertNull(manager.getCurrentScene());
    }

    @Test
    void registerFirstScene_becomesCurrentAndSetupIsCalled() {
        FakeScene scene = new FakeScene("A");
        manager.registerScene(scene);

        assertSame(scene, manager.getCurrentScene());
        assertEquals(1, scene.setupCount,
                "registerScene should call setupScene() on the first scene");
    }

    @Test
    void registerSecondScene_doesNotChangeCurrentScene() {
        FakeScene first = new FakeScene("A");
        FakeScene second = new FakeScene("B");
        manager.registerScene(first);
        manager.registerScene(second);

        assertSame(first, manager.getCurrentScene(),
                "Current scene should remain the first registered scene");
        assertEquals(0, second.setupCount,
                "setupScene() must not be called on scenes that are merely registered");
    }

    @Test
    void registerMultipleScenes_countIsCorrect() {
        manager.registerScene(new FakeScene("A"));
        manager.registerScene(new FakeScene("B"));
        manager.registerScene(new FakeScene("C"));
        assertEquals(3, manager.getSceneCount());
    }

    // -----------------------------------------------------------------------
    // nextScene
    // -----------------------------------------------------------------------

    @Test
    void nextScene_withNoScenes_doesNotThrow() {
        assertDoesNotThrow(() -> manager.nextScene());
    }

    @Test
    void nextScene_withOneScene_doesNotCallSetupAgain() {
        FakeScene only = new FakeScene("Only");
        manager.registerScene(only);
        int setupBefore = only.setupCount;

        manager.nextScene();

        assertEquals(setupBefore, only.setupCount,
                "nextScene() on a single-scene manager must not call setupScene() again");
        assertSame(only, manager.getCurrentScene());
    }

    @Test
    void nextScene_withTwoScenes_switchesToSecondAndCallsSetup() {
        FakeScene first = new FakeScene("A");
        FakeScene second = new FakeScene("B");
        manager.registerScene(first);
        manager.registerScene(second);

        manager.nextScene();

        assertSame(second, manager.getCurrentScene());
        assertEquals(1, second.setupCount,
                "nextScene() must call setupScene() on the new scene");
    }

    @Test
    void nextScene_doesNotCallSetupOnPreviousScene() {
        FakeScene first = new FakeScene("A");
        FakeScene second = new FakeScene("B");
        manager.registerScene(first);
        manager.registerScene(second);
        int firstSetupBefore = first.setupCount; // 1 (from registerScene)

        manager.nextScene();

        assertEquals(firstSetupBefore, first.setupCount,
                "The previously active scene must not have setupScene() called on switch");
        assertEquals(1, first.disposeCount,
                "The previously active scene must be disposed exactly once on switch");
    }

    @Test
    void nextScene_wrapsAroundToFirst() {
        FakeScene a = new FakeScene("A");
        FakeScene b = new FakeScene("B");
        FakeScene c = new FakeScene("C");
        manager.registerScene(a);
        manager.registerScene(b);
        manager.registerScene(c);

        manager.nextScene(); // A → B
        manager.nextScene(); // B → C
        manager.nextScene(); // C → A (wrap)

        assertSame(a, manager.getCurrentScene(),
                "nextScene() must wrap from the last scene back to the first");
    }

    // -----------------------------------------------------------------------
    // previousScene
    // -----------------------------------------------------------------------

    @Test
    void previousScene_withNoScenes_doesNotThrow() {
        assertDoesNotThrow(() -> manager.previousScene());
    }

    @Test
    void previousScene_withOneScene_doesNotCallSetupAgain() {
        FakeScene only = new FakeScene("Only");
        manager.registerScene(only);
        int setupBefore = only.setupCount;

        manager.previousScene();

        assertEquals(setupBefore, only.setupCount,
                "previousScene() on a single-scene manager must not call setupScene() again");
    }

    @Test
    void previousScene_withTwoScenes_switchesAndCallsSetup() {
        FakeScene first = new FakeScene("A");
        FakeScene second = new FakeScene("B");
        manager.registerScene(first);
        manager.registerScene(second);
        manager.nextScene(); // now on second

        manager.previousScene(); // back to first

        assertSame(first, manager.getCurrentScene());
        // first.setupCount was 1 (from registerScene), now it should be 2
        assertEquals(2, first.setupCount,
                "previousScene() must call setupScene() on the new scene");
    }

    @Test
    void previousScene_wrapsAroundToLast() {
        FakeScene a = new FakeScene("A");
        FakeScene b = new FakeScene("B");
        FakeScene c = new FakeScene("C");
        manager.registerScene(a);
        manager.registerScene(b);
        manager.registerScene(c);

        manager.previousScene(); // A → C (wrap)

        assertSame(c, manager.getCurrentScene(),
                "previousScene() must wrap from the first scene back to the last");
    }

    // -----------------------------------------------------------------------
    // nextScene / previousScene symmetry (the bug that was fixed)
    // -----------------------------------------------------------------------

    @Test
    void nextSceneAndPreviousScene_bothCallSetupOnNewScene() {
        FakeScene a = new FakeScene("A");
        FakeScene b = new FakeScene("B");
        manager.registerScene(a);
        manager.registerScene(b);

        manager.nextScene();     // A → B: must call b.setupScene()
        int bSetupAfterNext = b.setupCount;

        manager.previousScene(); // B → A: must call a.setupScene()
        int aSetupAfterPrev = a.setupCount;

        assertEquals(1, bSetupAfterNext,
                "nextScene() must call setupScene() — the bug was that it did not");
        assertEquals(2, aSetupAfterPrev,
                "previousScene() must call setupScene() on the new scene");
    }

    // -----------------------------------------------------------------------
    // setCurrentSceneIndex
    // -----------------------------------------------------------------------

    @Test
    void setCurrentSceneIndex_validIndex_switchesSceneAndCallsSetup() {
        FakeScene a = new FakeScene("A");
        FakeScene b = new FakeScene("B");
        FakeScene c = new FakeScene("C");
        manager.registerScene(a);
        manager.registerScene(b);
        manager.registerScene(c);

        manager.setCurrentSceneIndex(2);

        assertSame(c, manager.getCurrentScene());
        assertEquals(1, c.setupCount);
    }

    @Test
    void setCurrentSceneIndex_negativeIndex_isIgnored() {
        FakeScene a = new FakeScene("A");
        manager.registerScene(a);

        manager.setCurrentSceneIndex(-1);

        assertSame(a, manager.getCurrentScene(),
                "Negative index must be ignored and current scene must not change");
    }

    @Test
    void setCurrentSceneIndex_indexTooLarge_isIgnored() {
        FakeScene a = new FakeScene("A");
        manager.registerScene(a);

        manager.setCurrentSceneIndex(5);

        assertSame(a, manager.getCurrentScene(),
                "Out-of-bounds index must be ignored and current scene must not change");
    }

    // -----------------------------------------------------------------------
    // clearScenes
    // -----------------------------------------------------------------------

    @Test
    void clearScenes_resetsCountToZero() {
        manager.registerScene(new FakeScene("A"));
        manager.registerScene(new FakeScene("B"));

        manager.clearScenes();

        assertEquals(0, manager.getSceneCount());
    }

    @Test
    void clearScenes_getCurrentSceneReturnsNull() {
        FakeScene scene = new FakeScene("A");
        manager.registerScene(scene);
        manager.clearScenes();

        assertNull(manager.getCurrentScene());
        assertEquals(1, scene.disposeCount,
                "clearScenes() must dispose the active scene");
    }

    @Test
    void clearScenes_allowsReregistering() {
        manager.registerScene(new FakeScene("A"));
        manager.clearScenes();

        FakeScene fresh = new FakeScene("Fresh");
        manager.registerScene(fresh);

        assertSame(fresh, manager.getCurrentScene());
        assertEquals(1, manager.getSceneCount());
    }

    @Test
    void clearScenes_isIdempotentAndDoesNotRedisposeInactiveScenes() {
        FakeScene first = new FakeScene("A");
        FakeScene second = new FakeScene("B");
        manager.registerScene(first);
        manager.registerScene(second);
        manager.nextScene();

        manager.clearScenes();
        manager.clearScenes();

        assertEquals(1, first.disposeCount);
        assertEquals(1, second.disposeCount);
    }

    @Test
    void reloadCurrentScenePerformsOneCompleteLifecycleCycle() {
        FakeScene scene = new FakeScene("Reloadable");
        manager.registerScene(scene);

        assertTrue(manager.reloadCurrentScene());

        assertSame(scene, manager.getCurrentScene());
        assertEquals(2, scene.setupCount);
        assertEquals(1, scene.disposeCount);
    }

    @Test
    void reloadWithoutActiveSceneIsIgnored() {
        assertFalse(manager.reloadCurrentScene());
    }

    // -----------------------------------------------------------------------
    // Minimal Scene stub — no OpenGL context needed
    // -----------------------------------------------------------------------

    /**
     * A lightweight stub that implements the minimum required by {@link Scene}.
     * It records how many times {@link #setupScene()} has been called so that
     * tests can assert scene lifecycle behaviour.
     */
    private static class FakeScene implements Scene {

        final String name;
        int setupCount = 0;
        int disposeCount = 0;

        FakeScene(String name) {
            this.name = name;
        }

        @Override
        public void sceneRender(PGraphicsOpenGL pg) {
            // no-op — tests never trigger rendering
        }

        @Override
        public void setupScene() {
            setupCount++;
        }

        @Override
        public void dispose() {
            disposeCount++;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
