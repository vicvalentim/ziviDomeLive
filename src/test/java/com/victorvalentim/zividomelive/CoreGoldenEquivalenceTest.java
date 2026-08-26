package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.core.action.ActionMap;
import com.victorvalentim.zividomelive.core.camera.CameraPose;
import com.victorvalentim.zividomelive.core.math.Vec3;
import com.victorvalentim.zividomelive.core.ports.InputPort;
import com.victorvalentim.zividomelive.core.ports.Ports;
import com.victorvalentim.zividomelive.core.projection.DomemasterSettings;
import com.victorvalentim.zividomelive.core.projection.ProjectionType;
import com.victorvalentim.zividomelive.core.task.FrameThreadQueue;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Deterministic side-by-side fixtures for the frozen Processing 2.0 and Core implementations. */
class CoreGoldenEquivalenceTest {

    private static final float FLOAT_EPSILON = 1.0e-5f;
    private static final double DOUBLE_EPSILON = 1.0e-12;

    @Test
    void frameClockStateAndExceptionsMatchGoldenImplementation() {
        AtomicLong nanos = new AtomicLong(1_000_000_000L);
        FrameClock golden = new FrameClock(nanos::get);
        com.victorvalentim.zividomelive.core.time.FrameClock core =
                new com.victorvalentim.zividomelive.core.time.FrameClock(nanos::get);
        golden.setMaxDeltaSeconds(0.1);
        core.setMaxDeltaSeconds(0.1);

        for (long increment : new long[]{0L, 50_000_000L, 500_000_000L, -700_000_000L, 20_000_000L}) {
            nanos.addAndGet(increment);
            assertEquals(golden.tick(), core.tick(), DOUBLE_EPSILON);
            assertEquals(golden.getDeltaSeconds(), core.getDeltaSeconds(), DOUBLE_EPSILON);
            assertEquals(golden.getElapsedSeconds(), core.getElapsedSeconds(), DOUBLE_EPSILON);
            assertEquals(golden.getFrameIndex(), core.getFrameIndex());
        }

        golden.reset();
        core.reset();
        assertEquals(golden.tick(), core.tick(), DOUBLE_EPSILON);
        assertEquals(golden.getFrameIndex(), core.getFrameIndex());
        assertEquals(
                assertThrows(IllegalArgumentException.class,
                        () -> golden.setMaxDeltaSeconds(Double.NaN)).getMessage(),
                assertThrows(IllegalArgumentException.class,
                        () -> core.setMaxDeltaSeconds(Double.NaN)).getMessage());
    }

    @Test
    void simulationTimelineTransitionsAndOrderingMatchGoldenImplementation() {
        SimulationTimeline golden = new SimulationTimeline();
        com.victorvalentim.zividomelive.core.time.SimulationTimeline core =
                new com.victorvalentim.zividomelive.core.time.SimulationTimeline();
        golden.setFixedStep(0.1);
        core.setFixedStep(0.1);
        golden.setRate(2.0);
        core.setRate(2.0);
        golden.setMaxSubSteps(3);
        core.setMaxSubSteps(3);
        List<Double> goldenSteps = new ArrayList<>();
        List<Double> coreSteps = new ArrayList<>();

        for (double delta : new double[]{0.0, 0.26, 1.05, 0.04}) {
            assertEquals(golden.advance(delta, goldenSteps::add),
                    core.advance(delta, coreSteps::add));
            assertTimelineEquals(golden, core);
        }
        assertEquals(goldenSteps, coreSteps);
        golden.pause();
        core.pause();
        assertEquals(golden.advance(1.0, ignored -> { }), core.advance(1.0, ignored -> { }));
        golden.resume();
        core.resume();
        golden.setPosition(42.0);
        core.setPosition(42.0);
        golden.jump(-2.5);
        core.jump(-2.5);
        assertTimelineEquals(golden, core);
    }

