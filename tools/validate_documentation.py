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
from pathlib import Path

EXPECTED_VERSION = "2.0.0"
EXPECTED_DOI = "10.5281/zenodo.15671506"
RENDER_MODES = {"FULL", "STANDARD", "DOMEMASTER", "EQUIRECTANGULAR", "SKYBOX"}
VIEW_TYPES = {"STANDARD", "DOMEMASTER", "EQUIRECTANGULAR", "SKYBOX"}
EXAMPLES = {"EmptyProject", "Basic", "SphereParticle", "InfiniteBackground", "FulldomePBR", "SolarSystem", "CalibrationTool", "BenchmarkTool"}
CUSTOM_TASKS = {"qualificationTests", "buildReleaseArtifacts", "verifyReleaseTag"}
STANDARD_GRADLE_TASKS = {"clean", "test", "build", "javadoc", "check", "assemble"}

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
    for rel in ['README.md','mkdocs.yml','library.properties','CITATION.cff','.zenodo.json','examples','src/main/java','docs/en','docs/pt']:
        if not (root/rel).exists(): c.error(f"required path missing: {rel}")

def parse_props(path):
    out={}
    for line in read(path).splitlines():
        if not line or line.lstrip().startswith('#') or '=' not in line: continue
        k,v=line.split('=',1);out[k.strip()]=v.strip()
    return out

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
    if any(tested_values.values()):
        c.error('tested.* metadata contains a qualification claim; record release evidence before publishing it')
    if re.search(r'(?i)(^|[,\s])VR([,\s]|$)|(^|[,\s])XR([,\s]|$)',props.get('library.keywords','')):
        c.error('library keywords contain generic VR/XR claim')
    source_props=parse_props(root/'release.properties')
    if not source_props:
        c.error('release.properties missing or empty: it is the source for generated library.properties')
    else:
        for key in ('name','version','authors','url','categories','sentence','paragraph','minRevision','maxRevision','library.copyright','library.dependencies','library.keywords'):
            if source_props.get(key) != props.get(key):
                c.error(f'release.properties and library.properties differ for {key}')
        source_tested = {key: source_props.get(key, '').strip() for key in ('tested.platform','tested.processingVersion')}
        if any(source_tested.values()):
            c.error('release.properties contains tested.* qualification claims without recorded release evidence')
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
    manifest=root/'docs/img/PLACEHOLDERS.txt'
    if manifest.exists(): c.error('docs/img/PLACEHOLDERS.txt exists: replace provisional images and remove the manifest before tagging')

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

def check_package(path: Path,c):
    if not path.exists(): c.error(f'package not found: {path}'); return
    with zipfile.ZipFile(path) as z: names=[n.replace('\\','/') for n in z.namelist() if not n.endswith('/')]
    def any_suffix(s): return any(n.endswith(s) for n in names)
    for suffix in ['/library.properties','/reference/index.html']:
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
    txt=read(p)
    if re.search(r'\bUNVERIFIED\b|\bPENDING\b|\[ \]',txt): c.error('release evidence still contains UNVERIFIED/PENDING/unchecked gates')

def check_editorial_system(root,c):
    mk=read(root/'mkdocs.yml')
    css=root/'docs/assets/css/material-editorial.css'
    if not css.exists(): c.error('Material editorial stylesheet missing')
    for token in ['pymdownx.emoji','material-editorial.css']:
        if token not in mk: c.error(f'Material editorial configuration missing: {token}')
    if 'navigation.instant' in mk:
        c.error('navigation.instant is incompatible with the configured mkdocs-static-i18n contextual language switcher')
    for rel in ['docs/en/index.md','docs/pt/index.md','docs/en/getting-started/quickstart.md','docs/pt/getting-started/quickstart.md','docs/en/api/artist-api-map.md','docs/pt/api/artist-api-map.md']:
        txt=read(root/rel)
        if not txt.startswith('---\n'): c.error(f'editorial front matter missing: {rel}')
    # Core surfaces should exercise the semantic components intentionally.
    en_home=read(root/'docs/en/index.md')
    if 'grid cards' not in en_home or 'md-button' not in en_home: c.error('homepage is missing Material card/button composition')
    outputs=read(root/'docs/en/usage/external-integration.md')
    if '=== "NDI"' not in outputs: c.error('external outputs page is missing backend content tabs')

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--root',default='.')
    ap.add_argument('--package',type=Path)
    ap.add_argument('--release-dir',type=Path)
    ap.add_argument('--release-evidence',action='store_true')
    ap.add_argument('--skip-links',action='store_true')
    args=ap.parse_args(); root=Path(args.root).resolve(); c=Check()
    check_required(root,c);check_metadata(root,c);check_claims(root,c);check_api(root,c);check_examples(root,c);check_language_parity(root,c);check_editorial_system(root,c)
    if not args.skip_links: check_local_links(root,c)
    if args.package: check_package(args.package,c)
    if args.release_dir: check_release_dir(args.release_dir,c)
    if args.release_evidence: check_evidence(root,c)
    return c.report()

if __name__=='__main__': raise SystemExit(main())
