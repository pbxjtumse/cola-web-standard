# Changelog

## 1.3.0-SNAPSHOT

本版本不改变 1.2.0 的重试执行语义，重点修正注释策略和源码可读性。

### 注释调整

- 删除全部 package、import、注解、括号、链式参数和普通语句后的机械行尾注释。
- 删除构造器、Getter、Setter 以及 Builder 简单赋值方法上的重复 Javadoc。
- 保留类型、业务字段、核心业务方法、扩展点和重要边界说明。
- 在 `DefaultRetryExecutor` 的状态流转中增加少量“为什么这样处理”的业务注释。
- 删除逐行注释生成脚本，避免后续再次批量污染源码。
- 使用 `verify-comment-style.py` 检查机械注释、行尾注释和 Getter/Setter Javadoc。

### 兼容性

- Java API、Spring Boot 配置、状态枚举和重试执行行为与 1.2.0 保持兼容。
- 项目版本升级为 `1.3.0-SNAPSHOT`。

## 1.2.0-SNAPSHOT

本版本继续保持一期“进程内同步有限重试”的职责边界，重点修复 1.1.0 中可能造成行为歧义、测试困难、配置覆盖错误和扩展接口失控的问题。

### 新增

- `RetryExecution<T>`：封装一次逻辑执行的操作、策略、属性、取消令牌和可选 `retryId`。
- `RetryCancellationToken`：支持协作式取消。
- `RetryIdGenerator` 与 `UuidRetryIdGenerator`：解耦重试标识生成方式。
- `RetryStatus.CANCELLED`。
- `RetryEventType.EXECUTION_CANCELLED`。
- `RetryPolicy.maxCauseDepth`，默认值为 16。
- `BackoffStrategies.exponentialWithFullJitter(..., RandomGenerator)` 测试友好重载。
- `RetryPolicyRegistry.replace`：显式替换同名策略。
- `iron.retry.metrics-enabled` 配置。
- Micrometer active、safety warning 与 backoff duration 指标。
- Java 逐行注释生成和验证脚本。
- 优化审查、注释规范和类职责索引文档。

### 行为修复

- 同一动作中的异常规则改为最具体类型优先，不再依赖添加顺序。
- cause 遍历使用对象身份去重并限制最大深度，防止循环链无限遍历。
- 同一个异常类型不能同时配置为不同动作。
- 自定义分类器不能与 `retryOn`、`stopOn`、`abortOn` 或结果规则混用。
- 自定义分类器不能把带异常的 `RetryAttempt` 判定为 `SUCCESS`。
- `RetryDecision` 只有 `RETRY` 可以携带 `delayOverride`。
- `RetryDecision.retry` 禁止使用 `NON_RETRYABLE` 分类。
- 显式零等待继续保留 `SERVER_DIRECTED` 等真实来源。
- `RetryResult` 增加状态、异常、值和尝试次数的一致性校验。
- `RetryAttempt` 增加时间、返回值与异常互斥校验。
- `DefaultRetryPolicyRegistry.register` 发现重复名称时立即失败，不再静默覆盖。
- 策略快照与策略名称按名称稳定排序。
- Spring 策略继承支持显式空列表清空父策略列表。
- Spring 循环继承错误输出完整路径，并确保异常后正确清理解析栈。
- 配置异常类使用线程上下文类加载器加载。
- 按失败类别委托退避时保留真实委托来源。

### 可测试性改进

- `RetryClock`、`RetrySleeper`、`RetryIdGenerator` 可以注入。
- 全抖动策略可以传入固定种子的随机源。
- 新增取消、规则特异性、cause 环、重复注册、非法分类器等测试。
- API 与 Core 主源码和测试源码使用 Java 21 `-Xlint:all -Werror` 静态编译通过。

### 工程治理

- Maven Enforcer 要求 Java `[21,22)` 与 Maven `[3.9.0,)`。
- 编译插件和 Surefire 在父工程中显式启用，编译统一使用 `-Xlint:all`。
- 属性 Map 的只读语义明确为浅复制，属性值对象不会深复制。
- 48 个 Java 文件的所有非空代码行均有注释覆盖。

## 1.1.0-SNAPSHOT

本版本在一期边界内完成模型升级。

### 新增

- `RetryAttempt<T>`：统一描述已经完成的一次物理尝试。
- `RetryDecision` 值对象：携带动作、原因、失败码、失败类别和延迟覆盖。
- `RetryFailureCategory`：统一瞬时错误、限流、并发冲突和依赖不可用等语义。
- `RetryDelay` 与 `RetryDelaySource`。
- 服务端指定延迟覆盖。
- 按失败类别选择退避策略。
- `OperationSafety` 与 `RetrySafetyMode`。
- 命名策略继承。
- Spring ApplicationEvent 桥接。
- Micrometer 指标。

### 明确不实现

- 异步重试。
- 单次尝试强制终止。
- 持久化重试和分布式调度。
- 重试预算、自适应限流、熔断及业务降级。
