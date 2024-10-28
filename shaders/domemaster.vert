#version 410 core

uniform mat4 transform;
uniform mat4 modelview;
uniform mat3 normalMatrix;

in vec4 vertex;
in vec3 normal;

out vec3 vNormal;
out vec3 vPosition;

void main() {
    // Calcula a posição do vértice em espaço de tela
    gl_Position = transform * vertex;

    // Calcula a posição do vértice em coordenadas do olho (câmera)
    vPosition = vec3(modelview * vertex);

    // Calcula a normal do vértice em coordenadas do olho (câmera)
    vNormal = normalize(normalMatrix * normal);
}