    @Test
    void quaternionMathMatchesGoldenFloatSemantics() {
        com.victorvalentim.zividomelive.render.Quaternion golden =
                com.victorvalentim.zividomelive.render.Quaternion.fromAxisAngle(
                        1.0f, 2.0f, 3.0f, 0.75f);
        com.victorvalentim.zividomelive.core.math.Quaternion core =
                com.victorvalentim.zividomelive.core.math.Quaternion.fromAxisAngle(
                        1.0f, 2.0f, 3.0f, 0.75f);
        assertQuaternionEquals(golden, core);

        float[][] rotations = {
                {0.0f, 1.0f, 0.0f, 0.25f},
                {1.0f, 0.0f, 0.0f, -0.4f},
                {0.0f, 0.0f, 1.0f, 1.1f}
        };
        for (float[] rotation : rotations) {
            golden = golden.multiply(
                    com.victorvalentim.zividomelive.render.Quaternion.fromAxisAngle(
                            rotation[0], rotation[1], rotation[2], rotation[3])).normalized();
            core = core.multiply(
                    com.victorvalentim.zividomelive.core.math.Quaternion.fromAxisAngle(
                            rotation[0], rotation[1], rotation[2], rotation[3])).normalized();
            assertQuaternionEquals(golden, core);
        }

        com.victorvalentim.zividomelive.render.Quaternion goldenEnd =
                com.victorvalentim.zividomelive.render.Quaternion.fromAxisAngle(
                        0.0f, 1.0f, 0.0f, (float) Math.PI);
        com.victorvalentim.zividomelive.core.math.Quaternion coreEnd =
                com.victorvalentim.zividomelive.core.math.Quaternion.fromAxisAngle(
                        0.0f, 1.0f, 0.0f, (float) Math.PI);
        for (float factor : new float[]{-1.0f, 0.0f, 0.1f, 0.5f, 1.0f, 2.0f}) {
            assertQuaternionEquals(golden.slerp(goldenEnd, factor), core.slerp(coreEnd, factor));
        }
    }

    @Test
    void sphericalOrientationLongSequenceMatchesGoldenEventComposition() {
        com.victorvalentim.zividomelive.render.SphericalOrientation golden =
                new com.victorvalentim.zividomelive.render.SphericalOrientation();
        com.victorvalentim.zividomelive.core.projection.SphericalOrientation core =
                new com.victorvalentim.zividomelive.core.projection.SphericalOrientation();

        for (int index = 1; index <= 2_000; index++) {
            float pitch = index * 0.013f;
            float yaw = index * -0.017f;
            float roll = index * 0.019f;
            golden.setPitch(pitch);
            core.setPitch(pitch);
            golden.setYaw(yaw);
            core.setYaw(yaw);
            golden.setRoll(roll);
            core.setRoll(roll);
            assertEquals(golden.getPitch(), core.getPitch());
            assertEquals(golden.getYaw(), core.getYaw());
            assertEquals(golden.getRoll(), core.getRoll());
            assertQuaternionEquals(golden.getQuaternion(), core.getQuaternion());
        }

        golden.setPitch(Float.NaN);
        core.setPitch(Float.NaN);
        assertQuaternionEquals(golden.getQuaternion(), core.getQuaternion());
        golden.reset();
        core.reset();
        assertQuaternionEquals(golden.getQuaternion(), core.getQuaternion());
    }

    @Test
    void orbitCameraMathMatchesGoldenAcrossSmoothAndImmediateOperations() {
        com.victorvalentim.zividomelive.render.camera.OrbitCamera golden =
                new com.victorvalentim.zividomelive.render.camera.OrbitCamera(-300.0f);
        com.victorvalentim.zividomelive.core.camera.OrbitCamera core =
                new com.victorvalentim.zividomelive.core.camera.OrbitCamera(-300.0f);
        golden.setDistanceLimits(-1000.0f, 1000.0f);
        core.setDistanceLimits(-1000.0f, 1000.0f);
        golden.setCollapseGuard(5.0f);
        core.setCollapseGuard(5.0f);
        golden.setLerpFactor(0.15f);
        core.setLerpFactor(0.15f);

        com.victorvalentim.zividomelive.render.Quaternion goldenOrientation =
                com.victorvalentim.zividomelive.render.Quaternion.fromAxisAngle(
                        1.0f, 0.0f, 0.0f, 0.5f);
        com.victorvalentim.zividomelive.core.math.Quaternion coreOrientation =
                com.victorvalentim.zividomelive.core.math.Quaternion.fromAxisAngle(
                        1.0f, 0.0f, 0.0f, 0.5f);
        golden.snapTo(1.0f, 2.0f, 3.0f, goldenOrientation, -300.0f);
        core.snapTo(1.0f, 2.0f, 3.0f, coreOrientation, -300.0f);
        assertCameraEquals(golden, core);

        golden.rotateAround(0.0f, 1.0f, 0.0f, 0.25f);
        core.rotateAround(0.0f, 1.0f, 0.0f, 0.25f);
        golden.zoom(100.0f);
        core.zoom(100.0f);
        golden.setTarget(10.0f, 20.0f, 30.0f);
        core.setTarget(10.0f, 20.0f, 30.0f);
        for (int index = 0; index < 80; index++) {
            golden.update();
            core.update();
            assertCameraEquals(golden, core);
        }

        golden.rotateAroundImmediate(0.0f, 0.0f, 1.0f, -0.1f);
        core.rotateAroundImmediate(0.0f, 0.0f, 1.0f, -0.1f);
        golden.zoomImmediate(-25.0f);
        core.zoomImmediate(-25.0f);
        golden.setTargetImmediate(new PVector(-4.0f, 5.0f, -6.0f));
        core.setTargetImmediate(new Vec3(-4.0f, 5.0f, -6.0f));
        assertCameraEquals(golden, core);
    }

