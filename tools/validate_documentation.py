#!/usr/bin/env python3
"""Small release-documentation validator for ziviDomeLive 2.0.0.

Uses only the Python standard library. It validates textual/documentary invariants and,
optionally, the generated Processing release package. It does not pretend to replace
GPU, receiver or installed-package runtime qualification.
"""
from __future__ import annotations
import argparse
import json
import re
import sys
import zipfile
import xml.etree.ElementTree as ElementTree
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

EXPECTED_VERSION = "2.0.0"
EXPECTED_DOI = "10.5281/zenodo.15671506"
RENDER_MODES = {"FULL", "STANDARD", "DOMEMASTER", "EQUIRECTANGULAR", "SKYBOX"}
VIEW_TYPES = {"STANDARD", "DOMEMASTER", "EQUIRECTANGULAR", "SKYBOX"}
EXAMPLES = {"EmptyProject", "Basic", "SphereParticle", "InfiniteBackground", "FulldomePBR", "SolarSystem", "CalibrationTool", "BenchmarkTool"}
CUSTOM_TASKS = {
    "qualificationTests", "buildReleaseArtifacts", "attachJavadocsToSite",
    "verifyReleaseTag",
}
STANDARD_GRADLE_TASKS = {"clean", "test", "build", "javadoc", "check", "assemble"}
API_LEVELS = {
    "Stable": {
        "ziviDomeLive", "StandardOutputAspectMode", "Scene", "SceneManager",
        "RenderMode", "ViewType", "LogMode",
    },
    "Advanced Stable": {
        "SceneServices", "FrameClock", "SimulationTimeline", "SceneTaskGroup",
        "SceneAssets", "SceneActionMap", "SceneCameraService",
        "SceneEnvironmentService", "ScenePorts", "SceneInputPort",
        "SceneOutputPort", "OutputManager", "OutputType", "OutputState",
        "Quaternion", "SphericalOrientation", "OrbitCamera",
    },
    "Experimental": {
        "PerformanceMode", "PerformanceMetric", "PerformanceSnapshot",
        "MetricStatistics", "GraphicsCapabilities", "GpuTimerPolicy",
        "GpuTimerBackend", "GpuTimerArchitecture",
    },
}
REMOVED_PLACEHOLDER_IMAGES = {
    "architecture-domains.png", "external-outputs.png",
    "preview-output-routing.png", "render-modes-overview.png",
    "spherical-calibration.png",
}

class Check:
    def __init__(self): self.errors=[]; self.warnings=[]
    def error(self,msg): self.errors.append(msg)
    def warn(self,msg): self.warnings.append(msg)
    def report(self):
        for m in self.warnings: print(f"WARNING: {m}")
        for m in self.errors: print(f"ERROR: {m}")
        print(f"documentation validation: {len(self.errors)} error(s), {len(self.warnings)} warning(s)")
        return 1 if self.errors else 0

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


class ExportedReferenceParser(HTMLParser):
    """Collect browser-visible local references from generated HTML."""

    REFERENCE_ATTRIBUTES = {"action", "href", "poster", "src"}

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.references = []

    def handle_starttag(self, _tag, attrs):
        self._collect(attrs)

    def handle_startendtag(self, _tag, attrs):
        self._collect(attrs)

    def _collect(self, attrs):
        for name, value in attrs:
            if not value:
                continue
            if name in self.REFERENCE_ATTRIBUTES:
                self.references.append((name, value.strip()))
            elif name == "srcset" and not value.lstrip().startswith("data:"):
                for candidate in value.split(","):
                    url = candidate.strip().split(maxsplit=1)[0]
                    if url:
                        self.references.append((name, url))

def public_text_files(root: Path):
    roots=[root/'README.md',root/'library.properties',root/'CITATION.cff',root/'.zenodo.json']
    for p in roots:
        if p.exists(): yield p
    for lang in ('en','pt'):
        base=root/'docs'/lang
        if not base.exists(): continue
        for p in base.rglob('*.md'):
            parts=set(p.relative_to(base).parts)
            if 'release-notes' in parts or p.name in {'roadmap.md'}: continue
            yield p

