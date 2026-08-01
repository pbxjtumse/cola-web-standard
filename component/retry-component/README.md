# retry-component

`retry-component` 是面向 Java 21 与 Spring Boot 3 的进程内同步重试组件。本项目不直接依赖 Failsafe、Resilience4j、Spring Retry 等第三方重试实现，而是吸收其中适合当前基础组件体系的设计，并保持核心 API、执行语义与框架集成之间的边界。

## 当前版本

```text
1.3.0-SNAPSHOT
```

这一版在 1.2.0 的功能基础上调整注释策略：保留类型、业务字段、核心方法和关键状态流转说明，删除 package/import、构造器、Getter/Setter、简单赋值和纯语法行上的机械注释。重试行为与 1.2.0 保持兼容。

## 本次核心优化

1. 增加 `RetryExecution<T>`，把操作、策略、属性、取消令牌和逻辑执行标识统一封装为一次不可变执行请求，避免 `RetryExecutor` 重载持续膨胀。
2. 增加 `RetryCancellationToken`，支持开始前、尝试之间和等待后的协作式取消，并增加 `RetryStatus.CANCELLED` 与 `EXECUTION_CANCELLED` 事件。
3. 增加 `RetryIdGenerator`，允许应用替换 UUID 生成方式，也允许调用方复用业务请求标识。
4. 异常规则改为“同一动作内最具体类型优先”，不再依赖规则声明顺序。
5. 异常 cause 遍历增加最大深度和对象身份去重，避免恶意或异常 cause 环导致死循环。
6. 同一异常类型禁止同时配置为 `RETRY`、`STOP` 或 `ABORT`，在策略构建阶段直接失败。
7. 自定义 `RetryClassifier` 与声明式规则禁止混用，避免两套规则来源产生隐式覆盖。
8. 执行器校验分类器契约，禁止分类器把带异常的尝试错误标记为成功。
9. `RetryDecision`、`RetryDelay`、`RetryResult`、`RetryAttempt` 和 `RetryEvent` 增加更严格的不变量校验。
10. `BackoffStrategies.exponentialWithFullJitter` 支持注入 `RandomGenerator`，使随机退避可以稳定测试。
11. `DefaultRetryPolicyRegistry.register` 不再静默覆盖同名策略，新增显式 `replace`。
12. Spring 策略继承支持区分“未配置列表”和“显式空列表”，子策略可以明确清空父策略规则。
13. 策略继承错误会给出完整循环路径，异常类加载使用线程上下文类加载器。
14. `RetryClock`、`RetrySleeper`、`RetryIdGenerator` 都可以通过 Spring Bean 覆盖。
15. Micrometer 监听器缓存计量器，并新增 active、safety warning 和 backoff duration 指标。
16. Maven Enforcer 明确要求 Java 21 与 Maven 3.9 以上。
17. 删除机械逐行注释，改为“业务语义优先”的注释规范，并增加注释风格检查脚本。

完整问题、风险和修复关系见 [优化审查报告](docs/optimization-review.md)。

## 模块结构

```text
retry-component
├── retry-api
│   ├── 公开执行接口
│   ├── 不可变策略与执行请求
│   ├── 上下文与物理尝试快照
│   ├── 决策、失败分类和退避模型
│   ├── 结果、状态和异常模型
│   └── 生命周期事件协议
├── retry-core
│   ├── 同步有限重试执行器
│   ├── 命名策略注册表
│   ├── UUID 标识生成器
│   ├── 可替换时钟
│   └── 可替换同步等待器
├── retry-config
│   ├── Spring Boot 自动配置
│   ├── 外部配置绑定
│   ├── 策略继承解析
│   ├── Spring ApplicationEvent 桥接
│   └── Micrometer 指标监听器
├── retry-demo
│   ├── 异常触发重试
│   ├── 返回结果触发重试
│   ├── 服务端等待覆盖
│   ├── 协作式取消
│   └── 不可重试异常
├── scripts
│   └── verify-comment-style.py
└── docs
    ├── optimization-review.md
    ├── comment-standard.md
    ├── class-index.md
    ├── architecture.md
    ├── execution-flow.md
    ├── configuration.md
    ├── api-reference.md
    ├── boundaries.md
    ├── integration-guidelines.md
    ├── open-source-adoption.md
    ├── testing.md
    └── phase-2-roadmap.md
```

## 最小使用方式

```java
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
        Map.of("downstream", "order-service"),
        context -> remoteClient.query(),
        policy
);
```

## 使用完整执行请求

当需要取消、业务重试标识或上下文属性时，使用 `RetryExecution<T>`：

```java
RetryExecution<Order> execution = RetryExecution.builder(
                "query-order",
                context -> remoteClient.query(orderId),
                policy
        )
        .retryId(requestId)
        .attributes(Map.of(
                "downstream", "order-service",
                "tenant", tenantCode
        ))
        .cancellationToken(() -> requestContext.isCancelled())
        .build();

RetryResult<Order> result = retryExecutor.execute(execution);
```

