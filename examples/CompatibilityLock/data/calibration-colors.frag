#version 410 core
#define PROCESSING_COLOR_SHADER

uniform float targetSize;

in vec2 localPosition;

out vec4 fragColor;

float rectangleMask(vec2 point, vec2 minimum, vec2 maximum) {
  vec2 lower = step(minimum, point);
  vec2 upper = step(point, maximum);
  return lower.x * lower.y * upper.x * upper.y;
}

vec3 colorBar(int index) {
  if (index == 0) return vec3(1.0, 0.0, 0.0);
  if (index == 1) return vec3(0.0, 1.0, 0.0);
  if (index == 2) return vec3(0.0, 0.0, 1.0);
  if (index == 3) return vec3(0.0, 1.0, 1.0);
  if (index == 4) return vec3(1.0, 0.0, 1.0);
  if (index == 5) return vec3(1.0, 1.0, 0.0);
  if (index == 6) return vec3(1.0);
  return vec3(0.0);
}

void main() {
  float halfSize = targetSize * 0.5;
  vec3 result = vec3(18.0 / 255.0);

  float colorWidth = 132.0;
  float colorLeft = -4.0 * colorWidth;
  float colorBottom = halfSize - 334.0;
  float colorTop = halfSize - 226.0;
  float inColorBars = rectangleMask(
      localPosition,
      vec2(colorLeft, colorBottom),
      vec2(-colorLeft, colorTop));
  if (inColorBars > 0.5) {
    int index = int(clamp(
        floor((localPosition.x - colorLeft) / colorWidth),
        0.0,
        7.0));
    result = colorBar(index);
  }

  float grayWidth = 118.0;
  float grayLeft = -4.5 * grayWidth;
  float grayBottom = halfSize - 181.0;
  float grayTop = halfSize - 109.0;
  float inGrayRamp = rectangleMask(
      localPosition,
      vec2(grayLeft, grayBottom),
      vec2(-grayLeft, grayTop));
  if (inGrayRamp > 0.5) {
    int index = int(clamp(
        floor((localPosition.x - grayLeft) / grayWidth),
        0.0,
        8.0));
    float level = float(index) / 8.0;
    result = vec3(level);
  }

  fragColor = vec4(result, 1.0);
}
