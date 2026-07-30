# retry-component

`retry-component` 是一期可落地的进程内同步重试组件。Java 包名统一使用 `com.xjtu.iron.retry`。

## 一期能力

- 显式 `RetryExecutor` API，不依赖注解和 AOP。
- 最大尝试次数，且 `maxAttempts` 包含第一次执行。
- 最大总持续时间，用来阻止后续尝试和退避等待越过总时间预算。
- 异常分类与返回结果分类。
- 固定退避、指数退避、全抖动指数退避。
- 线程中断正确传播，不吞掉 `InterruptedException`。
- 统一 `RetryResult` 和 `RetryStatus`。
- 命名策略注册表。
- 生命周期事件和 Micrometer 基础指标。
- Spring Boot 自动配置与 YAML 策略绑定。
- 一个可以直接运行的演示模块。

## 一期明确不做

- 不实现异步重试和延迟调度。
- 不实现单次尝试的强制超时。同步调用无法安全地强杀任意业务代码，后续应与并行组件的受管线程池集成。
- 不实现数据库持久化重试、分布式扫描、人工重放和管理后台。
- 不实现重试预算、熔断、限流和业务补偿。
- 不替代业务幂等。非幂等写操作必须在外部增加幂等保护。

## 模块结构

```text
retry-component
├── retry-api       # 稳定公开 API、策略、上下文、结果和事件
├── retry-core      # 同步重试执行器和命名策略注册表
├── retry-config    # Spring Boot 自动配置、配置绑定、Micrometer 指标
└── retry-demo      # 可运行示例
```

## 构建

```bash
mvn clean verify
```

## 启动演示

```bash
mvn -pl retry-demo -am spring-boot:run
```

默认端口：`18090`。

```bash
curl 'http://localhost:18090/demo/retry/exception?failures=2'
curl 'http://localhost:18090/demo/retry/result?pendingTimes=2'
curl 'http://localhost:18090/demo/retry/non-retryable'
```

## 最小使用方式

```java
RetryPolicy policy = RetryPolicy.builder("remote-query")
        .maxAttempts(3)
        .maxDuration(Duration.ofSeconds(5))
        .retryOn(IOException.class)
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

## 重要语义

1. `maxAttempts = 3` 表示总共最多执行三次，不是“首次执行后再重试三次”。
2. 默认分类器非常保守：没有明确配置的异常不会重试。
3. `Error` 不会被执行器吞掉，会直接向上传播。
4. `maxDuration` 不能中断已经开始执行的同步业务，只控制是否允许开始下一次尝试，以及是否允许进入下一段退避等待。
5. 监听器异常会被隔离，不能影响主业务执行结果。
6. `operationName` 和 `policyName` 会进入指标标签，应使用有限集合，不能放订单号、用户号等高基数数据。
