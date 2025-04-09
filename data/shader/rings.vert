#version 410

// Vertex Shader para os anéis de Saturno

in vec4 position;
in vec2 texcoord;

uniform mat4 transform;

out vec2 vTexCoord;

void main() {
  vTexCoord = texcoord;
  gl_Position = transform * position;
}
