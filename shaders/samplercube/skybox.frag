#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;

out vec4 FragColor;

const int POSITIVE_X = 0;
const int NEGATIVE_X = 1;
const int POSITIVE_Y = 2;
const int NEGATIVE_Y = 3;
const int POSITIVE_Z = 4;
const int NEGATIVE_Z = 5;

vec3 legacyCubemapDirectionToSamplerCube(vec3 dir) {
    vec3 absDir = abs(dir);

    // Match the legacy six-PGraphics layout while sampling the native OpenGL
    // cubemap. This is the same convention bridge used by the equirectangular
    // and fisheye samplerCube shaders.
    if (absDir.y >= absDir.x && absDir.y >= absDir.z) {
        return vec3(dir.x, dir.y, -dir.z);
    }
    return vec3(dir.x, -dir.y, dir.z);
}

vec3 faceDirection(int faceIndex, vec2 faceUV) {
    // The legacy renderer draws every PGraphics face rotated 180 degrees and
    // horizontally inverted, which is equivalent to sampling the source with a
    // vertical flip.
    vec2 sourceUV = vec2(faceUV.x, 1.0 - faceUV.y);
    float x = sourceUV.x * 2.0 - 1.0;
    float y = 1.0 - sourceUV.y * 2.0;

    if (faceIndex == POSITIVE_X) {
        return vec3(1.0, -y, -x);
    } else if (faceIndex == NEGATIVE_X) {
        return vec3(-1.0, -y, x);
    } else if (faceIndex == POSITIVE_Y) {
        return vec3(x, 1.0, y);
    } else if (faceIndex == NEGATIVE_Y) {
        return vec3(x, -1.0, -y);
    } else if (faceIndex == POSITIVE_Z) {
        return vec3(x, -y, 1.0);
    } else {
        return vec3(-x, -y, -1.0);
    }
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    uv.y = 1.0 - uv.y;

    vec2 st = uv * vec2(4.0, 3.0);

    int faceIndex = -1;
    vec2 faceUV = vec2(0.0);

    if (st.y >= 0.0 && st.y < 1.0 && st.x >= 1.0 && st.x < 2.0) {
        faceIndex = NEGATIVE_Y;
        faceUV = vec2(st.x - 1.0, st.y);
    } else if (st.y >= 1.0 && st.y < 2.0) {
        if (st.x >= 0.0 && st.x < 1.0) {
            faceIndex = NEGATIVE_X;
            faceUV = vec2(st.x, st.y - 1.0);
        } else if (st.x >= 1.0 && st.x < 2.0) {
            faceIndex = POSITIVE_Z;
            faceUV = vec2(st.x - 1.0, st.y - 1.0);
        } else if (st.x >= 2.0 && st.x < 3.0) {
            faceIndex = POSITIVE_X;
            faceUV = vec2(st.x - 2.0, st.y - 1.0);
        } else if (st.x >= 3.0 && st.x < 4.0) {
            faceIndex = NEGATIVE_Z;
            faceUV = vec2(st.x - 3.0, st.y - 1.0);
        }
    } else if (st.y >= 2.0 && st.y < 3.0 && st.x >= 1.0 && st.x < 2.0) {
        faceIndex = POSITIVE_Y;
        faceUV = vec2(st.x - 1.0, st.y - 2.0);
    }

    if (faceIndex == -1) {
        FragColor = vec4(0.0);
        return;
    }

    vec3 sampleDir = legacyCubemapDirectionToSamplerCube(normalize(faceDirection(faceIndex, faceUV)));
    FragColor = texture(cubemap, sampleDir);
}
