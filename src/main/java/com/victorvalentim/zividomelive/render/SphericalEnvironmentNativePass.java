package com.victorvalentim.zividomelive.render;

import com.jogamp.opengl.GL2ES3;
import com.victorvalentim.zividomelive.render.camera.CubemapFace;
import com.victorvalentim.zividomelive.support.LogManager;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.opengl.FrameBuffer;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;
import processing.opengl.Texture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Native fullscreen Environment pass used only by spherical cubemap capture.
 *
 * <p>This class deliberately does not use {@code PShader}. Processing remains the owner of
 * the source {@link PImage} and its {@link Texture}; the pass borrows the active texture name,
 * compiles its own GLSL program in the currently active scratch OpenGL context, and restores
 * every GL state it changes before returning.</p>
 */
final class SphericalEnvironmentNativePass {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int GL_CURRENT_PROGRAM = 0x8B8D;
    private static final int GL_VERTEX_ARRAY_BINDING = 0x85B5;
    private static final int GL_ACTIVE_TEXTURE = 0x84E0;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_VIEWPORT = 0x0BA2;
    private static final int GL_DEPTH_FUNC = 0x0B74;
    private static final int GL_COLOR_WRITEMASK = 0x0C23;

    private static final String VERTEX_SOURCE = """
            #version 330 core

            const vec2 FULLSCREEN_TRIANGLE[3] = vec2[](
                vec2(-1.0, -1.0),
                vec2( 3.0, -1.0),
                vec2(-1.0,  3.0)
            );

            void main() {
                gl_Position = vec4(FULLSCREEN_TRIANGLE[gl_VertexID], 1.0, 1.0);
            }
            """;

    private static final String FRAGMENT_SOURCE = """
            #version 330 core

            uniform sampler2D environmentMap;
            uniform vec2 faceResolution;
            uniform vec2 environmentUvScale;
            uniform vec2 environmentUvOffset;
            uniform int faceIndex;
            uniform mat4 environmentRotation;
            uniform float yawOffset;
            uniform float intensity;

            out vec4 FragColor;

            const float PI = 3.1415926535897932384626433832795;

            const int FACE_POSITIVE_X = 0;
            const int FACE_NEGATIVE_X = 1;
            const int FACE_POSITIVE_Y = 2;
            const int FACE_NEGATIVE_Y = 3;
            const int FACE_POSITIVE_Z = 4;
            const int FACE_NEGATIVE_Z = 5;

            vec3 directionForCanonicalFace(int face, vec2 faceUV) {
                if (face == FACE_POSITIVE_X) {
                    return vec3(0.5, faceUV.y - 0.5, 0.5 - faceUV.x);
                }
                if (face == FACE_NEGATIVE_X) {
                    return vec3(-0.5, faceUV.y - 0.5, faceUV.x - 0.5);
                }
                if (face == FACE_POSITIVE_Y) {
                    return vec3(faceUV.x - 0.5, 0.5, 0.5 - faceUV.y);
                }
                if (face == FACE_NEGATIVE_Y) {
                    return vec3(faceUV.x - 0.5, -0.5, faceUV.y - 0.5);
                }
                if (face == FACE_POSITIVE_Z) {
                    return vec3(faceUV.x - 0.5, faceUV.y - 0.5, 0.5);
                }
                return vec3(0.5 - faceUV.x, faceUV.y - 0.5, -0.5);
            }

            vec2 equirectangularUv(vec3 dir) {
                dir = normalize(dir);
                float theta = atan(-dir.x, -dir.z) - yawOffset;
                float u = fract(theta / (2.0 * PI));
                float v = acos(clamp(dir.y, -1.0, 1.0)) / PI;
                return vec2(u, v);
            }

            int wrapLongitudeIndex(int index, int size) {
                int wrapped = index % size;
                return wrapped < 0 ? wrapped + size : wrapped;
            }

            vec4 sampleEnvironmentLinear(vec2 uv) {
                ivec2 size = textureSize(environmentMap, 0);
                vec2 texelPosition = uv * vec2(size) - 0.5;
                ivec2 base = ivec2(floor(texelPosition));
                vec2 weight = fract(texelPosition);

                int x0 = wrapLongitudeIndex(base.x, size.x);
                int x1 = wrapLongitudeIndex(base.x + 1, size.x);
                int y0 = clamp(base.y, 0, size.y - 1);
                int y1 = clamp(base.y + 1, 0, size.y - 1);

                vec4 top = mix(
                        texelFetch(environmentMap, ivec2(x0, y0), 0),
                        texelFetch(environmentMap, ivec2(x1, y0), 0),
                        weight.x);
                vec4 bottom = mix(
                        texelFetch(environmentMap, ivec2(x0, y1), 0),
                        texelFetch(environmentMap, ivec2(x1, y1), 0),
                        weight.x);
                return mix(top, bottom, weight.y);
            }

            void main() {
                vec2 faceUV = gl_FragCoord.xy / faceResolution;

                // The later scratch-to-cubemap blit performs the framebuffer-origin flip.
                vec3 dir = directionForCanonicalFace(faceIndex, faceUV);
                dir = (environmentRotation * vec4(dir, 0.0)).xyz;

                vec2 environmentUV = equirectangularUv(dir);
                environmentUV = environmentUV * environmentUvScale + environmentUvOffset;
                vec4 color = sampleEnvironmentLinear(environmentUV);
                FragColor = vec4(color.rgb * max(intensity, 0.0), color.a);
            }
            """;

