# GitBook Publication Plan

Status: **planning only — not part of the ziviDomeLive 2.0.0 technical documentation deliverable**.

## Working title

**ziviDomeLive — User & Developer Guide**

The final editorial title may change before publication.

## Publication object

The GitBook will be a didactic/editorial publication independent from:

1. the Processing library distribution;
2. the versioned MkDocs technical manual;
3. generated Javadocs;
4. any future peer-reviewed paper.

Its purpose is to teach and contextualize creative practice, not to redefine the software API.

## Audience

- artists and creative coders;
- students and educators;
- technical teams working with domes/immersive installations;
- developers interested in deeper project/architecture study.

## Proposed parts

### Part I — Immersive creative coding

- creative coding and spherical representation;
- fulldome/domemaster fundamentals;
- 360°/equirectangular workflows;
- live installation and performance context.

### Part II — Creating with ziviDomeLive

- project workflow;
- scenes and animation;
- camera/orientation;
- calibration practice;
- outputs and installation practice;
- exercises.

### Part III — Advanced practice

- multi-scene works;
- Scene Services;
- assets/time/background work;
- profiling and project organization.

### Part IV — Developer perspective

- architecture concepts;
- Standard/Spherical domains;
- rendering/output lifecycle;
- extending/contributing;
- case studies where editorially appropriate.

## Relationship with MkDocs

MkDocs answers: **How do I install, use, program, configure, diagnose and understand this specific software version?**

The GitBook may explain the same concepts in a longer didactic sequence, but should link back to the MkDocs/Javadocs for current API contracts instead of copying complete technical pages.

## Relationship with Javadocs

Javadocs remain the signature-level authority. The GitBook can demonstrate API usage but must not become a parallel signature reference.

## Snapshot workflow

For a future edition:

1. choose a released ziviDomeLive version/tag as the technical baseline;
2. snapshot only the facts required for that edition;
3. resolve all API references against that release's Javadocs;
4. adapt the material editorially rather than mechanically copying MkDocs pages;
5. record the software version covered by the edition;
6. publish the book from its own source/version history.

## Git Sync / source repository

A future GitBook may use Git Sync. The source repository/location is intentionally **not assigned during the 2.0.0 freeze**. Do not make the software repository depend on a future editorial platform.

## Editions and bibliographic registration

A future edition may be released as web, PDF and/or e-book and may later receive bibliographic registration.

Internal planning metadata:

```yaml
documentation_doi: null
documentation_isbn: null
source_repository: null
edition: null
```

`null` is intentional. Replace it only after a real identifier/repository/edition exists.

## Non-duplication rule

- README: short repository entry point.
- MkDocs: complete technical manual for the software version.
- GitBook: didactic/editorial treatment and context.
- Javadocs: exact callable API.

No complete MkDocs section should be mirrored mechanically into the GitBook as an independently maintained duplicate.
