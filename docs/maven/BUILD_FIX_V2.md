# Build Fix V2

## 本轮目标

上一版将根 POM 中全局继承的测试依赖下沉后，部分测试模块只保留了 JUnit，导致直接使用 `org.assertj.core.api` 的测试无法编译。本轮按照源码实际 import 补齐测试依赖，并补充直接使用但只依靠传递依赖获得的生产依赖。

## 测试依赖修复

以下模块新增 `org.assertj:assertj-core`，scope 为 `test`：

- `concurrency-api`
- `concurrency-core`
- `distributed-lock-api`
- `distributed-lock-core`
- `distributed-lock-demo`
- `distributed-lock-fencing-provider-jdbc`
- `distributed-lock-starter`
- `governance-spring-boot-starter`
- `retry-config`

以下模块补充直接测试依赖：

- `distributed-lock-starter`：`junit-jupiter`、`mockito-core`
- `governance-spring-boot-starter`：`junit-jupiter`
- `retry-config`：`junit-jupiter`
- `retry-demo`：`junit-jupiter`
- `foundation-architecture-tests`：`archunit-junit5`

`spring-boot-starter-test` 在需要 Spring 测试上下文的模块中继续保留；显式加入 JUnit、AssertJ、Mockito 是为了让直接 import 与 POM 依赖一致，而不是依赖 Starter 的传递细节。

## 生产依赖修复

以下模块补充直接使用的 Jackson API：

- `cache-provider-redis`：`jackson-annotations`
- `cache-starter`：`jackson-databind`
- `foundation-serialization-jackson`：`jackson-core`、`jackson-annotations`
- `message-codec-jackson`：`jackson-core`

这些依赖原本可能通过 `jackson-databind` 或其他内部模块传递获得，Maven 通常仍可编译，但会形成脆弱的隐藏依赖。本轮改为显式声明。

## 版本管理

- AssertJ、JUnit、Mockito、Awaitility 的版本继续由 Spring Boot 3.5.14 BOM 管理。
- ArchUnit 不属于 Spring Boot 的稳定管理范围，在根 POM 增加 `archunit.version=1.4.2`，并在 `dependencyManagement` 管理 `archunit-junit5`。
- 各子模块只声明依赖坐标和 scope，不重复写版本。

## 新增校验

新增：

```text
scripts/check-source-dependencies.py
```

它会扫描常见第三方 API 的 Java import，并检查对应模块 POM 是否显式声明。当前结果：

```text
Modules checked: 96
Missing direct dependencies: 0
```

原有 POM 校验结果：

```text
POM count: 96
BOM managed artifacts: 53
Errors: 0
Warnings: 0
```

## 构建方式

在项目根目录执行：

```bash
./scripts/build-first.sh
```

或者分步执行：

```bash
python3 scripts/validate-poms.py
python3 scripts/check-source-dependencies.py
mvn -U -DskipTests compile
mvn -U clean verify
```

第一次构建需要能够访问 Maven Central 或你配置的 Maven 私服。
