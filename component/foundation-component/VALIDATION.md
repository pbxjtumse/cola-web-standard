# Validation

已在当前沙箱完成的检查：

- `python3 scripts/validate-poms.py`：通过，11 个 POM XML 均可解析；
- `foundation-id`：使用 `javac --release 17` 编译通过；
- `foundation-context`：使用 `javac --release 17` 编译通过；
- 已检查并清理旧的 `foundation-serialization-api` / `foundation-serialization-jackson` Maven 模块引用；
- 已清理 `target`、`.class`、`.iml` 和 macOS `._*` 文件。

未在沙箱完成的检查：

- 完整 `mvn clean verify`。当前环境没有 Maven，且不能联网下载 Maven 依赖。

本地建议执行：

```bash
python3 scripts/validate-poms.py
mvn -U -pl component/foundation-component -am clean verify
```
