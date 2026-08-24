#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;
uniform int layoutFaces[6];
uniform int faceRotations[6];
uniform int faceInversions[6];

out vec4 FragColor;

const float PI = 3.1415926535897932384626433832795;

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

vec3 safeNormalize(vec3 direction) {
    return direction * inversesqrt(max(dot(direction, direction), 1.0e-20));
}

vec4 sampleCubemapDirection(vec3 direction) {
    vec3 unitDirection = safeNormalize(direction);
    return textureGrad(
            cubemap,
            unitDirection,
            dFdx(unitDirection),
            dFdy(unitDirection));
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

vec3 directionForEquiangularFace(int faceIndex, vec2 faceUV) {
    // A real EAC face spaces angles uniformly. At the edges tan(+-PI/4) reaches +-1.
    vec2 faceAngles = (faceUV * 2.0 - 1.0) * (PI * 0.25);
    vec2 facePlane = tan(faceAngles);
    if (faceIndex == FACE_POSITIVE_X) {
        return vec3(1.0, facePlane.y, -facePlane.x);
    }
    if (faceIndex == FACE_NEGATIVE_X) {
        return vec3(-1.0, facePlane.y, facePlane.x);
    }
    if (faceIndex == FACE_POSITIVE_Y) {
        return vec3(facePlane.x, 1.0, -facePlane.y);
    }
    if (faceIndex == FACE_NEGATIVE_Y) {
        return vec3(facePlane.x, -1.0, facePlane.y);
    }
    if (faceIndex == FACE_POSITIVE_Z) {
        return vec3(facePlane.x, facePlane.y, 1.0);
    }
    return vec3(-facePlane.x, facePlane.y, -1.0);
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

    vec3 dir = directionForEquiangularFace(faceIndex, faceUV);
    dir = applyLegacyFaceTransform(dir, faceRotations[faceIndex], faceInversions[faceIndex]);
    dir.z = -dir.z;

    FragColor = sampleCubemapDirection(dir);
}
