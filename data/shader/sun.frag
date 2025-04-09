#version 410

uniform sampler2D texSampler;  // <- Nome alterado aqui
uniform float time;
uniform vec3 lightColor;

in vec3 vNormal;
in vec3 vPosition;
in vec2 vTexCoord;

out vec4 fragColor;

float hash(vec2 p) {
  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}
float noise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  vec2 u = f*f*(3.0 - 2.0*f);
  return mix(
    mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
    mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
    u.y
  );
}
float plasma(vec2 uv, float t) {
  float n = 0.0;
  float scale = 1.0;
  for (int i = 0; i < 4; i++) {
    n += noise(uv * scale + t * 0.1) / scale;
    scale *= 2.0;
  }
  return n;
}

void main() {
  vec3 N = normalize(vNormal);
  vec3 viewDir = normalize(-vPosition);
  float intensity = pow(dot(N, viewDir), 2.0);

  vec2 uv = vTexCoord * 4.0;

  float p = plasma(uv, time);

  // <- Atualizado aqui:
  vec3 texColor = texture(texSampler, vTexCoord).rgb;

  float corona = smoothstep(0.7, 1.0, length(vTexCoord - 0.5)) * 0.4;

  vec3 emissive = texColor * 1.2 + vec3(p * 0.3, p * 0.15, 0.05);
  emissive += corona * vec3(1.0, 0.85, 0.3);

  fragColor = vec4(emissive * lightColor * (1.2 + intensity), 1.0);
}
