package com.victorvalentim.zividomelive;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PShape;
import processing.opengl.PShader;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Processing-friendly, typed assets scoped to one scene activation.
 *
 * <p>Images, shaders, and shapes are created only on the bound render thread. The default
 * loaders keep borrowed Processing objects and release their Java references at scene
 * disposal.</p>
 */
public final class SceneAssets {

    private final PApplet applet;
    private final RenderThreadQueue renderQueue;
    private final SceneResourceCache<PImage> images = new SceneResourceCache<>();
    private final SceneResourceCache<PShader> shaders = new SceneResourceCache<>();
    private final SceneResourceCache<PShape> shapes = new SceneResourceCache<>();
    private boolean closed;

    SceneAssets(PApplet applet, RenderThreadQueue renderQueue) {
        this.applet = Objects.requireNonNull(applet, "applet");
        this.renderQueue = Objects.requireNonNull(renderQueue, "renderQueue");
    }

    /**
     * Loads an image once using a sketch-relative Processing data path.
     *
     * @param path Processing image path
     * @return loaded image, or null when Processing cannot load it
     */
    public PImage loadImage(String path) {
        ensureOpen();
        renderQueue.requireRenderThread();
        PImage cached = images.get(path);
        if (cached != null) {
            return cached;
        }
        PImage image = applet.loadImage(path);
        if (image != null) {
            images.putBorrowed(path, image);
        }
        return image;
    }

    /**
     * Loads a fragment shader once.
     *
     * @param fragmentPath Processing fragment shader path
     * @return loaded shader, or null when Processing cannot load it
     */
    public PShader loadShader(String fragmentPath) {
        ensureOpen();
        renderQueue.requireRenderThread();
        PShader cached = shaders.get(fragmentPath);
        if (cached != null) {
            return cached;
        }
        PShader shader = applet.loadShader(fragmentPath);
        if (shader != null) {
            shaders.putBorrowed(fragmentPath, shader);
        }
        return shader;
    }

    /**
     * Loads a fragment/vertex shader pair once.
     *
     * @param key cache key
     * @param fragmentPath Processing fragment shader path
     * @param vertexPath Processing vertex shader path
     * @return loaded shader, or null when Processing cannot load it
     */
    public PShader loadShader(String key, String fragmentPath, String vertexPath) {
        ensureOpen();
        renderQueue.requireRenderThread();
        PShader cached = shaders.get(key);
        if (cached != null) {
            return cached;
        }
        PShader shader = applet.loadShader(fragmentPath, vertexPath);
        if (shader != null) {
            shaders.putBorrowed(key, shader);
        }
        return shader;
    }

    /**
     * Creates a retained shape once on the render thread.
     *
     * @param key cache key
     * @param factory retained shape factory
     * @return cached or newly created shape
     */
    public PShape getOrCreateShape(String key, Supplier<? extends PShape> factory) {
        ensureOpen();
        renderQueue.requireRenderThread();
        return shapes.getOrCreateBorrowed(key, factory);
    }

    /**
     * Stores or replaces one borrowed retained shape.
     *
     * @param key stable shape key
     * @param shape retained Processing shape
     * @return the supplied shape
     */
    public PShape cacheShape(String key, PShape shape) {
        ensureOpen();
        renderQueue.requireRenderThread();
        shapes.putBorrowed(key, shape);
        return shape;
    }

    /**
     * Invalidates retained shapes whose keys start with the supplied prefix.
     *
     * @param prefix key prefix used to select retained shapes
     * @return number of removed shapes
     */
    public int removeShapesByPrefix(String prefix) {
        ensureOpen();
        renderQueue.requireRenderThread();
        return shapes.removeByPrefix(prefix);
    }

    void close() {
        if (closed) {
            return;
        }
        shapes.close();
        shaders.close();
        images.close();
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scene assets are closed.");
        }
    }
}
