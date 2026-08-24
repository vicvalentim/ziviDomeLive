package com.victorvalentim.zividomelive;

// Package-private implementation test grouped physically under _internal/output.

import com.victorvalentim.zividomelive.manager.OutputManager;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputBackendSeparationTest {

	@Test
	void nativeBackendsRemainConcreteFinalServicesBehindAnOptionalContract() {
		for (Class<?> backend : new Class<?>[]{
				NdiOutputBackend.class,
				SpoutOutputBackend.class,
				SyphonOutputBackend.class
		}) {
			assertTrue(Modifier.isFinal(backend.getModifiers()), backend.getName());
			assertFalse(Modifier.isPublic(backend.getModifiers()), backend.getName());
			assertFalse(backend.isInterface(), backend.getName());
			assertFalse(Modifier.isAbstract(backend.getModifiers()), backend.getName());
		}
		assertFalse(Modifier.isPublic(LocalTextureOutputBackend.class.getModifiers()));
		assertTrue(LocalTextureOutputBackend.class.isInterface());
	}

	@Test
	void eachNativeResourceIsOwnedOnlyByItsConcreteBackend() throws Exception {
		assertEquals(NdiOutputBackend.class,
				OutputManagerImpl.class.getDeclaredField("ndiBackend").getType());
		assertEquals(LocalTextureOutputBackend.class,
				OutputManagerImpl.class.getDeclaredField("localTextureOutput").getType());

		assertTrue(hasFieldWithTypeName(
				NdiOutputBackend.class, "me.walkerknapp.devolay.DevolaySender"));
		assertTrue(hasFieldWithTypeName(SpoutOutputBackend.class, "spout.Spout"));
		assertTrue(hasFieldWithTypeName(
				SyphonOutputBackend.class, "codeanticode.syphon.SyphonServer"));

		assertFalse(hasFieldWithTypeName(
				OutputManagerImpl.class, "me.walkerknapp.devolay.DevolaySender"));
		assertFalse(hasFieldWithTypeName(OutputManagerImpl.class, "spout.Spout"));
		assertFalse(hasFieldWithTypeName(
				OutputManagerImpl.class, "codeanticode.syphon.SyphonServer"));
	}

	@Test
	void unsupportedGpuBackendsStayUnavailableWithoutNativeInitialization() {
		SpoutOutputBackend spout = new SpoutOutputBackend(new PApplet(), false);
		SyphonOutputBackend syphon = new SyphonOutputBackend(new PApplet(), false);

		spout.initialize(null, 1024);
		syphon.initialize();

		assertEquals(OutputManager.OutputState.UNAVAILABLE, spout.state());
		assertEquals(OutputManager.OutputState.UNAVAILABLE, syphon.state());
	}

	private static boolean hasFieldWithTypeName(Class<?> owner, String fieldTypeName) {
		return Arrays.stream(owner.getDeclaredFields())
				.map(Field::getType)
				.map(Class::getName)
				.anyMatch(fieldTypeName::equals);
	}
}
