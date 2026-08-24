#version 410 core
#define PROCESSING_COLOR_SHADER
uniform sampler2D environmentMap;
uniform vec2 environmentUvScale;
uniform vec2 environmentUvOffset;
uniform vec3 cameraRight;
uniform vec3 cameraUp;
uniform vec3 cameraBackward;
uniform mat4 environmentRotation;
uniform float yawOffset;
uniform float intensity;

in vec3 environmentDirection;
out vec4 FragColor;

const float PI = 3.1415926535897932384626433832795;

vec2 equirectangularUv(vec3 dir) {
    dir *= inversesqrt(max(dot(dir, dir), 1.0e-20));
    float theta = atan(-dir.x, -dir.z) - yawOffset;
    float u = fract(theta / (2.0 * PI));
    float v = acos(clamp(dir.y, -1.0, 1.0)) / PI;
    return vec2(u, v);
}

int wrapLongitudeIndex(int index, int size) {
    int wrapped = index % size;
    return wrapped < 0 ? wrapped + size : wrapped;
}

vec4 sampleEnvironmentLinear(vec2 uv) {
    ivec2 size = textureSize(environmentMap, 0);
    vec2 texelPosition = uv * vec2(size) - 0.5;
    ivec2 base = ivec2(floor(texelPosition));
    vec2 weight = fract(texelPosition);

    int x0 = wrapLongitudeIndex(base.x, size.x);
    int x1 = wrapLongitudeIndex(base.x + 1, size.x);
    int y0 = clamp(base.y, 0, size.y - 1);
    int y1 = clamp(base.y + 1, 0, size.y - 1);

    vec4 top = mix(
            texelFetch(environmentMap, ivec2(x0, y0), 0),
            texelFetch(environmentMap, ivec2(x1, y0), 0),
            weight.x);
    vec4 bottom = mix(
            texelFetch(environmentMap, ivec2(x0, y1), 0),
            texelFetch(environmentMap, ivec2(x1, y1), 0),
            weight.x);
    return mix(top, bottom, weight.y);
}

void main() {
	vec3 worldDirection =
			environmentDirection.x * cameraRight
			+ environmentDirection.y * cameraUp
			+ environmentDirection.z * cameraBackward;
	worldDirection *= inversesqrt(max(dot(worldDirection, worldDirection), 1.0e-20));
	worldDirection = (environmentRotation * vec4(worldDirection, 0.0)).xyz;
	vec2 environmentUV = equirectangularUv(worldDirection);
	environmentUV = environmentUV * environmentUvScale + environmentUvOffset;
	vec4 color = sampleEnvironmentLinear(environmentUV);
    FragColor = vec4(color.rgb * max(intensity, 0.0), color.a);
}
