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

vec3 legacyCubemapDirectionToSamplerCube(vec3 dir) {
    vec3 absDir = abs(dir);

    // The qualified 1.x face shaders map each face with the opposite vertical
    // texel convention from OpenGL samplerCube. Keep the same major face, but
    // flip the minor axis that OpenGL uses as cube-map T for that face.
    if (absDir.y >= absDir.x && absDir.y >= absDir.z) {
        return vec3(dir.x, dir.y, -dir.z);
    }
    return vec3(dir.x, -dir.y, dir.z);
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;

    float theta = uv.x * 2.0 * PI;
    float phi = uv.y * PI;

    float sinPhi = sin(phi);
    float cosPhi = cos(phi);
    float sinTheta = sin(theta);
    float cosTheta = cos(theta);

    vec3 dir = vec3(
        -sinPhi * sinTheta,
        cosPhi,
        -sinPhi * cosTheta
    );

    vec3 sampleDir = legacyCubemapDirectionToSamplerCube(applyEAC(normalize(dir)));
    FragColor = texture(cubemap, sampleDir);
}
