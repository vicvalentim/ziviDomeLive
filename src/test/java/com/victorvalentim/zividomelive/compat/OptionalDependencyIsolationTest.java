package com.victorvalentim.zividomelive.compat;

import com.victorvalentim.zividomelive.ziviDomeLive;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OptionalDependencyIsolationTest {

	@Test
	void facadeAndPlatformOutputsFailSoftWhenOptionalLibrariesAreAbsent() throws Exception {
		URL mainClasses = ziviDomeLive.class.getProtectionDomain().getCodeSource().getLocation();
		try (OptionalLibrariesDeniedClassLoader loader = new OptionalLibrariesDeniedClassLoader(
				new URL[]{mainClasses}, getClass().getClassLoader())) {
			assertUnavailableAfterInitialization(loader, "Mac OS X", "SYPHON");
			assertUnavailableAfterInitialization(loader, "Windows 11", "SPOUT");
		}
	}

	private static void assertUnavailableAfterInitialization(
			ClassLoader loader,
			String osName,
			String outputName) throws Exception {
		String originalOsName = System.getProperty("os.name");
		try {
			System.setProperty("os.name", osName);
			Class<?> facadeType = loader.loadClass("com.victorvalentim.zividomelive.ziviDomeLive");
			Object facade = facadeType.getConstructor(PApplet.class).newInstance(new PApplet());

			Class<?> managerType = loader.loadClass(
					"com.victorvalentim.zividomelive.OutputManagerImpl");
			Constructor<?> constructor = managerType.getDeclaredConstructor(facadeType);
			constructor.setAccessible(true);
			Object manager = constructor.newInstance(facade);

			Method initialize = managerType.getDeclaredMethod("initializeLocalTextureOutput");
			initialize.setAccessible(true);
			initialize.invoke(manager);

			Class<?> outputType = loader.loadClass(
					"com.victorvalentim.zividomelive.manager.OutputManager$OutputType");
			@SuppressWarnings({"rawtypes", "unchecked"})
			Object output = Enum.valueOf((Class<? extends Enum>) outputType, outputName);
			Method getState = managerType.getMethod("getOutputState", outputType);
			getState.setAccessible(true);
			assertEquals("UNAVAILABLE", getState.invoke(manager, output).toString());
		} finally {
			if (originalOsName == null) {
				System.clearProperty("os.name");
			} else {
				System.setProperty("os.name", originalOsName);
			}
		}
	}

	private static final class OptionalLibrariesDeniedClassLoader extends URLClassLoader {

		OptionalLibrariesDeniedClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (name.startsWith("controlP5.")
					|| name.startsWith("codeanticode.syphon.")
					|| name.startsWith("spout.")) {
				throw new ClassNotFoundException("optional dependency denied by regression test: " + name);
			}
			if (name.startsWith("com.victorvalentim.zividomelive.")) {
				synchronized (getClassLoadingLock(name)) {
					Class<?> loaded = findLoadedClass(name);
					if (loaded == null) {
						loaded = findClass(name);
					}
					if (resolve) {
						resolveClass(loaded);
					}
					return loaded;
				}
			}
			return super.loadClass(name, resolve);
		}
	}
}
