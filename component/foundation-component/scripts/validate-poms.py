#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
errors = []
for pom in root.rglob('pom.xml'):
    try:
        ET.parse(pom)
    except Exception as exc:
        errors.append(f'{pom}: {exc}')
if errors:
    print('\n'.join(errors))
    raise SystemExit(1)
print(f'POM XML OK: {len(list(root.rglob("pom.xml")))} files')
