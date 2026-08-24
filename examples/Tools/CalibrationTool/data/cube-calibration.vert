#version 410 core
#define PROCESSING_TEXTURE_SHADER

uniform mat4 transform;

in vec4 position;
in vec2 texCoord;

out vec2 faceUv;

void main() {
  faceUv = texCoord;
  gl_Position = transform * position;
}
