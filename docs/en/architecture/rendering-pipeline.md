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

`FrameViews` is the engine-facing public container/boundary for final frame representations consumed by publishers/outputs. Users normally select those representations through `ViewType` instead of constructing pipeline internals.
