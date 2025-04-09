#version 410

// Fragment Shader para os anéis com transparência

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D texture;

void main() {
  vec4 tex = texture2D(texture, vTexCoord);

  // Descarte pixels quase transparentes para dar realismo
  if (tex.a < 0.05) discard;

  fragColor = vec4(tex.rgb, tex.a);
}