def check_required(root,c):
    for rel in [
        'README.md', 'CHANGELOG.md', 'THIRD_PARTY.md', 'mkdocs.yml',
        'requirements-docs.txt', 'library.properties', 'CITATION.cff',
        '.zenodo.json', 'examples', 'src/main/java', 'docs/en', 'docs/pt',
        'docs/en/research-software.md', 'docs/pt/research-software.md',
        'docs/en/release-notes/2.0.0.md', 'docs/pt/release-notes/2.0.0.md',
        'docs/en/tags.md', 'docs/pt/tags.md',
        'docs/img/hero-overview.svg', 'docs/img/hero-overview.png',
    ]:
        if not (root/rel).exists(): c.error(f"required path missing: {rel}")

def normalize_property_value(value: str) -> str:
    """Normalize the escaping emitted by java.util.Properties.store().

    Gradle writes library.properties through java.util.Properties, which escapes
    ':' in URLs (for example an escaped colon after 'https'). release.properties is author-edited
    and normally keeps the literal colon. They are semantically identical and must
    compare equal. Keep normalization deliberately narrow so the validator does not
    reinterpret arbitrary backslash sequences.
    """
    return value.replace('\\:', ':')

def parse_props(path):
    out={}
    for line in read(path).splitlines():
        if not line or line.lstrip().startswith('#') or '=' not in line: continue
        k,v=line.split('=',1)
        out[k.strip()]=normalize_property_value(v.strip())
    return out

def release_evidence_complete(root: Path) -> bool:
    p=root/'maintainer/release-evidence.md'
    if not p.exists(): return False
    return re.search(r'\b(?:UNVERIFIED|PENDING)\b|\[ \]', read(p)) is None

def check_metadata(root,c):
    props=parse_props(root/'library.properties')
    if props.get('prettyVersion') != EXPECTED_VERSION: c.error(f"library.properties prettyVersion != {EXPECTED_VERSION}")
    if not props.get('version','').isdigit(): c.error('library.properties version must be an integer release counter')
    try:
        minrev=int(props.get('minRevision',''))
        if minrev < 1285: c.error('minRevision predates the declared Processing 4 baseline; verify Processing revision mapping')
    except ValueError: c.error('minRevision must be an integer')
    tested_values = {
        key: props.get(key, '').strip()
        for key in ('tested.platform', 'tested.processingVersion')
    }
    if any(tested_values.values()) and not release_evidence_complete(root):
        c.error('tested.* metadata contains a qualification claim while release evidence is incomplete')
    if re.search(r'(?i)(^|[,\s])VR([,\s]|$)|(^|[,\s])XR([,\s]|$)',props.get('library.keywords','')):
        c.error('library keywords contain generic VR/XR claim')
    source_props=parse_props(root/'release.properties')
    if not source_props:
        c.error('release.properties missing or empty: it is the source for generated library.properties')
    else:
        for key in ('name','version','authors','url','categories','sentence','paragraph','minRevision','maxRevision','tested.platform','tested.processingVersion','library.copyright','library.dependencies','library.keywords'):
            if source_props.get(key) != props.get(key):
                c.error(f'release.properties and library.properties differ for {key}')
        source_tested = {key: source_props.get(key, '').strip() for key in ('tested.platform','tested.processingVersion')}
        if any(source_tested.values()) and not release_evidence_complete(root):
            c.error('release.properties contains tested.* qualification claims while release evidence is incomplete')
        if re.search(r'(?i)(^|[,\s])VR([,\s]|$)|(^|[,\s])XR([,\s]|$)', source_props.get('library.keywords','')):
            c.error('release.properties library keywords contain generic VR/XR claim')
    cff=read(root/'CITATION.cff')
    m=re.search(r'(?m)^version:\s*["\']?([^"\'\s]+)',cff)
    if not m or m.group(1)!=EXPECTED_VERSION: c.error('CITATION.cff version mismatch')
    m=re.search(r'(?m)^doi:\s*["\']?([^"\'\s]+)',cff)
    if not m or m.group(1)!=EXPECTED_DOI: c.error('CITATION.cff software DOI mismatch')
    try: zen=json.loads(read(root/'.zenodo.json'))
    except Exception as e:
        c.error(f'.zenodo.json is invalid JSON: {e}'); zen={}
    if zen.get('version') != EXPECTED_VERSION: c.error('.zenodo.json version mismatch')
    zen_claim_text=' '.join([str(zen.get('description','')), *map(str, zen.get('keywords',[]))])
    if re.search(r'(?i)(^|[,\s])VR([,\s]|$)|(^|[,\s])XR([,\s]|$)', zen_claim_text):
        c.error('.zenodo.json contains generic VR/XR product metadata outside the 2.0 public contract')
    all_public='\n'.join(read(p) for p in public_text_files(root))
    if EXPECTED_DOI not in all_public: c.warn('software DOI not found in current public documentation text')
    if re.search(r'10\.5281/zenodo\.(?:X+|0{4,}|<[^>]+>)',all_public,re.I): c.error('fake/placeholder Zenodo DOI found')
    if re.search(r'ISBN\s+(?:978[-\s]?X|<[^>]+>|X{3,})',all_public,re.I): c.error('fake/placeholder ISBN found')

