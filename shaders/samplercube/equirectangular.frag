#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;

out vec4 FragColor;

const float PI = 3.1415926535897932384626433832795;

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

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;

    /*
     * Convenção esférica qualificada da 1.5:
     *
     * +Z = Front
     * +Y = Top
     *
     * O centro horizontal da projeção equiretangular
     * (u = 0.5) corresponde a +Z Front.
     *
     * Esta equação é a tradução direta para samplerCube
     * da direção final usada pelo shader equirectangular
     * legado após sua rotação de 180 graus em Y.
     */
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

    FragColor = sampleCubemapDirection(dir);
}
