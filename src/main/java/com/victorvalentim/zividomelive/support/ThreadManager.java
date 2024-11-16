package com.victorvalentim.zividomelive.support;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ThreadManager class centralizes thread management for computational tasks across the application.
 * It provides an ExecutorService with a fixed thread pool based on the number of available processors,
 * designed specifically for CPU-intensive calculations and non-rendering tasks.
 */
public class ThreadManager {

	private static final Logger LOGGER = LogManager.getLogger(); // Using LogManager for centralized logging
	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
	private static final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

	private ThreadManager() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Submits a callable task for asynchronous execution and returns a Future representing the result.
	 *
	 * @param <T>  the type of the result returned by the task
	 * @param task the task to submit
	 * @return a Future representing the result of the task
	 */
	public static <T> Future<T> submitTask(Callable<T> task) {
		try {
			return executor.submit(task);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error submitting task for execution", e);
			throw e;
		}
	}

	/**
	 * Submits a runnable task for asynchronous execution without expecting a return result.
	 *
	 * @param task the task to submit
	 */
	public static void submitRunnable(Runnable task) {
		try {
			executor.submit(task);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error submitting runnable task for execution", e);
		}
	}

	/**
	 * Shuts down the ExecutorService, allowing previously submitted tasks to complete execution.
	 * This should be called when the application is stopping to release resources.
	 */
	public static void shutdown() {
		try {
			executor.shutdown();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error shutting down the executor", e);
		}
	}

	/**
	 * Shuts down the ExecutorService with a timeout, waiting for tasks to complete.
	 * If tasks do not complete in the specified time, a forced shutdown is initiated.
	 *
	 * @param timeout the maximum time to wait for tasks to complete
	 * @param unit the time unit of the timeout argument
	 */
	public static void shutdownWithTimeout(long timeout, TimeUnit unit) {
		try {
			executor.shutdown();
			if (!executor.awaitTermination(timeout, unit)) {
				LOGGER.warning("Executor did not terminate in the specified time.");
				List<Runnable> remainingTasks = executor.shutdownNow();
				LOGGER.warning("Remaining tasks that did not execute: " + remainingTasks.size());
			}
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Shutdown interrupted", e);
			executor.shutdownNow();
		}
	}

	/**
	 * Forces shutdown of the ExecutorService, attempting to stop all running tasks.
	 * This should only be used if an immediate shutdown is necessary.
	 */
	public static void shutdownNow() {
		try {
			List<Runnable> remainingTasks = executor.shutdownNow();
			LOGGER.warning("Forcing shutdown. Remaining tasks that did not execute: " + remainingTasks.size());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error forcing shutdown of the executor", e);
		}
	}

	/**
	 * Checks if the ExecutorService is already shut down.
	 *
	 * @return true if the executor is shut down, false otherwise
	 */
	public static boolean isShutdown() {
		return executor.isShutdown();
	}

	/**
	 * Checks if the ExecutorService has terminated all tasks.
	 *
	 * @return true if all tasks have completed following shutdown, false otherwise
	 */
	public static boolean isTerminated() {
		return executor.isTerminated();
	}

	/**
	 * Provides access to the executor service, allowing integration with other asynchronous processing classes.
	 *
	 * @return the ExecutorService used for task management
	 */
	public static ExecutorService getExecutor() {
		return executor;
	}
}
