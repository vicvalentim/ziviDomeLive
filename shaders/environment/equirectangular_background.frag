#version 410 core
#define PROCESSING_COLOR_SHADER

uniform sampler2D environmentMap;
uniform vec2 resolution;
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
    float theta = atan(-dir.x, -dir.z) + yawOffset;
    float phi = asin(clamp(dir.y, -1.0, 1.0));
    float u = fract((PI - theta) / (2.0 * PI));
    float v = clamp((phi + PI * 0.5) / PI, 0.0, 1.0);
    return vec2(u, v);
}

void main() {
    vec2 faceUV = gl_FragCoord.xy / resolution;
    faceUV.y = 1.0 - faceUV.y;

    vec3 dir = directionForCanonicalFace(faceIndex, faceUV);
    dir.z = -dir.z;
    dir = (environmentRotation * vec4(dir, 0.0)).xyz;

    vec4 color = texture(environmentMap, equirectangularUv(dir));
    FragColor = vec4(color.rgb * max(intensity, 0.0), color.a);
}