def check_claims(root,c):
    banned=[r'(?i)monoscopic\s+VR',r'(?i)mono\s+VR',r'(?i)VR\s+framework',r'(?i)stereo\s+VR\s+engine']
    for p in public_text_files(root):
        txt=read(p)
        for pat in banned:
            if re.search(pat,txt): c.error(f'unsupported generic VR claim in {p.relative_to(root)}')
        if 'setOutputResolution(' in txt: c.error(f'nonexistent API name setOutputResolution documented in {p.relative_to(root)}')
        if 'SPHERICAL_MIRROR' in txt: c.error(f'non-release RenderMode/ViewType documented in {p.relative_to(root)}')
        if 'zivito.github.io' in txt: c.error(f'stale zivito.github.io link in {p.relative_to(root)}')
        if 'toggleOutput("' in txt: c.error(f'legacy string output toggle documented in {p.relative_to(root)}')
        if 'zd-image-placeholder' in txt: c.error(f'provisional image block remains in {p.relative_to(root)}')
        if re.search(r'(?i)image placeholder|placeholder de imagem', txt):
            c.error(f'placeholder image prose remains in {p.relative_to(root)}')
    manifest=root/'docs/img/PLACEHOLDERS.txt'
    if manifest.exists(): c.error('docs/img/PLACEHOLDERS.txt exists: replace provisional images and remove the manifest before tagging')
    capture_plan=root/'docs/img/IMAGE-CAPTURE-PLAN.txt'
    if capture_plan.exists(): c.error('docs/img/IMAGE-CAPTURE-PLAN.txt exists: provisional capture work is not release evidence')
    for name in sorted(REMOVED_PLACEHOLDER_IMAGES):
        if (root/'docs/img'/name).exists(): c.error(f'obsolete raster placeholder remains: docs/img/{name}')

def check_processing_homepage(root,c):
    text=read(root/'README.md')
    headings = [
        '## Overview', '## Statement of need', '## Requirements',
        '## Installation', '## Examples', '## Documentation',
        '## Citation', '## License',
    ]
    for heading in headings:
        if heading not in text: c.error(f'Processing homepage section missing: {heading}')
    required_tokens = [
        'Processing 4', 'Java 17', 'pixelDensity(1)', 'P3D',
        '2026-08-23', 'library.keywords', 'ziviDomeLive.zip',
        'ziviDomeLive.txt', 'ziviDomeLive.pdex', 'reference/index.html',
    ]
    for token in required_tokens:
        if token not in text: c.error(f'Processing homepage information missing: {token}')
    for example in sorted(EXAMPLES):
        if example not in text: c.error(f'Processing homepage does not list example/tool: {example}')
    if not all(token in text for token in ('tested.platform', 'tested.processingVersion', 'qualification')):
        c.error('Processing homepage does not distinguish tested metadata from pending qualification')
    if not re.search(r'(?is)2\.0\.0.*untagged|untagged.*2\.0\.0', text):
        c.error('Processing homepage does not disclose that 2.0.0 is currently untagged')
    if not re.search(r'(?is)(?:latest published stable|published stable).*1\.5\.0', text):
        c.error('Processing homepage does not identify the latest published stable release')

