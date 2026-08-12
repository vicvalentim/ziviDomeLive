#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;

in vec3 reflectDir;
out vec4 fragColor;

void main() {
    vec3 color = texture(cubemap, normalize(reflectDir)).rgb;
    fragColor = vec4(color, 1.0);
}
