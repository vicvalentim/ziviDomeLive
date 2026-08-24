# Material editorial QA — 2.0.0

This checklist is applied after DOC-09 and DOC-10 and before documentation freeze. It validates presentation without redefining software behavior.

## Global

- [ ] light, dark and system palettes remain legible;
- [ ] desktop and mobile navigation remain usable;
- [ ] cards collapse cleanly on narrow screens;
- [ ] keyboard focus remains visible on buttons, tabs and links;
- [ ] reduced-motion preference is respected;
- [ ] code blocks remain copyable and readable;
- [ ] diagrams include meaningful alt text;
- [ ] no placeholder image ships in the tag.

## Editorial semantics

- [ ] cards are used for choices/maps, not ordinary paragraphs;
- [ ] admonitions identify contracts, recommendations or risks;
- [ ] tabs contain genuine alternatives;
- [ ] optional internals use collapsible details when useful;
- [ ] artist pages do not require engine vocabulary;
- [ ] API status labels match the classification audit;
- [ ] qualification pages cannot be mistaken for tutorials.

## EN/PT

- [ ] equivalent facts;
- [ ] equivalent component hierarchy;
- [ ] equivalent images/alt-text intent;
- [ ] no English-only callout introduced accidentally into PT.

## Build gate

Run after applying the commits to the full repository:

```bash
python3 tools/validate_documentation.py --root .
python3 -m mkdocs build --strict
```

The final build result must be recorded in `maintainer/release-evidence.md`; this file does not claim that the commands were executed in the artifact-generation environment.
