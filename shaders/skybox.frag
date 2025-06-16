#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;

out vec4 FragColor;

void main() {
    vec2 uv = (gl_FragCoord.xy / resolution) * 2.0 - 1.0;
    vec3 dir = normalize(vec3(uv, 1.0));
    dir.x = -dir.x;
    FragColor = texture(cubemap, dir);
}
