# Third-Party Notices

The ziviDomeLive 2.0 project license applies only to project-authored material
except where a file or notice states otherwise. Third-party software, scientific
data, images, textures and calibration assets retain their own provenance,
licenses and usage conditions.

## Devolay Community Fork

- Component: `io.github.vicvalentim:devolay:2.2.0-vic.2`
- Purpose: Java integration with the NDI SDK
- Original author: Walker Knapp
- Fork maintainer: Victor Valentim
- Source: <https://github.com/vicvalentim/devolay>
- License: Apache License 2.0
- License text: [`licenses/Apache-2.0.txt`](licenses/Apache-2.0.txt)

The installable ziviDomeLive package includes this Devolay artifact. The
proprietary NDI Runtime is not bundled and remains governed by its own current
license and distribution terms.

## Syphon for Processing macOS universal compatibility asset

The ziviDomeLive `v2.0.0` release publishes this separate compatibility asset:

- Asset: `Syphon-for-Processing-4.0-macOS-universal-community.zip`
- SHA-256: `59996d8e984c8662e1b964768861e28faa04ab9495daa641a0e14a5a1bf35995`
- Purpose: temporary universal macOS native payload for Syphon for Processing 4.0
- Native architectures: `arm64` and `x86_64`
- Upstream Processing wrapper: <https://github.com/Syphon/Processing>
- Upstream Java/JNI bridge: <https://github.com/Syphon/Java>
- Upstream native framework: <https://github.com/Syphon/Syphon-Framework>
- License: Syphon Framework and Implementations License (BSD-style)
- Status: community compatibility build; not an official Syphon Project release

The compatibility ZIP is a separate release asset and is not bundled inside
`ziviDomeLive.zip`/`.pdex`. Its included `LICENSE.txt` reproduces the upstream
Syphon redistribution notice and disclaimer. The ziviDomeLive Apache-2.0
license does not relicense Syphon.

## Processing dependencies

ControlP5, Syphon and Spout are external Processing dependencies. Their local
bootstrap JARs are used to compile the project and are not bundled in the
installable ziviDomeLive Processing package. Users normally obtain them through
their respective Processing installation channels. The Apple Silicon Syphon
compatibility ZIP documented above is an explicitly separate release asset.
Each dependency retains its upstream license.

## SolarSystem scientific data and media

The `SolarSystem` example deliberately separates four provenance layers:

1. project-authored example code and local curation — Apache-2.0;
2. underlying astronomical values — NASA/JPL scientific-data provenance;
3. planetary/space textures — Solar System Scope / INOVE, CC BY 4.0, based on
   NASA elevation and imagery data according to the upstream texture page;
4. `eso0932a.jpg` — ESO/S. Brunier, CC BY 4.0.

Detailed file-by-file credits, source links, integrity hashes and the unresolved
historical-background decision are recorded in:

- [`examples/Advanced/SolarSystem/THIRD_PARTY.md`](examples/Advanced/SolarSystem/THIRD_PARTY.md)
- [`examples/Advanced/SolarSystem/ASSET_PROVENANCE.json`](examples/Advanced/SolarSystem/ASSET_PROVENANCE.json)

The ziviDomeLive Apache-2.0 license does not relicense the Solar System Scope or
ESO media assets and does not claim authorship of underlying NASA/JPL scientific
facts/data.

## Paul Bourke Fulldome calibration assets

The Paul Bourke calibration images retain their original redistribution
conditions. Their authorship, source URLs, integrity hashes and terms are
documented in
[`examples/Tools/CalibrationTool/THIRD_PARTY.md`](examples/Tools/CalibrationTool/THIRD_PARTY.md).

No third-party material is relicensed merely because it is distributed beside
Apache-2.0 project code.
