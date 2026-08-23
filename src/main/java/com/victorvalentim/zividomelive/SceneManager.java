package com.victorvalentim.zividomelive;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Maintains an identity-ordered collection of scenes and switches their activation.
 *
 * <p>A detached manager selects its first registered scene but does not invoke lifecycle methods.
 * Attaching it through {@link ziviDomeLive#setSceneManager(SceneManager)} installs the facade
 * lifecycle and activates the selection with fresh {@link SceneServices}. Scene membership uses
 * Java object identity ({@code ==}), not {@link Object#equals(Object)}.</p>
 *
 * <p>Mutation methods are intended for the Processing thread and are not thread-safe.</p>
 *
 * <p><strong>API stability:</strong> Stable.</p>
 */
public class SceneManager {

	interface LifecycleListener {
		void beforeSetup(Scene scene);
		void beforeDispose(Scene scene);
		void afterDispose(Scene scene);
	}

	private final List<Scene> scenes; // List of registered scenes
	private int currentSceneIndex = -1; // Index of the current scene (-1 when no scene is active)
	private boolean currentSceneActive;
	private LifecycleListener lifecycleListener;
	private static final Logger LOGGER = LogManager.getLogger();
	/**
	 * Creates an empty, detached scene manager.
	 */
	public SceneManager() {
		this.scenes = new ArrayList<>();
	}

	/**
	 * Registers a scene by instance identity.
	 *
	 * <p>The first registration becomes the selected scene. It is set up immediately only when
	 * this manager is already attached to a facade; otherwise setup begins when attached.</p>
	 *
	 * @param scene scene to register; {@code null} and duplicate instances are ignored
	 */
	public void registerScene(Scene scene) {
		if (scene == null) {
			LOGGER.severe("Cannot register a null scene.");
			return;
		}

		if (indexOfIdentity(scene) >= 0) {
			LOGGER.warning("Scene already registered: " + scene.getName());
			return;
		}

		scenes.add(scene);
		if (currentSceneIndex == -1) {
			// Automatically set the first scene as current if none is active
			currentSceneIndex = 0;
			setupScene(scene);
			LOGGER.info("First scene registered and set as current: " + scene.getName());
		} else {
			LOGGER.info("Scene registered: " + scene.getName());
		}
	}

	/**
	 * Activates a scene by instance; registers it first if it is not in the manager yet.
	 *
	 * @param scene scene instance to activate; {@code null} is ignored
	 */
	public void activateScene(Scene scene) {
		if (scene == null) {
			LOGGER.severe("Cannot activate a null scene.");
			return;
		}

		int index = indexOfIdentity(scene);
		if (index == -1) {
			scenes.add(scene);
			index = scenes.size() - 1;
			LOGGER.info("Scene auto-registered during activation: " + scene.getName());
		}

		if (currentSceneIndex == index && currentSceneActive) {
			LOGGER.info("Scene already active, skipping reinitialization: " + scene.getName());
			return;
		}
		if (currentSceneIndex == index) {
			setupScene(scene);
			return;
		}

		int previousIndex = currentSceneIndex;
		currentSceneIndex = index;
		disposeScene(previousIndex);
		Scene activeScene = scenes.get(currentSceneIndex);
		setupScene(activeScene);
		LOGGER.info("Scene activated: " + activeScene.getName());
	}

	/**
	 * Returns true when the manager already contains the provided scene instance.
	 *
	 * @param scene scene to check
	 * @return {@code true} if that exact scene instance is already managed
	 */
	public boolean containsScene(Scene scene) {
		return indexOfIdentity(scene) >= 0;
	}

	/**
	 * Returns the total number of registered scenes.
	 *
	 * @return the number of scenes
	 */
	public int getSceneCount() {
		return scenes.size();
	}

	/**
	 * Switches to the next scene in registration order, wrapping at the end.
	 */
	public void nextScene() {
		if (scenes.isEmpty()) {
			LOGGER.severe("No scenes to switch to.");
			return;
		}

		int previousIndex = currentSceneIndex;
		currentSceneIndex = (currentSceneIndex + 1) % scenes.size();

		if (previousIndex != currentSceneIndex) {
			disposeScene(previousIndex);
			Scene newScene = getCurrentScene();
			setupScene(newScene);
			LOGGER.info("Switched to the next scene: " + newScene.getName());
		}
	}


	/**
	 * Switches to the previous scene in registration order, wrapping at the beginning.
	 */
	public void previousScene() {
		if (scenes.isEmpty()) {
			LOGGER.severe("No scenes to switch to.");
			return;
		}

		int previousIndex = currentSceneIndex;
		currentSceneIndex = (currentSceneIndex - 1 + scenes.size()) % scenes.size();
		if (previousIndex != currentSceneIndex) {
			disposeScene(previousIndex);
			Scene newScene = getCurrentScene();
			setupScene(newScene);
			LOGGER.info("Switched to the previous scene: " + newScene.getName());
		} else {
			LOGGER.info("No change in scene: still on " + getCurrentScene().getName());
		}
	}

	/**
	 * Returns the current scene.
	 *
	 * @return selected scene, or {@code null} when no scene is registered
	 */
	public Scene getCurrentScene() {
		if (scenes.isEmpty() || currentSceneIndex == -1) {
			return null;
		}
		return scenes.get(currentSceneIndex);
	}

	/**
	 * Selects and activates the scene at the supplied registration index.
	 *
	 * @param index zero-based registration index; invalid values are ignored
	 */
	public void setCurrentSceneIndex(int index) {
		if (index < 0 || index >= scenes.size()) {
			LOGGER.severe("Invalid scene index: " + index);
			return;
		}

		if (currentSceneIndex == index) {
			if (!currentSceneActive) {
				setupScene(getCurrentScene());
			}
			return;
		}

		int previousIndex = currentSceneIndex;
		currentSceneIndex = index;
		disposeScene(previousIndex);
		Scene newScene = getCurrentScene();
		if (newScene != null) {
			setupScene(newScene);
			LOGGER.info("Scene set to index " + index + ": " + newScene.getName());
		}
	}

	/**
	 * Disposes the scene at the given index, releasing its resources.
	 *
	 * @param index index of the scene to dispose; ignored if out of range
	 */
	private void disposeScene(int index) {
		if (index < 0 || index >= scenes.size() || !currentSceneActive) {
			return;
		}
		currentSceneActive = false;
		disposeActivation(scenes.get(index));
	}

	private void disposeActivation(Scene scene) {
		if (lifecycleListener != null) {
			try {
				lifecycleListener.beforeDispose(scene);
			} catch (RuntimeException | LinkageError error) {
				LOGGER.warning("Error preparing scene disposal " + scene.getName()
						+ ": " + error.getMessage());
			}
		}
		try {
			scene.dispose();
		} catch (RuntimeException | LinkageError error) {
			LOGGER.warning("Error disposing scene " + scene.getName() + ": " + error.getMessage());
		} finally {
			if (lifecycleListener != null) {
				try {
					lifecycleListener.afterDispose(scene);
				} catch (RuntimeException | LinkageError error) {
					LOGGER.warning("Error releasing scene " + scene.getName()
							+ ": " + error.getMessage());
				}
			}
		}
	}

	/**
	 * Performs one complete dispose/setup cycle for the active scene.
	 *
	 * <p>This method does not defer work. From scene code, prefer
	 * {@link SceneServices#requestReload()}, which schedules the cycle at a safe frame boundary.</p>
	 *
	 * @return {@code true} when an attached, active scene was reloaded
	 */
	public boolean reloadCurrentScene() {
		if (currentSceneIndex < 0 || currentSceneIndex >= scenes.size() || !currentSceneActive) {
			return false;
		}
		Scene scene = scenes.get(currentSceneIndex);
		disposeScene(currentSceneIndex);
		setupScene(scene);
		LOGGER.info("Scene reloaded: " + scene.getName());
		return true;
	}

	/**
	 * Disposes the active scene, clears all registrations, and resets the manager.
	 *
	 * <p>Inactive scenes have already been disposed when they were deactivated. Scenes that were
	 * only registered and never activated have not entered their setup/dispose lifecycle.</p>
	 */
	public void clearScenes() {
		disposeScene(currentSceneIndex);
		detachScenes();
		LOGGER.info("All scenes cleared. SceneManager reset.");
	}

	/** Resets registrations without disposing a scene transferred to another manager. */
	void detachScenes() {
		scenes.clear();
		currentSceneIndex = -1;
		currentSceneActive = false;
	}

	void setLifecycleListener(LifecycleListener lifecycleListener) {
		if (this.lifecycleListener == lifecycleListener) {
			return;
		}
		if (this.lifecycleListener != null && currentSceneActive) {
			disposeScene(currentSceneIndex);
		}
		this.lifecycleListener = lifecycleListener;
		if (lifecycleListener != null && !currentSceneActive) {
			Scene current = getCurrentScene();
			if (current != null) {
				setupScene(current);
			}
		}
	}

	private void setupScene(Scene scene) {
		if (lifecycleListener == null) {
			return;
		}
		try {
			lifecycleListener.beforeSetup(scene);
			scene.setupScene();
			currentSceneActive = true;
		} catch (RuntimeException | LinkageError error) {
			currentSceneActive = false;
			disposeActivation(scene);
			throw error;
		}
	}

	boolean isCurrentSceneActive() {
		return currentSceneActive;
	}

	private int indexOfIdentity(Scene target) {
		for (int index = 0; index < scenes.size(); index++) {
			if (scenes.get(index) == target) {
				return index;
			}
		}
		return -1;
	}
}
