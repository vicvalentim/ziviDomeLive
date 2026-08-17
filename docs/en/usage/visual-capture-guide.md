---
title: Preview and Output
icon: material/monitor-share
---

# Preview and Output

Preview and output are **independent consumers of rendered views**. Window size, preview policy and external-output resolution solve different problems and should remain independently understandable.

<figure markdown="span">
  ![Preview and output routing](../../img/preview-output-routing.png)
  <figcaption>In FULL, preview and external destinations can request different ViewTypes from the same frame.</figcaption>
</figure>

<div class="grid cards" markdown>

- :material-monitor: **Preview**

    Interactive inspection in the current Processing window. Its resolution follows the preview policy, not the external output target.

- :material-export: **Output**

    Final representation delivered to an enabled destination at the configured output resolution/aspect.

</div>

## Preview

- Standard preview follows the current Processing window dimensions.
- Spherical preview uses the library's automatic square preview policy rather than the output resolution.
- Resizing the window therefore does not redefine external-output resolution.

## Output resolution

The public resize entry point is `resetGraphics(int)`. Documentation and examples must use that implemented API name.

!!! info "Graphics changes are deferred"
    Output-target changes are applied at the render/draw boundary so graphics resources are recreated in the correct Processing/OpenGL context.

Changing output resolution must not silently redefine preview resolution. Domemaster Size% is calibration state and must remain persistent across output-target recreation.

## Independent routing in FULL

| Preview | Output | Why it is useful |
|---|---|---|
| Standard | Domemaster NDI | Work conventionally while feeding a dome pipeline |
| Standard | Equirectangular | Keep an operator view while publishing a 360° representation |
| Domemaster | Another enabled ViewType | Inspect calibration while another route remains active |

A dedicated `RenderMode` temporarily overrides effective representation without deleting the stored `ViewType` selections.

!!! warning "Do not equate window size with output size"
    The Processing window is an interaction/preview surface. External output resolution is a separate contract.
