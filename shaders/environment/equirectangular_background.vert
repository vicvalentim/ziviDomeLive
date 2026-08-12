#version 410 core

const vec2 FULLSCREEN_TRIANGLE[3] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 3.0, -1.0),
    vec2(-1.0,  3.0)
);

void main() {
    gl_Position = vec4(FULLSCREEN_TRIANGLE[gl_VertexID], 1.0, 1.0);
}
