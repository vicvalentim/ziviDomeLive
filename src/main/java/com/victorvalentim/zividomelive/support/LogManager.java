package com.victorvalentim.zividomelive.support;

import java.io.IOException;
import java.util.logging.*;

/**
 * Manages logging configuration for the application.
 */
public class LogManager {

	private static final Logger globalLogger = Logger.getLogger("com.victorvalentim.zividomelive");
	private static boolean isConfigured = false;
	private static String lastLogMessage = "";

	private LogManager() {}

	/**
	 * Configures the global logger with a custom format and handlers.
	 */
	private static void configureLogger() {
		if (isConfigured) return;

		globalLogger.setLevel(Level.ALL);

		// Remove existing handlers to prevent duplicates
		Handler[] handlers = globalLogger.getHandlers();
		for (Handler handler : handlers) {
			globalLogger.removeHandler(handler);
		}

		// Create and configure ConsoleHandler
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		consoleHandler.setFormatter(new CustomFormatter());
		globalLogger.addHandler(consoleHandler);

		// Create and configure FileHandler
		try {
			FileHandler fileHandler = new FileHandler("ziviDomeLive.log", true);
			fileHandler.setLevel(Level.ALL);
			fileHandler.setFormatter(new CustomFormatter());
			globalLogger.addHandler(fileHandler);
		} catch (IOException e) {
			System.err.println("FileHandler configuration failed. Logs will only appear in the console.");
		}

		// Filter to ignore duplicate messages
		globalLogger.setFilter(record -> {
			String message = record.getMessage();
			if (message.equals(lastLogMessage)) {
				return false; // Ignore duplicate messages
			}
			lastLogMessage = message;
			return true;
		});

		isConfigured = true;
	}

	/**
	 * Returns the global logger instance for the application.
	 * If the logger is not yet configured, it will be configured before returning.
	 *
	 * @return the global logger instance
	 */
	public static Logger getLogger() {
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
			StringBuilder sb = new StringBuilder();
			sb.append("[")
					.append(record.getLevel().getLocalizedName())
					.append("] ");
			sb.append(formatMessage(record)).append("\n");
			return sb.toString();
		}
	}
}
