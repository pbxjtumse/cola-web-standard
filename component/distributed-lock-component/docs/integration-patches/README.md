# 完整工程集成补丁

当前压缩包只包含 `distributed-lock-component`，因此无法直接修改其兄弟目录 `component-bom` 与最外层 `cola-web-standard/pom.xml`。

合入本组件后请同步两处：

1. `cola-web-standard-root-redisson-snippet.xml` -> 最外层根 POM：Redisson 第三方版本治理。
2. `component-bom-redisson-snippet.xml` -> `component/component-bom/pom.xml`：新增自研 `distributed-lock-provider-redisson` artifact 版本治理。

不要把这两个 snippet 文件本身作为 Maven module；它们只是精确的复制片段。
