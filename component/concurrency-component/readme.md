# XJTU Iron Concurrency Component

`concurrency-component` 是 `component/` 工程体系下的并行执行组件，目标不是简单封装 `CompletableFuture`，而是提供一套可配置、可观测、可诊断、可扩展的生产级异步执行能力。

当前版本定位为 **Phase 1：本地线程池并行执行组件**。

---

## 1. 组件解决什么问题

在业务系统里，常见并行场景包括：

- 多个 RPC / HTTP / DB 查询并行执行，最后聚合结果。
- 异步执行无返回任务，例如异步通知、异步埋点、缓存刷新。
- 对不同业务场景使用不同线程池隔离，例如 `biz-query-pool`、`rpc-pool`、`tiny-pool`。
- 支持任务超时、降级、上下文传播、任务监听、异常兜底。
- 观察线程池运行状态，例如活跃线程、队列积压、拒绝次数、执行耗时。
- 运行时查看和调整线程池核心参数。

这个组件的核心设计原则是：

```text
ThreadPoolExecutor 负责真正执行任务。
CompletableFuture 负责承载异步结果和编排。
TaskExecutionTemplate 负责安全投递任务。
AsyncTemplate 负责组合多个异步结果。
```

---

## 2. 模块结构

```text
concurrency-component
├── concurrency-api
│   ├── context        上下文传播 API
│   ├── enums          队列、拒绝策略、任务状态枚举
│   ├── event          任务执行事件
│   ├── exception      统一异常模型
│   ├── execution      AsyncExecutor、AsyncTemplate、AsyncTask、ThreadPoolManager
│   └── listener       任务监听器、fire-and-forget 异常处理器
│
├── concurrency-config
│   └── ThreadPoolSpecResolver 等配置解析抽象
│
├── concurrency-core
│   ├── context        上下文传播默认实现
│   ├── execution      默认执行实现、任务命令、线程池管理
│   ├── listener       默认监听/异常处理实现
│   ├── metrics        指标记录器、线程池 Gauge 指标枚举
│   └── spi            core 内部扩展点接口
│
├── concurrency-spring-boot-starter
│   ├── configuration  Spring Boot 自动装配
│   ├── properties     配置属性绑定
│   └── resolver       application.yml 到 ThreadPoolSpec 的转换
│
├── concurrency-integrations
│   ├── observability  MDC、可观测集成
│   ├── governance     治理能力集成预留
│   └── ttl            TTL 上下文传播预留
│
├── concurrency-provider
│   ├── local          本地 provider 预留
│   └── redis          分布式并发能力预留
│
└── concurrency-demo
    └── 使用示例和验证接口
```

---

## 3. 核心调用链

```text
业务代码
  ↓
AsyncExecutor.execute / run / supply / submit
  ↓
DefaultAsyncExecutor
  ↓
TaskExecutionTemplate
  ↓
ThreadPoolRegistry.getExecutor(executorName)
  ↓
ContextAwareTaskDecorator 包装上下文
  ↓
TaskCommand
  ↓
ThreadPoolExecutor.execute(command)
  ↓
RejectedExecutionHandler
  ↓
TaskCommand.run()
  ↓
记录 queueCost / runCost / totalCost
  ↓
TaskExecutionListener
  ↓
ConcurrencyMetricsRecorder
  ↓
CompletableFuture.complete / completeExceptionally
  ↓
AsyncTemplate allOf / anyOf / allOfOutcome / allOfFailFast
```

---

## 4. 快速接入

### 4.1 引入 starter

```xml
<dependency>
    <groupId>com.xjtu.iron</groupId>
    <artifactId>concurrency-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

如果需要 Actuator / Micrometer 指标：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

## 5. application.yml 示例

```yaml
xjtu:
  iron:
    concurrency:
      enabled: true
      default-executor: biz-query-pool

      context:
        enabled: true
        mdc-enabled: true
        ttl-enabled: false

      diagnostics:
        enabled: true
        queue-usage-warn-threshold: 0.8
        active-usage-warn-threshold: 0.9
        down-when-busy: false

      observability:
        enabled: true

      thread-pools:
        biz-query-pool:
          core-pool-size: 8
          max-pool-size: 32
          queue-capacity: 1000
          keep-alive-time: 60s
          thread-name-prefix: biz-query-pool-
          queue-type: BOUNDED_LINKED_BLOCKING_QUEUE
          rejection-policy: ABORT
          rejection-wait-time: 200ms
          allow-core-thread-timeout: false
          wait-for-tasks-to-complete-on-shutdown: true
          await-termination: 30s

        rpc-pool:
          core-pool-size: 16
          max-pool-size: 64
          queue-capacity: 2000
          keep-alive-time: 60s
          thread-name-prefix: rpc-pool-
          queue-type: BOUNDED_ARRAY_BLOCKING_QUEUE
          rejection-policy: BLOCKING_WAIT
          rejection-wait-time: 100ms

        tiny-pool:
          core-pool-size: 1
          max-pool-size: 1
          queue-capacity: 1
          keep-alive-time: 60s
          thread-name-prefix: tiny-pool-
          queue-type: BOUNDED_ARRAY_BLOCKING_QUEUE
          rejection-policy: ABORT
