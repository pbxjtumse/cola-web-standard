#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "[1/4] 校验 POM 聚合、父链和 component-bom"
python3 scripts/validate-poms.py

echo "[2/4] 校验源码直接使用的第三方 API 是否在模块 POM 中声明"
python3 scripts/check-source-dependencies.py

echo "[3/4] 编译所有生产代码"
mvn -U -DskipTests compile

echo "[4/4] 执行全部测试并打包"
mvn -U clean verify
