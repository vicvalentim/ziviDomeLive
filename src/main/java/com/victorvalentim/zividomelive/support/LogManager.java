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

		// Desativa o uso de handlers do logger pai
		globalLogger.setUseParentHandlers(false);

		globalLogger.setLevel(Level.ALL);

		// Remove handlers existentes para evitar duplicatas
		Handler[] handlers = globalLogger.getHandlers();
		for (Handler handler : handlers) {
			globalLogger.removeHandler(handler);
		}

		// Configurações de ConsoleHandler
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		consoleHandler.setFormatter(new CustomFormatter());
		globalLogger.addHandler(consoleHandler);

		// Configurações de FileHandler com validação de diretório
		try {
			FileHandler fileHandler = getFileHandler();
			globalLogger.addHandler(fileHandler);
		} catch (IOException e) {
			System.err.println("FileHandler configuration failed. Logs will only appear in the console.");
			e.printStackTrace();
		}

		// Filtro para ignorar mensagens duplicadas
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
	 * Configures and returns a FileHandler with appropriate directory and file handling.
	 *
	 * @return the configured FileHandler
	 * @throws IOException if the directory or file cannot be created
	 */
	private static FileHandler getFileHandler() throws IOException {
		String logDirectory;
		String logFile;

		// Define o local do log baseado no sistema operacional
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			logDirectory = System.getProperty("user.home") + "\\zividomelive\\logs";
			logFile = logDirectory + "\\ziviDomeLive.log";
		} else {
			logDirectory = "/tmp/zividomelive/logs";
			logFile = logDirectory + "/ziviDomeLive.log";
		}

		java.io.File directory = new java.io.File(logDirectory);

		// Verifica e cria o diretório, se necessário
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IOException("Failed to create log directory: " + logDirectory);
		}

		// Garante que o arquivo de log seja criado
		java.io.File logFileObject = new java.io.File(logFile);
		if (!logFileObject.exists() && !logFileObject.createNewFile()) {
			throw new IOException("Failed to create log file: " + logFile);
		}

		// Configura o FileHandler
		FileHandler fileHandler = new FileHandler(logFile, true); // true para anexar ao log existente
		fileHandler.setLevel(Level.ALL);
		fileHandler.setFormatter(new CustomFormatter());
		return fileHandler;
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
			return "[" +
					record.getLevel().getLocalizedName() +
					"] " +
					formatMessage(record) + "\n";
		}
	}
}