```

---

## 6. AsyncExecutor 使用方式

`AsyncExecutor` 负责提交单个任务。

### 6.1 execute：只提交任务，不关心结果

适合异步日志、异步通知、埋点、缓存刷新等弱依赖任务。

```java
asyncExecutor.execute("biz-query-pool", "sendLog", () -> {
    logService.send();
});
```

特点：

- 不返回 `CompletableFuture`。
- 任务提交被拒绝时，调用方会收到 `ConcurrencyRejectedException`。
- 任务执行时异常不会返回给调用方，统一交给 `AsyncUncaughtExceptionHandler`。

---

### 6.2 tryExecute：尝试提交任务

适合可丢弃、可降级任务。

```java
boolean accepted = asyncExecutor.tryExecute("biz-query-pool", "refreshCache", () -> {
    cacheService.refresh();
});

if (!accepted) {
    log.warn("refreshCache skipped because thread pool is busy");
}
```

特点：

- 提交成功返回 `true`。
- 被线程池拒绝返回 `false`。
- 参数错误、线程池不存在等问题不应该被吞掉。

---

### 6.3 run：无业务返回值，但可感知完成

```java
CompletableFuture<Void> future = asyncExecutor.run("biz-query-pool", "syncTag", () -> {
    tagService.sync(userId);
});

future.whenComplete((unused, ex) -> {
    if (ex == null) {
        log.info("syncTag success");
    } else {
        log.error("syncTag failed", ex);
    }
});
```

`CompletableFuture<Void>` 表示：

```text
任务没有业务返回值，但调用方可以知道它成功、失败、超时或完成。
```

---

### 6.4 supply：有业务返回值

```java
CompletableFuture<UserDTO> userFuture = asyncExecutor.supply(
        "biz-query-pool",
        "queryUser",
        () -> userService.query(userId)
);
```

适合：

- 并行查询。
- RPC 聚合。
- 页面数据聚合。
- 多来源数据合并。

---

### 6.5 submit：完整异步任务模型

```java
CompletableFuture<UserDTO> future = asyncExecutor.submit(
        AsyncTask.of("biz-query-pool", "queryUser", () -> userService.query(userId))
                .taskId("task-001")
                .bizKey("userId=" + userId)
                .description("查询用户基础信息")
                .tag("scene", "user-profile")
                .timeout(Duration.ofSeconds(2))
                .queueTimeout(Duration.ofMillis(500))
                .cancelOnTimeout(true)
                .interruptOnCancel(true)
                .fallback(ex -> UserDTO.empty(userId))
                .contextPropagation(true)
);
```

`submit` 适合需要精细控制的任务：

- 任务 ID。
- 业务 Key。
- 标签。
- 超时。
- fallback 降级。
- 排队超时。
- 是否取消。
- 是否传播上下文。

---

## 7. AsyncTask 任务元数据

`AsyncTask` 是完整异步任务模型。核心字段：

| 字段 | 说明 |
| --- | --- |
| `taskId` | 任务唯一标识，用于日志、监听、诊断、任务状态追踪。 |
| `executorName` | 使用哪个线程池执行。 |
| `taskName` | 任务名称。 |
| `bizKey` | 业务标识，例如 `orderId=xxx`、`userId=xxx`。 |
| `description` | 任务说明。 |
| `tags` | 任务标签，例如 `scene=user-profile`。 |
| `timeout` | 结果层超时时间。 |
| `queueTimeout` | 排队超时时间。 |
| `cancelOnTimeout` | 超时后是否尝试取消任务。 |
| `interruptOnCancel` | 取消时是否尝试中断执行线程。 |
| `contextPropagation` | 是否传播上下文。 |
| `fallback` | 失败或超时时的兜底返回。 |

注意：`interruptOnCancel` 不是强杀线程。Java 中断是协作式的，业务代码需要响应中断。

---

## 8. AsyncTemplate 编排能力

`AsyncTemplate` 不负责提交任务，只负责组合多个 `CompletableFuture`。

### 8.1 allOf：全部成功才成功

```java
CompletableFuture<UserDTO> userFuture = asyncExecutor.supply("biz-query-pool", "queryUser", () -> userService.query(userId));
CompletableFuture<AccountDTO> accountFuture = asyncExecutor.supply("biz-query-pool", "queryAccount", () -> accountService.query(userId));

