#!/usr/bin/env bash
set -euo pipefail
python3 scripts/validate-poms.py
mvn -U -DskipTests compile
mvn -U clean verify
