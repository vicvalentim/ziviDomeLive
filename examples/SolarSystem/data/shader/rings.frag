// File: rings.frag
#version 410 core
#define PROCESSING_TEXTURE_SHADER

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D texSampler;

void main() {
  vec4 tex = texture(texSampler, vTexCoord);
  if (tex.a < 0.05) discard;
  fragColor = vec4(tex.rgb, tex.a);
}
