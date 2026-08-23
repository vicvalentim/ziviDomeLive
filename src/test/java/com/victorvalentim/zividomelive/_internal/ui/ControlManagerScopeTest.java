package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/ui.

import com.victorvalentim.zividomelive.manager.OutputManager;
import com.victorvalentim.zividomelive.RenderMode;
import com.victorvalentim.zividomelive.ViewType;
import com.victorvalentim.zividomelive.ziviDomeLive;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PApplet;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlManagerScopeTest {
	@Test
	void internalControlsNeverRegisterAsProcessingKeyCallbacks() throws Exception {
		CallbackTrackingApplet applet = new CallbackTrackingApplet();
		PGraphicsJava2D graphics = new PGraphicsJava2D();
		graphics.setParent(applet);
		graphics.setSize(640, 640);
		applet.g = graphics;
		ziviDomeLive dome = new ziviDomeLive(applet);
		setOutputManager(dome, createOutputManager(dome));
		ControlManager controls = new ControlManager(applet, dome, 1024);
		try {
			assertTrue(applet.keyEventTargets.stream()
					.allMatch(target -> Modifier.isPublic(target.getClass().getModifiers())),
					"Processing reflection callbacks must be declared by public target types");
			assertFalse(applet.keyEventTargets.stream()
					.anyMatch(target -> target instanceof ControlManager
							|| target.getClass().getEnclosingClass() == ControlManager.class));
		} finally {
			controls.dispose();
			dome.dispose();
		}
	}

	@Test
	void scopedBuildersPreserveControlP5LayoutAndInitialSelections() throws Exception {
		PApplet applet = new PApplet();
		PGraphicsJava2D graphics = new PGraphicsJava2D();
		graphics.setParent(applet);
		graphics.setSize(640, 640);
		applet.g = graphics;
		ziviDomeLive dome = new ziviDomeLive(applet);
		OutputManager outputs = createOutputManager(dome);
		outputs.setNdiView(ViewType.EQUIRECTANGULAR);
		outputs.setSpoutView(ViewType.STANDARD);
		outputs.setSyphonView(ViewType.SKYBOX);
		setOutputManager(dome, outputs);
		dome.setCurrentView(ViewType.SKYBOX);

		ControlManager controls = new ControlManager(applet, dome, 3072);
		try {
			Object cp5 = readControlP5(controls);
			List<String> alwaysPresent = List.of(
					"fpsLabel",
					"pitchValue", "pitch",
					"yawValue", "yaw",
					"rollValue", "roll",
					"fovValue", "fov",
					"sizeValue", "size",
					"resetControls", "previewToggle", "View Mode",
					"Output Resolution", "ndiToggle", "NDI View");

			for (String controlName : alwaysPresent) {
				Object controller = getController(cp5, controlName);
				assertNotNull(controller, controlName);
				assertEquals(ControlPanelLayout.yFor(controlName), getPosition(controller)[1], controlName);
			}

			assertEquals(ViewType.SKYBOX.ordinal(),
					getValue(getController(cp5, "View Mode")));
			assertEquals(2, getValue(getController(cp5, "Output Resolution")));
			assertEquals(ViewType.EQUIRECTANGULAR.ordinal(),
					getValue(getController(cp5, "NDI View")));
			assertLocalOutputControls(cp5);
		} finally {
			controls.dispose();
			dome.dispose();
		}
	}

	@Test
	void panelVisibilityTracksRenderModeCapabilities() throws Exception {
		PApplet applet = configuredApplet();
		ziviDomeLive dome = new ziviDomeLive(applet);
		setOutputManager(dome, createOutputManager(dome));
		ControlManager controls = new ControlManager(applet, dome, 1024);
		try {
			Object cp5 = readControlP5(controls);

			dome.setRenderMode(RenderMode.EQUIRECTANGULAR);
			controls.show();
			assertTrue(isVisible(getController(cp5, "pitch")));
			assertFalse(isVisible(getController(cp5, "fov")));
			assertFalse(isVisible(getController(cp5, "sizeValue")));
			assertTrue(isVisible(getController(cp5, "resetControls")));
			assertFalse(isVisible(getController(cp5, "previewToggle")));
			assertFalse(isVisible(getController(cp5, "View Mode")));

			dome.setRenderMode(RenderMode.DOMEMASTER);
			controls.show();
			assertTrue(isVisible(getController(cp5, "pitchValue")));
			assertTrue(isVisible(getController(cp5, "fov")));
			assertTrue(isVisible(getController(cp5, "size")));
			assertFalse(isVisible(getController(cp5, "View Mode")));

			dome.setRenderMode(RenderMode.STANDARD);
			dome.setShowPreview(false);
			controls.show();
			assertFalse(isVisible(getController(cp5, "pitch")));
			assertFalse(isVisible(getController(cp5, "fov")));
			assertFalse(isVisible(getController(cp5, "resetControls")));
			assertTrue(isVisible(getController(cp5, "previewToggle")));

			dome.setShowPreview(true);
			controls.show();
			assertTrue(isVisible(getController(cp5, "pitch")));
			assertTrue(isVisible(getController(cp5, "fov")));
			assertTrue(isVisible(getController(cp5, "resetControls")));

			dome.setRenderMode(RenderMode.FULL);
			controls.show();
			assertTrue(isVisible(getController(cp5, "pitch")));
			assertTrue(isVisible(getController(cp5, "fov")));
			assertTrue(isVisible(getController(cp5, "previewToggle")));
			assertTrue(isVisible(getController(cp5, "View Mode")));
		} finally {
			controls.dispose();
			dome.dispose();
		}
	}

	@Test
	void pitchYawAndRollSlidersScrollCyclically() throws Exception {
		PApplet applet = configuredApplet();
		ziviDomeLive dome = new ziviDomeLive(applet);
		setOutputManager(dome, createOutputManager(dome));
		ControlManager controls = new ControlManager(applet, dome, 1024);
		try {
			Object cp5 = readControlP5(controls);
			Class<?> sliderClass = Class.forName("controlP5.Slider");
			int flexibleMode = sliderClass.getField("FLEXIBLE").getInt(null);
			for (String controlName : List.of("pitch", "yaw", "roll")) {
				Object slider = getController(cp5, controlName);
				assertEquals(flexibleMode, sliderMode(sliderClass, slider), controlName);

				setSliderValue(sliderClass, slider, (float) Math.PI - 0.01f);
				scrollSlider(sliderClass, slider, -1);
				assertTrue(getValue(slider) < 0.0f, controlName);
				assertEquals(getValue(slider), parentAngle(dome, controlName), 1.0e-5f);

				setSliderValue(sliderClass, slider, (float) -Math.PI + 0.01f);
				scrollSlider(sliderClass, slider, 1);
				assertTrue(getValue(slider) > 0.0f, controlName);
				assertEquals(getValue(slider), parentAngle(dome, controlName), 1.0e-5f);
			}
			assertEquals(
					sliderClass.getField("FIX").getInt(null),
					sliderMode(sliderClass, getController(cp5, "fov")));
		} finally {
			controls.dispose();
			dome.dispose();
		}
	}

	private static void assertLocalOutputControls(Object cp5) throws Exception {
		ControlPanelLayout.LocalOutput localOutput =
				ControlPanelLayout.localOutputFor(System.getProperty("os.name"));
		if (localOutput == ControlPanelLayout.LocalOutput.SPOUT) {
			Object toggle = getController(cp5, "spoutToggle");
			Object dropdown = getController(cp5, "Spout View");
			assertNotNull(toggle);
			assertNotNull(dropdown);
			assertEquals(ControlPanelLayout.yFor("spoutToggle"), getPosition(toggle)[1]);
			assertEquals(ControlPanelLayout.yFor("Spout View"), getPosition(dropdown)[1]);
			assertEquals(ViewType.STANDARD.ordinal(), getValue(dropdown));
			assertNull(getController(cp5, "syphonToggle"));
			assertNull(getController(cp5, "Syphon View"));
		} else if (localOutput == ControlPanelLayout.LocalOutput.SYPHON) {
			Object toggle = getController(cp5, "syphonToggle");
			Object dropdown = getController(cp5, "Syphon View");
			assertNotNull(toggle);
			assertNotNull(dropdown);
			assertEquals(ControlPanelLayout.yFor("syphonToggle"), getPosition(toggle)[1]);
			assertEquals(ControlPanelLayout.yFor("Syphon View"), getPosition(dropdown)[1]);
			assertEquals(ViewType.SKYBOX.ordinal(), getValue(dropdown));
			assertNull(getController(cp5, "spoutToggle"));
			assertNull(getController(cp5, "Spout View"));
		} else {
			assertNull(getController(cp5, "spoutToggle"));
			assertNull(getController(cp5, "Spout View"));
			assertNull(getController(cp5, "syphonToggle"));
			assertNull(getController(cp5, "Syphon View"));
		}
	}

	private static Object readControlP5(ControlManager controls) throws Exception {
		Field field = ControlManager.class.getDeclaredField("cp5");
		field.setAccessible(true);
		return field.get(controls);
	}

	private static Object getController(Object cp5, String name) throws Exception {
		return cp5.getClass().getMethod("getController", String.class).invoke(cp5, name);
	}

	private static float[] getPosition(Object controller) throws Exception {
		return (float[]) Class.forName("controlP5.Controller")
				.getMethod("getPosition")
				.invoke(controller);
	}

	private static float getValue(Object controller) throws Exception {
		return (float) Class.forName("controlP5.Controller")
				.getMethod("getValue")
				.invoke(controller);
	}

	private static boolean isVisible(Object controller) throws Exception {
		return (boolean) Class.forName("controlP5.Controller")
				.getMethod("isVisible")
				.invoke(controller);
	}

	private static int sliderMode(Class<?> sliderClass, Object slider) throws Exception {
		return (int) sliderClass.getMethod("getSliderMode").invoke(slider);
	}

	private static void setSliderValue(Class<?> sliderClass, Object slider, float value)
			throws Exception {
		sliderClass.getMethod("setValue", float.class).invoke(slider, value);
	}

	private static void scrollSlider(Class<?> sliderClass, Object slider, int steps)
			throws Exception {
		sliderClass.getMethod("scrolled", int.class).invoke(slider, steps);
	}

	private static PApplet configuredApplet() {
		PApplet applet = new PApplet();
		PGraphicsJava2D graphics = new PGraphicsJava2D();
		graphics.setParent(applet);
		graphics.setSize(640, 640);
		applet.g = graphics;
		return applet;
	}

	private static final class CallbackTrackingApplet extends PApplet {
		private final List<Object> keyEventTargets = new ArrayList<>();

		@Override
		public void registerMethod(String methodName, Object target) {
			if ("keyEvent".equals(methodName)) {
				keyEventTargets.add(target);
			}
			super.registerMethod(methodName, target);
		}
	}

	private static float parentAngle(ziviDomeLive dome, String controlName) {
		return switch (controlName) {
			case "pitch" -> dome.getPitch();
			case "yaw" -> dome.getYaw();
			case "roll" -> dome.getRoll();
			default -> throw new IllegalArgumentException("Unknown angle: " + controlName);
		};
	}

	private static void setOutputManager(ziviDomeLive dome, OutputManager outputManager) throws Exception {
		Field field = ziviDomeLive.class.getDeclaredField("outputManager");
		field.setAccessible(true);
		field.set(dome, outputManager);
	}

	private static OutputManager createOutputManager(ziviDomeLive dome) throws Exception {
		Class<?> implementation = Class.forName(
				"com.victorvalentim.zividomelive.OutputManagerImpl");
		var constructor = implementation.getDeclaredConstructor(ziviDomeLive.class);
		constructor.setAccessible(true);
		return (OutputManager) constructor.newInstance(dome);
	}
}