取消是协作式取消，只会阻止尚未开始的尝试或后续等待，不能强制终止已经进入业务代码的同步操作。

## 服务端指定等待时间

```java
RetryPolicy policy = RetryPolicy.builder("http-throttling")
        .maxAttempts(3)
        .maxDuration(Duration.ofSeconds(10))
        .classifier(attempt -> {
            HttpResponse response = (HttpResponse) attempt.getResult();
            if (response.statusCode() == 429) {
                return RetryDecision.retryAfter(
                        response.retryAfter(),
                        RetryDelaySource.SERVER_DIRECTED,
                        "HTTP Retry-After",
                        "HTTP_429",
                        RetryFailureCategory.THROTTLING
                );
            }
            return RetryDecision.success("request completed");
        })
        .build();
```

`delayOverride` 优先于策略退避，但仍受 `maxDuration` 约束。下游要求等待 30 秒而当前只剩 2 秒时，执行器返回 `TIMED_OUT`，不会继续阻塞。

## 重要语义

1. `maxAttempts` 包含第一次正常执行。
2. 默认不重试任何未被显式声明的异常。
3. `ABORT` 规则优先于 `STOP`，`STOP` 优先于 `RETRY`；这是安全优先级，不由声明顺序决定。
4. 同一动作内部存在父类与子类规则时，子类规则优先。
5. `Error` 不会被执行器捕获。
6. `InterruptedException` 会恢复线程中断标记并立即结束。
7. `maxDuration` 控制总逻辑预算，但无法强杀已经进入业务代码的同步调用。
8. 分类器、退避策略和监听器属于扩展点；分类器或退避策略违反契约会返回 `EXECUTION_FAILED`，监听器异常则被隔离。
9. `RetryPolicy`、`RetryExecution`、`RetryAttempt`、`RetryResult` 和事件均为不可变快照。
10. 非幂等安全声明只做风险防护，不提供真正的业务幂等。
11. 长时间、可恢复或跨进程重试不属于一期同步核心。

## 注释规范

本版本不再使用逐行行尾注释。注释只保留在类型、业务字段、核心业务方法、扩展点和不直观的状态流转上。构造器、Getter、Setter、Builder 简单赋值方法、package/import 和纯语法行保持干净。

执行：

```bash
python scripts/verify-comment-style.py
```

当前检查结果：

```text
Java comment style verification passed: 48 files
```

具体约定见 [Java 注释规范](docs/comment-standard.md)。

## Spring Boot 配置示例

```yaml
iron:
  retry:
    enabled: true
    publish-spring-events: true
    metrics-enabled: true
    policies:
      remote-base:
        max-attempts: 3
        max-duration: 5s
        operation-safety: READ_ONLY
        safety-mode: WARN
        traverse-causes: true
        max-cause-depth: 16
        retry-failure-category: TRANSIENT
        retry-failure-code: REMOTE_TRANSIENT
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

      payment-query:
        base-policy: remote-base
        max-attempts: 5
        retry-on: []
```

`payment-query.retry-on: []` 表示显式清空父策略的可重试异常；完全不写 `retry-on` 才表示继承。

## 构建与运行

```bash
mvn clean verify
mvn -pl retry-demo -am spring-boot:run
```

默认端口：`18090`。

```bash
curl 'http://localhost:18090/demo/retry/exception?failures=2'
curl 'http://localhost:18090/demo/retry/result?pendingTimes=2'
curl 'http://localhost:18090/demo/retry/server-delay'
curl 'http://localhost:18090/demo/retry/cancel'
curl 'http://localhost:18090/demo/retry/non-retryable'
```

## 一期仍然不做

- 异步重试和非阻塞调度。
- 单次尝试强制超时或线程强杀。
- 重试预算、自适应限流和熔断。
- 数据库持久化、分布式抢占和人工重放。
- 消息 ACK/NACK、事务提交与回滚。
- 业务幂等、业务补偿和工作流编排。

## 文档入口

- [本次优化审查](docs/optimization-review.md)
- [Java 注释规范](docs/comment-standard.md)
- [全部类职责索引](docs/class-index.md)
- [架构与模块职责](docs/architecture.md)
- [完整执行流程](docs/execution-flow.md)
- [配置与继承规则](docs/configuration.md)
- [API 参考](docs/api-reference.md)
- [一期职责边界](docs/boundaries.md)
- [组件集成原则](docs/integration-guidelines.md)
- [开源设计吸收说明](docs/open-source-adoption.md)
- [测试与验证策略](docs/testing.md)
- [二期演进路线](docs/phase-2-roadmap.md)
- [版本变更记录](CHANGELOG.md)
- [构建验证记录](BUILD-VALIDATION.md)
