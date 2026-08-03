# 直接导入说明

将本目录直接放入：

```text
component/retry-component
```

然后确认父工程 `component/pom.xml` 包含：

```xml
<module>foundation-component</module>
<module>retry-component</module>
```

建议执行：

```bash
cd component
python retry-component/scripts/verify-package-layout.py
python retry-component/scripts/verify-comment-style.py
mvn -pl retry-component -am clean verify
```

如果 Maven 提示找不到 `foundation-id`、`foundation-core`、`foundation-time` 或 `foundation-test-support` 的版本，说明 `component-bom` 还没有管理这些依赖。
