# POM 规范化版本修改清单

## 已实施修改

### 根父 POM

- 删除继承给所有模块的 `slf4j-api`；
- 删除继承给所有模块的 Lombok；
- 删除继承给所有模块的 `spring-boot-starter-test`；
- 删除全局 `maven.deploy.skip=true`；
- 删除重复的编码属性；
- 删除语义不明确的 Jackson/JUnit 独立版本属性，统一跟随 Spring Boot BOM；
- 删除全部 `component/*` 具体模块版本；
- 只保留顶层 COLA 模块版本；
- 修复 Compiler Plugin 属性与实际版本不一致；
- 增加 Java、Maven、重复依赖、旧测试框架 Enforcer 规则；
- 将严格 `dependencyConvergence` 放进独立 Profile。

### component 与 component-bom

- 新增 `component/component-bom`；
- BOM 管理 53 个具备实际实现的可消费组件；
- `component/pom.xml` 恢复为纯聚合 POM；
- 所有组件根 POM导入 BOM；
- `domain` 和 `start` 因直接消费组件，也导入 BOM。

### Concurrency

- 所有直接子模块统一继承 `concurrency-component`；
- Integration 和 Provider 子模块继承各自聚合父 POM；
- 删除 `concurrency-api` 硬编码版本；
- 删除冗余 `compile` scope；
- Demo 统一启用 Boot Plugin。

### Retry

- 删除独立 Spring Boot 3.4.5 BOM；
- 删除重复 Java、编码、插件和 Enforcer 配置；
- 统一继承根工程 Spring Boot 3.5.14 和构建插件；
- 导入 `component-bom`。

### Governance

- 删除 `governance-api` 的 Java 7 编译覆盖；
- 删除聚合 POM `governance-configs` 中的普通依赖；
- 将 `governance-model` 显式下沉到真正使用它的 `governance-config-api`；
- Demo 启用 Boot Plugin。

### 全部模块

- 删除 Maven 模板 URL；
- 补齐本地父 POM `relativePath`；
- 删除自研依赖上的显式版本；
- 使用 SLF4J、Lombok、JUnit、Spring Test 的模块补充直接依赖；
- 可执行 Boot 模块启用 `spring-boot-maven-plugin`；
- Demo 和 Architecture Tests 跳过部署。

## 本轮刻意未实施

以下修改会改变运行时依赖或组件职责，因此保留到下一轮：

1. 拆分 Cache、Concurrency、Distributed Lock 的重型 Starter；
2. 从 Domain 删除 Web、AOP、Concurrency Core；
3. 合并 `message-codec-jackson` 与 Foundation Serialization；
4. Cache Redis Provider 改造为 Foundation Serializer；
5. 删除所有空占位模块；
6. Foundation Core 去除 Commons 传递依赖；
7. Retry/Lock 默认时间和 ID 实现接入 Foundation。

本版本只做低风险 Maven 规范化，不修改 Java 业务代码和运行时装配语义。
