// File: planet.frag  (e pode ser reutilizado como sun.frag)
#version 410 core
#define PROCESSING_TEXTURE_SHADER

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D texSampler;

void main() {
  vec4 texColor = texture(texSampler, vTexCoord);
  fragColor = texColor;
}
