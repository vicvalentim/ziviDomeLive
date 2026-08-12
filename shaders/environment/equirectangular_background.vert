#version 410 core

uniform mat4 transform;

in vec4 vertex;

void main() {
    gl_Position = transform * vertex;
    gl_Position.z = gl_Position.w * 0.999999;
}
