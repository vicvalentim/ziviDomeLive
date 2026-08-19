package com.victorvalentim.zividomelive.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuTimerSelectionTest {

	@Test
	void detectsAppleSiliconFromJvmArchitecture() {
		assertEquals(
				GpuTimerArchitecture.APPLE_SILICON,
				GpuTimerArchitecture.detect(
						"Mac OS X",
						"aarch64",
						"Apple",
						"Apple M1"));
	}

	@Test
	void detectsAppleSiliconRendererWhenJvmRunsTranslated() {
		assertEquals(
				GpuTimerArchitecture.APPLE_SILICON,
				GpuTimerArchitecture.detect(
						"Mac OS X",
						"x86_64",
						"Apple",
						"Apple M2"));
	}

	@Test
	void detectsAppleIntelFromJvmArchitecture() {
		assertEquals(
				GpuTimerArchitecture.APPLE_INTEL,
				GpuTimerArchitecture.detect(
						"Mac OS X",
						"x86_64",
						"Apple",
						"Intel Iris"));
	}

	@Test
	void architectureAwarePolicyPrefersTimestampPairsOnAppleSilicon() {
		assertEquals(
				GpuTimerBackend.TIMESTAMP_PAIR,
				GpuTimerPolicy.ARCHITECTURE_AWARE.selectBackend(
						GpuTimerArchitecture.APPLE_SILICON,
						64,
						32));
	}

	@Test
	void architectureAwarePolicyPrefersTimestampPairsOnAppleIntel() {
		assertEquals(
				GpuTimerBackend.TIMESTAMP_PAIR,
				GpuTimerPolicy.ARCHITECTURE_AWARE.selectBackend(
						GpuTimerArchitecture.APPLE_INTEL,
						64,
						32));
	}

	@Test
	void architectureAwarePolicyUsesElapsedCandidateOnAppleSilicon() {
		assertEquals(
				GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE,
				GpuTimerPolicy.ARCHITECTURE_AWARE.selectBackend(
						GpuTimerArchitecture.APPLE_SILICON,
						0,
						32));
	}

	@Test
	void architectureAwarePolicyUsesElapsedCandidateOnAppleIntel() {
		assertEquals(
				GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE,
				GpuTimerPolicy.ARCHITECTURE_AWARE.selectBackend(
						GpuTimerArchitecture.APPLE_INTEL,
						0,
						32));
	}

	@Test
	void architectureAwarePolicyIsCapabilityDrivenOnLinux() {
		assertEquals(
				GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE,
				GpuTimerPolicy.ARCHITECTURE_AWARE.selectBackend(
						GpuTimerArchitecture.LINUX_X86_64,
						0,
						32));
	}

	@Test
	void safePolicyNeverClaimsElapsedQueryOwnership() {
		assertEquals(
				GpuTimerBackend.NONE,
				GpuTimerPolicy.SAFE.selectBackend(
						GpuTimerArchitecture.APPLE_SILICON,
						0,
						32));
	}

	@Test
	void explicitElapsedPolicyCanBeUsedForControlledCrossPlatformDiagnostics() {
		assertEquals(
				GpuTimerBackend.TIME_ELAPSED_EXCLUSIVE,
				GpuTimerPolicy.TIME_ELAPSED_EXCLUSIVE.selectBackend(
						GpuTimerArchitecture.WINDOWS_X86_64,
						64,
						32));
	}

	@Test
	void architectureAwarePolicyAllowsQualifiedFallbackLayers() {
		assertTrue(GpuTimerPolicy.ARCHITECTURE_AWARE.allowsElapsedFallback());
		assertTrue(GpuTimerPolicy.ARCHITECTURE_AWARE.allowsFenceFallback());
	}
}
