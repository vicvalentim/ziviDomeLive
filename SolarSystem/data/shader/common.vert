// File: common.vert
#version 410 core
#define PROCESSING_TEXTURE_SHADER
#define PROCESSING_COLOR_SHADER

uniform mat4 transform;
uniform mat4 modelview;
uniform mat3 normalMatrix;

in vec4 vertex;
in vec3 normal;
in vec2 texcoord;

out vec3 vNormal;
out vec3 vPosition;
out vec2 vTexCoord;

void main() {
  vTexCoord = texcoord;
  vNormal = normalize(normalMatrix * normal);
  vPosition = vec3(modelview * vertex);
  gl_Position = transform * vertex;
}
