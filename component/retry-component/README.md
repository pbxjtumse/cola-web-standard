# retry-component

`retry-component` 是面向 **Java 17 / Spring Boot 3** 的进程内同步重试组件。

当前交付里程碑为：

```text
Phase 1 V1 minimal for message-send
```

Maven 版本继续继承完整组件工程的统一版本：

```text
com.xjtu.iron:component:1.0.0-SNAPSHOT
```

本项目是完整 `component` 聚合工程下的子工程，根 POM 的 `relativePath` 指向 `../pom.xml`。将本目录放回完整组件工程后执行 Maven 构建，不应单独修改父工程坐标。

## 一、当前是否属于一期

是。

一期只解决：

> 在单个 JVM 进程内，以明确、有限、可观测的方式同步重试一个业务操作。

一期已经包含：

- 显式 `RetryExecutor` 执行入口；
- 最大尝试次数，且包含第一次正常执行；
- 整体 `maxDuration` 时间预算；
- 异常规则与返回结果规则；
- `RETRY`、`STOP`、`ABORT` 决策；
- 固定退避、指数退避和 Full Jitter；
- 服务端指定等待时间覆盖；
- 协作式取消和线程中断处理；
- 非幂等操作安全声明；
- 命名策略注册；
- Spring Boot 外部配置与策略继承；
- 生命周期事件、Spring 事件桥接和 Micrometer 指标；
- Java 17 构建基线；
- 有效的 API、Core、配置和 Demo 测试源码。

一期明确不包含：

- 异步或 Reactor 重试；
- 单次尝试强制超时和线程强杀；
- 重试预算、自适应限流和熔断；
- 数据库持久化重试、分布式抢占和人工重放；
- 消息 ACK/NACK、重试主题、死信和可靠发送；
- 业务幂等、业务补偿和工作流编排。

因此，本版本可以用于验证同步重试语义，但不能被描述为“完整可靠消息方案”。

## 二、模块结构

```text
retry-component
├── retry-api
├── retry-core
├── retry-config
├── retry-demo
├── docs
└── scripts
```

### retry-api

只保存稳定的公共协议和不可变模型，不依赖 Spring、Micrometer 或具体执行实现。

```text
com.xjtu.iron.retry.api
├── execution
│   ├── RetryExecutor
│   ├── RetryExecution
│   ├── RetryOperation
│   ├── RetryContext
│   ├── RetryAttempt
│   ├── RetryResult
│   ├── RetryStatus
│   └── RetryCancellationToken
├── policy
│   ├── RetryPolicy
│   ├── RetryPolicyRegistry
│   ├── RetryClassifier
│   ├── RetryDecision
│   ├── RetryDecisionType
│   ├── RetryFailureCategory
│   ├── OperationSafety
│   └── RetrySafetyMode
├── backoff
│   ├── BackoffStrategy
│   ├── BackoffStrategies
│   ├── RetryDelay
│   └── RetryDelaySource
├── event
│   ├── RetryEvent
│   ├── RetryEventType
│   └── RetryListener
└── exception
    └── RetryExecutionException
```

这里没有建立 `interface`、`model`、`enums` 之类的纯技术分包。接口与其紧密相关的模型应放在同一个职责包中，例如 `RetryExecutor` 与 `RetryExecution` 同属 `execution`。

### retry-core

```text
com.xjtu.iron.retry.core
├── executor
│   ├── DefaultRetryExecutor
│   └── RetryEventDispatcher
├── policy
│   └── DefaultRetryPolicyRegistry
└── time
    ├── RetryClock
    ├── RetrySleeper
    ├── SystemRetryClock
    └── ThreadSleepRetrySleeper
```

`DefaultRetryExecutor` 保留状态机编排职责；监听器复制、事件构造和监听器故障隔离已经提取到 `RetryEventDispatcher`。

### retry-config

```text
com.xjtu.iron.retry.config
├── autoconfigure
│   ├── RetryAutoConfiguration
│   └── RetryMetricsAutoConfiguration
├── properties
│   ├── RetryProperties
│   ├── RetryPolicyConfigurationLoader
│   ├── RetryPolicyPropertiesResolver
│   └── RetryPolicyFactory
└── observation
    ├── MicrometerRetryListener
    └── SpringApplicationRetryListener
```

自动配置、属性解析和观测适配不再混在同一个 `config` 根包。

## 三、本次发现并修复的重要问题

### 1. 核心执行器测试实际被全部注释

上传版本中的：

```text
retry-core/src/test/java/.../DefaultRetryExecutorTest.java
```

