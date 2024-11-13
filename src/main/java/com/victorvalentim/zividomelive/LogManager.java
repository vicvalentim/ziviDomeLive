package com.victorvalentim.zividomelive;

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

	private static void configureLogger() {
		if (isConfigured) return;

		globalLogger.setLevel(Level.ALL);

		// Remove handlers para evitar duplicação
		Handler[] handlers = globalLogger.getHandlers();
		for (Handler handler : handlers) {
			globalLogger.removeHandler(handler);
		}

		// Configuração do ConsoleHandler
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		consoleHandler.setFormatter(new SimpleFormatter());
		globalLogger.addHandler(consoleHandler);

		// Configuração do FileHandler
		try {
			FileHandler fileHandler = new FileHandler("ziviDomeLive.log", true);
			fileHandler.setLevel(Level.ALL);
			fileHandler.setFormatter(new SimpleFormatter());
			globalLogger.addHandler(fileHandler);
		} catch (IOException e) {
			System.err.println("FileHandler configuration failed. Logs will only appear in the console.");
		}

		// Filtro para ignorar mensagens duplicadas usando lambda
		globalLogger.setFilter(record -> {
			String message = record.getMessage();
			if (message.equals(lastLogMessage)) {
				return false; // Ignora mensagens duplicadas
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
}
