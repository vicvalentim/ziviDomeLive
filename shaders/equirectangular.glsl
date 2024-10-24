#version 410 core

const float PI = 3.1415926535897932384626433832795;

uniform sampler2D posX, negX, posY, negY, posZ, negZ;
uniform vec2 resolution;

out vec4 fragColor;

void convert_xyz_to_cube_uv(float x, float y, float z, out int index, out vec2 uv) {
    float absX = abs(x);
    float absY = abs(y);
    float absZ = abs(z);

    bool isXPositive = x > 0.0;
    bool isYPositive = y > 0.0;
    bool isZPositive = z > 0.0;

    float maxAxis, uc, vc;

    // Determine the major axis of projection
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

    uv = 0.5 * (vec2(uc, vc) / maxAxis + 1.0);
}

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    float theta = uv.x * 2.0 * PI; // Horizontal angle
    float phi = uv.y * PI; // Vertical angle
    vec3 dir = vec3(sin(phi) * sin(theta), cos(phi), sin(phi) * cos(theta));

    // Rotate 180 degrees around the vertical axis
    dir.x = -dir.x;
    dir.z = -dir.z;

    vec2 uvCube;
    int index;
    convert_xyz_to_cube_uv(dir.x, dir.y, dir.z, index, uvCube);

    switch(index) {
        case 0: fragColor = texture(posX, uvCube); break;
        case 1: fragColor = texture(negX, uvCube); break;
        case 2: fragColor = texture(posY, uvCube); break;
        case 3: fragColor = texture(negY, uvCube); break;
        case 4: fragColor = texture(posZ, uvCube); break;
        case 5: fragColor = texture(negZ, uvCube); break;
        default: fragColor = vec4(0.0); break;
    }
}