    private final PApplet parent;
    private final EnvironmentState state;

    private final IntBuffer scalarBuffer = IntBuffer.allocate(1);
    private final IntBuffer viewportBuffer = IntBuffer.allocate(4);
    private final IntBuffer colorMaskBuffer = IntBuffer.allocate(4);
    private final FloatBuffer matrixBuffer = ByteBuffer
            .allocateDirect(16 * Float.BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
    private final int[] generatedVertexArray = new int[1];

    private Object contextIdentity;
    private int programId;
    private int vertexArrayId;

    private int environmentMapLocation = -1;
    private int faceResolutionLocation = -1;
    private int environmentUvScaleLocation = -1;
    private int environmentUvOffsetLocation = -1;
    private int faceIndexLocation = -1;
    private int environmentRotationLocation = -1;
    private int yawOffsetLocation = -1;
    private int intensityLocation = -1;

    private boolean unavailableWarningLogged;
    private boolean renderFailureWarningLogged;

    SphericalEnvironmentNativePass(PApplet parent, EnvironmentState state) {
        this.parent = Objects.requireNonNull(parent, "parent cannot be null");
        this.state = Objects.requireNonNull(state, "state cannot be null");
    }

    boolean renderScratchCubemapFace(
            PGraphicsOpenGL target,
            CubemapFace face,
            PMatrix3D orientationMatrix) {
        if (!state.isVisible() || !state.hasSource()) {
            return false;
        }
        if (target == null || face == null) {
            warnUnavailable("spherical face");
            return false;
        }

        try {
            PImage source = state.getLdrEquirectangularSource();
            Texture texture = target.getTexture(source);
            if (texture == null || !texture.available()) {
                throw new IllegalStateException(
                        "Environment source has no available Processing OpenGL texture.");
            }
            if (texture.glTarget != PGL.TEXTURE_2D) {
                throw new IllegalStateException(
                        "Environment source must resolve to GL_TEXTURE_2D, found target="
                                + texture.glTarget);
            }

            float maxU = texture.maxTexcoordU();
            float maxV = texture.maxTexcoordV();
            float scaleU = texture.invertedX() ? -maxU : maxU;
            float scaleV = texture.invertedY() ? -maxV : maxV;
            float offsetU = texture.invertedX() ? maxU : 0.0f;
            float offsetV = texture.invertedY() ? maxV : 0.0f;
            PMatrix3D effectiveOrientation = orientationMatrix == null
                    ? new PMatrix3D()
                    : orientationMatrix;

            PGL pgl = target.beginPGL();
            try {
                renderNative(
                        target,
                        pgl,
                        texture,
                        face,
                        effectiveOrientation,
                        scaleU,
                        scaleV,
                        offsetU,
                        offsetV);
            } finally {
                target.endPGL();
            }

            renderFailureWarningLogged = false;
            unavailableWarningLogged = false;
            return true;
        } catch (RuntimeException error) {
            warnRenderFailure(error);
            return false;
        }
    }

    /**
     * Compatibility entry point for callers that already own an active PGL lifecycle.
     *
     * <p>The production cubemap path uses {@link #renderScratchCubemapFace}; this overload
     * exists only so the established public {@link EnvironmentBackgroundRenderer} surface can
     * delegate spherical rendering without reintroducing the former PShader/native hybrid.</p>
     */
    boolean renderCubemapFace(
            PGraphicsOpenGL target,
            PGL pgl,
            CubemapFace face,
            PMatrix3D orientationMatrix) {
        if (!state.isVisible() || !state.hasSource()) {
            return false;
        }
        if (target == null || pgl == null || face == null) {
            warnUnavailable("spherical face");
            return false;
        }

        try {
            PImage source = state.getLdrEquirectangularSource();
            Texture texture = target.getTexture(source);
            if (texture == null || !texture.available()) {
                throw new IllegalStateException(
                        "Environment source has no available Processing OpenGL texture.");
            }
            if (texture.glTarget != PGL.TEXTURE_2D) {
                throw new IllegalStateException(
                        "Environment source must resolve to GL_TEXTURE_2D, found target="
                                + texture.glTarget);
            }

            float maxU = texture.maxTexcoordU();
            float maxV = texture.maxTexcoordV();
            float scaleU = texture.invertedX() ? -maxU : maxU;
            float scaleV = texture.invertedY() ? -maxV : maxV;
            float offsetU = texture.invertedX() ? maxU : 0.0f;
            float offsetV = texture.invertedY() ? maxV : 0.0f;
            PMatrix3D effectiveOrientation = orientationMatrix == null
                    ? new PMatrix3D()
                    : orientationMatrix;

            renderNative(
                    target,
                    pgl,
                    texture,
                    face,
                    effectiveOrientation,
                    scaleU,
                    scaleV,
                    offsetU,
                    offsetV);

            renderFailureWarningLogged = false;
            unavailableWarningLogged = false;
            return true;
        } catch (RuntimeException error) {
            warnRenderFailure(error);
            return false;
        }
    }

    private void renderNative(
            PGraphicsOpenGL target,
            PGL pgl,
            Texture texture,
            CubemapFace face,
            PMatrix3D orientationMatrix,
            float scaleU,
            float scaleV,
            float offsetU,
            float offsetV) {
        PJOGL pjogl = requirePjogl(pgl);
        GL2ES3 gl = requireGl(pjogl);
        ensureResources(pgl, pjogl, gl);
        verifyScratchFramebuffer(target, pgl);

        int savedProgram = readInt(pgl, GL_CURRENT_PROGRAM);
        int savedVertexArray = readInt(pgl, GL_VERTEX_ARRAY_BINDING);
        int savedActiveTexture = readInt(pgl, GL_ACTIVE_TEXTURE);
        int savedDepthFunction = readInt(pgl, GL_DEPTH_FUNC);

        boolean savedDepthTest = pgl.isEnabled(PGL.DEPTH_TEST);
        boolean savedBlend = pgl.isEnabled(PGL.BLEND);
        boolean savedCull = pgl.isEnabled(PGL.CULL_FACE);
        boolean savedScissor = pgl.isEnabled(PGL.SCISSOR_TEST);

        scalarBuffer.clear();
        pgl.getBooleanv(PGL.DEPTH_WRITEMASK, scalarBuffer);
        boolean savedDepthMask = scalarBuffer.get(0) != 0;

        viewportBuffer.clear();
        pgl.getIntegerv(GL_VIEWPORT, viewportBuffer);
        int viewportX = viewportBuffer.get(0);
        int viewportY = viewportBuffer.get(1);
        int viewportWidth = viewportBuffer.get(2);
        int viewportHeight = viewportBuffer.get(3);

        colorMaskBuffer.clear();
        pgl.getBooleanv(GL_COLOR_WRITEMASK, colorMaskBuffer);
        boolean colorR = colorMaskBuffer.get(0) != 0;
        boolean colorG = colorMaskBuffer.get(1) != 0;
        boolean colorB = colorMaskBuffer.get(2) != 0;
        boolean colorA = colorMaskBuffer.get(3) != 0;

        pgl.activeTexture(PGL.TEXTURE0);
        int savedTexture2D = readInt(pgl, GL_TEXTURE_BINDING_2D);

        try {
            pgl.enable(PGL.DEPTH_TEST);
            pgl.depthFunc(PGL.LEQUAL);
            pgl.depthMask(false);
            pgl.disable(PGL.BLEND);
            pgl.disable(PGL.CULL_FACE);
            pgl.disable(PGL.SCISSOR_TEST);
            pgl.colorMask(true, true, true, true);

            // Preserve the qualified baseline shader contract: faceResolution uses
            // logical cubemap face dimensions, exactly as EnvironmentBackgroundRenderer did.
            int renderWidth = Math.max(1, target.width);
            int renderHeight = Math.max(1, target.height);
            pgl.viewport(0, 0, renderWidth, renderHeight);

            pgl.useProgram(programId);
            gl.glBindVertexArray(vertexArrayId);

            pgl.bindTexture(PGL.TEXTURE_2D, texture.glName);
            pgl.uniform1i(environmentMapLocation, 0);
            pgl.uniform2f(faceResolutionLocation, renderWidth, renderHeight);
            pgl.uniform2f(environmentUvScaleLocation, scaleU, scaleV);
            pgl.uniform2f(environmentUvOffsetLocation, offsetU, offsetV);
            pgl.uniform1i(faceIndexLocation, face.index());
            uploadMatrix(pgl, environmentRotationLocation, orientationMatrix);
            pgl.uniform1f(yawOffsetLocation, state.getYawOffset());
            pgl.uniform1f(intensityLocation, state.getIntensity());

            pgl.drawArrays(PGL.TRIANGLES, 0, 3);
        } finally {
            pgl.bindTexture(PGL.TEXTURE_2D, savedTexture2D);
            pgl.activeTexture(savedActiveTexture);

            gl.glBindVertexArray(savedVertexArray);
            pgl.useProgram(savedProgram);

            pgl.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
            pgl.colorMask(colorR, colorG, colorB, colorA);
            pgl.depthMask(savedDepthMask);
            pgl.depthFunc(savedDepthFunction);
            restoreCapability(pgl, PGL.DEPTH_TEST, savedDepthTest);
            restoreCapability(pgl, PGL.BLEND, savedBlend);
            restoreCapability(pgl, PGL.CULL_FACE, savedCull);
            restoreCapability(pgl, PGL.SCISSOR_TEST, savedScissor);
        }
    }

    private void verifyScratchFramebuffer(PGraphicsOpenGL target, PGL pgl) {
        FrameBuffer multisample = target.getFrameBuffer(true);
        FrameBuffer resolved = target.getFrameBuffer(false);
        int multisampleId = multisample == null ? 0 : multisample.glFbo;
        int resolvedId = resolved == null ? 0 : resolved.glFbo;

        if (multisampleId == 0 && resolvedId == 0) {
            throw new IllegalStateException(
                    "Spherical Environment scratch framebuffer is unavailable.");
        }

        int activeDrawFramebuffer = readInt(pgl, GL_DRAW_FRAMEBUFFER_BINDING);
        if (activeDrawFramebuffer != multisampleId
                && activeDrawFramebuffer != resolvedId) {
            throw new IllegalStateException(
                    "Spherical Environment is not drawing into a Processing scratch FBO: "
                            + "active="
                            + activeDrawFramebuffer
                            + ", multisample="
                            + multisampleId
                            + ", resolved="
                            + resolvedId
                            + ".");
        }
    }

    private void ensureResources(PGL pgl, PJOGL pjogl, GL2ES3 gl) {
        if (programId != 0
                && vertexArrayId != 0
                && contextIdentity == pjogl.context) {
            return;
        }

        abandonContext();
        contextIdentity = pjogl.context;

        int vertexShader = 0;
        int fragmentShader = 0;
        try {
            vertexShader = compileShader(pgl, PGL.VERTEX_SHADER, VERTEX_SOURCE, "vertex");
            fragmentShader = compileShader(pgl, PGL.FRAGMENT_SHADER, FRAGMENT_SOURCE, "fragment");

            programId = pgl.createProgram();
            if (programId == 0) {
                throw new IllegalStateException(
                        "Could not create the spherical Environment OpenGL program.");
            }
            pgl.attachShader(programId, vertexShader);
            pgl.attachShader(programId, fragmentShader);
            pgl.linkProgram(programId);

            scalarBuffer.clear();
            pgl.getProgramiv(programId, PGL.LINK_STATUS, scalarBuffer);
            if (scalarBuffer.get(0) == 0) {
                String log = pgl.getProgramInfoLog(programId);
                throw new IllegalStateException(
                        "Could not link spherical Environment program: " + log);
            }

            pgl.detachShader(programId, vertexShader);
            pgl.detachShader(programId, fragmentShader);
            pgl.deleteShader(vertexShader);
            pgl.deleteShader(fragmentShader);
            vertexShader = 0;
            fragmentShader = 0;

            environmentMapLocation = requireUniform(pgl, "environmentMap");
            faceResolutionLocation = requireUniform(pgl, "faceResolution");
            environmentUvScaleLocation = requireUniform(pgl, "environmentUvScale");
            environmentUvOffsetLocation = requireUniform(pgl, "environmentUvOffset");
            faceIndexLocation = requireUniform(pgl, "faceIndex");
            environmentRotationLocation = requireUniform(pgl, "environmentRotation");
            yawOffsetLocation = requireUniform(pgl, "yawOffset");
            intensityLocation = requireUniform(pgl, "intensity");

            generatedVertexArray[0] = 0;
            gl.glGenVertexArrays(1, generatedVertexArray, 0);
            if (generatedVertexArray[0] == 0) {
                throw new IllegalStateException(
                        "Could not allocate the spherical Environment fullscreen VAO.");
            }
            vertexArrayId = generatedVertexArray[0];
        } catch (RuntimeException error) {
            if (vertexShader != 0) {
                pgl.deleteShader(vertexShader);
            }
            if (fragmentShader != 0) {
                pgl.deleteShader(fragmentShader);
            }
            deleteCurrentContextResources(pgl, gl);
            abandonContext();
            throw error;
        }
    }

    private int compileShader(PGL pgl, int type, String source, String label) {
        int shader = pgl.createShader(type);
        if (shader == 0) {
            throw new IllegalStateException(
                    "Could not create spherical Environment " + label + " shader.");
        }

        pgl.shaderSource(shader, source);
        pgl.compileShader(shader);
        scalarBuffer.clear();
        pgl.getShaderiv(shader, PGL.COMPILE_STATUS, scalarBuffer);
        if (scalarBuffer.get(0) == 0) {
            String log = pgl.getShaderInfoLog(shader);
            pgl.deleteShader(shader);
            throw new IllegalStateException(
                    "Could not compile spherical Environment " + label + " shader: " + log);
        }
        return shader;
    }

    private int requireUniform(PGL pgl, String name) {
        int location = pgl.getUniformLocation(programId, name);
        if (location < 0) {
            throw new IllegalStateException(
                    "Spherical Environment uniform was not linked: " + name);
        }
        return location;
    }

    private void uploadMatrix(PGL pgl, int location, PMatrix3D matrix) {
        matrixBuffer.clear();
        matrixBuffer.put(matrix.m00).put(matrix.m01).put(matrix.m02).put(matrix.m03);
        matrixBuffer.put(matrix.m10).put(matrix.m11).put(matrix.m12).put(matrix.m13);
        matrixBuffer.put(matrix.m20).put(matrix.m21).put(matrix.m22).put(matrix.m23);
        matrixBuffer.put(matrix.m30).put(matrix.m31).put(matrix.m32).put(matrix.m33);
        matrixBuffer.flip();
        pgl.uniformMatrix4fv(location, 1, false, matrixBuffer);
    }

    private int readInt(PGL pgl, int name) {
        scalarBuffer.clear();
        pgl.getIntegerv(name, scalarBuffer);
        return scalarBuffer.get(0);
    }

    private static void restoreCapability(PGL pgl, int capability, boolean enabled) {
        if (enabled) {
            pgl.enable(capability);
        } else {
            pgl.disable(capability);
        }
    }

    private static PJOGL requirePjogl(PGL pgl) {
        if (!(pgl instanceof PJOGL pjogl) || pjogl.context == null) {
            throw new IllegalStateException(
                    "Spherical Environment requires an active JOGL context.");
        }
        return pjogl;
    }

    private static GL2ES3 requireGl(PJOGL pjogl) {
        if (pjogl.gl == null) {
            throw new IllegalStateException(
                    "Spherical Environment has no active OpenGL interface.");
        }
        try {
            return pjogl.gl.getGL2ES3();
        } catch (RuntimeException error) {
            throw new IllegalStateException(
                    "Spherical Environment requires a GL2ES3-compatible context.",
                    error);
        }
    }

    private void warnUnavailable(String pass) {
        if (!unavailableWarningLogged) {
            LOGGER.warning(
                    "Spherical Environment unavailable; " + pass + " background skipped.");
            unavailableWarningLogged = true;
        }
    }

    private void warnRenderFailure(RuntimeException error) {
        if (!renderFailureWarningLogged) {
            LOGGER.warning(
                    "Spherical Environment native pass failed; background skipped: "
                            + error.getMessage());
            renderFailureWarningLogged = true;
        }
    }

    void dispose() {
        if (programId == 0 && vertexArrayId == 0) {
            abandonContext();
            return;
        }
        if (!(parent.g instanceof PGraphicsOpenGL graphics)) {
            abandonContext();
            return;
        }

        PGL pgl = null;
        try {
            pgl = graphics.beginPGL();
            PJOGL pjogl = requirePjogl(pgl);
            if (pjogl.context != contextIdentity) {
                return;
            }
            GL2ES3 gl = requireGl(pjogl);
            deleteCurrentContextResources(pgl, gl);
        } catch (RuntimeException error) {
            LOGGER.fine(
                    "Spherical Environment native resources could not be explicitly deleted: "
                            + error.getMessage());
        } finally {
            if (pgl != null) {
                graphics.endPGL();
            }
            abandonContext();
        }
    }

    private void deleteCurrentContextResources(PGL pgl, GL2ES3 gl) {
        if (vertexArrayId != 0) {
            generatedVertexArray[0] = vertexArrayId;
            gl.glDeleteVertexArrays(1, generatedVertexArray, 0);
        }
        if (programId != 0) {
            pgl.deleteProgram(programId);
        }
    }

    private void abandonContext() {
        contextIdentity = null;
        programId = 0;
        vertexArrayId = 0;
        generatedVertexArray[0] = 0;
        environmentMapLocation = -1;
        faceResolutionLocation = -1;
        environmentUvScaleLocation = -1;
        environmentUvOffsetLocation = -1;
        faceIndexLocation = -1;
        environmentRotationLocation = -1;
        yawOffsetLocation = -1;
        intensityLocation = -1;
    }
}
