#version 410 core

// Uniforms que Processing tipicamente envia:
//   - "transform" costuma ser a matriz (projection * modelview)
//   - "modelview" e "projection" podem aparecer, dependendo do modo
uniform mat4 transform;

// Atributos de vértice que Processing envia por padrão:
in vec4 position;
in vec3 normal;
in vec2 texcoord;

// Passamos as coordenadas de textura para o fragment
out vec2 vTexCoord;

void main() {
  // Usamos a matriz 'transform' (projection * modelview)
  gl_Position = transform * position;
  
  // Para um simples sample equiretangular, podemos usar
  // as texcoords geradas pelo createShape(SPHERE, ...).
  // Se você quiser calcular manualmente, pode usar a vPosition e
  // derivar (u,v) via atan2, etc. 
  vTexCoord = texcoord;
}
