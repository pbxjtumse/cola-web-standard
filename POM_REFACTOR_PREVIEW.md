# POM 规范化预览版本

本目录基于原项目生成，仅修改 Maven POM 和新增 Maven 说明/校验文件，没有修改 Java 业务代码。

## 本版本目标

1. 根父 POM不再直接继承 SLF4J、Lombok、Spring Boot Test；
2. 根父 POM不再手工枚举 `component/*` 的具体 Jar；
3. 新增 `component-bom` 统一管理技术组件版本；
4. 统一 Concurrency 父链；
5. Retry 统一 Spring Boot 与插件版本；
6. Governance 删除 Java 7 历史配置；
7. 使用到日志、Lombok、测试能力的模块显式声明依赖；
8. 可执行应用启用 Spring Boot Maven Plugin。

## 先查看这些文件

- `docs/maven/COMPONENT_BOM_GUIDE.md`
- `docs/maven/POM_STANDARD.md`
- `docs/maven/MODIFICATION_SUMMARY.md`
- `docs/maven/VALIDATION_REPORT.txt`
- `docs/maven/POM_CHANGES.patch`

## 本地验证

当前生成环境没有 Maven，因此只完成了结构静态验证。请在本地项目根目录执行：

```bash
python3 scripts/validate-poms.py
mvn -U clean verify
mvn -Pstrict-dependencies verify
```

第二条用于正常构建；第三条额外启用严格依赖收敛检查。如果第三条因为 Pulsar、OpenTelemetry 等大型依赖树发生版本分歧，应先分析 dependency tree，而不是直接排除依赖。

## 本轮没有修改的内容

- 没有拆分重型 Starter；
- 没有调整 Domain 分层依赖；
- 没有删除空模块；
- 没有合并 Message 与 Foundation 的 Jackson 实现；
- 没有修改任何 Java 源码。