    @Test
    void namedActionReplacementTriggerAndCloseMatchGoldenSemantics() {
        SceneActionMap golden = new SceneActionMap();
        ActionMap core = new ActionMap();
        AtomicInteger goldenValue = new AtomicInteger();
        AtomicInteger coreValue = new AtomicInteger();
        golden.register("mode", () -> goldenValue.set(1));
        core.register("mode", () -> coreValue.set(1));
        golden.register("mode", () -> goldenValue.set(2));
        core.register("mode", () -> coreValue.set(2));

        assertEquals(golden.size(), core.size());
        assertEquals(golden.trigger("mode"), core.trigger("mode"));
        assertEquals(goldenValue.get(), coreValue.get());
        assertEquals(golden.trigger("missing"), core.trigger("missing"));
        golden.unregister("mode");
        core.unregister("mode");
        assertEquals(golden.size(), core.size());
        golden.close();
        core.close();
        assertThrows(IllegalStateException.class, () -> golden.trigger("mode"));
        assertThrows(IllegalStateException.class, () -> core.trigger("mode"));
    }

    @Test
    void frameQueueFiniteSnapshotAndRejectionMatchGoldenSemantics() {
        RenderThreadQueue golden = new RenderThreadQueue();
        FrameThreadQueue core = new FrameThreadQueue();
        List<Integer> goldenOrder = new ArrayList<>();
        List<Integer> coreOrder = new ArrayList<>();
        golden.enqueue(() -> {
            goldenOrder.add(1);
            golden.enqueue(() -> goldenOrder.add(2));
        });
        core.enqueue(() -> {
            coreOrder.add(1);
            core.enqueue(() -> coreOrder.add(2));
        });

        assertEquals(golden.drain(), core.drain());
        assertEquals(goldenOrder, coreOrder);
        assertEquals(golden.getPendingCount(), core.getPendingCount());
        assertEquals(golden.drain(), core.drain());
        assertEquals(goldenOrder, coreOrder);
        golden.close();
        core.close();
        assertThrows(IllegalStateException.class, () -> golden.enqueue(() -> { }));
        assertThrows(IllegalStateException.class, () -> core.enqueue(() -> { }));
    }

    @Test
    void boundedPortsDropAndFrameDispatchMatchGoldenSemantics() {
        ScenePorts golden = new ScenePorts(new RenderThreadQueue(), 2, 1);
        Ports core = new Ports(new FrameThreadQueue(), 2, 1);
        GoldenInput<Integer> goldenInput = new GoldenInput<>();
        CoreInput<Integer> coreInput = new CoreInput<>();
        List<Integer> goldenValues = new ArrayList<>();
        List<Integer> coreValues = new ArrayList<>();
        golden.connectInput(goldenInput, goldenValues::add);
        core.connectInput(coreInput, coreValues::add);
        for (int value : new int[]{1, 2, 3}) {
            goldenInput.emit(value);
            coreInput.emit(value);
        }

        assertEquals(golden.getDroppedInputCount(), core.getDroppedInputCount());
        assertEquals(golden.getPendingInputCount(), core.getPendingInputCount());
        assertEquals(golden.drain(), core.drain());
        assertEquals(goldenValues, coreValues);
        assertEquals(golden.getPendingInputCount(), core.getPendingInputCount());
        golden.pause();
        core.pause();
        goldenInput.emit(4);
        coreInput.emit(4);
        assertEquals(golden.getPendingInputCount(), core.getPendingInputCount());
        golden.resume();
        core.resume();
        golden.close();
        core.close();
    }

