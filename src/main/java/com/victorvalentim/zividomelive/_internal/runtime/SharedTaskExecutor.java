package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/runtime.

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Process-wide daemon pool owned exclusively by the ziviDomeLive runtime. */
final class SharedTaskExecutor {

	private static final int MAX_QUEUED_TASKS = 256;
	private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();
	private static final ThreadFactory THREAD_FACTORY = runnable -> {
		Thread thread = new Thread(
				runnable,
				"zividomelive-task-" + THREAD_SEQUENCE.incrementAndGet());
		thread.setDaemon(true);
		return thread;
	};
	private static final int WORKER_COUNT = Math.max(
			1, Runtime.getRuntime().availableProcessors());
	private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
			WORKER_COUNT,
			WORKER_COUNT,
			0L,
			TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(MAX_QUEUED_TASKS),
			THREAD_FACTORY,
			new ThreadPoolExecutor.AbortPolicy());

	private SharedTaskExecutor() {
	}

	static void execute(Runnable task) {
		EXECUTOR.execute(Objects.requireNonNull(task, "task"));
	}

	static int queueCapacity() {
		return EXECUTOR.getQueue().size() + EXECUTOR.getQueue().remainingCapacity();
	}

	static int workerCount() {
		return WORKER_COUNT;
	}
}
