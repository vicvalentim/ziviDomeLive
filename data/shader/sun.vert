#version 410

uniform mat4 transform;
uniform mat3 normalMatrix;

in vec4 position;
in vec3 normal;
in vec2 texcoord;

out vec3 vNormal;
out vec3 vPosition;
out vec2 vTexCoord;

void main() {
  vNormal = normalize(normalMatrix * normal);
  vTexCoord = texcoord;
  vPosition = vec3(transform * position);
  gl_Position = transform * position;
}
