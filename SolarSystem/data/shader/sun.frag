#version 410 core

in vec2 vTexCoord;     // Coordenada de textura interpolada do vértice (normalizada: 0.0 a 1.0)
out vec4 fragColor;    // Cor final do fragmento

uniform float time;    // Uniform para controle temporal (animação)

// Função de pseudo-noise simples para variação (não é um noise real, mas serve para introduzir irregularidades)
float pseudoNoise(vec2 st) {
    return fract(sin(dot(st, vec2(12.9898,78.233))) * 43758.5453123);
}

void main() {
    // Ajusta as coordenadas para centralizar o efeito (centro em (0.5, 0.5))
    vec2 st = vTexCoord - vec2(0.5);
    
    // Calcula a distância radial a partir do centro; o fator 2.0 pode ser ajustado para "espalhar" a influência do gradiente
    float radius = length(st) * 2.0;
    
    // Cria um gradiente suave: maior intensidade no centro, decaindo suavemente para as bordas
    float gradient = smoothstep(1.0, 0.0, radius);
    
    // Aplica um efeito de pulsação, simulando variações dinâmicas na intensidade emissiva
    float pulsation = 0.1 * sin(time * 3.0);
    gradient += pulsation;
    
    // Incorpora uma leve variação utilizando pseudoNoise para simular irregularidades na "superfície" do sol
    float noise = pseudoNoise(vTexCoord * 10.0 + time);
    float noiseFactor = mix(0.9, 1.1, noise);
    
    // Define a cor base do sol, típica em tons amarelos/laranjas
    vec3 sunColor = vec3(1.0, 0.8, 0.0);
    
    // Combina os efeitos de gradiente, pulsação e variação de noise para simular a emissão
    fragColor = vec4(sunColor * gradient * noiseFactor, 1.0);
}