    @Test
    void projectionMappingAndDomemasterCalibrationMatchGoldenFacadeState() {
        for (ViewType view : ViewType.values()) {
            assertEquals(view.name(), mapProjection(view).name());
        }

        ziviDomeLive golden = new ziviDomeLive(new PApplet());
        DomemasterSettings core = new DomemasterSettings();
        for (float value : new float[]{-1.0f, 42.5f, 500.0f, Float.NaN}) {
            golden.setFishSize(value);
            core.setSizePercent(value);
            assertEquals(golden.getFishSize(), core.getSizePercent());
        }
        for (float value : new float[]{-1.0f, 225.0f, 500.0f, Float.POSITIVE_INFINITY}) {
            golden.setFov(value);
            core.setFieldOfViewDegrees(value);
            assertEquals(golden.getFov(), core.getFieldOfViewDegrees());
        }
        golden.dispose();
    }

    private static ProjectionType mapProjection(ViewType view) {
        return switch (view) {
            case STANDARD -> ProjectionType.STANDARD;
            case DOMEMASTER -> ProjectionType.DOMEMASTER;
            case EQUIRECTANGULAR -> ProjectionType.EQUIRECTANGULAR;
            case SKYBOX -> ProjectionType.SKYBOX;
        };
    }

    private static void assertTimelineEquals(
            SimulationTimeline golden,
            com.victorvalentim.zividomelive.core.time.SimulationTimeline core) {
        assertEquals(golden.getPosition(), core.getPosition(), DOUBLE_EPSILON);
        assertEquals(golden.getRate(), core.getRate(), DOUBLE_EPSILON);
        assertEquals(golden.getFixedStep(), core.getFixedStep(), DOUBLE_EPSILON);
        assertEquals(golden.getMaxSubSteps(), core.getMaxSubSteps());
        assertEquals(golden.getAccumulator(), core.getAccumulator(), DOUBLE_EPSILON);
        assertEquals(golden.getDroppedUnits(), core.getDroppedUnits(), DOUBLE_EPSILON);
        assertEquals(golden.isPaused(), core.isPaused());
    }

    private static void assertCameraEquals(
            com.victorvalentim.zividomelive.render.camera.OrbitCamera golden,
            com.victorvalentim.zividomelive.core.camera.OrbitCamera core) {
        PVector goldenTarget = golden.getTarget();
        CameraPose corePose = core.getPose();
        assertEquals(goldenTarget.x, corePose.target().x(), FLOAT_EPSILON);
        assertEquals(goldenTarget.y, corePose.target().y(), FLOAT_EPSILON);
        assertEquals(goldenTarget.z, corePose.target().z(), FLOAT_EPSILON);
        assertEquals(golden.getDistance(), corePose.distance(), FLOAT_EPSILON);
        assertQuaternionEquals(golden.getOrientation(), corePose.orientation());
    }

    private static void assertQuaternionEquals(
            com.victorvalentim.zividomelive.render.Quaternion golden,
            com.victorvalentim.zividomelive.core.math.Quaternion core) {
        assertEquals(golden.x(), core.x(), FLOAT_EPSILON);
        assertEquals(golden.y(), core.y(), FLOAT_EPSILON);
        assertEquals(golden.z(), core.z(), FLOAT_EPSILON);
        assertEquals(golden.w(), core.w(), FLOAT_EPSILON);
    }

    private static final class GoldenInput<T> implements SceneInputPort<T> {
        private Consumer<? super T> receiver;

        @Override
        public void start(Consumer<? super T> receiver) {
            this.receiver = receiver;
        }

        private void emit(T value) {
            receiver.accept(value);
        }

        @Override
        public void close() {
        }
    }

    private static final class CoreInput<T> implements InputPort<T> {
        private Consumer<? super T> receiver;

        @Override
        public void start(Consumer<? super T> receiver) {
            this.receiver = receiver;
        }

        private void emit(T value) {
            receiver.accept(value);
        }

        @Override
        public void close() {
        }
    }
}
