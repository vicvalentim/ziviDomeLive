// Canvas port of the Processing SplashScreen geometry and motion contract.
(function () {
    "use strict";

    const BASE_WIDTH = 566;
    const BASE_HEIGHT = 358;
    const CENTER_X = BASE_WIDTH / 2;
    const CENTER_Y = BASE_HEIGHT / 2;
    const CAMERA_Z = CENTER_Y / Math.tan(Math.PI / 6);
    const SPHERE_RADIUS = 120;
    const ORBIT_RADIUS = 160;
    const RINGS = 16;
    const SEGMENTS = 32;
    const CUBE_COUNT = 13;
    const CUBE_HALF_SIZE = 4;
    const MAX_PIXEL_RATIO = 2;
    const STATIC_FRAME_TIME = 1800;
    const DEG_TO_RAD = Math.PI / 180;

    const cubeVertices = new Float32Array([
        -1, -1, -1,  1, -1, -1,  1,  1, -1, -1,  1, -1,
        -1, -1,  1,  1, -1,  1,  1,  1,  1, -1,  1,  1
    ]);
    const cubeEdges = new Uint8Array([
        0, 1, 1, 2, 2, 3, 3, 0,
        4, 5, 5, 6, 6, 7, 7, 4,
        0, 4, 1, 5, 2, 6, 3, 7
    ]);
    const sphereRings = createSphereRings();
    const controllers = new Set();

    function createSphereRings() {
        const rings = [];
        for (let ring = 0; ring < RINGS; ring += 1) {
            const theta = -Math.PI / 2 + Math.PI * ring / (RINGS - 1);
            const ringRadius = Math.cos(theta) * SPHERE_RADIUS;
            const y = Math.sin(theta) * SPHERE_RADIUS;
            const points = new Float32Array((SEGMENTS + 1) * 3);

            for (let segment = 0; segment <= SEGMENTS; segment += 1) {
                const phi = Math.PI * 2 * segment / SEGMENTS;
                const offset = segment * 3;
                points[offset] = Math.cos(phi) * ringRadius;
                points[offset + 1] = y;
                points[offset + 2] = Math.sin(phi) * ringRadius;
            }
            rings.push(points);
        }
        return rings;
    }

    function projectSpherePoint(x, y, z, rotation, target, offset) {
        const zRotatedX = x * rotation.cosZ - y * rotation.sinZ;
        const zRotatedY = x * rotation.sinZ + y * rotation.cosZ;
        const yRotatedX = zRotatedX * rotation.cosY + z * rotation.sinY;
        const yRotatedZ = -zRotatedX * rotation.sinY + z * rotation.cosY;
        const xRotatedY = zRotatedY * rotation.cosX - yRotatedZ * rotation.sinX;
        const xRotatedZ = zRotatedY * rotation.sinX + yRotatedZ * rotation.cosX;
        const perspective = CAMERA_Z / (CAMERA_Z - xRotatedZ);

        target[offset] = CENTER_X + yRotatedX * perspective;
        target[offset + 1] = CENTER_Y + xRotatedY * perspective;
    }

    function projectOrbitPoint(x, y, z, cosZ, sinZ, target, offset) {
        const rotatedX = x * cosZ - y * sinZ;
        const rotatedY = x * sinZ + y * cosZ;
        const perspective = CAMERA_Z / (CAMERA_Z - z);

        target[offset] = CENTER_X + rotatedX * perspective;
        target[offset + 1] = CENTER_Y + rotatedY * perspective;
    }

    function createSplashController(stage) {
        const canvas = stage.querySelector("[data-zd-splash-canvas]");
        const context = canvas && canvas.getContext("2d", { alpha: true });
        if (!canvas || !context) {
            return null;
        }

        const speeds = new Float32Array(CUBE_COUNT);
        for (let cube = 0; cube < CUBE_COUNT; cube += 1) {
            speeds[cube] = 0.0008 + (Math.random() - 0.5) * 0.0004;
        }

        const ringProjection = new Float32Array((SEGMENTS + 1) * 2);
        const cubeProjection = new Float32Array(8 * 2);
        const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
        const animationStart = performance.now();
        let palette = readPalette();
        let resizeObserver = null;
        let intersectionObserver = null;
        let paletteObserver = null;
        let animationFrame = 0;
        let isVisible = true;
        let isDestroyed = false;
        let hasRendered = false;
        let canvasMetrics = null;
        let canvasNeedsResize = true;

        function resizeCanvas() {
            if (canvasMetrics && !canvasNeedsResize) {
                return canvasMetrics;
            }
            const bounds = stage.getBoundingClientRect();
            const pixelRatio = Math.min(window.devicePixelRatio || 1, MAX_PIXEL_RATIO);
            const width = Math.max(1, Math.round(bounds.width * pixelRatio));
            const height = Math.max(1, Math.round(bounds.height * pixelRatio));

            if (canvas.width !== width || canvas.height !== height) {
                canvas.width = width;
                canvas.height = height;
            }
            canvasMetrics = { width: bounds.width, height: bounds.height, pixelRatio };
            canvasNeedsResize = false;
            return canvasMetrics;
        }

        function prepareContext() {
            const size = resizeCanvas();
            const scale = Math.min(size.width / BASE_WIDTH, size.height / BASE_HEIGHT);
            const offsetX = (size.width - BASE_WIDTH * scale) / 2;
            const offsetY = (size.height - BASE_HEIGHT * scale) / 2;

            context.setTransform(1, 0, 0, 1, 0, 0);
            context.clearRect(0, 0, canvas.width, canvas.height);
            context.setTransform(
                size.pixelRatio * scale,
                0,
                0,
                size.pixelRatio * scale,
                size.pixelRatio * offsetX,
                size.pixelRatio * offsetY
            );
            context.lineCap = "butt";
            context.lineJoin = "miter";
        }

        function parseHexColor(value, fallback) {
            const match = value.trim().match(/^#([0-9a-f]{6})$/i);
            if (!match) {
                return fallback;
            }
            const color = Number.parseInt(match[1], 16);
            return {
                red: color >> 16,
                green: color >> 8 & 0xff,
                blue: color & 0xff
            };
        }

        function readPalette() {
            const styles = window.getComputedStyle(stage);
            return {
                ringStart: parseHexColor(
                    styles.getPropertyValue("--zd-splash-ring-start"),
                    { red: 0, green: 120, blue: 39 }
                ),
                ringEnd: parseHexColor(
                    styles.getPropertyValue("--zd-splash-ring-end"),
                    { red: 5, green: 157, blue: 44 }
                ),
                cube: parseHexColor(
                    styles.getPropertyValue("--zd-splash-cube"),
                    { red: 0, green: 120, blue: 68 }
                )
            };
        }

        function drawSphere(time) {
            const rotationX = time * 0.02 * DEG_TO_RAD;
            const rotationY = time * 0.015 * DEG_TO_RAD;
            const rotationZ = time * 0.01 * DEG_TO_RAD;
            const rotation = {
                sinX: Math.sin(rotationX), cosX: Math.cos(rotationX),
                sinY: Math.sin(rotationY), cosY: Math.cos(rotationY),
                sinZ: Math.sin(rotationZ), cosZ: Math.cos(rotationZ)
            };

            context.lineWidth = 2;
            for (let ring = 0; ring < sphereRings.length; ring += 1) {
                const points = sphereRings[ring];
                const colorMix = Math.abs(Math.sin(time * 0.0005 + ring));
                const red = Math.round(palette.ringStart.red
                    + (palette.ringEnd.red - palette.ringStart.red) * colorMix);
                const green = Math.round(palette.ringStart.green
                    + (palette.ringEnd.green - palette.ringStart.green) * colorMix);
                const blue = Math.round(palette.ringStart.blue
                    + (palette.ringEnd.blue - palette.ringStart.blue) * colorMix);
                const alpha = (50 + 100 * colorMix) / 255;

                for (let point = 0; point <= SEGMENTS; point += 1) {
                    const source = point * 3;
                    projectSpherePoint(
                        points[source], points[source + 1], points[source + 2],
                        rotation, ringProjection, point * 2
                    );
                }

                context.beginPath();
                context.moveTo(ringProjection[0], ringProjection[1]);
                for (let point = 1; point <= SEGMENTS; point += 1) {
                    context.lineTo(ringProjection[point * 2], ringProjection[point * 2 + 1]);
                }
                context.strokeStyle = `rgba(${red}, ${green}, ${blue}, ${alpha})`;
                context.stroke();
            }
        }

        function drawOrbitingCubes(time) {
            context.lineWidth = 1.5;
            context.strokeStyle = `rgb(${palette.cube.red}, ${palette.cube.green}, ${palette.cube.blue})`;

            for (let cube = 0; cube < CUBE_COUNT; cube += 1) {
                const rotationOffset = Math.PI * 2 * cube / CUBE_COUNT;
                const initialAngleOffset = rotationOffset;
                const zOffset = (cube % 2 === 0 ? 1 : -1) * ORBIT_RADIUS * 0.2;
                const angle = time * speeds[cube] + initialAngleOffset;
                const orbitY = ORBIT_RADIUS * Math.cos(angle);
                const orbitZ = ORBIT_RADIUS * Math.sin(angle) + zOffset;
                const cosZ = Math.cos(rotationOffset);
                const sinZ = Math.sin(rotationOffset);

                for (let vertex = 0; vertex < 8; vertex += 1) {
                    const source = vertex * 3;
                    projectOrbitPoint(
                        cubeVertices[source] * CUBE_HALF_SIZE,
                        orbitY + cubeVertices[source + 1] * CUBE_HALF_SIZE,
                        orbitZ + cubeVertices[source + 2] * CUBE_HALF_SIZE,
                        cosZ,
                        sinZ,
                        cubeProjection,
                        vertex * 2
                    );
                }

                context.beginPath();
                for (let edge = 0; edge < cubeEdges.length; edge += 2) {
                    const start = cubeEdges[edge] * 2;
                    const end = cubeEdges[edge + 1] * 2;
                    context.moveTo(cubeProjection[start], cubeProjection[start + 1]);
                    context.lineTo(cubeProjection[end], cubeProjection[end + 1]);
                }
                context.stroke();
            }
        }

        function draw(time) {
            prepareContext();
            drawSphere(time);
            drawOrbitingCubes(time);

            if (!hasRendered) {
                hasRendered = true;
                stage.dataset.zdSplashReady = "true";
            }
        }

        function shouldAnimate() {
            return !isDestroyed
                && isVisible
                && document.visibilityState === "visible"
                && !reducedMotion.matches;
        }

        function renderFrame(time) {
            animationFrame = 0;
            if (!canvas.isConnected) {
                destroy();
                return;
            }
            draw(Math.max(0, time - animationStart));
            if (shouldAnimate()) {
                animationFrame = window.requestAnimationFrame(renderFrame);
            }
        }

        function updateAnimation() {
            if (shouldAnimate()) {
                if (!animationFrame) {
                    animationFrame = window.requestAnimationFrame(renderFrame);
                }
            } else if (animationFrame) {
                window.cancelAnimationFrame(animationFrame);
                animationFrame = 0;
            }
        }

        function handleVisibility() {
            updateAnimation();
        }

        function handleMotionPreference() {
            if (reducedMotion.matches) {
                draw(STATIC_FRAME_TIME);
            }
            updateAnimation();
        }

        function handleResize() {
            canvasNeedsResize = true;
            if (!shouldAnimate()) {
                draw(reducedMotion.matches
                    ? STATIC_FRAME_TIME
                    : Math.max(0, performance.now() - animationStart));
            }
        }

        function handlePaletteChange() {
            palette = readPalette();
            if (!shouldAnimate()) {
                draw(reducedMotion.matches
                    ? STATIC_FRAME_TIME
                    : Math.max(0, performance.now() - animationStart));
            }
        }

        function destroy() {
            if (isDestroyed) {
                return;
            }
            isDestroyed = true;
            if (animationFrame) {
                window.cancelAnimationFrame(animationFrame);
                animationFrame = 0;
            }
            resizeObserver?.disconnect();
            intersectionObserver?.disconnect();
            paletteObserver?.disconnect();
            reducedMotion.removeEventListener?.("change", handleMotionPreference);
            document.removeEventListener("visibilitychange", handleVisibility);
            window.removeEventListener("resize", handleResize);
            controllers.delete(controller);
        }

        const controller = { canvas, destroy };
        document.addEventListener("visibilitychange", handleVisibility);
        reducedMotion.addEventListener?.("change", handleMotionPreference);
        window.addEventListener("resize", handleResize, { passive: true });

        if ("ResizeObserver" in window) {
            resizeObserver = new ResizeObserver(handleResize);
            resizeObserver.observe(stage);
        }

        if ("IntersectionObserver" in window) {
            intersectionObserver = new IntersectionObserver((entries) => {
                isVisible = entries[0]?.isIntersecting ?? true;
                updateAnimation();
            }, { rootMargin: "120px" });
            intersectionObserver.observe(stage);
        }

        if ("MutationObserver" in window) {
            paletteObserver = new MutationObserver(handlePaletteChange);
            paletteObserver.observe(document.body, {
                attributes: true,
                attributeFilter: ["data-md-color-scheme"]
            });
        }

        draw(reducedMotion.matches ? STATIC_FRAME_TIME : 0);
        updateAnimation();
        return controller;
    }

    function initializeSplashAnimations() {
        for (const controller of controllers) {
            if (!controller.canvas.isConnected) {
                controller.destroy();
            }
        }

        document.querySelectorAll("[data-zd-splash]:not([data-zd-splash-initialized])")
            .forEach((stage) => {
                stage.dataset.zdSplashInitialized = "true";
                const controller = createSplashController(stage);
                if (controller) {
                    controllers.add(controller);
                }
            });
    }

    function initializeDocumentation() {
        document.documentElement.classList.add("docs-ready");
        initializeSplashAnimations();
    }

    if (typeof document$ !== "undefined") {
        document$.subscribe(initializeDocumentation);
    } else if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initializeDocumentation, { once: true });
    } else {
        initializeDocumentation();
    }
}());
