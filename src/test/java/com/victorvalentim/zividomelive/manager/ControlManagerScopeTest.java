package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.zividomelive;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PApplet;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ControlManagerScopeTest {

	@Test
	void scopedBuildersPreserveControlP5LayoutAndInitialSelections() throws Exception {
		PApplet applet = new PApplet();
		PGraphicsJava2D graphics = new PGraphicsJava2D();
		graphics.setParent(applet);
		graphics.setSize(640, 640);
		applet.g = graphics;
		zividomelive dome = new zividomelive(applet);
		OutputManager outputs = new OutputManager(dome);
		outputs.setNdiView(zividomelive.ViewType.EQUIRECTANGULAR);
		outputs.setSpoutView(zividomelive.ViewType.STANDARD);
		outputs.setSyphonView(zividomelive.ViewType.CUBEMAP);
		setOutputManager(dome, outputs);
		dome.setCurrentView(zividomelive.ViewType.CUBEMAP);

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

			assertEquals(zividomelive.ViewType.CUBEMAP.ordinal(),
					getValue(getController(cp5, "View Mode")));
			assertEquals(2, getValue(getController(cp5, "Output Resolution")));
			assertEquals(zividomelive.ViewType.EQUIRECTANGULAR.ordinal(),
					getValue(getController(cp5, "NDI View")));
			assertLocalOutputControls(cp5);
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
			assertEquals(zividomelive.ViewType.STANDARD.ordinal(), getValue(dropdown));
			assertNull(getController(cp5, "syphonToggle"));
			assertNull(getController(cp5, "Syphon View"));
		} else if (localOutput == ControlPanelLayout.LocalOutput.SYPHON) {
			Object toggle = getController(cp5, "syphonToggle");
			Object dropdown = getController(cp5, "Syphon View");
			assertNotNull(toggle);
			assertNotNull(dropdown);
			assertEquals(ControlPanelLayout.yFor("syphonToggle"), getPosition(toggle)[1]);
			assertEquals(ControlPanelLayout.yFor("Syphon View"), getPosition(dropdown)[1]);
			assertEquals(zividomelive.ViewType.CUBEMAP.ordinal(), getValue(dropdown));
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
		return (float[]) controller.getClass().getMethod("getPosition").invoke(controller);
	}

	private static float getValue(Object controller) throws Exception {
		return (float) controller.getClass().getMethod("getValue").invoke(controller);
	}

	private static void setOutputManager(zividomelive dome, OutputManager outputManager) throws Exception {
		Field field = zividomelive.class.getDeclaredField("outputManager");
		field.setAccessible(true);
		field.set(dome, outputManager);
	}
}
