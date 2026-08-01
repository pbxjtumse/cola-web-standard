#!/usr/bin/env python3
"""校验 Java 注释是否遵循“业务语义优先、拒绝机械行尾注释”的规范。"""

from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
MECHANICAL_PHRASES = (
    "声明当前类型所属的 Java 包",
    "引入当前实现需要使用的类型",
    "执行当前赋值、调用或声明语句",
    "返回当前方法计算或构造得到的结果",
    "继续完成当前表达式或实现步骤",
    "结束当前语句、类型或控制代码块",
    "保存经过校验或规范化后的成员字段值",
    "声明并保存当前对象内部状态",
    "判断当前条件并进入对应处理分支",
)


def find_line_comment(line: str) -> int:
    """返回字符串和字符字面量之外的 // 位置。"""
    in_string = False
    in_char = False
    escaped = False
    index = 0
    while index < len(line) - 1:
        char = line[index]
        next_char = line[index + 1]
        if escaped:
            escaped = False
            index += 1
            continue
        if char == "\\" and (in_string or in_char):
            escaped = True
            index += 1
            continue
        if char == '"' and not in_char:
            in_string = not in_string
            index += 1
            continue
        if char == "'" and not in_string:
            in_char = not in_char
            index += 1
            continue
        if char == "/" and next_char == "/" and not in_string and not in_char:
            return index
        index += 1
    return -1


def collect_type_names(text: str) -> set[str]:
    """收集文件中的顶层和嵌套类型名称，用于识别构造器。"""
    return set(re.findall(r"\b(?:class|interface|enum)\s+([A-Za-z_$][\w$]*)", text))


def next_declaration(lines: list[str], start: int) -> tuple[int, str]:
    """读取 Javadoc 后的声明头，并跳过注解。"""
    index = start
    while index < len(lines):
        stripped = lines[index].strip()
        if not stripped:
            index += 1
            continue
        if stripped.startswith("@"):
            balance = stripped.count("(") - stripped.count(")")
            index += 1
            while balance > 0 and index < len(lines):
                balance += lines[index].count("(") - lines[index].count(")")
                index += 1
            continue
        break

    if index >= len(lines):
        return index, ""

    header: list[str] = []
    cursor = index
    parentheses = 0
    while cursor < len(lines) and len(header) < 20:
        part = lines[cursor].strip()
        header.append(part)
        parentheses += part.count("(") - part.count(")")
        if parentheses <= 0 and ("{" in part or part.endswith(";")):
            break
        cursor += 1
    return index, " ".join(header)


def method_name(header: str) -> str | None:
    """从方法或构造器声明头中提取名称。"""
    if "(" not in header:
        return None
    before_parenthesis = header.split("(", 1)[0]
    match = re.search(r"([A-Za-z_$][\w$]*)\s*$", before_parenthesis)
    return match.group(1) if match else None


def method_body(lines: list[str], declaration_index: int) -> str:
    """读取短方法体，用于识别 Builder 简单赋值方法。"""
    parts: list[str] = []
    depth = 0
    started = False
    for cursor in range(declaration_index, min(len(lines), declaration_index + 40)):
        line = lines[cursor]
        parts.append(line.strip())
        for char in line:
            if char == "{":
                depth += 1
                started = True
            elif char == "}":
                depth -= 1
        if started and depth == 0:
            break
        if not started and ";" in line:
            break
    return " ".join(parts)


def is_fluent_setter(header: str, body: str) -> bool:
    """判断方法是否只是 Builder 字段赋值并返回 this。"""
    name = method_name(header)
    if not name or name == "build":
        return False
    normalized = re.sub(r"\s+", " ", body)
    return bool(
        re.search(
            r"\{\s*this\.[A-Za-z_$][\w$]*\s*=.*?;\s*return\s+this\s*;\s*\}",
            normalized,
        )
    )


def trivial_javadoc_target(header: str, body: str, type_names: set[str]) -> bool:
    """判断 Javadoc 是否位于不需要说明的样板成员之前。"""
    name = method_name(header)
    if not name:
        return False
    if name in type_names:
        return True
    if re.match(r"^(?:get|set|is)[A-Z_]", name):
        return True
    return is_fluent_setter(header, body)


violations: list[str] = []
java_files = sorted(ROOT.rglob("*.java"))

for java_file in java_files:
    relative = java_file.relative_to(ROOT)
    lines = java_file.read_text(encoding="utf-8").splitlines()
    type_names = collect_type_names("\n".join(lines))

    for line_number, line in enumerate(lines, start=1):
        comment_index = find_line_comment(line)
        if comment_index >= 0 and line[:comment_index].strip():
            violations.append(f"{relative}:{line_number}: 禁止在代码后追加行尾注释")
        if line.lstrip().startswith(("package ", "import ")) and comment_index >= 0:
            violations.append(f"{relative}:{line_number}: package/import 行不应带注释")
        for phrase in MECHANICAL_PHRASES:
            if phrase in line:
                violations.append(
                    f"{relative}:{line_number}: 发现机械注释短语：{phrase}"
                )

    index = 0
    while index < len(lines):
        if not lines[index].lstrip().startswith("/**"):
            index += 1
            continue
        block_start = index
        while index < len(lines) and "*/" not in lines[index]:
            index += 1
        index += 1
        declaration_index, header = next_declaration(lines, index)
        body = method_body(lines, declaration_index) if declaration_index < len(lines) else ""
        if trivial_javadoc_target(header, body, type_names):
            violations.append(
                f"{relative}:{block_start + 1}: 构造器、Getter/Setter 或简单 Builder 方法不应保留 Javadoc"
            )

if violations:
    print("Java comment style verification failed:")
    print("\n".join(violations))
    sys.exit(1)

print(f"Java comment style verification passed: {len(java_files)} files")
