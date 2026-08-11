#version 410 core
#define PROCESSING_COLOR_SHADER

uniform float targetSize;
uniform vec2 faceResolution;
uniform float gridDivisions;
uniform int faceIndex;
uniform vec3 accentColor;

in vec2 localPosition;

out vec4 fragColor;

float rectangleMask(vec2 point, vec2 minimum, vec2 maximum) {
  vec2 lower = step(minimum, point);
  vec2 upper = step(point, maximum);
  return lower.x * lower.y * upper.x * upper.y;
}

float periodicDistance(float value, float spacing) {
  float cell = mod(value, spacing);
  return min(cell, spacing - cell);
}

float rectangleOutline(
    vec2 point,
    vec2 center,
    vec2 halfSize,
    float width,
    float antialiasWidth) {
  vec2 distanceToEdge = abs(point - center) - halfSize;
  float edge = abs(max(distanceToEdge.x, distanceToEdge.y));
  return 1.0 - smoothstep(width, width + antialiasWidth, edge);
}

float circleOutline(
    vec2 point,
    vec2 center,
    float radius,
    float width,
    float antialiasWidth) {
  float edge = abs(length(point - center) - radius);
  return 1.0 - smoothstep(width, width + antialiasWidth, edge);
}

float squarePoint(vec2 point, vec2 center, float sizePixels, float resolution) {
  vec2 deltaPixels = abs(point - center) * resolution;
  return 1.0 - step(sizePixels * 0.5, max(deltaPixels.x, deltaPixels.y));
}

float starBurst(vec2 point, vec2 center, float radiusPixels, float resolution) {
  vec2 deltaPixels = (point - center) * resolution;
  float core = 1.0 - step(radiusPixels, length(deltaPixels));
  float horizontal = (1.0 - step(0.65, abs(deltaPixels.y)))
      * (1.0 - step(radiusPixels * 4.0, abs(deltaPixels.x)));
  float vertical = (1.0 - step(0.65, abs(deltaPixels.x)))
      * (1.0 - step(radiusPixels * 4.0, abs(deltaPixels.y)));
  return max(core, max(horizontal, vertical));
}

