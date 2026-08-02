// PBR fragment shader (metallic-roughness, Cook-Torrance GGX) for FulldomePBR.
// Lighting is evaluated in eye space. World-space lights are transformed to eye
// space with uViewMatrix (the scene's camera/view matrix), which keeps lighting
// consistent across every projection and cubemap face.

#ifdef GL_ES
precision highp float;
precision highp int;
#endif

#define MAX_LIGHTS 4
#define PI 3.14159265359

uniform mat4 uViewMatrix;               // world -> eye (view) matrix
uniform int  uLightCount;
uniform vec3 uLightPos[MAX_LIGHTS];     // point: world position | dir: travel direction
uniform vec3 uLightColor[MAX_LIGHTS];   // radiance (linear-ish)
uniform float uLightType[MAX_LIGHTS];   // 0.0 = directional, 1.0 = point
uniform vec3 uAmbient;                  // ambient irradiance

uniform vec3  uAlbedo;                   // base color (0..1)
uniform float uMetallic;                 // 0..1
uniform float uRoughness;                // 0..1
uniform vec3  uEmissive;                 // emissive color (0..1)

varying vec3 vEyePos;
varying vec3 vEyeNormal;
varying vec4 vColor;

float distributionGGX(vec3 N, vec3 H, float rough) {
  float a = rough * rough;
  float a2 = a * a;
  float NdotH = max(dot(N, H), 0.0);
  float d = (NdotH * NdotH * (a2 - 1.0) + 1.0);
  return a2 / (PI * d * d + 1e-5);
}

float geometrySchlickGGX(float NdotV, float rough) {
  float r = rough + 1.0;
  float k = (r * r) / 8.0;
  return NdotV / (NdotV * (1.0 - k) + k);
}

float geometrySmith(vec3 N, vec3 V, vec3 L, float rough) {
  float NdotV = max(dot(N, V), 0.0);
  float NdotL = max(dot(N, L), 0.0);
  return geometrySchlickGGX(NdotV, rough) * geometrySchlickGGX(NdotL, rough);
}

vec3 fresnelSchlick(float cosTheta, vec3 F0) {
  return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

void main() {
  vec3 N = normalize(vEyeNormal);
  vec3 V = normalize(-vEyePos);
  vec3 albedo = uAlbedo * vColor.rgb;
  float metallic = clamp(uMetallic, 0.0, 1.0);
  float roughness = clamp(uRoughness, 0.04, 1.0);

  vec3 F0 = mix(vec3(0.04), albedo, metallic);
  vec3 Lo = vec3(0.0);

  for (int i = 0; i < MAX_LIGHTS; i++) {
    if (i >= uLightCount) break;

    vec3 radiance = uLightColor[i];
    vec3 L;

    if (uLightType[i] < 0.5) {
      // Directional light: uLightPos holds the direction the light travels.
      vec3 dEye = (uViewMatrix * vec4(uLightPos[i], 0.0)).xyz;
      L = normalize(-dEye);
    } else {
      // Point light: uLightPos holds a world-space position.
      vec3 pEye = (uViewMatrix * vec4(uLightPos[i], 1.0)).xyz;
      vec3 diff = pEye - vEyePos;
      float dist = length(diff);
      L = diff / max(dist, 1e-4);
      float atten = 1.0 / (1.0 + 1.5e-6 * dist * dist);
      radiance *= atten;
    }

    vec3 H = normalize(V + L);
    float NdotL = max(dot(N, L), 0.0);

    float NDF = distributionGGX(N, H, roughness);
    float G = geometrySmith(N, V, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * NdotL + 1e-4;
    vec3 specular = numerator / denominator;

    vec3 kd = (vec3(1.0) - F) * (1.0 - metallic);
    Lo += (kd * albedo / PI + specular) * radiance * NdotL;
  }

  vec3 ambient = uAmbient * albedo;
  vec3 color = ambient + Lo + uEmissive;

  // Reinhard tone mapping + gamma correction.
  color = color / (color + vec3(1.0));
  color = pow(color, vec3(1.0 / 2.2));

  gl_FragColor = vec4(color, 1.0);
}

