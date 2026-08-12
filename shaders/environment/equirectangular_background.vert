#version 410 core

uniform mat4 transform;

in vec4 vertex;

void main() {
    gl_Position = transform * vertex;
    gl_Position = vec4(gl_Position.xy, gl_Position.w, gl_Position.w);
}
