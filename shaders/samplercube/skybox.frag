#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;
uniform int faceRotations[6];
uniform bool faceInversions[6];

out vec4 FragColor;

vec3 applyEAC(vec3 dir) {
    vec3 absDir = abs(dir);
    float scaleFactor = 1.0 / max(max(absDir.x, absDir.y), absDir.z);
    return dir * scaleFactor;
}

vec3 applyTransformations(vec3 dir, int rotation, bool invert) {
    for (int i = 0; i < rotation; i++) {
        dir = vec3(-dir.z, dir.y, dir.x);
    }

    if (invert) {
        dir.x = -dir.x;
    }

    return dir;
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    uv.y = 1.0 - uv.y;

    vec2 st = uv * vec2(4.0, 3.0);

    int faceIndex = -1;
    vec2 faceUV = vec2(0.0);

    if (st.y >= 1.0 && st.y < 2.0) {
        if (st.x >= 0.0 && st.x < 1.0) {
            faceIndex = 1;
            faceUV = vec2(st.x, st.y - 1.0);
        } else if (st.x >= 1.0 && st.x < 2.0) {
            faceIndex = 2;
            faceUV = vec2(st.x - 1.0, st.y - 1.0);
        } else if (st.x >= 2.0 && st.x < 3.0) {
            faceIndex = 3;
            faceUV = vec2(st.x - 2.0, st.y - 1.0);
        } else if (st.x >= 3.0 && st.x < 4.0) {
            faceIndex = 4;
            faceUV = vec2(st.x - 3.0, st.y - 1.0);
        }
    } else if (st.y >= 0.0 && st.y < 1.0 && st.x >= 1.0 && st.x < 2.0) {
        faceIndex = 0;
        faceUV = vec2(st.x - 1.0, st.y);
    } else if (st.y >= 2.0 && st.y < 3.0 && st.x >= 1.0 && st.x < 2.0) {
        faceIndex = 5;
        faceUV = vec2(st.x - 1.0, st.y - 2.0);
    }

    if (faceIndex == -1) {
        FragColor = vec4(0.0);
        return;
    }

    vec3 dir;

    if (faceIndex == 0) {
        dir = vec3(faceUV.x - 0.5, 0.5, 0.5 - faceUV.y);
    } else if (faceIndex == 1) {
        dir = vec3(-0.5, faceUV.y - 0.5, faceUV.x - 0.5);
    } else if (faceIndex == 2) {
        dir = vec3(faceUV.x - 0.5, faceUV.y - 0.5, 0.5);
    } else if (faceIndex == 3) {
        dir = vec3(0.5, faceUV.y - 0.5, 0.5 - faceUV.x);
    } else if (faceIndex == 4) {
        dir = vec3(0.5 - faceUV.x, faceUV.y - 0.5, -0.5);
    } else {
        dir = vec3(faceUV.x - 0.5, -0.5, faceUV.y - 0.5);
    }

    dir = applyTransformations(dir, faceRotations[faceIndex], faceInversions[faceIndex]);
    dir = applyEAC(normalize(dir));
    dir.z = -dir.z;

    FragColor = texture(cubemap, dir);
}
