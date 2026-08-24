package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/support.

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles the loading and retrieval of library metadata from a properties file.
 */
class LibraryMetadata {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Properties properties = new Properties();
	private static boolean isLoaded = false;

	static {
		try {
			String basePath = getBasePath();
			File propertiesFile = new File(basePath, "library.properties");

			if (!propertiesFile.exists()) {
				throw new IOException("library.properties file not found at: " + propertiesFile.getAbsolutePath());
			}

			try (FileInputStream input = new FileInputStream(propertiesFile)) {
				properties.load(input);
				isLoaded = true;
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to load library.properties.", e);
		}
	}

	/**
	 * Retrieves a property value by key.
	 *
	 * @param key the property key
	 * @return the property value, or "Unknown" if the key is not found
	 */
	public static String get(String key) {
		return properties.getProperty(key, "Unknown");
	}

	/**
	 * Checks if the library metadata was loaded successfully.
	 *
	 * @return true if loaded, false otherwise
	 */
	public static boolean isLoaded() {
		return isLoaded;
	}

	/**
	 * Gets the base path of the library installation.
	 * This is the parent directory of the `library/` folder where the `.jar` resides.
	 *
	 * @return the base path as a string
	 */
	private static String getBasePath() {
		String jarPath = LibraryMetadata.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		File jarFile = new File(jarPath);
		return jarFile.getParentFile().getParent(); // Move to root directory
	}
}
