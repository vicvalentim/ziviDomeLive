#version 410 core
#define PROCESSING_COLOR_SHADER

uniform samplerCube cubemap;
uniform vec2 resolution;

const float PI = 3.1415926535897932384626433832795;

out vec4 FragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    float theta = uv.x * 2.0 * PI;
    float phi = uv.y * PI;
    vec3 dir = vec3(sin(phi) * sin(theta), cos(phi), sin(phi) * cos(theta));
    dir.x = -dir.x;
    dir.z = -dir.z;
    FragColor = texture(cubemap, dir);
}
