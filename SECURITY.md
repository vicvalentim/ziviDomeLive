# Security Policy

## Supported Versions

Security fixes are provided for the current 2.0 release line. Historical 1.x releases are retained
for provenance but no longer receive security updates.

| Version | Supported |
| ------- | --------- |
| 2.0.x   | Yes       |
| 1.x.x   | No        |

## Reporting a Vulnerability

Do not open a public issue when a report contains exploit details, credentials, personal data, or
other sensitive material. Use GitHub's private **Report a vulnerability** option when it is offered
on the repository's Security page. If that option is unavailable, contact the maintainer through
[victorvalentim.com](https://victorvalentim.com/) and identify the message as a ziviDomeLive 2.0
security report.

For non-sensitive hardening suggestions, open a regular
[GitHub issue](https://github.com/vicvalentim/ziviDomeLive/issues).

Include the affected ziviDomeLive and Processing versions, operating system, renderer/output in
use, reproduction steps, impact, and the smallest safe proof of concept. Do not include third-party
credentials or unrelated personal data.

The project aims to acknowledge a private report within 72 hours. Validation, remediation, release,
and disclosure timing depend on severity and on coordination with affected third-party runtimes.
Please allow a fix and supported-user guidance to be prepared before publishing exploit details.

## Scope

Reports may cover the Java/Processing library, bundled shaders, release artifacts, dependency
bootstrap, optional output backends, and the project build or release workflows. Vulnerabilities in
Processing, OpenGL drivers, NDI, Syphon, Spout, ControlP5, or other third-party components should
also be reported to their upstream maintainers; a ziviDomeLive report is still useful when the
library can reduce exposure or document a safe configuration.
