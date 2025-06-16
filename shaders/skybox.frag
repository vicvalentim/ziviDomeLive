#version 410 core

precision highp float;

uniform samplerCube cubemap;   // SampleCube como entrada
uniform vec2 resolution;       // Resolução total do cubemap horizontal (layout cruz deitada)

// Configuração de transformações para as faces
uniform int faceRotations[6];   // Número de rotações de 90 graus para cada face
uniform bool faceInversions[6]; // Se deve inverter horizontalmente cada face

out vec4 FragColor;

const float PI = 3.1415926535897932384626433832795;

// Função para aplicar transformação EAC (Equi-Angular Cubemap)
vec3 applyEAC(vec3 dir) {
    vec3 absDir = abs(dir);
    float scaleFactor = 1.0 / max(max(absDir.x, absDir.y), absDir.z);
    return dir * scaleFactor;
}

// Função para aplicar rotações e inversões em uma direção
vec3 applyTransformations(vec3 dir, int rotation, bool invert) {
    // Rotações (em incrementos de 90 graus)
    for (int i = 0; i < rotation; i++) {
        dir = vec3(-dir.z, dir.y, dir.x); // Roda 90 graus no sentido anti-horário no plano horizontal
    }

    // Inversão horizontal
    if (invert) {
        dir.x = -dir.x;
    }

    return dir;
}

void main() {
    // Coordenadas normalizadas [0, 1]
    vec2 uv = gl_FragCoord.xy / resolution;

    // Aplica inversão vertical global (de cima para baixo)
    uv.y = 1.0 - uv.y;

    // Resolução de cada face (1/4 da largura e 1/3 da altura)
    vec2 faceSize = vec2(resolution.x / 4.0, resolution.y / 3.0);

    // Coordenadas de recorte relativas à cruz (posição no layout)
    vec2 st = uv * vec2(4.0, 3.0);

    // Determina a face com base nas coordenadas
    int faceIndex = -1; // Face inválida por padrão
    vec2 faceUV;

    // Verifica em qual região da cruz as coordenadas estão
    if (st.y >= 1.0 && st.y < 2.0) { // Linha do meio (horizontal)
        if (st.x >= 0.0 && st.x < 1.0) {
            faceIndex = 1; // -X
            faceUV = vec2(st.x, st.y - 1.0);
        } else if (st.x >= 1.0 && st.x < 2.0) {
            faceIndex = 2; // +Z (central)
            faceUV = vec2(st.x - 1.0, st.y - 1.0);
        } else if (st.x >= 2.0 && st.x < 3.0) {
            faceIndex = 3; // +X
            faceUV = vec2(st.x - 2.0, st.y - 1.0);
        } else if (st.x >= 3.0 && st.x < 4.0) {
            faceIndex = 4; // -Z
            faceUV = vec2(st.x - 3.0, st.y - 1.0);
        }
    } else if (st.y >= 0.0 && st.y < 1.0 && st.x >= 1.0 && st.x < 2.0) { // Linha superior
        faceIndex = 0; // +Y
        faceUV = vec2(st.x - 1.0, st.y);
    } else if (st.y >= 2.0 && st.y < 3.0 && st.x >= 1.0 && st.x < 2.0) { // Linha inferior
        faceIndex = 5; // -Y
        faceUV = vec2(st.x - 1.0, st.y - 2.0);
    }

    // Se fora das regiões válidas, pinta transparente
    if (faceIndex == -1) {
        FragColor = vec4(0.0, 0.0, 0.0, 0.0);
        return;
    }

    // Mapeia as UVs para coordenadas de direção no cubemap
    vec3 dir;

    if (faceIndex == 0) {       // +Y (Topo)
        dir = vec3(faceUV.x - 0.5, 0.5, 0.5 - faceUV.y);
    } else if (faceIndex == 1) { // -X
        dir = vec3(-0.5, faceUV.y - 0.5, faceUV.x - 0.5);
    } else if (faceIndex == 2) { // +Z (Centro)
        dir = vec3(faceUV.x - 0.5, faceUV.y - 0.5, 0.5);
    } else if (faceIndex == 3) { // +X
        dir = vec3(0.5, faceUV.y - 0.5, 0.5 - faceUV.x);
    } else if (faceIndex == 4) { // -Z
        dir = vec3(0.5 - faceUV.x, faceUV.y - 0.5, -0.5);
    } else if (faceIndex == 5) { // -Y (Base)
        dir = vec3(faceUV.x - 0.5, -0.5, faceUV.y - 0.5);
    }

    // Aplica rotações e inversões específicas para a face
    dir = applyTransformations(dir, faceRotations[faceIndex], faceInversions[faceIndex]);

    // Aplica a transformação EAC ao vetor de direção
    dir = applyEAC(normalize(dir));

    // Ajuste para scale(-1, 1, -1)
    dir.z = -dir.z;

    // Amostra o cubemap
    FragColor = texture(cubemap, dir);
}
