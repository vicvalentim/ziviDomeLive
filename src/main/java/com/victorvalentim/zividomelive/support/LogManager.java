package com.victorvalentim.zividomelive.support;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;

/**
 * Manages logging configuration for the application.
 */
public class LogManager {
	/** Supported logging profiles for the library. */
	public enum Mode {
		/** Verbose logging to console and file. */
		DEBUG,
		/** Silent mode: disables LogManager output. */
		RELEASE
	}

	private static final Logger globalLogger = Logger.getLogger("com.victorvalentim.zividomelive");
	private static boolean isConfigured = false;
	private static final AtomicReference<String> lastLogMessage = new AtomicReference<>("");
	private static final java.util.concurrent.atomic.AtomicLong lastLogTimestamp = new java.util.concurrent.atomic.AtomicLong(0L);
	private static final long DUPLICATE_LOG_THROTTLE_MS = 5000L;
	private static Mode currentMode = Mode.RELEASE;

	private LogManager() {}


	/**
	 * Sets the global logging mode.
	 *
	 * @param mode desired logging mode
	 */
	public static synchronized void setMode(Mode mode) {
		if (mode == null) {
			throw new IllegalArgumentException("Log mode cannot be null.");
		}
		currentMode = mode;
		isConfigured = false;
		configureLogger();
	}

	/**
	 * Returns the currently configured logging mode.
	 *
	 * @return current logging mode
	 */
	public static synchronized Mode getMode() {
		return currentMode;
	}

	/**
	 * Configures the global logger with a custom format and handlers.
	 */
	private static synchronized void configureLogger() {
		if (isConfigured) {
			return;
		}

		// Disable parent logger handlers to prevent duplicate logging
		globalLogger.setUseParentHandlers(false);

		// Remove existing handlers to avoid duplicates
		Handler[] handlers = globalLogger.getHandlers();
		for (Handler handler : handlers) {
			globalLogger.removeHandler(handler);
			handler.close();
		}

		if (currentMode == Mode.RELEASE) {
			globalLogger.setLevel(Level.OFF);
			globalLogger.setFilter(null);
			isConfigured = true;
			return;
		}

		globalLogger.setLevel(Level.ALL);

		// Configure ConsoleHandler for console output
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		consoleHandler.setFormatter(new CustomFormatter());
		globalLogger.addHandler(consoleHandler);

		// Configure FileHandler with directory validation
		try {
			FileHandler fileHandler = getFileHandler();
			globalLogger.addHandler(fileHandler);
		} catch (IOException e) {
			globalLogger.log(Level.WARNING, "FileHandler configuration failed. Logs will only appear in the console.", e);
		}

		// Time-based throttle for duplicate log messages: the same message is
		// allowed through at most once every DUPLICATE_LOG_THROTTLE_MS.
		globalLogger.setFilter(record -> {
			String message = record.getMessage();
			long now = System.currentTimeMillis();
			String previousMessage = lastLogMessage.get();
			if (message != null && message.equals(previousMessage)
					&& (now - lastLogTimestamp.get()) < DUPLICATE_LOG_THROTTLE_MS) {
				return false; // Suppress duplicate message within throttle window
			}
			lastLogMessage.set(message);
			lastLogTimestamp.set(now);
			return true;
		});

		isConfigured = true;
	}

	/**
	 * Configures and returns a FileHandler with appropriate directory and file handling.
	 *
	 * @return the configured FileHandler
	 * @throws IOException if the directory or file cannot be created
	 */
	private static FileHandler getFileHandler() throws IOException {
		String logDirectory;
		String logFile;

		// Determine log directory based on operating system
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			logDirectory = System.getProperty("user.home") + "\\zividomelive\\logs";
			logFile = logDirectory + "\\ziviDomeLive.log";
		} else {
			logDirectory = "/tmp/zividomelive/logs";
			logFile = logDirectory + "/ziviDomeLive.log";
		}

		java.io.File directory = new java.io.File(logDirectory);

		// Create log directory if it does not exist
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IOException("Failed to create log directory: " + logDirectory);
		}

		// Ensure log file is created
		java.io.File logFileObject = new java.io.File(logFile);
		if (!logFileObject.exists() && !logFileObject.createNewFile()) {
			throw new IOException("Failed to create log file: " + logFile);
		}

		// Configure FileHandler with append mode enabled
		FileHandler fileHandler = new FileHandler(logFile, true); // true: append to existing log
		fileHandler.setLevel(Level.ALL);
		fileHandler.setFormatter(new CustomFormatter());
		return fileHandler;
	}

	/**
	 * Returns the global logger instance for the application.
	 * If the logger is not yet configured, it will be configured before returning.
	 * This method is thread-safe.
	 *
	 * @return the global logger instance
	 */
	public static synchronized Logger getLogger() {
		if (!isConfigured) {
			configureLogger();
		}
		return globalLogger;
	}

	/**
	 * A custom formatter for log messages with a clean, minimal design.
	 */
	private static class CustomFormatter extends Formatter {
		@Override
		public String format(LogRecord record) {
			return "[" +
					record.getLevel().getLocalizedName() +
					"] " +
					formatMessage(record) + "\n";
		}
	}
}
