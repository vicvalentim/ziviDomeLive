#version 410 core

precision highp float;

uniform samplerCube cubemap;   // Cubemap de entrada
uniform vec2 resolution;       // Resolução do domemaster
uniform float fov;             // Campo de visão em graus (até 360 graus)

out vec4 FragColor;            // Cor de saída do pixel

const float PI = 3.1415926535897932384626433832795;

// Função para aplicar Equi-Angular Cubemap (EAC)
vec3 applyEAC(vec3 dir) {
    vec3 absDir = abs(dir);
    float scaleFactor = 1.0 / max(max(absDir.x, absDir.y), absDir.z);
    return dir * scaleFactor;
}

void main() {
    // Coordenadas normalizadas da tela [-1, 1]
    vec2 uv = (gl_FragCoord.xy / resolution) * 2.0 - 1.0;
    uv.y *= resolution.y / resolution.x; // Corrige a proporção para resolução não quadrada

    // Calcula o raio polar e ângulo azimutal
    float r = length(uv);               // Distância radial do centro
    float phi = atan(uv.y, uv.x);       // Ângulo azimutal [-PI, PI]

    // Limita o raio ao círculo da projeção
    if (r > 1.0) {
        FragColor = vec4(0.0); // Fora do círculo, cor preta
        return;
    }

    // Calcula o ângulo polar (theta) baseado no FOV
    float maxTheta = radians(fov); // Ângulo máximo permitido pelo FOV (em radianos)
    float theta = r * (maxTheta / 2.0); // Mapeia o raio para o campo de visão total

    // Converte para vetor direcional 3D (projeção esférica)
    vec3 dir = vec3(
        sin(theta) * cos(phi), // X
        sin(theta) * sin(phi), // Y
        cos(theta)             // Z
    );

    // Ajuste para o sistema do cubemap (invertendo Z para scale(-1, 1, -1))
    dir.z = -dir.z;

    // Aplica EAC ao vetor de direção
    dir = applyEAC(normalize(dir));

    // Amostra o cubemap com o vetor direcional ajustado
    FragColor = texture(cubemap, dir);
}
