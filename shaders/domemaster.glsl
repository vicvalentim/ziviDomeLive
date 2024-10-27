#version 410 core
#define PROCESSING_COLOR_SHADER

uniform sampler2D equirectangularMap;
uniform vec2 resolution; // Resolução da tela ou imagem de saída
uniform float fov; // Campo de visão em graus, permitindo até 360 graus

const float PI = 3.1415926535897932384626433832795;

// Output variable
out vec4 FragColor;

vec3 equirectangularToDir(vec2 uv) {
    float theta = uv.y * PI; // de 0 a PI
    float phi = uv.x * 2.0 * PI; // de 0 a 2*PI
    return vec3(sin(theta) * cos(phi), sin(theta) * sin(phi), cos(theta));
}

void main() {
    vec2 uv = (gl_FragCoord.xy / resolution) * 2.0 - 1.0;
    uv.x = -uv.x; // Inverter a coordenada x para girar 180 graus no eixo horizontal
    float phi = atan(uv.y, uv.x);
    float l = length(uv);

    if (l > 1.0) {
        discard;
    } else {
        float maxTheta = radians(min(fov, 360.0)) / 2.0;
        float theta = l * maxTheta;

        vec2 sphericalUV = vec2((phi / (2.0 * PI)) + 0.5, theta / PI);
        sphericalUV = clamp(sphericalUV, 0.0, 1.0); // Garante que UV fique dentro dos limites

        vec3 dir = equirectangularToDir(sphericalUV);

        float u = 0.5 + atan(dir.x, dir.z) / (2.0 * PI);
        float v = 0.5 - asin(clamp(dir.y, -1.0, 1.0)) / PI;
        u = fract(u); // Garante repetição sem exceder limites
        v = clamp(v, 0.0, 1.0); // Evita acesso fora dos limites verticais

        vec4 color = texture(equirectangularMap, vec2(u, v));
        FragColor = color;
    }
}