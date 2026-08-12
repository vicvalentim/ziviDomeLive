#version 410 core

uniform mat4 transform;
uniform mat4 modelview;
uniform mat3 normalMatrix;

in vec4 vertex;
in vec3 normal;

out vec3 reflectDir;

void main() {
    gl_Position = transform * vertex;

    vec3 ecNormal = normalize(normalMatrix * normal);
    vec3 ecVertex = vec3(modelview * vertex);
    reflectDir = reflect(ecVertex, ecNormal);
}
