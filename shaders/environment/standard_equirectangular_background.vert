#version 410 core

uniform mat4 transform;

in vec4 vertex;
out vec3 environmentDirection;

void main() {
    environmentDirection = vertex.xyz;
    gl_Position = transform * vertex;
}
