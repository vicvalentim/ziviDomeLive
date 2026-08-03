# System Requirements for ziviDomeLive

The **ziviDomeLive** library is designed to powerfully support high-performance, immersive visual environments, including fulldome projections, virtual reality installations, and interactive media displays. Given its real-time 3D rendering capabilities, **ziviDomeLive** requires certain system specifications and dependencies to ensure optimal performance. Below are the detailed system, hardware, and software requirements.

---

## Processing Environment

To fully leverage **ziviDomeLive** capabilities, use a compatible version of Processing with an appropriate environment configuration. These settings will enable the library to maximize graphical performance, compatibility, and user experience:

- **Processing Version**: Processing 4.x or newer. This library is optimized for the latest Processing releases, as they incorporate advanced 3D rendering updates and better compatibility with current hardware. Earlier versions may not fully support some features.

- **Graphics Renderer**: Set your Processing sketches to use `P3D` for 3D rendering. The `P3D` renderer is essential for handling complex 3D scenes, especially those involving immersive fisheye or equirectangular projections required in fulldome environments. Without `P3D`, 3D functionalities may be limited or unavailable.

- **Supported Operating Systems**:
    - **macOS** (10.14 Mojave or later)
    - **Windows** (Windows 10 or later)
    - **Linux** (Ubuntu 18.04 LTS or later, Debian-based recommended)

  Keeping your OS updated can significantly improve performance and compatibility with external integrations, particularly **Syphon** on macOS and **Spout** on Windows.

---

## Hardware Recommendations

For optimal real-time rendering, especially in high-resolution (up to 4K) fulldome or VR applications, **ziviDomeLive** requires robust hardware to ensure a smooth and responsive experience. Below are our hardware recommendations:

- **Dedicated GPU**: Use a modern, dedicated graphics card (e.g., NVIDIA GeForce RTX or AMD Radeon RX series). Integrated graphics may struggle with high-resolution projections and shader-heavy scenes.
- **Memory (RAM)**: 8GB is the minimum, but 16GB or more is recommended for handling larger scenes or simultaneous integrations.
- **Multi-core Processor**: A multi-core CPU enhances performance for real-time rendering, especially when handling complex visual calculations and multiple libraries.

---

## Notes for Apple Silicon Users (M1, M2, and Later)

If you're using a **macOS system with Apple Silicon (M series)** processors, please consider the following:

- **Syphon Compatibility**: Syphon currently does not support the native ARM version of Processing on Apple Silicon. Therefore, if you need Syphon, run the **Intel version of Processing** using **Rosetta 2**. This allows you to retain full Syphon functionality.

- **ARM Version Limitations**: While the ARM (Apple Silicon native) version of Processing works for most standard functionalities, it may lack full support for certain real-time integrations. If Syphon is non-essential, using the ARM version is acceptable.

---

By following these requirements and recommendations, you’ll ensure **ziviDomeLive** performs at its best, delivering high-quality visuals and seamless interactivity for your immersive media projects.
