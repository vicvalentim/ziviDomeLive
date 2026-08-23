package com.victorvalentim.zividomelive;

// Package-private implementation grouped physically under _internal/runtime.

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Process-wide daemon pool owned exclusively by the ziviDomeLive runtime. */
final class SharedTaskExecutor {

	private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();
	private static final ThreadFactory THREAD_FACTORY = runnable -> {
		Thread thread = new Thread(
				runnable,
				"zividomelive-task-" + THREAD_SEQUENCE.incrementAndGet());
		thread.setDaemon(true);
		return thread;
	};
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
			Math.max(1, Runtime.getRuntime().availableProcessors()),
			THREAD_FACTORY);

	private SharedTaskExecutor() {
	}

	static void execute(Runnable task) {
		EXECUTOR.execute(Objects.requireNonNull(task, "task"));
	}
}