从 `package` 到最后一行都以 `//` 开头，因此 Maven 不会发现任何执行器测试。

本版本已经恢复全部测试源码，并将 Java 21 的：

```java
events.getFirst();
events.getLast();
```

修改为 Java 17 兼容写法。

### 2. API 根包类型过度堆积

原来二十多个接口、枚举和值对象都位于：

```text
com.xjtu.iron.retry.api
```

本次按执行、策略、退避、事件和异常五个职责域拆分。

### 3. RetryPolicy 同时承担策略和值规则执行细节

规则匹配实现已从 `RetryPolicy` 中提取为包内实现：

```text
RuleBasedRetryClassifier
RetryExceptionRule
RetryResultRule
```

`RetryPolicy` 回归“不可变策略与 Builder”职责。

### 4. DefaultRetryExecutor 同时管理事件分发

事件构造和监听器异常隔离已提取为：

```text
RetryEventDispatcher
```

执行器继续负责状态机，不再直接维护事件基础设施细节。

### 5. Java 版本说明与真实需求不一致

组件没有使用 Java 21 专属语言能力，现统一为：

```text
构建 JDK：17 或更高
目标字节码：Java 17
最低运行环境：Java 17
```

同步休眠使用 Java 17 支持的：

```java
Thread.sleep(millis, nanos);
```

### 6. Spring 动态 Map 元数据识别不完整

增加：

```text
META-INF/additional-spring-configuration-metadata.json
```

显式描述：

```yaml
xjtu.iron.retry.policies.*.retry-failure-category
xjtu.iron.retry.policies.*.max-attempts
xjtu.iron.retry.policies.*.backoff.type
```

避免 IDEA 将 `TRANSIENT` 错误识别为整数。YAML 正确写法仍然是：

```yaml
retry-failure-category: TRANSIENT
```

### 7. 发布包包含构建与 IDE 产物

上传版本包含：

```text
target/
*.class
*.iml
__MACOSX/
```

本次交付包已全部清理，并增加包结构检查脚本。

## 四、最小使用方式

```java
import com.xjtu.iron.retry.api.backoff.BackoffStrategies;
import com.xjtu.iron.retry.api.execution.RetryResult;
import com.xjtu.iron.retry.api.policy.OperationSafety;
import com.xjtu.iron.retry.api.policy.RetryFailureCategory;
import com.xjtu.iron.retry.api.policy.RetryPolicy;

RetryPolicy policy = RetryPolicy.builder("remote-query")
        .maxAttempts(3)
        .maxDuration(Duration.ofSeconds(5))
        .operationSafety(OperationSafety.READ_ONLY)
        .retryOn(
                RetryFailureCategory.TRANSIENT,
                "REMOTE_IO_FAILURE",
                IOException.class
        )
        .stopOn(IllegalArgumentException.class)
        .backoffStrategy(BackoffStrategies.exponentialWithFullJitter(
                Duration.ofMillis(100),
                Duration.ofSeconds(1),
                2.0D
        ))
        .build();

RetryResult<String> result = retryExecutor.execute(
        "query-order",
        context -> remoteClient.query(),
        policy
);
```

## 五、完整执行请求

```java
RetryExecution<Order> execution = RetryExecution.builder(
                "query-order",
                context -> remoteClient.query(orderId),
                policy
        )
        .retryId(requestId)
        .attributes(Map.of("downstream", "order-service"))
        .cancellationToken(() -> requestContext.isCancelled())
        .build();

RetryResult<Order> result = retryExecutor.execute(execution);
```

取消属于协作式取消，只能阻止尚未开始的下一次尝试，不能强制终止已经进入业务代码的同步调用。

## 六、Spring Boot 配置

配置前缀统一为：

```text
xjtu.iron.retry
```

这和 `message-component`、`lock-component`、`cache-component` 等技术组件的命名空间保持一致。

```yaml
xjtu:
  iron:
    retry:
      enabled: true
      publish-spring-events: true
      metrics-enabled: true
      policies:
        remote-call:
          max-attempts: 3
          max-duration: 5s
          operation-safety: READ_ONLY
          safety-mode: WARN
          traverse-causes: true
          max-cause-depth: 16
          retry-failure-category: TRANSIENT
          retry-failure-code: REMOTE_TRANSIENT_FAILURE
          retry-on:
            - java.io.IOException
          stop-on:
            - java.lang.IllegalArgumentException
          abort-on:
            - java.lang.SecurityException
          backoff:
            type: EXPONENTIAL_FULL_JITTER
            initial-delay: 100ms
            max-delay: 1s
            multiplier: 2.0
```