def check_api_levels(root,c):
    surfaces = {
        'README.md': read(root/'README.md'),
        'docs/en/api/overview.md': read(root/'docs/en/api/overview.md'),
    }
    for rel, text in surfaces.items():
        for level, names in API_LEVELS.items():
            if level not in text: c.error(f'{rel} does not label the {level} API level')
            for name in sorted(names):
                if not re.search(r'`[^`]*\b'+re.escape(name)+r'\b[^`]*`', text):
                    c.error(f'{rel} omits {level} type `{name}`')
    readme=surfaces['README.md']
    for label in ('Processing callbacks', 'Internal'):
        if label not in readme: c.error(f'README API boundary missing: {label}')
    pt=read(root/'docs/pt/api/overview.md')
    for label in ('Stable', 'Advanced Stable', 'Experimental', 'callbacks Processing', 'Internal'):
        if label not in pt: c.error(f'Portuguese API boundary missing: {label}')
    scene=read(root/'src/main/java/com/victorvalentim/zividomelive/Scene.java')
    if 'controlEvent(' in scene: c.error('Scene reintroduces the removed ControlP5 controlEvent callback')

def check_research_readiness(root,c):
    requirements = {
        'docs/en/research-software.md': [
            'Statement of need', 'State of the field', 'Research impact',
            'Evidence matrix', 'AI-assisted work', 'does not claim submission',
            '```mermaid',
        ],
        'docs/pt/research-software.md': [
            'Declaração de necessidade', 'Estado da área', 'Impacto de pesquisa',
            'Matriz de evidências', 'Trabalho assistido por IA',
            'Não alega submissão', '```mermaid',
        ],
    }
    for rel, tokens in requirements.items():
        text=read(root/rel)
        for token in tokens:
            if token not in text: c.error(f'research-readiness page {rel} is missing: {token}')
        if not re.search(r'(?i)incomplete|incomplet', text):
            c.error(f'research-readiness page {rel} does not mark incomplete evidence')

def check_release_documents(root,c):
    changelog=read(root/'CHANGELOG.md')
    for token in [
        '## [2.0.0] - Unreleased', '### Public API freeze',
        '### Breaking changes', '### Added — Scene lifecycle and services',
        '### Performance and allocation work',
        '### Documentation and research-software readiness',
        '### Validation', '### Release gates still open',
        '### Not included in 2.0.0', '## [1.5.0]', '## [1.4.0]',
    ]:
        if token not in changelog: c.error(f'CHANGELOG 2.0/history detail missing: {token}')
    for rel in ('docs/en/release-notes/2.0.0.md', 'docs/pt/release-notes/2.0.0.md'):
        text=read(root/rel)
        if len(text.splitlines()) < 180: c.error(f'release notes are not sufficiently detailed: {rel}')
        release_status = 'Unreleased' if rel.startswith('docs/en/') else 'Não lançada'
        for token in ('2.0.0', '```mermaid', 'SceneServices', 'OutputType', release_status):
            if token not in text: c.error(f'release notes {rel} are missing: {token}')

def enum_constants(src, enum_name):
    txt=read(src)
    m=re.search(r'public\s+enum\s+'+re.escape(enum_name)+r'\s*\{(.*?)(?:;|\})',txt,re.S)
    if not m: return None
    head=re.sub(r'/\*.*?\*/|//.*?$','',m.group(1),flags=re.S|re.M)
    return {x.strip() for x in head.split(',') if re.fullmatch(r'[A-Z][A-Z0-9_]*',x.strip())}

def check_api(root,c):
    base=root/'src/main/java/com/victorvalentim/zividomelive'
    rm=enum_constants(base/'RenderMode.java','RenderMode')
    vt=enum_constants(base/'ViewType.java','ViewType')
    if rm is not None and rm != RENDER_MODES: c.error(f'RenderMode source differs from documentation contract: {sorted(rm)}')
    if vt is not None and vt != VIEW_TYPES: c.error(f'ViewType source differs from documentation contract: {sorted(vt)}')
    scene=read(base/'Scene.java')
    for token in ['default void update()', 'void sceneRender(PGraphicsOpenGL pg)']:
        if token not in scene: c.error(f'Scene contract signature missing: {token}')
    build=read(root/'build.gradle.kts')
    for task in CUSTOM_TASKS:
        if task not in build: c.error(f'documented Gradle task not found in build.gradle.kts: {task}')

def check_examples(root,c):
    ex=root/'examples'
    if not ex.exists(): return
    actual={p.name for p in ex.iterdir() if p.is_dir()}
    missing=EXAMPLES-actual
    if missing: c.error('missing required examples/tools: '+', '.join(sorted(missing)))

