#version 410 core

layout(location = 0) in vec4 vertex;  // Posição do vértice
layout(location = 1) in vec3 normal;  // Normal do vértice

uniform mat4 transform;      // Matriz de transformação final
uniform mat4 modelview;      // Matriz modelo-visão
uniform mat3 normalMatrix;   // Matriz para transformar as normais

out vec3 reflectDir;         // Direção refletida para o fragment shader

void main() {
    gl_Position = transform * vertex; // Calcula a posição final no espaço de tela

    // Calcula o vetor normal e direção refletida no espaço do olho
    vec3 ecNormal = normalize(normalMatrix * normal); // Normal transformada
    vec3 ecVertex = vec3(modelview * vertex);         // Posição do vértice no espaço da câmera
    vec3 eyeDir = ecVertex;                           // Direção do olho (origem da câmera)
    reflectDir = reflect(eyeDir, ecNormal);           // Direção refletida
}
