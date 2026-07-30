package com.victorvalentim.zividomelive;

import com.victorvalentim.zividomelive.support.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages scenes and allows switching between them dynamically.
 */
public class SceneManager {

	private final List<Scene> scenes; // List of registered scenes
	private int currentSceneIndex = -1; // Index of the current scene (-1 when no scene is active)
	private static final Logger LOGGER = LogManager.getLogger();
	/**
	 * Constructs a SceneManager.
	 */
	public SceneManager() {
		this.scenes = new ArrayList<>();
	}

	/**
	 * Registers a new scene.
	 *
	 * @param scene the scene to register
	 */
	public void registerScene(Scene scene) {
		if (scene == null) {
			LOGGER.severe("Cannot register a null scene.");
			return;
		}

		if (scenes.contains(scene)) {
			LOGGER.warning("Scene already registered: " + scene.getName());
			return;
		}

		scenes.add(scene);
		if (currentSceneIndex == -1) {
			// Automatically set the first scene as current if none is active
			currentSceneIndex = 0;
			scene.setupScene();
			LOGGER.info("First scene registered and set as current: " + scene.getName());
		} else {
			LOGGER.info("Scene registered: " + scene.getName());
		}
	}

	/**
	 * Activates a scene by instance; registers it first if it is not in the manager yet.
	 *
	 * @param scene scene to activate
	 */
	public void activateScene(Scene scene) {
		if (scene == null) {
			LOGGER.severe("Cannot activate a null scene.");
			return;
		}

		int index = scenes.indexOf(scene);
		if (index == -1) {
			scenes.add(scene);
			index = scenes.size() - 1;
			LOGGER.info("Scene auto-registered during activation: " + scene.getName());
		}

		currentSceneIndex = index;
		Scene activeScene = scenes.get(currentSceneIndex);
		activeScene.setupScene();
		LOGGER.info("Scene activated: " + activeScene.getName());
	}

	/**
	 * Returns true when the manager already contains the provided scene instance.
	 *
	 * @param scene scene to check
	 * @return true if scene is already managed
	 */
	public boolean containsScene(Scene scene) {
		return scenes.contains(scene);
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
	 * Switches to the next scene in the list.
	 */
	public void nextScene() {
		if (scenes.isEmpty()) {
			LOGGER.severe("No scenes to switch to.");
			return;
		}

		int previousIndex = currentSceneIndex;
		currentSceneIndex = (currentSceneIndex + 1) % scenes.size();

		if (previousIndex != currentSceneIndex) {
			Scene newScene = getCurrentScene();
			newScene.setupScene();
			LOGGER.info("Switched to the next scene: " + newScene.getName());
		}
	}


	/**
	 * Switches to the previous scene in the list.
	 */
	public void previousScene() {
		if (scenes.isEmpty()) {
			LOGGER.severe("No scenes to switch to.");
			return;
		}

		int previousIndex = currentSceneIndex;
		currentSceneIndex = (currentSceneIndex - 1 + scenes.size()) % scenes.size();
		if (previousIndex != currentSceneIndex) {
			Scene newScene = getCurrentScene();
			newScene.setupScene();
			LOGGER.info("Switched to the previous scene: " + newScene.getName());
		} else {
			LOGGER.info("No change in scene: still on " + getCurrentScene().getName());
		}
	}

	/**
	 * Returns the current scene.
	 *
	 * @return the current scene or null if no scene is active
	 */
	public Scene getCurrentScene() {
		if (scenes.isEmpty() || currentSceneIndex == -1) {
			LOGGER.severe("No current scene is set.");
			return null;
		}
		return scenes.get(currentSceneIndex);
	}

	/**
	 * Sets the scene to the specified index, if valid.
	 *
	 * @param index the index of the scene to switch to
	 */
	public void setCurrentSceneIndex(int index) {
		if (index < 0 || index >= scenes.size()) {
			LOGGER.severe("Invalid scene index: " + index);
			return;
		}

		currentSceneIndex = index;
		Scene newScene = getCurrentScene();
		if (newScene != null) {
			newScene.setupScene();
			LOGGER.info("Scene set to index " + index + ": " + newScene.getName());
		}
	}

	/**
	 * Clears all registered scenes and resets the manager.
	 */
	public void clearScenes() {
		scenes.clear();
		currentSceneIndex = -1;
		LOGGER.info("All scenes cleared. SceneManager reset.");
	}
}