def check_language_parity(root,c):
    en=root/'docs/en'; pt=root/'docs/pt'
    if not en.exists() or not pt.exists(): return
    enf={p.relative_to(en) for p in en.rglob('*.md')}
    ptf={p.relative_to(pt) for p in pt.rglob('*.md')}
    for rel in sorted(enf-ptf): c.error(f'PT counterpart missing for docs/en/{rel}')
    for rel in sorted(ptf-enf): c.error(f'EN counterpart missing for docs/pt/{rel}')

def check_local_links(root,c):
    link_re=re.compile(r'!?\[[^\]]*\]\(([^)]+)\)')
    for base in [root/'README.md',*(root/'docs/en').rglob('*.md'),*(root/'docs/pt').rglob('*.md')]:
        if not Path(base).exists(): continue
        txt=read(Path(base))
        for raw in link_re.findall(txt):
            target=raw.split('#',1)[0].strip()
            if not target or target.startswith(('http://','https://','mailto:','#')): continue
            if target.startswith('<') and target.endswith('>'): target=target[1:-1]
            p=(Path(base).parent/target).resolve()
            # README paths are root-relative in practice; retry project-root form.
            if not p.exists() and Path(base)==root/'README.md': p=(root/target).resolve()
            if not p.exists(): c.error(f'broken local link in {Path(base).relative_to(root)}: {raw}')


def exported_site_url(root: Path) -> str:
    match = re.search(r'(?m)^site_url:\s*["\']?([^"\'\s]+)', read(root/'mkdocs.yml'))
    return match.group(1) if match else ''


def resolve_exported_reference(
        site: Path,
        source: Path,
        raw_reference: str,
        public_site_url: str):
    """Resolve one generated-site URL to a local exported file when it is site-owned."""
    raw_reference = raw_reference.strip()
    if not raw_reference or raw_reference.startswith(('#', 'data:', 'javascript:')):
        return None, None

    parsed = urlsplit(raw_reference)
    public = urlsplit(public_site_url)
    path = unquote(parsed.path)

    if parsed.scheme or parsed.netloc:
        is_public_site = (
            parsed.scheme in {'http', 'https'}
            and parsed.netloc == public.netloc
        )
        if not is_public_site:
            return None, None
        public_prefix = public.path.rstrip('/') + '/'
        if path == public.path.rstrip('/'):
            path = ''
        elif path.startswith(public_prefix):
            path = path[len(public_prefix):]
        else:
            return None, None
        target = site/path
    elif path.startswith('/'):
        public_prefix = public.path.rstrip('/') + '/'
        if public_prefix != '/' and path.startswith(public_prefix):
            target = site/path[len(public_prefix):]
        else:
            return None, f'root-absolute URL escapes the configured project site: {raw_reference}'
    else:
        target = source.parent/path

    site_root = site.resolve()
    target = target.resolve()
    try:
        target.relative_to(site_root)
    except ValueError:
        return None, f'URL escapes the exported site: {raw_reference}'

    candidates = [target]
    if path.endswith('/') or target.is_dir():
        candidates = [target/'index.html']
    elif not target.suffix:
        candidates.extend([target/'index.html', target.with_suffix('.html')])

    for candidate in candidates:
        if candidate.is_file():
            return candidate, None
    return None, f'missing exported target: {raw_reference}'


