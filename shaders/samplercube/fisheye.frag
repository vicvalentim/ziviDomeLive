#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;
uniform float fov;
uniform float sizePercentage;

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
    vec2 uv = (gl_FragCoord.xy / resolution) * 2.0 - 1.0;
    uv.y *= resolution.y / resolution.x;

    float sizeScale = clamp(sizePercentage, 0.0, 100.0) / 100.0;
    if (sizeScale <= 0.0) {
        FragColor = vec4(0.0);
        return;
    }
    uv /= sizeScale;

    float r = length(uv);
    float phi = atan(uv.y, uv.x);

    float pixelWidth = max(fwidth(r), 1.0 / max(resolution.x, resolution.y));
    float coverage = 1.0 - smoothstep(1.0 - pixelWidth, 1.0 + pixelWidth, r);

    float maxTheta = radians(clamp(fov, 0.0, 360.0));
    float theta = r * (maxTheta / 2.0);

    /*
     * Equivalente direto, via samplerCube, à cadeia qualificada:
     *
     * Domemaster 1.5
     *      ->
     * Equirectangular 1.5
     *      ->
     * Cubemap
     *
     * O centro amostra a face +Z do cubemap. A orientação frontal da cena
     * permanece definida separadamente pelos controles esféricos.
     *
     * A inversão de Y preserva a orientação azimutal produzida
     * anteriormente pela combinação do uv.x invertido no
     * Domemaster legado com o mapeamento Equirectangular.
     */
    vec3 dir = vec3(
            sin(theta) * cos(phi),
            -sin(theta) * sin(phi),
            cos(theta)
    );

    vec4 color = sampleCubemapDirection(dir);
    FragColor = vec4(color.rgb, color.a * coverage);
}
