# SolarSystem — Scientific Data and Media Provenance

This notice records the provenance and licensing boundary of the scientific data
and media distributed with the `SolarSystem` example. It supplements the
repository `LICENSE` and `THIRD_PARTY.md`.

A machine-readable integrity manifest is available in
[`ASSET_PROVENANCE.json`](ASSET_PROVENANCE.json).

## 1. Project-authored example code

The Processing/Java code, local example structure and project-authored material
are covered by the ziviDomeLive 2.0 project license, **Apache-2.0**, except where
this notice states otherwise.

## 2. Astronomical dataset — `data/solar2.json`

Maintainer provenance declaration: the astronomical values used by this example
were compiled from **NASA/JPL scientific-data sources**. The primary reference
authorities recorded for this curated snapshot are:

- JPL Solar System Dynamics: <https://ssd.jpl.nasa.gov/>
- JPL Planetary Physical Parameters: <https://ssd.jpl.nasa.gov/planets/phys_par.html>
- JPL Horizons: <https://ssd.jpl.nasa.gov/horizons/>

`solar2.json` is a project-maintained snapshot for a creative/educational
simulation. It is not represented as an authoritative JPL ephemeris product and
must not be used for mission navigation or precision ephemerides. Scientific
users should consult and cite the current primary JPL source appropriate to
their analysis.

Licensing boundary: to the extent that the local **selection, structure and
curation** are copyrightable project-authored material, they follow
Apache-2.0. The underlying astronomical facts and source data are recorded as
NASA/JPL provenance and are **not claimed as ziviDomeLive-authored Apache-2.0
content**.

Integrity for the audited 2.0 snapshot:

- SHA-256: `caf7ef7350a5a6e8c51ca706dc67539d03c19e2e6de1f44eeaa9b227a761faf3`
- audited Git blob SHA-1: `83dda44cedf7c415cbc8084700241b954d7b01e1`

## 3. Planetary and space textures — Solar System Scope / INOVE

The texture files listed below are from the **Solar System Scope — Solar
Textures** pack.

- Creator/attribution: **Solar System Scope**
- Invented and developed by: **INOVE**
- Source collection: <https://www.solarsystemscope.com/textures/>
- License: **Creative Commons Attribution 4.0 International (CC BY 4.0)**
- License URL: <https://creativecommons.org/licenses/by/4.0/>

The official Solar System Scope source page states that the pack is based on
**NASA elevation and imagery data**. It also states that colors and shades are
tuned using spacecraft and Hubble imagery, that unmapped gaps can contain
fictional terrain, and that the Earth textures merge geodata, space imagery and
NASA Blue Marble material.

This distinction is intentional:

**NASA is an upstream data/imagery source for these derived textures. The
texture files themselves are attributed and licensed by Solar System Scope /
INOVE under CC BY 4.0. ziviDomeLive does not relabel them as direct NASA/JPL
images and does not relicense them under Apache-2.0.**

NASA media-use guidance: <https://www.nasa.gov/nasa-brand-center/images-and-media/>

### Audited texture inventory

