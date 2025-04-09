#version 410 core

uniform sampler2D texture;

in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    vec4 texColor = texture(texture, vTexCoord);
    fragColor = texColor;
}
