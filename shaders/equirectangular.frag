#version 410 core

// Declaração explícita da precisão
precision highp float;

uniform samplerCube cubemap;   // Cubemap de entrada
uniform vec2 resolution;       // Resolução do retângulo 2:1 (width, height)

out vec4 FragColor;            // Cor de saída do pixel

const float PI = 3.1415926535897932384626433832795;

// Função para aplicar transformação EAC (Equi-Angular Cubemap)
vec3 applyEAC(vec3 dir) {
    // Calcula as componentes absolutas
    vec3 absDir = abs(dir);

    // Determina o fator de escala para minimizar distorções nas bordas
    float scaleFactor = 1.0 / max(max(absDir.x, absDir.y), absDir.z);

    // Retorna o vetor ajustado
    return dir * scaleFactor;
}

void main() {
    // Coordenadas normalizadas da tela (0 a 1)
    vec2 uv = gl_FragCoord.xy / resolution;

    // Converte UV para longitude (theta) e latitude (phi)
    float theta = -(uv.x * 2.0 * PI - PI); // Longitude invertida para alinhar as direções
    float phi = uv.y * PI - PI / 2.0;   // Latitude alinhada para +Y e -Y

    // Pré-calcula os valores trigonométricos para eficiência
    float sinPhi = sin(phi);
    float cosPhi = cos(phi);
    float sinTheta = sin(theta);
    float cosTheta = cos(theta);

    // Constrói o vetor de direção 3D no sistema do cubemap
    vec3 dir = vec3(
        -cosPhi * sinTheta,  // X invertido devido ao scale(-1, 1, -1)
        sinPhi,              // Y consistente com o sistema de coordenadas
        -cosPhi * cosTheta   // Z invertido devido ao scale(-1, 1, -1)
    );

    // Aplica a transformação EAC ao vetor de direção
    dir = applyEAC(dir);

    // Amostra o cubemap usando o vetor direcional transformado
    FragColor = texture(cubemap, dir);
}