CompletableFuture<List<Object>> resultFuture = asyncTemplate.allOf(List.of(
        userFuture.thenApply(v -> (Object) v),
        accountFuture.thenApply(v -> (Object) v)
));
```

一个任务失败，整体失败。

---

### 8.2 allOfOutcome：全部等完，保留成功和失败明细

```java
CompletableFuture<AsyncBatchResult<Object>> resultFuture = asyncTemplate.allOfOutcome(List.of(
        NamedFuture.of("queryUser", userFuture.thenApply(v -> (Object) v)),
        NamedFuture.of("queryAccount", accountFuture.thenApply(v -> (Object) v))
));
```

适合非强依赖聚合场景，例如运营后台、管理页面。

---

### 8.3 allOfFailFast：任意失败则快速失败

```java
CompletableFuture<List<Object>> resultFuture = asyncTemplate.allOfFailFast(List.of(
        userFuture.thenApply(v -> (Object) v),
        accountFuture.thenApply(v -> (Object) v)
));
```

注意：`CompletableFuture.cancel(true)` 不保证强制中断底层线程。

---

### 8.4 anyOf：任意一个完成即返回

```java
CompletableFuture<Object> fastest = asyncTemplate.anyOf(List.of(
        cacheFuture,
        dbFuture,
        remoteFuture
));
```

注意：`anyOf` 是第一个完成，不一定是第一个成功。后续建议扩展 `anySuccess`。

---

### 8.5 withTimeout / withFallback

```java
CompletableFuture<UserDTO> result = asyncTemplate.withFallback(
        asyncTemplate.withTimeout(userFuture, Duration.ofSeconds(2)),
        ex -> UserDTO.empty(userId)
);
```

---

## 9. TaskExecutionTemplate 投递模板

`TaskExecutionTemplate` 是组件核心投递模板，负责把任务安全地交给 `ThreadPoolExecutor`。

它统一处理：

- 参数校验。
- 查找线程池。
- 上下文传播。
- 任务包装。
- submitted / started / success / failure / rejected / timeout / fallback 事件。
- queueCost / runCost / totalCost 统计。
- `CompletableFuture.complete` / `completeExceptionally`。
- 拒绝策略异常转换。

业务侧一般不直接使用 `TaskExecutionTemplate`，而是使用 `AsyncExecutor`。

---

## 10. 耗时统计

组件区分三类耗时：

| 指标 | 说明 |
| --- | --- |
| `queueCostMillis` | 任务从提交到开始执行的排队耗时。 |
| `runCostMillis` | 任务真正执行业务逻辑的耗时。 |
| `totalCostMillis` | 从提交到完成的总耗时。 |

判断方式：

```text
queueCostMillis 高：线程池压力大，任务在队列里排队。
runCostMillis 高：业务逻辑慢，可能是 RPC / DB / HTTP / IO 慢。
```

---

## 11. 任务监听器

实现 `TaskExecutionListener` 即可监听任务生命周期。

```java
@Component
public class MyTaskExecutionListener implements TaskExecutionListener {

    @Override
    public void onSuccess(TaskExecutionEvent event) {
        log.info("task success, taskId={}, taskName={}, queueCost={}ms, runCost={}ms",
                event.getTaskId(),
                event.getTaskName(),
                event.getQueueCostMillis(),
                event.getRunCostMillis());
    }

