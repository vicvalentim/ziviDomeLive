#version 410 core
#define PROCESSING_COLOR_SHADER

uniform sampler2D posX, negX, posY, negY, posZ, negZ;
uniform vec2 resolution;

const float PI = 3.1415926535897932384626433832795;
const float edgeBlendWidth = 0.02; // Controle de largura da transição

// Output variable
out vec4 FragColor;

// Função que aplica a transformação Equi-Angular (EAC) no cubemap
vec3 applyEACMapping(vec3 dir) {
    float theta = acos(dir.y);        // Ângulo polar (latitude)
    float phi = atan(dir.z, dir.x);   // Ângulo azimutal (longitude)

    // Aplica a transformação EAC ao ângulo polar
    float eacTheta = 2.0 * atan(tan(theta * 0.5));

    // Converte de volta para coordenadas cartesianas
    return normalize(vec3(sin(eacTheta) * cos(phi), cos(eacTheta), sin(eacTheta) * sin(phi)));
}

// Converte coordenadas XYZ para UV e face
void convert_xyz_to_cube_uv(float x, float y, float z, out int index, out vec2 uv) {
    float absX = abs(x);
    float absY = abs(y);
    float absZ = abs(z);

    bool isXPositive = x > 0.0;
    bool isYPositive = y > 0.0;
    bool isZPositive = z > 0.0;

    float maxAxis, uc, vc;

    if (isXPositive && absX >= absY && absX >= absZ) {
        maxAxis = absX;
        uc = -z;
        vc = y;
        index = 0; // +X
    } else if (!isXPositive && absX >= absY && absX >= absZ) {
        maxAxis = absX;
        uc = z;
        vc = y;
        index = 1; // -X
    } else if (isYPositive && absY >= absX && absY >= absZ) {
        maxAxis = absY;
        uc = x;
        vc = -z;
        index = 2; // +Y
    } else if (!isYPositive && absY >= absX && absY >= absZ) {
        maxAxis = absY;
        uc = x;
        vc = z;
        index = 3; // -Y
    } else if (isZPositive && absZ >= absX && absZ >= absY) {
        maxAxis = absZ;
        uc = x;
        vc = y;
        index = 4; // +Z
    } else {
        maxAxis = absZ;
        uc = -x;
        vc = y;
        index = 5; // -Z
    }

    uv = clamp(0.5 * (vec2(uc, vc) / maxAxis + 1.0), 0.0, 1.0);
}

vec4 sampleCubemapFace(vec3 dir) {
    vec2 uvCube;
    int index;

    // Aplica mapeamento EAC na direção
    vec3 adjustedDir = applyEACMapping(dir);

    // Converte direção ajustada para face do cubemap e coordenadas UV
    convert_xyz_to_cube_uv(adjustedDir.x, adjustedDir.y, adjustedDir.z, index, uvCube);

    // Amostra a face correspondente sem interpolação
    vec4 color;
    if (index == 0) color = texture(posX, uvCube);
    else if (index == 1) color = texture(negX, uvCube);
    else if (index == 2) color = texture(posY, uvCube);
    else if (index == 3) color = texture(negY, uvCube);
    else if (index == 4) color = texture(posZ, uvCube);
    else if (index == 5) color = texture(negZ, uvCube);

    return color;
}

// Função de interpolação bilinear
vec4 bilinearInterpolate(sampler2D tex, vec2 uv) {
    vec2 texSize = vec2(textureSize(tex, 0));
    vec2 f = fract(uv * texSize);
    uv -= f / texSize;

    vec4 p00 = texture(tex, uv);
    vec4 p10 = texture(tex, uv + vec2(1.0, 0.0) / texSize);
    vec4 p01 = texture(tex, uv + vec2(0.0, 1.0) / texSize);
    vec4 p11 = texture(tex, uv + vec2(1.0, 1.0) / texSize);

    vec4 col0 = mix(p00, p10, f.x);
    vec4 col1 = mix(p01, p11, f.x);

    return mix(col0, col1, f.y);
}

void main() {
    // Calcula a direção com base nas coordenadas da tela
    vec2 uv = gl_FragCoord.xy / resolution;
    float theta = uv.x * 2.0 * PI;
    float phi = uv.y * PI;
    vec3 dir = vec3(sin(phi) * sin(theta), cos(phi), sin(phi) * cos(theta));

    // Rotaciona 180 graus ao redor do eixo vertical
    dir.x = -dir.x;
    dir.z = -dir.z;

    // Amostra a cor resultante da amostragem direta do cubemap
    vec4 color = sampleCubemapFace(dir);

    // Aplica interpolação bilinear na cor final
    FragColor = color;
}