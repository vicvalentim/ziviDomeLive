package com.victorvalentim.zividomelive;

import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PShape;

import static org.junit.jupiter.api.Assertions.*;

class SceneAssetsTest {

    @Test
    void concreteShapeOperationsDoNotExposeTheRawCache() {
        RenderThreadQueue queue = new RenderThreadQueue();
        SceneAssets assets = new SceneAssets(new PApplet(), queue);
        PShape shape = new PShape();

        assertSame(shape, assets.cacheShape("solar:shape:0:sun", shape));
        assertSame(shape, assets.getOrCreateShape(
                "solar:shape:0:sun", () -> new PShape()));
        assertEquals(1, assets.removeShapesByPrefix("solar:shape:0:"));

        assets.close();
        assertThrows(IllegalStateException.class,
                () -> assets.cacheShape("late", new PShape()));
    }
}
