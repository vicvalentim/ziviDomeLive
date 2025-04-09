#version 410

// Vertex Shader para mapeamento equiretangular da esfera celeste

in vec4 position;

out vec3 vDirection;

uniform mat4 modelview;
uniform mat4 projection;

void main() {
  // Converte o vértice para direção de visualização
  vec4 worldPos = modelview * position;
  vDirection = normalize(worldPos.xyz);

  gl_Position = projection * worldPos;
}
