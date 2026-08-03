#!/usr/bin/env python3
"""检查源码直接使用的常见第三方 API 是否在对应模块 POM 中显式声明。"""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}

RULES = {
    'org.assertj.': ('org.assertj', 'assertj-core', 'test'),
    'org.junit.jupiter.': ('org.junit.jupiter', 'junit-jupiter', 'test'),
    'org.mockito.': ('org.mockito', 'mockito-core', 'test'),
    'org.awaitility.': ('org.awaitility', 'awaitility', 'test'),
    'com.tngtech.archunit.': ('com.tngtech.archunit', 'archunit-junit5', 'test'),
    'org.slf4j.': ('org.slf4j', 'slf4j-api', 'main'),
    'lombok.': ('org.projectlombok', 'lombok', 'main'),
    'org.apache.commons.lang3.': ('org.apache.commons', 'commons-lang3', 'main'),
    'org.apache.commons.collections4.': ('org.apache.commons', 'commons-collections4', 'main'),
    'org.apache.commons.codec.': ('commons-codec', 'commons-codec', 'main'),
    'org.apache.commons.io.': ('commons-io', 'commons-io', 'main'),
    'com.fasterxml.jackson.databind.': ('com.fasterxml.jackson.core', 'jackson-databind', 'main'),
    'com.fasterxml.jackson.annotation.': ('com.fasterxml.jackson.core', 'jackson-annotations', 'main'),
    'com.fasterxml.jackson.core.': ('com.fasterxml.jackson.core', 'jackson-core', 'main'),
}


def module_poms():
    return {path.parent: path for path in ROOT.rglob('pom.xml')}


def nearest_module(file_path: Path, poms):
    current = file_path.parent
    while current != ROOT.parent:
        if current in poms:
            return current
        current = current.parent
    return None


def declared_dependencies(pom: Path):
    project = ET.parse(pom).getroot()
    dependencies = set()
    node = project.find('m:dependencies', NS)
    if node is None:
        return dependencies
    for dependency in node.findall('m:dependency', NS):
        group = dependency.findtext('m:groupId', default='', namespaces=NS)
        artifact = dependency.findtext('m:artifactId', default='', namespaces=NS)
        dependencies.add((group, artifact))
    return dependencies


def imports_of(file_path: Path):
    content = file_path.read_text(encoding='utf-8', errors='ignore')
    return [match.group(1) for match in re.finditer(
        r'^import\s+(?:static\s+)?([\w.]+)', content, re.MULTILINE
    )]


def main():
    poms = module_poms()
    used = {}
    for source in ROOT.rglob('*.java'):
        source_kind = 'test' if '/src/test/' in source.as_posix() else 'main'
        if '/src/main/' not in source.as_posix() and source_kind != 'test':
            continue
        module = nearest_module(source, poms)
        if module is None:
            continue
        for imported in imports_of(source):
            for prefix, dependency in RULES.items():
                group, artifact, allowed_kind = dependency
                if imported.startswith(prefix) and source_kind == allowed_kind:
                    used.setdefault(module, set()).add((group, artifact, source_kind))

    errors = []
    for module, requirements in sorted(used.items()):
        declared = declared_dependencies(poms[module])
        for group, artifact, source_kind in sorted(requirements):
            if (group, artifact) not in declared:
                errors.append(
                    f'{module.relative_to(ROOT)}: {source_kind} 源码使用 {group}:{artifact}，但 POM 未显式声明'
                )

    print(f'Modules checked: {len(poms)}')
    print(f'Missing direct dependencies: {len(errors)}')
    for error in errors:
        print(f'ERROR: {error}')
    return 1 if errors else 0


if __name__ == '__main__':
    sys.exit(main())
