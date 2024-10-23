## ziviDomeLive Library

ziviDomeLive is a Processing library that facilitates the creation of immersive visual experiences for fulldome projections. It allows for interactive and sound-reactive scene manipulation, making it ideal for fulldome environments and other immersive installations.

Features:
- Support for dome projection techniques (e.g., equirectangular and domemaster shaders).
- Interaction with 3D scenes in real time.
- Sound-reactive visual effects to enhance audience experience.
- Customizable scene managers for control over dome content.

## Installation:

Install with the Contribution Manager:
- You can install ziviDomeLive using the Processing Contribution Manager:
   1. Open Processing and navigate to Sketch → Import Library... → Add Library....
   2. Search for ziviDomeLive and click Install.

- If the library is not available in the Contribution Manager, follow the manual installation steps below.

Manual Install:
1. Download ziviDomeLive from https://github.com/vicvalentim/zividomelive.
2. Unzip the downloaded file.
3. Copy the folder into the libraries directory of your Processing sketchbook. You can find the sketchbook location in Processing under Preferences.
4. The folder structure should be as follows:

   Processing
   libraries
   ziviDomeLive
   examples
   library
   ziviDomeLive.jar
   reference
   src

5. Restart Processing to use the library.

## Usage:
To use ziviDomeLive in your Processing sketch, import the library at the beginning of your code:

import ziviDomeLive.*;

ziviDomeLive dome;

void setup() {
size(800, 800, P3D);
dome = new ziviDomeLive(this);
dome.setup();
}

void draw() {

dome.draw();

}

## Examples:
Check out the examples folder for a variety of sample sketches that demonstrate how to use ziviDomeLive to create immersive, sound-reactive visual experiences.

## Development:
If you want to contribute to the development of ziviDomeLive:
1. Fork the repository on GitHub: https://github.com/vicvalentim/zividomelive
2. Clone your fork to your local machine.
3. Make changes and test them locally.
4. Push your changes and create a pull request.

## Author:
Developed by Victor Valentim.

For more information, visit Victor Valentim's GitHub page: https://github.com/vicvalentim.

## License:
ziviDomeLive is licensed under the Apache License, Version 2.0. See the LICENSE file for more details.
