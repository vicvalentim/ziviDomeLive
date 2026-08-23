# Material editorial design system

This file defines the presentation grammar used by the ziviDomeLive MkDocs site. It changes presentation, not API truth.

## Principles

1. **Artist-first readability.** A first-use page must remain understandable without OpenGL internals.
2. **Semantic components.** Cards map choices; admonitions mark contracts/risks; tabs compare real alternatives; collapsible details contain optional internals.
3. **No decorative overload.** Icons and visual components must improve scanning or hierarchy.
4. **EN/PT parity.** Components may be translated, but facts and information hierarchy remain equivalent.
5. **Theme-native behavior.** Custom CSS uses Material variables and preserves light/dark modes, responsive layout and reduced-motion preferences.

## Page grammar

- page front matter: `title`, optional `description`, `icon`, and status where relevant;
- H1 + lead: one concise statement of purpose;
- cards: overview/decision map;
- H2/H3: procedural or conceptual hierarchy;
- `!!! info`: public contract;
- `!!! tip`: recommended workflow;
- `!!! warning`: correctness/release risk;
- `??? abstract "Under the hood"`: optional implementation context;
- tabs: alternatives such as NDI/Syphon/Spout, never whole chapters;
- figures: one explanatory visual with useful alt text;
- code: executable/current API only.

## API statuses

- `stable` — stable API;
- `advanced` — Advanced Stable API;
- `experimental` — experimental public API;
- `callback` — Processing/ControlP5 callback surface;
- `internal` — internal implementation or migration boundary;
- `qualification` — evidence/tooling rather than ordinary creative workflow.

The Java modifier `public` alone never determines the documentation audience.

Architecture/lifecycle/routing diagrams use Material's native Mermaid fence so
they remain versionable, responsive and theme-aware. Raster assets are reserved
for identity/artwork and real captured evidence.
