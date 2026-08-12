#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;

out vec4 FragColor;

const float PI = 3.1415926535897932384626433832795;

vec3 applyEAC(vec3 dir) {
    vec3 absDir = abs(dir);
    float dominantAxis = max(max(absDir.x, absDir.y), absDir.z);
    return dir / max(dominantAxis, 0.000001);
}

vec4 sampleCubemapEAC(vec3 dir) {
    return texture(cubemap, applyEAC(normalize(dir)));
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;

    float theta = -(uv.x * 2.0 * PI - PI);
    float phi = uv.y * PI - PI / 2.0;

    float sinPhi = sin(phi);
    float cosPhi = cos(phi);
    float sinTheta = sin(theta);
    float cosTheta = cos(theta);

    vec3 dir = vec3(
        -cosPhi * sinTheta,
        sinPhi,
        -cosPhi * cosTheta
    );

    FragColor = sampleCubemapEAC(dir);
}
