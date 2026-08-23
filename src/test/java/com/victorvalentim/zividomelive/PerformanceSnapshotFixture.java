package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.performance.GpuTimerArchitecture;
import com.victorvalentim.zividomelive.performance.GpuTimerBackend;
import com.victorvalentim.zividomelive.performance.GpuTimerPolicy;
import com.victorvalentim.zividomelive.performance.PerformanceMode;
import com.victorvalentim.zividomelive.performance.PerformanceSnapshot;

import java.util.List;

/** Test-only access to runtime-owned snapshot construction. */
public final class PerformanceSnapshotFixture {
	private PerformanceSnapshotFixture() {
	}

	public static PerformanceSnapshot create(
			PerformanceMode requestedMode,
			PerformanceMode effectiveMode,
			long totalFrames,
			int storedFrames,
			long overwrittenFrames,
			long[][] durationsNanos,
			int[][] calls,
			long invariantViolations,
			long cubemapCaptureViolations,
			long unexpectedPassViolations,
			List<String> diagnostics,
			long[][] gpuDurationsNanos,
			int[][] gpuCalls,
			GpuTimerPolicy gpuTimerPolicy,
			GpuTimerBackend gpuTimerBackend,
			GpuTimerArchitecture gpuTimerArchitecture) {
		return new PerformanceSnapshotImpl(
				requestedMode,
				effectiveMode,
				totalFrames,
				storedFrames,
				overwrittenFrames,
				durationsNanos,
				calls,
				invariantViolations,
				cubemapCaptureViolations,
				unexpectedPassViolations,
				diagnostics,
				gpuDurationsNanos,
				gpuCalls,
				gpuTimerPolicy,
				gpuTimerBackend,
				gpuTimerArchitecture);
	}
}
