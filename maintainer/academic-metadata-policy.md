# Academic Metadata Policy

## Scope

This policy prevents identifier and citation metadata from leaking between different publication objects.

| Object | Role | Versioning | Identifier policy |
|---|---|---|---|
| ziviDomeLive software | Processing library/product | software SemVer | software DOI only |
| MkDocs | official technical documentation | follows software version | no independent DOI/ISBN unless a later explicit decision creates a separate publication object |
| GitBook | future didactic/editorial publication | editions | future DOI/ISBN only after real registration |
| JOSS paper | future peer-reviewed paper | journal publication | journal DOI only after acceptance/publication |

## Current software metadata

The repository records:

- software version: `2.0.0`;
- software DOI: `10.5281/zenodo.15671506`;
- ORCID: `0000-0002-0282-7947`;
- license: `GPL-2.0-only`.

The DOI must be checked against the external Zenodo record before the release tag. Until that verification is recorded, preserve the repository identifier rather than inventing a replacement.

## Consistency surface

Before tagging, compare at least:

- `CITATION.cff`;
- `.zenodo.json`;
- README citation section;
- MkDocs Citation page;
- release metadata/version;
- actual release/tag name.

## Prohibited metadata

Never publish placeholder identifiers such as fake Zenodo suffixes or fabricated ISBNs. Do not reuse a future paper DOI as a software DOI, or a software DOI as an ISBN/documentation identifier.
