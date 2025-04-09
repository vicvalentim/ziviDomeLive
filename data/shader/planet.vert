#version 410 core

uniform mat4 transform;
uniform mat4 projection;
uniform mat4 modelview;

in vec4 position;
in vec3 normal;
in vec2 texcoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = texcoord;
    gl_Position = projection * modelview * position;
}
