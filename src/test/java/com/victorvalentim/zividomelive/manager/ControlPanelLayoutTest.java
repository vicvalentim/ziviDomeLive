package com.victorvalentim.zividomelive.manager;

import com.victorvalentim.zividomelive.zividomelive;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControlPanelLayoutTest {

	@Test
	void everyControlBelongsToOneExplicitScope() {
		assertEquals(
				List.of("fpsLabel"),
				namesIn(ControlScope.GLOBAL));
		assertEquals(
				List.of(
						"pitchValue", "pitch",
						"yawValue", "yaw",
						"rollValue", "roll",
						"fovValue", "fov",
						"sizeValue", "size",
						"resetControls"),
				namesIn(ControlScope.SPHERICAL));
		assertEquals(
				List.of("previewToggle", "View Mode"),
				namesIn(ControlScope.VIEW));
		assertEquals(
				List.of(
						"Output Resolution",
						"ndiToggle", "spoutToggle", "syphonToggle",
						"NDI View", "Spout View", "Syphon View"),
				namesIn(ControlScope.OUTPUTS));
	}

	@Test
	void scopesPreserveLegacyPanelCoordinates() {
		assertAll(
				() -> assertEquals(20, ControlPanelLayout.yFor("fpsLabel")),
				() -> assertEquals(55, ControlPanelLayout.yFor("pitch")),
				() -> assertEquals(90, ControlPanelLayout.yFor("yaw")),
				() -> assertEquals(125, ControlPanelLayout.yFor("roll")),
				() -> assertEquals(160, ControlPanelLayout.yFor("fov")),
				() -> assertEquals(195, ControlPanelLayout.yFor("size")),
				() -> assertEquals(230, ControlPanelLayout.yFor("resetControls")),
				() -> assertEquals(265, ControlPanelLayout.yFor("previewToggle")),
				() -> assertEquals(300, ControlPanelLayout.yFor("View Mode")),
				() -> assertEquals(335, ControlPanelLayout.yFor("Output Resolution")),
				() -> assertEquals(370, ControlPanelLayout.yFor("ndiToggle")),
				() -> assertEquals(405, ControlPanelLayout.yFor("spoutToggle")),
				() -> assertEquals(405, ControlPanelLayout.yFor("syphonToggle")),
				() -> assertEquals(440, ControlPanelLayout.yFor("NDI View")),
				() -> assertEquals(475, ControlPanelLayout.yFor("Spout View")),
				() -> assertEquals(475, ControlPanelLayout.yFor("Syphon View")));
	}

	@Test
	void sphericalRangesAndDefaultsRemainCompatible() {
		List<ControlPanelLayout.SphericalControlSpec> controls =
				ControlPanelLayout.sphericalControls();

		assertEquals(List.of("pitch", "yaw", "roll", "fov", "size"),
				controls.stream().map(ControlPanelLayout.SphericalControlSpec::name).toList());
		assertAll(
				() -> assertEquals(-Math.PI, controls.get(0).minimum()),
				() -> assertEquals(Math.PI, controls.get(0).maximum()),
				() -> assertEquals(0.0f, controls.get(0).defaultValue()),
				() -> assertEquals(0.0, controls.get(3).minimum()),
				() -> assertEquals(360.0, controls.get(3).maximum()),
				() -> assertEquals(210.0f, controls.get(3).defaultValue()),
				() -> assertEquals(0.0, controls.get(4).minimum()),
				() -> assertEquals(100.0, controls.get(4).maximum()),
				() -> assertEquals(100.0f, controls.get(4).defaultValue()));
	}

	@Test
	void viewLabelsKeepCompatibilityLockedEnumOrder() {
		assertEquals(
				List.of("Fisheye Domemaster", "Equirectangular", "Cubemap Skybox", "Standard"),
				ControlPanelLayout.viewLabels());
		for (int index = 0; index < zividomelive.ViewType.values().length; index++) {
			assertEquals(zividomelive.ViewType.values()[index], ControlPanelLayout.viewForIndex(index));
			assertEquals(index, ControlPanelLayout.indexForView(zividomelive.ViewType.values()[index]));
		}
		assertEquals(0, ControlPanelLayout.indexForView(null));
	}

	@Test
	void outputResolutionPresetsAndSelectionRemainStable() {
		assertEquals(List.of(1024, 2048, 3072, 4096), ControlPanelLayout.outputResolutions());
		for (int index = 0; index < ControlPanelLayout.outputResolutions().size(); index++) {
			int resolution = ControlPanelLayout.outputResolutionForIndex(index);
			assertEquals(index, ControlPanelLayout.indexForOutputResolution(resolution));
		}
		assertEquals(-1, ControlPanelLayout.indexForOutputResolution(1536));
	}

	@Test
	void localOutputControlsFollowPlatformBoundary() {
		assertAll(
				() -> assertEquals(ControlPanelLayout.LocalOutput.SPOUT,
						ControlPanelLayout.localOutputFor("Windows 11")),
				() -> assertEquals(ControlPanelLayout.LocalOutput.SYPHON,
						ControlPanelLayout.localOutputFor("Mac OS X")),
				() -> assertEquals(ControlPanelLayout.LocalOutput.NONE,
						ControlPanelLayout.localOutputFor("Linux")),
				() -> assertEquals(ControlPanelLayout.LocalOutput.NONE,
						ControlPanelLayout.localOutputFor(null)));
	}

	@Test
	void unknownControlCannotSilentlyEnterThePanel() {
		assertThrows(IllegalArgumentException.class, () -> ControlPanelLayout.yFor("unknown"));
		assertThrows(IllegalArgumentException.class,
				() -> ControlPanelLayout.yFor(ControlScope.OUTPUTS, "pitch"));
	}

	private static List<String> namesIn(ControlScope scope) {
		return ControlPanelLayout.slotsFor(scope).stream()
				.map(ControlPanelLayout.ControlSlot::name)
				.toList();
	}
}
