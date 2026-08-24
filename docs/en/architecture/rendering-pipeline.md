---
title: "Rendering Pipeline"
icon: material/source-branch
---
# Rendering Pipeline


The render pipeline resolves the views needed for the current frame, renders the required domain(s), and provides final views to preview/output consumers.

## Requirement resolution

The internal policy decides whether the frame requires Standard rendering, spherical cubemap capture and one or more spherical projections. Internal policy types are architecture, not artist-facing API.

## Reuse rule

A spherical cubemap should be captured once per frame and reused by all requested spherical projections/consumers. A Standard final view remains independent.

## Final views

An internal immutable-per-frame boundary carries final representations to preview and output producers. Users select those representations through `ViewType`; neither the container nor producer operations are public.

Every final off-screen view begins at RGBA `(0, 0, 0, 0)`. Alpha remains part of the internal
Scene → renderer → final view → preview/output path. Scene backgrounds and visible configured
Environment images are explicit content rather than library fallbacks. The primary Processing
window and external receivers can have their own compositor/alpha limitations; those limitations
do not change the final framebuffer contract.
