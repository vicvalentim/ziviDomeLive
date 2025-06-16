package com.victorvalentim.zividomelive.render;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.render.camera.CameraManager;
import com.victorvalentim.zividomelive.render.camera.CameraOrientation;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

import java.nio.IntBuffer;
import java.nio.FloatBuffer;

/**
 * Renderer that draws scenes directly to an OpenGL cubemap texture using a custom FBO.
 */
public class PGLCubemapRenderer implements PConstants {
    private static final int NUM_FACES = 6;
    private static final float DEFAULT_NEAR_PLANE = 0.01f;
    private static final float DEFAULT_FAR_PLANE = 10000000.0f;

    private final PApplet parent;
    private final int resolution;

    private PGraphicsOpenGL faceGraphics;
    private int cubemapTexId = -1;
    private int fboId = -1;
    private int depthRboId = -1;

    private float cachedNearPlane = DEFAULT_NEAR_PLANE;
    private float cachedFarPlane = DEFAULT_FAR_PLANE;
    private float cachedFieldOfView = PApplet.PI / 2f;

    private Quaternion currentOrientation = new Quaternion(0, 0, 0, 1);

    /**
     * Creates a renderer with the supplied parent and cubemap resolution.
     */
    public PGLCubemapRenderer(PApplet parent, int resolution) {
        this.parent = parent;
        this.resolution = resolution;
        initializeGL();
    }

    private void initializeGL() {
        faceGraphics = (PGraphicsOpenGL) parent.createGraphics(resolution, resolution, P3D);
        PGL pgl = parent.beginPGL();
        IntBuffer buffer = IntBuffer.allocate(1);

        // cubemap texture
        pgl.genTextures(1, buffer);
        cubemapTexId = buffer.get(0);
        pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, cubemapTexId);
        for (int i = 0; i < NUM_FACES; i++) {
            pgl.texImage2D(PGL.TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, PGL.RGBA8,
                    resolution, resolution, 0, PGL.RGBA, PGL.UNSIGNED_BYTE, null);
        }
        pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MIN_FILTER, PGL.LINEAR_MIPMAP_LINEAR);
        pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MAG_FILTER, PGL.LINEAR);
        pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_S, PGL.CLAMP_TO_EDGE);
        pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_T, PGL.CLAMP_TO_EDGE);
        pgl.texParameteri(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_WRAP_R, PGL.CLAMP_TO_EDGE);

        FloatBuffer aniso = FloatBuffer.allocate(1);
        if (pgl.getFloatv(PGL.MAX_TEXTURE_MAX_ANISOTROPY, aniso)) {
            pgl.texParameterf(PGL.TEXTURE_CUBE_MAP, PGL.TEXTURE_MAX_ANISOTROPY, aniso.get(0));
        }

        // FBO and depth renderbuffer
        buffer.rewind();
        pgl.genFramebuffers(1, buffer);
        fboId = buffer.get(0);
        buffer.rewind();
        pgl.genRenderbuffers(1, buffer);
        depthRboId = buffer.get(0);

        pgl.bindRenderbuffer(PGL.RENDERBUFFER, depthRboId);
        pgl.renderbufferStorage(PGL.RENDERBUFFER, PGL.DEPTH_COMPONENT24, resolution, resolution);

        pgl.bindFramebuffer(PGL.FRAMEBUFFER, fboId);
        pgl.framebufferRenderbuffer(PGL.FRAMEBUFFER, PGL.DEPTH_ATTACHMENT, PGL.RENDERBUFFER, depthRboId);
        pgl.bindFramebuffer(PGL.FRAMEBUFFER, 0);

        parent.endPGL();
    }

    private void configureCameraForFace(PGraphicsOpenGL pg, CameraOrientation orientation,
                                        float pitch, float yaw, float roll) {
        PVector eye = new PVector(0, 0, 0);

        pg.camera(eye.x, eye.y, eye.z,
                orientation.centerX, orientation.centerY, orientation.centerZ,
                orientation.upX, orientation.upY, orientation.upZ);
        pg.perspective(cachedFieldOfView, 1, cachedNearPlane, cachedFarPlane);

        Quaternion qPitch = Quaternion.fromAxisAngle(1f, 0f, 0f, pitch);
        Quaternion qYaw = Quaternion.fromAxisAngle(0f, 0f, 1f, yaw);
        Quaternion qRoll = Quaternion.fromAxisAngle(0f, 1f, 0f, roll);
        Quaternion target = qYaw.multiply(qRoll).multiply(qPitch);
        currentOrientation = currentOrientation.slerp(target, 1f);
        pg.applyMatrix(currentOrientation.toMatrix());
    }

    /**
     * Renders all six faces of the cubemap.
     */
    public void captureCubemap(float pitch, float yaw, float roll,
                               CameraManager cameraManager, Scene scene) {
        for (int i = 0; i < NUM_FACES; i++) {
            faceGraphics.beginDraw();
            faceGraphics.background(0, 0);

            PGL pgl = faceGraphics.beginPGL();
            pgl.bindFramebuffer(PGL.FRAMEBUFFER, fboId);
            pgl.framebufferTexture2D(PGL.FRAMEBUFFER, PGL.COLOR_ATTACHMENT0,
                    PGL.TEXTURE_CUBE_MAP_POSITIVE_X + i, cubemapTexId, 0);
            pgl.viewport(0, 0, resolution, resolution);
            faceGraphics.endPGL();

            configureCameraForFace(faceGraphics, cameraManager.getOrientation(i),
                    pitch, yaw, roll);
            if (scene != null) {
                scene.sceneRender(faceGraphics);
            }

            PGL endPgl = faceGraphics.beginPGL();
            endPgl.bindFramebuffer(PGL.FRAMEBUFFER, 0);
            faceGraphics.endPGL();
            faceGraphics.endDraw();
        }

        PGL pgl = parent.beginPGL();
        pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, cubemapTexId);
        pgl.generateMipmap(PGL.TEXTURE_CUBE_MAP);
        pgl.bindTexture(PGL.TEXTURE_CUBE_MAP, 0);
        parent.endPGL();
    }

    /**
     * Returns the OpenGL texture handle of the cubemap.
     */
    public int getCubemapTexture() {
        return cubemapTexId;
    }

    /**
     * Releases OpenGL resources.
     */
    public void dispose() {
        if (cubemapTexId != -1) {
            PGL pgl = parent.beginPGL();
            pgl.deleteTextures(1, IntBuffer.wrap(new int[]{cubemapTexId}));
            pgl.deleteFramebuffers(1, IntBuffer.wrap(new int[]{fboId}));
            pgl.deleteRenderbuffers(1, IntBuffer.wrap(new int[]{depthRboId}));
            parent.endPGL();
            cubemapTexId = -1;
            fboId = -1;
            depthRboId = -1;
        }
        if (faceGraphics != null) {
            faceGraphics.dispose();
            faceGraphics = null;
        }
    }
}
