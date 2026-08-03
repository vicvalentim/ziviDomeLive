# Dependencies for ziviDomeLive

The **ziviDomeLive** library is architected to support high-performance and interactive visual applications in immersive environments. To fully leverage its capabilities, **ziviDomeLive** requires several additional Processing libraries. These dependencies ensure compatibility and stability across macOS, Windows, and Linux, even though certain external integrations like **Syphon** and **Spout** may function only on specific operating systems.

Installing all dependencies is recommended across all systems, as **ziviDomeLive** is architected with certain compatibility checks and fallback mechanisms that rely on the presence of these libraries.

---

## List of Required Dependencies

### 1. ControlP5

- **Purpose**: The **ControlP5** library is essential for creating custom graphical user interfaces (GUIs) within Processing. It provides a suite of interactive components, such as sliders, buttons, toggles, and knobs, which enable users to dynamically adjust visual parameters. This library enhances **ziviDomeLive** by allowing real-time control over aspects of the immersive environment directly from the Processing sketch.

- **Installation Instructions**:
    - **Option 1**: Install via Processing’s Contribution Manager:
        1. Open Processing.
        2. Go to **Sketch > Import Library > Add Library...**
        3. In the Contribution Manager, search for **ControlP5** and click **Install** to add it to your Processing environment.

    - **Option 2**: Download directly from [ControlP5’s GitHub repository](https://github.com/sojamo/controlp5){:target="_blank"}:
        1. Visit the [ControlP5 GitHub page](https://github.com/sojamo/controlp5){:target="_blank"}.
        2. Click **Code** and select **Download ZIP**.
        3. Extract the downloaded file and place the `ControlP5` folder into the `libraries` directory in Processing (usually located at `Documents/Processing/libraries/`).

- **Usage**: Once installed, ControlP5 can be imported in Processing sketches to add various UI controls. It plays a critical role in facilitating real-time parameter adjustments in **ziviDomeLive** projects.

---

### 2. Syphon (macOS Only)

- **Purpose**: **Syphon** is a macOS-exclusive library that enables real-time frame sharing between Processing and other applications. This feature is particularly beneficial for multimedia artists and developers working in environments where visuals need to be routed to multiple software applications for further manipulation or display. While Syphon is macOS-specific, its presence ensures **ziviDomeLive** operates correctly on macOS, even if not all of its functionality is used.

- **Installation Instructions**:
    1. Visit the [Syphon for Processing GitHub repository](https://github.com/Syphon/Processing){:target="_blank"}.
    2. Click **Code** and choose **Download ZIP**.
    3. Extract the ZIP file and move the `Syphon` folder into the Processing `libraries` directory (`Documents/Processing/libraries/`).

- **Note**: Syphon is only compatible with macOS. However, it is still recommended to include this library in your Processing setup for compatibility purposes, even if your project does not currently use Syphon’s frame-sharing features.

---

### 3. Spout (Windows Only)

- **Purpose**: **Spout** provides similar frame-sharing functionality to Syphon but is tailored specifically for Windows. Spout allows users to share real-time frames between Processing and other Spout-compatible applications on Windows. This library is widely used in multimedia and performance art to seamlessly transfer visuals across different software. Having Spout installed is beneficial even if your current project doesn’t use its features, as it helps maintain compatibility and flexibility within the **ziviDomeLive** architecture.

- **Installation Instructions**:
    1. Access the [Spout for Processing GitHub repository](https://github.com/leadedge/SpoutProcessing){:target="_blank"}.
    2. Click **Code** and select **Download ZIP**.
    3. Unzip the downloaded file and place the `Spout` folder into the `libraries` directory in Processing (`Documents/Processing/libraries/`).

- **Note**: Spout is only available for Windows. Including it in your Processing setup can still be helpful for compatibility and ensures your environment is fully prepared for any **ziviDomeLive** projects that may incorporate frame-sharing in the future.

---

## Verifying Dependency Installation

After installing these dependencies, you can verify the installation by opening Processing and navigating to **Sketch > Import Library**. Check that **ControlP5**, **Syphon** (for macOS), and **Spout** (for Windows) appear in the list. This confirmation ensures the libraries are available and ready to be used with **ziviDomeLive**.

Installing all dependencies, even those specific to certain operating systems, maximizes compatibility and prepares your Processing environment for seamless performance with **ziviDomeLive**.
