#!/usr/bin/env python3
"""校验 retry-component 的包路径、一期模块边界和源码洁净度。"""

from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
ALLOWED_API_PACKAGES = {"backoff", "event", "exception", "execution", "policy"}
FORBIDDEN_ARTIFACT_NAMES = {"target", "__MACOSX"}
FORBIDDEN_SUFFIXES = {".iml", ".class"}

violations: list[str] = []

for path in ROOT.rglob("*"):
    relative = path.relative_to(ROOT)
    if any(part in FORBIDDEN_ARTIFACT_NAMES for part in relative.parts):
        violations.append(f"{relative}: 发布源码中不应包含构建或系统目录")
    if path.is_file() and path.suffix in FORBIDDEN_SUFFIXES:
        violations.append(f"{relative}: 发布源码中不应包含 IDE 或编译产物")

for java_file in ROOT.rglob("*.java"):
    relative = java_file.relative_to(ROOT)
    text = java_file.read_text(encoding="utf-8")
    match = re.search(r"^package\s+([\w.]+);", text, re.MULTILINE)
    if match is None:
        violations.append(f"{relative}: 缺少有效 package 声明")
        continue
    package_name = match.group(1)
    source_marker = Path("src") / ("test" if "/src/test/" in f"/{relative.as_posix()}/" else "main") / "java"
    parts = relative.parts
    try:
        marker_index = parts.index("java")
    except ValueError:
        violations.append(f"{relative}: Java 文件不在标准 Maven 源码目录")
        continue
    expected_package = ".".join(parts[marker_index + 1:-1])
    if package_name != expected_package:
        violations.append(
            f"{relative}: package={package_name} 与目录={expected_package} 不一致"
        )
    if package_name == "com.xjtu.iron.retry.api":
        violations.append(f"{relative}: retry-api 根包不应直接堆放公开类型")
    if relative.parts[0] == "retry-api" and "com.xjtu.iron.foundation" in text:
        violations.append(f"{relative}: retry-api 不应依赖 Foundation 实现模块")
    if package_name.startswith("com.xjtu.iron.retry.api."):
        first_segment = package_name.split(".")[5]
        if first_segment not in ALLOWED_API_PACKAGES:
            violations.append(
                f"{relative}: retry-api 一级包 {first_segment} 不在允许集合中"
            )
    if text.startswith("//package "):
        violations.append(f"{relative}: 测试或源码文件被整体注释")

if violations:
    print("Package layout verification failed:")
    print("\n".join(sorted(set(violations))))
    sys.exit(1)

print("Package layout verification passed")
