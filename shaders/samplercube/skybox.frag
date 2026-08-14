#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;
uniform int layoutFaces[6];
uniform int faceRotations[6];
uniform int faceInversions[6];

out vec4 FragColor;

const int SLOT_TOP = 0;
const int SLOT_LEFT = 1;
const int SLOT_CENTER = 2;
const int SLOT_RIGHT = 3;
const int SLOT_FAR_RIGHT = 4;
const int SLOT_BOTTOM = 5;

const int FACE_POSITIVE_X = 0;
const int FACE_NEGATIVE_X = 1;
const int FACE_POSITIVE_Y = 2;
const int FACE_NEGATIVE_Y = 3;
const int FACE_POSITIVE_Z = 4;
const int FACE_NEGATIVE_Z = 5;

vec3 applyEAC(vec3 dir) {
    vec3 absDir = abs(dir);
    float dominantAxis = max(max(absDir.x, absDir.y), absDir.z);
    return dir / max(dominantAxis, 0.000001);
}

vec4 sampleCubemapEAC(vec3 dir) {
    return texture(cubemap, applyEAC(normalize(dir)));
}

vec3 applyLegacyFaceTransform(vec3 dir, int rotation, int invert) {
    for (int i = 0; i < rotation; i++) {
        dir = vec3(-dir.z, dir.y, dir.x);
    }

    if (invert != 0) {
        dir.x = -dir.x;
    }

    return dir;
}

bool resolveOriginalCubemapViewSlot(vec2 st, out int faceIndex, out vec2 faceUV) {
    if (st.y >= 1.0 && st.y < 2.0) {
        if (st.x >= 0.0 && st.x < 1.0) {
            faceIndex = layoutFaces[SLOT_LEFT];
            faceUV = vec2(st.x, st.y - 1.0);
            return true;
        }
        if (st.x >= 1.0 && st.x < 2.0) {
            faceIndex = layoutFaces[SLOT_CENTER];
            faceUV = vec2(st.x - 1.0, st.y - 1.0);
            return true;
        }
        if (st.x >= 2.0 && st.x < 3.0) {
            faceIndex = layoutFaces[SLOT_RIGHT];
            faceUV = vec2(st.x - 2.0, st.y - 1.0);
            return true;
        }
        if (st.x >= 3.0 && st.x < 4.0) {
            faceIndex = layoutFaces[SLOT_FAR_RIGHT];
            faceUV = vec2(st.x - 3.0, st.y - 1.0);
            return true;
        }
    }

    if (st.y >= 0.0 && st.y < 1.0 && st.x >= 1.0 && st.x < 2.0) {
        faceIndex = layoutFaces[SLOT_TOP];
        faceUV = vec2(st.x - 1.0, st.y);
        return true;
    }

    if (st.y >= 2.0 && st.y < 3.0 && st.x >= 1.0 && st.x < 2.0) {
        faceIndex = layoutFaces[SLOT_BOTTOM];
        faceUV = vec2(st.x - 1.0, st.y - 2.0);
        return true;
    }

    faceIndex = -1;
    faceUV = vec2(0.0);
    return false;
}

vec3 directionForCanonicalFace(int faceIndex, vec2 faceUV) {
    if (faceIndex == FACE_POSITIVE_X) {
        return vec3(0.5, faceUV.y - 0.5, 0.5 - faceUV.x);
    }
    if (faceIndex == FACE_NEGATIVE_X) {
        return vec3(-0.5, faceUV.y - 0.5, faceUV.x - 0.5);
    }
    if (faceIndex == FACE_POSITIVE_Y) {
        return vec3(faceUV.x - 0.5, 0.5, 0.5 - faceUV.y);
    }
    if (faceIndex == FACE_NEGATIVE_Y) {
        return vec3(faceUV.x - 0.5, -0.5, faceUV.y - 0.5);
    }
    if (faceIndex == FACE_POSITIVE_Z) {
        return vec3(faceUV.x - 0.5, faceUV.y - 0.5, 0.5);
    }
    return vec3(0.5 - faceUV.x, faceUV.y - 0.5, -0.5);
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    uv.y = 1.0 - uv.y;

    vec2 st = uv * vec2(4.0, 3.0);

    int faceIndex;
    vec2 faceUV = vec2(0.0);

    if (!resolveOriginalCubemapViewSlot(st, faceIndex, faceUV)) {
        FragColor = vec4(0.0);
        return;
    }

    vec3 dir = directionForCanonicalFace(faceIndex, faceUV);
    dir = applyLegacyFaceTransform(dir, faceRotations[faceIndex], faceInversions[faceIndex]);
    dir.z = -dir.z;

    FragColor = sampleCubemapEAC(dir);
}
