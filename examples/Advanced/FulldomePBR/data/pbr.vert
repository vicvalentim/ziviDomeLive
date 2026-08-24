#version 410
#define PROCESSING_LIGHT_SHADER

// FulldomePBR vertex shader.
//
// Declaring PROCESSING_LIGHT_SHADER opts this shader into Processing's
// native LIGHT contract. Processing owns the current modelview/normal
// matrices and the light state for the active PGraphicsOpenGL.
//
// During ziviDomeLive cubemap capture the active camera changes for each
// face. Processing therefore supplies the matching eye-space matrices and
// matching eye-space light state automatically.

uniform mat4 transformMatrix;
uniform mat4 modelviewMatrix;
uniform mat3 normalMatrix;

in vec4 position;
in vec4 color;
in vec3 normal;

out vec3 vEyePos;
out vec3 vEyeNormal;
out vec4 vColor;

void main() {
  gl_Position = transformMatrix * position;

  vEyePos =
      (modelviewMatrix * position).xyz;

  vEyeNormal =
      normalize(normalMatrix * normal);

  vColor = color;
}
