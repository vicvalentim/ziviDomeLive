#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;

out vec4 FragColor;

const float PI = 3.1415926535897932384626433832795;

vec3 applyEAC(vec3 dir) {
    vec3 absDir = abs(dir);
    float scaleFactor = 1.0 / max(max(absDir.x, absDir.y), absDir.z);
    return dir * scaleFactor;
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

    FragColor = texture(cubemap, applyEAC(normalize(dir)));
}