| Local file | Solar System Scope upstream file | Verification | SHA-256 | Local filename note |
|---|---|---|---|---|
| `2k_earth.jpg` | `2k_earth_daymap.jpg` | byte-match | `767ee1dc6eb3802699bfccf6f264880f8acd0b80de3191cd24984fe279b07b7c` | Renamed locally from the upstream Earth Day Map filename. |
| `2k_earth_clouds.jpg` | `2k_earth_clouds.jpg` | byte-match | `fffd7f68d41b37274822150e54a6ef605af1d3ec35624d9f628c3b896bfa42ed` | Stored under the upstream filename. |
| `2k_earth_nightmap.jpg` | `2k_earth_nightmap.jpg` | byte-match | `c16fd1bc096ab91a5c5265c6ff9847c43f489f2e2ee790ccdbcbd03251cf3a5a` | Stored under the upstream filename. |
| `2k_earth_normal_map.tif` | `2k_earth_normal_map.tif` | official-endpoint | `f518ce2646ca935dbc17e316041de4fea7a5da0ec441e4eb22e711eabd843ba2` | Stored under the upstream filename. |
| `2k_earth_specular_map.tif` | `2k_earth_specular_map.tif` | official-endpoint | `6b90ecfce248591a1ecc9a3e49acca1a7059b6828877e718302ed9a6b4471bd7` | Stored under the upstream filename. |
| `2k_jupiter.jpg` | `2k_jupiter.jpg` | byte-match | `b0f04d005350252636b0e3396fc592548cbd9e9126b269d32d5c6abd4b0e4f2b` | Stored under the upstream filename. |
| `2k_mars.jpg` | `2k_mars.jpg` | byte-match | `2d187f3e77a98eaa8cea5f4cc722f633c122ef170b9e94ace6b5fb6cbc3f8e01` | Stored under the upstream filename. |
| `2k_mercury.jpg` | `2k_mercury.jpg` | byte-match | `5a5c80607f643496bac9a631e71957def35ed788895f18b678ac849c2b38e48a` | Stored under the upstream filename. |
| `2k_moon.jpg` | `2k_moon.jpg` | byte-match | `2764ba6535ea0481a062846ee033cc7a909dae05b31a8fd13f3e98f3a7fd92bd` | Stored under the upstream filename. |
| `2k_neptune.jpg` | `2k_neptune.jpg` | byte-match | `cb42ea82709741d28b0af44d8b283cbc6dbd0c521a7f0e1e1e010ade00977df6` | Stored under the upstream filename. |
| `2k_saturn.jpg` | `2k_saturn.jpg` | byte-match | `54a900ca9bf7ab62e70f862852759abdf342e6d6436a95a2fe9ebdb6bcd3bbac` | Stored under the upstream filename. |
| `2k_saturn_ring_alpha.png` | `2k_saturn_ring_alpha.png` | byte-match | `4b0644b2f3ef259fd0bfaedd9dc52f9e7d9738f65f50363e8c4dd19f650d7334` | Stored under the upstream filename. |
| `2k_sun.jpg` | `2k_sun.jpg` | byte-match | `ff0f076ba65e03b5ab518451bc96699325be38e3ccbdd5869ee1c00f3a0c8816` | Stored under the upstream filename. |
| `2k_uranus.jpg` | `2k_uranus.jpg` | byte-match | `d15239d46f82d3ea13d2b260b5b29b2a382f42f2916dae0694d0387b1204a09d` | Stored under the upstream filename. |
| `2k_venus.jpg` | `2k_venus_surface.jpg` | byte-match | `dbe5db1c794a8ab4cbf7dd6bf193540c400fc833ce1e6cc399318aa68026278b` | Renamed locally from the upstream Venus Surface filename. |
| `2k_venus_atmosphere.jpg` | `2k_venus_atmosphere.jpg` | official-endpoint | `225012ad4911730605c4e189ca2a3bf674fce50cc48aab4102b936b47d6991ac` | Stored under the upstream filename. |
| `8k_stars.jpg` | `8k_stars.jpg` | official-endpoint | `80c2259d7d020f47e7d16d38a75a3ad932f32ab8b99b55be03f23e64bc970f62` | Stored under the upstream filename. |
| `8k_stars_milky_way.jpg` | `8k_stars_milky_way.jpg` | byte-match | `1fd005ddd6d53364cc5106e0121b83fd3bca236b1503f6b51f5501d9d51eafaf` | Stored under the upstream filename. |
| `8k_sun.jpg` | `8k_sun.jpg` | official-endpoint | `f22b1cfb306ddce72a7e3b628668a0175b745038ce6268557cb2f7f1bdf98b9d` | Stored under the upstream filename. |

Verification labels:

- `byte-match`: the local Git blob was cross-checked against a public
  distribution that identifies the corresponding asset as Solar System Scope
  material.
- `official-endpoint`: the exact upstream asset name/download endpoint exists on
  the current official Solar System Scope texture page; the audited
  ziviDomeLive Git blob is pinned by `ASSET_PROVENANCE.json` so a future
  replacement cannot silently inherit this attribution.

The local renames of the Earth day map and Venus surface map are recorded above
so attribution is not obscured by project lookup filenames.

## 4. Milky Way panorama — ESO/S. Brunier

`data/textures/eso0932a.jpg` is the ESO public image:

- Title: **The Milky Way panorama**
- ESO image ID: `eso0932a`
- Credit: **ESO/S. Brunier**
- Source: <https://www.eso.org/public/images/eso0932a/>
- ESO usage policy: <https://www.eso.org/public/outreach/copyright/>
- License: **CC BY 4.0**
- License URL: <https://creativecommons.org/licenses/by/4.0/>
- Public image dimensions: 6000 × 3000 px
- Local SHA-256: `60400c92c54b7c1bd12299c69e83b16e5b6256e7dabacc478c021758ecd28179`
- audited Git blob SHA-1: `05fc323684c193d0e1838f098a851bdb74c929ed`

The credit **ESO/S. Brunier** must be preserved in full. The ziviDomeLive
Apache-2.0 license does not apply to this image.

## 5. Unresolved historical asset

The historical `data/textures/background.jpg` file was removed from the local 2.0 working tree by this migration because its provenance could not be established and the current `SolarSystem` code does not require it.

Audited historical Git blob SHA-1:
`bd1c221b79f8391bf8c4665cc2175ef9343a8450`

No NASA, JPL, Solar System Scope, ESO or other credit is assigned to this file
without evidence.

## 6. Non-endorsement and marks

Use of NASA/JPL source data, NASA upstream imagery, Solar System Scope textures
or ESO material does not imply endorsement of ziviDomeLive by NASA, JPL,
Caltech, Solar System Scope, INOVE, ESO or Serge Brunier.

NASA, JPL, Caltech, Solar System Scope, INOVE and ESO names, identifiers, logos
and marks are not licensed by the ziviDomeLive Apache-2.0 license.

## 7. Reproducibility

The example keeps versioned local copies so the visual and numerical behavior of
a ziviDomeLive release can be reproduced. The integrity hashes in
`ASSET_PROVENANCE.json` identify the exact audited files. Upstream data,
policies and media collections can evolve independently; scientific use should
always consult the current primary sources.
