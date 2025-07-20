#version 410 core

// Recebemos as variáveis de "out" do vertex shader:
in vec2 vTexCoord;

// A cor final do pixel
out vec4 fragColor;

// O sampler2D atrelado ao "texSampler" no ShaderManager
uniform sampler2D texSampler;

void main() {
  // Simplesmente lê a cor da textura usando as coordenadas
  // que vêm do vertex shader:
  fragColor = texture(texSampler, vTexCoord);
}
