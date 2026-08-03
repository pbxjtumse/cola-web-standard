from pathlib import Path
from collections import defaultdict
from lxml import etree
import os, sys

ROOT=Path(__file__).resolve().parents[1]
NS='http://maven.apache.org/POM/4.0.0'; Q=lambda n:f'{{{NS}}}{n}'
errors=[]; warnings=[]

def parse(p): return etree.parse(str(p)).getroot()
def txt(e,n,default=None):
 c=e.find(Q(n)); return c.text.strip() if c is not None and c.text else default

def gav(p):
 r=parse(p); par=r.find(Q('parent'))
 g=txt(r,'groupId') or (txt(par,'groupId') if par is not None else None)
 v=txt(r,'version') or (txt(par,'version') if par is not None else None)
 return g,txt(r,'artifactId'),v

poms=list(ROOT.rglob('pom.xml'))
index=defaultdict(list)
for p in poms: index[gav(p)[1]].append(p)

# XML, modules and parents
for p in poms:
 try: r=parse(p)
 except Exception as e: errors.append(f'XML parse failed {p.relative_to(ROOT)}: {e}'); continue
 for m in r.findall(f'{Q("modules")}/{Q("module")}'):
  target=p.parent/(m.text.strip())/'pom.xml'
  if not target.exists(): errors.append(f'Missing module {p.relative_to(ROOT)} -> {m.text}')
 par=r.find(Q('parent'))
 if par is not None:
  rel=txt(par,'relativePath','../pom.xml')
  target=(p.parent/rel).resolve()
  if not target.exists(): errors.append(f'Missing parent path {p.relative_to(ROOT)} -> {rel}')
  else:
   pg,pa,pv=gav(target)
   if (pg,pa,pv)!=(txt(par,'groupId'),txt(par,'artifactId'),txt(par,'version')):
    errors.append(f'Parent coordinate mismatch {p.relative_to(ROOT)} -> {rel}: {(pg,pa,pv)} != {(txt(par,"groupId"),txt(par,"artifactId"),txt(par,"version"))}')

 # duplicate dependencies
 seen=set()
 deps=r.find(Q('dependencies'))
 if deps is not None:
  for d in deps.findall(Q('dependency')):
   key=(txt(d,'groupId'),txt(d,'artifactId'),txt(d,'type','jar'),txt(d,'classifier'))
   if key in seen: errors.append(f'Duplicate dependency {p.relative_to(ROOT)} {key}')
   seen.add(key)

# Root boundaries
rr=parse(ROOT/'pom.xml')
if rr.find(Q('dependencies')) is not None: errors.append('Root POM must not declare inherited runtime/test dependencies')
rdm=rr.find(f'{Q("dependencyManagement")}/{Q("dependencies")}')
for d in rdm.findall(Q('dependency')):
 if txt(d,'groupId')=='com.xjtu.iron' and txt(d,'artifactId') not in {'client','domain','app','adapter','infrastructure'}:
  errors.append(f'Root POM still manages component artifact {txt(d,"artifactId")}')
 if txt(d,'groupId')=='com.xjtu.iron' and txt(d,'version') is None:
  errors.append(f'Root internal DM missing version {txt(d,"artifactId")}')

# BOM boundaries
bom=ROOT/'component/component-bom/pom.xml'; br=parse(bom)
if br.find(Q('dependencies')) is not None: errors.append('component-bom must not have normal dependencies')
managed=set()
for d in br.findall(f'{Q("dependencyManagement")}/{Q("dependencies")}/{Q("dependency")}'):
 managed.add(txt(d,'artifactId'))
 if txt(d,'version')!='${project.version}': errors.append(f'BOM invalid version {txt(d,"artifactId")}: {txt(d,"version")}')

