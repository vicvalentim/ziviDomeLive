# ziviDomeLive 2.0.0 Documentation Freeze Report

This report accompanies the DOC-01…DOC-08 handoff. It records the intended final documentary state but does **not** fabricate command results that have not been executed in a complete local checkout.

## 1. Initial state

- audited branch: `release/2.0.0`;
- audit baseline observed during preparation: `bc560dbb8dab1d39b55824ae71ae70179512ec6d`;
- local working-tree dirty/clean state: not observable from the public snapshot used to prepare this handoff.

Before applying/tagging, record the actual local branch/HEAD/status again because the branch may have moved after this handoff was generated.

## 2. Historical knowledge recovered

| Concept | Classification | Final placement |
|---|---|---|
| Scene `update()` vs `sceneRender()` | PRESERVE + EXPAND | Quickstart, Scene Javadoc, API/Developer docs |
| library-owned draw frame | PRESERVE | Quickstart, Scene Javadoc, lifecycle docs |
| `FULL` independent routes | PRESERVE | User Guide + API |
| dedicated modes preserve stored routes | PRESERVE | User Guide + API |
| Pitch/Yaw/Roll shared spherical orientation | PRESERVE | calibration + architecture |
| Domemaster FOV | PRESERVE | calibration |
| Domemaster Size% physical fit | PRESERVE | calibration |
| preview/output independence | PRESERVE + UPDATE | Preview and Output |
| legacy six-texture spherical topology | HISTORICAL | release/history; not current tutorial |
| native cubemap/samplerCube architecture | MOVE | Developer Guide; optional “under the hood” context |
| NDI worker/buffer details | MOVE | Developer Output Backends |
| benchmark/calibration as beginner material | MOVE | Qualification Tools |

## 3. Information removed from current public identity

- generic `VR`/`XR` keywords where they implied a runtime/headset contract;
- “monoscopic VR” as the product's current defining capability;
- unqualified `tested.platform` and `tested.processingVersion` metadata;
- literal homepage screenshot placeholder prose.

Historical notes may still mention superseded language when clearly labelled as history/migration.

## 4. Information corrected

| Before | After |
|---|---|
| README/MkDocs lead with renderer internals | artist-first purpose, representations, start path; internals moved to Developer Guide |
| public types presented as one undifferentiated group | Artist-facing / Advanced / Experimental / Deprecated / Engine-facing classification |
| external-output artist page mixed with backend internals | artist page focuses on destination/view/enable/state/receiver; internals moved |
| qualification after release tag | qualification/evidence becomes pre-tag gate |
| `tested.*` without release evidence | omitted until qualification justifies a claim |
| MkDocs edit target `main` | documentation edit target `release/2.0.0` during freeze |
| future publication identifiers ambiguous | GitBook DOI/ISBN explicitly null/absent until real registration |

## 5. Documentary architecture

- **README / Processing package:** concise entry point, installation, examples, Javadocs, legal/citation metadata.
- **MkDocs:** official versioned technical manual.
- **Generated Javadocs:** exact callable signature/lifecycle reference.
- **Developer Guide:** rendering architecture and engine internals.
- **Maintainer docs:** publication, qualification, metadata, historical audit and tag gate.
- **Future GitBook:** separate didactic/editorial publication; no dependency from the Processing package/MkDocs.

## 6. API documented

- Artist-facing stable: runtime facade, `Scene`, `SceneManager`, `RenderMode`, `ViewType`, `OutputManager` and normal facade controls.
- Advanced public: Scene Services/time/camera/orientation and direct renderer facilities where public.
- Experimental public: current performance instrumentation.
- Deprecated: compatibility methods explicitly retained by implementation/Javadocs.
- Engine-facing public: `FrameViews`, `CubemapTarget`, `ProcessingGlAdapter` and comparable low-level boundaries.
- Internal: package-private pipeline/policy architecture documented only in Developer Guide.

## 7. Processing compliance

The handoff provides:

- a tightened `library.properties`;
- eight-example catalogue (6 learning + 2 qualification);
- publication/readiness checklists;
- pre-tag automated workflow;
- stable ZIP/TXT/PDEX/package validator rules;
- explicit `reference/index.html`, source, license and installed-package checks.

Actual Contribution Manager publication remains a maintainer action after release evidence is complete.

## 8. Academic metadata

- software DOI retained from repository metadata: `10.5281/zenodo.15671506`;
- ORCID retained: `0000-0002-0282-7947`;
- `CITATION.cff` and `.zenodo.json` rewritten to describe current fulldome/spherical/immersive capabilities without generic VR-runtime claims;
- external DOI-record verification remains a required pre-tag evidence item.

## 9. GitBook preparation

`maintainer/gitbook-publication-plan.md` establishes title/status, audience, proposed parts, snapshot model, relation with MkDocs/Javadocs, Git Sync boundary and null future DOI/ISBN fields. No GitBook chapter set was duplicated into MkDocs.

## 10. EN/PT

All newly introduced technical pages are paired EN/PT. The final validator requires relative Markdown-file parity. Human review remains required for semantic/terminological quality after applying the overlays to the complete repository.

## 11. Validation

Performed on the generated handoff itself:

- ZIP integrity;
- Python validator syntax compilation;
- JSON syntax for `.zenodo.json`;
- YAML syntax for generated `mkdocs.yml` when PyYAML is available in the packaging environment;
- placeholder image readability/dimensions.

Not claimed as executed here because this handoff environment is not a complete checkout:

- Gradle clean/test/build;
- `qualificationTests`;
- full MkDocs strict build against every original repository page;
- GPU/receiver/benchmark/package-installation qualification.

Those are intentionally represented as release evidence gates, not fictional PASS results.

## 12. Release blockers

At handoff time the expected blockers are:

1. apply DOC-01…DOC-08 to the current `release/2.0.0` checkout and resolve any concurrent branch changes;
2. replace six `docs/img/` provisional images and delete `docs/img/PLACEHOLDERS.txt`;
3. execute/record all `maintainer/release-evidence.md` gates;
4. verify the external Zenodo DOI record;
5. decide and record actual tested platform/backend claims, leaving unqualified combinations unclaimed;
6. run final strict build/package inspection from the complete checkout.

## 13. Files changed

The authoritative per-stage file lists are the contents of the eight DOC ZIPs. Apply them in numeric order. Later stages intentionally supersede `mkdocs.yml` and selected workflows from earlier stages.

## 14. Commit hashes

This handoff does not create Git commits. Record the real hash after each manual commit:

| Stage | Suggested commit subject | Hash |
|---|---|---|
| DOC-01 | `docs: establish documentation contract and historical audit` | to be recorded |
| DOC-02 | `docs: rebuild artist-first user guide` | to be recorded |
| DOC-03 | `docs(api): classify public API and strengthen Scene Javadocs` | to be recorded |
| DOC-04 | `docs(dev): add advanced and developer architecture guides` | to be recorded |
| DOC-05 | `docs(release): align Processing publication and pre-tag qualification` | to be recorded |
| DOC-06 | `docs(metadata): align citation metadata and plan GitBook publication` | to be recorded |
| DOC-07 | `docs(ci): synchronize EN/PT and add documentation validation` | to be recorded |
| DOC-08 | `docs: freeze 2.0.0 documentation and release evidence gate` | to be recorded |

## Material editorial layer

DOC-09 through DOC-11 add a Material-for-MkDocs presentation layer without changing API or renderer behavior. Final evidence still requires `mkdocs build --strict` on the complete repository and replacement of all provisional images.
