package com.victorvalentim.zividomelive.compat;

import com.victorvalentim.zividomelive.Scene;
import com.victorvalentim.zividomelive.SceneServices;
import org.junit.jupiter.api.Test;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class SceneContractTest {

	@Test
	void sceneRenderIsTheOnlyAbstractSceneMethod() throws Exception {
		Method sceneRender = Scene.class.getMethod("sceneRender", PGraphicsOpenGL.class);
		assertTrue(Modifier.isAbstract(sceneRender.getModifiers()));

		assertDefaultMethod("setupScene");
		assertDefaultMethod("configure", SceneServices.class);
		assertDefaultMethod("update");
		assertDefaultMethod("keyEvent", KeyEvent.class);
		assertDefaultMethod("mouseEvent", MouseEvent.class);
		assertDefaultControlEventMethod();
		assertDefaultMethod("dispose");
		assertDefaultMethod("getName");
	}

	@Test
	void defaultSceneNameUsesConcreteClassSimpleName() {
		Scene scene = new ContractScene();
		assertEquals("ContractScene", scene.getName());
	}

	private static void assertDefaultMethod(String name, Class<?>... parameterTypes) throws Exception {
		Method method = Scene.class.getMethod(name, parameterTypes);
		assertTrue(method.isDefault(), name + " must remain a default method");
	}

	private static void assertDefaultControlEventMethod() {
		boolean found = false;
		for (Method method : Scene.class.getMethods()) {
			if (method.getName().equals("controlEvent")
					&& method.getParameterCount() == 1
					&& method.getParameterTypes()[0].getName().equals("controlP5.ControlEvent")) {
				found = method.isDefault();
				break;
			}
		}
		assertTrue(found, "controlEvent(ControlEvent) must remain a default method");
	}

	private static class ContractScene implements Scene {
		@Override
		public void sceneRender(PGraphicsOpenGL pg) {
			// no-op
		}
	}
}