def check_exported_site(root: Path, site: Path, c):
    """Crawl generated MkDocs/Javadocs references as they will be published."""
    if not site.exists():
        c.error(f'exported site directory not found: {site}')
        return

    required_routes = [
        'index.html',
        'favicon.ico',
        'api/javadocs/index.html',
        'pt/index.html',
        'pt/api/javadocs/index.html',
        'reference/index.html',
        'sitemap.xml',
        'sitemap.xml.gz',
    ]
    for route in required_routes:
        if not (site/route).is_file():
            c.error(f'exported documentation route missing: {route}')
    if (site/'pt/reference').exists():
        c.error('localized Javadocs duplicate found at site/pt/reference; use canonical site/reference')

    public_site_url = exported_site_url(root)
    if not public_site_url:
        c.error('mkdocs.yml site_url is required to validate exported absolute routes')
        return

    checked = 0
    reported = set()

    def validate(source: Path, attribute: str, reference: str):
        nonlocal checked
        target, error = resolve_exported_reference(
            site, source, reference, public_site_url)
        if target is not None:
            checked += 1
        elif error:
            key = (source, attribute, reference, error)
            if key not in reported:
                reported.add(key)
                relative_source = source.relative_to(site)
                c.error(f'broken exported {attribute} in {relative_source}: {error}')

    for html in sorted(site.rglob('*.html')):
        parser = ExportedReferenceParser()
        try:
            parser.feed(read(html))
        except Exception as error:
            c.error(f'cannot parse exported HTML {html.relative_to(site)}: {error}')
            continue
        for attribute, reference in parser.references:
            validate(html, attribute, reference)

    css_url = re.compile(r'url\(\s*["\']?([^"\')]+)')
    for stylesheet in sorted(site.rglob('*.css')):
        for reference in css_url.findall(read(stylesheet)):
            validate(stylesheet, 'css-url', reference.strip())

    sitemap = site/'sitemap.xml'
    if sitemap.is_file():
        try:
            sitemap_root = ElementTree.parse(sitemap).getroot()
            for element in sitemap_root.iter():
                if element.tag.endswith('loc') and element.text:
                    validate(sitemap, 'sitemap-loc', element.text.strip())
                alternate = element.attrib.get('href')
                if alternate:
                    validate(sitemap, 'sitemap-href', alternate.strip())
        except ElementTree.ParseError as error:
            c.error(f'cannot parse exported sitemap.xml: {error}')

    if checked == 0:
        c.error('exported site route validation did not inspect any local references')
    else:
        print(f'exported site validation: {checked} local reference(s) resolved')

def check_package(path: Path,c):
    if not path.exists(): c.error(f'package not found: {path}'); return
    with zipfile.ZipFile(path) as z: names=[n.replace('\\','/') for n in z.namelist() if not n.endswith('/')]
    def any_suffix(s): return any(n.endswith(s) for n in names)
    for suffix in ['/library.properties','/reference/index.html']:
        if not any_suffix(suffix): c.error(f'package missing {suffix.lstrip("/")}')
    for suffix in ['/README.md', '/CHANGELOG.md', '/CITATION.cff', '/THIRD_PARTY.md']:
        if not any_suffix(suffix): c.error(f'package missing {suffix.lstrip("/")}')
    if not any('/library/' in n for n in names): c.error('package missing library/ content')
    if not any('/src/' in n for n in names): c.error('package missing src/ content')
    for ex in EXAMPLES:
        if not any(f'/examples/{ex}/' in n for n in names): c.error(f'package missing example/tool {ex}')
    if not any_suffix('/LICENSE') and not any_suffix('/LICENSE.txt'): c.error('package missing project license')
    for n in names:
        bad=('/src/test/' in n or '/build/reports/' in n or '/benchmark-results/' in n or n.endswith('/.DS_Store') or n.endswith('.DS_Store'))
        if bad: c.error(f'package contains forbidden generated/test file: {n}')

def check_release_dir(path:Path,c):
    if not path.exists(): c.error(f'release directory not found: {path}'); return
    for name in ['ziviDomeLive.zip','ziviDomeLive.txt','ziviDomeLive.pdex']:
        if not (path/name).exists(): c.error(f'release sibling missing: {name}')

def check_evidence(root,c):
    p=root/'maintainer/release-evidence.md'
    if not p.exists(): c.error('maintainer/release-evidence.md missing'); return
    if not release_evidence_complete(root): c.error('release evidence still contains UNVERIFIED/PENDING/unchecked gates')