# Direct imports and explicit dependency checks
for p in poms:
 r=parse(p); aid=txt(r,'artifactId','')
 if p.parts[-2].endswith('-component') and p.parent.parent.name=='component':
  imports=[txt(d,'artifactId') for d in r.findall(f'{Q("dependencyManagement")}/{Q("dependencies")}/{Q("dependency")}') if txt(d,'scope')=='import']
  if 'component-bom' not in imports: errors.append(f'Component root does not import BOM: {p.relative_to(ROOT)}')

 main='\n'.join(x.read_text(errors='ignore') for x in (p.parent/'src/main/java').rglob('*.java')) if (p.parent/'src/main/java').exists() else ''
 test='\n'.join(x.read_text(errors='ignore') for x in (p.parent/'src/test/java').rglob('*.java')) if (p.parent/'src/test/java').exists() else ''
 deps={(txt(d,'groupId'),txt(d,'artifactId')) for d in r.findall(f'{Q("dependencies")}/{Q("dependency")}')}
 if 'import org.slf4j.' in main and ('org.slf4j','slf4j-api') not in deps: errors.append(f'Missing explicit slf4j: {p.relative_to(ROOT)}')
 if ('import lombok.' in main or 'import lombok.' in test) and ('org.projectlombok','lombok') not in deps: errors.append(f'Missing explicit lombok: {p.relative_to(ROOT)}')
 needs_spring='import org.springframework.test.' in test or 'import org.springframework.boot.test.' in test
 if needs_spring and ('org.springframework.boot','spring-boot-starter-test') not in deps: errors.append(f'Missing Spring test dependency: {p.relative_to(ROOT)}')
 if 'import org.junit.jupiter.' in test and not needs_spring and ('org.junit.jupiter','junit-jupiter') not in deps and ('org.junit.jupiter','junit-jupiter-api') not in deps:
  errors.append(f'Missing JUnit dependency: {p.relative_to(ROOT)}')

 # no explicit internal dependency version except BOM import/BOM entries
 for d in r.findall(f'{Q("dependencies")}/{Q("dependency")}'):
  if txt(d,'groupId')=='com.xjtu.iron' and txt(d,'version') is not None:
   errors.append(f'Explicit internal version {p.relative_to(ROOT)} -> {txt(d,"artifactId")}')

# Every dependency on a component artifact must be covered by component-bom.
artifact_locations={}
for p in poms:
 r=parse(p); artifact_locations[txt(r,'artifactId')]=p
for p in poms:
 r=parse(p)
 for d in r.findall(f'{Q("dependencies")}/{Q("dependency")}'):
  if txt(d,'groupId')!='com.xjtu.iron':
   continue
  aid=txt(d,'artifactId')
  target=artifact_locations.get(aid)
  if target is not None and target.is_relative_to(ROOT/'component') and aid not in managed:
   errors.append(f'Component dependency is not managed by BOM: {p.relative_to(ROOT)} -> {aid}')

# Boot plugin for executable apps
for java in ROOT.rglob('src/main/java/**/*.java'):
 if '@SpringBootApplication' not in java.read_text(errors='ignore'): continue
 mod=java.parent
 while mod != ROOT and not (mod/'pom.xml').exists():
  mod=mod.parent
 p=mod/'pom.xml'; r=parse(p)
 plugins={txt(x,'artifactId') for x in r.findall(f'{Q("build")}/{Q("plugins")}/{Q("plugin")}')}
 if 'spring-boot-maven-plugin' not in plugins: errors.append(f'Missing boot plugin: {p.relative_to(ROOT)}')

# Governance and concurrency known fixes
r=parse(ROOT/'component/governance-component/governance-api/pom.xml')
if r.find(f'.//{Q("source")}') is not None or r.find(f'.//{Q("target")}') is not None: errors.append('governance-api still overrides Java source/target')
for name in ['concurrency-api','concurrency-config','concurrency-core','concurrency-starter','concurrency-demo','concurrency-integrations','concurrency-provider']:
 p=ROOT/'component/concurrency-component'/name/'pom.xml'; r=parse(p)
 if txt(r.find(Q('parent')),'artifactId')!='concurrency-component': errors.append(f'Wrong concurrency parent: {p.relative_to(ROOT)}')

print(f'POM count: {len(poms)}')
print(f'BOM managed artifacts: {len(managed)}')
print(f'Errors: {len(errors)}')
for e in errors: print('ERROR:',e)
print(f'Warnings: {len(warnings)}')
for w in warnings: print('WARN:',w)
sys.exit(1 if errors else 0)