`retry-failure-category` 只描述已经命中 `retry-on` 的失败类别，本身不会决定异常是否可重试。

### message-component 二期发送可靠性推荐策略

这个策略是给 `message-component` 二期可靠发送使用的最小策略。它只负责“是否短时间再试一次”，不负责判断 MQ Broker 是否已经收到消息。`UNKNOWN`、`RETRY_EXHAUSTED` 等消息发送语义由 `message-component` 自己解释。

```yaml
xjtu:
  iron:
    retry:
      policies:
        message-send:
          max-attempts: 3
          max-duration: 5s
          operation-safety: IDEMPOTENCY_PROTECTED
          safety-mode: WARN
          traverse-causes: true
          max-cause-depth: 8
          retry-failure-category: TRANSIENT
          retry-failure-code: MESSAGE_SEND_RETRYABLE_FAILURE
          retry-on:
            - java.io.IOException
            - java.util.concurrent.TimeoutException
          stop-on:
            - java.lang.IllegalArgumentException
          abort-on:
            - java.lang.SecurityException
          backoff:
            type: EXPONENTIAL_FULL_JITTER
            initial-delay: 100ms
            max-delay: 1s
            multiplier: 2.0
```

## 七、构建

将本目录放在完整 `component` 工程中：

```bash
cd component
mvn -pl retry-component -am clean verify
```

运行附加检查：

```bash
python retry-component/scripts/verify-comment-style.py
python retry-component/scripts/verify-package-layout.py
```

启动 Demo：

```bash
mvn -pl retry-component/retry-demo -am spring-boot:run
```

## 八、与消息组件的后续关系

补充文档：`docs/message-send-minimal-support.md` 记录了 `message-component` 二期发送可靠性接入 retry V1 的最小边界。


一期消息组件验证阶段，建议先完成：

1. Kafka、RocketMQ、Pulsar 客户端与 K8s 中间件的基本连通；
2. 统一消息模型、序列化、发送结果和异常转换；
3. 明确各客户端自身的重试次数，避免和外层重试叠加；
4. 暂不承诺可靠发送、Exactly Once 或持久化补偿；
5. 收集“确定失败、确定成功、结果未知”三种发送结果。

消息可靠发送二期再设计：

- 进程内短重试；
- `UNKNOWN_OUTCOME` 处理；
- Outbox；
- 幂等发送与消费；
- 重试主题和死信；
- 对账与人工重放；
- 多层重试预算。

详细步骤见 [消息组件一期验证计划](docs/message-phase1-validation-plan.md)。

## 基础 ID 依赖

本版本不再在 `retry-core` 内实现 UUID。逻辑重试 ID 由用户当前真实的
`foundation-component/foundation-id` 提供。

依赖方向：

```text
retry-api
    不依赖 foundation-id

retry-core
    -> retry-api + foundation-id + foundation-time + foundation-core

retry-config
    -> retry-core + foundation-id + Spring Boot

retry-core tests
    -> foundation-test-support
```

非 Spring 使用 `DefaultRetryExecutor` 时，默认调用：

```java
IdGenerators.uuidV7()
```

Spring 环境由 `retry-config` 注册固定名称 Bean：

```text
retryIdGenerator
```

该 Bean 默认也是 UUID v7。业务需要替换算法时，只需注册同名 `StringIdGenerator`，
不会与 `messageIdGenerator`、`taskIdGenerator` 等其他生成器发生按类型注入冲突。

调用方已有 requestId、messageId 或任务执行 ID 时，仍可在 `RetryExecution` 中显式指定
`retryId`。自动生成的 retryId 只是技术关联标识，不是幂等键。

## Phase 1 V1 minimal for message-send

- 对接已经冻结的 Foundation ID 一期终版。
- 公共 ID 契约统一为 `foundation.id.api.StringIdGenerator`。
- 算法工厂统一为 `foundation.id.factory.IdGenerators`。
- Spring 自动配置可以从 `StringIdGeneratorRegistry` 的 `retry` 名称选择生成器。
- 未提供专用 Bean且不存在 Registry 时，默认使用 Foundation UUID v7；Registry 存在但缺少 retry 时启动失败。
- `RetryClock` 扩展 Foundation `ClockProvider`，事件时间戳复用统一墙上时钟。
- 单调耗时仍由 `RetryClock.nanoTime()` 提供，避免系统时钟调整破坏 `maxDuration`。
- `retry-core` 复用 Foundation `Arguments` 和 `ExceptionSupport`。
- 测试复用 `foundation-test-support` 的 `FixedStringIdGenerator`。
- 详细取舍见 `docs/foundation-capability-reuse.md`。
