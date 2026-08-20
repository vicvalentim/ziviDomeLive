#version 410 core
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