    @Override
    public void onFailure(TaskExecutionEvent event) {
        log.error("task failed, taskId={}, taskName={}",
                event.getTaskId(),
                event.getTaskName(),
                event.getThrowable());
    }
}
```

支持的事件：

```text
onSubmitted
onStarted
onSuccess
onFailure
onRejected
onTimeout
onFallback
onCompleted
```

---

## 12. fire-and-forget 异常处理器

`execute()` 不返回 `CompletableFuture`，因此任务异常需要统一兜底处理。

```java
@Component
public class MyAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleException(TaskExecutionEvent event, Throwable throwable) {
        log.error("fire-and-forget task failed, taskId={}, taskName={}, bizKey={}",
                event.getTaskId(),
                event.getTaskName(),
                event.getBizKey(),
                throwable);
    }
}
```

如果业务没有提供 Bean，默认使用 `NoopAsyncUncaughtExceptionHandler`。

---

## 13. ThreadPoolManager 运行时管理

`ThreadPoolManager` 用于查看和调整线程池。

### 13.1 查看单个线程池快照

```java
ThreadPoolSnapshot snapshot = threadPoolManager.snapshot("biz-query-pool");
```

### 13.2 查看所有线程池快照

```java
Map<String, ThreadPoolSnapshot> snapshots = threadPoolManager.snapshots();
```

### 13.3 运行时调整 core / max

```java
ThreadPoolSnapshot snapshot = threadPoolManager.resize("biz-query-pool", 16, 64);
```

### 13.4 完整更新

```java
ThreadPoolUpdateRequest request = new ThreadPoolUpdateRequest();
request.setCorePoolSize(16);
request.setMaximumPoolSize(64);
request.setKeepAliveTime(Duration.ofSeconds(30));
request.setAllowCoreThreadTimeout(true);
request.setPrestartAllCoreThreads(true);

ThreadPoolSnapshot snapshot = threadPoolManager.update("biz-query-pool", request);
```

能安全运行时调整：

- `corePoolSize`
- `maximumPoolSize`
- `keepAliveTime`
- `allowCoreThreadTimeout`
- `rejectedExecutionHandler`
- `prestartAllCoreThreads`

不建议运行时调整：

- `queueType`
- `queueCapacity`
- `threadFactory`

---

## 14. snapshot 怎么理解

组件里有两种 snapshot。

### 14.1 ThreadPoolRegistry.snapshot()

返回：

```java
Map<String, ThreadPoolExecutor>
```

这是注册表快照，适合组件内部使用，例如 Micrometer `Gauge` 绑定真实 `ThreadPoolExecutor`。

### 14.2 ThreadPoolManager.snapshots()

返回：

```java
Map<String, ThreadPoolSnapshot>
```

这是诊断快照，适合管理接口、健康检查、页面展示、日志输出。

`ThreadPoolSnapshot` 包含：

- `corePoolSize`
- `maximumPoolSize`
- `poolSize`
- `largestPoolSize`
- `activeCount`
- `queueSize`
- `queueRemainingCapacity`
- `queueCapacity`
- `activeUsageRatio`
- `queueUsageRatio`
- `completedTaskCount`
- `taskCount`
- `shutdown`
- `terminating`
- `terminated`
- `rejectedExecutionHandler`

---

## 15. 拒绝策略

支持策略：

| 策略 | 说明 |
| --- | --- |
| `ABORT` | 直接拒绝，推荐默认策略。 |
| `CALLER_RUNS` | 调用方线程执行，形成反压。 |
| `BLOCKING_WAIT` | 调用方等待一小段时间，尝试入队。 |
| `DISCARD` | 丢弃当前任务。组件增强后会让 Future 感知拒绝。 |
| `DISCARD_OLDEST` | 丢弃队列最老任务。组件增强后会让被丢弃任务感知拒绝。 |

生产建议：

```text
run / supply / submit：推荐 ABORT、CALLER_RUNS、BLOCKING_WAIT。
execute / tryExecute：可以视业务选择 DISCARD 或 DISCARD_OLDEST。
```

默认建议：

```yaml
rejection-policy: ABORT
```

或短等待：

```yaml
rejection-policy: BLOCKING_WAIT
rejection-wait-time: 100ms
```

---

## 16. 可观测指标

线程池 Gauge 名称由 `ThreadPoolMetricName` 枚举统一维护。

常见指标：

```text
xjtu.iron.concurrency.thread.pool.active
xjtu.iron.concurrency.thread.pool.size
xjtu.iron.concurrency.thread.pool.core.size
xjtu.iron.concurrency.thread.pool.max.size
xjtu.iron.concurrency.thread.pool.queue.size
xjtu.iron.concurrency.thread.pool.queue.remaining
xjtu.iron.concurrency.thread.pool.completed
xjtu.iron.concurrency.thread.pool.task.count
```

后续建议继续补充 Counter / Timer：

- submitted count
- success count
- failure count
- rejected count
- timeout count
- fallback count
- queue cost timer
- run cost timer
- total cost timer

---

## 17. Demo 接口

启动 `concurrency-demo` 后，可以访问：

### AsyncExecutor

```text
GET /demo/async-executor/execute
GET /demo/async-executor/try-execute
GET /demo/async-executor/run
GET /demo/async-executor/supply
GET /demo/async-executor/submit
```

### AsyncTemplate

```text
GET /demo/async-template/all-of
GET /demo/async-template/all-of-outcome
GET /demo/async-template/all-of-fail-fast
GET /demo/async-template/any-of
GET /demo/async-template/with-timeout
GET /demo/async-template/with-fallback
```

### ThreadPoolManager

```text
GET /demo/thread-pool-manager/snapshot
GET /demo/thread-pool-manager/snapshots
GET /demo/thread-pool-manager/resize
GET /demo/thread-pool-manager/update
```

### Context

```text
GET /demo/context/mdc
```

### Enhanced Task

```text
GET /demo/enhanced/metadata
GET /demo/enhanced/queue-cost
GET /demo/enhanced/queue-timeout
GET /demo/enhanced/fire-and-forget-exception
GET /demo/enhanced/timeout-cancel
GET /demo/enhanced/events
```

---

## 18. 关于重试能力的边界

重试不要直接塞进并行组件一期主链路。

推荐边界：

```text
concurrency-component
  负责线程池隔离、任务投递、Future 编排、上下文、指标、诊断。

