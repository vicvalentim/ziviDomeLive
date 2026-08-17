---
title: "Threading"
icon: material/source-branch
---
# Threading


Processing drawing and OpenGL access are constrained by the renderer's active graphics context.

## Render thread

Scene drawing and renderer-owned GL operations execute on the Processing/render thread. Background code must not make arbitrary GL calls merely because it has a reference to an object associated with rendering.

## Background work

Scene Services can support background tasks and a mechanism for returning render-context work to the correct thread. Follow the exact current API/Javadocs rather than sharing mutable graphics objects across threads.

## NDI boundary

Network sending can use worker-thread work after frame data has crossed the GPU/CPU boundary. GL capture itself remains context-bound; a worker must not assume ownership of the Processing GL context.
