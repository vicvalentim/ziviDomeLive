#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;

in vec3 reflectDir;
out vec4 fragColor;

void main() {
    vec3 direction = reflectDir * inversesqrt(max(dot(reflectDir, reflectDir), 1.0e-20));
    vec3 color = textureGrad(
            cubemap, direction, dFdx(direction), dFdy(direction)).rgb;
    fragColor = vec4(color, 1.0);
}
