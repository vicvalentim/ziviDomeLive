package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.LogMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LogManagerTest {

	@AfterEach
	void restoreReleaseMode() {
		LogManager.setMode(LogMode.RELEASE);
	}

	@Test
	void setModeRejectsNull() {
		assertThrows(IllegalArgumentException.class, () -> LogManager.setMode(null));
	}

	@Test
	void setModeUpdatesGetMode() {
		LogManager.setMode(LogMode.DEBUG);
		assertEquals(LogMode.DEBUG, LogManager.getMode());
		assertTrue(LogManager.isDebugEnabled());

		LogManager.setMode(LogMode.RELEASE);
		assertEquals(LogMode.RELEASE, LogManager.getMode());
		assertFalse(LogManager.isDebugEnabled());
	}

	@Test
	void releaseModeDisablesLogging() {
		LogManager.setMode(LogMode.RELEASE);
		Logger logger = LogManager.getLogger();
		assertFalse(logger.isLoggable(Level.SEVERE));
	}

	@Test
	void duplicateMessagesAreThrottledWithinWindow() {
		LogManager.setMode(LogMode.DEBUG);
		Filter filter = LogManager.getLogger().getFilter();
		assertNotNull(filter, "DEBUG mode should install a duplicate-throttling filter");

		LogRecord first = new LogRecord(Level.INFO, "throttle-test-message");
		LogRecord duplicate = new LogRecord(Level.INFO, "throttle-test-message");
		LogRecord different = new LogRecord(Level.INFO, "another-message");

		assertTrue(filter.isLoggable(first), "First occurrence must pass");
		assertFalse(filter.isLoggable(duplicate), "Immediate duplicate must be suppressed");
		assertTrue(filter.isLoggable(different), "A different message must pass");
	}
}