governance-component
  负责超时、重试、限流、熔断、隔离、降级等治理策略。

message-component / job / 业务补偿表
  负责持久化业务重试、可靠重试、人工补偿。
```

### 18.1 快速重试

快速重试一般是毫秒级、请求内、本地内存级重试。

适合：

- 瞬时网络抖动。
- 偶发 RPC 失败。
- 幂等读请求。

示例策略：

```text
maxAttempts = 3
backoff = 100ms, 200ms, 400ms
retryOn = TimeoutException / IOException
```

这类能力建议放到 `governance-component`，并通过 `concurrency-integrations/governance` 接入。

### 18.2 业务重试

业务重试通常是分钟级、小时级、天级，需要持久化状态。

适合：

- 入账失败重试。
- 三方通知补偿。
- 批处理失败恢复。
- MQ 消息消费失败后重投。

这类能力不应该放在线程池组件里，而应该由业务任务表、消息组件、调度组件或补偿系统负责。

### 18.3 并行组件里是否预留 RetryPolicy

可以预留模型，但不建议一期实现完整重试。

建议：

```text
一期：AsyncTask 可以预留 retryPolicy 字段，但默认不启用。
二期：通过 governance-component 提供 RetryExecutor，再接入 TaskExecutionTemplate。
```

---

## 19. Roadmap

### Phase 1 已完成 / 正在完善

- ThreadPoolExecutor 底座。
- TaskExecutionTemplate 投递模板。
- AsyncExecutor 单任务提交。
- AsyncTemplate 多任务编排。
- AsyncTask 任务元数据。
- TaskExecutionListener 生命周期监听。
- fire-and-forget 异常处理。
- queueCost / runCost / totalCost 统计。
- ThreadPoolManager 运行时查看与调整。
- ThreadPoolSnapshot 诊断快照。
- Micrometer Gauge。
- MDC 上下文传播。
- Demo 覆盖核心 API。

### Phase 1 下一步增强

- Micrometer Counter / Timer 完整落地。
- HealthIndicator 阈值配置化。
- Actuator Endpoint 或管理 Controller。
- AsyncTemplate 增加 `anySuccess`。
- 本地 TaskStatusRegistry，支持按 taskId 查询任务状态。
- AsyncTask 预留 RetryPolicy 字段，但暂不实现复杂重试。

### Phase 2 能力清单

- 自动扩缩容。
- 分布式锁。
- 分布式信号量。
- 分布式令牌桶限流。
- 任务 DAG。
- 强制取消 / 协作式中断。
- 复杂重试策略接入。
- 任务去重与幂等。
- 分布式任务状态中心。
- 与 governance-component 打通：限流、熔断、隔离、降级、重试。
- 与 observability-component 打通：Trace、Metrics、Log、Profile。

---

## 20. 设计结论

`concurrency-component` 的定位不是“线程池工具类”，而是：

```text
生产级本地并行执行组件。
```

它应该管：

- 线程池隔离。
- 任务投递。
- 异步结果。
- 任务编排。
- 上下文传播。
- 任务元数据。
- 生命周期监听。
- 异常兜底。
- 指标监控。
- 运行时诊断。
- 运行时调整。

它不应该直接吞掉所有治理能力。重试、限流、熔断这类策略应优先沉淀到 `governance-component`，再通过 integration 接入。
