#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;
uniform float fov;

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
    vec2 uv = (gl_FragCoord.xy / resolution) * 2.0 - 1.0;
    uv.y *= resolution.y / resolution.x;

    float r = length(uv);
    float phi = atan(uv.y, uv.x);

    if (r > 1.0) {
        FragColor = vec4(0.0);
        return;
    }

    float maxTheta = radians(clamp(fov, 0.0, 360.0));
    float theta = r * (maxTheta / 2.0);

    vec3 dir = vec3(
        sin(theta) * cos(phi),
        sin(theta) * sin(phi),
        cos(theta)
    );
    dir.z = -dir.z;

    FragColor = sampleCubemapEAC(dir);
}
