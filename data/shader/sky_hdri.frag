#version 410

// Fragment Shader para renderização HDRi do céu

uniform sampler2D skyTexture;
in vec3 vDirection;

out vec4 fragColor;

// Conversão de direção para coordenadas UV em projeção equiretangular
vec2 getEquirectUV(vec3 dir) {
  float longitude = atan(dir.z, dir.x);
  float latitude = asin(clamp(dir.y, -1.0, 1.0));
  return vec2((longitude / 3.1415926 + 1.0) * 0.5, (latitude / 1.5707963 + 1.0) * 0.5);
}

void main() {
  vec3 dir = normalize(vDirection);
  vec2 uv = getEquirectUV(dir);

  vec3 texColor = texture(skyTexture, uv).rgb;

  // Simula um leve efeito HDRi (brilho suave no topo)
  float brightnessBoost = 1.0 + 0.2 * pow(max(dir.y, 0.0), 2.0);
  fragColor = vec4(texColor * brightnessBoost, 1.0);
}
