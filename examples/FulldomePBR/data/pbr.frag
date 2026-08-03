#version 410
// PBR fragment shader (metallic-roughness, Cook-Torrance GGX) for FulldomePBR.
// Lighting is evaluated in eye space. World-space lights are transformed to eye
// space with uViewMatrix (the scene's camera/view matrix), which keeps lighting
// consistent across every projection and cubemap face.
//
// Enrichment: hemispheric image-based ambient (sky/ground) with a roughness-aware
// Fresnel ambient specular term, plus ACES filmic tone mapping.


#define MAX_LIGHTS 4
#define PI 3.14159265359

uniform mat4 uViewMatrix;
uniform int  uLightCount;
uniform vec3 uLightPos[MAX_LIGHTS];
uniform vec3 uLightColor[MAX_LIGHTS];
uniform float uLightType[MAX_LIGHTS];
uniform vec3 uAmbient;

uniform vec3 uSkyColor;
uniform vec3 uGroundColor;
uniform float uEnvIntensity;

uniform vec3  uAlbedo;
uniform float uMetallic;
uniform float uRoughness;
uniform vec3  uEmissive;

in vec3 vEyePos;
in vec3 vEyeNormal;
in vec4 vColor;

out vec4 fragColor;


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

vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float rough) {
  vec3 Fr = max(vec3(1.0 - rough), F0);
  return F0 + (Fr - F0) * pow(1.0 - cosTheta, 5.0);
}

vec3 sampleEnv(vec3 dir, vec3 upEye) {
  float h = clamp(dot(dir, upEye) * 0.5 + 0.5, 0.0, 1.0);
  return mix(uGroundColor, uSkyColor, h);
}

vec3 acesFilmic(vec3 x) {
  const float a = 2.51;
  const float b = 0.03;
  const float c = 2.43;
  const float d = 0.59;
  const float e = 0.14;
  return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
  vec3 N = normalize(vEyeNormal);
  vec3 V = normalize(-vEyePos);
  vec3 albedo = uAlbedo * vColor.rgb;
  float metallic = clamp(uMetallic, 0.0, 1.0);
  float roughness = clamp(uRoughness, 0.04, 1.0);
  float NdotV = max(dot(N, V), 0.0);

  vec3 F0 = mix(vec3(0.04), albedo, metallic);
  vec3 Lo = vec3(0.0);

  for (int i = 0; i < MAX_LIGHTS; i++) {
    if (i >= uLightCount) break;

    vec3 radiance = uLightColor[i];
    vec3 L;

    if (uLightType[i] < 0.5) {
      vec3 dEye = (uViewMatrix * vec4(uLightPos[i], 0.0)).xyz;
      L = normalize(-dEye);
    } else {
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
    float denominator = 4.0 * NdotV * NdotL + 1e-4;
    vec3 specular = numerator / denominator;

    vec3 kd = (vec3(1.0) - F) * (1.0 - metallic);
    Lo += (kd * albedo / PI + specular) * radiance * NdotL;
  }

  // Hemispheric IBL ambient (diffuse + roughness-aware specular).
  vec3 upEye = normalize((uViewMatrix * vec4(0.0, 1.0, 0.0, 0.0)).xyz);
  vec3 irradiance = sampleEnv(N, upEye);
  vec3 R = reflect(-V, N);
  vec3 prefiltered = sampleEnv(R, upEye);

  vec3 Fr = fresnelSchlickRoughness(NdotV, F0, roughness);
  vec3 kdA = (vec3(1.0) - Fr) * (1.0 - metallic);
  vec3 ambientDiffuse = kdA * irradiance * albedo;
  vec3 ambientSpecular = prefiltered * Fr;
  vec3 ambient = uEnvIntensity * (ambientDiffuse + ambientSpecular) + uAmbient * albedo;

  vec3 color = ambient + Lo + uEmissive;

  color = acesFilmic(color);
  color = pow(color, vec3(1.0 / 2.2));

  fragColor = vec4(color, 1.0);
}

