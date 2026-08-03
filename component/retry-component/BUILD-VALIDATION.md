# 构建验证记录

## Phase 1 RC4 实际完成

### 1. Foundation 接入

- 以冻结后的 Foundation ID 一期终版为唯一 ID API；
- `retry-core` 使用 `foundation.id.api.StringIdGenerator`；
- 默认算法使用 `foundation.id.factory.IdGenerators.uuidV7()`；
- Spring 自动配置支持从 `StringIdGeneratorRegistry` 的 `retry` 名称读取生成器；
- `RetryClock` 扩展 Foundation `ClockProvider`；
- `SystemRetryClock` 复用 Foundation `SystemClockProvider`；
- `retry-core` 复用 Foundation `Arguments` 与 `ExceptionSupport`；
- 测试复用 `FixedStringIdGenerator` 与 `MutableClockProvider`。

### 2. Java 17 严格编译

已使用：

```text
--release 17
-Xlint:all
-Werror
```

实际编译通过：

```text
Foundation ID 终版源码
Foundation Arguments / ExceptionSupport
Foundation ClockProvider / SystemClockProvider
retry-api 主源码
retry-core 主源码
retry-core 测试源码
retry-config 主源码（Spring/Micrometer 最小类型桩）
retry-demo 主源码（Spring Web 最小类型桩）
```

结果：

```text
RETRY_RC4_MAIN_COMPILE_OK
RETRY_RC4_CORE_TEST_COMPILE_OK
RETRY_RC4_CONFIG_DEMO_COMPILE_OK
```

### 3. 运行烟雾验证

实际运行覆盖：

- 两次 IOException 后第三次成功；
- 默认 retryId 为 UUID v7；
- Foundation Registry 按 `retry` 名称获取固定生成器；
- 注入生成器产生的 ID 被执行器使用；
- `SystemRetryClock` 可以暴露 Foundation `ClockProvider`；
- 重试状态与尝试次数正确。

结果：

```text
RETRY_FOUNDATION_REUSE_OK
```

### 4. 工程结构验证

已通过：

```text
Package layout verification passed
Java comment style verification passed: 60 files
RETRY_RC4_RESOURCE_PARSE_OK
RETRY_RC4_LAYOUT_OK
```

检查内容包括：

- Java package 与目录一致；
- `retry-api` 不依赖 Foundation；
- 不存在旧 Foundation 根包 ID 导入；
- POM XML 可解析；
- Spring 配置元数据 JSON 可解析；
- Demo YAML 可解析；
- 不包含 target、class、iml、__MACOSX 或 DS_Store。

## 当前环境限制

当前容器没有 Maven，因此没有执行真实 Maven 依赖解析、Spring Boot ContextRunner、JUnit、ArchUnit 和 Surefire 测试。

合入完整工程后执行：

```bash
cd component

python retry-component/scripts/verify-comment-style.py
python retry-component/scripts/verify-package-layout.py

mvn -pl foundation-component -am clean verify
mvn -pl retry-component -am clean verify
```

还需要确认 `component-bom` 已管理：

```text
foundation-core
foundation-time
foundation-id
foundation-test-support
retry-api
retry-core
retry-config
```