float hash21(vec2 value) {
  return fract(sin(dot(value, vec2(127.1, 311.7))) * 43758.5453123);
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

vec3 gradientColor(int index) {
  if (index == 0) return vec3(1.0, 0.0, 0.0);
  if (index == 1) return vec3(0.0, 1.0, 0.0);
  if (index == 2) return vec3(0.0, 0.0, 1.0);
  if (index == 3) return vec3(0.0, 1.0, 1.0);
  if (index == 4) return vec3(1.0, 0.0, 1.0);
  return vec3(1.0, 1.0, 0.0);
}

vec3 blackColorWhiteRamp(float amount, vec3 primary) {
  if (amount < 0.5) {
    return mix(vec3(0.0), primary, amount * 2.0);
  }
  return mix(primary, vec3(1.0), (amount - 0.5) * 2.0);
}

float clippingLevel(int index) {
  if (index == 0) return 0.0;
  if (index == 1) return 1.0 / 255.0;
  if (index == 2) return 2.0 / 255.0;
  if (index == 3) return 4.0 / 255.0;
  if (index == 4) return 8.0 / 255.0;
  if (index == 5) return 16.0 / 255.0;
  if (index == 6) return 239.0 / 255.0;
  if (index == 7) return 247.0 / 255.0;
  if (index == 8) return 251.0 / 255.0;
  if (index == 9) return 253.0 / 255.0;
  if (index == 10) return 254.0 / 255.0;
  return 1.0;
}

void main() {
  vec2 uv = localPosition / targetSize + 0.5;
  float resolution = max(1.0, min(faceResolution.x, faceResolution.y));
  vec2 pixel = uv * resolution;
  float onePixel = 1.0 / resolution;
  float antialiasWidth = max(fwidth(uv.x), fwidth(uv.y));
  vec3 result = vec3(18.0 / 255.0);

  // A 24 x 24 grid, major quarter lines, and a two-pixel face boundary.
  float minorSpacing = resolution / gridDivisions;
  float minorDistance = min(
      periodicDistance(pixel.x, minorSpacing),
      periodicDistance(pixel.y, minorSpacing));
  float minorGrid = 1.0 - step(0.75, minorDistance);
  result = mix(result, vec3(58.0 / 255.0), minorGrid);

  float majorSpacing = resolution / 4.0;
  float majorDistance = min(
      periodicDistance(pixel.x, majorSpacing),
      periodicDistance(pixel.y, majorSpacing));
  float majorGrid = 1.0 - step(1.5, majorDistance);
  result = mix(result, accentColor * 0.72 + vec3(0.12), majorGrid);

  vec2 targetCenter = vec2(0.5, 0.43);
  float safeLarge = rectangleOutline(
      uv, targetCenter, vec2(0.34, 0.24), 1.5 * onePixel, antialiasWidth);
  float safeSmall = rectangleOutline(
      uv, targetCenter, vec2(0.26, 0.18), 1.5 * onePixel, antialiasWidth);
  result = mix(result, vec3(0.82), max(safeLarge, safeSmall));

  float rings = 0.0;
  rings = max(rings, circleOutline(uv, targetCenter, 0.22, onePixel, antialiasWidth));
  rings = max(rings, circleOutline(uv, targetCenter, 0.14, onePixel, antialiasWidth));
  rings = max(rings, circleOutline(uv, targetCenter, 0.06, onePixel, antialiasWidth));
  result = mix(result, vec3(0.9), rings);

  vec2 radial = uv - targetCenter;
  float radialLength = length(radial);
  float spokePhase = abs(sin(atan(radial.y, radial.x) * 18.0));
  float spokes = (1.0 - smoothstep(0.0, 0.035, spokePhase))
      * step(0.065, radialLength) * step(radialLength, 0.22);
  result = mix(result, vec3(0.58), spokes);

  float centerHorizontal = (1.0 - step(onePixel * 2.0, abs(uv.y - targetCenter.y)))
      * rectangleMask(uv, targetCenter - vec2(0.10, 0.22), targetCenter + vec2(0.10, 0.22));
  float centerVertical = (1.0 - step(onePixel * 2.0, abs(uv.x - targetCenter.x)))
      * rectangleMask(uv, targetCenter - vec2(0.22, 0.10), targetCenter + vec2(0.22, 0.10));
  result = mix(result, accentColor, max(centerHorizontal, centerVertical));

  // Vertical and horizontal line pairs at 1, 2, 4, and 8 pixel widths.
  vec2 verticalMinimum = vec2(0.045, 0.15);
  vec2 verticalMaximum = vec2(0.225, 0.36);
  float inVerticalFocus = rectangleMask(uv, verticalMinimum, verticalMaximum);
  if (inVerticalFocus > 0.5) {
    vec2 local = (uv - verticalMinimum) / (verticalMaximum - verticalMinimum);
    int band = min(3, int(floor(local.y * 4.0)));
    float stripeWidth = band == 0 ? 1.0 : band == 1 ? 2.0 : band == 2 ? 4.0 : 8.0;
    result = vec3(step(stripeWidth, mod(pixel.x, stripeWidth * 2.0)));
  }

  vec2 horizontalMinimum = vec2(0.245, 0.15);
  vec2 horizontalMaximum = vec2(0.425, 0.36);
  float inHorizontalFocus = rectangleMask(uv, horizontalMinimum, horizontalMaximum);
  if (inHorizontalFocus > 0.5) {
    vec2 local = (uv - horizontalMinimum) / (horizontalMaximum - horizontalMinimum);
    int band = min(3, int(floor(local.x * 4.0)));
    float stripeWidth = band == 0 ? 1.0 : band == 1 ? 2.0 : band == 2 ? 4.0 : 8.0;
    result = vec3(step(stripeWidth, mod(pixel.y, stripeWidth * 2.0)));
  }

  float focusBorders = max(
      rectangleOutline(
          uv, (verticalMinimum + verticalMaximum) * 0.5,
          (verticalMaximum - verticalMinimum) * 0.5, onePixel, antialiasWidth),
      rectangleOutline(
          uv, (horizontalMinimum + horizontalMaximum) * 0.5,
          (horizontalMaximum - horizontalMinimum) * 0.5, onePixel, antialiasWidth));
  result = mix(result, vec3(0.65), focusBorders);

  // Exact 1, 2, 3, and 4 pixel points plus optical starbursts.
  float points = 0.0;
  float bursts = 0.0;
  for (int index = 0; index < 4; index++) {
    vec2 pointCenter = vec2(0.72 + float(index) * 0.065, 0.19);
    vec2 burstCenter = vec2(0.72 + float(index) * 0.065, 0.275);
    points = max(points, squarePoint(uv, pointCenter, float(index + 1), resolution));
    bursts = max(bursts, starBurst(uv, burstCenter, float(index + 1), resolution));
  }
  result = mix(result, vec3(1.0), max(points, bursts));

  vec2 fieldMinimum = vec2(0.69, 0.32);
  vec2 fieldMaximum = vec2(0.96, 0.47);
  float inStarField = rectangleMask(uv, fieldMinimum, fieldMaximum);
  if (inStarField > 0.5) {
    vec2 gridSize = vec2(10.0, 5.0);
    vec2 fieldCoordinate = (uv - fieldMinimum) / (fieldMaximum - fieldMinimum) * gridSize;
    vec2 cell = floor(fieldCoordinate);
    vec2 starCenter = fieldMinimum
        + ((cell + 0.5) / gridSize) * (fieldMaximum - fieldMinimum);
    float pointSize = 1.0 + floor(hash21(cell + float(faceIndex)) * 4.0);
    float star = squarePoint(uv, starCenter, pointSize, resolution);
    result = mix(result, vec3(0.92), star);
  }

  // Six continuous black-primary-white ramps.
  vec2 rgbMinimum = vec2(0.05, 0.64);
  vec2 rgbMaximum = vec2(0.48, 0.745);
  if (rectangleMask(uv, rgbMinimum, rgbMaximum) > 0.5) {
    vec2 local = (uv - rgbMinimum) / (rgbMaximum - rgbMinimum);
    int band = min(2, int(floor(local.y * 3.0)));
    result = blackColorWhiteRamp(local.x, gradientColor(band));
  }

  vec2 cmyMinimum = vec2(0.52, 0.64);
  vec2 cmyMaximum = vec2(0.95, 0.745);
  if (rectangleMask(uv, cmyMinimum, cmyMaximum) > 0.5) {
    vec2 local = (uv - cmyMinimum) / (cmyMaximum - cmyMinimum);
    int band = min(2, int(floor(local.y * 3.0)));
    result = blackColorWhiteRamp(local.x, gradientColor(band + 3));
  }

  // Pure RGB/CMY/W/K bars.
  vec2 colorMinimum = vec2(0.08, 0.77);
  vec2 colorMaximum = vec2(0.92, 0.825);
  if (rectangleMask(uv, colorMinimum, colorMaximum) > 0.5) {
    float localX = (uv.x - colorMinimum.x) / (colorMaximum.x - colorMinimum.x);
    int index = min(7, int(floor(localX * 8.0)));
    result = colorBar(index);
  }

  // Continuous and discrete luminance references.
  vec2 grayMinimum = vec2(0.08, 0.85);
  vec2 grayMaximum = vec2(0.92, 0.88);
  if (rectangleMask(uv, grayMinimum, grayMaximum) > 0.5) {
    float level = (uv.x - grayMinimum.x) / (grayMaximum.x - grayMinimum.x);
    result = vec3(level);
  }

  vec2 stepsMinimum = vec2(0.08, 0.90);
  vec2 stepsMaximum = vec2(0.92, 0.945);
  if (rectangleMask(uv, stepsMinimum, stepsMaximum) > 0.5) {
    float localX = (uv.x - stepsMinimum.x) / (stepsMaximum.x - stepsMinimum.x);
    int index = min(8, int(floor(localX * 9.0)));
    result = vec3(float(index) / 8.0);
  }

  // Near-black and near-white patches expose clipping and crushed levels.
  vec2 clippingMinimum = vec2(0.08, 0.10);
  vec2 clippingMaximum = vec2(0.92, 0.13);
  if (rectangleMask(uv, clippingMinimum, clippingMaximum) > 0.5) {
    float localX = (uv.x - clippingMinimum.x) / (clippingMaximum.x - clippingMinimum.x);
    int index = min(11, int(floor(localX * 12.0)));
    result = vec3(clippingLevel(index));
  }

  float edgeDistance = min(min(uv.x, uv.y), min(1.0 - uv.x, 1.0 - uv.y));
  float outerBorder = 1.0 - step(2.0 * onePixel, edgeDistance);
  result = mix(result, accentColor, outerBorder);

  fragColor = vec4(result, 1.0);
}
