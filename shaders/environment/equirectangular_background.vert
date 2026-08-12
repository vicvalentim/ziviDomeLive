#version 410 core

uniform mat4 transform;

in vec4 vertex;

void main() {
    gl_Position = transform * vertex;
}
