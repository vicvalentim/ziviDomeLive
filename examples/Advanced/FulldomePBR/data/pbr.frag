#version 410
#define PROCESSING_LIGHT_SHADER

// FulldomePBR fragment shader.
//
// Metallic-roughness Cook-Torrance GGX using Processing's native light
// uniforms. No custom face/view matrix is reconstructed by the example.
//
// Processing supplies lightPosition/lightNormal in the eye space of the
// currently active PGraphicsOpenGL camera. This is essential for cubemap
// capture: all six faces observe the same physical light rig while their
// individual view orientations remain projection concerns.

#define MAX_LIGHTS 8
#define PI 3.14159265359

// ---------------------------------------------------------------------
// Processing native LIGHT shader uniforms.
// These names are populated automatically by PShader/PGraphicsOpenGL.
// ---------------------------------------------------------------------

uniform int  lightCount;
uniform vec4 lightPosition[MAX_LIGHTS];
uniform vec3 lightNormal[MAX_LIGHTS];
uniform vec3 lightAmbient[MAX_LIGHTS];
uniform vec3 lightDiffuse[MAX_LIGHTS];

// ---------------------------------------------------------------------
// PBR material uniforms controlled by this example.
// ---------------------------------------------------------------------

uniform vec3  uAlbedo;
uniform float uMetallic;
uniform float uRoughness;
uniform vec3  uEmissive;

in vec3 vEyePos;
in vec3 vEyeNormal;
in vec4 vColor;

out vec4 fragColor;


// ---------------------------------------------------------------------
// Cook-Torrance / GGX
// ---------------------------------------------------------------------

float distributionGGX(
    vec3 N,
    vec3 H,
    float roughness) {

  float a = roughness * roughness;
  float a2 = a * a;

  float NdotH =
      max(dot(N, H), 0.0);

  float denominatorTerm =
      NdotH * NdotH * (a2 - 1.0) + 1.0;

  return a2 /
      (PI
       * denominatorTerm
       * denominatorTerm
       + 1e-5);
}


float geometrySchlickGGX(
    float NdotV,
    float roughness) {

  float r = roughness + 1.0;
  float k = (r * r) / 8.0;

  return NdotV /
      (NdotV * (1.0 - k) + k);
}


float geometrySmith(
    vec3 N,
    vec3 V,
    vec3 L,
    float roughness) {

  float NdotV =
      max(dot(N, V), 0.0);

  float NdotL =
      max(dot(N, L), 0.0);

  return
      geometrySchlickGGX(NdotV, roughness)
      * geometrySchlickGGX(NdotL, roughness);
}


vec3 fresnelSchlick(
    float cosTheta,
    vec3 F0) {

  return
      F0
      + (1.0 - F0)
      * pow(1.0 - cosTheta, 5.0);
}


// ---------------------------------------------------------------------
// Display transform
// ---------------------------------------------------------------------

vec3 acesFilmic(vec3 x) {
  const float a = 2.51;
  const float b = 0.03;
  const float c = 2.43;
  const float d = 0.59;
  const float e = 0.14;

  return clamp(
      (x * (a * x + b))
      / (x * (c * x + d) + e),
      0.0,
      1.0);
}


// ---------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------

void main() {

  vec3 N =
      normalize(vEyeNormal);

  /*
   * In eye space the camera origin is always (0,0,0).
   *
   * This remains true for every cubemap face. The face changes orientation,
   * but its eye-space camera origin does not become a new physical observer.
   */
  vec3 V =
      normalize(-vEyePos);

  vec3 albedo =
      uAlbedo * vColor.rgb;

  float metallic =
      clamp(uMetallic, 0.0, 1.0);

  float roughness =
      clamp(uRoughness, 0.04, 1.0);

  float NdotV =
      max(dot(N, V), 0.0);

  vec3 F0 =
      mix(
          vec3(0.04),
          albedo,
          metallic);

  vec3 directLighting =
      vec3(0.0);

  vec3 ambientLighting =
      vec3(0.0);

  for (int i = 0; i < MAX_LIGHTS; i++) {

    if (i >= lightCount) {
      break;
    }

    /*
     * Processing uses lightPosition.w to distinguish directional lights:
     *
     *   w < 1 : directional
     *   w = 1 : positional
     *
     * Directional orientation lives in lightNormal.
     */
    bool directional =
        lightPosition[i].w < 1.0;

    /*
     * Ambient lights contribute through lightAmbient. Other native
     * lights normally carry zero ambient contribution.
     */
    ambientLighting +=
        lightAmbient[i] * albedo;

    vec3 radiance =
        lightDiffuse[i];

    // Skip pure ambient entries.
    if (!any(greaterThan(radiance, vec3(0.0)))) {
      continue;
    }

    vec3 L;

    if (directional) {

      /*
       * lightNormal is already transformed by Processing into the eye
       * space of the current cubemap face.
       */
      L =
          normalize(-lightNormal[i]);

    } else {

      /*
       * lightPosition.xyz is likewise already in current eye space.
       */
      vec3 toLight =
          lightPosition[i].xyz - vEyePos;

      float distanceToLight =
          length(toLight);

      L =
          toLight
          / max(distanceToLight, 1e-4);

      /*
       * Preserve the previous FulldomePBR point-light response.
       * Only coordinate transformation ownership changes.
       */
      float attenuation =
          1.0
          / (1.0
             + 1.5e-6
             * distanceToLight
             * distanceToLight);

      radiance *= attenuation;
    }

    float NdotL =
        max(dot(N, L), 0.0);

    if (NdotL <= 0.0) {
      continue;
    }

    vec3 H =
        normalize(V + L);

    float NDF =
        distributionGGX(
            N,
            H,
            roughness);

    float G =
        geometrySmith(
            N,
            V,
            L,
            roughness);

    vec3 F =
        fresnelSchlick(
            max(dot(H, V), 0.0),
            F0);

    vec3 numerator =
        NDF * G * F;

    float denominator =
        4.0
        * NdotV
        * NdotL
        + 1e-4;

    vec3 specular =
        numerator / denominator;

    vec3 kd =
        (vec3(1.0) - F)
        * (1.0 - metallic);

    directLighting +=
        (kd * albedo / PI + specular)
        * radiance
        * NdotL;
  }

  /*
   * Neutral native ambient replaces the previous hemispheric pseudo-IBL.
   *
   * The previous sky/ground term depended on a manually reconstructed
   * face view matrix, which made it unsuitable as a cubemap-independent
   * lighting contract.
   */
  vec3 color =
      ambientLighting
      + directLighting
      + uEmissive;

  color =
      acesFilmic(
          max(color, vec3(0.0)));

  color =
      pow(
          color,
          vec3(1.0 / 2.2));

  fragColor =
      vec4(color, 1.0);
}
