#version 410 core
#define PROCESSING_COLOR_SHADER

uniform mat4 transform;

in vec4 position;

out vec2 localPosition;

void main() {
  localPosition = position.xy;
  gl_Position = transform * position;
}
