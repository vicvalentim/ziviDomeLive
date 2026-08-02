// PBR vertex shader for the FulldomePBR example (Processing P3D).
// Feeds eye-space position/normal to the fragment stage. Lighting is computed
// in eye space; the view matrix (uViewMatrix) is supplied by the scene so that
// world-space lights are transformed consistently across all cubemap faces.

uniform mat4 transform;      // projection * modelview (Processing built-in)
uniform mat4 modelview;      // camera * model (Processing built-in)
uniform mat3 normalMatrix;   // inverse-transpose of modelview (built-in)

attribute vec4 position;
attribute vec4 color;
attribute vec3 normal;

varying vec3 vEyePos;
varying vec3 vEyeNormal;
varying vec4 vColor;

void main() {
  gl_Position = transform * position;
  vEyePos = (modelview * position).xyz;
  vEyeNormal = normalize(normalMatrix * normal);
  vColor = color;
}

