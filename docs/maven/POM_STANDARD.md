# Maven POM 分层规范

## 一级：cola-web-standard-parent

只负责全工程构建基线：

- Java 17；
- UTF-8；
- Spring Boot、COLA、Resilience4j、OpenTelemetry、Testcontainers BOM；
- Compiler、Surefire、Failsafe、Enforcer、Spring Boot Plugin 版本；
- 顶层 COLA 模块聚合；
- 顶层 `client/domain/app/adapter/infrastructure` 的版本管理。

根父 POM禁止声明普通 `dependencies`，避免所有子模块被动继承 SLF4J、Lombok 或 Spring Boot Test。

## 二级：component

只负责聚合：

```text
component-bom
foundation-component
cache-component
concurrency-component
...
```

不管理具体组件依赖，不放运行时依赖。

## 三级：各 `*-component`

负责：

- 聚合本组件子模块；
- 导入 `component-bom`；
- 管理该组件专属的第三方客户端版本。

例如 Message 可以管理 Kafka、RocketMQ、Pulsar；Foundation 可以管理仅 Foundation 使用的 Commons 版本。

## 四级：具体 Jar 模块

负责：

- 声明真实使用的直接依赖；
- 声明真实使用的测试依赖；
- 特殊可执行模块启用 Spring Boot Plugin。

具体模块不写自研组件版本。

## 依赖声明原则

1. 使用 SLF4J 的模块自己声明 `slf4j-api`；
2. 使用 Lombok 的模块自己声明 Lombok，scope 为 `provided`；
3. 纯单元测试声明 `junit-jupiter`；
4. Spring 上下文测试才声明 `spring-boot-starter-test`；
5. `compile` 是默认 scope，不重复书写；
6. 自研模块版本由 BOM 管理；
7. Demo 和 Architecture Tests 设置 `maven.deploy.skip=true`；
8. `dependencyConvergence` 只在 `strict-dependencies` Profile 中执行。