def check_editorial_system(root,c):
    mk=read(root/'mkdocs.yml')
    build=read(root/'build.gradle.kts')
    css=root/'docs/assets/css/material-editorial.css'
    if not css.exists(): c.error('Material editorial stylesheet missing')
    for token in [
        'pymdownx.emoji', 'pymdownx.superfences', 'custom_fences',
        'name: mermaid', 'material-editorial.css', 'splash-sphere.js', '- tags:', '- social:',
        'callback:', 'internal:', 'Advanced Stable API', 'Removed 1.x API',
        'Internal Boundary', 'Research Software and JOSS Readiness',
    ]:
        if token not in mk: c.error(f'Material editorial configuration missing: {token}')
    if 'navigation.instant' in mk:
        c.error('navigation.instant is incompatible with the configured mkdocs-static-i18n contextual language switcher')
    if 'exclude("**/_internal/**")' not in build:
        c.error('Javadocs do not exclude the current _internal source taxonomy')
    if 'tasks.register<Sync>("attachJavadocsToSite")' not in build:
        c.error('canonical Javadocs-to-site assembly task is missing')
    if 'exclude("**/internal/**")' in build:
        c.error('stale pre-taxonomy Javadocs exclusion remains in build.gradle.kts')

    publication_workflows = [
        '.github/workflows/automated-qualification.yml',
        '.github/workflows/pre-release.yml',
        '.github/workflows/deploy_website.yml',
        '.github/workflows/pr_preview.yml',
    ]
    for rel in publication_workflows:
        workflow = read(root/rel)
        for token in (
                'python3 -m mkdocs build --strict',
                './gradlew attachJavadocsToSite --console=plain',
                '--site-dir site'):
            if token not in workflow:
                c.error(f'documentation publication gate missing from {rel}: {token}')
        if 'cp -R build/docs/javadoc' in workflow:
            c.error(f'ad-hoc Javadocs copy remains in {rel}; use attachJavadocsToSite')

    en_javadocs=read(root/'docs/en/api/javadocs.md')
    pt_javadocs=read(root/'docs/pt/api/javadocs.md')
    if 'href="../../reference/"' not in en_javadocs:
        c.error('English Javadocs landing page does not target canonical site/reference')
    if 'href="../../../reference/"' not in pt_javadocs:
        c.error('Portuguese Javadocs landing page does not target canonical site/reference')
    if 'site/pt/reference' in en_javadocs or 'site/pt/reference' in pt_javadocs:
        c.error('Javadocs guide still documents a duplicated localized reference tree')
    for rel in ['docs/en/index.md','docs/pt/index.md','docs/en/getting-started/quickstart.md','docs/pt/getting-started/quickstart.md','docs/en/api/artist-api-map.md','docs/pt/api/artist-api-map.md']:
        txt=read(root/rel)
        if not txt.startswith('---\n'): c.error(f'editorial front matter missing: {rel}')
    # Core surfaces should exercise the semantic components intentionally.
    en_home=read(root/'docs/en/index.md')
    if 'grid cards' not in en_home or 'md-button' not in en_home: c.error('homepage is missing Material card/button composition')
    pt_home=read(root/'docs/pt/index.md')
    splash_script=read(root/'docs/assets/js/splash-sphere.js')
    for rel,home in [('docs/en/index.md',en_home),('docs/pt/index.md',pt_home)]:
        for token in ('data-zd-splash', 'data-zd-splash-canvas'):
            if token not in home: c.error(f'homepage splash animation hook missing from {rel}: {token}')
        if 'splash.jpg' in home or 'hero-overview.png' in home:
            c.error(f'homepage still uses an opaque splash/hero image: {rel}')
        if 'width="566" height="480"' not in home:
            c.error(f'homepage splash canvas does not preserve the expanded safe frame: {rel}')
    for token in (
            'const BASE_HEIGHT = 480', 'const PROJECTION_HEIGHT = 358',
            'const SPHERE_RADIUS = 120', 'const ORBIT_RADIUS = 160',
            'const RINGS = 16', 'const SEGMENTS = 32', 'const CUBE_COUNT = 13',
            'const INITIAL_FRAME_TIME = 1800',
            'time * 0.02 * DEG_TO_RAD', 'time * 0.015 * DEG_TO_RAD',
            'time * 0.01 * DEG_TO_RAD', '0.0008 + (Math.random() - 0.5) * 0.0004',
            'drawOrbitingCubes(false)', 'drawOrbitingCubes(true)',
            'IntersectionObserver', 'ResizeObserver',
            'visibilitychange', 'getContext("2d", { alpha: true })',
            '--zd-splash-ring-start', '--zd-splash-ring-end', '--zd-splash-cube'):
        if token not in splash_script: c.error(f'homepage splash fidelity/runtime guard missing: {token}')
    for forbidden in ('drawBackground()', 'fillText("ziviDomeLive"', 'prefers-reduced-motion'):
        if forbidden in splash_script: c.error(f'homepage splash transparency/title/forced-motion contract violated: {forbidden}')
    docs_script=read(root/'docs/assets/js/extra.js')
    for token in ('data-zd-cookie-settings', 'Gerenciar preferências de cookies', 'GitHub repository data'):
        if token not in docs_script: c.error(f'cookie preferences integration missing: {token}')
    for token in ('consent:', 'cookies:', 'github:', '- accept', '- reject', '- manage'):
        if token not in mk: c.error(f'MkDocs cookie consent contract missing: {token}')
    outputs=read(root/'docs/en/usage/external-integration.md')
    if '=== "NDI"' not in outputs: c.error('external outputs page is missing backend content tabs')
    requirements=read(root/'requirements-docs.txt')
    if 'mkdocs-material[imaging]' not in requirements:
        c.error('documentation dependencies do not include Material social-card imaging support')
    for rel in [
        'docs/en/architecture/overview.md', 'docs/pt/architecture/overview.md',
        'docs/en/architecture/testing.md', 'docs/pt/architecture/testing.md',
        'docs/en/usage/basic-usage.md', 'docs/pt/usage/basic-usage.md',
        'docs/en/usage/external-integration.md', 'docs/pt/usage/external-integration.md',
        'docs/en/usage/spherical-calibration.md', 'docs/pt/usage/spherical-calibration.md',
    ]:
        if '```mermaid' not in read(root/rel): c.error(f'MkDocs Mermaid diagram missing: {rel}')
    publication = read(root/'docs/en/qualification/processing-publication.md')
    for token in [
        '## Official-guideline mapping', '### Project homepage',
        '### Package and reference', 'reference/index.html',
        'library.keywords', 'ziviDomeLive.zip', 'ziviDomeLive.txt',
        'ziviDomeLive.pdex', 'CITATION.cff', 'THIRD_PARTY.md',
    ]:
        if token not in publication: c.error(f'Processing publication checklist is missing: {token}')
    testing = read(root/'docs/en/architecture/testing.md')
    for token in [
        '## Evidence levels', '## Automated contract', '## Package installation',
        '## GPU visual and calibration', '## Benchmark', '## Native output',
        '## Research-quality reporting',
    ]:
        if token not in testing: c.error(f'research-quality testing guide is missing: {token}')
    command_guides = [
        'README.md', 'docs/en/contributing.md', 'docs/pt/contributing.md',
        'docs/en/research-software.md', 'docs/pt/research-software.md',
        'docs/en/release-notes/2.0.0.md', 'docs/pt/release-notes/2.0.0.md',
    ]
    for rel in command_guides:
        text=read(root/rel)
        if re.search(r'(?m)^mkdocs\s+(?:serve|build)\b', text):
            c.error(f'Python-ambiguous MkDocs command in {rel}; use python3 -m mkdocs')
    readme=read(root/'README.md')
    for token in ('python3 -m pip install -r requirements-docs.txt', 'python3 -m mkdocs serve'):
        if token not in readme: c.error(f'local MkDocs setup guidance missing: {token}')

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--root',default='.')
    ap.add_argument('--package',type=Path)
    ap.add_argument('--release-dir',type=Path)
    ap.add_argument('--site-dir',type=Path)
    ap.add_argument('--release-evidence',action='store_true')
    ap.add_argument('--skip-links',action='store_true')
    args=ap.parse_args(); root=Path(args.root).resolve(); c=Check()
    check_required(root,c)
    check_metadata(root,c)
    check_claims(root,c)
    check_processing_homepage(root,c)
    check_api(root,c)
    check_api_levels(root,c)
    check_examples(root,c)
    check_language_parity(root,c)
    check_research_readiness(root,c)
    check_release_documents(root,c)
    check_editorial_system(root,c)
    if not args.skip_links: check_local_links(root,c)
    if args.package: check_package(args.package,c)
    if args.release_dir: check_release_dir(args.release_dir,c)
    if args.site_dir:
        site = args.site_dir if args.site_dir.is_absolute() else root/args.site_dir
        check_exported_site(root, site.resolve(), c)
    if args.release_evidence: check_evidence(root,c)
    return c.report()

if __name__=='__main__': raise SystemExit(main())
